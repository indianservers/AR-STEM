package com.indianservers.ai_stem.feature.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.domain.probability.ProbabilityEngine
import com.indianservers.ai_stem.domain.statistics.StatisticsEngine
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun DataProbabilityLaboratoryScreen(onBack: () -> Unit) {
    val values = listOf(2.0, 4.0, 4.0, 6.0, 8.0, 10.0)
    val stats = StatisticsEngine.describe(values)
    val regression = StatisticsEngine.linearRegression(listOf(1.0 to 2.0, 2.0 to 4.1, 3.0 to 5.9, 4.0 to 8.2))
    val histogram = StatisticsEngine.histogram(values, 3)
    val csv = StatisticsEngine.exportCsv(listOf(values))
    val coin = ProbabilityEngine.coinFlips(20, seed = 7)
    val dice = ProbabilityEngine.diceRolls(12, seed = 7)
    val binomial = ProbabilityEngine.binomialDistribution(5, 0.5)
    Scaffold(topBar = { SimpleHeader("Data and Probability", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LabCard("Descriptive Statistics", "Mean ${stats.mean.format()}, median ${stats.median.format()}, standard deviation ${stats.standardDeviation.format()}, IQR ${stats.interquartileRange.format()}") }
            item { LabCard("Regression", "${regression.equation}\nR² = ${regression.rSquared.format()}") }
            item { LabCard("Histogram", histogram.joinToString("\n") { "${it.lower.format()} to ${it.upper.format()}: ${it.count}" }) }
            item { LabCard("CSV", "Exported: $csv\nParsed rows: ${StatisticsEngine.parseCsvNumbers(csv).size}") }
            item { LabCard("Deterministic Simulations", "20 coin flips heads: ${coin.counts[1] ?: 0}\n12 dice rolls mean: ${dice.experimentalMean.format()}") }
            item { LabCard("Probability Distributions", "C(6,2) = ${ProbabilityEngine.combinations(6, 2)}\nP(X=3) for Binomial(5, 0.5) = ${binomial.first { it.x == 3 }.probability.format()}\nNormal density at 0 = ${ProbabilityEngine.normalDensity(0.0).format()}") }
        }
    }
}
