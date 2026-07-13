package com.indianservers.ai_stem.data.project

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun hardDeleteProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY lastModifiedAt DESC")
    fun observeRecentProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL AND name LIKE '%' || :query || '%' ORDER BY lastModifiedAt DESC")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun observeProjectsSortedByName(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedProjects(): Flow<List<ProjectEntity>>

    @Query("UPDATE projects SET name = :name, lastModifiedAt = :modifiedAt WHERE id = :id")
    suspend fun renameProject(id: String, name: String, modifiedAt: Long)

    @Query("UPDATE projects SET isFavourite = :favourite, lastModifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setFavourite(id: String, favourite: Boolean, modifiedAt: Long)

    @Query("UPDATE projects SET deletedAt = :deletedAt, lastModifiedAt = :deletedAt WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long)

    @Query("UPDATE projects SET deletedAt = NULL, lastModifiedAt = :restoredAt WHERE id = :id")
    suspend fun restoreProject(id: String, restoredAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedProject(project: DeletedProjectEntity)

    @Query("DELETE FROM deleted_projects WHERE projectId = :projectId")
    suspend fun removeDeletedProject(projectId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScene(scene: SceneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSceneObject(sceneObject: SceneObjectEntity)

    @Query("SELECT COUNT(*) FROM scene_objects WHERE projectId = :projectId")
    suspend fun sceneObjectCount(projectId: String): Int

    @Query("SELECT * FROM scene_objects WHERE projectId = :projectId")
    suspend fun sceneObjects(projectId: String): List<SceneObjectEntity>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId")
    suspend fun scenes(projectId: String): List<SceneEntity>

    @Query("SELECT * FROM graph_projects WHERE projectId = :projectId")
    suspend fun graphProjects(projectId: String): List<GraphProjectEntity>

    @Query("SELECT * FROM graph_expressions WHERE graphProjectId IN (:graphProjectIds)")
    suspend fun graphExpressions(graphProjectIds: List<String>): List<GraphExpressionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGraphProject(project: GraphProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGraphExpression(expression: GraphExpressionEntity)

    @Query("SELECT COUNT(*) FROM graph_expressions WHERE graphProjectId IN (:graphProjectIds)")
    suspend fun graphExpressionCount(graphProjectIds: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: SchemaMetadataEntity)

    @Query("SELECT value FROM schema_metadata WHERE `key` = :key")
    suspend fun metadataValue(key: String): String?
}
