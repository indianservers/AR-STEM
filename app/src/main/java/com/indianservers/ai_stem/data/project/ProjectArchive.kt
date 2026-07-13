package com.indianservers.ai_stem.data.project

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProjectArchiveManifest(
    val schemaVersion: Int,
    val projectId: String,
    val projectName: String,
    val sections: Set<String>
)

data class ProjectArchivePayload(
    val manifest: ProjectArchiveManifest,
    val projectJson: String,
    val graphJson: String? = null,
    val sceneJson: String? = null,
    val geometryJson: String? = null,
    val tables: Map<String, String> = emptyMap(),
    val thumbnailPng: ByteArray? = null
)

sealed interface ProjectArchiveResult {
    data class Success(val payload: ProjectArchivePayload) : ProjectArchiveResult
    data class Failure(val userMessage: String, val technicalMessage: String? = null) : ProjectArchiveResult
}

class ProjectArchiveCodec(
    private val maxBytes: Int = 8 * 1024 * 1024,
    private val maxEntries: Int = 64,
    private val maxJsonDepth: Int = 40
) {
    fun export(payload: ProjectArchivePayload): ByteArray {
        require(payload.manifest.schemaVersion == CURRENT_ARCHIVE_SCHEMA) { "Unsupported schema version" }
        require(payload.projectJson.length <= MAX_JSON_CHARS) { "Project JSON is too large" }
        return ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.writeText("manifest.json", payload.manifest.toJson())
                zip.writeText("project.json", payload.projectJson)
                payload.graphJson?.let { zip.writeText("graph.json", it) }
                payload.sceneJson?.let { zip.writeText("scene.json", it) }
                payload.geometryJson?.let { zip.writeText("geometry.json", it) }
                payload.tables.forEach { (name, csv) ->
                    validateEntryName("tables/$name.csv")
                    zip.writeText("tables/$name.csv", csv.take(MAX_TABLE_CHARS))
                }
                payload.thumbnailPng?.let {
                    zip.putNextEntry(ZipEntry("thumbnail.png"))
                    zip.write(it)
                    zip.closeEntry()
                }
            }
            bytes.toByteArray()
        }.also { require(it.size <= maxBytes) { "Archive is too large" } }
    }

    fun import(bytes: ByteArray): ProjectArchiveResult {
        if (bytes.size > maxBytes) return ProjectArchiveResult.Failure("The project file is too large.")
        val entries = mutableMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count += 1
                    if (count > maxEntries) return ProjectArchiveResult.Failure("The project file contains too many entries.")
                    validateEntryName(entry.name)
                    val entryBytes = zip.readBytes()
                    if (entryBytes.size > maxBytes) return ProjectArchiveResult.Failure("A project file entry is too large.")
                    entries[entry.name] = entryBytes
                }
            }
        }.getOrElse {
            return ProjectArchiveResult.Failure("The project file could not be opened.", it.message)
        }
        val manifestText = entries["manifest.json"]?.decodeToString()
            ?: return ProjectArchiveResult.Failure("The project file is missing manifest.json.")
        val manifest = parseManifest(manifestText)
            ?: return ProjectArchiveResult.Failure("The project manifest is invalid.")
        if (manifest.schemaVersion > CURRENT_ARCHIVE_SCHEMA) {
            return ProjectArchiveResult.Failure("This project was saved by a newer app version.")
        }
        val projectJson = entries["project.json"]?.decodeToString()
            ?: return ProjectArchiveResult.Failure("The project file is missing project.json.")
        if (!hasSafeJsonDepth(projectJson)) return ProjectArchiveResult.Failure("The project JSON is too deeply nested.")
        return ProjectArchiveResult.Success(
            ProjectArchivePayload(
                manifest = manifest,
                projectJson = projectJson,
                graphJson = entries["graph.json"]?.decodeToString(),
                sceneJson = entries["scene.json"]?.decodeToString(),
                geometryJson = entries["geometry.json"]?.decodeToString(),
                tables = entries
                    .filterKeys { it.startsWith("tables/") && it.endsWith(".csv") }
                    .mapKeys { it.key.removePrefix("tables/").removeSuffix(".csv") }
                    .mapValues { it.value.decodeToString() },
                thumbnailPng = entries["thumbnail.png"]
            )
        )
    }

    private fun validateEntryName(name: String) {
        require(name.isNotBlank()) { "Blank ZIP entry" }
        require(!name.startsWith("/") && !name.startsWith("\\")) { "Absolute ZIP paths are not allowed" }
        require(".." !in name.split("/", "\\")) { "ZIP path traversal is not allowed" }
    }

    private fun hasSafeJsonDepth(json: String): Boolean {
        var depth = 0
        var inString = false
        var escaped = false
        for (char in json) {
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\' && inString) {
                escaped = true
                continue
            }
            if (char == '"') inString = !inString
            if (!inString && (char == '{' || char == '[')) depth += 1
            if (!inString && (char == '}' || char == ']')) depth -= 1
            if (depth > maxJsonDepth || depth < 0) return false
        }
        return !inString && depth == 0
    }

    private fun parseManifest(json: String): ProjectArchiveManifest? {
        if (!hasSafeJsonDepth(json)) return null
        val version = json.valueFor("schemaVersion")?.toIntOrNull() ?: return null
        val projectId = json.valueFor("projectId") ?: return null
        val projectName = json.valueFor("projectName") ?: return null
        val sections = json.valueFor("sections")?.split('|')?.filter { it.isNotBlank() }?.toSet().orEmpty()
        return ProjectArchiveManifest(version, projectId, projectName, sections)
    }

    private fun ProjectArchiveManifest.toJson(): String =
        """{"schemaVersion":$schemaVersion,"projectId":"${projectId.escapeJson()}","projectName":"${projectName.escapeJson()}","sections":"${sections.joinToString("|").escapeJson()}"}"""

    private fun String.valueFor(key: String): String? {
        val match = Regex(""""$key"\s*:\s*(?:"([^"]*)"|([0-9]+))""").find(this) ?: return null
        return match.groupValues[1].ifBlank { match.groupValues[2] }.unescapeJson()
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
    private fun String.unescapeJson(): String = replace("\\\"", "\"").replace("\\\\", "\\")

    private fun ZipOutputStream.writeText(name: String, value: String) {
        validateEntryName(name)
        putNextEntry(ZipEntry(name))
        write(value.encodeToByteArray())
        closeEntry()
    }

    private companion object {
        const val CURRENT_ARCHIVE_SCHEMA = 1
        const val MAX_JSON_CHARS = 1_000_000
        const val MAX_TABLE_CHARS = 1_000_000
    }
}
