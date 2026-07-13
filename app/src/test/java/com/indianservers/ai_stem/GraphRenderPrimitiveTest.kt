package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.GraphCurve
import com.indianservers.ai_stem.domain.graph.GraphExpression
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphPoint
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitiveBuilder
import com.indianservers.ai_stem.domain.graph.GraphStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphRenderPrimitiveTest {
    @Test
    fun builderCreatesRendererReadyPolylinesWithBoundsAndPickingMetadata() {
        val expression = GraphExpression(
            id = "expr-line",
            kind = GraphExpressionKind.Explicit2D,
            source = "y = x",
            displayName = "Line",
            style = GraphStyle(0xFF000000)
        )
        val primitive = GraphRenderPrimitiveBuilder()
            .fromCurves(
                curves = listOf(GraphCurve("expr-line", listOf(GraphPoint(-1.0, -2.0), GraphPoint(3.0, 4.0)))),
                expressions = listOf(expression),
                updateVersion = 42L
            )
            .single() as GraphRenderPrimitive.Polyline

        assertEquals("polyline-expr-line", primitive.id)
        assertEquals("expr-line", primitive.sourceExpressionId)
        assertEquals("Line", primitive.picking.label)
        assertEquals(42L, primitive.updateVersion)
        assertEquals(-1.0, primitive.bounds.minX, 0.0)
        assertEquals(3.0, primitive.bounds.maxX, 0.0)
        assertEquals(-2.0, primitive.bounds.minY, 0.0)
        assertEquals(4.0, primitive.bounds.maxY, 0.0)
        assertTrue(primitive.picking.pickable)
    }

    @Test
    fun builderSkipsDegenerateCurves() {
        val primitives = GraphRenderPrimitiveBuilder()
            .fromCurves(
                curves = listOf(GraphCurve("point-only", listOf(GraphPoint(1.0, 1.0)))),
                expressions = emptyList(),
                updateVersion = 1L
            )

        assertTrue(primitives.isEmpty())
    }
}
