package com.indianservers.ai_stem.domain.interaction

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class PickableKind {
    Solid,
    GraphCurve,
    GraphPoint,
    Surface,
    GeometryPoint,
    Line,
    Segment,
    Ray,
    Circle,
    Polygon,
    Vector,
    Label,
    TransformHandle
}

data class ScreenPoint(val x: Double, val y: Double)
data class ScreenRect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun contains(point: ScreenPoint): Boolean =
        point.x in left..right && point.y in top..bottom
}

sealed interface PickShape {
    data class Point(val center: ScreenPoint, val radiusPx: Double) : PickShape
    data class Segment(val start: ScreenPoint, val end: ScreenPoint, val tolerancePx: Double) : PickShape
    data class Circle(val center: ScreenPoint, val radiusPx: Double, val tolerancePx: Double, val filled: Boolean = false) : PickShape
    data class Rect(val rect: ScreenRect) : PickShape
    data class Polyline(val points: List<ScreenPoint>, val tolerancePx: Double) : PickShape
}

data class PickableItem(
    val id: String,
    val name: String,
    val kind: PickableKind,
    val shape: PickShape,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val groupId: String? = null,
    val layer: Int = 0,
    val selectable: Boolean = true
)

data class PickHit(
    val item: PickableItem,
    val distancePx: Double,
    val layer: Int
)

data class PickResult(
    val primary: PickHit?,
    val candidates: List<PickHit>
) {
    val needsDisambiguation: Boolean get() = candidates.size > 1
}

class PickingEngine {
    fun pick(point: ScreenPoint, items: List<PickableItem>): PickResult {
        val hits = items
            .asSequence()
            .filter { it.visible && it.selectable }
            .mapNotNull { item -> distance(point, item.shape)?.let { PickHit(item, it, item.layer) } }
            .sortedWith(compareBy<PickHit> { it.distancePx }.thenByDescending { it.layer })
            .toList()
        return PickResult(hits.firstOrNull(), hits)
    }

    fun selectNext(currentId: String?, candidates: List<PickHit>): PickHit? {
        if (candidates.isEmpty()) return null
        val index = candidates.indexOfFirst { it.item.id == currentId }
        return candidates[(index + 1).floorMod(candidates.size)]
    }

    private fun distance(point: ScreenPoint, shape: PickShape): Double? =
        when (shape) {
            is PickShape.Point -> point.distanceTo(shape.center).takeIf { it <= shape.radiusPx }
            is PickShape.Segment -> distanceToSegment(point, shape.start, shape.end).takeIf { it <= shape.tolerancePx }
            is PickShape.Circle -> {
                val distance = point.distanceTo(shape.center)
                if (shape.filled && distance <= shape.radiusPx) 0.0 else abs(distance - shape.radiusPx).takeIf { it <= shape.tolerancePx }
            }
            is PickShape.Rect -> if (shape.rect.contains(point)) 0.0 else null
            is PickShape.Polyline -> shape.points.zipWithNext()
                .minOfOrNull { (a, b) -> distanceToSegment(point, a, b) }
                ?.takeIf { it <= shape.tolerancePx }
        }

    private fun distanceToSegment(point: ScreenPoint, start: ScreenPoint, end: ScreenPoint): Double {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0.0 && dy == 0.0) return point.distanceTo(start)
        val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        return point.distanceTo(ScreenPoint(start.x + t * dx, start.y + t * dy))
    }
}

private fun ScreenPoint.distanceTo(other: ScreenPoint): Double = hypot(x - other.x, y - other.y)
private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
