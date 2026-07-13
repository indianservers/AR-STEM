package com.indianservers.ai_stem.data.graph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import com.indianservers.ai_stem.domain.graph.GraphViewport
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

data class GraphPngExportOptions(
    val width: Int = 1920,
    val height: Int = 1080,
    val transparent: Boolean = false,
    val includeAxes: Boolean = true,
    val includeGrid: Boolean = true,
    val includeLabels: Boolean = true,
    val title: String? = null
)

class GraphPngExporter {
    fun export(
        primitives: List<GraphRenderPrimitive>,
        viewport: GraphViewport,
        options: GraphPngExportOptions = GraphPngExportOptions()
    ): ByteArray {
        require(options.width in 256..8192) { "Width is outside the supported range." }
        require(options.height in 256..8192) { "Height is outside the supported range." }
        val bitmap = Bitmap.createBitmap(options.width, options.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (!options.transparent) canvas.drawColor(Color.WHITE)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(52, 128, 128, 128)
            strokeWidth = 1.2f
        }
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            strokeWidth = 3f
        }
        if (options.includeGrid) {
            for (i in -10..10) {
                val x = mapX(i.toDouble(), viewport, options.width)
                val y = mapY(i.toDouble(), viewport, options.height)
                canvas.drawLine(x, 0f, x, options.height.toFloat(), gridPaint)
                canvas.drawLine(0f, y, options.width.toFloat(), y, gridPaint)
            }
        }
        if (options.includeAxes) {
            canvas.drawLine(mapX(viewport.xMin, viewport, options.width), mapY(0.0, viewport, options.height), mapX(viewport.xMax, viewport, options.width), mapY(0.0, viewport, options.height), axisPaint)
            canvas.drawLine(mapX(0.0, viewport, options.width), mapY(viewport.yMin, viewport, options.height), mapX(0.0, viewport, options.width), mapY(viewport.yMax, viewport, options.height), axisPaint)
        }

        primitives.filterIsInstance<GraphRenderPrimitive.Polyline>().forEachIndexed { index, primitive ->
            if (primitive.vertices.size < 2) return@forEachIndexed
            val path = Path()
            primitive.vertices.forEachIndexed { pointIndex, point ->
                val x = mapX(point.x, viewport, options.width)
                val y = mapY(point.y, viewport, options.height)
                if (pointIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette[index % palette.size]
                strokeWidth = 5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, paint)
        }

        if (options.includeLabels && options.title != null) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(32, 33, 36)
                textSize = (options.width * 0.018f).coerceIn(18f, 40f)
            }
            canvas.drawText(options.title, 28f, 48f, textPaint)
        }

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun mapX(x: Double, viewport: GraphViewport, width: Int): Float =
        (((x - viewport.xMin) / (viewport.xMax - viewport.xMin)) * width).toFloat()

    private fun mapY(y: Double, viewport: GraphViewport, height: Int): Float =
        (height - ((y - viewport.yMin) / (viewport.yMax - viewport.yMin)) * height).toFloat()

    private companion object {
        val palette = listOf(
            0xFF2E7DFF.toInt(),
            0xFFE53935.toInt(),
            0xFF43A047.toInt(),
            0xFFFFA000.toInt(),
            0xFF8E24AA.toInt(),
            0xFF00897B.toInt()
        )
    }
}
