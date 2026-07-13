package com.indianservers.ai_stem.domain.statistics

import kotlin.math.pow
import kotlin.math.sqrt

data class DescriptiveStatistics(
    val count: Int,
    val mean: Double,
    val median: Double,
    val modes: List<Double>,
    val range: Double,
    val variance: Double,
    val standardDeviation: Double,
    val q1: Double,
    val q3: Double,
    val interquartileRange: Double
)

data class LinearRegressionResult(
    val slope: Double,
    val intercept: Double,
    val r: Double,
    val rSquared: Double,
    val equation: String
)

data class HistogramBin(val lower: Double, val upper: Double, val count: Int)

object StatisticsEngine {
    fun describe(values: List<Double>): DescriptiveStatistics {
        require(values.isNotEmpty()) { "Enter at least one value." }
        val sorted = values.sorted()
        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2) } / values.size
        val counts = values.groupingBy { it }.eachCount()
        val maxCount = counts.values.max()
        val modes = counts.filterValues { it == maxCount && maxCount > 1 }.keys.sorted()
        val q1 = percentile(sorted, 25.0)
        val q3 = percentile(sorted, 75.0)
        return DescriptiveStatistics(
            count = values.size,
            mean = mean,
            median = percentile(sorted, 50.0),
            modes = modes,
            range = sorted.last() - sorted.first(),
            variance = variance,
            standardDeviation = sqrt(variance),
            q1 = q1,
            q3 = q3,
            interquartileRange = q3 - q1
        )
    }

    fun zScore(value: Double, values: List<Double>): Double {
        val stats = describe(values)
        return (value - stats.mean) / stats.standardDeviation
    }

    fun linearRegression(points: List<Pair<Double, Double>>): LinearRegressionResult {
        require(points.size >= 2) { "Use at least two points." }
        val meanX = points.map { it.first }.average()
        val meanY = points.map { it.second }.average()
        val sxx = points.sumOf { (it.first - meanX).pow(2) }
        val syy = points.sumOf { (it.second - meanY).pow(2) }
        val sxy = points.sumOf { (it.first - meanX) * (it.second - meanY) }
        val slope = sxy / sxx
        val intercept = meanY - slope * meanX
        val r = sxy / sqrt(sxx * syy)
        return LinearRegressionResult(slope, intercept, r, r * r, "y = ${format(slope)}x + ${format(intercept)}")
    }

    fun histogram(values: List<Double>, binCount: Int): List<HistogramBin> {
        require(binCount in 1..100) { "Use 1 to 100 bins." }
        val min = values.minOrNull() ?: return emptyList()
        val max = values.maxOrNull() ?: return emptyList()
        val width = if (max == min) 1.0 else (max - min) / binCount
        return (0 until binCount).map { index ->
            val lower = min + index * width
            val upper = if (index == binCount - 1) max else lower + width
            HistogramBin(lower, upper, values.count { it >= lower && (it < upper || index == binCount - 1 && it <= upper) })
        }
    }

    fun parseCsvNumbers(csv: String): List<List<Double>> =
        csv.lineSequence()
            .filter { it.isNotBlank() }
            .map { line -> line.split(",").mapNotNull { it.trim().toDoubleOrNull() } }
            .filter { it.isNotEmpty() }
            .toList()

    fun exportCsv(rows: List<List<Double>>): String =
        rows.joinToString("\n") { row -> row.joinToString(",") { format(it) } }

    private fun percentile(sorted: List<Double>, percentile: Double): Double {
        if (sorted.size == 1) return sorted.first()
        val rank = percentile / 100.0 * (sorted.size - 1)
        val lower = rank.toInt()
        val upper = kotlin.math.ceil(rank).toInt()
        val weight = rank - lower
        return sorted[lower] * (1 - weight) + sorted[upper] * weight
    }

    private fun format(value: Double): String = "%.6f".format(value).trimEnd('0').trimEnd('.')
}
