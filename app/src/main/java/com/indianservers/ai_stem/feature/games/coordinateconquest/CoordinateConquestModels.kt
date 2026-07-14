package com.indianservers.ai_stem.feature.games.coordinateconquest

import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginDefinition
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ConquestMode {
    SoloCoordinateTraining,
    SoloMissionCampaign,
    TeamTerritoryCapture,
    CooperativeGridDefence,
    VectorRelay,
    TransformationBattle,
    TeacherChallengeMode,
    SeatedTabletopMode
}

enum class CaptureRuleType { ExactPoint, DistanceTolerance, LineThroughPoints, TransformationMatch, PolygonComplete, VectorPath }
enum class AxisOrientation { XPositiveRight, XPositiveForward, XPositiveLeft, XPositiveBackward }
enum class ConquestDifficulty { Beginner, Easy, Medium, Hard, Expert }
enum class InteractionMode { PhysicalMovement, TabletopPlacement }
enum class ConquestTrackingState { Ready, MarkerLost, WeakTracking, MovingTooFast, OutsideGrid, BoundaryWarning, Paused }
enum class CoordinateRole { Navigator, Plotter, Analyst, VectorController, Strategist, Verifier }
enum class TerritoryCaptureState { Neutral, Contested, Captured, Locked }
enum class ConquestMissionType {
    PlotPoint,
    IdentifyPoint,
    MoveToOrderedPair,
    PlaceBeacon,
    FindDistance,
    FindMidpoint,
    DrawLine,
    MatchSlope,
    FindLineEquation,
    CreateParallelLine,
    CreatePerpendicularLine,
    TranslatePointOrShape,
    ReflectAcrossAxis,
    RotateAroundOrigin,
    EnlargeUsingScaleFactor,
    ApplyVectorMovement,
    FindIntersection,
    SolveGraphically,
    CapturePolygonalTerritory,
    MultiStepTeamMission
}

data class GridScale(val metresPerUnit: Double, val visibleUnitRadius: Int)
data class CoordinatePoint(val x: Double, val y: Double) {
    fun rounded(): CoordinatePoint = CoordinatePoint(round2(x), round2(y))
}

data class GridBoundary(
    val maxMovementRadiusMetres: Double,
    val noGoAreas: List<List<CoordinatePoint>>,
    val spectatorArea: List<CoordinatePoint>,
    val teamStartZones: Map<String, List<CoordinatePoint>>,
    val gridPolygon: List<CoordinatePoint>,
    val safeDeviceInstruction: String
)

data class CoordinateGridDefinition(
    val gridId: String,
    val sharedOrigin: SharedTransform,
    val widthUnits: Int,
    val heightUnits: Int,
    val unitSizeMetres: Double,
    val axisOrientation: AxisOrientation,
    val visibleRange: Int,
    val difficulty: ConquestDifficulty,
    val boundary: GridBoundary,
    val gridVersion: Long,
    val quadrantCount: Int = 4
) {
    init {
        require(widthUnits > 0 && heightUnits > 0) { "Grid dimensions must be positive." }
        require(unitSizeMetres in 0.05..5.0) { "Unit size must be safe and visible." }
        require(visibleRange in 2..50) { "Visible range must be bounded for performance." }
        require(boundary.maxMovementRadiusMetres > 0.0) { "Safe movement radius must be defined." }
    }

    constructor(gridId: String, origin: Vector3Dto, scale: GridScale, quadrantCount: Int = 4) : this(
        gridId = gridId,
        sharedOrigin = SharedTransform(positionMetres = origin),
        widthUnits = scale.visibleUnitRadius * 2,
        heightUnits = scale.visibleUnitRadius * 2,
        unitSizeMetres = scale.metresPerUnit,
        axisOrientation = AxisOrientation.XPositiveRight,
        visibleRange = scale.visibleUnitRadius,
        difficulty = ConquestDifficulty.Beginner,
        boundary = GridBoundary(
            maxMovementRadiusMetres = scale.visibleUnitRadius * scale.metresPerUnit,
            noGoAreas = emptyList(),
            spectatorArea = emptyList(),
            teamStartZones = emptyMap(),
            gridPolygon = listOf(
                CoordinatePoint(-scale.visibleUnitRadius.toDouble(), -scale.visibleUnitRadius.toDouble()),
                CoordinatePoint(scale.visibleUnitRadius.toDouble(), -scale.visibleUnitRadius.toDouble()),
                CoordinatePoint(scale.visibleUnitRadius.toDouble(), scale.visibleUnitRadius.toDouble()),
                CoordinatePoint(-scale.visibleUnitRadius.toDouble(), scale.visibleUnitRadius.toDouble())
            ),
            safeDeviceInstruction = "Walk slowly and keep the device pointed toward the play area."
        ),
        gridVersion = 1,
        quadrantCount = quadrantCount
    )

    val scale: GridScale get() = GridScale(unitSizeMetres, visibleRange)
    val origin: Vector3Dto get() = sharedOrigin.positionMetres
}

data class CoordinateCalibration(
    val markerOrigin: SharedOriginDefinition,
    val planeValidated: Boolean,
    val floorPlane: Boolean,
    val tabletopFallback: Boolean,
    val orientationConfirmed: Boolean,
    val scalePreviewConfirmed: Boolean,
    val boundaryConfirmed: Boolean,
    val playerStatuses: Map<String, ConquestTrackingState>
) {
    val matchCanStart: Boolean
        get() = orientationConfirmed && scalePreviewConfirmed && boundaryConfirmed && (planeValidated || tabletopFallback)
}

data class SafeBoundaryWarning(val state: ConquestTrackingState, val message: String, val severity: Int)

data class ConquestTeacherSettings(
    val grade: String,
    val topics: Set<ConquestMissionType>,
    val gridSize: Int,
    val unitScaleMetres: Double,
    val interactionMode: InteractionMode,
    val teamCount: Int,
    val matchDurationSeconds: Int,
    val safeBoundary: GridBoundary,
    val hintsEnabled: Boolean,
    val transformationsEnabled: Boolean,
    val advancedLineMode: Boolean,
    val roleRotation: Boolean,
    val extendedTime: Boolean,
    val reducedMotion: Boolean
)

data class ConquestAccessibilitySettings(
    val tabletopMode: Boolean = false,
    val seatedMode: Boolean = false,
    val nonArAnalystRole: Boolean = false,
    val largeCoordinateLabels: Boolean = true,
    val highContrast: Boolean = true,
    val colourIndependentTeams: Boolean = true,
    val reducedMotion: Boolean = false,
    val audioCoordinateAnnouncements: Boolean = true,
    val hapticAxisCrossing: Boolean = true,
    val extendedTime: Boolean = false,
    val oneHandedPlacement: Boolean = true
)

data class CoordinateMission(
    val missionId: String,
    val type: ConquestMissionType,
    val prompt: String,
    val skill: MathSkill,
    val targetPoint: CoordinatePoint? = null,
    val secondaryPoint: CoordinatePoint? = null,
    val targetLine: LineEquation? = null,
    val sourceShape: List<CoordinatePoint> = emptyList(),
    val targetShape: List<CoordinatePoint> = emptyList(),
    val vector: CoordinateVector? = null,
    val toleranceUnits: Double = 0.25,
    val hints: List<String>,
    val workedSolution: List<String>,
    val difficulty: ConquestDifficulty,
    val points: Int,
    val captureZoneId: String? = null
)

data class PointMission(val missionId: String, val target: CoordinatePoint, val toleranceUnits: Double)
data class LineMission(val missionId: String, val pointA: CoordinatePoint, val pointB: CoordinatePoint)
data class TransformationMission(val missionId: String, val sourcePointIds: List<String>, val transformationName: String)
data class VectorMovement(val from: CoordinatePoint, val to: CoordinatePoint)
data class CoordinateVector(val dx: Double, val dy: Double)
data class LineEquation(val slope: Double?, val intercept: Double?, val xConstant: Double? = null) {
    fun isVertical(): Boolean = xConstant != null
}

data class TerritoryZone(
    val zoneId: String,
    val boundary: List<CoordinatePoint>,
    val owningTeamId: String?,
    val difficultyWeight: Int = 1,
    val captureState: TerritoryCaptureState = TerritoryCaptureState.Neutral,
    val locked: Boolean = false
)

data class CaptureRule(val ruleId: String, val type: CaptureRuleType, val requiredMissionIds: Set<String>)

data class CaptureAttempt(
    val zoneId: String,
    val teamId: String,
    val playerId: String,
    val missionId: String,
    val mathematicallyCorrect: Boolean,
    val stablePlacement: Boolean,
    val explanationQuality: Int,
    val hostAuthorized: Boolean
)

data class CaptureResult(
    val accepted: Boolean,
    val zone: TerritoryZone,
    val scoreDelta: Int,
    val reason: String
)

data class CoordinateTeam(
    val teamId: String,
    val displayName: String,
    val score: Int = 0,
    val roles: Map<String, CoordinateRole> = emptyMap(),
    val reconnectTokens: Map<String, String> = emptyMap()
)

data class ConquestMatchState(
    val mode: ConquestMode,
    val grid: CoordinateGridDefinition,
    val zones: List<TerritoryZone>,
    val teams: List<CoordinateTeam>,
    val activeMission: CoordinateMission,
    val round: Int,
    val paused: Boolean,
    val hostPlayerId: String,
    val interactionMode: InteractionMode,
    val trackingState: ConquestTrackingState,
    val completedMissionIds: Set<String> = emptySet()
)

data class SoloChapter(
    val chapterId: String,
    val title: String,
    val summary: String,
    val missions: List<CoordinateMission>
)

data class MissionValidationResult(
    val correct: Boolean,
    val message: String,
    val score: Int,
    val stablePlacementRequired: Boolean,
    val mathematicalDifference: String? = null
)

data class ConquestScoreBreakdown(
    val coordinateAccuracy: Int,
    val mathematicalAnswer: Int,
    val time: Int,
    val hintUse: Int,
    val teamParticipation: Int,
    val stablePhysicalPlacement: Int,
    val transformation: Int,
    val territoryDifficulty: Int,
    val explanation: Int
) {
    val total: Int
        get() = listOf(
            coordinateAccuracy,
            mathematicalAnswer,
            time,
            hintUse,
            teamParticipation,
            stablePhysicalPlacement,
            transformation,
            territoryDifficulty,
            explanation
        ).sum().coerceAtLeast(0)
}

data class VisualPerformancePolicy(
    val batchGridLines: Boolean,
    val labelPoolSize: Int,
    val visibleRange: Int,
    val distanceLabelFadeMetres: Double,
    val lowVisualTier: Boolean,
    val reducedEffects: Boolean,
    val anchorCleanupSeconds: Int,
    val networkCompression: Boolean
)

class CoordinateConquestEngine {
    val modes: List<ConquestMode> = ConquestMode.entries
    val roles: List<CoordinateRole> = CoordinateRole.entries
    val soloChapters: List<SoloChapter> = buildSoloChapters()
    val missionCatalog: List<CoordinateMission> = buildMissionCatalog()

    fun defaultGrid(): CoordinateGridDefinition = CoordinateGridDefinition(
        gridId = "conquest-grid-default",
        sharedOrigin = SharedTransform(
            positionMetres = Vector3Dto(0f, 0f, 0f),
            rotation = QuaternionDto(0f, 0f, 0f, 1f)
        ),
        widthUnits = 16,
        heightUnits = 16,
        unitSizeMetres = 0.5,
        axisOrientation = AxisOrientation.XPositiveRight,
        visibleRange = 8,
        difficulty = ConquestDifficulty.Medium,
        boundary = defaultBoundary(4.5),
        gridVersion = 1
    )

    fun markerCalibration(hostPlayerId: String, grid: CoordinateGridDefinition = defaultGrid()): CoordinateCalibration =
        CoordinateCalibration(
            markerOrigin = SharedOriginDefinition(
                originId = "${grid.gridId}-origin",
                originVersion = grid.gridVersion,
                mode = SharedOriginMode.PrintedMarkerOrigin,
                hostTransform = grid.sharedOrigin,
                createdByPlayerId = hostPlayerId
            ),
            planeValidated = true,
            floorPlane = true,
            tabletopFallback = false,
            orientationConfirmed = true,
            scalePreviewConfirmed = true,
            boundaryConfirmed = true,
            playerStatuses = mapOf(hostPlayerId to ConquestTrackingState.Ready)
        )

    fun gridToShared(grid: CoordinateGridDefinition, point: CoordinatePoint): Vector3Dto {
        val metresX = point.x * grid.unitSizeMetres
        val metresZ = point.y * grid.unitSizeMetres
        val oriented = when (grid.axisOrientation) {
            AxisOrientation.XPositiveRight -> Vector3Dto(metresX.toFloat(), 0f, metresZ.toFloat())
            AxisOrientation.XPositiveForward -> Vector3Dto(metresZ.toFloat(), 0f, (-metresX).toFloat())
            AxisOrientation.XPositiveLeft -> Vector3Dto((-metresX).toFloat(), 0f, (-metresZ).toFloat())
            AxisOrientation.XPositiveBackward -> Vector3Dto((-metresZ).toFloat(), 0f, metresX.toFloat())
        }
        return grid.origin + oriented
    }

    fun sharedToGrid(grid: CoordinateGridDefinition, position: Vector3Dto): CoordinatePoint {
        val dx = (position.x - grid.origin.x).toDouble()
        val dz = (position.z - grid.origin.z).toDouble()
        val raw = when (grid.axisOrientation) {
            AxisOrientation.XPositiveRight -> CoordinatePoint(dx / grid.unitSizeMetres, dz / grid.unitSizeMetres)
            AxisOrientation.XPositiveForward -> CoordinatePoint(-dz / grid.unitSizeMetres, dx / grid.unitSizeMetres)
            AxisOrientation.XPositiveLeft -> CoordinatePoint(-dx / grid.unitSizeMetres, -dz / grid.unitSizeMetres)
            AxisOrientation.XPositiveBackward -> CoordinatePoint(dz / grid.unitSizeMetres, -dx / grid.unitSizeMetres)
        }
        return raw.rounded()
    }

    fun distance(a: CoordinatePoint, b: CoordinatePoint): Double = hypot(a.x - b.x, a.y - b.y)
    fun midpoint(a: CoordinatePoint, b: CoordinatePoint): CoordinatePoint = CoordinatePoint((a.x + b.x) / 2.0, (a.y + b.y) / 2.0).rounded()
    fun slope(a: CoordinatePoint, b: CoordinatePoint): Double? = if (abs(a.x - b.x) < EPSILON) null else (b.y - a.y) / (b.x - a.x)
    fun vector(from: CoordinatePoint, to: CoordinatePoint): CoordinateVector = CoordinateVector(to.x - from.x, to.y - from.y)
    fun applyVector(point: CoordinatePoint, vector: CoordinateVector): CoordinatePoint = CoordinatePoint(point.x + vector.dx, point.y + vector.dy).rounded()
    fun lineThrough(a: CoordinatePoint, b: CoordinatePoint): LineEquation {
        val m = slope(a, b)
        return if (m == null) LineEquation(null, null, xConstant = a.x) else LineEquation(m, a.y - m * a.x)
    }

    fun parallelLineThrough(line: LineEquation, point: CoordinatePoint): LineEquation =
        if (line.isVertical()) LineEquation(null, null, point.x) else LineEquation(line.slope, point.y - (line.slope ?: 0.0) * point.x)

    fun perpendicularLineThrough(line: LineEquation, point: CoordinatePoint): LineEquation =
        when {
            line.isVertical() -> LineEquation(0.0, point.y)
            abs(line.slope ?: 0.0) < EPSILON -> LineEquation(null, null, point.x)
            else -> {
                val m = -1.0 / (line.slope ?: 1.0)
                LineEquation(m, point.y - m * point.x)
            }
        }

    fun intersection(a: LineEquation, b: LineEquation): CoordinatePoint? {
        if (a.isVertical() && b.isVertical()) return null
        if (a.isVertical()) {
            val x = a.xConstant ?: return null
            return CoordinatePoint(x, (b.slope ?: 0.0) * x + (b.intercept ?: 0.0)).rounded()
        }
        if (b.isVertical()) return intersection(b, a)
        if (abs((a.slope ?: 0.0) - (b.slope ?: 0.0)) < EPSILON) return null
        val x = ((b.intercept ?: 0.0) - (a.intercept ?: 0.0)) / ((a.slope ?: 0.0) - (b.slope ?: 0.0))
        val y = (a.slope ?: 0.0) * x + (a.intercept ?: 0.0)
        return CoordinatePoint(x, y).rounded()
    }

    fun translate(shape: List<CoordinatePoint>, vector: CoordinateVector): List<CoordinatePoint> = shape.map { applyVector(it, vector) }
    fun reflect(point: CoordinatePoint, axis: String): CoordinatePoint = when (axis.lowercase()) {
        "x" -> CoordinatePoint(point.x, -point.y)
        "y" -> CoordinatePoint(-point.x, point.y)
        else -> CoordinatePoint(-point.x, -point.y)
    }.rounded()

    fun rotateAroundOrigin(point: CoordinatePoint, degrees: Int): CoordinatePoint {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized % 90 == 0) {
            return when (normalized) {
                90 -> CoordinatePoint(-point.y, point.x)
                180 -> CoordinatePoint(-point.x, -point.y)
                270 -> CoordinatePoint(point.y, -point.x)
                else -> point
            }.rounded()
        }
        val radians = Math.toRadians(degrees.toDouble())
        return CoordinatePoint(point.x * cos(radians) - point.y * sin(radians), point.x * sin(radians) + point.y * cos(radians)).rounded()
    }

    fun enlarge(point: CoordinatePoint, scaleFactor: Double): CoordinatePoint = CoordinatePoint(point.x * scaleFactor, point.y * scaleFactor).rounded()

    fun validateMission(mission: CoordinateMission, submittedPoint: CoordinatePoint?, submittedLine: LineEquation?, submittedShape: List<CoordinatePoint>, stable: Boolean, interactionMode: InteractionMode): MissionValidationResult {
        val physicalNeedsStable = interactionMode == InteractionMode.PhysicalMovement
        if (physicalNeedsStable && !stable) {
            return MissionValidationResult(false, "Hold position steady before capture.", 0, stablePlacementRequired = true)
        }
        val correct = when (mission.type) {
            ConquestMissionType.PlotPoint,
            ConquestMissionType.IdentifyPoint,
            ConquestMissionType.MoveToOrderedPair,
            ConquestMissionType.PlaceBeacon -> submittedPoint != null && mission.targetPoint != null && distance(submittedPoint, mission.targetPoint) <= mission.toleranceUnits
            ConquestMissionType.FindDistance -> submittedPoint?.x?.let { abs(it - distance(mission.targetPoint!!, mission.secondaryPoint!!)) <= mission.toleranceUnits } == true
            ConquestMissionType.FindMidpoint -> submittedPoint != null && mission.targetPoint != null && mission.secondaryPoint != null && distance(submittedPoint, midpoint(mission.targetPoint, mission.secondaryPoint)) <= mission.toleranceUnits
            ConquestMissionType.DrawLine,
            ConquestMissionType.MatchSlope,
            ConquestMissionType.FindLineEquation,
            ConquestMissionType.CreateParallelLine,
            ConquestMissionType.CreatePerpendicularLine,
            ConquestMissionType.FindIntersection,
            ConquestMissionType.SolveGraphically -> linesEquivalent(submittedLine, mission.targetLine)
            ConquestMissionType.TranslatePointOrShape,
            ConquestMissionType.ReflectAcrossAxis,
            ConquestMissionType.RotateAroundOrigin,
            ConquestMissionType.EnlargeUsingScaleFactor,
            ConquestMissionType.ApplyVectorMovement,
            ConquestMissionType.CapturePolygonalTerritory,
            ConquestMissionType.MultiStepTeamMission -> shapeEquivalent(submittedShape, mission.targetShape)
        }
        return MissionValidationResult(
            correct = correct,
            message = if (correct) "Coordinate mission complete." else "Check the coordinate math and try again.",
            score = if (correct) mission.points else 0,
            stablePlacementRequired = physicalNeedsStable,
            mathematicalDifference = if (correct) null else describeDifference(mission, submittedPoint, submittedLine, submittedShape)
        )
    }

    fun captureZone(match: ConquestMatchState, attempt: CaptureAttempt): CaptureResult {
        val zone = match.zones.first { it.zoneId == attempt.zoneId }
        if (!attempt.hostAuthorized) return CaptureResult(false, zone, 0, "Only host can assign zone ownership.")
        if (zone.locked) return CaptureResult(false, zone, 0, "Zone is locked.")
        if (!attempt.mathematicallyCorrect || !attempt.stablePlacement) return CaptureResult(false, zone.copy(captureState = TerritoryCaptureState.Contested), 0, "Capture needs correct math and stable placement.")
        val delta = 50 * zone.difficultyWeight + attempt.explanationQuality.coerceIn(0, 20)
        return CaptureResult(true, zone.copy(owningTeamId = attempt.teamId, captureState = TerritoryCaptureState.Captured), delta, "Zone captured by ${attempt.teamId}.")
    }

    fun rotateRoles(players: List<String>, round: Int): Map<String, CoordinateRole> =
        players.mapIndexed { index, player -> player to roles[(index + round) % roles.size] }.toMap()

    fun boundaryWarning(grid: CoordinateGridDefinition, sharedPosition: Vector3Dto, speedMetresPerSecond: Double, markerVisible: Boolean): SafeBoundaryWarning {
        if (!markerVisible) return SafeBoundaryWarning(ConquestTrackingState.MarkerLost, "Marker lost. Stop and re-scan the shared marker.", 3)
        if (speedMetresPerSecond > 1.4) return SafeBoundaryWarning(ConquestTrackingState.MovingTooFast, "Move slower. Walking only.", 2)
        val point = sharedToGrid(grid, sharedPosition)
        val distanceMetres = hypot((sharedPosition.x - grid.origin.x).toDouble(), (sharedPosition.z - grid.origin.z).toDouble())
        if (distanceMetres > grid.boundary.maxMovementRadiusMetres) return SafeBoundaryWarning(ConquestTrackingState.BoundaryWarning, "Outside safe boundary. Return to the grid.", 3)
        if (abs(point.x) > grid.widthUnits / 2.0 || abs(point.y) > grid.heightUnits / 2.0) return SafeBoundaryWarning(ConquestTrackingState.OutsideGrid, "You exited the coordinate grid.", 2)
        if (distanceMetres > grid.boundary.maxMovementRadiusMetres * 0.85) return SafeBoundaryWarning(ConquestTrackingState.BoundaryWarning, "Near boundary. Slow down and turn back.", 2)
        return SafeBoundaryWarning(ConquestTrackingState.Ready, "Safe.", 0)
    }

    fun startMatch(mode: ConquestMode, hostPlayerId: String, interactionMode: InteractionMode, teamCount: Int = 2): ConquestMatchState {
        val grid = defaultGrid()
        val teams = (1..teamCount.coerceIn(1, 4)).map { CoordinateTeam("team-$it", "Team $it") }
        return ConquestMatchState(
            mode = mode,
            grid = grid,
            zones = buildZones(),
            teams = teams,
            activeMission = missionCatalog.first(),
            round = 1,
            paused = false,
            hostPlayerId = hostPlayerId,
            interactionMode = interactionMode,
            trackingState = ConquestTrackingState.Ready
        )
    }

    fun scoreBreakdown(correct: Boolean, stable: Boolean, usedHint: Boolean, participationCount: Int, zoneWeight: Int, explanationQuality: Int): ConquestScoreBreakdown =
        ConquestScoreBreakdown(
            coordinateAccuracy = if (correct) 40 else 0,
            mathematicalAnswer = if (correct) 80 else 0,
            time = 20,
            hintUse = if (usedHint) -10 else 15,
            teamParticipation = participationCount.coerceIn(0, 6) * 5,
            stablePhysicalPlacement = if (stable) 25 else 0,
            transformation = if (correct) 20 else 0,
            territoryDifficulty = zoneWeight * 15,
            explanation = explanationQuality.coerceIn(0, 30)
        )

    fun visualPolicy(grid: CoordinateGridDefinition, lowTier: Boolean): VisualPerformancePolicy =
        VisualPerformancePolicy(
            batchGridLines = true,
            labelPoolSize = if (lowTier) 24 else 64,
            visibleRange = grid.visibleRange.coerceAtMost(if (lowTier) 6 else 12),
            distanceLabelFadeMetres = if (lowTier) 3.0 else 5.0,
            lowVisualTier = lowTier,
            reducedEffects = lowTier,
            anchorCleanupSeconds = 20,
            networkCompression = true
        )

    private fun linesEquivalent(actual: LineEquation?, expected: LineEquation?): Boolean {
        if (actual == null || expected == null) return false
        if (actual.isVertical() || expected.isVertical()) return actual.isVertical() && expected.isVertical() && abs((actual.xConstant ?: 0.0) - (expected.xConstant ?: 0.0)) < 0.05
        return abs((actual.slope ?: 0.0) - (expected.slope ?: 0.0)) < 0.05 && abs((actual.intercept ?: 0.0) - (expected.intercept ?: 0.0)) < 0.05
    }

    private fun shapeEquivalent(actual: List<CoordinatePoint>, expected: List<CoordinatePoint>): Boolean =
        actual.size == expected.size && actual.zip(expected).all { (a, e) -> distance(a, e) <= 0.25 }

    private fun describeDifference(mission: CoordinateMission, point: CoordinatePoint?, line: LineEquation?, shape: List<CoordinatePoint>): String =
        when {
            mission.targetPoint != null && point != null -> "Submitted ${point.rounded()}, expected ${mission.targetPoint.rounded()}."
            mission.targetLine != null && line != null -> "Submitted line $line, expected ${mission.targetLine}."
            mission.targetShape.isNotEmpty() && shape.isNotEmpty() -> "Submitted ${shape.map { it.rounded() }}, expected ${mission.targetShape.map { it.rounded() }}."
            else -> "No comparable answer was submitted."
        }

    private fun buildZones(): List<TerritoryZone> = listOf(
        TerritoryZone("zone-alpha", square(-4.0, -4.0, -1.0, -1.0), null, 1),
        TerritoryZone("zone-beta", square(1.0, -4.0, 4.0, -1.0), null, 2),
        TerritoryZone("zone-gamma", square(-4.0, 1.0, -1.0, 4.0), null, 2),
        TerritoryZone("zone-delta", square(1.0, 1.0, 4.0, 4.0), null, 3)
    )

    private fun buildSoloChapters(): List<SoloChapter> = listOf(
        chapter("axis", "Axis Academy", "Number lines, axes and ordered pairs.", ConquestDifficulty.Beginner, ConquestMissionType.PlotPoint, ConquestMissionType.IdentifyPoint),
        chapter("quadrant", "Quadrant Quest", "Four-quadrant plotting and movement.", ConquestDifficulty.Easy, ConquestMissionType.MoveToOrderedPair, ConquestMissionType.PlaceBeacon),
        chapter("slope", "Slope City", "Distance, midpoint, slope and lines.", ConquestDifficulty.Medium, ConquestMissionType.FindDistance, ConquestMissionType.FindMidpoint, ConquestMissionType.MatchSlope, ConquestMissionType.FindLineEquation),
        chapter("vector", "Vector Valley", "Vectors, paths and relay missions.", ConquestDifficulty.Hard, ConquestMissionType.ApplyVectorMovement, ConquestMissionType.MultiStepTeamMission),
        chapter("transform", "Transformation Realm", "Translation, reflection, rotation and enlargement.", ConquestDifficulty.Expert, ConquestMissionType.TranslatePointOrShape, ConquestMissionType.ReflectAcrossAxis, ConquestMissionType.RotateAroundOrigin, ConquestMissionType.EnlargeUsingScaleFactor)
    )

    private fun chapter(id: String, title: String, summary: String, difficulty: ConquestDifficulty, vararg types: ConquestMissionType): SoloChapter =
        SoloChapter(
            chapterId = id,
            title = title,
            summary = summary,
            missions = (1..8).map { index ->
                val type = types[(index - 1) % types.size]
                mission("${id}-$index", type, index, difficulty, "chapter-zone-$index")
            }
        )

    private fun buildMissionCatalog(): List<CoordinateMission> =
        ConquestMissionType.entries.mapIndexed { index, type -> mission("catalog-${index + 1}", type, index + 1, ConquestDifficulty.Medium, "zone-${index % 4}") }

    private fun mission(id: String, type: ConquestMissionType, seed: Int, difficulty: ConquestDifficulty, zoneId: String): CoordinateMission {
        val a = CoordinatePoint((seed % 7 - 3).toDouble(), ((seed * 2) % 7 - 3).toDouble())
        val b = CoordinatePoint(((seed + 3) % 7 - 3).toDouble(), ((seed * 3) % 7 - 3).toDouble())
        val baseLine = lineThrough(a, b.takeIf { it != a } ?: CoordinatePoint(a.x + 1, a.y + 2))
        val vector = CoordinateVector((seed % 3 + 1).toDouble(), ((seed + 1) % 3 + 1).toDouble())
        val shape = listOf(CoordinatePoint(1.0, 1.0), CoordinatePoint(2.0, 1.0), CoordinatePoint(2.0, 2.0))
        val targetShape = when (type) {
            ConquestMissionType.TranslatePointOrShape -> translate(shape, vector)
            ConquestMissionType.ReflectAcrossAxis -> shape.map { reflect(it, "x") }
            ConquestMissionType.RotateAroundOrigin -> shape.map { rotateAroundOrigin(it, 90) }
            ConquestMissionType.EnlargeUsingScaleFactor -> shape.map { enlarge(it, 2.0) }
            ConquestMissionType.ApplyVectorMovement,
            ConquestMissionType.CapturePolygonalTerritory,
            ConquestMissionType.MultiStepTeamMission -> translate(shape, vector)
            else -> emptyList()
        }
        val targetLine = when (type) {
            ConquestMissionType.DrawLine,
            ConquestMissionType.MatchSlope,
            ConquestMissionType.FindLineEquation,
            ConquestMissionType.SolveGraphically -> baseLine
            ConquestMissionType.CreateParallelLine -> parallelLineThrough(baseLine, CoordinatePoint(0.0, 1.0))
            ConquestMissionType.CreatePerpendicularLine -> perpendicularLineThrough(baseLine, CoordinatePoint(0.0, 1.0))
            ConquestMissionType.FindIntersection -> LineEquation(1.0, 0.0)
            else -> null
        }
        return CoordinateMission(
            missionId = id,
            type = type,
            prompt = promptFor(type, a, b, vector),
            skill = skillFor(type),
            targetPoint = when (type) {
                ConquestMissionType.FindDistance -> CoordinatePoint(distance(a, b), 0.0)
                ConquestMissionType.FindMidpoint -> midpoint(a, b)
                ConquestMissionType.FindIntersection -> intersection(LineEquation(1.0, 0.0), LineEquation(-1.0, 4.0))
                else -> a
            },
            secondaryPoint = b,
            targetLine = targetLine,
            sourceShape = shape,
            targetShape = targetShape,
            vector = vector,
            toleranceUnits = 0.3,
            hints = listOf("Check the x value first.", "Then check y, slope, or transformation rule."),
            workedSolution = workedSolutionFor(type, a, b, vector),
            difficulty = difficulty,
            points = 80 + difficulty.ordinal * 20,
            captureZoneId = zoneId
        )
    }

    private fun skillFor(type: ConquestMissionType): MathSkill = when (type) {
        ConquestMissionType.FindDistance -> MathSkill.GridDistance
        ConquestMissionType.FindMidpoint -> MathSkill.CoordinateIdentification
        ConquestMissionType.MatchSlope,
        ConquestMissionType.FindLineEquation,
        ConquestMissionType.CreateParallelLine,
        ConquestMissionType.CreatePerpendicularLine -> MathSkill.SlopeBasics
        else -> MathSkill.CoordinateIdentification
    }

    private fun promptFor(type: ConquestMissionType, a: CoordinatePoint, b: CoordinatePoint, vector: CoordinateVector): String = when (type) {
        ConquestMissionType.PlotPoint -> "Plot point ${a.rounded()}."
        ConquestMissionType.IdentifyPoint -> "Identify the beacon at ${a.rounded()}."
        ConquestMissionType.MoveToOrderedPair -> "Move to ordered pair ${a.rounded()}."
        ConquestMissionType.PlaceBeacon -> "Place a beacon at ${a.rounded()}."
        ConquestMissionType.FindDistance -> "Find distance between ${a.rounded()} and ${b.rounded()}."
        ConquestMissionType.FindMidpoint -> "Find midpoint of ${a.rounded()} and ${b.rounded()}."
        ConquestMissionType.DrawLine -> "Draw the line through ${a.rounded()} and ${b.rounded()}."
        ConquestMissionType.MatchSlope -> "Match the slope from ${a.rounded()} to ${b.rounded()}."
        ConquestMissionType.FindLineEquation -> "Find the line equation through ${a.rounded()} and ${b.rounded()}."
        ConquestMissionType.CreateParallelLine -> "Create a parallel line through (0, 1)."
        ConquestMissionType.CreatePerpendicularLine -> "Create a perpendicular line through (0, 1)."
        ConquestMissionType.TranslatePointOrShape -> "Translate the shape by vector <${vector.dx}, ${vector.dy}>."
        ConquestMissionType.ReflectAcrossAxis -> "Reflect the shape across the x-axis."
        ConquestMissionType.RotateAroundOrigin -> "Rotate the shape 90 degrees around origin."
        ConquestMissionType.EnlargeUsingScaleFactor -> "Enlarge the shape by scale factor 2."
        ConquestMissionType.ApplyVectorMovement -> "Apply vector <${vector.dx}, ${vector.dy}>."
        ConquestMissionType.FindIntersection -> "Find the intersection of y=x and y=-x+4."
        ConquestMissionType.SolveGraphically -> "Solve the simple system by graphing."
        ConquestMissionType.CapturePolygonalTerritory -> "Complete the polygonal territory."
        ConquestMissionType.MultiStepTeamMission -> "Complete a multi-step coordinate mission as a team."
    }

    private fun workedSolutionFor(type: ConquestMissionType, a: CoordinatePoint, b: CoordinatePoint, vector: CoordinateVector): List<String> = when (type) {
        ConquestMissionType.FindDistance -> listOf("Use d = sqrt((x2-x1)^2 + (y2-y1)^2).", "Distance = ${round2(distance(a, b))}.")
        ConquestMissionType.FindMidpoint -> listOf("Average x values and y values.", "Midpoint = ${midpoint(a, b)}.")
        ConquestMissionType.ApplyVectorMovement -> listOf("Add dx to x and dy to y.", "Vector is <$vector>.")
        else -> listOf("Read the coordinate rule.", "Apply it exactly on the shared grid.")
    }

    private fun defaultBoundary(radius: Double): GridBoundary = GridBoundary(
        maxMovementRadiusMetres = radius,
        noGoAreas = emptyList(),
        spectatorArea = square(-5.0, 5.0, 5.0, 6.0),
        teamStartZones = mapOf("team-1" to square(-4.0, -4.0, -3.0, -3.0), "team-2" to square(3.0, 3.0, 4.0, 4.0)),
        gridPolygon = square(-8.0, -8.0, 8.0, 8.0),
        safeDeviceInstruction = "Hold the device with both hands when walking; tabletop mode is equal for scoring."
    )

    private fun square(x1: Double, y1: Double, x2: Double, y2: Double): List<CoordinatePoint> =
        listOf(CoordinatePoint(x1, y1), CoordinatePoint(x2, y1), CoordinatePoint(x2, y2), CoordinatePoint(x1, y2))
}

private const val EPSILON = 0.0001
private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
