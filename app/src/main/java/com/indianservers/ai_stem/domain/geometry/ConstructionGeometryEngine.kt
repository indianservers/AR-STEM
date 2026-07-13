package com.indianservers.ai_stem.domain.geometry

import com.indianservers.ai_stem.domain.scene.Vector3Value
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

enum class ConstructionObjectKind { Point, Line, Segment, Ray, Vector, Plane, Circle, Polygon, Intersection, Midpoint, Perpendicular, Parallel }
enum class ConstructionDependencyKind { Free, ThroughTwoPoints, VectorBetweenPoints, PlaneThroughThreePoints, CircleCenterPoint, Midpoint, Intersection, ParallelThroughPoint, PerpendicularThroughPoint, PolygonThroughPoints }
enum class ConstructionConstraintKind { FixedDistance, Parallel, Perpendicular, EqualLength, FixedAngle, Coincident }

data class ConstructionPoint(
    val id: String,
    val label: String,
    val position: Vector3Value,
    val free: Boolean = true
)

data class ConstructionObject(
    val id: String,
    val label: String,
    val kind: ConstructionObjectKind,
    val pointIds: List<String>,
    val dependencyKind: ConstructionDependencyKind,
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class ConstructionConstraint(
    val id: String,
    val kind: ConstructionConstraintKind,
    val objectIds: List<String>,
    val value: Double? = null,
    val label: String = kind.name
)

data class ResolvedConstructionObject(
    val id: String,
    val label: String,
    val kind: ConstructionObjectKind,
    val points: List<Vector3Value>,
    val valueLabel: String,
    val visible: Boolean = true
)

data class ConstructionGeometryState(
    val points: List<ConstructionPoint> = emptyList(),
    val objects: List<ConstructionObject> = emptyList(),
    val constraints: List<ConstructionConstraint> = emptyList(),
    val selectedPointIds: List<String> = emptyList(),
    val selectedObjectId: String? = null,
    val revision: Int = 0
) {
    val pointMap: Map<String, ConstructionPoint> get() = points.associateBy { it.id }
}

class ConstructionGeometryEngine {
    fun addPoint(state: ConstructionGeometryState, position: Vector3Value, label: String = "P${state.points.size + 1}"): ConstructionGeometryState {
        val point = ConstructionPoint(id = nextId("pt"), label = label, position = position)
        return state.copy(
            points = state.points + point,
            selectedPointIds = (state.selectedPointIds + point.id).takeLast(3),
            revision = state.revision + 1
        )
    }

    fun movePoint(state: ConstructionGeometryState, pointId: String, position: Vector3Value): ConstructionGeometryState =
        state.copy(
            points = state.points.map { if (it.id == pointId && it.free) it.copy(position = position) else it },
            revision = state.revision + 1
        )

    fun selectPoint(state: ConstructionGeometryState, pointId: String, multi: Boolean = true): ConstructionGeometryState {
        val ids = if (multi) (state.selectedPointIds + pointId).distinct().takeLast(3) else listOf(pointId)
        return state.copy(selectedPointIds = ids)
    }

    fun createLineThroughSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Line, ConstructionDependencyKind.ThroughTwoPoints, 2, "Line")

    fun createSegmentThroughSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Segment, ConstructionDependencyKind.ThroughTwoPoints, 2, "Segment")

    fun createVectorBetweenSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Vector, ConstructionDependencyKind.VectorBetweenPoints, 2, "Vector")

    fun createPlaneThroughSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Plane, ConstructionDependencyKind.PlaneThroughThreePoints, 3, "Plane")

    fun createCircleFromSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Circle, ConstructionDependencyKind.CircleCenterPoint, 2, "Circle")

    fun createPolygonFromSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Polygon, ConstructionDependencyKind.PolygonThroughPoints, 3, "Polygon")

    fun createMidpoint(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Midpoint, ConstructionDependencyKind.Midpoint, 2, "Midpoint", createsDerivedPoint = true)

    fun createParallelThroughSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Parallel, ConstructionDependencyKind.ParallelThroughPoint, 3, "Parallel")

    fun createPerpendicularThroughSelected(state: ConstructionGeometryState): ConstructionGeometryState =
        createObject(state, ConstructionObjectKind.Perpendicular, ConstructionDependencyKind.PerpendicularThroughPoint, 3, "Perpendicular")

    fun addConstraint(state: ConstructionGeometryState, kind: ConstructionConstraintKind, objectIds: List<String>, value: Double? = null): ConstructionGeometryState =
        state.copy(
            constraints = state.constraints + ConstructionConstraint(nextId("constraint"), kind, objectIds, value, kind.label()),
            revision = state.revision + 1
        )

    fun resolve(state: ConstructionGeometryState): List<ResolvedConstructionObject> {
        val points = state.pointMap
        return state.objects.mapNotNull { obj ->
            val resolvedPoints = obj.pointIds.mapNotNull { points[it]?.position }
            if (resolvedPoints.size < obj.pointIds.size) return@mapNotNull null
            when (obj.kind) {
                ConstructionObjectKind.Line,
                ConstructionObjectKind.Parallel,
                ConstructionObjectKind.Perpendicular -> resolveLineLike(obj, resolvedPoints)
                ConstructionObjectKind.Segment -> ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints.take(2), "d=${format(distance(resolvedPoints[0], resolvedPoints[1]))}m", obj.visible)
                ConstructionObjectKind.Ray -> ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints.take(2), "ray", obj.visible)
                ConstructionObjectKind.Vector -> {
                    val a = resolvedPoints[0]
                    val b = resolvedPoints[1]
                    ResolvedConstructionObject(obj.id, obj.label, obj.kind, listOf(a, b), "|v|=${format(distance(a, b))}", obj.visible)
                }
                ConstructionObjectKind.Plane -> ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints.take(3), "plane ${format(planeAreaHint(resolvedPoints))}m2", obj.visible)
                ConstructionObjectKind.Circle -> {
                    val radius = distance(resolvedPoints[0], resolvedPoints[1])
                    ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints.take(2), "r=${format(radius)}m", obj.visible)
                }
                ConstructionObjectKind.Polygon -> ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints, "A=${format(polygonAreaXZ(resolvedPoints))}m2", obj.visible)
                ConstructionObjectKind.Midpoint -> {
                    val mid = midpoint(resolvedPoints[0], resolvedPoints[1])
                    ResolvedConstructionObject(obj.id, obj.label, obj.kind, listOf(mid), "mid (${format(mid.x)}, ${format(mid.z)})", obj.visible)
                }
                ConstructionObjectKind.Point,
                ConstructionObjectKind.Intersection -> ResolvedConstructionObject(obj.id, obj.label, obj.kind, resolvedPoints, obj.kind.name, obj.visible)
            }
        }
    }

    private fun createObject(
        state: ConstructionGeometryState,
        kind: ConstructionObjectKind,
        dependency: ConstructionDependencyKind,
        requiredPoints: Int,
        labelPrefix: String,
        createsDerivedPoint: Boolean = false
    ): ConstructionGeometryState {
        val ids = state.selectedPointIds.takeLast(requiredPoints)
        require(ids.size == requiredPoints) { "$labelPrefix needs $requiredPoints points." }
        val label = "$labelPrefix ${state.objects.count { it.kind == kind } + 1}"
        val obj = ConstructionObject(nextId("construction"), label, kind, ids, dependency)
        val next = state.copy(objects = state.objects + obj, selectedObjectId = obj.id, revision = state.revision + 1)
        return if (!createsDerivedPoint) next else {
            val resolved = resolve(next).firstOrNull { it.id == obj.id }?.points?.firstOrNull() ?: return next
            addPoint(next, resolved, "M${next.points.size + 1}").copy(selectedObjectId = obj.id)
        }
    }

    private fun resolveLineLike(obj: ConstructionObject, points: List<Vector3Value>): ResolvedConstructionObject {
        val a = points[0]
        val b = points[1]
        val direction = when (obj.dependencyKind) {
            ConstructionDependencyKind.ParallelThroughPoint -> b.minus(a)
            ConstructionDependencyKind.PerpendicularThroughPoint -> {
                val base = b.minus(a)
                Vector3Value(-base.z, base.y, base.x)
            }
            else -> b.minus(a)
        }
        val anchor = if (obj.dependencyKind in setOf(ConstructionDependencyKind.ParallelThroughPoint, ConstructionDependencyKind.PerpendicularThroughPoint)) {
            points[2]
        } else {
            a
        }
        val length = 0.7
        val unit = direction.normalized()
        val start = anchor.minus(unit.scale(length))
        val end = anchor.plus(unit.scale(length))
        val label = when (obj.kind) {
            ConstructionObjectKind.Perpendicular -> "perpendicular"
            ConstructionObjectKind.Parallel -> "parallel"
            else -> "angle=${format(angleXZ(a, b))}deg"
        }
        return ResolvedConstructionObject(obj.id, obj.label, obj.kind, listOf(start, end), label, obj.visible)
    }

    private fun distance(a: Vector3Value, b: Vector3Value): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun midpoint(a: Vector3Value, b: Vector3Value): Vector3Value =
        Vector3Value((a.x + b.x) / 2.0, (a.y + b.y) / 2.0, (a.z + b.z) / 2.0)

    private fun planeAreaHint(points: List<Vector3Value>): Double =
        if (points.size < 3) 0.0 else distance(points[0], points[1]) * distance(points[0], points[2]) / 2.0

    private fun polygonAreaXZ(points: List<Vector3Value>): Double {
        if (points.size < 3) return 0.0
        val sum = points.indices.sumOf { i ->
            val a = points[i]
            val b = points[(i + 1) % points.size]
            a.x * b.z - b.x * a.z
        }
        return abs(sum) / 2.0
    }

    private fun angleXZ(a: Vector3Value, b: Vector3Value): Double {
        val dx = b.x - a.x
        val dz = b.z - a.z
        val length = hypot(dx, dz)
        if (length == 0.0) return 0.0
        return Math.toDegrees(acos((dx / length).coerceIn(-1.0, 1.0)))
    }

    private fun Vector3Value.minus(other: Vector3Value) = Vector3Value(x - other.x, y - other.y, z - other.z)
    private fun Vector3Value.scale(value: Double) = Vector3Value(x * value, y * value, z * value)
    private fun Vector3Value.normalized(): Vector3Value {
        val length = sqrt(x * x + y * y + z * z).takeIf { it > 1e-9 } ?: 1.0
        return Vector3Value(x / length, y / length, z / length)
    }

    private fun nextId(prefix: String): String = "$prefix-${System.currentTimeMillis()}-${counter++}"
    private fun format(value: Double): String = "%.2f".format(value)

    companion object {
        private var counter = 0L
    }
}

private fun ConstructionConstraintKind.label(): String = when (this) {
    ConstructionConstraintKind.FixedDistance -> "Fixed distance"
    ConstructionConstraintKind.Parallel -> "Parallel"
    ConstructionConstraintKind.Perpendicular -> "Perpendicular"
    ConstructionConstraintKind.EqualLength -> "Equal length"
    ConstructionConstraintKind.FixedAngle -> "Fixed angle"
    ConstructionConstraintKind.Coincident -> "Coincident"
}
