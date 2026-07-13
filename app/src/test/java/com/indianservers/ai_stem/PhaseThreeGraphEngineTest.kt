package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.GraphAnalysisEngine
import com.indianservers.ai_stem.domain.graph.GraphExpression
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import com.indianservers.ai_stem.domain.graph.GraphSampler
import com.indianservers.ai_stem.domain.graph.GraphStyle
import com.indianservers.ai_stem.domain.graph.GraphViewport
import com.indianservers.ai_stem.domain.graph.MathExpressionEvaluator
import com.indianservers.ai_stem.domain.graph.MathExpressionParser
import com.indianservers.ai_stem.domain.graph.ParseOutcome
import com.indianservers.ai_stem.domain.graph.defaultVariables
import com.indianservers.ai_stem.domain.graph.normalizeEquationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PhaseThreeGraphEngineTest {
    private val parser = MathExpressionParser()
    private val evaluator = MathExpressionEvaluator()

    @Test
    fun parserHonorsPrecedenceAndPowerAssociativity() {
        assertNearly(14.0, eval("2 + 3 * 4"))
        assertNearly(512.0, eval("2^3^2"))
        assertNearly(-4.0, eval("-2^2"))
        assertNearly(4.0, eval("(-2)^2"))
    }

    @Test
    fun parserNormalizesUnicodeAndImplicitMultiplication() {
        assertEquals("y = x^2 + pi", normalizeEquationSource("y = x² + π"))
        assertNearly(6.0, eval("2x", mapOf("x" to 3.0)))
        assertNearly(0.0, eval("sin(pi)"))
    }

    @Test
    fun evaluatorSupportsCoreFunctionsAndSafeErrors() {
        assertNearly(5.0, eval("sqrt(9) + abs(-2)"))
        assertNearly(10.0, eval("combinations(5,2)"))
        assertFalse(evaluator.evaluate(requireAst("1 / 0"), defaultVariables()).isSuccess)
    }

    @Test
    fun graphSamplerProducesExplicitPolarAndSurfaceData() {
        val sampler = GraphSampler()
        val explicit = sampler.parseExpression(expr("y = x^2", GraphExpressionKind.Explicit2D))
        val curve = sampler.sampleExpression(explicit, GraphViewport(xMin = -2.0, xMax = 2.0, yMin = -1.0, yMax = 5.0), GraphQualityPreset.BatterySaver)
        assertTrue(curve.first().points.isNotEmpty())

        val polar = sampler.parseExpression(expr("r = 2 cos(theta)", GraphExpressionKind.Polar))
        assertTrue(sampler.sampleExpression(polar, GraphViewport(), GraphQualityPreset.BatterySaver).first().points.isNotEmpty())

        val surface = sampler.parseExpression(expr("z = sin(x) * cos(y)", GraphExpressionKind.ExplicitSurface3D))
        assertTrue(requireNotNull(sampler.sampleSurface(surface, GraphViewport(), GraphQualityPreset.BatterySaver)).vertices.isNotEmpty())
    }

    @Test
    fun analysisFindsRootsDerivativeAndIntegral() {
        val analysis = GraphAnalysisEngine()
        val roots = analysis.roots("y = x^2 - 1", GraphViewport(xMin = -2.0, xMax = 2.0))
        assertTrue(roots.any { kotlin.math.abs(it.x + 1.0) < 0.05 })
        assertTrue(roots.any { kotlin.math.abs(it.x - 1.0) < 0.05 })
        assertNearly(4.0, analysis.derivative("y = x^2", 2.0), 0.001)
        assertNearly(2.0, analysis.definiteIntegral("y = x", 0.0, 2.0), 0.001)
    }

    @Test
    fun invalidFormulaReturnsUserSafeParseError() {
        val parsed = parser.parse("sin(x")
        assertTrue(parsed is ParseOutcome.Failure)
        assertTrue((parsed as ParseOutcome.Failure).message.contains("bracket", ignoreCase = true))
    }

    private fun eval(source: String, variables: Map<String, Double> = emptyMap()): Double {
        val ast = requireAst(source)
        return requireNotNull(evaluator.evaluate(ast, defaultVariables() + variables).value)
    }

    private fun requireAst(source: String) =
        (parser.parse(source) as ParseOutcome.Success).expression

    private fun expr(source: String, kind: GraphExpressionKind) =
        GraphExpression("test-${source.hashCode()}", kind, source, "Test", style = GraphStyle(0xFF000000))

    private fun assertNearly(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }
}
