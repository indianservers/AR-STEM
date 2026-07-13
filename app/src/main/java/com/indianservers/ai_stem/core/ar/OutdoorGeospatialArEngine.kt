package com.indianservers.ai_stem.core.ar

import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Mesh
import com.google.ar.core.Session
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.TrackingState
import kotlin.math.atan2
import kotlin.math.sqrt

enum class ArEngineMode(val label: String) {
    Indoor("Indoor AR"),
    AirPlacement("Air Placement"),
    SurfacePlacement("Surface Placement"),
    OutdoorGeospatialMath("Outdoor Geospatial Math")
}

enum class OutdoorGeometryType { Building, Terrain, Unknown }
enum class OutdoorMeshQuality { Lod1, Lod2, Terrain, Unknown }

data class OutdoorGeometryMeshSummary(
    val vertexCount: Int,
    val indexCount: Int,
    val triangleCount: Int
)

data class OutdoorGeometryObservation(
    val id: Int,
    val type: OutdoorGeometryType,
    val quality: OutdoorMeshQuality,
    val mesh: OutdoorGeometryMeshSummary,
    val trackingState: TrackingState
)

data class OutdoorGeometryLessonMetrics(
    val estimatedHeightMeters: Float,
    val footprintWidthMeters: Float,
    val footprintDepthMeters: Float,
    val footprintAreaSquareMeters: Float,
    val volumeApproxCubicMeters: Float,
    val surfaceAreaApproxSquareMeters: Float,
    val rooflineDistanceMeters: Float,
    val angleOfElevationDegrees: Float,
    val slopeToRoofline: Float
)

data class OutdoorGeospatialFrameState(
    val enabled: Boolean,
    val buildingCount: Int,
    val terrainCount: Int,
    val lod1Count: Int,
    val lod2Count: Int,
    val trackedMeshes: List<OutdoorGeometryObservation>,
    val selectedMetrics: OutdoorGeometryLessonMetrics? = null
) {
    val totalMeshCount: Int get() = buildingCount + terrainCount
}

data class OutdoorWireframeEdge(
    val startX: Float,
    val startY: Float,
    val startZ: Float,
    val endX: Float,
    val endY: Float,
    val endZ: Float
)

object OutdoorGeospatialArEngine {
    fun configureSession(session: Session, config: Config, enabled: Boolean): OutdoorSessionConfigurationResult {
        if (!enabled) {
            config.geospatialMode = Config.GeospatialMode.DISABLED
            config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED
            return OutdoorSessionConfigurationResult(enabled = false, geospatialDepthEnabled = false)
        }
        config.geospatialMode = Config.GeospatialMode.ENABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
        val depthSupported = runCatching { session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) }.getOrDefault(false)
        if (depthSupported) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        }
        return OutdoorSessionConfigurationResult(enabled = true, geospatialDepthEnabled = depthSupported)
    }

    fun observeFrame(session: Session, frame: Frame, selectedHit: HitResult? = null): OutdoorGeospatialFrameState {
        val observations = session.getAllTrackables(StreetscapeGeometry::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
            .map { geometry ->
                OutdoorGeometryObservation(
                    id = System.identityHashCode(geometry),
                    type = geometry.type.toOutdoorType(),
                    quality = geometry.quality.toOutdoorQuality(geometry.type),
                    mesh = geometry.mesh.toSummary(),
                    trackingState = geometry.trackingState
                )
            }
            .sortedWith(compareBy<OutdoorGeometryObservation> { it.type != OutdoorGeometryType.Building }.thenBy { it.id })
            .take(MAX_OBSERVED_MESHES)
        val selectedGeometry = selectedHit?.trackable as? StreetscapeGeometry
        return OutdoorGeospatialFrameState(
            enabled = true,
            buildingCount = observations.count { it.type == OutdoorGeometryType.Building },
            terrainCount = observations.count { it.type == OutdoorGeometryType.Terrain },
            lod1Count = observations.count { it.quality == OutdoorMeshQuality.Lod1 },
            lod2Count = observations.count { it.quality == OutdoorMeshQuality.Lod2 },
            trackedMeshes = observations,
            selectedMetrics = selectedGeometry?.estimateLessonMetrics(frame)
        )
    }

    fun geometryHit(frame: Frame, x: Float, y: Float): HitResult? =
        runCatching {
            frame.hitTest(x, y).firstOrNull { hit ->
                val geometry = hit.trackable as? StreetscapeGeometry
                geometry != null && geometry.trackingState == TrackingState.TRACKING
            }
        }.getOrNull()

    fun lessonMetrics(geometry: StreetscapeGeometry, frame: Frame): OutdoorGeometryLessonMetrics =
        geometry.estimateLessonMetrics(frame)

    fun wireframeEdges(geometry: StreetscapeGeometry, maxEdges: Int = MAX_WIREFRAME_EDGES): List<OutdoorWireframeEdge> {
        val mesh = geometry.mesh
        val vertices = mesh.vertexList
        val indices = mesh.indexList
        if (mesh.vertexListSize <= 0 || mesh.indexListSize < TRIANGLE_INDEX_COUNT) return emptyList()
        vertices.rewind()
        indices.rewind()
        val positions = FloatArray(mesh.vertexListSize * 3)
        var positionIndex = 0
        while (vertices.hasRemaining() && positionIndex < positions.size) {
            positions[positionIndex++] = vertices.get()
        }
        vertices.rewind()
        val edges = ArrayList<OutdoorWireframeEdge>(minOf(maxEdges, mesh.indexListSize))
        while (indices.remaining() >= TRIANGLE_INDEX_COUNT && edges.size < maxEdges) {
            val a = indices.get().toInt()
            val b = indices.get().toInt()
            val c = indices.get().toInt()
            addEdge(positions, a, b, edges, maxEdges)
            addEdge(positions, b, c, edges, maxEdges)
            addEdge(positions, c, a, edges, maxEdges)
        }
        indices.rewind()
        return edges
    }

    private fun StreetscapeGeometry.estimateLessonMetrics(frame: Frame): OutdoorGeometryLessonMetrics {
        val bounds = mesh.bounds()
        val height = bounds.height.coerceAtLeast(0f)
        val width = bounds.width.coerceAtLeast(0f)
        val depth = bounds.depth.coerceAtLeast(0f)
        val area = width * depth
        val volume = area * height
        val surfaceArea = 2f * (area + width * height + depth * height)
        val meshPose = meshPose
        val cameraPose = frame.camera.pose
        val dx = meshPose.tx() - cameraPose.tx()
        val dz = meshPose.tz() - cameraPose.tz()
        val horizontalDistance = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
        val roofRise = (meshPose.ty() + bounds.maxY) - cameraPose.ty()
        return OutdoorGeometryLessonMetrics(
            estimatedHeightMeters = height,
            footprintWidthMeters = width,
            footprintDepthMeters = depth,
            footprintAreaSquareMeters = area,
            volumeApproxCubicMeters = volume,
            surfaceAreaApproxSquareMeters = surfaceArea,
            rooflineDistanceMeters = sqrt(horizontalDistance * horizontalDistance + roofRise * roofRise),
            angleOfElevationDegrees = Math.toDegrees(atan2(roofRise, horizontalDistance).toDouble()).toFloat(),
            slopeToRoofline = roofRise / horizontalDistance
        )
    }

    private fun Mesh.toSummary(): OutdoorGeometryMeshSummary =
        OutdoorGeometryMeshSummary(
            vertexCount = vertexListSize,
            indexCount = indexListSize,
            triangleCount = indexListSize / TRIANGLE_INDEX_COUNT
        )

    private fun Mesh.bounds(): MeshBounds {
        val buffer = vertexList
        if (vertexListSize <= 0 || buffer.limit() < 3) return MeshBounds.Zero
        buffer.rewind()
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        while (buffer.remaining() >= 3) {
            val x = buffer.get()
            val y = buffer.get()
            val z = buffer.get()
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
        }
        buffer.rewind()
        return MeshBounds(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun StreetscapeGeometry.Type.toOutdoorType(): OutdoorGeometryType = when (this) {
        StreetscapeGeometry.Type.BUILDING -> OutdoorGeometryType.Building
        StreetscapeGeometry.Type.TERRAIN -> OutdoorGeometryType.Terrain
        else -> OutdoorGeometryType.Unknown
    }

    private fun StreetscapeGeometry.Quality.toOutdoorQuality(type: StreetscapeGeometry.Type): OutdoorMeshQuality = when (this) {
        StreetscapeGeometry.Quality.BUILDING_LOD_1 -> OutdoorMeshQuality.Lod1
        StreetscapeGeometry.Quality.BUILDING_LOD_2 -> OutdoorMeshQuality.Lod2
        else -> if (type == StreetscapeGeometry.Type.TERRAIN) OutdoorMeshQuality.Terrain else OutdoorMeshQuality.Unknown
    }

    private fun addEdge(
        positions: FloatArray,
        startIndex: Int,
        endIndex: Int,
        edges: MutableList<OutdoorWireframeEdge>,
        maxEdges: Int
    ) {
        if (edges.size >= maxEdges) return
        val start = startIndex * 3
        val end = endIndex * 3
        if (start < 0 || end < 0 || start + 2 >= positions.size || end + 2 >= positions.size) return
        edges += OutdoorWireframeEdge(
            startX = positions[start],
            startY = positions[start + 1],
            startZ = positions[start + 2],
            endX = positions[end],
            endY = positions[end + 1],
            endZ = positions[end + 2]
        )
    }

    private const val TRIANGLE_INDEX_COUNT = 3
    private const val MAX_OBSERVED_MESHES = 24
    private const val MAX_WIREFRAME_EDGES = 220
}

data class OutdoorSessionConfigurationResult(
    val enabled: Boolean,
    val geospatialDepthEnabled: Boolean
)

private data class MeshBounds(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val depth: Float get() = maxZ - minZ

    companion object {
        val Zero = MeshBounds(0f, 0f, 0f, 0f, 0f, 0f)
    }
}
