package com.indianservers.ai_stem.feature.games.geometryarchitect

import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.abs
import kotlin.math.sqrt

enum class ArchitectMode { SingleDesigner, CollaborativeTeam, ClassroomDesignCompetition }
enum class ShapeConstraintType { Area, Perimeter, SurfaceArea, Volume, Scale, Transformation, Stability }
enum class ArchitectArState { SurfaceScanning, GridLocked, WeakTracking, Paused }

data class DesignBrief(
    val briefId: String,
    val title: String,
    val requiredConstraints: List<ShapeConstraint>,
    val materialBudget: MaterialBudget
)

data class GeometryVertex(val id: String, val position: Vector3Dto)
data class GeometryEdge(val id: String, val startVertexId: String, val endVertexId: String)
data class GeometryConstruction(val constructionId: String, val vertices: List<GeometryVertex>, val edges: List<GeometryEdge>)
data class ShapeConstraint(val constraintId: String, val type: ShapeConstraintType, val targetValue: Double?, val tolerance: Double)
data class ScaleModel(val unitsPerMetre: Double, val displayUnit: String)
data class MaterialBudget(val maxUnits: Double, val materialName: String)
data class ArchitectValidationResult(val valid: Boolean, val messages: List<String>)

data class ArchitectBriefTemplate(
    val brief: DesignBrief,
    val prompt: String,
    val expectedLength: Double,
    val expectedWidth: Double,
    val expectedHeight: Double,
    val points: Int
)

data class ArchitectProgress(
    val briefIndex: Int = 0,
    val score: Int = 0,
    val completedBriefIds: Set<String> = emptySet(),
    val attempts: Int = 0,
    val arState: ArchitectArState = ArchitectArState.SurfaceScanning
)

interface MeasurementServiceContract {
    fun distanceMetres(a: Vector3Dto, b: Vector3Dto): Double
}

class GeometryArchitectEngine : MeasurementServiceContract {
    val briefs: List<ArchitectBriefTemplate> = buildBriefs()

    fun currentBrief(progress: ArchitectProgress): ArchitectBriefTemplate = briefs[progress.briefIndex.coerceIn(0, briefs.lastIndex)]

    fun validate(template: ArchitectBriefTemplate, length: Double, width: Double, height: Double): ArchitectValidationResult {
        if (!listOf(length, width, height).all { it.isFinite() && it > 0.0 }) {
            return ArchitectValidationResult(false, listOf("All dimensions must be positive numbers."))
        }
        val area = length * width
        val perimeter = 2.0 * (length + width)
        val volume = area * height
        val surfaceArea = 2.0 * (length * width + length * height + width * height)
        val messages = mutableListOf<String>()
        template.brief.requiredConstraints.forEach { constraint ->
            val measured = when (constraint.type) {
                ShapeConstraintType.Area -> area
                ShapeConstraintType.Perimeter -> perimeter
                ShapeConstraintType.SurfaceArea -> surfaceArea
                ShapeConstraintType.Volume -> volume
                ShapeConstraintType.Scale -> length / width
                ShapeConstraintType.Transformation -> height
                ShapeConstraintType.Stability -> width / height
            }
            val target = constraint.targetValue
            if (target != null && abs(measured - target) > constraint.tolerance) {
                messages += "${constraint.type.name.lowercase()} should be near ${round(target)} but is ${round(measured)}."
            }
        }
        val materialUsed = surfaceArea
        if (materialUsed > template.brief.materialBudget.maxUnits) {
            messages += "Material use ${round(materialUsed)} exceeds ${template.brief.materialBudget.maxUnits} ${template.brief.materialBudget.materialName}."
        }
        return if (messages.isEmpty()) {
            ArchitectValidationResult(true, listOf("Design approved. Area, volume and material constraints match the brief."))
        } else {
            ArchitectValidationResult(false, messages)
        }
    }

    fun apply(progress: ArchitectProgress, template: ArchitectBriefTemplate, result: ArchitectValidationResult): ArchitectProgress {
        val passed = result.valid
        return progress.copy(
            briefIndex = if (passed) (progress.briefIndex + 1).coerceAtMost(briefs.lastIndex) else progress.briefIndex,
            score = progress.score + if (passed) template.points else 0,
            completedBriefIds = if (passed) progress.completedBriefIds + template.brief.briefId else progress.completedBriefIds,
            attempts = progress.attempts + 1,
            arState = if (passed) ArchitectArState.GridLocked else progress.arState
        )
    }

    fun rectangleConstruction(id: String, length: Float, width: Float): GeometryConstruction {
        val vertices = listOf(
            GeometryVertex("a", Vector3Dto(0f, 0f, 0f)),
            GeometryVertex("b", Vector3Dto(length, 0f, 0f)),
            GeometryVertex("c", Vector3Dto(length, 0f, width)),
            GeometryVertex("d", Vector3Dto(0f, 0f, width))
        )
        return GeometryConstruction(
            id,
            vertices,
            listOf(
                GeometryEdge("ab", "a", "b"),
                GeometryEdge("bc", "b", "c"),
                GeometryEdge("cd", "c", "d"),
                GeometryEdge("da", "d", "a")
            )
        )
    }

    fun isClosedPolygon(construction: GeometryConstruction): Boolean {
        if (construction.vertices.size < 3 || construction.edges.size < 3) return false
        val degree = construction.vertices.associate { vertex ->
            vertex.id to construction.edges.count { it.startVertexId == vertex.id || it.endVertexId == vertex.id }
        }
        return degree.values.all { it == 2 }
    }

    override fun distanceMetres(a: Vector3Dto, b: Vector3Dto): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    private fun buildBriefs(): List<ArchitectBriefTemplate> = (1..12).map { index ->
        val length = 2.0 + index
        val width = 1.0 + (index % 4)
        val height = 1.0 + (index % 3)
        val area = length * width
        val volume = area * height
        val surface = 2.0 * (length * width + length * height + width * height)
        ArchitectBriefTemplate(
            brief = DesignBrief(
                briefId = "architect-$index",
                title = "Structure $index",
                requiredConstraints = listOf(
                    ShapeConstraint("area-$index", ShapeConstraintType.Area, area, 0.05),
                    ShapeConstraint("volume-$index", ShapeConstraintType.Volume, volume, 0.05),
                    ShapeConstraint("surface-$index", ShapeConstraintType.SurfaceArea, surface, 0.10)
                ),
                materialBudget = MaterialBudget(surface + 1.0, "surface units")
            ),
            prompt = "Build a ${round(length)} m x ${round(width)} m footprint with ${round(height)} m height.",
            expectedLength = length,
            expectedWidth = width,
            expectedHeight = height,
            points = 90 + (index % 4) * 10
        )
    }

    private fun round(value: Double): String = "%.2f".format(value).trimEnd('0').trimEnd('.')
}
