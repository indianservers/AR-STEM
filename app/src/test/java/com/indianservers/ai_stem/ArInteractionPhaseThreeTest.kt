package com.indianservers.ai_stem

import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.interaction.ArInteractionEngine
import com.indianservers.ai_stem.domain.interaction.ArSnapCandidate
import com.indianservers.ai_stem.domain.interaction.ArSnappingProfile
import com.indianservers.ai_stem.domain.interaction.SnapKind
import com.indianservers.ai_stem.domain.scene.Vector3Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArInteractionPhaseThreeTest {
    private val math = ArMathEngine()
    private val interaction = ArInteractionEngine(math)

    @Test
    fun snapWorldPointPrefersHighPriorityGraphCandidate() {
        val result = interaction.snapWorldPoint(
            point = Vector3Value(0.11, 0.0, 0.09),
            profile = ArSnappingProfile(enabled = true, graphToleranceMeters = 0.05),
            candidates = listOf(
                ArSnapCandidate(Vector3Value(0.10, 0.0, 0.10), SnapKind.GraphPoint, "root", priority = 10)
            )
        )

        assertTrue(result.snapped)
        assertEquals("root", result.label)
        assertNearly(0.10, result.position.x)
    }

    @Test
    fun snapWorldPointFallsBackToAxisAndGrid() {
        val axis = interaction.snapWorldPoint(
            point = Vector3Value(0.01, 0.0, 0.13),
            profile = ArSnappingProfile(enabled = true, axisToleranceMeters = 0.02)
        )
        assertTrue(axis.snapped)
        assertNearly(0.0, axis.position.x)

        val grid = interaction.snapWorldPoint(
            point = Vector3Value(0.074, 0.0, 0.126),
            profile = ArSnappingProfile(enabled = true, gridStepMeters = 0.05, axisToleranceMeters = 0.0)
        )
        assertTrue(grid.snapped)
        assertNearly(0.05, grid.position.x)
        assertNearly(0.15, grid.position.z)
    }

    @Test
    fun pickGraphPointConvertsWorldPositionToMathCoordinates() {
        val compiled = math.compile(
            source = "y = x^2",
            domain = ArGraphDomain(xMin = -2.0, xMax = 2.0, valueClamp = 5.0)
        )

        val picked = interaction.pickGraphPoint(compiled, Vector3Value(0.0, 0.0, 0.0))

        assertNearly(0.0, picked.graphX)
        assertNearly(0.0, picked.graphY)
        assertTrue(picked.label.contains("0.00"))
    }

    @Test
    fun rulerMeasurementReturnsDistanceRiseRunAngleAndSlope() {
        val measurement = interaction.rulerMeasurement(
            Vector3Value(0.0, 0.0, 0.0),
            Vector3Value(3.0, 4.0, 0.0)
        )

        assertNearly(5.0, measurement.distance)
        assertNearly(4.0, measurement.rise)
        assertNearly(3.0, measurement.run)
        assertNearly(4.0 / 3.0, measurement.slope)
        assertNearly(53.130102, measurement.angleDegrees, 0.0001)
    }

    @Test
    fun graphSnapCandidatesUseAnalysisPoints() {
        val compiled = math.compile(
            source = "y = x^2 - 1",
            domain = ArGraphDomain(xMin = -2.0, xMax = 2.0, valueClamp = 5.0)
        )
        val report = math.analyze(compiled)
        val candidates = interaction.graphSnapCandidates(compiled, report.highlightedPoints)

        assertTrue(candidates.any { it.kind == SnapKind.Intersection || it.kind == SnapKind.GraphPoint })
        assertTrue(candidates.any { it.label.contains("(") })
    }

    private fun assertNearly(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }
}
