package com.indianservers.ai_stem.domain.interaction

import com.indianservers.ai_stem.domain.graph.ArCompiledExpression
import com.indianservers.ai_stem.domain.graph.ArGraphAnalysisPoint
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.scene.Vector3Value
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sqrt

enum class ArGestureHandle(val label: String) {
    MoveX("Move X"),
    MoveY("Move Y"),
    MoveZ("Move Z"),
    RotateY("Rotate"),
    UniformScale("Scale"),
    StretchX("Stretch X"),
    StretchZ("Stretch Z"),
    Lift("Lift")
}

data class ArTransformHandleAction(
    val handle: ArGestureHandle,
    val delta: Double = 1.0
)

data class ArSnappingProfile(
    val enabled: Boolean = true,
    val gridStepMeters: Double = 0.05,
    val axisToleranceMeters: Double = 0.025,
    val graphToleranceMeters: Double = 0.04,
    val buildingEdgeToleranceMeters: Double = 0.08,
    val paperAxisToleranceMeters: Double = 0.035
)

data class ArSnapCandidate(
    val position: Vector3Value,
    val kind: SnapKind,
    val label: String,
    val priority: Int = 0
)

data class ArSnapResult(
    val position: Vector3Value,
    val kind: SnapKind?,
    val label: String,
    val snapped: Boolean
)

data class ArPickedMathPoint(
    val worldPosition: Vector3Value,
    val graphX: Double,
    val graphY: Double,
    val label: String,
    val distanceToGraph: Double
)

data class ArRulerMeasurement(
    val distance: Double,
    val rise: Double,
    val run: Double,
    val angleDegrees: Double,
    val slope: Double,
    val displacement: Vector3Value
)

class ArInteractionEngine(private val mathEngine: ArMathEngine = ArMathEngine()) {
    fun snapWorldPoint(
        point: Vector3Value,
        profile: ArSnappingProfile,
        candidates: List<ArSnapCandidate> = emptyList()
    ): ArSnapResult {
        if (!profile.enabled) return ArSnapResult(point, null, "Free", snapped = false)
        val candidate = candidates
            .filter { point.distanceTo(it.position) <= profile.graphToleranceMeters }
            .minWithOrNull(compareBy<ArSnapCandidate> { point.distanceTo(it.position) }.thenByDescending { it.priority })
        if (candidate != null) return ArSnapResult(candidate.position, candidate.kind, candidate.label, snapped = true)

        if (abs(point.x) <= profile.axisToleranceMeters) {
            return ArSnapResult(point.copy(x = 0.0), SnapKind.Axis, "X=0 axis", true)
        }
        if (abs(point.z) <= profile.axisToleranceMeters) {
            return ArSnapResult(point.copy(z = 0.0), SnapKind.Axis, "Z=0 axis", true)
        }
        val grid = Vector3Value(
            round(point.x / profile.gridStepMeters) * profile.gridStepMeters,
            round(point.y / profile.gridStepMeters) * profile.gridStepMeters,
            round(point.z / profile.gridStepMeters) * profile.gridStepMeters
        )
        return if (point.distanceTo(grid) <= profile.gridStepMeters * 0.75) {
            ArSnapResult(grid, SnapKind.Grid, "Grid ${profile.gridStepMeters}m", true)
        } else {
            ArSnapResult(point, null, "Free", false)
        }
    }

    fun graphSnapCandidates(
        compiled: ArCompiledExpression,
        analysisPoints: List<ArGraphAnalysisPoint>,
        worldScale: Double = 0.48
    ): List<ArSnapCandidate> =
        analysisPoints.map {
            ArSnapCandidate(
                position = graphToWorld(compiled, it.x, it.y, worldScale),
                kind = when {
                    it.label.startsWith("r") -> SnapKind.Intersection
                    it.label.startsWith("i") -> SnapKind.Intersection
                    else -> SnapKind.GraphPoint
                },
                label = "${it.label} (${format(it.x)}, ${format(it.y)})",
                priority = 10
            )
        }

    fun pickGraphPoint(
        compiled: ArCompiledExpression,
        worldPoint: Vector3Value,
        worldScale: Double = 0.48
    ): ArPickedMathPoint {
        val x = worldToGraphX(compiled, worldPoint.x, worldScale)
        val y = mathEngine.evaluate2d(compiled, x)
        val graphWorld = graphToWorld(compiled, x, y, worldScale)
        return ArPickedMathPoint(
            worldPosition = graphWorld,
            graphX = x,
            graphY = y,
            label = "(${format(x)}, ${format(y)})",
            distanceToGraph = worldPoint.distanceTo(graphWorld)
        )
    }

    fun rulerMeasurement(a: Vector3Value, b: Vector3Value): ArRulerMeasurement {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val run = hypot(dx, dz)
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        return ArRulerMeasurement(
            distance = distance,
            rise = dy,
            run = run,
            angleDegrees = Math.toDegrees(atan2(dy, run)),
            slope = if (run < 1e-9) Double.POSITIVE_INFINITY else dy / run,
            displacement = Vector3Value(dx, dy, dz)
        )
    }

    private fun graphToWorld(compiled: ArCompiledExpression, graphX: Double, graphY: Double, worldScale: Double): Vector3Value {
        val domain = compiled.domain
        val normalizedX = ((graphX - domain.xMin) / (domain.xMax - domain.xMin)).coerceIn(0.0, 1.0) - 0.5
        return Vector3Value(
            x = normalizedX * worldScale,
            y = 0.018,
            z = graphY.coerceIn(-compiled.domain.valueClamp, compiled.domain.valueClamp) * 0.08
        )
    }

    private fun worldToGraphX(compiled: ArCompiledExpression, worldX: Double, worldScale: Double): Double {
        val normalized = (worldX / worldScale + 0.5).coerceIn(0.0, 1.0)
        return compiled.domain.xMin + (compiled.domain.xMax - compiled.domain.xMin) * normalized
    }

    private fun Vector3Value.distanceTo(other: Vector3Value): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun format(value: Double): String = "%.2f".format(value)
}
