package com.indianservers.ai_stem.data.project

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProjectType { Graph, Geometry, ArScene, Data, Mixed }

@Entity(
    tableName = "projects",
    indices = [Index("name"), Index("lastModifiedAt"), Index("isFavourite"), Index("deletedAt")]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val isFavourite: Boolean = false,
    val deletedAt: Long? = null,
    val schemaVersion: Int = 1
)

@Entity(
    tableName = "scenes",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class SceneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "scene_groups",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class SceneGroupEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String
)

@Entity(
    tableName = "scene_objects",
    foreignKeys = [
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SceneEntity::class, ["id"], ["sceneId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SceneGroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("projectId"), Index("sceneId"), Index("groupId")]
)
data class SceneObjectEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sceneId: String,
    val groupId: String? = null,
    val objectType: String,
    val displayName: String,
    val transformJson: String,
    val parametersJson: String,
    val appearanceId: String? = null,
    val locked: Boolean = false,
    val visible: Boolean = true,
    val updatedAt: Long
)

@Entity(
    tableName = "graph_projects",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class GraphProjectEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val viewportId: String?,
    val appearanceId: String?,
    val updatedAt: Long
)

@Entity(
    tableName = "graph_expressions",
    foreignKeys = [ForeignKey(GraphProjectEntity::class, ["id"], ["graphProjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("graphProjectId"), Index("visible")]
)
data class GraphExpressionEntity(
    @PrimaryKey val id: String,
    val graphProjectId: String,
    val kind: String,
    val source: String,
    val displayName: String,
    val styleJson: String,
    val visible: Boolean = true,
    val updateVersion: Long
)

@Entity(
    tableName = "graph_sliders",
    foreignKeys = [ForeignKey(GraphProjectEntity::class, ["id"], ["graphProjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("graphProjectId")]
)
data class GraphSliderEntity(
    @PrimaryKey val id: String,
    val graphProjectId: String,
    val symbol: String,
    val value: Double,
    val min: Double,
    val max: Double,
    val step: Double
)

@Entity(
    tableName = "data_tables",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class DataTableEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val csvPreview: String,
    val rowCount: Int,
    val columnCount: Int
)

@Entity(
    tableName = "geometry_objects",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("parentId")]
)
data class GeometryObjectEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val parentId: String? = null,
    val kind: String,
    val definitionJson: String,
    val visible: Boolean = true,
    val locked: Boolean = false
)

@Entity(
    tableName = "construction_dependencies",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("sourceObjectId"), Index("targetObjectId")]
)
data class ConstructionDependencyEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sourceObjectId: String,
    val targetObjectId: String,
    val dependencyKind: String
)

@Entity(
    tableName = "annotations",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("targetId")]
)
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val targetId: String?,
    val text: String,
    val worldPositionJson: String?,
    val visible: Boolean = true
)

@Entity(
    tableName = "measurements",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("targetId")]
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val targetId: String,
    val kind: String,
    val value: Double,
    val unit: String,
    val updatedAt: Long
)

@Entity(
    tableName = "appearances",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class AppearanceEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val color: Long,
    val opacity: Double,
    val strokeWidth: Double,
    val materialJson: String
)

@Entity(
    tableName = "viewports",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class ViewportEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minZ: Double? = null,
    val maxZ: Double? = null
)

@Entity(
    tableName = "ar_placements",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class ArPlacementEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val placementStyle: String,
    val planeKind: String?,
    val poseJson: String,
    val scale: Double,
    val provisional: Boolean = false
)

@Entity(
    tableName = "project_thumbnails",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class ProjectThumbnailEntity(
    @PrimaryKey val projectId: String,
    val pngBytes: ByteArray,
    val updatedAt: Long
)

@Entity(tableName = "deleted_projects", indices = [Index("deletedAt")])
data class DeletedProjectEntity(
    @PrimaryKey val projectId: String,
    val name: String,
    val type: String,
    val deletedAt: Long,
    val restoreUntil: Long
)

@Entity(tableName = "schema_metadata")
data class SchemaMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)
