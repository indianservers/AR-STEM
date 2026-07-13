package com.indianservers.ai_stem.domain.graph

import kotlin.math.PI

const val CURRENT_GRAPH_SCHEMA_VERSION = 1

enum class CoordinateSystemKind { Cartesian2D, Polar2D, Cartesian3D, Cylindrical, Spherical }
enum class GraphExpressionKind { Explicit2D, Polar, Parametric2D, Implicit2D, Inequality2D, Points, Sequence, DataTable, ExplicitSurface3D, ParametricSurface3D, SpaceCurve, Vector, VectorField }
enum class GraphRenderMode { Screen, AR }
enum class GraphQualityPreset(val curveSamples: Int, val surfaceResolution: Int) {
    BatterySaver(160, 32),
    Balanced(320, 48),
    HighQuality(640, 72),
    Presentation(960, 96)
}

data class CoordinateSystemDefinition(
    val kind: CoordinateSystemKind = CoordinateSystemKind.Cartesian2D,
    val xLabel: String = "x",
    val yLabel: String = "y",
    val zLabel: String = "z"
)

data class GraphViewport(
    val xMin: Double = -10.0,
    val xMax: Double = 10.0,
    val yMin: Double = -10.0,
    val yMax: Double = 10.0,
    val zMin: Double = -5.0,
    val zMax: Double = 5.0
)

data class GraphStyle(
    val color: Long,
    val lineWidth: Float = 2.5f,
    val opacity: Float = 1f,
    val dashed: Boolean = false,
    val labelVisible: Boolean = true
)

data class GraphExpression(
    val id: String,
    val kind: GraphExpressionKind,
    val source: String,
    val displayName: String,
    val visible: Boolean = true,
    val style: GraphStyle,
    val parse: ParseOutcome = ParseOutcome.NotParsed
)

data class GraphSlider(
    val id: String,
    val symbol: String,
    val minimum: Double,
    val maximum: Double,
    val step: Double,
    val value: Double
)

data class GraphPoint(val x: Double, val y: Double, val z: Double = 0.0)
data class GraphCurve(val expressionId: String, val points: List<GraphPoint>, val warnings: List<String> = emptyList())
data class GraphSurface(val expressionId: String, val vertices: List<GraphPoint>, val rowCount: Int, val columnCount: Int, val warnings: List<String> = emptyList())
data class GraphAnnotation(val id: String, val text: String, val point: GraphPoint?)
data class GraphDataTable(val id: String, val name: String, val columns: List<String>, val rows: List<List<Double>>)

data class GraphAnalysisResult(
    val expressionId: String,
    val type: String,
    val point: GraphPoint?,
    val value: Double?,
    val message: String
)

data class GraphAnimationState(
    val playing: Boolean = false,
    val sliderId: String? = null,
    val speed: Double = 1.0
)

data class GraphAppearance(
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val highContrast: Boolean = false,
    val qualityPreset: GraphQualityPreset = GraphQualityPreset.Balanced
)

data class GraphProject(
    val id: String = "graph-${System.currentTimeMillis()}",
    val name: String = "Graphing Studio Project",
    val coordinateSystem: CoordinateSystemDefinition = CoordinateSystemDefinition(),
    val expressions: List<GraphExpression> = emptyList(),
    val sliders: List<GraphSlider> = emptyList(),
    val dataTables: List<GraphDataTable> = emptyList(),
    val annotations: List<GraphAnnotation> = emptyList(),
    val viewport: GraphViewport = GraphViewport(),
    val appearance: GraphAppearance = GraphAppearance(),
    val animation: GraphAnimationState = GraphAnimationState(),
    val analysisResults: List<GraphAnalysisResult> = emptyList(),
    val schemaVersion: Int = CURRENT_GRAPH_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = createdAt
)

sealed interface ParseOutcome {
    data object NotParsed : ParseOutcome
    data class Success(val expression: MathExpressionNode, val variables: Set<String>) : ParseOutcome
    data class Failure(val message: String, val position: Int? = null) : ParseOutcome
}

sealed interface MathExpressionNode {
    data class Constant(val value: Double) : MathExpressionNode
    data class Variable(val name: String) : MathExpressionNode
    data class UnaryOperation(val operation: UnaryOperator, val operand: MathExpressionNode) : MathExpressionNode
    data class BinaryOperation(val operation: BinaryOperator, val left: MathExpressionNode, val right: MathExpressionNode) : MathExpressionNode
    data class FunctionCall(val name: String, val arguments: List<MathExpressionNode>) : MathExpressionNode
    data class Conditional(val condition: BooleanExpressionNode, val whenTrue: MathExpressionNode, val whenFalse: MathExpressionNode?) : MathExpressionNode
}

sealed interface BooleanExpressionNode {
    data class Comparison(val operation: ComparisonOperator, val left: MathExpressionNode, val right: MathExpressionNode) : BooleanExpressionNode
    data class And(val left: BooleanExpressionNode, val right: BooleanExpressionNode) : BooleanExpressionNode
    data class Or(val left: BooleanExpressionNode, val right: BooleanExpressionNode) : BooleanExpressionNode
}

enum class UnaryOperator { Positive, Negative, Factorial }
enum class BinaryOperator { Add, Subtract, Multiply, Divide, Power, Modulo }
enum class ComparisonOperator { LessThan, LessThanOrEqual, GreaterThan, GreaterThanOrEqual, Equal, NotEqual }

object GraphSamples {
    fun starterProject(): GraphProject =
        GraphProject(
            name = "Starter Graphs",
            expressions = listOf(
                graph("expr-parabola", GraphExpressionKind.Explicit2D, "y = x^2", "Parabola", 0xFF2E7DFF),
                graph("expr-sine", GraphExpressionKind.Explicit2D, "y = sin(x)", "Sine Wave", 0xFFE53935),
                graph("expr-polar", GraphExpressionKind.Polar, "r = 2 cos(theta)", "Polar Circle", 0xFF43A047),
                graph("expr-surface", GraphExpressionKind.ExplicitSurface3D, "z = sin(x) * cos(y)", "3D Wave", 0xFF8E24AA)
            )
        )

    fun templates(): List<GraphExpression> = listOf(
        graph("template-line", GraphExpressionKind.Explicit2D, "y = x", "Straight Line", 0xFF2E7DFF),
        graph("template-parabola", GraphExpressionKind.Explicit2D, "y = x^2", "Parabola", 0xFFE53935),
        graph("template-circle", GraphExpressionKind.Implicit2D, "x^2 + y^2 = 25", "Circle", 0xFF43A047),
        graph("template-sine", GraphExpressionKind.Explicit2D, "y = sin(x)", "Sine Wave", 0xFFFFA000),
        graph("template-cosine", GraphExpressionKind.Explicit2D, "y = cos(x)", "Cosine Wave", 0xFF00897B),
        graph("template-exp", GraphExpressionKind.Explicit2D, "y = e^x", "Exponential", 0xFF5E35B1),
        graph("template-recip", GraphExpressionKind.Explicit2D, "y = 1 / x", "Reciprocal", 0xFFD81B60),
        graph("template-abs", GraphExpressionKind.Explicit2D, "y = abs(x)", "Absolute Value", 0xFF6D4C41),
        graph("template-bowl", GraphExpressionKind.ExplicitSurface3D, "z = x^2 + y^2", "3D Bowl", 0xFF3949AB),
        graph("template-wave", GraphExpressionKind.ExplicitSurface3D, "z = sin(x) * cos(y)", "3D Wave", 0xFF7CB342),
        graph("template-vector", GraphExpressionKind.Vector, "<3, 4, 0>", "Vector", 0xFF00ACC1)
    )

    private fun graph(id: String, kind: GraphExpressionKind, source: String, name: String, color: Long) =
        GraphExpression(id, kind, source, name, style = GraphStyle(color = color))
}

fun normalizeEquationSource(source: String): String =
    source
        .replace('−', '-')
        .replace('×', '*')
        .replace('÷', '/')
        .replace("π", "pi")
        .replace("θ", "theta")
        .replace("²", "^2")
        .replace("³", "^3")
        .trim()

fun extractRightHandExpression(source: String): String {
    val normalized = normalizeEquationSource(source)
    val parts = normalized.split("=", limit = 2)
    return if (parts.size == 2) parts[1].trim() else normalized
}

fun defaultVariables(sliders: List<GraphSlider> = emptyList()): Map<String, Double> =
    buildMap {
        put("pi", PI)
        put("e", kotlin.math.E)
        put("phi", 1.618033988749895)
        sliders.forEach { put(it.symbol, it.value) }
    }
