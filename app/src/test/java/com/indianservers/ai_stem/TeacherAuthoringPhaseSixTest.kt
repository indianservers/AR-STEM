package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.authoring.AuthoredAnswerType
import com.indianservers.ai_stem.feature.games.authoring.AuthoredCampaign
import com.indianservers.ai_stem.feature.games.authoring.AuthoredMission
import com.indianservers.ai_stem.feature.games.authoring.AuthoringValidator
import com.indianservers.ai_stem.feature.games.authoring.BackupPlan
import com.indianservers.ai_stem.feature.games.authoring.BackupRestoreEngine
import com.indianservers.ai_stem.feature.games.authoring.BackupSensitivity
import com.indianservers.ai_stem.feature.games.authoring.ContentPackCodec
import com.indianservers.ai_stem.feature.games.authoring.CoordinateConquestConfig
import com.indianservers.ai_stem.feature.games.authoring.EscapePuzzleConfig
import com.indianservers.ai_stem.feature.games.authoring.FactoryOrderConfig
import com.indianservers.ai_stem.feature.games.authoring.FortressMissionConfig
import com.indianservers.ai_stem.feature.games.authoring.GamePreset
import com.indianservers.ai_stem.feature.games.authoring.PreviewEngine
import com.indianservers.ai_stem.feature.games.authoring.PublishState
import com.indianservers.ai_stem.feature.games.authoring.ReadinessDiagnosticsEngine
import com.indianservers.ai_stem.feature.games.authoring.SchoolProfile
import com.indianservers.ai_stem.feature.games.authoring.TournamentPreset
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinatePoint
import com.indianservers.ai_stem.feature.games.learning.CampaignChapter
import com.indianservers.ai_stem.feature.games.learning.CampaignNode
import com.indianservers.ai_stem.feature.games.mathexpedition.CheckpointVerificationRule
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionArMission
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionCheckpoint
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionDefinition
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionDifficulty
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionFinishZone
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionMathMission
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionMissionType
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionMode
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionRoute
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionRole
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionStartZone
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionTopic
import com.indianservers.ai_stem.feature.games.mathexpedition.MapCoordinate
import com.indianservers.ai_stem.feature.games.mathexpedition.MapProviderCapability
import com.indianservers.ai_stem.feature.games.mathexpedition.MapProviderDefinition
import com.indianservers.ai_stem.feature.games.mathexpedition.RouteApprovalState
import com.indianservers.ai_stem.feature.games.mathexpedition.RouteSegment
import com.indianservers.ai_stem.feature.games.mathexpedition.SafeBoundary
import com.indianservers.ai_stem.feature.games.mathexpedition.TeamAssignment
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.tournament.TieBreakRule
import com.indianservers.ai_stem.feature.games.tournament.TournamentEventType
import com.indianservers.ai_stem.feature.games.tournament.TournamentScoreWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherAuthoringPhaseSixTest {
    private val validator = AuthoringValidator()

    @Test
    fun missionPublishingRequiresValidationAndPreview() {
        val mission = mission(previewCompletions = 0)
        val previewed = mission.copy(previewCompletions = 1, state = PublishState.Previewed)
        val invalid = mission.copy(expectedAnswer = "")

        assertTrue(validator.validateMission(mission).publishable)
        assertEquals(PublishState.ReadyForPreview, validator.publishState(mission))
        assertEquals(PublishState.ReadyToPublish, validator.publishState(previewed))
        assertEquals(PublishState.ValidationFailed, validator.publishState(invalid))
    }

    @Test
    fun gameSpecificEditorsRejectUnsafeOrUnplayableContent() {
        assertFalse(validator.validateFortress(FortressMissionConfig(0, emptyMap(), "", emptySet(), 1)).publishable)
        assertFalse(validator.validateEscape(EscapePuzzleConfig(setOf("a"), setOf("a"), emptySet(), listOf("a" to "a"), mapOf("a" to "2"))).publishable)
        assertFalse(validator.validateFactory(FactoryOrderConfig("1//2", "litres", emptyList(), -1.0)).publishable)
        assertFalse(validator.validateCoordinate(CoordinateConquestConfig(4, 4, listOf(CoordinatePoint(9.0, 0.0)), tabletopAllowed = true)).publishable)
        assertTrue(validator.validateFactory(FactoryOrderConfig("3/4", "litres", listOf("mix", "seal"), 0.01)).publishable)
    }

    @Test
    fun campaignPresetsAndTournamentPresetsAreValidated() {
        val campaign = AuthoredCampaign(
            campaignId = "c1",
            title = "Coordinate Run",
            chapters = listOf(CampaignChapter("chapter", "Slope", listOf(node("a", setOf("b")), node("b", emptySet())))),
            masterySkillIds = setOf("coordinate-slope"),
            completionCriteria = "Complete final node."
        )
        val gamePreset = GamePreset("p1", "coordinate_conquest_ar", GradeBand.Grade8, "Coordinates", DifficultyLevel.Medium, 12, 3, setOf("Navigator"), true, setOf("large-labels"), "balanced", "local-wifi", "local", true)
        val tournament = TournamentPreset("t1", "Coordinate Cup", listOf("coordinate_conquest_ar"), TournamentEventType.MixedGameChampionship, 3, 4, "balanced", TournamentScoreWeights(), listOf(TieBreakRule.HigherAccuracy), setOf("tabletop"), false, 20, true)

        assertTrue(validator.validateCampaign(campaign).publishable)
        assertTrue(validator.validatePreset(gamePreset).publishable)
        assertTrue(validator.validateTournamentPreset(tournament).publishable)
        assertFalse(validator.validateTournamentPreset(tournament.copy(gameIds = emptyList())).publishable)
    }

    @Test
    fun routeAuthoringRequiresTeacherApprovalAndSafeGeometry() {
        val approved = expedition(RouteApprovalState.TeacherApproved)
        val draft = expedition(RouteApprovalState.Draft)

        assertTrue(validator.validateExpedition(com.indianservers.ai_stem.feature.games.authoring.ExpeditionRouteConfig(approved, safetyApproved = true)).publishable)
        assertFalse(validator.validateExpedition(com.indianservers.ai_stem.feature.games.authoring.ExpeditionRouteConfig(draft, safetyApproved = true)).publishable)
        assertFalse(validator.validateExpedition(com.indianservers.ai_stem.feature.games.authoring.ExpeditionRouteConfig(approved, safetyApproved = false)).publishable)
    }

    @Test
    fun previewModeDoesNotMutateProgressOrAwardBadges() {
        val preview = PreviewEngine(validator).previewMission(mission(previewCompletions = 1), "2/3")

        assertTrue(preview.valid)
        assertTrue(preview.answerAccepted)
        assertFalse(preview.progressMutated)
        assertFalse(preview.badgesAwarded)
    }

    @Test
    fun contentPackImportSecurityRejectsTraversalScriptsExecutablesUrlsAndMissingChecksum() {
        val codec = ContentPackCodec(validator)

        assertTrue(codec.validateImport("pack|safe|checksum=abc", byteSize = 24, entryCount = 1).publishable)
        assertFalse(codec.validateImport("../pack|checksum=abc", byteSize = 24, entryCount = 1).publishable)
        assertFalse(codec.validateImport("<script>alert(1)</script>|checksum=abc", byteSize = 60, entryCount = 1).publishable)
        assertFalse(codec.validateImport("lib.dll|checksum=abc", byteSize = 24, entryCount = 1).publishable)
        assertFalse(codec.validateImport("https://evil.test/pack|checksum=abc", byteSize = 48, entryCount = 1).publishable)
        assertFalse(codec.validateImport("pack-without-integrity", byteSize = 24, entryCount = 1).publishable)
        assertFalse(codec.validateImport("pack|checksum=abc", byteSize = 5_000_001, entryCount = 1).publishable)
    }

    @Test
    fun conflictResolutionAndBackupRestoreAreConservative() {
        val codec = ContentPackCodec(validator)
        val conflicts = codec.conflicts(mapOf("m1" to 1, "m2" to 2), mapOf("m1" to 2, "m2" to 1), localModifiedIds = setOf("m1"))
        val backup = BackupRestoreEngine().createBackup(
            BackupPlan(true, true, true, true, true, true, BackupSensitivity.IncludeTournamentHistory),
            secureKeyManagementAvailable = false
        )
        val restore = BackupRestoreEngine().restore(backup, setOf("missions", "routes"))

        assertEquals(2, conflicts.size)
        assertEquals(BackupSensitivity.IncludeTeacherContent, backup.sensitivity)
        assertFalse(backup.encrypted)
        assertTrue(restore.publishable)
    }

    @Test
    fun schoolProfileReadinessDiagnosticsPerformanceSecurityAndPrivacyStayLocalAndAnonymized() {
        val school = SchoolProfile("Local School", "2026", setOf(GradeBand.Grade8), listOf("Euler"), listOf("Clear floor"), 12, setOf("large-labels"), true, null)
        val diagnostics = ReadinessDiagnosticsEngine()
        val readiness = diagnostics.readinessReport(listOf(com.indianservers.ai_stem.feature.games.authoring.DeviceReadiness("Device A", true, true, true, true, true, false, true, true, 80, 2000, "nominal", true, true, true, emptySet())))
        val unsafe = diagnostics.diagnostics(com.indianservers.ai_stem.feature.games.authoring.DiagnosticsPackage("1", "14", "model", "ar", "network", emptyList(), 1, emptyMap(), "map", "renderer", includesCameraImagery = true))

        assertEquals("Local School", school.schoolDisplayName)
        assertTrue(readiness.contains("Device A"))
        assertFalse(readiness.contains("@"))
        assertFalse(unsafe.publishable)
        assertTrue(diagnostics.performanceAudit().isNotEmpty())
        assertTrue(diagnostics.securityAudit().all { it.passed })
        assertTrue(diagnostics.privacyAudit().all { it.passed })
    }

    private fun mission(previewCompletions: Int = 0): AuthoredMission = AuthoredMission(
        missionId = "m1",
        title = "Slope Bridge",
        gameId = "coordinate_conquest_ar",
        topic = "Coordinates",
        skillId = "coordinate-slope",
        grade = GradeBand.Grade8,
        difficulty = DifficultyLevel.Medium,
        prompt = "Build a bridge from (0, 0) to (6, 4) and compute the slope.",
        answerType = AuthoredAnswerType.Fraction,
        expectedAnswer = "2/3",
        units = null,
        tolerance = null,
        workedSolution = listOf("Rise is 4.", "Run is 6.", "Slope is 2/3."),
        hints = listOf("Use rise over run.", "Simplify 4/6."),
        commonMisconception = null,
        timeLimitSeconds = 180,
        score = 200,
        requiredRoles = setOf("Navigator"),
        arInteractionType = "Markerless AR coordinate placement",
        accessibilityAlternative = "Tabletop graph input",
        prerequisiteSkillIds = emptySet(),
        tags = setOf("coordinates"),
        previewCompletions = previewCompletions
    )

    private fun node(id: String, unlocks: Set<String>): CampaignNode = CampaignNode(id, id, "coordinate_conquest_ar", "coordinate-slope", id.hashCode().toLong(), unlocks, optional = false, finalChallenge = unlocks.isEmpty(), difficulty = DifficultyLevel.Medium)

    private fun expedition(state: RouteApprovalState): ExpeditionDefinition {
        val boundary = SafeBoundary("boundary", listOf(MapCoordinate(12.0, 77.0), MapCoordinate(12.0, 77.01), MapCoordinate(12.01, 77.01), MapCoordinate(12.01, 77.0)))
        val mission = ExpeditionMathMission("em1", ExpeditionMissionType.StraightLineDistance, ExpeditionTopic.Distance, "Estimate distance.", 10.0, 1.0, listOf("Use map scale."), listOf("Measure.", "Convert."), 20)
        val ar = ExpeditionArMission("ar1", "Distance AR", optional = true, requiresGeospatial = false, nonArFallbackPrompt = "Use map view.", points = 10)
        val checkpointA = ExpeditionCheckpoint("a", "A", MapCoordinate(12.002, 77.002), mission, ar, CheckpointVerificationRule(10.0, 2, 20.0, false), 1)
        val checkpointB = ExpeditionCheckpoint("b", "B", MapCoordinate(12.004, 77.004), mission.copy(missionId = "em2"), null, CheckpointVerificationRule(10.0, 2, 20.0, false), 2)
        val route = ExpeditionRoute(
            routeId = "route",
            title = "Route",
            checkpoints = listOf(checkpointA, checkpointB),
            boundary = boundary,
            noGoZones = emptyList(),
            startZone = ExpeditionStartZone("start", "Start", boundary),
            finishZone = ExpeditionFinishZone("finish", "Finish", boundary),
            segments = listOf(RouteSegment("s1", "a", "b", 200.0, 45.0)),
            estimatedDurationMinutes = 15,
            difficulty = ExpeditionDifficulty.Easy,
            grade = "Grade 8",
            topics = setOf(ExpeditionTopic.Distance)
        )
        return ExpeditionDefinition(
            expeditionId = "expedition",
            version = 1,
            title = "Campus Distance",
            mode = ExpeditionMode.CampusSurvey,
            route = route,
            mapProvider = MapProviderDefinition("local", "Local", "Open map attribution", null, setOf(MapProviderCapability.CampusImage), offlineDownloadAvailable = false, usagePolicySummary = "Local package"),
            offlineRegion = null,
            approvalState = state,
            teamAssignments = listOf(TeamAssignment("team", "Team", listOf("a", "b"), listOf(ExpeditionRole.Navigator))),
            teacherApprovedBy = state.takeIf { it == RouteApprovalState.TeacherApproved }?.let { "teacher" },
            createdAtEpochMs = 1L
        )
    }
}
