package com.indianservers.ai_stem.domain.probability

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class SimulationResult(val outcomes: List<Int>, val counts: Map<Int, Int>, val experimentalMean: Double)
data class DistributionPoint(val x: Int, val probability: Double)

object ProbabilityEngine {
    fun coinFlips(trials: Int, seed: Int = 42): SimulationResult {
        require(trials in 1..100_000) { "Use 1 to 100000 trials." }
        val random = Random(seed)
        val outcomes = List(trials) { if (random.nextBoolean()) 1 else 0 }
        return SimulationResult(outcomes, outcomes.groupingBy { it }.eachCount(), outcomes.average())
    }

    fun diceRolls(trials: Int, sides: Int = 6, seed: Int = 42): SimulationResult {
        require(trials in 1..100_000) { "Use 1 to 100000 trials." }
        require(sides in 2..120) { "Dice must have 2 to 120 sides." }
        val random = Random(seed)
        val outcomes = List(trials) { random.nextInt(1, sides + 1) }
        return SimulationResult(outcomes, outcomes.groupingBy { it }.eachCount(), outcomes.average())
    }

    fun combinations(n: Int, r: Int): Long {
        require(n >= 0 && r >= 0 && r <= n && n <= 60) { "Use 0 <= r <= n <= 60." }
        val k = minOf(r, n - r)
        var result = 1L
        for (i in 1..k) {
            result = result * (n - k + i) / i
        }
        return result
    }

    fun permutations(n: Int, r: Int): Long {
        require(n >= 0 && r >= 0 && r <= n && n <= 20) { "Use 0 <= r <= n <= 20." }
        return ((n - r + 1)..n).fold(1L) { acc, value -> acc * value }
    }

    fun binomialDistribution(trials: Int, probability: Double): List<DistributionPoint> {
        require(trials in 0..60) { "Use at most 60 trials." }
        require(probability in 0.0..1.0) { "Probability must be between 0 and 1." }
        return (0..trials).map { successes ->
            DistributionPoint(successes, combinations(trials, successes) * probability.pow(successes) * (1 - probability).pow(trials - successes))
        }
    }

    fun normalDensity(x: Double, mean: Double = 0.0, standardDeviation: Double = 1.0): Double {
        require(standardDeviation > 0.0) { "Standard deviation must be positive." }
        val z = (x - mean) / standardDeviation
        return exp(-0.5 * z * z) / (standardDeviation * sqrt(2 * PI))
    }
}
