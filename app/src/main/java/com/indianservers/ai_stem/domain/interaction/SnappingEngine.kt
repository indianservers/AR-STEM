package com.indianservers.ai_stem.domain.interaction

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

enum class SnapKind(val label: String) {
    Grid("Grid"),
    Axis("Axis"),
    Origin("Origin"),
    Endpoint("Endpoint"),
    Midpoint("Midpoint"),
    Centre("Centre"),
    Intersection("Intersection"),
    Perpendicular("Perpendicular"),
    Parallel("Parallel"),
    Tangent("Tangent"),
    Angle("Angle"),
    Surface("Surface"),
    ObjectAlignment("Aligned"),
    GraphPoint("Graph Point"),
    GraphCurve("Graph Curve"),
    CoordinateValue("Coordinate")
}

data class SnapSettings(
    val enabled: Boolean = true,
    val distanceStep: Double = 0.01,
    val angleStepDegrees: Double = 15.0,
    val tolerance: Double = 0.03,
    val gridEnabled: Boolean = true,
    val pointEnabled: Boolean = true,
    val axisEnabled: Boolean = true
)

data class SnapTarget(val point: ScreenPoint, val kind: SnapKind, val priority: Int = 0)
data class SnapResult(val point: ScreenPoint, val kind: SnapKind?, val label: String?, val snapped: Boolean)

class SnappingEngine {
    fun snapPoint(point: ScreenPoint, settings: SnapSettings, targets: List<SnapTarget> = emptyList()): SnapResult {
        if (!settings.enabled) return SnapResult(point, null, null, snapped = false)
        val target = if (settings.pointEnabled) {
            targets.minWithOrNull(compareBy<SnapTarget> { point.distanceTo(it.point) }.thenByDescending { it.priority })
                ?.takeIf { point.distanceTo(it.point) <= settings.tolerance }
        } else {
            null
        }
        if (target != null) return SnapResult(target.point, target.kind, target.kind.label, snapped = true)

        if (settings.axisEnabled) {
            if (abs(point.x) <= settings.tolerance) return SnapResult(point.copy(x = 0.0), SnapKind.Axis, SnapKind.Axis.label, true)
            if (abs(point.y) <= settings.tolerance) return SnapResult(point.copy(y = 0.0), SnapKind.Axis, SnapKind.Axis.label, true)
        }
        if (settings.gridEnabled) {
            val grid = ScreenPoint(
                x = round(point.x / settings.distanceStep) * settings.distanceStep,
                y = round(point.y / settings.distanceStep) * settings.distanceStep
            )
            if (point.distanceTo(grid) <= settings.tolerance) return SnapResult(grid, SnapKind.Grid, SnapKind.Grid.label, true)
        }
        return SnapResult(point, null, null, snapped = false)
    }

    fun snapAngleRadians(angleRadians: Double, settings: SnapSettings): Double {
        if (!settings.enabled || settings.angleStepDegrees <= 0.0) return angleRadians
        val step = Math.toRadians(settings.angleStepDegrees)
        return round(angleRadians / step) * step
    }

    fun snapVector(start: ScreenPoint, end: ScreenPoint, settings: SnapSettings): SnapResult {
        val length = start.distanceTo(end)
        if (length == 0.0) return SnapResult(end, null, null, false)
        val angle = atan2(end.y - start.y, end.x - start.x)
        val snappedAngle = snapAngleRadians(angle, settings)
        val snapped = ScreenPoint(start.x + cos(snappedAngle) * length, start.y + sin(snappedAngle) * length)
        return if (abs(snappedAngle - angle) > 1e-9) SnapResult(snapped, SnapKind.Angle, SnapKind.Angle.label, true) else SnapResult(end, null, null, false)
    }
}

private fun ScreenPoint.distanceTo(other: ScreenPoint): Double = hypot(x - other.x, y - other.y)
