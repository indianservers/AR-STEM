package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.model.StemSubject
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathematicsCatalogue
import com.indianservers.ai_stem.domain.mathematics.SineCurveSampler
import com.indianservers.ai_stem.domain.mathematics.clampScale
import com.indianservers.ai_stem.domain.mathematics.normalizeRotationDegrees
import com.indianservers.ai_stem.feature.arviewer.ArSessionStatus
import com.indianservers.ai_stem.feature.arviewer.ArViewerUiState
import com.indianservers.ai_stem.feature.arviewer.ObjectInteractionCommand
import com.indianservers.ai_stem.feature.arviewer.PlacedMathObject
import com.indianservers.ai_stem.feature.arviewer.reduceInteraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PhaseOneDomainTest {
    @Test
    fun onlyMathematicsIsEnabledInPhaseOne() {
        assertTrue(StemSubject.Mathematics.enabled)
        assertFalse(StemSubject.Physics.enabled)
        assertFalse(StemSubject.Chemistry.enabled)
        assertFalse(StemSubject.Biology.enabled)
    }

    @Test
    fun catalogueContainsExactlyTheThreePhaseOneObjects() {
        assertEquals(
            listOf(MathObjectType.CoordinatePlane, MathObjectType.Cube, MathObjectType.SineCurve),
            MathematicsCatalogue.phaseOneObjects.map { it.type }
        )
    }

    @Test
    fun sineSamplerHasCorrectBoundaryValues() {
        val samples = SineCurveSampler.sample()
        assertNearly(0f, samples.first().y)
        assertNearly(0f, samples.last().y)
        listOf(-2 * PI, -PI, 0.0, PI / 2.0, PI, 3.0 * PI / 2.0, 2.0 * PI).forEach { x ->
            assertNearly(sin(x).toFloat(), samples.closestTo(x.toFloat()).y)
        }
    }

    @Test
    fun transformMathIsClampedAndNormalized() {
        assertEquals(0.25f, clampScale(-10f))
        assertEquals(4f, clampScale(10f))
        assertEquals(1f, clampScale(Float.NaN))
        assertEquals(350f, normalizeRotationDegrees(-10f))
        assertEquals(5f, normalizeRotationDegrees(365f))
    }

    @Test
    fun interactionReducerResetsDeletesAndTransformsPlacedObject() {
        val initial = ArViewerUiState(
            placedObject = PlacedMathObject("active", MathObjectType.Cube, rotationDegrees = 350f, scaleFactor = 1f),
            sessionStatus = ArSessionStatus.ObjectSelected
        )
        val transformed = reduceInteraction(
            reduceInteraction(initial, ObjectInteractionCommand.Rotate(30f)),
            ObjectInteractionCommand.Scale(10f)
        )
        val transformedObject = requireNotNull(transformed.placedObject)
        assertEquals(20f, transformedObject.rotationDegrees)
        assertEquals(4f, transformedObject.scaleFactor)

        val reset = reduceInteraction(transformed, ObjectInteractionCommand.ResetTransform)
        val resetObject = requireNotNull(reset.placedObject)
        assertEquals(0f, resetObject.rotationDegrees)
        assertEquals(1f, resetObject.scaleFactor)

        val deleted = reduceInteraction(reset, ObjectInteractionCommand.DeleteSelected)
        assertEquals(null, deleted.placedObject)
        assertEquals(ArSessionStatus.Scanning, deleted.sessionStatus)
    }

    private fun List<com.indianservers.ai_stem.domain.mathematics.CurveSample>.closestTo(x: Float) =
        minBy { abs(it.x - x) }

    private fun assertNearly(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }
}
