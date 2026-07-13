package com.indianservers.ai_stem.domain.calculus

import com.indianservers.ai_stem.domain.graph.GraphAnalysisEngine
import kotlin.math.PI
import kotlin.math.abs

data class LimitEstimate(val left: Double, val right: Double, val twoSided: Double?, val message: String)
data class IntegralEstimate(val value: Double, val method: String, val intervals: Int, val exactness: String)
data class RiemannEstimate(val left: Double, val right: Double, val midpoint: Double, val trapezoid: Double)
data class VolumeEstimate(val value: Double, val method: String)

class CalculusEngine(private val graphAnalysis: GraphAnalysisEngine = GraphAnalysisEngine()) {
    fun limit(source: String, at: Double): LimitEstimate {
        val h = 1e-5 * maxOf(1.0, abs(at))
        val left = evaluateNear(source, at - h)
        val right = evaluateNear(source, at + h)
        val twoSided = if (left.isFinite() && right.isFinite() && abs(left - right) < 1e-3) (left + right) / 2.0 else null
        return LimitEstimate(
            left = left,
            right = right,
            twoSided = twoSided,
            message = if (twoSided == null) "The left and right estimates do not agree." else "The two-sided limit appears to exist."
        )
    }

    fun derivative(source: String, at: Double, order: Int = 1): Double {
        require(order in 1..4) { "Derivative order is limited to 1 through 4." }
        return if (order == 1) graphAnalysis.derivative(source, at) else {
            val h = 1e-3 * maxOf(1.0, abs(at))
            (derivative(source, at + h, order - 1) - derivative(source, at - h, order - 1)) / (2 * h)
        }
    }

    fun definiteIntegral(source: String, lower: Double, upper: Double, intervals: Int = 200): IntegralEstimate =
        IntegralEstimate(graphAnalysis.definiteIntegral(source, lower, upper, intervals), "Simpson", if (intervals % 2 == 0) intervals else intervals + 1, "Numerical approximation")

    fun riemannSums(source: String, lower: Double, upper: Double, intervals: Int): RiemannEstimate {
        require(intervals > 0 && intervals <= 10_000) { "Use between 1 and 10000 intervals." }
        val width = (upper - lower) / intervals
        fun f(x: Double) = evaluateNear(source, x)
        var left = 0.0
        var right = 0.0
        var midpoint = 0.0
        var trapezoid = 0.0
        repeat(intervals) { index ->
            val x0 = lower + index * width
            val x1 = x0 + width
            left += f(x0) * width
            right += f(x1) * width
            midpoint += f((x0 + x1) / 2.0) * width
            trapezoid += (f(x0) + f(x1)) * width / 2.0
        }
        return RiemannEstimate(left, right, midpoint, trapezoid)
    }

    fun diskVolumeAroundXAxis(source: String, lower: Double, upper: Double, intervals: Int = 200): VolumeEstimate {
        val squaredSource = "pi * (${com.indianservers.ai_stem.domain.graph.extractRightHandExpression(source)})^2"
        return VolumeEstimate(graphAnalysis.definiteIntegral(squaredSource, lower, upper, intervals), "Disk method around x-axis")
    }

    fun partialDerivative(source: String, x: Double, y: Double, variable: String): Double {
        val h = 1e-5
        val parser = com.indianservers.ai_stem.domain.graph.MathExpressionParser()
        val evaluator = com.indianservers.ai_stem.domain.graph.MathExpressionEvaluator()
        val ast = (parser.parse(source) as? com.indianservers.ai_stem.domain.graph.ParseOutcome.Success)?.expression ?: return Double.NaN
        fun eval(xv: Double, yv: Double): Double =
            evaluator.evaluate(ast, com.indianservers.ai_stem.domain.graph.defaultVariables() + mapOf("x" to xv, "y" to yv)).value ?: Double.NaN
        return when (variable.lowercase()) {
            "x" -> (eval(x + h, y) - eval(x - h, y)) / (2 * h)
            "y" -> (eval(x, y + h) - eval(x, y - h)) / (2 * h)
            else -> Double.NaN
        }
    }

    private fun evaluateNear(source: String, x: Double): Double {
        val parser = com.indianservers.ai_stem.domain.graph.MathExpressionParser()
        val evaluator = com.indianservers.ai_stem.domain.graph.MathExpressionEvaluator()
        val ast = (parser.parse(source) as? com.indianservers.ai_stem.domain.graph.ParseOutcome.Success)?.expression ?: return Double.NaN
        return evaluator.evaluate(ast, com.indianservers.ai_stem.domain.graph.defaultVariables() + mapOf("x" to x, "pi" to PI)).value ?: Double.NaN
    }
}
