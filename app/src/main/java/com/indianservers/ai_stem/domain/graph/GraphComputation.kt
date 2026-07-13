package com.indianservers.ai_stem.domain.graph

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

data class GraphComputationJob(
    val id: String,
    val inputFingerprint: Int,
    val qualityPreset: GraphQualityPreset,
    val status: GraphComputationStatus,
    val elapsedMillis: Long? = null,
    val error: String? = null
)

enum class GraphComputationStatus { Queued, Running, Complete, Cancelled, Failed }

class GraphSampler(
    private val parser: MathExpressionParser = MathExpressionParser(),
    private val evaluator: MathExpressionEvaluator = MathExpressionEvaluator()
) {
    fun parseExpression(expression: GraphExpression): GraphExpression =
        expression.copy(parse = parser.parse(expression.source))

    fun sampleExpression(expression: GraphExpression, viewport: GraphViewport, quality: GraphQualityPreset): List<GraphCurve> {
        val parsed = expression.parse as? ParseOutcome.Success ?: parser.parse(expression.source) as? ParseOutcome.Success
            ?: return emptyList()
        return when (expression.kind) {
            GraphExpressionKind.Explicit2D -> listOf(sampleExplicit(expression.id, parsed.expression, viewport, quality.curveSamples))
            GraphExpressionKind.Polar -> listOf(samplePolar(expression.id, parsed.expression, quality.curveSamples))
            GraphExpressionKind.Parametric2D -> sampleParametric2D(expression, viewport, quality)
            GraphExpressionKind.Implicit2D -> sampleImplicit(expression, viewport, quality)
            GraphExpressionKind.Inequality2D -> sampleImplicit(expression, viewport, quality)
            GraphExpressionKind.Points,
            GraphExpressionKind.Sequence,
            GraphExpressionKind.DataTable,
            GraphExpressionKind.Vector,
            GraphExpressionKind.VectorField,
            GraphExpressionKind.ExplicitSurface3D,
            GraphExpressionKind.ParametricSurface3D,
            GraphExpressionKind.SpaceCurve -> emptyList()
        }
    }

    fun sampleSurface(expression: GraphExpression, viewport: GraphViewport, quality: GraphQualityPreset): GraphSurface? {
        if (expression.kind != GraphExpressionKind.ExplicitSurface3D) return null
        val parsed = expression.parse as? ParseOutcome.Success ?: parser.parse(expression.source) as? ParseOutcome.Success ?: return null
        val resolution = quality.surfaceResolution.coerceAtMost(96)
        val vertices = mutableListOf<GraphPoint>()
        for (row in 0 until resolution) {
            val y = viewport.yMin + (viewport.yMax - viewport.yMin) * row / (resolution - 1)
            for (col in 0 until resolution) {
                val x = viewport.xMin + (viewport.xMax - viewport.xMin) * col / (resolution - 1)
                val value = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x, "y" to y)).value
                if (value != null && value.isFinite()) vertices += GraphPoint(x, y, value.coerceIn(viewport.zMin, viewport.zMax))
            }
        }
        return GraphSurface(expression.id, vertices, resolution, resolution)
    }

    private fun sampleExplicit(id: String, ast: MathExpressionNode, viewport: GraphViewport, samples: Int): GraphCurve {
        val points = mutableListOf<GraphPoint>()
        val warnings = mutableSetOf<String>()
        for (i in 0..samples) {
            val x = viewport.xMin + (viewport.xMax - viewport.xMin) * i / samples
            val value = evaluator.evaluate(ast, defaultVariables() + mapOf("x" to x))
            val y = value.value
            if (y != null && y.isFinite() && y in viewport.yMin..viewport.yMax) {
                points += GraphPoint(x, y)
            } else if (value.error != null) {
                warnings += value.error
            }
        }
        return GraphCurve(id, points, warnings.toList())
    }

    private fun samplePolar(id: String, ast: MathExpressionNode, samples: Int): GraphCurve {
        val points = (0..samples).mapNotNull { i ->
            val theta = -Math.PI * 2.0 + Math.PI * 4.0 * i / samples
            val r = evaluator.evaluate(ast, defaultVariables() + mapOf("theta" to theta)).value
            if (r != null && r.isFinite()) GraphPoint(r * cos(theta), r * sin(theta)) else null
        }
        return GraphCurve(id, points)
    }

    private fun sampleParametric2D(expression: GraphExpression, viewport: GraphViewport, quality: GraphQualityPreset): List<GraphCurve> {
        val parts = normalizeEquationSource(expression.source).split(",")
        if (parts.size < 2) return emptyList()
        val xParse = parser.parse(parts[0])
        val yParse = parser.parse(parts[1])
        if (xParse !is ParseOutcome.Success || yParse !is ParseOutcome.Success) return emptyList()
        val points = (0..quality.curveSamples).mapNotNull { i ->
            val t = viewport.xMin + (viewport.xMax - viewport.xMin) * i / quality.curveSamples
            val variables = defaultVariables() + mapOf("t" to t)
            val x = evaluator.evaluate(xParse.expression, variables).value
            val y = evaluator.evaluate(yParse.expression, variables).value
            if (x != null && y != null && x.isFinite() && y.isFinite()) GraphPoint(x, y) else null
        }
        return listOf(GraphCurve(expression.id, points))
    }

    private fun sampleImplicit(expression: GraphExpression, viewport: GraphViewport, quality: GraphQualityPreset): List<GraphCurve> {
        val normalized = normalizeEquationSource(expression.source)
        val sides = normalized.split("=", limit = 2)
        if (sides.size != 2) return emptyList()
        val left = parser.parse(sides[0])
        val right = parser.parse(sides[1])
        if (left !is ParseOutcome.Success || right !is ParseOutcome.Success) return emptyList()
        val resolution = (quality.surfaceResolution * 2).coerceAtMost(128)
        val points = mutableListOf<GraphPoint>()
        for (row in 0 until resolution) {
            val y = viewport.yMin + (viewport.yMax - viewport.yMin) * row / (resolution - 1)
            var previousX = viewport.xMin
            var previous = implicitValue(left.expression, right.expression, previousX, y)
            for (col in 1 until resolution) {
                val x = viewport.xMin + (viewport.xMax - viewport.xMin) * col / (resolution - 1)
                val current = implicitValue(left.expression, right.expression, x, y)
                if (previous.isFinite() && current.isFinite() && sign(previous) != sign(current)) {
                    points += GraphPoint((previousX + x) / 2.0, y)
                }
                previousX = x
                previous = current
            }
        }
        return listOf(GraphCurve(expression.id, points, listOf("Implicit curves are approximated from visible samples.")))
    }

    private fun implicitValue(left: MathExpressionNode, right: MathExpressionNode, x: Double, y: Double): Double {
        val variables = defaultVariables() + mapOf("x" to x, "y" to y)
        val l = evaluator.evaluate(left, variables).value ?: Double.NaN
        val r = evaluator.evaluate(right, variables).value ?: Double.NaN
        return l - r
    }
}

class GraphAnalysisEngine(
    private val parser: MathExpressionParser = MathExpressionParser(),
    private val evaluator: MathExpressionEvaluator = MathExpressionEvaluator()
) {
    fun derivative(source: String, x: Double): Double {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return Double.NaN
        val h = 1e-5 * maxOf(1.0, abs(x))
        val f1 = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x + h)).value ?: return Double.NaN
        val f0 = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x - h)).value ?: return Double.NaN
        return (f1 - f0) / (2.0 * h)
    }

    fun secondDerivative(source: String, x: Double): Double {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return Double.NaN
        val h = 1e-4 * maxOf(1.0, abs(x))
        val fm = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x - h)).value ?: return Double.NaN
        val f = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x)).value ?: return Double.NaN
        val fp = evaluator.evaluate(parsed.expression, defaultVariables() + mapOf("x" to x + h)).value ?: return Double.NaN
        return (fp - 2 * f + fm) / (h * h)
    }

    fun roots(source: String, viewport: GraphViewport, samples: Int = 200): List<GraphPoint> {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return emptyList()
        val roots = mutableListOf<GraphPoint>()
        var prevX = viewport.xMin
        var prevY = value(parsed.expression, prevX)
        for (i in 1..samples) {
            val x = viewport.xMin + (viewport.xMax - viewport.xMin) * i / samples
            val y = value(parsed.expression, x)
            if (prevY.isFinite() && y.isFinite() && sign(prevY) != sign(y)) {
                val root = bisect(parsed.expression, prevX, x)
                roots += GraphPoint(root, value(parsed.expression, root))
            }
            prevX = x
            prevY = y
        }
        return roots
    }

    fun extrema(source: String, viewport: GraphViewport, samples: Int = 200): List<GraphPoint> {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return emptyList()
        val candidates = mutableListOf<GraphPoint>()
        var prevX = viewport.xMin
        var prevSlope = derivative(source, prevX)
        for (i in 1..samples) {
            val x = viewport.xMin + (viewport.xMax - viewport.xMin) * i / samples
            val slope = derivative(source, x)
            if (prevSlope.isFinite() && slope.isFinite() && sign(prevSlope) != sign(slope)) {
                val mid = (prevX + x) / 2.0
                candidates += GraphPoint(mid, value(parsed.expression, mid))
            }
            prevX = x
            prevSlope = slope
        }
        return candidates
    }

    fun definiteIntegral(source: String, lower: Double, upper: Double, intervals: Int = 200): Double {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return Double.NaN
        val n = if (intervals % 2 == 0) intervals else intervals + 1
        val h = (upper - lower) / n
        var sum = value(parsed.expression, lower) + value(parsed.expression, upper)
        for (i in 1 until n) {
            val x = lower + i * h
            val y = value(parsed.expression, x)
            if (!y.isFinite()) return Double.NaN
            sum += if (i % 2 == 0) 2 * y else 4 * y
        }
        return sum * h / 3.0
    }

    fun tangent(source: String, x: Double): GraphAnalysisResult {
        val parsed = parser.parse(source) as? ParseOutcome.Success
            ?: return GraphAnalysisResult("", "Tangent", null, null, "This formula is incomplete.")
        val y = value(parsed.expression, x)
        val slope = derivative(source, x)
        return GraphAnalysisResult("", "Tangent", GraphPoint(x, y), slope, "Tangent slope ${"%.3f".format(slope)} at x=${"%.3f".format(x)}")
    }

    fun normal(source: String, x: Double): GraphAnalysisResult {
        val tangent = tangent(source, x)
        val slope = tangent.value
        val normalSlope = if (slope == null || slope == 0.0) Double.NaN else -1.0 / slope
        return tangent.copy(type = "Normal", value = normalSlope, message = "Normal slope ${"%.3f".format(normalSlope)}")
    }

    private fun bisect(ast: MathExpressionNode, aRaw: Double, bRaw: Double): Double {
        var a = aRaw
        var b = bRaw
        repeat(48) {
            val mid = (a + b) / 2.0
            val left = value(ast, a)
            val center = value(ast, mid)
            if (sign(left) == sign(center)) a = mid else b = mid
        }
        return (a + b) / 2.0
    }

    private fun value(ast: MathExpressionNode, x: Double): Double =
        evaluator.evaluate(ast, defaultVariables() + mapOf("x" to x)).value ?: Double.NaN
}
