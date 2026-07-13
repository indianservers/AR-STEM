package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.ar.ArGuidanceEngine
import com.indianservers.ai_stem.core.ar.ArGuidanceInput
import com.indianservers.ai_stem.core.ar.ArGuidanceSeverity
import com.indianservers.ai_stem.core.ar.ArPlacementMode
import com.indianservers.ai_stem.feature.arviewer.ArRecoveryMode
import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArGuidanceEngineTest {
    private val engine = ArGuidanceEngine()

    @Test
    fun anchorLossRequiresRePlacement() {
        val result = engine.guide(input(recoveryMode = ArRecoveryMode.RePlacementRequired, hasPlacedObject = true))

        assertEquals(ArGuidanceSeverity.ActionRequired, result.severity)
        assertEquals("Re-place the object", result.headline)
        assertFalse(result.canAutoRecover)
    }

    @Test
    fun lowLightGuidesUserBeforePlacement() {
        val result = engine.guide(input(lightStable = false))

        assertEquals("More light needed", result.headline)
        assertEquals(ArGuidanceSeverity.Warning, result.severity)
    }

    @Test
    fun fastMotionGuidesUserToHoldSteady() {
        val result = engine.guide(input(motionStable = false))

        assertEquals("Hold steady", result.headline)
        assertTrue(result.canAutoRecover)
    }

    @Test
    fun excellentPlaneRecommendsFloorMode() {
        val result = engine.guide(input(placementQuality = PlacementQuality.Excellent, placementScore = 92))

        assertEquals("Excellent placement", result.headline)
        assertEquals(ArPlacementMode.Floor, result.recommendedMode)
    }

    @Test
    fun instantPlacementWarnsItIsApproximate() {
        val result = engine.guide(
            input(
                placementHitKind = PlacementHitKind.Instant,
                placementQuality = PlacementQuality.Weak,
                placementScore = 45,
                shouldPreferPlane = true
            )
        )

        assertEquals("Approximate placement", result.headline)
        assertEquals(ArPlacementMode.Instant, result.recommendedMode)
    }

    private fun input(
        cameraTracking: TrackingStatus = TrackingStatus.Tracking,
        anchorTracking: TrackingStatus = TrackingStatus.Unknown,
        recoveryMode: ArRecoveryMode = ArRecoveryMode.Normal,
        placementQuality: PlacementQuality = PlacementQuality.Good,
        placementHitKind: PlacementHitKind = PlacementHitKind.Plane,
        placementScore: Int = 70,
        motionStable: Boolean = true,
        lightStable: Boolean = true,
        shouldPreferPlane: Boolean = false,
        hasPlacedObject: Boolean = false,
        hasValidPlacementHit: Boolean = true
    ) = ArGuidanceInput(
        cameraTracking = cameraTracking,
        anchorTracking = anchorTracking,
        recoveryMode = recoveryMode,
        placementQuality = placementQuality,
        placementHitKind = placementHitKind,
        placementScore = placementScore,
        motionStable = motionStable,
        lightStable = lightStable,
        shouldPreferPlane = shouldPreferPlane,
        hasPlacedObject = hasPlacedObject,
        hasValidPlacementHit = hasValidPlacementHit
    )
}
