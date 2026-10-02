package com.indianservers.ai_stem.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class GraphWorkspaceAccuracyTest {
    private val engine = GraphInsightsEngine()

    @Test
    fun requestedQuadraticHasExactVertexAndRoots() {
        val polynomial = requireNotNull(engine.polynomial("y = x^2 - 2x - 1"))
        val roots = engine.realRoots(polynomial)

        assertEquals(2.0, polynomial.degree.toDouble(), 0.0)
        assertEquals(1.0 - sqrt(2.0), roots[0], 1e-8)
        assertEquals(1.0 + sqrt(2.0), roots[1], 1e-8)
        assertEquals(-2.0, polynomial.valueAt(1.0), 1e-9)
    }

    @Test
    fun requestedLineHasCorrectIntercepts() {
        val expression = graph("line", "y = 2x + 3")
        val insight = requireNotNull(engine.analyze(expression))
        val xIntercept = insight.featurePoints.first { it.kind == "x-intercept" }.point
        val yIntercept = insight.featurePoints.first { it.kind == "y-intercept" }.point

        assertEquals(-1.5, xIntercept.x, 1e-9)
        assertEquals(3.0, yIntercept.y, 1e-9)
    }

    @Test
    fun requestedLineAndQuadraticIntersectionsAreCalculated() {
        val intersections = engine.intersections(
            graph("quadratic", "y = x^2 - 2x - 1"),
            graph("line", "y = 2x + 3")
        )

        assertEquals(2, intersections.size)
        assertEquals(2.0 - 2.0 * sqrt(2.0), intersections[0].point.x, 1e-8)
        assertEquals(1.3431457505, intersections[0].point.y, 1e-8)
        assertEquals(2.0 + 2.0 * sqrt(2.0), intersections[1].point.x, 1e-8)
        assertEquals(12.6568542495, intersections[1].point.y, 1e-8)
    }

    @Test
    fun reciprocalSamplingDoesNotBridgeAsymptote() {
        val expression = graph("reciprocal", "y = 1/x")
        val curves = GraphSampler().sampleExpression(expression, GraphViewport(-4.0, 4.0, -4.0, 4.0), GraphQualityPreset.HighQuality)

        assertTrue(curves.size >= 2)
        assertTrue(curves.none { curve -> curve.points.any { it.x < 0.0 } && curve.points.any { it.x > 0.0 } })
    }

    private fun graph(id: String, source: String) = GraphExpression(
        id = id,
        kind = GraphExpressionKind.Explicit2D,
        source = source,
        displayName = id,
        style = GraphStyle(0xFF00CFE8)
    )
}
