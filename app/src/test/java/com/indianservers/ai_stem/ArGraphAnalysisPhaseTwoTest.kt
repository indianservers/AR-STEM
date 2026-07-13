package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArGraphAnalysisPhaseTwoTest {
    private val engine = ArMathEngine()

    @Test
    fun analysisFindsRootsExtremaAreaAndVolume() {
        val compiled = engine.compile(
            source = "y = x^2 - 1",
            domain = ArGraphDomain(xMin = -2.0, xMax = 2.0, yMin = -2.0, yMax = 4.0)
        )
        val report = engine.analyze(compiled, focusX = 0.0)

        assertTrue(report.roots.any { kotlin.math.abs(it.x + 1.0) < 0.01 })
        assertTrue(report.roots.any { kotlin.math.abs(it.x - 1.0) < 0.01 })
        assertTrue(report.extrema.any { kotlin.math.abs(it.x) < 0.03 && it.y < -0.99 })
        assertTrue(requireNotNull(report.integral).absoluteArea > 1.0)
        assertTrue(report.integral.volumeOfRevolution > 0.0)
    }

    @Test
    fun analysisComputesTangentAndNormalAtFocusPoint() {
        val compiled = engine.compile("y = x^2")
        val report = engine.analyze(compiled, focusX = 2.0)

        val tangent = requireNotNull(report.tangent)
        assertNearly(4.0, tangent.tangentSlope, 0.001)
        assertNearly(-0.25, tangent.normalSlope, 0.001)
        assertNearly(4.0, tangent.point.y, 0.001)
    }

    @Test
    fun analysisFindsIntersectionsAgainstComparisonEquation() {
        val primary = engine.compile(
            source = "y = x^2",
            domain = ArGraphDomain(xMin = -3.0, xMax = 3.0, yMin = -1.0, yMax = 10.0)
        )
        val comparison = engine.compile(
            source = "y = 4",
            domain = primary.domain
        )
        val report = engine.analyze(primary, focusX = 0.0, comparison = comparison)

        assertTrue(report.intersections.any { kotlin.math.abs(it.x + 2.0) < 0.01 })
        assertTrue(report.intersections.any { kotlin.math.abs(it.x - 2.0) < 0.01 })
    }

    @Test
    fun sinusoidAnalysisFindsInflectionsAndArcLength() {
        val compiled = engine.compile("y = sin(x)")
        val report = engine.analyze(compiled, focusX = 0.0)

        assertTrue(report.inflections.isNotEmpty())
        assertTrue(requireNotNull(report.integral).arcLength > 1.0)
    }

    private fun assertNearly(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }
}
