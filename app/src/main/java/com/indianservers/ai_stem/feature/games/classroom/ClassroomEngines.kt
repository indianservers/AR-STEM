package com.indianservers.ai_stem.feature.games.classroom

import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.hypot

class AdaptiveDifficultyEngine {
    fun decide(current: DifficultyLevel, history: List<RecentMissionPerformance>, enabled: Boolean): AdaptiveDifficultyDecision {
        if (!enabled || history.size < 3) return AdaptiveDifficultyDecision(current, current, "Adaptive difficulty disabled or insufficient history.")
        val recent = history.takeLast(5)
        val accuracy = recent.count { it.status == ValidationStatus.Correct }.toDouble() / recent.size
        val avgTime = recent.map { it.responseSeconds }.average()
        val avgHints = recent.map { it.hintCount }.average()
        val next = when {
            accuracy >= 0.85 && avgHints < 0.5 && avgTime < 35 -> current.step(1)
            accuracy <= 0.45 || avgHints > 1.5 -> current.step(-1)
            else -> current
        }
        val reason = "Recent accuracy ${(accuracy * 100).toInt()}%, average time ${avgTime.toInt()}s, hints ${"%.1f".format(avgHints)}."
        return AdaptiveDifficultyDecision(current, next, reason)
    }

    private fun DifficultyLevel.step(delta: Int): DifficultyLevel {
        val ordered = listOf(DifficultyLevel.Beginner, DifficultyLevel.Easy, DifficultyLevel.Medium, DifficultyLevel.Hard, DifficultyLevel.Expert)
        val index = ordered.indexOf(this).takeIf { it >= 0 } ?: 2
        return ordered[(index + delta).coerceIn(0, ordered.lastIndex)]
    }
}

class TeacherDashboardReducer {
    fun logIntervention(interventions: List<HostIntervention>, intervention: HostIntervention, hostAuthorized: Boolean): List<HostIntervention> {
        require(hostAuthorized) { "Host-only control." }
        return interventions + intervention
    }

    fun applyAccessibility(config: com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseConfig, settings: AccessibilitySettings) =
        settings.applyTo(config)
}

class BoundarySafetyEngine {
    fun evaluate(position: Vector3Dto, policy: BoundaryPolicy): BoundaryWarning {
        val distance = hypot((position.x - policy.origin.x).toDouble(), (position.z - policy.origin.z).toDouble()).toFloat()
        return when {
            distance > policy.maxRadiusMetres -> BoundaryWarning.OutsideBoundary
            distance >= policy.warningRadiusMetres -> BoundaryWarning.NearBoundary
            else -> BoundaryWarning.Safe
        }
    }
}

class MatchAnalyticsEngine {
    fun summarize(
        match: BaseDefenseMatchState,
        missionResults: List<MissionResultRecord>,
        participation: List<PlayerParticipationRecord>,
        interventions: List<HostIntervention>,
        individualHistoryEnabled: Boolean
    ): MatchSummaryRecord {
        val teams = match.bases.map { base ->
            val teamMissions = missionResults.filter { it.teamId == base.teamId }
            val correct = teamMissions.count { it.status == ValidationStatus.Correct || it.status == ValidationStatus.CorrectButNeedsSimplification }
            TeamSummaryRecord(
                teamId = base.teamId,
                teamName = base.teamName,
                symbol = TeamSymbol.entries.getOrElse(match.bases.indexOf(base) % TeamSymbol.entries.size) { TeamSymbol.Star },
                finalScore = base.score,
                accuracy = if (teamMissions.isEmpty()) 0.0 else correct.toDouble() / teamMissions.size,
                averageResponseSeconds = teamMissions.map { it.responseSeconds }.average().takeIf { !it.isNaN() } ?: 0.0,
                hintUse = teamMissions.sumOf { it.hintCount },
                missionsCompleted = correct,
                baseHealth = base.health,
                resourcesCollected = match.resources.count { it.owningTeamId == base.teamId && it.lifecycle.name == "Collected" },
                participationBalance = participationBalance(participation),
                bossContribution = match.boss?.completedRoles?.size ?: 0
            )
        }
        val topics = missionResults.groupBy { it.topic }.map { (topic, records) ->
            val correct = records.count { it.status == ValidationStatus.Correct || it.status == ValidationStatus.CorrectButNeedsSimplification }
            TopicPerformanceRecord(
                topic = topic,
                attempts = records.size,
                correct = correct,
                accuracy = if (records.isEmpty()) 0.0 else correct.toDouble() / records.size,
                averageResponseSeconds = records.map { it.responseSeconds }.average().takeIf { !it.isNaN() } ?: 0.0,
                commonMisconceptions = records.mapNotNull { it.commonMisconception }.distinct().take(3)
            )
        }
        return MatchSummaryRecord(
            matchId = match.matchId,
            startedAtEpochMs = 0,
            endedAtEpochMs = System.currentTimeMillis(),
            mode = match.config.mode,
            gradeBand = com.indianservers.ai_stem.feature.games.mission.GradeBand.Mixed,
            difficulty = com.indianservers.ai_stem.feature.games.mission.DifficultyLevel.Medium,
            finalStage = match.stage.name,
            teamSummaries = teams,
            topicPerformance = topics,
            missionResults = missionResults,
            playerParticipation = if (individualHistoryEnabled) participation else emptyList(),
            interventions = interventions,
            networkInterruptions = emptyList(),
            arCalibrationIssues = emptyList(),
            individualHistoryEnabled = individualHistoryEnabled
        )
    }

    fun strongSkills(summary: MatchSummaryRecord): List<MathTopic> = summary.topicPerformance.filter { it.accuracy >= 0.8 }.map { it.topic }
    fun needsPractice(summary: MatchSummaryRecord): List<MathTopic> = summary.topicPerformance.filter { it.attempts > 0 && it.accuracy < 0.6 }.map { it.topic }

    private fun participationBalance(records: List<PlayerParticipationRecord>): Double {
        if (records.isEmpty()) return 0.0
        val actions = records.map { it.actionCount.toDouble() }
        val max = actions.maxOrNull() ?: 0.0
        val min = actions.minOrNull() ?: 0.0
        return if (max == 0.0) 0.0 else min / max
    }
}

class ContentPackValidator {
    fun validate(pack: ContentPack): Result<Unit> = runCatching {
        require(pack.schemaVersion == 1) { "Unsupported content schema." }
        require(pack.name.isNotBlank()) { "Content pack name is required." }
        require(pack.templates.isNotEmpty()) { "At least one mission template is required." }
        pack.templates.forEach {
            require(it.id.matches(Regex("[A-Za-z0-9_.-]{3,48}"))) { "Invalid template id." }
            require(it.question.length in 8..400) { "Question length is invalid." }
            require(it.answer.isNotBlank() && it.answer.length <= 120) { "Answer is invalid." }
            require(it.workedSolution.length in 8..600) { "Worked solution is required." }
            require(it.hint.length in 3..240) { "Hint is invalid." }
            require(it.timeLimitSeconds in 10..300) { "Time limit is invalid." }
        }
    }
}
