package com.indianservers.ai_stem.data.scene

import android.content.Context
import com.indianservers.ai_stem.domain.scene.CURRENT_SCENE_SCHEMA_VERSION
import com.indianservers.ai_stem.domain.scene.MathScene
import java.io.File

data class SavedSceneSummary(
    val id: String,
    val name: String,
    val modifiedAt: Long,
    val objectCount: Int
)

sealed interface SceneStorageResult {
    data class Success(val scene: MathScene) : SceneStorageResult
    data class Failure(val userMessage: String, val technicalMessage: String? = null) : SceneStorageResult
}

interface SceneRepository {
    fun listScenes(): List<SavedSceneSummary>
    fun save(scene: MathScene, name: String = scene.name): SceneStorageResult
    fun load(sceneId: String): SceneStorageResult
    fun delete(sceneId: String): Boolean
}

class LocalSceneRepository(private val context: Context) : SceneRepository {
    private val directory: File by lazy {
        File(context.filesDir, "math-scenes").apply { mkdirs() }
    }

    override fun listScenes(): List<SavedSceneSummary> =
        directory.listFiles { file -> file.extension == "scene" }
            ?.mapNotNull { file ->
                runCatching {
                    val lines = file.readLines()
                    SavedSceneSummary(
                        id = file.nameWithoutExtension,
                        name = lines.getOrNull(1)?.removePrefix("name=") ?: "Saved Scene",
                        modifiedAt = file.lastModified(),
                        objectCount = lines.count { it.startsWith("object=") }
                    )
                }.getOrNull()
            }
            ?.sortedByDescending { it.modifiedAt }
            ?: emptyList()

    override fun save(scene: MathScene, name: String): SceneStorageResult =
        runCatching {
            val cleanName = name.trim().ifBlank { "Mathematics Scene" }.take(60)
            val savedScene = scene.copy(name = cleanName, updatedAt = System.currentTimeMillis())
            File(directory, "${savedScene.id}.scene").writeText(encodeScene(savedScene))
            SceneStorageResult.Success(savedScene)
        }.getOrElse {
            SceneStorageResult.Failure("The scene could not be saved.", it.message)
        }

    override fun load(sceneId: String): SceneStorageResult =
        runCatching {
            val file = File(directory, "$sceneId.scene")
            if (!file.exists()) return SceneStorageResult.Failure("The saved scene could not be found.")
            decodeScene(file.readText())
        }.getOrElse {
            SceneStorageResult.Failure("The saved scene is damaged and cannot be opened.", it.message)
        }

    override fun delete(sceneId: String): Boolean = File(directory, "$sceneId.scene").delete()
}

fun migrateSceneSchema(schemaVersion: Int): Boolean =
    schemaVersion in 1..CURRENT_SCENE_SCHEMA_VERSION

private fun encodeScene(scene: MathScene): String = buildString {
    appendLine("schema=${scene.schemaVersion}")
    appendLine("name=${scene.name.escapeField()}")
    appendLine("id=${scene.id}")
    appendLine("createdAt=${scene.createdAt}")
    appendLine("updatedAt=${scene.updatedAt}")
    scene.objects.forEach { objectState ->
        appendLine(
            listOf(
                "object",
                objectState.id,
                objectState.definitionId,
                objectState.displayName.escapeField(),
                objectState.transform.position.x,
                objectState.transform.position.y,
                objectState.transform.position.z,
                objectState.transform.scale.x,
                objectState.transform.scale.y,
                objectState.transform.scale.z,
                objectState.interactionState.locked,
                objectState.visibility.visible,
                objectState.groupId ?: ""
            ).joinToString("=")
        )
    }
    scene.groups.forEach { group ->
        appendLine("group=${group.id}=${group.displayName.escapeField()}=${group.memberObjectIds.joinToString(",")}")
    }
    scene.annotations.forEach { annotation ->
        appendLine("annotation=${annotation.id}=${annotation.objectId.orEmpty()}=${annotation.text.escapeField()}=${annotation.hidden}")
    }
}

private fun decodeScene(raw: String): SceneStorageResult {
    val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
    val schema = lines.firstOrNull { it.startsWith("schema=") }?.removePrefix("schema=")?.toIntOrNull()
        ?: return SceneStorageResult.Failure("The saved scene is missing version information.")
    if (!migrateSceneSchema(schema)) {
        return SceneStorageResult.Failure("This scene was saved by an unsupported version of the app.")
    }
    val id = lines.firstOrNull { it.startsWith("id=") }?.removePrefix("id=") ?: return SceneStorageResult.Failure("The saved scene is missing an ID.")
    val name = lines.firstOrNull { it.startsWith("name=") }?.removePrefix("name=")?.unescapeField() ?: "Saved Scene"
    val createdAt = lines.firstOrNull { it.startsWith("createdAt=") }?.removePrefix("createdAt=")?.toLongOrNull() ?: System.currentTimeMillis()
    val objects = lines.filter { it.startsWith("object=") }.mapNotNull { line ->
        val parts = line.split("=")
        if (parts.size < 13) return@mapNotNull null
        val definition = com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry.getDefinition(parts[2]) ?: return@mapNotNull null
        com.indianservers.ai_stem.domain.scene.MathSceneObject(
            id = parts[1],
            definitionId = parts[2],
            displayName = parts[3].unescapeField(),
            objectType = definition.type,
            transform = com.indianservers.ai_stem.domain.scene.ObjectTransform(
                position = com.indianservers.ai_stem.domain.scene.Vector3Value(parts[4].toDouble(), parts[5].toDouble(), parts[6].toDouble()),
                scale = com.indianservers.ai_stem.domain.scene.Vector3Value(parts[7].toDouble(), parts[8].toDouble(), parts[9].toDouble())
            ),
            parameters = definition.defaultParameters,
            interactionState = com.indianservers.ai_stem.domain.scene.ObjectInteractionState(locked = parts[10].toBoolean()),
            visibility = com.indianservers.ai_stem.domain.scene.ObjectVisibility(parts[11].toBoolean()),
            groupId = parts[12].ifBlank { null },
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
    return SceneStorageResult.Success(MathScene(id = id, name = name, schemaVersion = CURRENT_SCENE_SCHEMA_VERSION, objects = objects, createdAt = createdAt))
}

private fun String.escapeField(): String = replace("%", "%25").replace("=", "%3D").replace("\n", "%0A")
private fun String.unescapeField(): String = replace("%0A", "\n").replace("%3D", "=").replace("%25", "%")
