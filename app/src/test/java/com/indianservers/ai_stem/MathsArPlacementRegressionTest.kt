package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.ar.CommonArSurfaceEngine
import com.indianservers.ai_stem.core.ar.CommonArSurfaceHitKind
import com.indianservers.ai_stem.core.ar.CommonArSurfaceMode
import com.indianservers.ai_stem.core.ar.CommonArSurfaceObservation
import com.indianservers.ai_stem.core.ar.CommonArSurfaceOrientation
import com.indianservers.ai_stem.core.ar.CommonArSurfacePose
import com.indianservers.ai_stem.core.ar.CommonArSurfaceQuality
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.feature.arviewer.ArViewerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathsArPlacementRegressionTest {
    @Test
    fun firstObjectIsPlacedAtAnchorLocalOriginNotWorldHitCoordinates() {
        val viewModel = ArViewerViewModel()
        viewModel.selectArEngineMode(ArEngineMode.Indoor)

        viewModel.recordPlacementPoint(1.4f, 0.0f, -2.2f)
        viewModel.onAnchorEstablished()

        val objectState = viewModel.uiState.value.mathScene.primarySelectedObject
        requireNotNull(objectState)
        assertEquals(0.0, objectState.transform.position.x, 0.0001)
        assertEquals(0.0, objectState.transform.position.y, 0.0001)
        assertEquals(0.0, objectState.transform.position.z, 0.0001)
        assertEquals(1.4f, viewModel.uiState.value.lastPlacementPoint?.x)
    }

    @Test
    fun markerlessAcceptsEarlySmallTrackedPlaneInsteadOfWaitingForLargeSurface() {
        val state = CommonArSurfaceEngine.evaluate(
            listOf(
                CommonArSurfaceObservation(
                    hitKind = CommonArSurfaceHitKind.Plane,
                    pose = CommonArSurfacePose(0f, 0f, -1f),
                    planeWidthMetres = 0.22f,
                    planeHeightMetres = 0.2f,
                    distanceMetres = 1.1f,
                    orientation = CommonArSurfaceOrientation.HorizontalUp,
                    polygonContainsHit = true
                )
            ),
            CommonArSurfaceMode.Markerless
        )

        assertEquals(CommonArSurfaceQuality.Good, state.quality)
        assertTrue(state.canPlace)
    }
}
