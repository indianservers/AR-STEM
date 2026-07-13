package com.indianservers.ai_stem.data.graph

import com.indianservers.ai_stem.domain.graph.GraphPoint
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import java.util.Locale

data class CsvTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

sealed interface CsvParseResult {
    data class Success(val table: CsvTable) : CsvParseResult
    data class Failure(val message: String) : CsvParseResult
}

class GraphCsvCodec(
    private val maxRows: Int = 50_000,
    private val maxColumns: Int = 128
) {
    fun exportPrimitives(primitives: List<GraphRenderPrimitive>): String {
        val rows = mutableListOf<List<String>>()
        primitives.forEach { primitive ->
            when (primitive) {
                is GraphRenderPrimitive.Polyline -> primitive.vertices.forEachIndexed { index, point ->
                    rows += row(primitive.sourceExpressionId, primitive.id, index, point)
                }
                is GraphRenderPrimitive.PointSet -> primitive.vertices.forEachIndexed { index, point ->
                    rows += row(primitive.sourceExpressionId, primitive.id, index, point)
                }
                is GraphRenderPrimitive.TriangleMesh -> primitive.vertices.forEachIndexed { index, point ->
                    rows += row(primitive.sourceExpressionId, primitive.id, index, point)
                }
                is GraphRenderPrimitive.FilledRegion -> primitive.vertices.forEachIndexed { index, point ->
                    rows += row(primitive.sourceExpressionId, primitive.id, index, point)
                }
                is GraphRenderPrimitive.ArrowSet -> primitive.starts.zip(primitive.ends).forEachIndexed { index, (start, end) ->
                    rows += listOf(
                        primitive.sourceExpressionId,
                        primitive.id,
                        index.toString(),
                        fmt(start.x),
                        fmt(start.y),
                        fmt(start.z),
                        fmt(end.x),
                        fmt(end.y),
                        fmt(end.z)
                    )
                }
                is GraphRenderPrimitive.TextAnchor -> rows += row(primitive.sourceExpressionId, primitive.id, 0, primitive.point)
            }
        }
        return buildString {
            appendLine(listOf("expression_id", "primitive_id", "point_index", "x", "y", "z", "x2", "y2", "z2").joinToString(","))
            rows.forEach { appendLine(it.joinToString(",") { cell -> cell.escapeCsv() }) }
        }
    }

    fun parse(raw: String, hasHeader: Boolean = true): CsvParseResult {
        val rows = raw.lineSequence()
            .filter { it.isNotBlank() }
            .map(::parseLine)
            .toList()
        if (rows.isEmpty()) return CsvParseResult.Failure("The CSV file is empty.")
        if (rows.size > maxRows) return CsvParseResult.Failure("The CSV file has too many rows.")
        if (rows.any { it.size > maxColumns }) return CsvParseResult.Failure("The CSV file has too many columns.")
        val headers = if (hasHeader) rows.first() else rows.first().indices.map { "Column ${it + 1}" }
        val body = if (hasHeader) rows.drop(1) else rows
        return CsvParseResult.Success(CsvTable(headers, body))
    }

    private fun row(expressionId: String, primitiveId: String, index: Int, point: GraphPoint): List<String> =
        listOf(expressionId, primitiveId, index.toString(), fmt(point.x), fmt(point.y), fmt(point.z), "", "", "")

    private fun fmt(value: Double): String = String.format(Locale.US, "%.8f", value)

    private fun parseLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                quoted && char == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i += 1
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            i += 1
        }
        cells += current.toString()
        return cells
    }

    private fun String.escapeCsv(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${replace("\"", "\"\"")}\"" else this
}
