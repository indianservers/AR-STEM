package com.indianservers.ai_stem.domain.graph

import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

data class ArGraphDomain(
    val xMin: Double = -6.283185307179586,
    val xMax: Double = 6.283185307179586,
    val yMin: Double = -6.283185307179586,
    val yMax: Double = 6.283185307179586,
    val zMin: Double = -4.0,
    val zMax: Double = 4.0,
    val valueClamp: Double = 4.0
) {
    fun toViewport(): GraphViewport = GraphViewport(xMin, xMax, yMin, yMax, zMin, zMax)
}

data class ArGraphParameter(
    val symbol: String,
    val minimum: Double = -5.0,
    val maximum: Double = 5.0,
    val step: Double = 0.1,
    val value: Double = 1.0
) {
    fun toSlider(): GraphSlider = GraphSlider(
        id = "ar-param-$symbol",
        symbol = symbol,
        minimum = minimum,
        maximum = maximum,
        step = step,
        value = value
    )
}

data class ArCompiledExpression(
    val source: String,
    val normalizedSource: String,
    val kind: GraphExpressionKind,
    val parse: ParseOutcome,
    val parameters: List<GraphSlider>,
    val domain: ArGraphDomain = ArGraphDomain(),
    val qualityPreset: GraphQualityPreset = GraphQualityPreset.Balanced
) {
    val isValid: Boolean get() = parse is ParseOutcome.Success
    val message: String
        get() = when (parse) {
            ParseOutcome.NotParsed -> "Formula not parsed yet."
            is ParseOutcome.Success -> {
                val parameterText = if (parameters.isEmpty()) "no parameters" else parameters.joinToString { it.symbol }
                "Ready: ${kind.label} with $parameterText."
            }
            is ParseOutcome.Failure -> parse.message
        }
}

data class ArGraphSampleSet(
    val curve: GraphCurve,
    val surface: GraphSurface?,
    val warnings: List<String> = emptyList()
)

data class ArGraphAnalysisPoint(
    val x: Double,
    val y: Double,
    val label: String,
    val type: ArGraphAnalysisPointType
)

enum class ArGraphAnalysisPointType { Root, Intersection, Minimum, Maximum, Inflection, TangentPoint }

data class ArGraphLineAnalysis(
    val point: GraphPoint,
    val tangentSlope: Double,
    val normalSlope: Double,
    val tangentAngleDegrees: Double
)

data class ArGraphIntegralAnalysis(
    val lower: Double,
    val upper: Double,
    val signedArea: Double,
    val absoluteArea: Double,
    val volumeOfRevolution: Double,
    val arcLength: Double
)

data class ArGraphAnalysisReport(
    val roots: List<ArGraphAnalysisPoint> = emptyList(),
    val intersections: List<ArGraphAnalysisPoint> = emptyList(),
    val extrema: List<ArGraphAnalysisPoint> = emptyList(),
    val inflections: List<ArGraphAnalysisPoint> = emptyList(),
    val tangent: ArGraphLineAnalysis? = null,
    val integral: ArGraphIntegralAnalysis? = null,
    val warnings: List<String> = emptyList()
) {
    val highlightedPoints: List<ArGraphAnalysisPoint>
        get() = roots + intersections + extrema + inflections + listOfNotNull(
            tangent?.let { ArGraphAnalysisPoint(it.point.x, it.point.y, "T", ArGraphAnalysisPointType.TangentPoint) }
        )
}

class ArMathEngine(
    private val parser: MathExpressionParser = MathExpressionParser(),
    private val evaluator: MathExpressionEvaluator = MathExpressionEvaluator(),
    private val sampler: GraphSampler = GraphSampler(parser, evaluator)
) {
    fun compile(
        source: String,
        existingSliders: List<GraphSlider> = emptyList(),
        domain: ArGraphDomain = ArGraphDomain(),
        qualityPreset: GraphQualityPreset = GraphQualityPreset.Balanced
    ): ArCompiledExpression {
        val normalized = normalizeArFormula(source)
        val kind = detectKind(normalized)
        val parse = validateForKind(normalized, kind)
        val parameters = if (parse is ParseOutcome.Success) {
            buildParameterSliders(parse.variables, existingSliders)
        } else {
            existingSliders
        }
        return ArCompiledExpression(
            source = source,
            normalizedSource = normalized,
            kind = kind,
            parse = parse,
            parameters = parameters,
            domain = domain,
            qualityPreset = qualityPreset
        )
    }

    fun evaluate2d(compiled: ArCompiledExpression, x: Double, phase: Double = 0.0): Double {
        val value = evaluate2dRaw(compiled, x, phase)
        return value.coerceFinite(compiled.domain.valueClamp)
    }

    fun evaluate2dUnclamped(compiled: ArCompiledExpression, x: Double, phase: Double = 0.0): Double =
        evaluate2dRaw(compiled, x, phase)

    fun evaluate3d(compiled: ArCompiledExpression, x: Double, y: Double, phase: Double = 0.0): Double {
        val value = evaluate3dRaw(compiled, x, y, phase)
        return value.coerceFinite(compiled.domain.valueClamp)
    }

    private fun evaluate2dRaw(compiled: ArCompiledExpression, x: Double, phase: Double = 0.0): Double {
        val ast = (compiled.parse as? ParseOutcome.Success)?.expression ?: return Double.NaN
        val value = evaluator.evaluate(ast, variables(compiled.parameters) + mapOf("x" to x + phase)).value ?: return Double.NaN
        return if (value.isFinite()) value else Double.NaN
    }

    private fun evaluate3dRaw(compiled: ArCompiledExpression, x: Double, y: Double, phase: Double = 0.0): Double {
        val ast = (compiled.parse as? ParseOutcome.Success)?.expression ?: return Double.NaN
        val vars = variables(compiled.parameters) + mapOf("x" to x + phase, "y" to y)
        val value = evaluator.evaluate(ast, vars).value ?: return Double.NaN
        return if (value.isFinite()) value else Double.NaN
    }

    fun sample(compiled: ArCompiledExpression): ArGraphSampleSet {
        val expression = GraphExpression(
            id = "ar-live-expression",
            kind = compiled.kind,
            source = compiled.normalizedSource,
            displayName = "AR function",
            style = GraphStyle(color = 0xFF2E7DFF),
            parse = compiled.parse
        )
        val curve = when (compiled.kind) {
            GraphExpressionKind.ExplicitSurface3D -> sampleCurveFromSurface(compiled)
            GraphExpressionKind.Parametric2D,
            GraphExpressionKind.SpaceCurve -> sampleParametricCurve(compiled)
            else -> sampler.sampleExpression(expression, compiled.domain.toViewport(), compiled.qualityPreset).firstOrNull()
                ?: GraphCurve(expression.id, emptyList(), listOf("No visible curve samples."))
        }
        val surface = if (compiled.kind == GraphExpressionKind.ExplicitSurface3D) {
            sampler.sampleSurface(expression, compiled.domain.toViewport(), compiled.qualityPreset)
        } else {
            null
        }
        return ArGraphSampleSet(curve, surface, curve.warnings + (surface?.warnings ?: emptyList()))
    }

    private fun validateForKind(source: String, kind: GraphExpressionKind): ParseOutcome =
        when (kind) {
            GraphExpressionKind.Parametric2D,
            GraphExpressionKind.SpaceCurve -> validateParametric(source)
            GraphExpressionKind.Implicit2D,
            GraphExpressionKind.Inequality2D -> validateImplicitOrInequality(source)
            else -> parser.parse(source)
        }

    private fun validateParametric(source: String): ParseOutcome {
        val parts = normalizeParametricSource(source).split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size < 2) return ParseOutcome.Failure("Use parametric form: x=cos(t), y=sin(t).")
        val parsed = parts.map { parser.parse(it.substringAfter("=")) }
        val first = parsed.firstOrNull() as? ParseOutcome.Success
            ?: return parsed.firstOrNull { it is ParseOutcome.Failure } ?: ParseOutcome.Failure("Parametric expression could not be parsed.")
        val variables = parsed.filterIsInstance<ParseOutcome.Success>().flatMap { it.variables }.toSet()
        return ParseOutcome.Success(first.expression, variables)
    }

    private fun validateImplicitOrInequality(source: String): ParseOutcome {
        val operator = listOf("<=", ">=", "<", ">", "=").firstOrNull { source.contains(it) }
            ?: return ParseOutcome.Failure("Use an implicit relation like x^2 + y^2 = 1.")
        val parts = source.split(operator, limit = 2).map { it.trim() }
        if (parts.size != 2 || parts.any { it.isBlank() }) return ParseOutcome.Failure("Both sides of the relation are required.")
        val left = parser.parse(parts[0])
        val right = parser.parse(parts[1])
        if (left !is ParseOutcome.Success) return left
        if (right !is ParseOutcome.Success) return right
        return ParseOutcome.Success(left.expression, left.variables + right.variables)
    }

    fun analyze(
        compiled: ArCompiledExpression,
        focusX: Double = (compiled.domain.xMin + compiled.domain.xMax) / 2.0,
        comparison: ArCompiledExpression? = null
    ): ArGraphAnalysisReport {
        if (!compiled.isValid) return ArGraphAnalysisReport(warnings = listOf(compiled.message))
        val samples = compiled.qualityPreset.curveSamples.coerceIn(180, 960)
        val roots = findZeroCrossings(compiled, samples) { x -> evaluate2dRaw(compiled, x) }
            .mapIndexed { index, point -> point.toAnalysisPoint("r${index + 1}", ArGraphAnalysisPointType.Root) }
        val intersections = comparison
            ?.takeIf { it.isValid }
            ?.let { other ->
                findZeroCrossings(compiled, samples) { x -> evaluate2dRaw(compiled, x) - evaluate2dRaw(other, x) }
                    .mapIndexed { index, point -> point.toAnalysisPoint("i${index + 1}", ArGraphAnalysisPointType.Intersection) }
            }
            ?: emptyList()
        val extrema = findDerivativeCrossings(compiled, samples).mapIndexed { index, x ->
            val second = secondDerivative(compiled, x)
            val type = if (second >= 0.0) ArGraphAnalysisPointType.Minimum else ArGraphAnalysisPointType.Maximum
            ArGraphAnalysisPoint(x, evaluate2dRaw(compiled, x), if (type == ArGraphAnalysisPointType.Minimum) "min${index + 1}" else "max${index + 1}", type)
        }
        val inflections = findSecondDerivativeCrossings(compiled, samples).mapIndexed { index, x ->
            ArGraphAnalysisPoint(x, evaluate2dRaw(compiled, x), "infl${index + 1}", ArGraphAnalysisPointType.Inflection)
        }
        val safeFocus = focusX.coerceIn(compiled.domain.xMin, compiled.domain.xMax)
        val y = evaluate2dRaw(compiled, safeFocus)
        val slope = derivative(compiled, safeFocus)
        val tangent = if (y.isFinite() && slope.isFinite()) {
            ArGraphLineAnalysis(
                point = GraphPoint(safeFocus, y),
                tangentSlope = slope,
                normalSlope = if (abs(slope) < 1e-9) Double.POSITIVE_INFINITY else -1.0 / slope,
                tangentAngleDegrees = Math.toDegrees(kotlin.math.atan(slope))
            )
        } else {
            null
        }
        val integral = integrate(compiled, compiled.domain.xMin, compiled.domain.xMax, samples)
        val warnings = buildList {
            if (roots.size >= 12) add("Many roots detected; showing the strongest visible crossings.")
            if (comparison != null && !comparison.isValid) add("Comparison equation could not be analyzed.")
            if (compiled.kind == GraphExpressionKind.ExplicitSurface3D) add("Surface analysis uses the y=0 cross-section.")
        }
        return ArGraphAnalysisReport(
            roots = roots.take(12),
            intersections = intersections.take(12),
            extrema = extrema.take(10),
            inflections = inflections.take(10),
            tangent = tangent,
            integral = integral,
            warnings = warnings
        )
    }

    private fun sampleCurveFromSurface(compiled: ArCompiledExpression): GraphCurve {
        val samples = compiled.qualityPreset.curveSamples.coerceAtMost(360)
        val points = (0..samples).mapNotNull { index ->
            val x = compiled.domain.xMin + (compiled.domain.xMax - compiled.domain.xMin) * index / samples
            val z = evaluate3d(compiled, x, 0.0)
            if (z.isFinite()) GraphPoint(x, z) else null
        }
        return GraphCurve("ar-live-expression", points)
    }

    private fun sampleParametricCurve(compiled: ArCompiledExpression): GraphCurve {
        val parts = normalizeParametricSource(compiled.normalizedSource)
            .split(",")
            .map { it.substringAfter("=").trim() }
            .filter { it.isNotBlank() }
        if (parts.size < 2) return GraphCurve("ar-live-expression", emptyList(), listOf("Use x=..., y=... parametric form."))
        val xParse = parser.parse(parts[0]) as? ParseOutcome.Success
            ?: return GraphCurve("ar-live-expression", emptyList(), listOf("Parametric x(t) could not be parsed."))
        val yParse = parser.parse(parts[1]) as? ParseOutcome.Success
            ?: return GraphCurve("ar-live-expression", emptyList(), listOf("Parametric y(t) could not be parsed."))
        val samples = compiled.qualityPreset.curveSamples.coerceIn(120, 960)
        val baseVariables = variables(compiled.parameters)
        val points = (0..samples).mapNotNull { index ->
            val t = compiled.domain.xMin + (compiled.domain.xMax - compiled.domain.xMin) * index / samples
            val vars = baseVariables + mapOf("t" to t)
            val x = evaluator.evaluate(xParse.expression, vars).value
            val y = evaluator.evaluate(yParse.expression, vars).value
            if (x != null && y != null && x.isFinite() && y.isFinite()) {
                GraphPoint(x.coerceIn(compiled.domain.xMin, compiled.domain.xMax), y.coerceIn(compiled.domain.yMin, compiled.domain.yMax))
            } else {
                null
            }
        }
        return GraphCurve("ar-live-expression", points, if (points.isEmpty()) listOf("No visible parametric samples.") else emptyList())
    }

    private fun findZeroCrossings(compiled: ArCompiledExpression, samples: Int, function: (Double) -> Double): List<GraphPoint> {
        val points = mutableListOf<GraphPoint>()
        var previousX = compiled.domain.xMin
        var previousY = function(previousX)
        if (previousY.isFinite() && abs(previousY) < 1e-7) points += GraphPoint(previousX, evaluate2dRaw(compiled, previousX))
        for (i in 1..samples) {
            val x = compiled.domain.xMin + (compiled.domain.xMax - compiled.domain.xMin) * i / samples
            val y = function(x)
            if (previousY.isFinite() && y.isFinite()) {
                when {
                    abs(y) < 1e-7 -> points += GraphPoint(x, evaluate2dRaw(compiled, x))
                    sign(previousY) != sign(y) -> {
                        val root = bisect(previousX, x, function)
                        points += GraphPoint(root, evaluate2dRaw(compiled, root))
                    }
                }
            }
            previousX = x
            previousY = y
        }
        return points.distinctBy { "%.4f".format(it.x) }
    }

    private fun findDerivativeCrossings(compiled: ArCompiledExpression, samples: Int): List<Double> =
        findDerivativeRoots(compiled, samples) { x -> derivative(compiled, x) }

    private fun findSecondDerivativeCrossings(compiled: ArCompiledExpression, samples: Int): List<Double> =
        findDerivativeRoots(compiled, samples) { x -> secondDerivative(compiled, x) }

    private fun findDerivativeRoots(compiled: ArCompiledExpression, samples: Int, function: (Double) -> Double): List<Double> {
        val roots = mutableListOf<Double>()
        var previousX = compiled.domain.xMin
        var previous = function(previousX)
        for (i in 1..samples) {
            val x = compiled.domain.xMin + (compiled.domain.xMax - compiled.domain.xMin) * i / samples
            val current = function(x)
            if (previous.isFinite() && current.isFinite() && sign(previous) != sign(current)) {
                roots += bisect(previousX, x, function)
            }
            previousX = x
            previous = current
        }
        return roots.distinctBy { "%.3f".format(it) }
    }

    private fun derivative(compiled: ArCompiledExpression, x: Double): Double {
        val h = 1e-5 * max(1.0, abs(x))
        val left = evaluate2dRaw(compiled, x - h)
        val right = evaluate2dRaw(compiled, x + h)
        return if (left.isFinite() && right.isFinite()) (right - left) / (2.0 * h) else Double.NaN
    }

    private fun secondDerivative(compiled: ArCompiledExpression, x: Double): Double {
        val h = 1e-4 * max(1.0, abs(x))
        val left = evaluate2dRaw(compiled, x - h)
        val center = evaluate2dRaw(compiled, x)
        val right = evaluate2dRaw(compiled, x + h)
        return if (left.isFinite() && center.isFinite() && right.isFinite()) (right - 2.0 * center + left) / (h * h) else Double.NaN
    }

    private fun integrate(compiled: ArCompiledExpression, lower: Double, upper: Double, intervalsRaw: Int): ArGraphIntegralAnalysis {
        val intervals = intervalsRaw.coerceIn(120, 960).let { if (it % 2 == 0) it else it + 1 }
        val h = (upper - lower) / intervals
        var signed = 0.0
        var absolute = 0.0
        var volume = 0.0
        var length = 0.0
        var previousX = lower
        var previousY = evaluate2dRaw(compiled, lower)
        for (i in 0 until intervals) {
            val x0 = lower + i * h
            val x1 = x0 + h
            val mid = (x0 + x1) / 2.0
            val y0 = evaluate2dRaw(compiled, x0)
            val ym = evaluate2dRaw(compiled, mid)
            val y1 = evaluate2dRaw(compiled, x1)
            if (y0.isFinite() && ym.isFinite() && y1.isFinite()) {
                signed += (y0 + 4.0 * ym + y1) * h / 6.0
                absolute += (abs(y0) + 4.0 * abs(ym) + abs(y1)) * h / 6.0
                volume += PI * (y0 * y0 + 4.0 * ym * ym + y1 * y1) * h / 6.0
            }
            if (previousY.isFinite() && y1.isFinite()) {
                length += sqrt((x1 - previousX) * (x1 - previousX) + (y1 - previousY) * (y1 - previousY))
            }
            previousX = x1
            previousY = y1
        }
        return ArGraphIntegralAnalysis(lower, upper, signed, absolute, volume, length)
    }

    private fun bisect(aRaw: Double, bRaw: Double, function: (Double) -> Double): Double {
        var a = aRaw
        var b = bRaw
        var fa = function(a)
        repeat(52) {
            val mid = (a + b) / 2.0
            val fm = function(mid)
            if (!fm.isFinite()) return@repeat
            if (sign(fa) == sign(fm)) {
                a = mid
                fa = fm
            } else {
                b = mid
            }
        }
        return (a + b) / 2.0
    }

    private fun GraphPoint.toAnalysisPoint(label: String, type: ArGraphAnalysisPointType): ArGraphAnalysisPoint =
        ArGraphAnalysisPoint(x, y, label, type)

    private fun buildParameterSliders(variables: Set<String>, existing: List<GraphSlider>): List<GraphSlider> {
        val existingBySymbol = existing.associateBy { it.symbol }
        return variables
            .filterNot { it in reservedSymbols }
            .sorted()
            .take(8)
            .map { symbol ->
                existingBySymbol[symbol] ?: defaultParameter(symbol).toSlider()
            }
    }

    private fun defaultParameter(symbol: String): ArGraphParameter =
        when (symbol) {
            "a" -> ArGraphParameter(symbol, -5.0, 5.0, 0.1, 1.0)
            "b" -> ArGraphParameter(symbol, -10.0, 10.0, 0.1, 1.0)
            "c" -> ArGraphParameter(symbol, -6.283185307179586, 6.283185307179586, 0.05, 0.0)
            "d" -> ArGraphParameter(symbol, -5.0, 5.0, 0.1, 0.0)
            else -> ArGraphParameter(symbol)
        }

    private fun detectKind(source: String): GraphExpressionKind {
        val left = source.substringBefore("=").trim().lowercase()
        return when {
            source.contains("<=") || source.contains(">=") || source.contains("<") || source.contains(">") -> GraphExpressionKind.Inequality2D
            source.looksSpaceCurve() -> GraphExpressionKind.SpaceCurve
            source.looksParametric2D() -> GraphExpressionKind.Parametric2D
            source.contains("=") && left !in setOf("y", "z", "r") -> GraphExpressionKind.Implicit2D
            left == "z" || source.contains("y") && source.contains("x") && !source.startsWith("y") -> GraphExpressionKind.ExplicitSurface3D
            left == "r" -> GraphExpressionKind.Polar
            else -> GraphExpressionKind.Explicit2D
        }
    }

    private fun variables(sliders: List<GraphSlider>): Map<String, Double> = defaultVariables(sliders)

    companion object {
        val reservedSymbols = setOf(
            "x", "y", "z", "t", "theta", "r", "pi", "e", "phi",
            "sin", "cos", "tan", "sec", "csc", "cot", "asin", "acos", "atan", "atan2",
            "sinh", "cosh", "tanh", "sqrt", "cbrt", "abs", "exp", "ln", "log",
            "floor", "ceil", "round", "sign", "min", "max", "mod", "pow", "root",
            "rad", "deg", "clamp", "sum", "product", "gcd", "lcm", "ncr", "npr"
        )
    }
}

val GraphExpressionKind.label: String
    get() = when (this) {
        GraphExpressionKind.Explicit2D -> "2D function"
        GraphExpressionKind.Polar -> "polar function"
        GraphExpressionKind.Parametric2D -> "parametric curve"
        GraphExpressionKind.Implicit2D -> "implicit curve"
        GraphExpressionKind.Inequality2D -> "inequality"
        GraphExpressionKind.Points -> "points"
        GraphExpressionKind.Sequence -> "sequence"
        GraphExpressionKind.DataTable -> "data table"
        GraphExpressionKind.ExplicitSurface3D -> "3D surface"
        GraphExpressionKind.ParametricSurface3D -> "parametric surface"
        GraphExpressionKind.SpaceCurve -> "space curve"
        GraphExpressionKind.Vector -> "vector"
        GraphExpressionKind.VectorField -> "vector field"
    }

fun normalizeArFormula(source: String): String =
    source
        .replace('−', '-')
        .replace('–', '-')
        .replace('×', '*')
        .replace('·', '*')
        .replace('÷', '/')
        .replace("π", "pi")
        .replace("θ", "theta")
        .replace("²", "^2")
        .replace("³", "^3")
        .trim()
        .normalizeStudentMathSyntax()

private fun String.normalizeStudentMathSyntax(): String =
    replace('−', '-')
        .replace('–', '-')
        .replace('×', '*')
        .replace('·', '*')
        .replace('÷', '/')
        .replace("π", "pi")
        .replace("θ", "theta")
        .replace("²", "^2")
        .replace("³", "^3")
        .replace("ˣ", "^x")
        .replace(Regex("""√\s*([A-Za-z0-9_.]+)"""), "sqrt($1)")
        .replace(Regex("""\|([^|]+)\|"""), "abs($1)")
        .insertImplicitMultiplication()

private fun String.insertImplicitMultiplication(): String =
    replace(Regex("""(?<=[0-9)])(?=[A-Za-z(])"""), "*")
        .replace(Regex("""(?<=[A-Za-z)])(?=[0-9])"""), "*")

private fun String.looksParametric2D(): Boolean {
    val normalized = lowercase().replace(" ", "")
    return normalized.contains(",") && normalized.contains("x=") && normalized.contains("y=")
}

private fun String.looksSpaceCurve(): Boolean {
    val normalized = lowercase().replace(" ", "")
    return normalized.contains(",") && normalized.contains("x=") && normalized.contains("y=") && normalized.contains("z=")
}

private fun normalizeParametricSource(source: String): String =
    source
        .removePrefix("param:")
        .removePrefix("curve:")
        .removePrefix("space:")
        .trim()

private fun Double.coerceFinite(limit: Double): Double =
    if (isFinite()) coerceIn(-abs(limit), abs(limit)) else Double.NaN
