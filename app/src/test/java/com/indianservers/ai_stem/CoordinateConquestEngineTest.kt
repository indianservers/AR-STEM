package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.coordinateconquest.CaptureAttempt
import com.indianservers.ai_stem.feature.games.coordinateconquest.ConquestAccessibilitySettings
import com.indianservers.ai_stem.feature.games.coordinateconquest.ConquestMode
import com.indianservers.ai_stem.feature.games.coordinateconquest.ConquestMissionType
import com.indianservers.ai_stem.feature.games.coordinateconquest.ConquestTrackingState
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinateConquestEngine
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinatePoint
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinateVector
import com.indianservers.ai_stem.feature.games.coordinateconquest.InteractionMode
import com.indianservers.ai_stem.feature.games.coordinateconquest.LineEquation
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateConquestEngineTest {
    private val engine = CoordinateConquestEngine()

    @Test
    fun exposesEightModesFiveChaptersAndTwentyMissionTypes() {
        assertEquals(8, engine.modes.size)
        assertTrue(engine.modes.contains(ConquestMode.TeamTerritoryCapture))
        assertEquals(5, engine.soloChapters.size)
        assertTrue(engine.soloChapters.all { it.missions.size >= 8 })
        assertEquals(20, engine.missionCatalog.map { it.type }.distinct().size)
    }

    @Test
    fun gridConversionRoundTripsThroughSharedCoordinates() {
        val grid = engine.defaultGrid()
        val point = CoordinatePoint(3.0, -2.0)
        val shared = engine.gridToShared(grid, point)
        val roundTrip = engine.sharedToGrid(grid, shared)

        assertEquals(point, roundTrip)
    }

    @Test
    fun coordinateMathSupportsDistanceMidpointSlopeLinesVectorsAndIntersection() {
        val a = CoordinatePoint(0.0, 0.0)
        val b = CoordinatePoint(3.0, 4.0)
        val lineA = engine.lineThrough(CoordinatePoint(0.0, 0.0), CoordinatePoint(2.0, 2.0))
        val lineB = LineEquation(-1.0, 4.0)

        assertEquals(5.0, engine.distance(a, b), 0.0001)
        assertEquals(CoordinatePoint(1.5, 2.0), engine.midpoint(a, b))
        assertEquals(4.0 / 3.0, engine.slope(a, b) ?: 0.0, 0.0001)
        assertEquals(CoordinatePoint(5.0, 6.0), engine.applyVector(CoordinatePoint(2.0, 2.0), CoordinateVector(3.0, 4.0)))
        assertEquals(CoordinatePoint(2.0, 2.0), engine.intersection(lineA, lineB))
    }

    @Test
    fun transformationsAreMathematicallyExact() {
        val point = CoordinatePoint(2.0, 3.0)

        assertEquals(CoordinatePoint(5.0, 2.0), engine.applyVector(point, CoordinateVector(3.0, -1.0)))
        assertEquals(CoordinatePoint(2.0, -3.0), engine.reflect(point, "x"))
        assertEquals(CoordinatePoint(-3.0, 2.0), engine.rotateAroundOrigin(point, 90))
        assertEquals(CoordinatePoint(4.0, 6.0), engine.enlarge(point, 2.0))
    }

    @Test
    fun validatesPhysicalAndTabletopModesWithEqualMathScore() {
        val mission = engine.missionCatalog.first { it.type == ConquestMissionType.PlotPoint }
        val physical = engine.validateMission(mission, mission.targetPoint, null, emptyList(), stable = true, interactionMode = InteractionMode.PhysicalMovement)
        val tabletop = engine.validateMission(mission, mission.targetPoint, null, emptyList(), stable = false, interactionMode = InteractionMode.TabletopPlacement)
        val unstablePhysical = engine.validateMission(mission, mission.targetPoint, null, emptyList(), stable = false, interactionMode = InteractionMode.PhysicalMovement)

        assertTrue(physical.correct)
        assertTrue(tabletop.correct)
        assertEquals(physical.score, tabletop.score)
        assertFalse(unstablePhysical.correct)
    }

    @Test
    fun hostAuthorityControlsTerritoryCapture() {
        val match = engine.startMatch(ConquestMode.TeamTerritoryCapture, "host", InteractionMode.PhysicalMovement, teamCount = 2)
        val rejected = engine.captureZone(
            match,
            CaptureAttempt("zone-alpha", "team-1", "player", match.activeMission.missionId, mathematicallyCorrect = true, stablePlacement = true, explanationQuality = 10, hostAuthorized = false)
        )
        val accepted = engine.captureZone(
            match,
            CaptureAttempt("zone-alpha", "team-1", "player", match.activeMission.missionId, mathematicallyCorrect = true, stablePlacement = true, explanationQuality = 10, hostAuthorized = true)
        )

        assertFalse(rejected.accepted)
        assertTrue(accepted.accepted)
        assertEquals("team-1", accepted.zone.owningTeamId)
    }

    @Test
    fun boundaryTrackingWarnsForMarkerLossSpeedAndRadius() {
        val grid = engine.defaultGrid()

        assertEquals(ConquestTrackingState.MarkerLost, engine.boundaryWarning(grid, Vector3Dto(0f, 0f, 0f), 0.0, markerVisible = false).state)
        assertEquals(ConquestTrackingState.MovingTooFast, engine.boundaryWarning(grid, Vector3Dto(0f, 0f, 0f), 2.0, markerVisible = true).state)
        assertEquals(ConquestTrackingState.BoundaryWarning, engine.boundaryWarning(grid, Vector3Dto(20f, 0f, 20f), 0.1, markerVisible = true).state)
    }

    @Test
    fun markerCalibrationAndVisualPolicyAreReadyForSharedArGrid() {
        val grid = engine.defaultGrid()
        val calibration = engine.markerCalibration("host", grid)
        val policy = engine.visualPolicy(grid, lowTier = true)

        assertTrue(calibration.matchCanStart)
        assertTrue(policy.batchGridLines)
        assertTrue(policy.networkCompression)
        assertTrue(policy.visibleRange <= 6)
    }

    @Test
    fun roleRotationReconnectionAndAccessibilityAreModeled() {
        val roles = engine.rotateRoles(listOf("a", "b", "c", "d", "e", "f"), round = 2)
        val accessibility = ConquestAccessibilitySettings(tabletopMode = true, seatedMode = true, nonArAnalystRole = true, reducedMotion = true)
        val match = engine.startMatch(ConquestMode.VectorRelay, "host", InteractionMode.TabletopPlacement, teamCount = 4)

        assertEquals(6, roles.values.toSet().size)
        assertTrue(accessibility.tabletopMode)
        assertTrue(accessibility.nonArAnalystRole)
        assertEquals(4, match.teams.size)
        assertNotNull(match.teams.first().reconnectTokens)
    }
}
