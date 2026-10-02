package com.indianservers.ai_stem.domain.graph

data class GraphBounds(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minZ: Double = 0.0,
    val maxZ: Double = 0.0
)

data class GraphPickingMetadata(
    val pickable: Boolean = true,
    val tolerancePx: Double = 28.0,
    val label: String
)

sealed interface GraphRenderPrimitive {
    val id: String
    val sourceExpressionId: String
    val bounds: GraphBounds
    val materialKey: String
    val picking: GraphPickingMetadata
    val updateVersion: Long

    data class Polyline(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val vertices: List<GraphPoint>
    ) : GraphRenderPrimitive

    data class PointSet(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val vertices: List<GraphPoint>
    ) : GraphRenderPrimitive

    data class TriangleMesh(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val vertices: List<GraphPoint>,
        val indices: List<Int>
    ) : GraphRenderPrimitive

    data class FilledRegion(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val vertices: List<GraphPoint>,
        val indices: List<Int>
    ) : GraphRenderPrimitive

    data class ArrowSet(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val starts: List<GraphPoint>,
        val ends: List<GraphPoint>
    ) : GraphRenderPrimitive

    data class TextAnchor(
        override val id: String,
        override val sourceExpressionId: String,
        override val bounds: GraphBounds,
        override val materialKey: String,
        override val picking: GraphPickingMetadata,
        override val updateVersion: Long,
        val point: GraphPoint,
        val text: String
    ) : GraphRenderPrimitive
}

class GraphRenderPrimitiveBuilder {
    fun fromCurves(curves: List<GraphCurve>, expressions: List<GraphExpression>, updateVersion: Long): List<GraphRenderPrimitive> {
        val expressionNames = expressions.associate { it.id to it.displayName }
        return curves.mapIndexedNotNull { index, curve ->
            if (curve.points.size < 2) return@mapIndexedNotNull null
            GraphRenderPrimitive.Polyline(
                id = if (index == 0) "polyline-${curve.expressionId}" else "polyline-${curve.expressionId}-$index",
                sourceExpressionId = curve.expressionId,
                bounds = curve.points.bounds(),
                materialKey = "graph-${curve.expressionId}",
                picking = GraphPickingMetadata(label = expressionNames[curve.expressionId] ?: curve.expressionId),
                updateVersion = updateVersion,
                vertices = curve.points
            )
        }
    }
}

private fun List<GraphPoint>.bounds(): GraphBounds =
    GraphBounds(
        minX = minOf { it.x },
        maxX = maxOf { it.x },
        minY = minOf { it.y },
        maxY = maxOf { it.y },
        minZ = minOf { it.z },
        maxZ = maxOf { it.z }
    )
