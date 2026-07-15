package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.ar.CommonArHudTone
import com.indianservers.ai_stem.core.ar.CommonArSurfaceEngine
import com.indianservers.ai_stem.core.ar.CommonArSurfaceHitKind
import com.indianservers.ai_stem.core.ar.CommonArSurfaceMode
import com.indianservers.ai_stem.core.ar.CommonArSurfaceObservation
import com.indianservers.ai_stem.core.ar.CommonArSurfaceOrientation
import com.indianservers.ai_stem.core.ar.CommonArSurfacePose
import com.indianservers.ai_stem.core.ar.CommonArSurfaceQuality
import com.indianservers.ai_stem.core.ar.CommonArSurfaceTracking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonArSurfaceEngineTest {
    @Test
    fun markerlessAndMathHubAcceptRealPlanesOnly() {
        val state = CommonArSurfaceEngine.evaluate(
            listOf(plane(width = 1.2f, height = 1.1f, polygon = true)),
            CommonArSurfaceMode.Markerless
        )

        assertEquals(CommonArSurfaceQuality.Excellent, state.quality)
        assertEquals(CommonArSurfaceHitKind.Plane, state.hitKind)
        assertTrue(state.canPlace)
    }

    @Test
    fun surfaceModeRejectsDepthFeatureAndInstantFallbacks() {
        val markerlessDepth = CommonArSurfaceEngine.evaluate(listOf(obs(CommonArSurfaceHitKind.DepthPoint)), CommonArSurfaceMode.Markerless)
        val depth = CommonArSurfaceEngine.evaluate(listOf(obs(CommonArSurfaceHitKind.DepthPoint)), CommonArSurfaceMode.SurfaceOnly)
        val instant = CommonArSurfaceEngine.evaluate(listOf(obs(CommonArSurfaceHitKind.InstantPreview)), CommonArSurfaceMode.SurfaceOnly)

        assertFalse(markerlessDepth.canPlace)
        assertEquals(CommonArSurfaceQuality.Searching, markerlessDepth.quality)
        assertFalse(depth.canPlace)
        assertFalse(instant.canPlace)
        assertEquals(CommonArSurfaceQuality.Searching, depth.quality)
    }

    @Test
    fun airModeAllowsFeaturePointButKeepsInstantAsPreviewOnly() {
        val feature = CommonArSurfaceEngine.evaluate(listOf(obs(CommonArSurfaceHitKind.FeaturePoint)), CommonArSurfaceMode.AirPlacement)
        val instant = CommonArSurfaceEngine.evaluate(listOf(obs(CommonArSurfaceHitKind.InstantPreview)), CommonArSurfaceMode.AirPlacement)

        assertTrue(feature.canPlace)
        assertEquals(CommonArSurfaceQuality.Weak, feature.quality)
        assertFalse(instant.canPlace)
        assertEquals(CommonArSurfaceQuality.PreviewOnly, instant.quality)
    }

    @Test
    fun compactHudLimitsTextForMathsArHub() {
        val surface = CommonArSurfaceEngine.evaluate(listOf(plane(width = 1.0f, height = 1.0f, polygon = true)), CommonArSurfaceMode.Markerless)
        val compact = CommonArSurfaceEngine.hud(surface, trackingLabel = "Tracking", expanded = false)
        val expanded = CommonArSurfaceEngine.hud(surface, trackingLabel = "Tracking", expanded = true)

        assertEquals(CommonArHudTone.Success, compact.tone)
        assertTrue(compact.chips.size <= 2)
        assertTrue(expanded.chips.size <= 5)
    }

    @Test
    fun trackingPausedBlocksPlacement() {
        val state = CommonArSurfaceEngine.evaluate(
            listOf(plane(width = 1f, height = 1f, polygon = true).copy(tracking = CommonArSurfaceTracking.Paused)),
            CommonArSurfaceMode.Markerless
        )

        assertEquals(CommonArSurfaceQuality.Lost, state.quality)
        assertFalse(state.canPlace)
    }

    private fun obs(kind: CommonArSurfaceHitKind): CommonArSurfaceObservation =
        CommonArSurfaceObservation(kind, CommonArSurfacePose(0f, 0f, -1f), polygonContainsHit = true)

    private fun plane(width: Float, height: Float, polygon: Boolean): CommonArSurfaceObservation =
        CommonArSurfaceObservation(
            hitKind = CommonArSurfaceHitKind.Plane,
            pose = CommonArSurfacePose(0f, 0f, -1f),
            planeWidthMetres = width,
            planeHeightMetres = height,
            distanceMetres = 1f,
            orientation = CommonArSurfaceOrientation.HorizontalUp,
            polygonContainsHit = polygon
        )
}
