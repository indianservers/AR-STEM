package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.scene.ArDepthOcclusionMode
import com.indianservers.ai_stem.domain.scene.ArPerformanceProfile
import com.indianservers.ai_stem.domain.scene.ArSceneProductionSettings
import com.indianservers.ai_stem.domain.scene.ArSceneShareExporter
import com.indianservers.ai_stem.domain.scene.ArSceneTemplates
import com.indianservers.ai_stem.domain.scene.MathScene
import com.indianservers.ai_stem.domain.scene.PersistentAnchorKind
import com.indianservers.ai_stem.domain.scene.PersistentAnchorRecord
import com.indianservers.ai_stem.domain.scene.Vector3Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArSceneProductionPhaseFiveTest {
    @Test
    fun exportsSceneWithPersistentAnchorAndProductionSettings() {
        val scene = MathScene(
            id = "scene-phase-five",
            name = "Phase 5 Surface Lab",
            persistentAnchor = PersistentAnchorRecord(
                kind = PersistentAnchorKind.Plane,
                engineMode = "SurfacePlacement",
                label = "Surface plane anchor",
                worldPosition = Vector3Value(1.0, 0.2, -0.4)
            ),
            arProductionSettings = ArSceneProductionSettings(
                depthMode = ArDepthOcclusionMode.DepthTest,
                performanceProfile = ArPerformanceProfile.HighQuality,
                meshDensity = 0.9f,
                maxSceneObjects = 64,
                screenshotReady = true
            )
        )

        val share = ArSceneShareExporter().export(
            scene = scene,
            equation = "z = sin(x) * cos(y)",
            comparisonEquation = "y = x",
            constructionCount = 3,
            graphQuality = "High quality",
            colorMap = "Curvature"
        )

        assertEquals("Phase-5-Surface-Lab.aistem-ar.json", share.fileName)
        assertEquals("application/vnd.aistem.ar+json", share.mimeType)
        assertTrue(share.payload.contains("\"sceneId\":\"scene-phase-five\""))
        assertTrue(share.payload.contains("\"depth\":\"DepthTest\""))
        assertTrue(share.payload.contains("\"performance\":\"HighQuality\""))
        assertTrue(share.payload.contains("\"kind\":\"Plane\""))
        assertTrue(share.payload.contains("\"constructions\":3"))
    }

    @Test
    fun templatesCoverMarkerlessPaperOutdoorAndVectorWorkflows() {
        val ids = ArSceneTemplates.templates.map { it.id }.toSet()

        assertTrue("paper-graph-lab" in ids)
        assertTrue("surface-explorer" in ids)
        assertTrue("volume-builder" in ids)
        assertTrue("outdoor-building-geometry" in ids)
        assertTrue("vector-field-lab" in ids)
        assertTrue(ArSceneTemplates.templates.any { it.depthMode == ArDepthOcclusionMode.GeospatialDepth })
        assertTrue(ArSceneTemplates.templates.any { it.performanceProfile == ArPerformanceProfile.HighQuality })
    }
}
