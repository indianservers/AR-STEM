package com.indianservers.ai_stem.domain.graph

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PolynomialDefinition(
    val coefficients: Map<Int, Double>
) {
    val degree: Int = coefficients.keys.maxOrNull() ?: 0
    fun valueAt(x: Double): Double = coefficients.entries.sumOf { (power, coefficient) ->
        coefficient * Math.pow(x, power.toDouble())
    }
}

data class GraphFeaturePoint(
    val kind: String,
    val point: GraphPoint,
    val label: String
)

data class GraphEquationInsight(
    val expressionId: String,
    val equation: String,
    val type: String,
    val properties: List<Pair<String, String>>,
    val featurePoints: List<GraphFeaturePoint>
)

class GraphInsightsEngine(
    private val parser: MathExpressionParser = MathExpressionParser()
) {
    fun polynomial(source: String): PolynomialDefinition? {
        val parsed = parser.parse(source) as? ParseOutcome.Success ?: return null
        val coefficients = coefficients(parsed.expression) ?: return null
        return PolynomialDefinition(coefficients.clean())
    }

    fun analyze(expression: GraphExpression): GraphEquationInsight? {
        val polynomial = polynomial(expression.source) ?: return null
        return when (polynomial.degree) {
            1 -> lineInsight(expression, polynomial)
            2 -> quadraticInsight(expression, polynomial)
            else -> null
        }
    }

    fun intersections(first: GraphExpression, second: GraphExpression): List<GraphFeaturePoint> {
        val a = polynomial(first.source) ?: return emptyList()
        val b = polynomial(second.source) ?: return emptyList()
        val difference = (a.coefficients.keys + b.coefficients.keys).associateWith { power ->
            a.coefficients.getOrDefault(power, 0.0) - b.coefficients.getOrDefault(power, 0.0)
        }.clean()
        return realRoots(PolynomialDefinition(difference)).map { x ->
            val y = a.valueAt(x)
            GraphFeaturePoint("intersection", GraphPoint(x, y), "(${format(x)}, ${format(y)})")
        }
    }

    fun realRoots(polynomial: PolynomialDefinition): List<Double> = when (polynomial.degree) {
        1 -> {
            val b = polynomial.coefficients.getOrDefault(1, 0.0)
            val c = polynomial.coefficients.getOrDefault(0, 0.0)
            if (abs(b) < EPSILON) emptyList() else listOf(-c / b)
        }
        2 -> {
            val a = polynomial.coefficients.getOrDefault(2, 0.0)
            val b = polynomial.coefficients.getOrDefault(1, 0.0)
            val c = polynomial.coefficients.getOrDefault(0, 0.0)
            val discriminant = b * b - 4.0 * a * c
            when {
                abs(a) < EPSILON -> realRoots(PolynomialDefinition(mapOf(1 to b, 0 to c)))
                discriminant < -EPSILON -> emptyList()
                abs(discriminant) <= EPSILON -> listOf(-b / (2.0 * a))
                else -> listOf(
                    (-b - sqrt(discriminant)) / (2.0 * a),
                    (-b + sqrt(discriminant)) / (2.0 * a)
                )
            }
        }
        else -> emptyList()
    }

    private fun lineInsight(expression: GraphExpression, polynomial: PolynomialDefinition): GraphEquationInsight {
        val slope = polynomial.coefficients.getOrDefault(1, 0.0)
        val intercept = polynomial.coefficients.getOrDefault(0, 0.0)
        val xIntercept = if (abs(slope) < EPSILON) null else -intercept / slope
        val points = buildList {
            add(GraphFeaturePoint("y-intercept", GraphPoint(0.0, intercept), "(0, ${format(intercept)})"))
            xIntercept?.let { add(GraphFeaturePoint("x-intercept", GraphPoint(it, 0.0), "(${format(it)}, 0)")) }
        }
        return GraphEquationInsight(
            expression.id,
            expression.source,
            "Line",
            listOf(
                "Slope" to format(slope),
                "y-intercept" to format(intercept),
                "Direction" to when { slope > 0 -> "Increasing"; slope < 0 -> "Decreasing"; else -> "Constant" },
                "Domain" to "(-∞, ∞)",
                "Range" to if (abs(slope) < EPSILON) "{${format(intercept)}}" else "(-∞, ∞)"
            ),
            points
        )
    }

    private fun quadraticInsight(expression: GraphExpression, polynomial: PolynomialDefinition): GraphEquationInsight {
        val a = polynomial.coefficients.getOrDefault(2, 0.0)
        val b = polynomial.coefficients.getOrDefault(1, 0.0)
        val c = polynomial.coefficients.getOrDefault(0, 0.0)
        val vertexX = -b / (2.0 * a)
        val vertexY = polynomial.valueAt(vertexX)
        val discriminant = b * b - 4.0 * a * c
        val roots = realRoots(polynomial)
        val points = buildList {
            add(GraphFeaturePoint("vertex", GraphPoint(vertexX, vertexY), "(${format(vertexX)}, ${format(vertexY)})"))
            add(GraphFeaturePoint("y-intercept", GraphPoint(0.0, c), "(0, ${format(c)})"))
            roots.forEach { root -> add(GraphFeaturePoint("x-intercept", GraphPoint(root, 0.0), "(${format(root)}, 0)")) }
        }
        val exactRoots = exactQuadraticRoots(a, b, discriminant)
        return GraphEquationInsight(
            expression.id,
            expression.source,
            "Quadratic",
            listOf(
                "Vertex" to "(${format(vertexX)}, ${format(vertexY)})",
                "Axis of symmetry" to "x = ${format(vertexX)}",
                "Opening" to if (a > 0) "Upward" else "Downward",
                "Discriminant" to format(discriminant),
                "Exact roots" to exactRoots,
                "Roots" to roots.joinToString { format(it) }.ifBlank { "No real roots" },
                "Domain" to "(-∞, ∞)",
                "Range" to if (a > 0) "[${format(vertexY)}, ∞)" else "(-∞, ${format(vertexY)}]"
            ),
            points
        )
    }

    private fun coefficients(node: MathExpressionNode): Map<Int, Double>? = when (node) {
        is MathExpressionNode.Constant -> mapOf(0 to node.value)
        is MathExpressionNode.Variable -> if (node.name == "x") mapOf(1 to 1.0) else null
        is MathExpressionNode.UnaryOperation -> coefficients(node.operand)?.let { values ->
            when (node.operation) {
                UnaryOperator.Positive -> values
                UnaryOperator.Negative -> values.mapValues { -it.value }
                UnaryOperator.Factorial -> null
            }
        }
        is MathExpressionNode.BinaryOperation -> {
            val left = coefficients(node.left)
            val right = coefficients(node.right)
            when (node.operation) {
                BinaryOperator.Add -> if (left != null && right != null) add(left, right, 1.0) else null
                BinaryOperator.Subtract -> if (left != null && right != null) add(left, right, -1.0) else null
                BinaryOperator.Multiply -> if (left != null && right != null) multiply(left, right) else null
                BinaryOperator.Divide -> {
                    val denominator = (node.right as? MathExpressionNode.Constant)?.value
                    if (left != null && denominator != null && abs(denominator) > EPSILON) left.mapValues { it.value / denominator } else null
                }
                BinaryOperator.Power -> {
                    val power = (node.right as? MathExpressionNode.Constant)?.value?.roundToInt()
                    if (left != null && power != null && power in 0..2) {
                        generateSequence(mapOf(0 to 1.0)) { multiply(it, left) }.take(power + 1).last()
                    } else null
                }
                BinaryOperator.Modulo -> null
            }
        }
        is MathExpressionNode.FunctionCall,
        is MathExpressionNode.Conditional -> null
    }

    private fun add(a: Map<Int, Double>, b: Map<Int, Double>, sign: Double): Map<Int, Double> =
        (a.keys + b.keys).associateWith { a.getOrDefault(it, 0.0) + sign * b.getOrDefault(it, 0.0) }.clean()

    private fun multiply(a: Map<Int, Double>, b: Map<Int, Double>): Map<Int, Double>? {
        val result = mutableMapOf<Int, Double>()
        a.forEach { (leftPower, leftValue) ->
            b.forEach { (rightPower, rightValue) ->
                val power = leftPower + rightPower
                if (power > 2) return null
                result[power] = result.getOrDefault(power, 0.0) + leftValue * rightValue
            }
        }
        return result.clean()
    }

    private fun exactQuadraticRoots(a: Double, b: Double, discriminant: Double): String {
        if (discriminant < 0.0) return "No real roots"
        val sqrtInteger = sqrt(discriminant).roundToInt()
        return if (abs(sqrtInteger * sqrtInteger - discriminant) < EPSILON) {
            listOf(
                (-b - sqrtInteger) / (2.0 * a),
                (-b + sqrtInteger) / (2.0 * a)
            ).distinct().joinToString { format(it) }
        } else if (abs(a - 1.0) < EPSILON && abs(b + 2.0) < EPSILON && abs(discriminant - 8.0) < EPSILON) {
            "1 ± √2"
        } else {
            "(${-b} ± √${format(discriminant)}) / ${format(2.0 * a)}"
        }
    }

    private fun Map<Int, Double>.clean(): Map<Int, Double> = filterValues { abs(it) > EPSILON }.ifEmpty { mapOf(0 to 0.0) }
    private fun format(value: Double): String = if (abs(value - value.roundToInt()) < 1e-9) value.roundToInt().toString() else "%.6f".format(value).trimEnd('0').trimEnd('.')

    private companion object {
        const val EPSILON = 1e-9
    }
}
