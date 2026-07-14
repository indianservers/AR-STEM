package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseConfig
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMode
import com.indianservers.ai_stem.feature.games.basedefense.TeamBase
import com.indianservers.ai_stem.feature.games.classroom.AccessibilitySettings
import com.indianservers.ai_stem.feature.games.classroom.AdaptiveDifficultyEngine
import com.indianservers.ai_stem.feature.games.classroom.BoundaryPolicy
import com.indianservers.ai_stem.feature.games.classroom.BoundarySafetyEngine
import com.indianservers.ai_stem.feature.games.classroom.BoundaryWarning
import com.indianservers.ai_stem.feature.games.classroom.ContentMissionTemplate
import com.indianservers.ai_stem.feature.games.classroom.ContentPack
import com.indianservers.ai_stem.feature.games.classroom.ContentPackValidator
import com.indianservers.ai_stem.feature.games.classroom.HostIntervention
import com.indianservers.ai_stem.feature.games.classroom.HostInterventionType
import com.indianservers.ai_stem.feature.games.classroom.MatchAnalyticsEngine
import com.indianservers.ai_stem.feature.games.classroom.MatchReportExporter
import com.indianservers.ai_stem.feature.games.classroom.MissionResultRecord
import com.indianservers.ai_stem.feature.games.classroom.PlayerParticipationRecord
import com.indianservers.ai_stem.feature.games.classroom.RecentMissionPerformance
import com.indianservers.ai_stem.feature.games.classroom.TeacherDashboardReducer
import com.indianservers.ai_stem.feature.games.classroom.TeamSymbol
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.InteractionType
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaClassroomDashboardAnalyticsTest {
    @Test
    fun accessibilitySettingsAffectGameplayConfigAndKeepColorIndependentIdentity() {
        val settings = AccessibilitySettings(largeText = true, highContrast = true, seatedPlayMode = true, reducedMotion = true)
        val config = settings.applyTo(BaseDefenseConfig(maxClassroomRadiusMetres = 5f, depthOcclusionEnabled = true))

        assertEquals(2.0f, config.maxClassroomRadiusMetres, 0.0001f)
        assertTrue(config.lowPerformanceMode)
        assertFalse(config.depthOcclusionEnabled)
        assertTrue(TeamSymbol.entries.size >= 4)
    }

    @Test
    fun hostControlsRequireAuthorityAndLogInterventions() {
        val reducer = TeacherDashboardReducer()
        val intervention = HostIntervention("i1", "match", HostInterventionType.GiveHint, "m1", "Hint approved")

        assertTrue(runCatching { reducer.logIntervention(emptyList(), intervention, hostAuthorized = false) }.isFailure)
        val logged = reducer.logIntervention(emptyList(), intervention, hostAuthorized = true)
        assertEquals(1, logged.size)
    }

    @Test
    fun adaptiveDifficultyIsExplainableAndAvoidsAbruptJumps() {
        val history = listOf(
            RecentMissionPerformance(MathTopic.Fractions, MathSkill.FractionAddSubtract, ValidationStatus.Incorrect, 80, 2, 2),
            RecentMissionPerformance(MathTopic.Fractions, MathSkill.FractionAddSubtract, ValidationStatus.Incorrect, 75, 2, 1),
            RecentMissionPerformance(MathTopic.Algebra, MathSkill.TwoStepEquation, ValidationStatus.Correct, 60, 1, 1)
        )
        val decision = AdaptiveDifficultyEngine().decide(DifficultyLevel.Medium, history, enabled = true)

        assertEquals(DifficultyLevel.Easy, decision.next)
        assertTrue(decision.reason.contains("accuracy", ignoreCase = true))
    }

    @Test
    fun analyticsCalculatesTeamTopicAndPrivacySafeReport() {
        val match = BaseDefenseMatchState(
            matchId = "m",
            hostPlayerId = "host",
            config = BaseDefenseConfig(mode = BaseDefenseMode.Competitive),
            bases = listOf(TeamBase("red", "Red Team", SharedTransform(positionMetres = Vector3Dto(1f, 0f, 1f)), score = 220, health = 400))
        )
        val missions = listOf(
            MissionResultRecord("mission1", "red", MathTopic.Fractions, MathSkill.FractionAddSubtract, ValidationStatus.Correct, 30, 0, 0, null),
            MissionResultRecord("mission2", "red", MathTopic.Algebra, MathSkill.TwoStepEquation, ValidationStatus.Incorrect, 70, 2, 1, "Sign error")
        )
        val players = listOf(PlayerParticipationRecord("local-p1", "Player 1", mapOf(ArenaRole.Solver to 1), 4, 2, 0, 0.75))

        val summary = MatchAnalyticsEngine().summarize(match, missions, players, emptyList(), individualHistoryEnabled = false)
        val json = MatchReportExporter.toJson(summary, includeIndividuals = false)
        val csv = MatchReportExporter.toCsv(summary, includeIndividuals = false)

        assertEquals(1, summary.teamSummaries.size)
        assertEquals(2, summary.topicPerformance.size)
        assertTrue(json.contains("\"teams\""))
        assertFalse(json.contains("local-p1"))
        assertTrue(csv.contains("team,red"))
    }

    @Test
    fun individualSummariesArePrivateWhenEnabledOnly() {
        val match = BaseDefenseMatchState("m2", "host", bases = listOf(TeamBase("blue", "Blue Team", SharedTransform(positionMetres = Vector3Dto(1f, 0f, 1f)))))
        val player = PlayerParticipationRecord("anon-1", "Player 1", mapOf(ArenaRole.Analyst to 1), 3, 1, 1, 1.0)
        val summary = MatchAnalyticsEngine().summarize(match, emptyList(), listOf(player), emptyList(), individualHistoryEnabled = true)

        assertTrue(MatchReportExporter.toJson(summary, includeIndividuals = true).contains("anon-1"))
        assertFalse(MatchReportExporter.toJson(summary, includeIndividuals = false).contains("anon-1"))
    }

    @Test
    fun contentPackValidationRejectsMalformedContent() {
        val valid = ContentPack(
            name = "Local Pack",
            templates = listOf(ContentMissionTemplate("template-1", MathTopic.Integers, MathSkill.IntegerOperations, "What is 4 + 5?", "9", "Add 4 and 5.", "Add the numbers.", DifficultyLevel.Easy, GradeBand.Grade6, 30, InteractionType.NonArCalculation))
        )
        val invalid = valid.copy(templates = listOf(valid.templates.first().copy(id = "!", answer = "")))

        assertTrue(ContentPackValidator().validate(valid).isSuccess)
        assertTrue(ContentPackValidator().validate(invalid).isFailure)
    }

    @Test
    fun boundaryWarningsProtectClassroomSafety() {
        val engine = BoundarySafetyEngine()
        val policy = BoundaryPolicy(maxRadiusMetres = 5f, warningRadiusMetres = 4f)

        assertEquals(BoundaryWarning.Safe, engine.evaluate(Vector3Dto(1f, 0f, 1f), policy))
        assertEquals(BoundaryWarning.NearBoundary, engine.evaluate(Vector3Dto(4.2f, 0f, 0f), policy))
        assertEquals(BoundaryWarning.OutsideBoundary, engine.evaluate(Vector3Dto(6f, 0f, 0f), policy))
    }
}
