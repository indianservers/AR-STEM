package com.indianservers.ai_stem.data.project

import android.content.Context
import androidx.room.withTransaction
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ProjectSummary(
    val id: String,
    val name: String,
    val type: ProjectType,
    val lastModifiedAt: Long,
    val isFavourite: Boolean,
    val deleted: Boolean,
    val objectCount: Int
)

enum class ProjectSort { Recent, Name }

interface ProjectRepository {
    fun observeRecent(): Flow<List<ProjectEntity>>
    fun search(query: String): Flow<List<ProjectEntity>>
    fun observeDeleted(): Flow<List<ProjectEntity>>
    suspend fun create(name: String, type: ProjectType): ProjectEntity
    suspend fun read(id: String): ProjectEntity?
    suspend fun update(project: ProjectEntity)
    suspend fun delete(id: String)
    suspend fun restore(id: String)
    suspend fun duplicate(id: String, newName: String): ProjectEntity?
    suspend fun saveAs(id: String, newName: String): ProjectEntity?
    suspend fun favourite(id: String, favourite: Boolean)
    suspend fun summary(project: ProjectEntity): ProjectSummary
    fun listRecent(): Flow<List<ProjectEntity>> = observeRecent()
    fun sort(sort: ProjectSort): Flow<List<ProjectEntity>>
}

interface SceneRepository {
    suspend fun saveScene(projectId: String, name: String, objects: List<SceneObjectEntity>)
}

interface GraphProjectRepository {
    suspend fun saveGraphProject(projectId: String, expressions: List<GraphExpressionEntity>)
}

interface DeletedProjectRepository {
    fun observeDeleted(): Flow<List<ProjectEntity>>
    suspend fun restore(projectId: String)
}

data class LegacyMigrationResult(
    val migratedCount: Int,
    val skippedCount: Int,
    val messages: List<String>
)

class RoomProjectRepository(
    private val database: AiStemDatabase,
    private val dao: ProjectDao = database.projectDao()
) : ProjectRepository, DeletedProjectRepository {
    override fun observeRecent(): Flow<List<ProjectEntity>> = dao.observeRecentProjects()
    override fun search(query: String): Flow<List<ProjectEntity>> = dao.searchProjects(query.trim())
    override fun observeDeleted(): Flow<List<ProjectEntity>> = dao.observeDeletedProjects()
    override fun sort(sort: ProjectSort): Flow<List<ProjectEntity>> = when (sort) {
        ProjectSort.Recent -> dao.observeRecentProjects()
        ProjectSort.Name -> dao.observeProjectsSortedByName()
    }

    override suspend fun create(name: String, type: ProjectType): ProjectEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName(name),
            type = type.name,
            createdAt = now,
            lastModifiedAt = now
        )
        dao.insertProject(project)
        project
    }

    override suspend fun read(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        dao.getProject(id)
    }

    override suspend fun update(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.updateProject(project.copy(lastModifiedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val project = dao.getProject(id) ?: return@withContext
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.markDeleted(id, now)
            dao.insertDeletedProject(
                DeletedProjectEntity(
                    projectId = id,
                    name = project.name,
                    type = project.type,
                    deletedAt = now,
                    restoreUntil = now + THIRTY_DAYS_MS
                )
            )
        }
    }

    override suspend fun restore(projectId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.restoreProject(projectId, System.currentTimeMillis())
            dao.removeDeletedProject(projectId)
        }
    }

    override suspend fun duplicate(id: String, newName: String): ProjectEntity? = cloneProject(id, cleanName(newName))

    override suspend fun saveAs(id: String, newName: String): ProjectEntity? = cloneProject(id, cleanName(newName))

    override suspend fun favourite(id: String, favourite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavourite(id, favourite, System.currentTimeMillis())
    }

    override suspend fun summary(project: ProjectEntity): ProjectSummary = withContext(Dispatchers.IO) {
        val graphIds = dao.graphProjects(project.id).map { it.id }
        val graphCount = if (graphIds.isEmpty()) 0 else dao.graphExpressionCount(graphIds)
        val objectCount = dao.sceneObjectCount(project.id) + graphCount
        ProjectSummary(
            id = project.id,
            name = project.name,
            type = runCatching { ProjectType.valueOf(project.type) }.getOrDefault(ProjectType.Mixed),
            lastModifiedAt = project.lastModifiedAt,
            isFavourite = project.isFavourite,
            deleted = project.deletedAt != null,
            objectCount = objectCount
        )
    }

    private suspend fun cloneProject(id: String, newName: String): ProjectEntity? = withContext(Dispatchers.IO) {
        val original = dao.getProject(id) ?: return@withContext null
        val now = System.currentTimeMillis()
        val clone = original.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            createdAt = now,
            lastModifiedAt = now,
            deletedAt = null
        )
        database.withTransaction {
            dao.insertProject(clone)
            val sceneIdMap = mutableMapOf<String, String>()
            dao.scenes(id).forEach { scene ->
                val newSceneId = UUID.randomUUID().toString()
                sceneIdMap[scene.id] = newSceneId
                dao.upsertScene(scene.copy(id = newSceneId, projectId = clone.id, updatedAt = now))
            }
            dao.sceneObjects(id).forEach { sceneObject ->
                dao.upsertSceneObject(
                    sceneObject.copy(
                        id = UUID.randomUUID().toString(),
                        projectId = clone.id,
                        sceneId = sceneIdMap[sceneObject.sceneId] ?: sceneObject.sceneId,
                        groupId = null,
                        updatedAt = now
                    )
                )
            }
            dao.graphProjects(id).forEach { graph ->
                val newGraphId = UUID.randomUUID().toString()
                dao.upsertGraphProject(graph.copy(id = newGraphId, projectId = clone.id, updatedAt = now))
                dao.graphExpressions(listOf(graph.id)).forEach { expression ->
                    dao.upsertGraphExpression(
                        expression.copy(
                            id = UUID.randomUUID().toString(),
                            graphProjectId = newGraphId,
                            updateVersion = now
                        )
                    )
                }
            }
        }
        clone
    }

    private fun cleanName(name: String): String = name.trim().ifBlank { "Untitled Project" }.take(80)

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    }
}

class RoomSceneRepository(
    private val database: AiStemDatabase,
    private val dao: ProjectDao = database.projectDao()
) : SceneRepository {
    override suspend fun saveScene(projectId: String, name: String, objects: List<SceneObjectEntity>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sceneId = objects.firstOrNull()?.sceneId ?: UUID.randomUUID().toString()
        database.withTransaction {
            dao.upsertScene(SceneEntity(sceneId, projectId, cleanName(name), now, now))
            objects.forEach { dao.upsertSceneObject(it.copy(projectId = projectId, sceneId = sceneId, updatedAt = now)) }
        }
    }

    private fun cleanName(name: String): String = name.trim().ifBlank { "Mathematics Scene" }.take(80)
}

class RoomGraphProjectRepository(
    private val database: AiStemDatabase,
    private val dao: ProjectDao = database.projectDao()
) : GraphProjectRepository {
    override suspend fun saveGraphProject(projectId: String, expressions: List<GraphExpressionEntity>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val graphId = expressions.firstOrNull()?.graphProjectId ?: UUID.randomUUID().toString()
        database.withTransaction {
            dao.upsertGraphProject(GraphProjectEntity(graphId, projectId, "Graph Project", null, null, now))
            expressions.forEach { dao.upsertGraphExpression(it.copy(graphProjectId = graphId, updateVersion = now)) }
        }
    }
}

class LegacySceneMigration(
    private val context: Context,
    private val database: AiStemDatabase,
    private val dao: ProjectDao = database.projectDao()
) {
    suspend fun migrateIfNeeded(): LegacyMigrationResult = withContext(Dispatchers.IO) {
        if (dao.metadataValue(MIGRATION_KEY) == "complete") return@withContext LegacyMigrationResult(0, 0, listOf("Legacy migration already complete."))
        val directory = File(context.filesDir, "math-scenes")
        val files = directory.listFiles { file -> file.extension == "scene" }.orEmpty()
        var migrated = 0
        var skipped = 0
        val messages = mutableListOf<String>()
        database.withTransaction {
            files.forEach { file ->
                val parsed = parseLegacyScene(file)
                if (parsed == null) {
                    skipped += 1
                    messages += "Skipped corrupt legacy scene: ${file.name}"
                } else {
                    val now = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                    dao.upsertProject(ProjectEntity(parsed.id, parsed.name, ProjectType.ArScene.name, now, now))
                    dao.upsertScene(SceneEntity("scene-${parsed.id}", parsed.id, parsed.name, now, now))
                    repeat(parsed.objectCount) { index ->
                        dao.upsertSceneObject(
                            SceneObjectEntity(
                                id = "${parsed.id}-object-$index",
                                projectId = parsed.id,
                                sceneId = "scene-${parsed.id}",
                                objectType = "LegacyObject",
                                displayName = "Legacy Object ${index + 1}",
                                transformJson = "{}",
                                parametersJson = "{}",
                                updatedAt = now
                            )
                        )
                    }
                    migrated += 1
                    messages += "Migrated ${parsed.name}"
                }
            }
            dao.upsertMetadata(SchemaMetadataEntity(MIGRATION_KEY, "complete", System.currentTimeMillis()))
        }
        LegacyMigrationResult(migrated, skipped, messages)
    }

    private fun parseLegacyScene(file: File): LegacyScene? = runCatching {
        val lines = file.readLines()
        val schema = lines.firstOrNull { it.startsWith("schema=") }?.removePrefix("schema=")?.toIntOrNull() ?: return null
        if (schema !in 1..10) return null
        val id = lines.firstOrNull { it.startsWith("id=") }?.removePrefix("id=")?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
        val name = lines.firstOrNull { it.startsWith("name=") }?.removePrefix("name=")?.unescapeField() ?: "Migrated Scene"
        LegacyScene(id = id, name = name.take(80), objectCount = lines.count { it.startsWith("object=") })
    }.getOrNull()

    private data class LegacyScene(val id: String, val name: String, val objectCount: Int)

    private companion object {
        const val MIGRATION_KEY = "legacy_scene_migration"
    }
}

private fun String.unescapeField(): String = replace("%0A", "\n").replace("%3D", "=").replace("%25", "%")
