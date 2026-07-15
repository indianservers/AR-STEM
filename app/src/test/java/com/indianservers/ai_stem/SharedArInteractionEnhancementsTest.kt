package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.interaction.ArGameContext
import com.indianservers.ai_stem.feature.games.interaction.ArGestureAction
import com.indianservers.ai_stem.feature.games.interaction.ArGestureCommand
import com.indianservers.ai_stem.feature.games.interaction.ArObjectInspectorEngine
import com.indianservers.ai_stem.feature.games.interaction.ArObjectState
import com.indianservers.ai_stem.feature.games.interaction.CalibrationWizardInput
import com.indianservers.ai_stem.feature.games.interaction.CalibrationWizardStep
import com.indianservers.ai_stem.feature.games.interaction.CompactArHudInput
import com.indianservers.ai_stem.feature.games.interaction.CompactArHudReducer
import com.indianservers.ai_stem.feature.games.interaction.CompactHudTone
import com.indianservers.ai_stem.feature.games.interaction.CrossGameMissionTemplateRegistry
import com.indianservers.ai_stem.feature.games.interaction.SharedArGestureEngine
import com.indianservers.ai_stem.feature.games.interaction.SmartArSnapEngine
import com.indianservers.ai_stem.feature.games.interaction.SnapCandidate
import com.indianservers.ai_stem.feature.games.interaction.SnapRequest
import com.indianservers.ai_stem.feature.games.interaction.SnapTargetKind
import com.indianservers.ai_stem.feature.games.interaction.TopArEnhancementRegistry
import com.indianservers.ai_stem.feature.games.interaction.UniversalCalibrationWizard
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedArInteractionEnhancementsTest {
    @Test
    fun sharedGestureEngineMovesScalesLocksDuplicatesAndResetsObjects() {
        val engine = SharedArGestureEngine()
        val base = objectState()

        val moved = engine.apply(base, ArGestureCommand(ArGestureAction.GrabMove, translationMetres = Vector3Dto(1f, 0f, 0.5f))).objectState
        val scaled = engine.apply(moved, ArGestureCommand(ArGestureAction.Scale, scaleFactor = 2f)).objectState
        val locked = engine.apply(scaled, ArGestureCommand(ArGestureAction.LockToggle)).objectState
        val blocked = engine.apply(locked, ArGestureCommand(ArGestureAction.GrabMove, translationMetres = Vector3Dto(9f, 0f, 0f)))
        val duplicate = engine.apply(locked, ArGestureCommand(ArGestureAction.Duplicate)).duplicatedObject
        val reset = engine.apply(scaled, ArGestureCommand(ArGestureAction.Reset)).objectState

        assertEquals(1f, moved.transform.positionMetres.x)
        assertEquals(2f, scaled.transform.scale.x)
        assertTrue(locked.locked)
        assertFalse(blocked.changed)
        assertNotNull(duplicate)
        assertEquals(1f, reset.transform.scale.x)
    }

    @Test
    fun calibrationWizardCoversIndoorPaperAndOutdoorReadiness() {
        val wizard = UniversalCalibrationWizard()
        val indoor = wizard.evaluate(CalibrationWizardInput(ArGameContext.Indoor))
        val paper = wizard.evaluate(CalibrationWizardInput(ArGameContext.PaperGraph, imageLocked = true, scaleReferenceMetres = 0.2f, axesConfirmed = true, trackingScore = 90, lightScore = 90))
        val outdoor = wizard.evaluate(CalibrationWizardInput(ArGameContext.OutdoorGeo, buildingMeshFound = true, locationReady = true, scaleReferenceMetres = 10f, axesConfirmed = true, safetyBoundarySet = true, trackingScore = 80, lightScore = 70))

        assertEquals(CalibrationWizardStep.FindSurfaceOrMarker, indoor.step)
        assertTrue(paper.canStartGame)
        assertTrue(outdoor.canStartGame)
    }

    @Test
    fun objectInspectorSummarizesFormulaMeasurementsDependenciesAndWarnings() {
        val inspection = ArObjectInspectorEngine().inspect(
            objectState().copy(
                formulas = listOf("V = l x w x h"),
                dimensionsMetres = mapOf("length" to 2f, "width" to 1.5f),
                dependencies = setOf("axis-a"),
                hints = listOf("Use cubic metres.")
            )
        )
        val warning = ArObjectInspectorEngine().inspect(objectState().copy(formulas = emptyList(), dimensionsMetres = emptyMap()))

        assertTrue(inspection.formulaSummary.contains("V ="))
        assertTrue(inspection.measurementSummary.contains("length=2.00m"))
        assertTrue(inspection.dependencySummary.contains("axis-a"))
        assertTrue(warning.warnings.isNotEmpty())
    }

    @Test
    fun smartSnapPrioritizesNearHighValueTargetsAndFallsBackToGrid() {
        val engine = SmartArSnapEngine()
        val request = SnapRequest(Vector3Dto(0.11f, 0f, 0.1f), toleranceMetres = 0.2f)
        val result = engine.snap(
            request,
            listOf(
                SnapCandidate("grid-a", SnapTargetKind.GridLine, Vector3Dto(0.1f, 0f, 0.1f), priority = 1, label = "grid"),
                SnapCandidate("root-a", SnapTargetKind.FunctionPoint, Vector3Dto(0.15f, 0f, 0.15f), priority = 8, label = "root")
            )
        )
        val grid = engine.snap(SnapRequest(Vector3Dto(0.49f, 0f, 0.49f), toleranceMetres = 0.03f, gridStepMetres = 0.5f), emptyList())

        assertTrue(result.snapped)
        assertEquals("root-a", result.target?.candidateId)
        assertTrue(grid.snapped)
        assertEquals(0.5f, grid.positionMetres.x)
    }

    @Test
    fun crossGameMissionTemplatesCoverEveryGameAndInstantiatePlayablePlans() {
        assertEquals(5, TopArEnhancementRegistry.labels.size)
        GamesCatalog.games.forEach { game ->
            val templates = CrossGameMissionTemplateRegistry.templatesForGame(game.id)
            assertTrue("${game.id} needs shared templates", templates.isNotEmpty())
        }

        val plan = CrossGameMissionTemplateRegistry.instantiate(
            templateId = "build-solid",
            gameId = "geometry_architect_ar",
            variables = mapOf("solid" to "rectangular prism")
        )

        assertTrue(plan.prompt.contains("rectangular prism"))
        assertTrue(plan.arActions.contains(ArGestureAction.Scale))
        assertTrue(plan.validationChecks.any { it.startsWith("snap:") })
    }

    @Test
    fun compactHudKeepsSurfaceGuidanceShortAndExpandable() {
        val compact = CompactArHudReducer.reduce(
            CompactArHudInput(
                surfaceStatus = "Surface ready",
                primaryAction = "Tap to place",
                trackingStatus = "Tracking",
                warnings = listOf("Move slower"),
                details = listOf("Plane 1.2m", "Light 80", "Depth on", "HDR on")
            )
        )
        val expanded = CompactArHudReducer.reduce(
            CompactArHudInput(
                surfaceStatus = "Surface ready",
                primaryAction = "Tap to place",
                trackingStatus = "Tracking",
                details = listOf("Plane 1.2m", "Light 80", "Depth on", "HDR on"),
                expanded = true
            )
        )

        assertEquals(3, compact.chips.size)
        assertEquals(CompactHudTone.Warning, compact.tone)
        assertTrue(expanded.chips.size <= 6)
        assertEquals(CompactHudTone.Success, expanded.tone)
    }

    private fun objectState(): ArObjectState = ArObjectState(
        objectId = "object-1",
        gameId = "geometry_architect_ar",
        label = "Volume Block",
        transform = SharedTransform(positionMetres = Vector3Dto(0f, 0f, 0f))
    )
}
