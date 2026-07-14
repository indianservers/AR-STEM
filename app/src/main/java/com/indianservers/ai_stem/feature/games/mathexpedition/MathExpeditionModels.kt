package com.indianservers.ai_stem.feature.games.mathexpedition

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val MATH_EXPEDITION_ROUTE_VERSION = 1

enum class ExpeditionMode {
    SoloExplorer,
    TeamExpedition,
    CampusSurvey,
    MathematicsTreasureRoute,
    RouteOptimisationChallenge,
    StatisticsFieldMission,
    TeacherLedClassExpedition,
    IndoorCampusMapMode
}

enum class RouteApprovalState { Draft, NeedsReview, TeacherApproved, Retired }
enum class CheckpointVerificationStatus { NotChecked, Verified, NearCheckpoint, PoorAccuracy, OutsideBoundary, InNoGoZone, DwellTimeRequired, MovingTooFast, TeacherConfirmationRequired, LocationUnavailable }
enum class ExpeditionDifficulty { Beginner, Easy, Medium, Hard, Expert }
enum class ExpeditionTopic { Scale, Distance, Bearing, SpeedTime, AreaPerimeter, Coordinates, Elevation, Optimisation, PercentRatios, Statistics, DataCollection, FinalAnalysis }
enum class MapProviderCapability { OnlineTiles, OfflineRegion, CampusImage, AttributionRequired, ConfigurableTileSource }
enum class LocationPermissionState { NotRequested, ExplanationRequired, GrantedPrecise, GrantedApproximate, Denied, PermanentlyDenied, GpsDisabled, Unavailable }
enum class ExpeditionMissionType {
    MapScale,
    StraightLineDistance,
    RouteDistance,
    Bearing,
    Direction,
    AverageSpeed,
    TimeEstimation,
    AreaEstimation,
    Perimeter,
    CoordinateComparison,
    ElevationDifference,
    RouteOptimisation,
    PercentageCompletion,
    RatioComparison,
    Mean,
    Median,
    Mode,
    Range,
    FrequencyTable,
    DataCollection,
    EstimationError,
    MultiCheckpointFinalAnalysis
}

data class MapCoordinate(val latitude: Double, val longitude: Double) {
    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class MapProviderDefinition(
    val providerId: String,
    val displayName: String,
    val attributionText: String,
    val tileSourceTemplate: String?,
    val capabilities: Set<MapProviderCapability>,
    val offlineDownloadAvailable: Boolean,
    val usagePolicySummary: String
) {
    init {
        require(providerId.isNotBlank()) { "Map provider ID is required." }
        require(attributionText.isNotBlank()) { "Open-map attribution is required." }
        require(!offlineDownloadAvailable || MapProviderCapability.OfflineRegion in capabilities) {
            "Offline availability must match provider capability."
        }
    }
}

data class OfflineMapPackageMetadata(
    val packageId: String,
    val providerName: String,
    val licenseName: String,
    val regionName: String,
    val byteSize: Long,
    val downloaded: Boolean = false,
    val transferable: Boolean = false
)

data class SafeBoundary(val boundaryId: String, val vertices: List<MapCoordinate>) {
    init {
        require(vertices.size >= 3) { "Safe boundary requires at least three vertices." }
        require(vertices.all { it.isValid() }) { "Safe boundary contains invalid coordinates." }
    }
}

data class NoGoZone(val zoneId: String, val title: String, val boundary: SafeBoundary)
data class ExpeditionStartZone(val zoneId: String, val title: String, val boundary: SafeBoundary)
data class ExpeditionFinishZone(val zoneId: String, val title: String, val boundary: SafeBoundary)

data class RouteSegment(
    val segmentId: String,
    val fromCheckpointId: String,
    val toCheckpointId: String,
    val expectedDistanceMetres: Double,
    val expectedBearingDegrees: Double
)

data class CheckpointVerificationRule(
    val radiusMetres: Double,
    val requiredDwellSeconds: Int,
    val maxAcceptedAccuracyMetres: Double,
    val teacherConfirmationRequired: Boolean
)

data class ExpeditionMathMission(
    val missionId: String,
    val type: ExpeditionMissionType,
    val topic: ExpeditionTopic,
    val prompt: String,
    val expectedNumericAnswer: Double?,
    val tolerance: Double,
    val hints: List<String>,
    val workedSolution: List<String>,
    val points: Int
)

data class ExpeditionArMission(
    val arMissionId: String,
    val title: String,
    val optional: Boolean,
    val requiresGeospatial: Boolean,
    val nonArFallbackPrompt: String,
    val points: Int
)

data class ExpeditionCheckpoint(
    val checkpointId: String,
    val title: String,
    val coordinate: MapCoordinate,
    val mathMission: ExpeditionMathMission,
    val arMission: ExpeditionArMission?,
    val verificationRule: CheckpointVerificationRule,
    val order: Int
)

data class ExpeditionRoute(
    val routeId: String,
    val title: String,
    val checkpoints: List<ExpeditionCheckpoint>,
    val boundary: SafeBoundary,
    val noGoZones: List<NoGoZone>,
    val startZone: ExpeditionStartZone,
    val finishZone: ExpeditionFinishZone,
    val segments: List<RouteSegment>,
    val estimatedDurationMinutes: Int,
    val difficulty: ExpeditionDifficulty,
    val grade: String,
    val topics: Set<ExpeditionTopic>
)

data class TeamAssignment(val teamId: String, val title: String, val checkpointIds: List<String>, val roles: List<ExpeditionRole>)
enum class ExpeditionRole { Navigator, DistanceAnalyst, BearingSpecialist, DataCollector, ArSolver, SafetyCaptain }

data class ExpeditionDefinition(
    val expeditionId: String,
    val version: Int,
    val title: String,
    val mode: ExpeditionMode,
    val route: ExpeditionRoute,
    val mapProvider: MapProviderDefinition,
    val offlineRegion: OfflineMapPackageMetadata?,
    val approvalState: RouteApprovalState,
    val teamAssignments: List<TeamAssignment>,
    val teacherApprovedBy: String?,
    val createdAtEpochMs: Long
)

data class LocalMissionCoordinate(val checkpointId: String, val localXMetres: Double, val localYMetres: Double)
data class LocationSample(
    val coordinate: MapCoordinate?,
    val accuracyMetres: Double?,
    val speedMetresPerSecond: Double?,
    val elapsedDwellSeconds: Int,
    val providerAvailable: Boolean,
    val approximateOnly: Boolean,
    val mockFlaggedByAndroid: Boolean
)

data class CheckpointVerificationResult(
    val status: CheckpointVerificationStatus,
    val distanceMetres: Double?,
    val message: String,
    val hostAuthoritative: Boolean,
    val canComplete: Boolean
)

data class RouteValidationIssue(val code: String, val message: String, val severity: Int)
data class RouteValidationResult(val valid: Boolean, val issues: List<RouteValidationIssue>)

data class RouteAuthoringState(
    val selectedStart: MapCoordinate?,
    val checkpoints: List<ExpeditionCheckpoint>,
    val finish: MapCoordinate?,
    val boundary: SafeBoundary?,
    val noGoZones: List<NoGoZone>,
    val checkpointRadiusMetres: Double,
    val previewDistanceMetres: Double,
    val approved: Boolean
)

data class ExpeditionAnalyticsSummary(
    val routeId: String,
    val distanceTravelledMetres: Double,
    val routeCompleted: Boolean,
    val estimatedVersusActualMinutes: Double,
    val checkpointsCompleted: Int,
    val accuracyAverageMetres: Double,
    val topicPerformance: Map<ExpeditionTopic, Double>,
    val routeEfficiency: Double,
    val hintUse: Int,
    val arMissionParticipation: Int,
    val teamContribution: Map<String, Int>,
    val safetyInterruptions: Int
)

data class ExpeditionPrivacyState(
    val savedRouteIds: Set<String>,
    val downloadedMapIds: Set<String>,
    val historyEntryIds: Set<String>,
    val locationCacheEntries: Int
)

data class ExpeditionPackage(
    val version: Int,
    val metadata: String,
    val checkpointCount: Int,
    val payload: String
)

interface CheckpointVerificationContract {
    fun verify(userLocation: MapCoordinate, route: ExpeditionRoute): CheckpointVerificationStatus
}

class MathExpeditionEngine {
    val modes: List<ExpeditionMode> = ExpeditionMode.entries
    val roles: List<ExpeditionRole> = ExpeditionRole.entries
    val mapProviders: List<MapProviderDefinition> = listOf(
        MapProviderDefinition(
            providerId = "configurable-osm-raster",
            displayName = "Configurable OpenStreetMap-compatible Raster",
            attributionText = "Map data © OpenStreetMap contributors. Tile provider attribution must be configured.",
            tileSourceTemplate = null,
            capabilities = setOf(MapProviderCapability.OnlineTiles, MapProviderCapability.AttributionRequired, MapProviderCapability.ConfigurableTileSource),
            offlineDownloadAvailable = false,
            usagePolicySummary = "No public tile service is hardcoded. Configure a compliant provider before live use."
        ),
        MapProviderDefinition(
            providerId = "local-campus-image",
            displayName = "Teacher Imported Campus/Floor Image",
            attributionText = "Teacher-provided local map image. Confirm source rights before use.",
            tileSourceTemplate = null,
            capabilities = setOf(MapProviderCapability.CampusImage, MapProviderCapability.OfflineRegion),
            offlineDownloadAvailable = true,
            usagePolicySummary = "Local image is usable offline when imported by the teacher."
        )
    )
    val sampleTemplates: List<ExpeditionDefinition> = buildSamples()

    fun permissionMessage(state: LocationPermissionState): String = when (state) {
        LocationPermissionState.NotRequested -> "Location starts only when you begin a real expedition."
        LocationPermissionState.ExplanationRequired -> "Foreground location is used to verify checkpoints and route progress."
        LocationPermissionState.GrantedPrecise -> "Precise foreground location is available."
        LocationPermissionState.GrantedApproximate -> "Approximate location may be too coarse for checkpoint verification."
        LocationPermissionState.Denied -> "Location denied. Use indoor campus mode or request again."
        LocationPermissionState.PermanentlyDenied -> "Location permanently denied. Enable it in system settings or use indoor campus mode."
        LocationPermissionState.GpsDisabled -> "GPS/location services are disabled."
        LocationPermissionState.Unavailable -> "Location is temporarily unavailable."
    }

    fun routeLength(route: ExpeditionRoute): Double =
        route.checkpoints.sortedBy { it.order }.zipWithNext().sumOf { (a, b) -> geodesicDistanceMetres(a.coordinate, b.coordinate) }

    fun directDistance(route: ExpeditionRoute): Double =
        if (route.checkpoints.size < 2) 0.0 else geodesicDistanceMetres(route.checkpoints.first().coordinate, route.checkpoints.last().coordinate)

    fun bearingDegrees(from: MapCoordinate, to: MapCoordinate): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val dLon = (to.longitude - from.longitude).toRadians()
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (atan2(y, x).toDegrees() + 360.0) % 360.0
    }

    fun geodesicDistanceMetres(a: MapCoordinate, b: MapCoordinate): Double {
        require(a.isValid() && b.isValid()) { "Invalid coordinate." }
        val earthRadius = 6371008.8
        val dLat = (b.latitude - a.latitude).toRadians()
        val dLon = (b.longitude - a.longitude).toRadians()
        val lat1 = a.latitude.toRadians()
        val lat2 = b.latitude.toRadians()
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * earthRadius * kotlin.math.asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun verifyCheckpoint(route: ExpeditionRoute, checkpoint: ExpeditionCheckpoint, sample: LocationSample, hostAuthoritative: Boolean): CheckpointVerificationResult {
        val coordinate = sample.coordinate ?: return CheckpointVerificationResult(CheckpointVerificationStatus.LocationUnavailable, null, "Location unavailable.", hostAuthoritative, false)
        if (!sample.providerAvailable) return CheckpointVerificationResult(CheckpointVerificationStatus.LocationUnavailable, null, "Location provider unavailable.", hostAuthoritative, false)
        val accuracy = sample.accuracyMetres ?: Double.MAX_VALUE
        if (sample.approximateOnly || accuracy > checkpoint.verificationRule.maxAcceptedAccuracyMetres) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.PoorAccuracy, null, "Location accuracy is too low for checkpoint verification.", hostAuthoritative, false)
        }
        val speed = sample.speedMetresPerSecond ?: 0.0
        if (speed > 3.0) return CheckpointVerificationResult(CheckpointVerificationStatus.MovingTooFast, null, "Movement speed suggests unsafe travel. Pause and ask teacher to confirm.", hostAuthoritative, false)
        if (!pointInPolygon(coordinate, route.boundary.vertices)) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.OutsideBoundary, null, "Outside teacher-approved boundary.", hostAuthoritative, false)
        }
        if (route.noGoZones.any { pointInPolygon(coordinate, it.boundary.vertices) }) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.InNoGoZone, null, "Inside a no-go zone.", hostAuthoritative, false)
        }
        val distance = geodesicDistanceMetres(coordinate, checkpoint.coordinate)
        if (distance > checkpoint.verificationRule.radiusMetres) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.NearCheckpoint, distance, "Move closer to ${checkpoint.title}.", hostAuthoritative, false)
        }
        if (sample.elapsedDwellSeconds < checkpoint.verificationRule.requiredDwellSeconds) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.DwellTimeRequired, distance, "Hold position briefly before verification.", hostAuthoritative, false)
        }
        if (checkpoint.verificationRule.teacherConfirmationRequired && !hostAuthoritative) {
            return CheckpointVerificationResult(CheckpointVerificationStatus.TeacherConfirmationRequired, distance, "Teacher confirmation required.", hostAuthoritative, false)
        }
        return CheckpointVerificationResult(CheckpointVerificationStatus.Verified, distance, if (sample.mockFlaggedByAndroid) "Verified, but Android flagged mock-location metadata for teacher review." else "Checkpoint verified.", hostAuthoritative, true)
    }

    fun validateRoute(route: ExpeditionRoute): RouteValidationResult {
        val issues = mutableListOf<RouteValidationIssue>()
        if (route.checkpoints.size < 2) issues += RouteValidationIssue("missing_checkpoints", "At least start and finish checkpoints are required.", 3)
        if (route.checkpoints.any { !it.coordinate.isValid() }) issues += RouteValidationIssue("invalid_coordinate", "Route has invalid checkpoint coordinates.", 3)
        if (route.checkpoints.any { !pointInPolygon(it.coordinate, route.boundary.vertices) }) issues += RouteValidationIssue("checkpoint_outside_boundary", "One or more checkpoints are outside the safe boundary.", 3)
        if (route.checkpoints.any { it.mathMission.prompt.isBlank() }) issues += RouteValidationIssue("missing_mission", "Every checkpoint needs a mathematics mission.", 3)
        if (route.checkpoints.any { it.verificationRule.radiusMetres !in 3.0..80.0 }) issues += RouteValidationIssue("radius", "Checkpoint radius should be realistic: 3 m to 80 m.", 2)
        if (routeLength(route) > 3000.0) issues += RouteValidationIssue("distance", "Route is long for a school expedition. Review supervision and duration.", 2)
        route.noGoZones.forEach { zone ->
            if (route.checkpoints.any { pointInPolygon(it.coordinate, zone.boundary.vertices) }) {
                issues += RouteValidationIssue("no_go_overlap", "Checkpoint overlaps no-go zone ${zone.title}.", 3)
            }
        }
        return RouteValidationResult(issues.none { it.severity >= 3 }, issues)
    }

    fun validateImport(pkg: ExpeditionPackage): RouteValidationResult {
        val issues = mutableListOf<RouteValidationIssue>()
        if (pkg.version != MATH_EXPEDITION_ROUTE_VERSION) issues += RouteValidationIssue("version", "Unsupported route package version.", 3)
        if (pkg.checkpointCount !in 2..100) issues += RouteValidationIssue("size", "Route package checkpoint count is invalid.", 3)
        if (pkg.payload.length > 250_000) issues += RouteValidationIssue("payload", "Route package is too large.", 3)
        if (Regex("""<script|function\s*\(|class\s+|Runtime\.getRuntime""", RegexOption.IGNORE_CASE).containsMatchIn(pkg.payload)) {
            issues += RouteValidationIssue("executable", "Route packages cannot contain executable content.", 3)
        }
        return RouteValidationResult(issues.none { it.severity >= 3 }, issues)
    }

    fun exportRoute(definition: ExpeditionDefinition): ExpeditionPackage =
        ExpeditionPackage(
            version = MATH_EXPEDITION_ROUTE_VERSION,
            metadata = "${definition.expeditionId}|${definition.title}|${definition.mapProvider.providerId}",
            checkpointCount = definition.route.checkpoints.size,
            payload = definition.route.checkpoints.joinToString(";") { "${it.checkpointId},${it.coordinate.latitude},${it.coordinate.longitude},${it.mathMission.type}" }
        )

    fun validateMathMission(mission: ExpeditionMathMission, answer: Double): Boolean =
        mission.expectedNumericAnswer?.let { abs(answer - it) <= mission.tolerance } ?: true

    fun analytics(route: ExpeditionRoute, completed: Int, actualMinutes: Int, accuracySamples: List<Double>, hints: Int, arCount: Int, safetyInterruptions: Int): ExpeditionAnalyticsSummary {
        val length = routeLength(route)
        val direct = directDistance(route).coerceAtLeast(1.0)
        return ExpeditionAnalyticsSummary(
            routeId = route.routeId,
            distanceTravelledMetres = length * (completed.toDouble() / route.checkpoints.size.coerceAtLeast(1)),
            routeCompleted = completed >= route.checkpoints.size,
            estimatedVersusActualMinutes = actualMinutes - route.estimatedDurationMinutes.toDouble(),
            checkpointsCompleted = completed,
            accuracyAverageMetres = accuracySamples.average().takeIf { !it.isNaN() } ?: 0.0,
            topicPerformance = route.topics.associateWith { completed.toDouble() / route.checkpoints.size.coerceAtLeast(1) },
            routeEfficiency = direct / length.coerceAtLeast(direct),
            hintUse = hints,
            arMissionParticipation = arCount,
            teamContribution = emptyMap(),
            safetyInterruptions = safetyInterruptions
        )
    }

    fun deleteHistory(state: ExpeditionPrivacyState): ExpeditionPrivacyState = state.copy(historyEntryIds = emptySet(), locationCacheEntries = 0)
    fun deleteDownloadedMaps(state: ExpeditionPrivacyState): ExpeditionPrivacyState = state.copy(downloadedMapIds = emptySet())
    fun deleteSavedRoutes(state: ExpeditionPrivacyState): ExpeditionPrivacyState = state.copy(savedRouteIds = emptySet())

    fun pointInPolygon(point: MapCoordinate, polygon: List<MapCoordinate>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        polygon.forEachIndexed { i, current ->
            val previous = polygon[j]
            val crosses = (current.longitude > point.longitude) != (previous.longitude > point.longitude) &&
                point.latitude < (previous.latitude - current.latitude) * (point.longitude - current.longitude) / ((previous.longitude - current.longitude).takeIf { abs(it) > 0.0000001 } ?: 0.0000001) + current.latitude
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }

    private fun buildSamples(): List<ExpeditionDefinition> = listOf(
        sample("distance-discovery", "Distance Discovery", ExpeditionMode.SoloExplorer, ExpeditionDifficulty.Beginner, ExpeditionTopic.Distance),
        sample("bearing-trail", "Bearing Trail", ExpeditionMode.MathematicsTreasureRoute, ExpeditionDifficulty.Easy, ExpeditionTopic.Bearing),
        sample("speed-time-circuit", "Speed and Time Circuit", ExpeditionMode.RouteOptimisationChallenge, ExpeditionDifficulty.Medium, ExpeditionTopic.SpeedTime),
        sample("campus-geometry-survey", "Campus Geometry Survey", ExpeditionMode.CampusSurvey, ExpeditionDifficulty.Medium, ExpeditionTopic.AreaPerimeter),
        sample("statistics-field-study", "Statistics Field Study", ExpeditionMode.StatisticsFieldMission, ExpeditionDifficulty.Hard, ExpeditionTopic.Statistics)
    )

    private fun sample(id: String, title: String, mode: ExpeditionMode, difficulty: ExpeditionDifficulty, topic: ExpeditionTopic): ExpeditionDefinition {
        val base = MapCoordinate(12.9716, 77.5946)
        val coordinates = listOf(
            base,
            MapCoordinate(base.latitude + 0.00045, base.longitude),
            MapCoordinate(base.latitude + 0.00045, base.longitude + 0.00045),
            MapCoordinate(base.latitude, base.longitude + 0.00045)
        )
        val boundary = SafeBoundary(
            "$id-boundary",
            listOf(
                MapCoordinate(base.latitude - 0.0002, base.longitude - 0.0002),
                MapCoordinate(base.latitude + 0.00065, base.longitude - 0.0002),
                MapCoordinate(base.latitude + 0.00065, base.longitude + 0.00065),
                MapCoordinate(base.latitude - 0.0002, base.longitude + 0.00065)
            )
        )
        val checkpoints = coordinates.mapIndexed { index, coordinate ->
            val next = coordinates.getOrNull(index + 1) ?: coordinates.first()
            ExpeditionCheckpoint(
                checkpointId = "$id-cp-${index + 1}",
                title = "Checkpoint ${index + 1}",
                coordinate = coordinate,
                mathMission = missionFor("$id-m-${index + 1}", topic, coordinate, next, index),
                arMission = ExpeditionArMission("$id-ar-${index + 1}", "Checkpoint AR Task", optional = true, requiresGeospatial = false, nonArFallbackPrompt = "Complete the same measurement using map data.", points = 40),
                verificationRule = CheckpointVerificationRule(radiusMetres = 25.0, requiredDwellSeconds = 3, maxAcceptedAccuracyMetres = 35.0, teacherConfirmationRequired = false),
                order = index + 1
            )
        }
        val route = ExpeditionRoute(
            routeId = "$id-route",
            title = title,
            checkpoints = checkpoints,
            boundary = boundary,
            noGoZones = emptyList(),
            startZone = ExpeditionStartZone("$id-start", "Start", boundary),
            finishZone = ExpeditionFinishZone("$id-finish", "Finish", boundary),
            segments = checkpoints.zipWithNext().map { (a, b) -> RouteSegment("${a.checkpointId}-${b.checkpointId}", a.checkpointId, b.checkpointId, geodesicDistanceMetres(a.coordinate, b.coordinate), bearingDegrees(a.coordinate, b.coordinate)) },
            estimatedDurationMinutes = 20,
            difficulty = difficulty,
            grade = "Grades 6-10",
            topics = setOf(topic)
        )
        return ExpeditionDefinition(
            expeditionId = id,
            version = MATH_EXPEDITION_ROUTE_VERSION,
            title = title,
            mode = mode,
            route = route,
            mapProvider = mapProviders.first(),
            offlineRegion = OfflineMapPackageMetadata("$id-offline", "Teacher/local provider", "Provider-specific", title, 0, downloaded = false),
            approvalState = RouteApprovalState.NeedsReview,
            teamAssignments = (1..2).map { TeamAssignment("team-$it", "Team $it", checkpoints.map { cp -> cp.checkpointId }, roles) },
            teacherApprovedBy = null,
            createdAtEpochMs = 0
        )
    }

    private fun missionFor(id: String, topic: ExpeditionTopic, a: MapCoordinate, b: MapCoordinate, index: Int): ExpeditionMathMission {
        val distance = geodesicDistanceMetres(a, b)
        val bearing = bearingDegrees(a, b)
        val type = ExpeditionMissionType.entries[index % ExpeditionMissionType.entries.size]
        val expected = when (type) {
            ExpeditionMissionType.StraightLineDistance,
            ExpeditionMissionType.RouteDistance,
            ExpeditionMissionType.MapScale,
            ExpeditionMissionType.RouteOptimisation,
            ExpeditionMissionType.PercentageCompletion,
            ExpeditionMissionType.EstimationError -> distance
            ExpeditionMissionType.Bearing,
            ExpeditionMissionType.Direction -> bearing
            ExpeditionMissionType.AverageSpeed -> distance / 300.0
            ExpeditionMissionType.TimeEstimation -> 5.0
            ExpeditionMissionType.AreaEstimation -> 1000.0
            ExpeditionMissionType.Perimeter -> distance * 4
            ExpeditionMissionType.Mean -> 5.0
            ExpeditionMissionType.Median -> 5.0
            ExpeditionMissionType.Mode -> 5.0
            ExpeditionMissionType.Range -> 4.0
            ExpeditionMissionType.RatioComparison -> 1.5
            else -> null
        }
        return ExpeditionMathMission(
            missionId = id,
            type = type,
            topic = topic,
            prompt = "Use the route data to solve ${type.name}.",
            expectedNumericAnswer = expected,
            tolerance = if (expected == null) 0.0 else expected.coerceAtLeast(1.0) * 0.08,
            hints = listOf("Use checkpoint coordinates and route geometry.", "Check units before submitting."),
            workedSolution = listOf("Calculate from real route/checkpoint data.", "Use geodesic distance for geographic coordinates."),
            points = 100
        )
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0
private fun Double.toDegrees(): Double = this * 180.0 / PI
