package com.indianservers.ai_stem.feature.games.classroom

import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMode
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseConfig
import com.indianservers.ai_stem.feature.games.basedefense.MatchPace
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.InteractionType
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.spatial.CalibrationState
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto

enum class HintPolicy { HostApproval, AutoAfterDelay, Disabled }
enum class RetryPolicy { NoRetry, OneRetry, TwoRetries, UnlimitedPractice }
enum class MarkerMode { PrintedMarker, SurfaceFallback, NonArAnalyst }
enum class TeamSymbol { Star, Triangle, Circle, Square, Diamond, Hexagon }
enum class HostInterventionType {
    Pause,
    Resume,
    GiveHint,
    ReplaceMission,
    ReduceDifficulty,
    IncreaseDifficulty,
    ReassignRole,
    MovePlayerTeam,
    RecalibratePlayer,
    RemovePlayer,
    SkipBrokenArObject,
    EndRound,
    EndMatch
}

data class AccessibilitySettings(
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val colorBlindSafe: Boolean = true,
    val reducedMotion: Boolean = false,
    val reducedParticles: Boolean = false,
    val screenReaderLabels: Boolean = true,
    val hapticAlternatives: Boolean = true,
    val audioCaptions: Boolean = true,
    val oneHandedControls: Boolean = false,
    val leftHandedLayout: Boolean = false,
    val extendedAnswerTimeMultiplier: Double = 1.25,
    val simplifiedArGuidance: Boolean = false,
    val seatedPlayMode: Boolean = false,
    val nonArParticipation: Boolean = true
) {
    fun applyTo(config: BaseDefenseConfig): BaseDefenseConfig =
        config.copy(
            maxClassroomRadiusMetres = if (seatedPlayMode) 2.0f else config.maxClassroomRadiusMetres,
            lowPerformanceMode = reducedMotion || reducedParticles || config.lowPerformanceMode,
            depthOcclusionEnabled = config.depthOcclusionEnabled && !reducedMotion,
            hapticsEnabled = config.hapticsEnabled && !hapticAlternatives
        )
}

data class TeacherMatchSettings(
    val gradeBand: GradeBand = GradeBand.Mixed,
    val topics: Set<MathTopic> = setOf(MathTopic.Integers, MathTopic.Fractions, MathTopic.Algebra, MathTopic.CoordinateGeometry),
    val difficulty: DifficultyLevel = DifficultyLevel.Medium,
    val teamCount: Int = 2,
    val playerBalancing: Boolean = true,
    val roleRotation: Boolean = true,
    val matchDurationMinutes: Int = 12,
    val hintPolicy: HintPolicy = HintPolicy.HostApproval,
    val retryPolicy: RetryPolicy = RetryPolicy.TwoRetries,
    val adaptiveDifficulty: Boolean = true,
    val mode: BaseDefenseMode = BaseDefenseMode.Competitive,
    val markerMode: MarkerMode = MarkerMode.PrintedMarker,
    val accessibilityDefaults: AccessibilitySettings = AccessibilitySettings()
) {
    fun sanitized(): TeacherMatchSettings = copy(
        teamCount = teamCount.coerceIn(1, 4),
        matchDurationMinutes = matchDurationMinutes.coerceIn(5, 45)
    )
}

data class PlayerClassroomState(
    val playerId: String,
    val displayName: String,
    val teamId: String?,
    val role: ArenaRole?,
    val connected: Boolean,
    val calibrationState: CalibrationState?,
    val nonArParticipant: Boolean = false,
    val teamSymbol: TeamSymbol = TeamSymbol.Star
)

data class HostIntervention(
    val id: String,
    val matchId: String,
    val type: HostInterventionType,
    val targetId: String?,
    val reason: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AdaptiveDifficultyDecision(
    val previous: DifficultyLevel,
    val next: DifficultyLevel,
    val reason: String,
    val publicMessage: String = "Difficulty adjusted for the next mission."
)

data class RecentMissionPerformance(
    val topic: MathTopic,
    val skill: MathSkill,
    val status: ValidationStatus,
    val responseSeconds: Int,
    val hintCount: Int,
    val retryCount: Int
)

data class BoundaryPolicy(
    val origin: Vector3Dto = Vector3Dto(0f, 0f, 0f),
    val maxRadiusMetres: Float = 5f,
    val warningRadiusMetres: Float = 4.3f
)

enum class BoundaryWarning { Safe, NearBoundary, OutsideBoundary }

data class NetworkArHealth(
    val playerId: String,
    val latencyMs: Int,
    val reconnectionCount: Int,
    val calibrationQuality: Int,
    val trackingWarnings: Int,
    val lowFrameRate: Boolean,
    val thermalWarning: Boolean,
    val batteryWarning: Boolean,
    val unsupportedFeatures: List<String>
)

data class MissionResultRecord(
    val missionId: String,
    val teamId: String,
    val topic: MathTopic,
    val skill: MathSkill,
    val status: ValidationStatus,
    val responseSeconds: Int,
    val hintCount: Int,
    val retryCount: Int,
    val commonMisconception: String?
)

data class PlayerParticipationRecord(
    val playerId: String,
    val localAlias: String,
    val roleCounts: Map<ArenaRole, Int>,
    val actionCount: Int,
    val answerContributionCount: Int,
    val hintUse: Int,
    val accuracy: Double
)

data class TeamSummaryRecord(
    val teamId: String,
    val teamName: String,
    val symbol: TeamSymbol,
    val finalScore: Int,
    val accuracy: Double,
    val averageResponseSeconds: Double,
    val hintUse: Int,
    val missionsCompleted: Int,
    val baseHealth: Int,
    val resourcesCollected: Int,
    val participationBalance: Double,
    val bossContribution: Int
)

data class TopicPerformanceRecord(
    val topic: MathTopic,
    val attempts: Int,
    val correct: Int,
    val accuracy: Double,
    val averageResponseSeconds: Double,
    val commonMisconceptions: List<String>
)

data class MatchSummaryRecord(
    val schemaVersion: Int = 1,
    val matchId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val mode: BaseDefenseMode,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val finalStage: String,
    val teamSummaries: List<TeamSummaryRecord>,
    val topicPerformance: List<TopicPerformanceRecord>,
    val missionResults: List<MissionResultRecord>,
    val playerParticipation: List<PlayerParticipationRecord>,
    val interventions: List<HostIntervention>,
    val networkInterruptions: List<String>,
    val arCalibrationIssues: List<String>,
    val individualHistoryEnabled: Boolean
)

data class ContentMissionTemplate(
    val id: String,
    val topic: MathTopic,
    val skill: MathSkill,
    val question: String,
    val answer: String,
    val workedSolution: String,
    val hint: String,
    val difficulty: DifficultyLevel,
    val gradeBand: GradeBand,
    val timeLimitSeconds: Int,
    val interactionType: InteractionType
)

data class ContentPack(
    val schemaVersion: Int = 1,
    val name: String,
    val templates: List<ContentMissionTemplate>
)
