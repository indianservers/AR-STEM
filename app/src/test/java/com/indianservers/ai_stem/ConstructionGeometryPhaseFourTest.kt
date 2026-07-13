package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.geometry.ConstructionConstraintKind
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryEngine
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState
import com.indianservers.ai_stem.domain.geometry.ConstructionObjectKind
import com.indianservers.ai_stem.domain.scene.Vector3Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionGeometryPhaseFourTest {
    private val engine = ConstructionGeometryEngine()

    @Test
    fun createsLineSegmentVectorPlaneCircleAndPolygonFromSelectedPoints() {
        val state = baseThreePointState()

        val line = engine.createLineThroughSelected(state)
        val segment = engine.createSegmentThroughSelected(state)
        val vector = engine.createVectorBetweenSelected(state)
        val plane = engine.createPlaneThroughSelected(state)
        val circle = engine.createCircleFromSelected(state)
        val polygon = engine.createPolygonFromSelected(state)

        assertEquals(ConstructionObjectKind.Line, line.objects.last().kind)
        assertEquals(ConstructionObjectKind.Segment, segment.objects.last().kind)
        assertEquals(ConstructionObjectKind.Vector, vector.objects.last().kind)
        assertEquals(ConstructionObjectKind.Plane, plane.objects.last().kind)
        assertEquals(ConstructionObjectKind.Circle, circle.objects.last().kind)
        assertEquals(ConstructionObjectKind.Polygon, polygon.objects.last().kind)
        assertTrue(engine.resolve(polygon).last().valueLabel.startsWith("A="))
    }

    @Test
    fun movingSourcePointUpdatesDependentSegmentLength() {
        val state = baseThreePointState()
        val segment = engine.createSegmentThroughSelected(state)
        val before = engine.resolve(segment).last().valueLabel
        val moved = engine.movePoint(segment, segment.objects.last().pointIds.first(), Vector3Value(0.0, 0.0, 3.0))
        val after = engine.resolve(moved).last().valueLabel

        assertTrue(before != after)
        assertTrue(after.contains("2."))
    }

    @Test
    fun midpointCreatesDerivedPointAndResolvedMidpointObject() {
        val state = baseThreePointState()
        val midpoint = engine.createMidpoint(state)
        val resolved = engine.resolve(midpoint)

        assertTrue(midpoint.points.size > state.points.size)
        assertTrue(resolved.any { it.kind == ConstructionObjectKind.Midpoint })
    }

    @Test
    fun createsParallelAndPerpendicularDependentLines() {
        val state = baseThreePointState()
        val parallel = engine.createParallelThroughSelected(state)
        val perpendicular = engine.createPerpendicularThroughSelected(state)

        assertEquals(ConstructionObjectKind.Parallel, parallel.objects.last().kind)
        assertEquals(ConstructionObjectKind.Perpendicular, perpendicular.objects.last().kind)
        assertTrue(engine.resolve(parallel).last().valueLabel.contains("parallel"))
        assertTrue(engine.resolve(perpendicular).last().valueLabel.contains("perpendicular"))
    }

    @Test
    fun addsConstructionConstraints() {
        val state = engine.createSegmentThroughSelected(baseThreePointState())
        val constrained = engine.addConstraint(state, ConstructionConstraintKind.EqualLength, state.objects.map { it.id })

        assertEquals(1, constrained.constraints.size)
        assertEquals(ConstructionConstraintKind.EqualLength, constrained.constraints.first().kind)
    }

    private fun baseThreePointState(): ConstructionGeometryState {
        val a = engine.addPoint(ConstructionGeometryState(), Vector3Value(0.0, 0.0, 0.0), "A")
        val b = engine.addPoint(a, Vector3Value(1.0, 0.0, 0.0), "B")
        return engine.addPoint(b, Vector3Value(0.0, 0.0, 1.0), "C")
    }
}
