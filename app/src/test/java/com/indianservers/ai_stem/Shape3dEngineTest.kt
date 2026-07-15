package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.scene.AnchorStrategy
import com.indianservers.ai_stem.domain.scene.Marker3dWorkspace
import com.indianservers.ai_stem.domain.scene.ObjectTransform
import com.indianservers.ai_stem.domain.scene.Shape3dAnchorMode
import com.indianservers.ai_stem.domain.scene.Shape3dCapability
import com.indianservers.ai_stem.domain.scene.Shape3dEngine
import com.indianservers.ai_stem.domain.scene.Shape3dPlacementRequest
import com.indianservers.ai_stem.domain.scene.Shape3dRenderQuality
import com.indianservers.ai_stem.domain.scene.Vector3Value
import com.indianservers.ai_stem.domain.scene.markerWorkspaceForA4Worksheet
import kotlin.math.abs
import kotlin.math.round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Shape3dEngineTest {
    @Test
    fun markerPlacementSnapsInsideUsableMarkerArea() {
        val workspace = Marker3dWorkspace(
            widthMeters = 0.24,
            heightMeters = 0.18,
            gridStepMeters = 0.03,
            safeInsetMeters = 0.02
        )

        val plan = Shape3dEngine.placementPlan(
            Shape3dPlacementRequest(
                definitionId = "cube",
                anchorMode = Shape3dAnchorMode.MarkerImage,
                markerWorkspace = workspace,
                normalizedX = 0.87,
                normalizedZ = -0.64,
                snapToGrid = true
            )
        )

        assertEquals(AnchorStrategy.SceneOrigin, plan.anchorStrategy)
        assertTrue(abs(plan.transform.position.x) <= workspace.usableHalfWidth)
        assertTrue(abs(plan.transform.position.z) <= workspace.usableHalfHeight)
        assertEquals(
            round(plan.transform.position.x / workspace.gridStepMeters) * workspace.gridStepMeters,
            plan.transform.position.x,
            0.00001
        )
        assertTrue(plan.transform.position.y > 0.0)
        assertTrue(plan.renderProfile.capabilities.contains(Shape3dCapability.VolumeLayers))
    }

    @Test
    fun metricsExposeBoundsAndPrimaryFormulaForSolids() {
        val metrics = Shape3dEngine.metricSummary("sphere")

        assertEquals(0.32, metrics.bounds.widthMeters, 0.0001)
        assertEquals(0.32, metrics.bounds.heightMeters, 0.0001)
        assertTrue(metrics.primaryLabel.startsWith("Volume:"))
        assertTrue((metrics.volumeCubicMeters ?: 0.0) > 0.0)
        assertTrue((metrics.surfaceAreaSquareMeters ?: 0.0) > 0.0)
    }

    @Test
    fun renderQualityControlsMeshBudget() {
        val draft = Shape3dEngine.renderProfile(
            type = com.indianservers.ai_stem.domain.mathematics.MathObjectType.Sphere,
            quality = Shape3dRenderQuality.Draft
        )
        val ultra = Shape3dEngine.renderProfile(
            type = com.indianservers.ai_stem.domain.mathematics.MathObjectType.Sphere,
            quality = Shape3dRenderQuality.Ultra
        )

        assertTrue(ultra.meshBudget.vertexCount > draft.meshBudget.vertexCount)
        assertTrue(ultra.meshBudget.triangleCount > draft.meshBudget.triangleCount)
        assertTrue(ultra.depthSorted)
    }

    @Test
    fun a4MarkerWorkspaceUsesPaperAspectRatioAndGrid() {
        val workspace = markerWorkspaceForA4Worksheet(widthMeters = 0.21)

        assertTrue(workspace.heightMeters > workspace.widthMeters)
        assertTrue(workspace.gridStepMeters > 0.0)
        val snapped = Shape3dEngine.snapToMarkerGrid(Vector3Value(0.043, 0.0, -0.044), workspace)
        assertFalse(snapped.x.isNaN())
        assertFalse(snapped.z.isNaN())
    }

    @Test
    fun transformValidationReportsOversizedOrOutOfMarkerTransforms() {
        val workspace = Marker3dWorkspace(widthMeters = 0.2, heightMeters = 0.2)
        val warnings = Shape3dEngine.validateTransform(
            ObjectTransform(
                position = Vector3Value(0.5, 0.0, 0.5),
                scale = Vector3Value(9.0, 1.0, 1.0)
            ),
            workspace
        )

        assertTrue(warnings.any { it.contains("outside marker width") })
        assertTrue(warnings.any { it.contains("outside marker height") })
        assertTrue(warnings.any { it.contains("too large") })
    }
}
