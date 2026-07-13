package com.indianservers.ai_stem.domain.algebra

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

data class RationalNumber(val numerator: Long, val denominator: Long = 1) {
    init {
        require(denominator != 0L) { "Denominator cannot be zero." }
    }

    private val normalizedSign: Long = if (denominator < 0) -1 else 1
    private val divisor: Long = gcd(abs(numerator), abs(denominator))
    val n: Long = normalizedSign * numerator / divisor
    val d: Long = abs(denominator) / divisor

    operator fun plus(other: RationalNumber) = RationalNumber(n * other.d + other.n * d, d * other.d)
    operator fun minus(other: RationalNumber) = RationalNumber(n * other.d - other.n * d, d * other.d)
    operator fun times(other: RationalNumber) = RationalNumber(n * other.n, d * other.d)
    operator fun div(other: RationalNumber) = RationalNumber(n * other.d, d * other.n)
    fun toDouble(): Double = n.toDouble() / d.toDouble()
    override fun toString(): String = if (d == 1L) "$n" else "$n/$d"
}

data class AlgebraStep(val expression: String, val reason: String)
data class EquationSolution(val title: String, val values: List<Double>, val exact: List<String>, val steps: List<AlgebraStep>)
data class ComplexNumber(val real: Double, val imaginary: Double) {
    fun plus(other: ComplexNumber) = ComplexNumber(real + other.real, imaginary + other.imaginary)
    fun times(other: ComplexNumber) = ComplexNumber(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real)
    fun conjugate() = ComplexNumber(real, -imaginary)
    fun magnitude(): Double = hypot(real, imaginary)
    fun argumentRadians(): Double = atan2(imaginary, real)
}

data class Matrix(val rows: List<List<Double>>) {
    val rowCount: Int = rows.size
    val columnCount: Int = rows.firstOrNull()?.size ?: 0

    init {
        require(rows.isNotEmpty()) { "Matrix cannot be empty." }
        require(rows.all { it.size == columnCount }) { "All rows must have the same length." }
    }
}

object AlgebraEngine {
    fun solveLinear(a: Double, b: Double): EquationSolution {
        require(a != 0.0) { "Linear coefficient cannot be zero." }
        val x = -b / a
        return EquationSolution(
            title = "Linear equation",
            values = listOf(x),
            exact = listOf("x = ${format(x)}"),
            steps = listOf(
                AlgebraStep("${format(a)}x + ${format(b)} = 0", "Start with standard form."),
                AlgebraStep("x = -${format(b)} / ${format(a)}", "Move the constant and divide."),
                AlgebraStep("x = ${format(x)}", "Simplify.")
            )
        )
    }

    fun solveQuadratic(a: Double, b: Double, c: Double): EquationSolution {
        require(a != 0.0) { "Quadratic coefficient cannot be zero." }
        val discriminant = b * b - 4 * a * c
        val steps = mutableListOf(
            AlgebraStep("${format(a)}x² + ${format(b)}x + ${format(c)} = 0", "Start with standard form."),
            AlgebraStep("D = b² - 4ac = ${format(discriminant)}", "Compute the discriminant.")
        )
        return if (discriminant < 0) {
            steps += AlgebraStep("D < 0", "There are no real roots.")
            EquationSolution("Quadratic equation", emptyList(), listOf("No real solution"), steps)
        } else {
            val root = sqrt(discriminant)
            val x1 = (-b + root) / (2 * a)
            val x2 = (-b - root) / (2 * a)
            steps += AlgebraStep("x = (-b ± √D) / 2a", "Apply the quadratic formula.")
            steps += AlgebraStep("x = ${format(x1)}, ${format(x2)}", "Evaluate both roots.")
            EquationSolution("Quadratic equation", listOf(x1, x2), listOf("x = ${format(x1)}", "x = ${format(x2)}"), steps)
        }
    }

    fun expandSquare(a: String, b: Double): List<AlgebraStep> =
        listOf(
            AlgebraStep("($a + ${format(b)})²", "Start with a binomial square."),
            AlgebraStep("$a² + 2·$a·${format(b)} + ${format(b)}²", "Use (u + v)² = u² + 2uv + v²."),
            AlgebraStep("$a² + ${format(2 * b)}$a + ${format(b * b)}", "Simplify constants.")
        )

    fun factorMonicQuadratic(b: Int, c: Int): List<AlgebraStep> {
        val pair = (-abs(c)..abs(c)).firstNotNullOfOrNull { r ->
            if (r != 0 && c % r == 0 && r + c / r == b) r to c / r else null
        }
        return if (pair == null) {
            listOf(AlgebraStep("x² + ${b}x + $c", "No integer factor pair was found."))
        } else {
            listOf(
                AlgebraStep("x² + ${b}x + $c", "Find two numbers with sum $b and product $c."),
                AlgebraStep("(x + ${pair.first})(x + ${pair.second})", "Write the factorization.")
            )
        }
    }

    fun determinant2x2(matrix: Matrix): Double {
        require(matrix.rowCount == 2 && matrix.columnCount == 2) { "Use a 2 by 2 matrix." }
        return matrix.rows[0][0] * matrix.rows[1][1] - matrix.rows[0][1] * matrix.rows[1][0]
    }

    fun multiply(left: Matrix, right: Matrix): Matrix {
        require(left.columnCount == right.rowCount) { "Matrix dimensions do not match." }
        return Matrix(List(left.rowCount) { r ->
            List(right.columnCount) { c ->
                (0 until left.columnCount).sumOf { k -> left.rows[r][k] * right.rows[k][c] }
            }
        })
    }

    fun solveTwoByTwo(a1: Double, b1: Double, c1: Double, a2: Double, b2: Double, c2: Double): EquationSolution {
        val determinant = a1 * b2 - a2 * b1
        if (abs(determinant) < 1e-9) {
            return EquationSolution("Linear system", emptyList(), listOf("No unique solution"), listOf(AlgebraStep("det = 0", "The lines are parallel or identical.")))
        }
        val x = (c1 * b2 - c2 * b1) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant
        return EquationSolution("Linear system", listOf(x, y), listOf("x = ${format(x)}", "y = ${format(y)}"), listOf(AlgebraStep("Use Cramer's rule", "The determinant is non-zero.")))
    }

    private fun format(value: Double): String = "%.4f".format(value).trimEnd('0').trimEnd('.')
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) if (a == 0L) 1 else a else gcd(b, a % b)
