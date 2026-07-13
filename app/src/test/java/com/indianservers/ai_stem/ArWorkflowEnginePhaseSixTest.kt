package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.scene.ArSceneSharePackage
import com.indianservers.ai_stem.domain.scene.ArWorkflowEngine
import com.indianservers.ai_stem.domain.scene.ArWorkflowProgress
import com.indianservers.ai_stem.domain.scene.ArWorkflowSignal
import com.indianservers.ai_stem.domain.scene.ArWorkflowTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArWorkflowEnginePhaseSixTest {
    private val engine = ArWorkflowEngine()

    @Test
    fun evaluatesWorkflowFromLiveArSignals() {
        val template = ArWorkflowTemplates.templates.first { it.id == "surface-masterclass" }
        val progress = ArWorkflowProgress(templateId = template.id)
        val signal = strongSignal()

        val evaluation = engine.evaluate(template, progress, signal)

        assertTrue(evaluation.completionPercent >= 70)
        assertTrue(evaluation.readinessScore >= 80)
        assertTrue("Equation ready" in evaluation.badges)
        assertTrue("Measured" in evaluation.badges)
        assertTrue(evaluation.warnings.isEmpty())
    }

    @Test
    fun completingCurrentStepAdvancesProgress() {
        val template = ArWorkflowTemplates.templates.first { it.id == "paper-graph-to-3d" }
        val progress = ArWorkflowProgress(templateId = template.id)

        val completed = engine.completeCurrent(progress, template)

        assertEquals(setOf("scan-paper"), completed.completedStepIds)
        assertEquals(1, completed.currentStepIndex)
    }

    @Test
    fun exportsShareableActivityPack() {
        val template = ArWorkflowTemplates.templates.first { it.id == "outdoor-building-lab" }
        val progress = ArWorkflowProgress(templateId = template.id, completedStepIds = setOf("scan-geo", "place"))
        val evaluation = engine.evaluate(template, progress, strongSignal(mode = "OutdoorGeospatialMath"))
        val scenePackage = ArSceneSharePackage(
            fileName = "Outdoor.aistem-ar.json",
            mimeType = "application/vnd.aistem.ar+json",
            payload = "{}",
            summary = "Outdoor scene"
        )

        val activity = engine.exportActivity(template, progress, evaluation, scenePackage)

        assertEquals("Outdoor-Building-Lab.aistem-activity.json", activity.fileName)
        assertEquals("application/vnd.aistem.activity+json", activity.mimeType)
        assertTrue(activity.payload.contains("\"activityId\":\"outdoor-building-lab\""))
        assertTrue(activity.payload.contains("Outdoor.aistem-ar.json"))
    }

    private fun strongSignal(mode: String = "Indoor") = ArWorkflowSignal(
        mode = mode,
        hasPlacedObject = true,
        placementScore = 92,
        paperCalibrated = mode == "PaperGraph",
        equationValid = true,
        hasAnalysis = true,
        pickedPointCount = 2,
        rulerAnchorCount = 2,
        constructionObjectCount = 1,
        anchorSaved = true,
        exportReady = true,
        depthEnabled = true
    )
}
