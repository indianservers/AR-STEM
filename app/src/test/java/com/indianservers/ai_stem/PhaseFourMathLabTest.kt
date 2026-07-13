package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.algebra.AlgebraEngine
import com.indianservers.ai_stem.domain.algebra.ComplexNumber
import com.indianservers.ai_stem.domain.algebra.Matrix
import com.indianservers.ai_stem.domain.algebra.RationalNumber
import com.indianservers.ai_stem.domain.calculus.CalculusEngine
import com.indianservers.ai_stem.domain.graph.MathExpressionEvaluator
import com.indianservers.ai_stem.domain.graph.MathExpressionParser
import com.indianservers.ai_stem.domain.graph.ParseOutcome
import com.indianservers.ai_stem.domain.graph.defaultVariables
import com.indianservers.ai_stem.domain.probability.ProbabilityEngine
import com.indianservers.ai_stem.domain.statistics.StatisticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseFourMathLabTest {
    @Test
    fun algebraSolvesEquationsAndExactFractions() {
        assertEquals("5/6", (RationalNumber(1, 2) + RationalNumber(1, 3)).toString())
        assertNearly(4.0, AlgebraEngine.solveLinear(2.0, -8.0).values.single())
        assertEquals(listOf(3.0, 2.0), AlgebraEngine.solveQuadratic(1.0, -5.0, 6.0).values)
        assertNearly(5.0, ComplexNumber(3.0, 4.0).magnitude())
    }

    @Test
    fun algebraHandlesMatricesAndSystems() {
        assertNearly(5.0, AlgebraEngine.determinant2x2(Matrix(listOf(listOf(2.0, 3.0), listOf(1.0, 4.0)))))
        val product = AlgebraEngine.multiply(
            Matrix(listOf(listOf(1.0, 2.0))),
            Matrix(listOf(listOf(3.0), listOf(4.0)))
        )
        assertEquals(listOf(listOf(11.0)), product.rows)
        val system = AlgebraEngine.solveTwoByTwo(2.0, 1.0, 7.0, 1.0, -1.0, 1.0)
        assertNearly(8.0 / 3.0, system.values[0])
    }

    @Test
    fun calculusEstimatesLimitsIntegralsAndVolumes() {
        val calculus = CalculusEngine()
        assertTrue(calculus.limit("y = sin(x) / x", 0.0).twoSided != null)
        assertNearly(12.0, calculus.derivative("y = x^3", 2.0), 0.001)
        assertNearly(1.0 / 3.0, calculus.definiteIntegral("y = x^2", 0.0, 1.0).value, 0.001)
        assertNearly(Math.PI / 3.0, calculus.diskVolumeAroundXAxis("y = x", 0.0, 1.0).value, 0.001)
        assertNearly(6.0, calculus.partialDerivative("z = x^2 + y^2", 3.0, 4.0, "x"), 0.001)
    }

    @Test
    fun statisticsAndRegressionAreComputed() {
        val stats = StatisticsEngine.describe(listOf(2.0, 4.0, 4.0, 6.0, 8.0, 10.0))
        assertNearly(5.666666, stats.mean, 0.0001)
        assertEquals(listOf(4.0), stats.modes)
        val regression = StatisticsEngine.linearRegression(listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0))
        assertNearly(2.0, regression.slope)
        assertEquals(2, StatisticsEngine.parseCsvNumbers("1,2,3\n4,5,6").size)
    }

    @Test
    fun probabilityUsesDeterministicSimulationAndDistributions() {
        val first = ProbabilityEngine.coinFlips(20, seed = 7)
        val second = ProbabilityEngine.coinFlips(20, seed = 7)
        assertEquals(first.outcomes, second.outcomes)
        assertEquals(15L, ProbabilityEngine.combinations(6, 2))
        assertEquals(20L, ProbabilityEngine.permutations(5, 2))
        assertNearly(0.3125, ProbabilityEngine.binomialDistribution(5, 0.5).first { it.x == 3 }.probability)
    }

    @Test
    fun evaluatorSupportsBoundedSumAndProduct() {
        val parser = MathExpressionParser()
        val evaluator = MathExpressionEvaluator()
        val sum = (parser.parse("sum(n^2, n, 1, 3)") as ParseOutcome.Success).expression
        val product = (parser.parse("product(n, n, 1, 4)") as ParseOutcome.Success).expression
        assertNearly(14.0, requireNotNull(evaluator.evaluate(sum, defaultVariables()).value))
        assertNearly(24.0, requireNotNull(evaluator.evaluate(product, defaultVariables()).value))
    }

    private fun assertNearly(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }
}
