package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.interaction.CoalescedInteractionHistory
import com.indianservers.ai_stem.domain.interaction.PickShape
import com.indianservers.ai_stem.domain.interaction.PickableItem
import com.indianservers.ai_stem.domain.interaction.PickableKind
import com.indianservers.ai_stem.domain.interaction.PickingEngine
import com.indianservers.ai_stem.domain.interaction.ScreenPoint
import com.indianservers.ai_stem.domain.interaction.SnapKind
import com.indianservers.ai_stem.domain.interaction.SnapSettings
import com.indianservers.ai_stem.domain.interaction.SnapTarget
import com.indianservers.ai_stem.domain.interaction.SnappingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemediationInteractionTest {
    @Test
    fun pickingReturnsNearestVisibleObject() {
        val engine = PickingEngine()
        val far = item("far", PickShape.Point(ScreenPoint(20.0, 0.0), 30.0))
        val near = item("near", PickShape.Point(ScreenPoint(3.0, 0.0), 30.0))
        val hidden = item("hidden", PickShape.Point(ScreenPoint(0.0, 0.0), 30.0), visible = false)

        val result = engine.pick(ScreenPoint(0.0, 0.0), listOf(far, near, hidden))

        assertEquals("near", result.primary?.item?.id)
        assertFalse(result.candidates.any { it.item.id == "hidden" })
    }

    @Test
    fun pickingSupportsOverlapAndSelectionCycling() {
        val engine = PickingEngine()
        val a = item("a", PickShape.Segment(ScreenPoint(0.0, 0.0), ScreenPoint(100.0, 0.0), 12.0))
        val b = item("b", PickShape.Segment(ScreenPoint(0.0, 4.0), ScreenPoint(100.0, 4.0), 12.0))

        val result = engine.pick(ScreenPoint(50.0, 2.0), listOf(a, b))

        assertTrue(result.needsDisambiguation)
        assertEquals(2, result.candidates.size)
        assertEquals("b", engine.selectNext("a", result.candidates)?.item?.id)
    }

    @Test
    fun snappingPrefersExplicitTargetsThenGrid() {
        val engine = SnappingEngine()
        val settings = SnapSettings(distanceStep = 1.0, tolerance = 0.2)
        val target = SnapTarget(ScreenPoint(2.0, 2.0), SnapKind.Midpoint, priority = 10)

        val midpoint = engine.snapPoint(ScreenPoint(2.1, 2.05), settings, listOf(target))
        val grid = engine.snapPoint(ScreenPoint(3.05, 3.1), settings)

        assertEquals(SnapKind.Midpoint, midpoint.kind)
        assertEquals(ScreenPoint(2.0, 2.0), midpoint.point)
        assertEquals(SnapKind.Grid, grid.kind)
        assertEquals(ScreenPoint(3.0, 3.0), grid.point)
    }

    @Test
    fun continuousInteractionCommitsOneHistoryEntry() {
        val history = CoalescedInteractionHistory<Int>()
        history.begin("drag-1", "Move point", 0)
        repeat(50) { history.update(it + 1) }
        val entry = history.commit()

        assertEquals(1, history.undoCount)
        assertEquals(0, entry?.before)
        assertEquals(50, entry?.after)
        assertEquals(0, history.undo(50))
        assertEquals(50, history.redo(0))
    }

    @Test
    fun cancelRestoresInitialInteractionState() {
        val history = CoalescedInteractionHistory<String>()
        history.begin("scale", "Resize", "small")
        history.update("large")

        assertEquals("small", history.cancel())
        assertFalse(history.canUndo)
    }

    private fun item(id: String, shape: PickShape, visible: Boolean = true) =
        PickableItem(
            id = id,
            name = id,
            kind = PickableKind.GraphCurve,
            shape = shape,
            visible = visible
        )
}
