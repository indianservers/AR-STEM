package com.indianservers.ai_stem.domain.mathematics

import kotlin.math.PI
import kotlin.math.sin

enum class MathObjectType(val displayName: String) {
    CoordinatePlane("Coordinate Plane"),
    Cube("Cube"),
    SineCurve("Sine Curve")
}

data class MathObjectDefinition(
    val id: String,
    val type: MathObjectType,
    val displayName: String,
    val description: String,
    val defaultScaleMeters: Float,
    val minimumScaleFactor: Float = 0.25f,
    val maximumScaleFactor: Float = 4f
)

object MathematicsCatalogue {
    val phaseOneObjects = listOf(
        MathObjectDefinition(
            id = "coordinate-plane",
            type = MathObjectType.CoordinatePlane,
            displayName = "Coordinate Plane",
            description = "X-axis, Y-axis, grid, ticks, labels and point (2, 2)",
            defaultScaleMeters = 0.35f
        ),
        MathObjectDefinition(
            id = "cube",
            type = MathObjectType.Cube,
            displayName = "Cube",
            description = "6 faces, 12 edges, 8 vertices",
            defaultScaleMeters = 0.24f
        ),
        MathObjectDefinition(
            id = "sine-curve",
            type = MathObjectType.SineCurve,
            displayName = "Sine Curve",
            description = "y = sin(x), -2pi <= x <= 2pi",
            defaultScaleMeters = 0.42f
        )
    )
}

data class CurveSample(val x: Float, val y: Float)

object SineCurveSampler {
    const val DEFAULT_SEGMENTS = 96
    val minX: Float = (-2.0 * PI).toFloat()
    val maxX: Float = (2.0 * PI).toFloat()

    fun sample(segments: Int = DEFAULT_SEGMENTS): List<CurveSample> {
        require(segments >= 8) { "Sine curve requires at least 8 segments." }
        val step = (maxX - minX) / segments
        return (0..segments).map { index ->
            val x = minX + step * index
            CurveSample(x = x, y = sin(x))
        }
    }
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
