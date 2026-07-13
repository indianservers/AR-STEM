package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.ar.ArSensorState
import com.indianservers.ai_stem.core.ar.ArStabilityEngine
import com.indianservers.ai_stem.feature.arviewer.ArRecoveryMode
import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArStabilityEngineTest {
    private val engine = ArStabilityEngine()

    @Test
    fun trackedPlaneIsExcellentPlacement() {
        val state = engine.evaluate(
            ArSensorState(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                hasPlacedObject = false,
                trackingMessage = "ready"
            )
        )

        assertEquals(ArRecoveryMode.Normal, state.recoveryMode)
        assertEquals(PlacementQuality.Excellent, state.placementQuality)
        assertTrue(state.canPlace)
        assertFalse(state.canRePlace)
    }

    @Test
    fun limitedCameraAllowsReplacementWhenObjectExists() {
        val state = engine.evaluate(
            ArSensorState(
                cameraTracking = TrackingStatus.Limited,
                anchorTracking = TrackingStatus.Tracking,
                placementHitKind = PlacementHitKind.Instant,
                hasPlacedObject = true,
                trackingMessage = "move slowly"
            )
        )

        assertEquals(ArRecoveryMode.TrackingLimited, state.recoveryMode)
        assertEquals(PlacementQuality.Weak, state.placementQuality)
        assertTrue(state.canPlace)
        assertTrue(state.canRePlace)
    }

    @Test
    fun pausedAnchorRequiresRePlacement() {
        val state = engine.evaluate(
            ArSensorState(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Paused,
                placementHitKind = PlacementHitKind.None,
                hasPlacedObject = true,
                trackingMessage = "anchor lost"
            )
        )

        assertEquals(ArRecoveryMode.RePlacementRequired, state.recoveryMode)
        assertEquals(PlacementQuality.Lost, state.placementQuality)
        assertFalse(state.canPlace)
        assertTrue(state.canRePlace)
    }
}
