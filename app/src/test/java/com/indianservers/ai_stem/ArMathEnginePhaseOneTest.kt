package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class ArMathEnginePhaseOneTest {
    private val engine = ArMathEngine()

    @Test
    fun compilesParameterizedEquationAndCreatesArSliders() {
        val compiled = engine.compile("y = a * sin(b*x + c) + d")

        assertTrue(compiled.isValid)
        assertEquals(GraphExpressionKind.Explicit2D, compiled.kind)
        assertEquals(listOf("a", "b", "c", "d"), compiled.parameters.map { it.symbol })
    }

    @Test
    fun evaluatesArEquationWithSliderValues() {
        val base = engine.compile("y = a*sin(b*x+c)+d")
        val sliders = base.parameters.map {
            when (it.symbol) {
                "a" -> it.copy(value = 2.0)
                "b" -> it.copy(value = 0.5)
                "c" -> it.copy(value = 0.0)
                "d" -> it.copy(value = 1.0)
                else -> it
            }
        }
        val compiled = engine.compile(base.source, sliders)

        assertNearly(3.0, engine.evaluate2d(compiled, PI))
    }

    @Test
    fun evaluatesExplicitSurfaceForArMeshes() {
        val compiled = engine.compile("z = sin(x) * cos(y)")

        assertEquals(GraphExpressionKind.ExplicitSurface3D, compiled.kind)
        assertNearly(1.0, engine.evaluate3d(compiled, PI / 2.0, 0.0))
    }

    @Test
    fun supportsScientificNotationAndExtendedFunctions() {
        val compiled = engine.compile("y = clamp(1e2 * x, -3, 3)")

        assertTrue(compiled.isValid)
        assertNearly(3.0, engine.evaluate2d(compiled, 1.0))
    }

    @Test
    fun samplesCurveAndSurfaceForArQualityPresets() {
        val curve = engine.compile(
            source = "y = x^2 - 1",
            domain = ArGraphDomain(xMin = -2.0, xMax = 2.0, yMin = -2.0, yMax = 3.0),
            qualityPreset = GraphQualityPreset.BatterySaver
        )
        assertTrue(engine.sample(curve).curve.points.isNotEmpty())

        val surface = engine.compile(
            source = "z = x^2 + y^2",
            domain = ArGraphDomain(xMin = -1.0, xMax = 1.0, yMin = -1.0, yMax = 1.0),
            qualityPreset = GraphQualityPreset.BatterySaver
        )
        assertTrue(requireNotNull(engine.sample(surface).surface).vertices.isNotEmpty())
    }

    private fun assertNearly(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }
}
