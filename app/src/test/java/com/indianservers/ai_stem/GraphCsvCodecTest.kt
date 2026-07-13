package com.indianservers.ai_stem

import com.indianservers.ai_stem.data.graph.CsvParseResult
import com.indianservers.ai_stem.data.graph.GraphCsvCodec
import com.indianservers.ai_stem.domain.graph.GraphBounds
import com.indianservers.ai_stem.domain.graph.GraphPickingMetadata
import com.indianservers.ai_stem.domain.graph.GraphPoint
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphCsvCodecTest {
    @Test
    fun parsesQuotedCsvCells() {
        val result = GraphCsvCodec().parse("name,value\n\"a,b\",12\nplain,\"3\"") as CsvParseResult.Success

        assertEquals(listOf("name", "value"), result.table.headers)
        assertEquals("a,b", result.table.rows[0][0])
        assertEquals("3", result.table.rows[1][1])
    }

    @Test
    fun exportsPolylinePoints() {
        val primitive = GraphRenderPrimitive.Polyline(
            id = "polyline-1",
            sourceExpressionId = "expr-1",
            bounds = GraphBounds(0.0, 1.0, 0.0, 1.0),
            materialKey = "blue",
            picking = GraphPickingMetadata(label = "Line"),
            updateVersion = 1L,
            vertices = listOf(GraphPoint(0.0, 0.5), GraphPoint(1.0, 1.5))
        )

        val csv = GraphCsvCodec().exportPrimitives(listOf(primitive))

        assertTrue(csv.startsWith("expression_id,primitive_id,point_index,x,y,z"))
        assertTrue(csv.contains("expr-1,polyline-1,1,1.00000000,1.50000000,0.00000000"))
    }

    @Test
    fun rejectsEmptyCsv() {
        val result = GraphCsvCodec().parse("")

        assertTrue(result is CsvParseResult.Failure)
    }
}
