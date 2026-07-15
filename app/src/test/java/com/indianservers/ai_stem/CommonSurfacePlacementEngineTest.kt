package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceHitKind
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceObservation
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfacePlacementEngine
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceQuality
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceTracking
import com.indianservers.ai_stem.feature.games.spatial.LocalPoseDto
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SurfaceOrientation
import com.indianservers.ai_stem.feature.games.spatial.SurfacePlacementState
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonSurfacePlacementEngineTest {
    @Test
    fun acceptsOnlyRealTrackedPlaneInsidePolygonWithEnoughExtent() {
        val state = CommonSurfacePlacementEngine.evaluate(
            listOf(
                observation(CommonSurfaceHitKind.Plane, width = 1.4f, height = 1.2f, polygon = true, distance = 1.1f)
            )
        )

        assertEquals(CommonSurfaceQuality.Excellent, state.quality)
        assertEquals(SurfacePlacementState.ValidPlaneFound, state.placementState)
        assertTrue(state.canPlace)
        assertEquals("Tap to place", state.primaryAction)
    }

    @Test
    fun rejectsInstantPreviewAndFeaturePointsForFinalPlacement() {
        val instant = CommonSurfacePlacementEngine.evaluate(
            listOf(observation(CommonSurfaceHitKind.InstantPreview, width = 2f, height = 2f, polygon = true))
        )
        val feature = CommonSurfacePlacementEngine.evaluate(
            listOf(observation(CommonSurfaceHitKind.FeaturePoint, width = 0f, height = 0f, polygon = true))
        )

        assertEquals(CommonSurfaceQuality.PreviewOnly, instant.quality)
        assertFalse(instant.canPlace)
        assertEquals(CommonSurfaceQuality.Weak, feature.quality)
        assertFalse(feature.canPlace)
    }

    @Test
    fun lostTrackingProducesCompactHoldStillState() {
        val lost = CommonSurfacePlacementEngine.evaluate(
            listOf(observation(CommonSurfaceHitKind.Plane, tracking = CommonSurfaceTracking.Paused))
        )

        assertEquals(SurfacePlacementState.LostTracking, lost.placementState)
        assertFalse(lost.canPlace)
        assertEquals("Hold still", lost.primaryAction)
    }

    private fun observation(
        kind: CommonSurfaceHitKind,
        width: Float = 1f,
        height: Float = 1f,
        polygon: Boolean = true,
        distance: Float = 1f,
        tracking: CommonSurfaceTracking = CommonSurfaceTracking.Tracking
    ): CommonSurfaceObservation = CommonSurfaceObservation(
        hitKind = kind,
        pose = LocalPoseDto(Vector3Dto(0f, 0f, -distance), QuaternionDto(0f, 0f, 0f, 1f)),
        planeWidthMetres = width,
        planeHeightMetres = height,
        distanceMetres = distance,
        orientation = SurfaceOrientation.HorizontalUp,
        polygonContainsHit = polygon,
        tracking = tracking
    )
}
