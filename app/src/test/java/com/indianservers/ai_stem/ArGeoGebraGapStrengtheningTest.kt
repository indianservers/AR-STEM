package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.ArAdvancedMathTools
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArGeoGebraGapStrengtheningTest {
    private val engine = ArMathEngine()

    @Test
    fun compilesParametricCurveForArTraceWorkflows() {
        val compiled = engine.compile("x = a*cos(t), y = b*sin(t)")

        assertTrue(compiled.isValid)
        assertEquals(GraphExpressionKind.Parametric2D, compiled.kind)
        assertTrue(compiled.parameters.map { it.symbol }.containsAll(listOf("a", "b")))
        assertTrue(engine.sample(compiled).curve.points.isNotEmpty())
    }

    @Test
    fun compilesImplicitRelationForApproximateArSampling() {
        val compiled = engine.compile("x^2 + y^2 = a^2")

        assertTrue(compiled.isValid)
        assertEquals(GraphExpressionKind.Implicit2D, compiled.kind)
        assertTrue(compiled.parameters.any { it.symbol == "a" })
    }

    @Test
    fun detectsInequalityRegionsForArShadingTools() {
        val compiled = engine.compile("y <= a*x + b")

        assertTrue(compiled.isValid)
        assertEquals(GraphExpressionKind.Inequality2D, compiled.kind)
    }

    @Test
    fun reportsAdvancedGeoGebraParityCapabilities() {
        val report = ArAdvancedMathTools.evaluate(
            equationKind = GraphExpressionKind.Parametric2D,
            hasAnalysis = true,
            constructionCount = 2,
            pickedPointCount = 3,
            exported = true
        )

        assertTrue(report.strengthScore >= 60)
        assertTrue(report.supportedTools.any { it.id == "parametric-orbit" })
        assertTrue(report.supportedTools.any { it.id == "proof-check" })
    }
}
