package com.indianservers.ai_stem.domain.mathematics

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class MathObjectType(val displayName: String) {
    CoordinatePlane("Coordinate Plane"),
    Cube("Cube"),
    SineCurve("Sine Curve"),
    Sphere("Sphere"),
    Cylinder("Cylinder"),
    Cone("Cone"),
    RectangularPrism("Rectangular Prism"),
    Triangle("Triangle"),
    Circle("Circle"),
    NumberLine("Number Line"),
    VectorArrow("Vector Arrow")
}

enum class MathObjectCategory(val displayName: String) {
    Graphs("Graphs"),
    Shapes("Shapes"),
    Solids("Solids"),
    Coordinates("Coordinates"),
    Vectors("Vectors"),
    NumberTools("Number Tools")
}

enum class MeasurementUnit(val label: String, val meters: Double) {
    Millimetres("mm", 0.001),
    Centimetres("cm", 0.01),
    Metres("m", 1.0),
    Inches("in", 0.0254),
    Feet("ft", 0.3048)
}

sealed interface MathParameterValue {
    data class NumberValue(val value: Double) : MathParameterValue
    data class IntegerValue(val value: Int) : MathParameterValue
    data class BooleanValue(val value: Boolean) : MathParameterValue
    data class TextValue(val value: String) : MathParameterValue
    data class ChoiceValue(val value: String) : MathParameterValue
}

sealed interface MathParameterDefinition {
    val id: String
    val label: String
    val description: String?
    val defaultValue: MathParameterValue
}

data class NumberParameterDefinition(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val minimum: Double,
    val maximum: Double,
    override val defaultValue: MathParameterValue.NumberValue
) : MathParameterDefinition

data class IntegerParameterDefinition(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val minimum: Int,
    val maximum: Int,
    override val defaultValue: MathParameterValue.IntegerValue
) : MathParameterDefinition

data class BooleanParameterDefinition(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    override val defaultValue: MathParameterValue.BooleanValue
) : MathParameterDefinition

data class ChoiceParameterDefinition(
    override val id: String,
    override val label: String,
    override val description: String? = null,
    val choices: List<String>,
    override val defaultValue: MathParameterValue.ChoiceValue
) : MathParameterDefinition

data class CalculatedMathProperty(
    val id: String,
    val label: String,
    val value: Double,
    val unitLabel: String? = null,
    val formula: String? = null
)

data class MathObjectDefinition(
    val definitionId: String,
    val type: MathObjectType,
    val displayName: String,
    val category: MathObjectCategory,
    val description: String,
    val supportedParameters: List<MathParameterDefinition>,
    val defaultScaleMeters: Float,
    val minimumScaleFactor: Float = 0.15f,
    val maximumScaleFactor: Float = 5f,
    val measurementProvider: (Map<String, MathParameterValue>) -> List<CalculatedMathProperty>
) {
    val defaultParameters: Map<String, MathParameterValue> =
        supportedParameters.associate { it.id to it.defaultValue }

    fun validate(parameters: Map<String, MathParameterValue>): List<String> =
        supportedParameters.mapNotNull { definition ->
            val value = parameters[definition.id] ?: definition.defaultValue
            validateParameter(definition, value)
        }
}

interface MathObjectRegistry {
    fun getDefinition(definitionId: String): MathObjectDefinition?
    fun getAllDefinitions(): List<MathObjectDefinition>
    fun getDefinitionsByCategory(category: MathObjectCategory): List<MathObjectDefinition>
}

object DefaultMathObjectRegistry : MathObjectRegistry {
    private val definitions = listOf(
        coordinatePlaneDefinition(),
        cubeDefinition(),
        sineCurveDefinition(),
        sphereDefinition(),
        cylinderDefinition(),
        coneDefinition(),
        rectangularPrismDefinition(),
        triangleDefinition(),
        circleDefinition(),
        numberLineDefinition(),
        vectorArrowDefinition()
    )

    override fun getDefinition(definitionId: String): MathObjectDefinition? =
        definitions.firstOrNull { it.definitionId == definitionId }

    override fun getAllDefinitions(): List<MathObjectDefinition> = definitions

    override fun getDefinitionsByCategory(category: MathObjectCategory): List<MathObjectDefinition> =
        definitions.filter { it.category == category }
}

object MathematicsCatalogue {
    val phaseOneObjects = DefaultMathObjectRegistry.getAllDefinitions()
    val phaseTwoObjects = phaseOneObjects
}

data class CurveSample(val x: Float, val y: Float)

object SineCurveSampler {
    const val DEFAULT_SEGMENTS = 96
    val minX: Float = (-2.0 * PI).toFloat()
    val maxX: Float = (2.0 * PI).toFloat()

    fun sample(
        segments: Int = DEFAULT_SEGMENTS,
        amplitude: Float = 1f,
        frequency: Float = 1f,
        phaseShift: Float = 0f,
        verticalShift: Float = 0f,
        rangeMin: Float = minX,
        rangeMax: Float = maxX
    ): List<CurveSample> {
        require(segments >= 8) { "Sine curve requires at least 8 segments." }
        require(rangeMax > rangeMin) { "Sine curve maximum X must be greater than minimum X." }
        val step = (rangeMax - rangeMin) / segments
        return (0..segments).map { index ->
            val x = rangeMin + step * index
            CurveSample(x = x, y = amplitude * sin(frequency * x + phaseShift) + verticalShift)
        }
    }
}

object MeasurementFormatter {
    fun length(meters: Double, unit: MeasurementUnit = MeasurementUnit.Centimetres): String {
        val value = meters / unit.meters
        return "${number(value)} ${unit.label}"
    }

    fun number(value: Double): String =
        when {
            value.isNaN() || value.isInfinite() -> "Not available"
            kotlin.math.abs(value) >= 100 -> "%.0f".format(value)
            kotlin.math.abs(value) >= 10 -> "%.1f".format(value)
            else -> "%.2f".format(value)
        }.trimEnd('0').trimEnd('.')
}

fun clampScale(value: Float, min: Float = 0.25f, max: Float = 4f): Float =
    when {
        value.isNaN() || value.isInfinite() -> 1f
        value < min -> min
        value > max -> max
        else -> value
    }

fun normalizeRotationDegrees(value: Float): Float {
    if (value.isNaN() || value.isInfinite()) return 0f
    val mod = value % 360f
    return if (mod < 0f) mod + 360f else mod
}

fun parameterNumber(parameters: Map<String, MathParameterValue>, id: String, fallback: Double): Double =
    (parameters[id] as? MathParameterValue.NumberValue)?.value ?: fallback

fun validateParameter(definition: MathParameterDefinition, value: MathParameterValue): String? =
    when (definition) {
        is NumberParameterDefinition -> {
            val number = (value as? MathParameterValue.NumberValue)?.value
            when {
                number == null -> "${definition.label} must be a number."
                number.isNaN() || number.isInfinite() -> "${definition.label} must be finite."
                number < definition.minimum || number > definition.maximum ->
                    "${definition.label} must be between ${definition.minimum} and ${definition.maximum}."
                else -> null
            }
        }
        is IntegerParameterDefinition -> {
            val number = (value as? MathParameterValue.IntegerValue)?.value
            when {
                number == null -> "${definition.label} must be a whole number."
                number < definition.minimum || number > definition.maximum ->
                    "${definition.label} must be between ${definition.minimum} and ${definition.maximum}."
                else -> null
            }
        }
        is BooleanParameterDefinition -> if (value is MathParameterValue.BooleanValue) null else "${definition.label} must be on or off."
        is ChoiceParameterDefinition -> {
            val choice = (value as? MathParameterValue.ChoiceValue)?.value
            if (choice in definition.choices) null else "${definition.label} is not a supported choice."
        }
    }

private fun lengthParam(id: String, label: String, default: Double, min: Double = 0.01, max: Double = 5.0) =
    NumberParameterDefinition(id, label, minimum = min, maximum = max, defaultValue = MathParameterValue.NumberValue(default))

private fun scalarParam(id: String, label: String, default: Double, min: Double = -100.0, max: Double = 100.0) =
    NumberParameterDefinition(id, label, minimum = min, maximum = max, defaultValue = MathParameterValue.NumberValue(default))

private fun coordinatePlaneDefinition() = MathObjectDefinition(
    definitionId = "coordinate-plane",
    type = MathObjectType.CoordinatePlane,
    displayName = "Coordinate Plane",
    category = MathObjectCategory.Coordinates,
    description = "Grid, axes and a point on the plane.",
    supportedParameters = listOf(
        scalarParam("xMin", "X minimum", -5.0),
        scalarParam("xMax", "X maximum", 5.0),
        scalarParam("yMin", "Y minimum", -5.0),
        scalarParam("yMax", "Y maximum", 5.0),
        lengthParam("gridInterval", "Grid interval", 1.0, 0.1, 10.0),
        scalarParam("pointX", "Point X", 2.0),
        scalarParam("pointY", "Point Y", 2.0)
    ),
    defaultScaleMeters = 0.35f
) { p ->
    val x = parameterNumber(p, "pointX", 2.0)
    val y = parameterNumber(p, "pointY", 2.0)
    val slope = if (x == 0.0) Double.NaN else y / x
    listOf(
        CalculatedMathProperty("distance", "Distance from origin", sqrt(x * x + y * y)),
        CalculatedMathProperty("quadrant", "Quadrant", quadrantOf(x, y).toDouble()),
        CalculatedMathProperty("slope", "Slope from origin", slope)
    )
}

private fun cubeDefinition() = MathObjectDefinition(
    definitionId = "cube",
    type = MathObjectType.Cube,
    displayName = "Cube",
    category = MathObjectCategory.Solids,
    description = "A solid with six equal square faces.",
    supportedParameters = listOf(lengthParam("sideLength", "Side length", 0.24)),
    defaultScaleMeters = 0.24f
) { p ->
    val a = parameterNumber(p, "sideLength", 0.24)
    listOf(
        CalculatedMathProperty("surfaceArea", "Surface area", 6 * a * a, "m²", "Surface Area = 6a²"),
        CalculatedMathProperty("volume", "Volume", a.pow(3.0), "m³", "Volume = a³"),
        CalculatedMathProperty("faces", "Face count", 6.0),
        CalculatedMathProperty("edges", "Edge count", 12.0),
        CalculatedMathProperty("vertices", "Vertex count", 8.0),
        CalculatedMathProperty("diagonal", "Space diagonal", a * sqrt(3.0), "m", "Space Diagonal = a√3")
    )
}

private fun sphereDefinition() = MathObjectDefinition(
    "sphere", MathObjectType.Sphere, "Sphere", MathObjectCategory.Solids,
    "A perfectly round solid.", listOf(lengthParam("radius", "Radius", 0.16)), 0.32f
) { p ->
    val r = parameterNumber(p, "radius", 0.16)
    listOf(
        CalculatedMathProperty("diameter", "Diameter", 2 * r, "m", "Diameter = 2r"),
        CalculatedMathProperty("circumference", "Circumference", 2 * PI * r, "m", "Circumference = 2πr"),
        CalculatedMathProperty("surfaceArea", "Surface area", 4 * PI * r * r, "m²", "Surface Area = 4πr²"),
        CalculatedMathProperty("volume", "Volume", 4.0 / 3.0 * PI * r.pow(3.0), "m³", "Volume = 4/3 πr³")
    )
}

private fun cylinderDefinition() = MathObjectDefinition(
    "cylinder", MathObjectType.Cylinder, "Cylinder", MathObjectCategory.Solids,
    "A solid with circular top and bottom.", listOf(lengthParam("radius", "Radius", 0.12), lengthParam("height", "Height", 0.32)), 0.32f
) { p ->
    val r = parameterNumber(p, "radius", 0.12)
    val h = parameterNumber(p, "height", 0.32)
    listOf(
        CalculatedMathProperty("diameter", "Diameter", 2 * r, "m"),
        CalculatedMathProperty("curvedSurfaceArea", "Curved surface area", 2 * PI * r * h, "m²", "Curved Surface Area = 2πrh"),
        CalculatedMathProperty("surfaceArea", "Total surface area", 2 * PI * r * (r + h), "m²", "Total Surface Area = 2πr(r + h)"),
        CalculatedMathProperty("volume", "Volume", PI * r * r * h, "m³", "Volume = πr²h")
    )
}

private fun coneDefinition() = MathObjectDefinition(
    "cone", MathObjectType.Cone, "Cone", MathObjectCategory.Solids,
    "A solid with a circular base and one point.", listOf(lengthParam("radius", "Radius", 0.12), lengthParam("height", "Height", 0.32)), 0.32f
) { p ->
    val r = parameterNumber(p, "radius", 0.12)
    val h = parameterNumber(p, "height", 0.32)
    val l = sqrt(r * r + h * h)
    listOf(
        CalculatedMathProperty("slantHeight", "Slant height", l, "m", "Slant Height = √(r² + h²)"),
        CalculatedMathProperty("curvedSurfaceArea", "Curved surface area", PI * r * l, "m²", "Curved Surface Area = πrl"),
        CalculatedMathProperty("surfaceArea", "Total surface area", PI * r * (r + l), "m²", "Total Surface Area = πr(r + l)"),
        CalculatedMathProperty("volume", "Volume", PI * r * r * h / 3.0, "m³", "Volume = 1/3 πr²h")
    )
}

private fun rectangularPrismDefinition() = MathObjectDefinition(
    "rectangular-prism", MathObjectType.RectangularPrism, "Rectangular Prism", MathObjectCategory.Solids,
    "A box-shaped solid.", listOf(lengthParam("length", "Length", 0.32), lengthParam("width", "Width", 0.18), lengthParam("height", "Height", 0.22)), 0.32f
) { p ->
    val l = parameterNumber(p, "length", 0.32)
    val w = parameterNumber(p, "width", 0.18)
    val h = parameterNumber(p, "height", 0.22)
    listOf(
        CalculatedMathProperty("surfaceArea", "Surface area", 2 * (l * w + l * h + w * h), "m²"),
        CalculatedMathProperty("volume", "Volume", l * w * h, "m³"),
        CalculatedMathProperty("diagonal", "Space diagonal", sqrt(l * l + w * w + h * h), "m")
    )
}

private fun triangleDefinition() = MathObjectDefinition(
    "triangle", MathObjectType.Triangle, "Triangle", MathObjectCategory.Shapes,
    "A triangle controlled by three side lengths.", listOf(lengthParam("sideA", "Side A", 0.3), lengthParam("sideB", "Side B", 0.25), lengthParam("sideC", "Side C", 0.22)), 0.3f
) { p ->
    val a = parameterNumber(p, "sideA", 0.3)
    val b = parameterNumber(p, "sideB", 0.25)
    val c = parameterNumber(p, "sideC", 0.22)
    if (!isValidTriangle(a, b, c)) return@MathObjectDefinition listOf(CalculatedMathProperty("invalid", "Invalid triangle", Double.NaN))
    val s = (a + b + c) / 2.0
    listOf(
        CalculatedMathProperty("perimeter", "Perimeter", a + b + c, "m"),
        CalculatedMathProperty("area", "Area", sqrt(s * (s - a) * (s - b) * (s - c)), "m²"),
        CalculatedMathProperty("angleA", "Angle A", triangleAngle(b, c, a), "°"),
        CalculatedMathProperty("angleB", "Angle B", triangleAngle(a, c, b), "°"),
        CalculatedMathProperty("angleC", "Angle C", triangleAngle(a, b, c), "°")
    )
}

private fun circleDefinition() = MathObjectDefinition(
    "circle", MathObjectType.Circle, "Circle", MathObjectCategory.Shapes,
    "A flat circle with radius, diameter and area.", listOf(lengthParam("radius", "Radius", 0.18)), 0.36f
) { p ->
    val r = parameterNumber(p, "radius", 0.18)
    listOf(
        CalculatedMathProperty("diameter", "Diameter", 2 * r, "m"),
        CalculatedMathProperty("circumference", "Circumference", 2 * PI * r, "m"),
        CalculatedMathProperty("area", "Area", PI * r * r, "m²")
    )
}

private fun numberLineDefinition() = MathObjectDefinition(
    "number-line", MathObjectType.NumberLine, "Number Line", MathObjectCategory.NumberTools,
    "A straight number scale with ticks.", listOf(scalarParam("minimum", "Minimum", -5.0), scalarParam("maximum", "Maximum", 5.0), scalarParam("step", "Step interval", 1.0, 0.1, 20.0)), 0.5f
) { p ->
    val min = parameterNumber(p, "minimum", -5.0)
    val max = parameterNumber(p, "maximum", 5.0)
    val step = parameterNumber(p, "step", 1.0)
    val range = max - min
    listOf(
        CalculatedMathProperty("range", "Visible range", range),
        CalculatedMathProperty("ticks", "Tick count", if (range > 0) kotlin.math.floor(range / step) + 1 else 0.0)
    )
}

private fun vectorArrowDefinition() = MathObjectDefinition(
    "vector-arrow", MathObjectType.VectorArrow, "Vector Arrow", MathObjectCategory.Vectors,
    "An arrow described by X, Y and Z components.", listOf(scalarParam("x", "X", 1.0), scalarParam("y", "Y", 1.0), scalarParam("z", "Z", 0.0)), 0.35f
) { p ->
    val x = parameterNumber(p, "x", 1.0)
    val y = parameterNumber(p, "y", 1.0)
    val z = parameterNumber(p, "z", 0.0)
    val magnitude = sqrt(x * x + y * y + z * z)
    listOf(
        CalculatedMathProperty("magnitude", "Magnitude", magnitude),
        CalculatedMathProperty("unitX", "Unit X", if (magnitude == 0.0) Double.NaN else x / magnitude),
        CalculatedMathProperty("unitY", "Unit Y", if (magnitude == 0.0) Double.NaN else y / magnitude),
        CalculatedMathProperty("unitZ", "Unit Z", if (magnitude == 0.0) Double.NaN else z / magnitude),
        CalculatedMathProperty("directionXY", "Direction angle", atan2(y, x) * 180.0 / PI, "°")
    )
}

private fun sineCurveDefinition() = MathObjectDefinition(
    "sine-curve", MathObjectType.SineCurve, "Sine Curve", MathObjectCategory.Graphs,
    "A graph of y = A sin(Bx + C) + D.", listOf(
        scalarParam("amplitude", "Amplitude A", 1.0, -20.0, 20.0),
        scalarParam("frequency", "Frequency B", 1.0, 0.05, 20.0),
        scalarParam("phaseShift", "Phase shift C", 0.0),
        scalarParam("verticalShift", "Vertical shift D", 0.0),
        scalarParam("minimumX", "Minimum X", -2 * PI),
        scalarParam("maximumX", "Maximum X", 2 * PI)
    ),
    defaultScaleMeters = 0.42f
) { p ->
    val a = parameterNumber(p, "amplitude", 1.0)
    val b = parameterNumber(p, "frequency", 1.0)
    val c = parameterNumber(p, "phaseShift", 0.0)
    val d = parameterNumber(p, "verticalShift", 0.0)
    listOf(
        CalculatedMathProperty("amplitude", "Amplitude", kotlin.math.abs(a)),
        CalculatedMathProperty("period", "Period", 2 * PI / b),
        CalculatedMathProperty("phaseShift", "Phase shift", -c / b),
        CalculatedMathProperty("midline", "Midline", d),
        CalculatedMathProperty("maximum", "Maximum", d + kotlin.math.abs(a)),
        CalculatedMathProperty("minimum", "Minimum", d - kotlin.math.abs(a))
    )
}

fun isValidTriangle(a: Double, b: Double, c: Double): Boolean =
    a > 0 && b > 0 && c > 0 && a + b > c && a + c > b && b + c > a

private fun triangleAngle(side1: Double, side2: Double, opposite: Double): Double =
    acos(((side1 * side1 + side2 * side2 - opposite * opposite) / (2 * side1 * side2)).coerceIn(-1.0, 1.0)) * 180.0 / PI

private fun quadrantOf(x: Double, y: Double): Int =
    when {
        x > 0 && y > 0 -> 1
        x < 0 && y > 0 -> 2
        x < 0 && y < 0 -> 3
        x > 0 && y < 0 -> 4
        else -> 0
    }
