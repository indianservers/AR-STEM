package com.indianservers.ai_stem.domain.geometry

import com.indianservers.ai_stem.domain.scene.Vector3Value
import org.junit.Assert.assertEquals
import org.junit.Test

class NormalWorkspaceConstructionTest {
    @Test
    fun midpointAndSegmentMeasurementsFollowDraggedPoint() {
        val engine = ConstructionGeometryEngine()
        var state = engine.addPoint(ConstructionGeometryState(), Vector3Value(0.0, 0.0, 0.0), "A")
        state = engine.addPoint(state, Vector3Value(4.0, 0.0, 0.0), "B")
        state = engine.createSegmentThroughSelected(state)
        state = engine.createMidpoint(state)

        val pointB = state.points.first { it.label == "B" }
        state = engine.movePoint(state, pointB.id, Vector3Value(6.0, 0.0, 0.0))
        val resolved = engine.resolve(state)
        val midpoint = resolved.first { it.kind == ConstructionObjectKind.Midpoint }.points.first()

        assertEquals(3.0, midpoint.x, 1e-9)
        assertEquals("d=6.00m", resolved.first { it.kind == ConstructionObjectKind.Segment }.valueLabel)
    }
}
