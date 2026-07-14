package com.indianservers.ai_stem.feature.games.mission

import android.content.Context
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole

data class AntiDominationConfig(
    val requiredRoleParticipation: Set<ArenaRole> = setOf(ArenaRole.Solver, ArenaRole.Analyst, ArenaRole.Builder),
    val rotatingSubmitter: Boolean = true,
    val actionTokensPerMission: Int = 2,
    val teamConsensusRequired: Boolean = false,
    val multiDeviceConfirmationRequired: Boolean = false,
    val noImmediateRepeatSubmitter: Boolean = true,
    val inactivityThresholdSeconds: Int = 90,
    val teacherOverride: Boolean = true
)

data class PlayerParticipation(
    val playerId: String,
    val role: ArenaRole,
    val actionsTaken: Int = 0,
    val submissions: Int = 0,
    val confirmations: Int = 0,
    val lastActiveEpochMs: Long = 0
)

data class ParticipationDecision(
    val allowed: Boolean,
    val reason: String,
    val updated: PlayerParticipation?
)

class RoleClueDistributor {
    fun cluesFor(mission: MathMission): List<RoleClue> =
        ArenaRole.entries.map { role ->
            when (role) {
                ArenaRole.Navigator -> RoleClue(role, mission.prompt.navigatorClue ?: "Locate the mission zone.", false, listOf("scan", "ping-location"))
                ArenaRole.Solver -> RoleClue(role, mission.prompt.studentText, true, listOf("calculate", "submit-answer"))
                ArenaRole.Builder -> RoleClue(role, mission.prompt.builderClue ?: "Wait for a correct answer, then build.", false, listOf("place-piece", "confirm-build"))
                ArenaRole.Analyst -> RoleClue(role, mission.prompt.analystClue ?: mission.hints.firstOrNull()?.text.orEmpty(), false, listOf("request-hint", "check-formula"))
                ArenaRole.Commander -> RoleClue(role, mission.prompt.commanderClue ?: "Manage team resources.", false, listOf("approve-hint", "confirm-upgrade"))
            }
        }
}

class AntiDominationEngine(private val config: AntiDominationConfig = AntiDominationConfig()) {
    fun canAct(progress: MissionProgress, participation: PlayerParticipation, now: Long = System.currentTimeMillis()): ParticipationDecision {
        if (config.noImmediateRepeatSubmitter && progress.lastSubmitterId == participation.playerId && participation.role == ArenaRole.Solver) {
            return ParticipationDecision(false, "Another teammate must submit next.", null)
        }
        if (participation.actionsTaken >= config.actionTokensPerMission) {
            return ParticipationDecision(false, "Action tokens used. Let another teammate act.", null)
        }
        val updated = participation.copy(actionsTaken = participation.actionsTaken + 1, lastActiveEpochMs = now)
        return ParticipationDecision(true, "Action allowed.", updated)
    }

    fun participationBonus(progress: MissionProgress, participation: List<PlayerParticipation>): Int {
        val rolesUsed = participation.filter { it.actionsTaken > 0 }.map { it.role }.toSet()
        val requiredMet = config.requiredRoleParticipation.count { it in rolesUsed }
        val base = progress.mission.scoringRule.teamParticipation
        return (base * (requiredMet.toDouble() / config.requiredRoleParticipation.size)).toInt()
    }

    fun inactivePlayers(participation: List<PlayerParticipation>, now: Long): List<String> =
        participation.filter { it.lastActiveEpochMs > 0 && now - it.lastActiveEpochMs > config.inactivityThresholdSeconds * 1000L }.map { it.playerId }
}

class MissionStateMachine(
    private val validator: AnswerValidationEngine = AnswerValidationEngine()
) {
    fun transition(progress: MissionProgress, event: MissionEvent, hostAuthorized: Boolean): MissionProgress {
        require(hostAuthorized) { "Host authority is required for mission state transitions." }
        return when (event) {
            MissionEvent.StartBriefing -> progress.copy(state = MissionState.Briefing)
            MissionEvent.StartMission -> progress.copy(state = MissionState.Active, startedAtEpochMs = System.currentTimeMillis())
            MissionEvent.MakeHintAvailable -> if (progress.state == MissionState.Active) progress.copy(state = MissionState.HintAvailable) else progress
            is MissionEvent.RequestHint -> progress.copy(hintCount = progress.hintCount + 1, state = MissionState.HintAvailable)
            is MissionEvent.SubmitAnswer -> {
                val validation = validator.validate(event.attempt.answer, progress.mission.expectedAnswer, alreadySubmitted = progress.attempts.any { it.playerId == event.attempt.playerId && it.answer == event.attempt.answer }, timedOut = event.attempt.timedOut)
                val nextState = when (validation.status) {
                    ValidationStatus.Correct,
                    ValidationStatus.CorrectButNeedsSimplification -> MissionState.Correct
                    ValidationStatus.PartiallyCorrect,
                    ValidationStatus.Incorrect,
                    ValidationStatus.InvalidFormat,
                    ValidationStatus.MissingUnit,
                    ValidationStatus.WrongCoordinateOrder -> if (progress.attempts.size < 2) MissionState.IncorrectRetryAllowed else MissionState.Failed
                    ValidationStatus.TimedOut -> MissionState.Failed
                    ValidationStatus.AlreadySubmitted -> progress.state
                }
                progress.copy(
                    state = nextState,
                    attempts = progress.attempts + event.attempt,
                    usedPlayerIds = progress.usedPlayerIds + event.attempt.playerId,
                    lastSubmitterId = event.attempt.playerId,
                    validationResult = validation
                )
            }
            MissionEvent.Complete -> progress.copy(state = MissionState.Completed, completedAtEpochMs = System.currentTimeMillis())
            MissionEvent.Cancel -> progress.copy(state = MissionState.Cancelled)
        }
    }
}

sealed interface MissionEvent {
    data object StartBriefing : MissionEvent
    data object StartMission : MissionEvent
    data object MakeHintAvailable : MissionEvent
    data class RequestHint(val playerId: String) : MissionEvent
    data class SubmitAnswer(val attempt: MissionAttempt) : MissionEvent
    data object Complete : MissionEvent
    data object Cancel : MissionEvent
}

class MissionScoringEngine(
    private val antiDominationEngine: AntiDominationEngine = AntiDominationEngine()
) {
    fun score(teamId: String, progress: MissionProgress, participation: List<PlayerParticipation>, arAccurate: Boolean, explanationCorrect: Boolean): MissionScore {
        val rule = progress.mission.scoringRule
        val validation = progress.validationResult
        val answer = when (validation?.status) {
            ValidationStatus.Correct -> rule.correctAnswer
            ValidationStatus.CorrectButNeedsSimplification -> (rule.correctAnswer * 0.75).toInt()
            ValidationStatus.PartiallyCorrect -> (rule.correctAnswer * validation.scoreMultiplier).toInt()
            else -> 0
        }
        val process = if ((validation?.correctParts ?: 0) > 0) rule.correctProcessSteps else 0
        val ar = if (arAccurate) rule.accurateArInteraction else 0
        val participationPoints = antiDominationEngine.participationBonus(progress, participation)
        val hintBonus = if (progress.hintCount == 0 && validation?.correct == true) rule.noHint else 0
        val elapsed = progress.attempts.lastOrNull()?.elapsedSeconds ?: progress.mission.timeLimitSeconds
        val speed = if (validation?.correct == true) ((1.0 - elapsed.toDouble() / progress.mission.timeLimitSeconds).coerceIn(0.0, 1.0) * rule.fastCompletionMax).toInt() else 0
        val explanation = if (explanationCorrect) rule.correctExplanation else 0
        val wrongAttempts = progress.attempts.count { it.answer != progress.attempts.lastOrNull()?.answer }
        val penalties = progress.attempts.count { it.playerId.isNotBlank() && validation?.correct != true } * rule.wrongAttemptPenalty +
            wrongAttempts.coerceAtLeast(0) * rule.repeatedGuessPenalty
        val total = answer + process + ar + participationPoints + hintBonus + speed + explanation + penalties
        val contributors = participation.filter { it.actionsTaken > 0 || it.submissions > 0 }.associate { it.playerId to (participationPoints / participation.size.coerceAtLeast(1)) }
        return MissionScore(teamId, progress.mission.id, total.coerceAtLeast(0), answer, process, ar, participationPoints, hintBonus, speed, explanation, penalties, contributors, mapOf(progress.mission.topic to (validation?.scoreMultiplier ?: 0.0)))
    }
}

class MissionTemplateStore(context: Context) {
    private val prefs = context.getSharedPreferences("ar_math_arena_mission_templates_v1", Context.MODE_PRIVATE)

    fun saveTemplate(mission: MathMission) {
        prefs.edit().putString(mission.id, MissionTemplateMetadataCodec.encode(mission)).apply()
    }

    fun listTemplateMetadata(): List<String> = prefs.all.values.mapNotNull { it as? String }.sorted()

    fun clear() {
        prefs.edit().clear().apply()
    }
}

object MissionTemplateMetadataCodec {
    const val SCHEMA_VERSION = 1
    fun encode(mission: MathMission): String =
        listOf(SCHEMA_VERSION, mission.id, mission.topic.name, mission.skill.name, mission.gradeBand.name, mission.difficulty.name, mission.seed).joinToString("|")

    fun isValid(encoded: String): Boolean {
        val parts = encoded.split("|")
        return parts.size == 7 && parts.first().toIntOrNull() == SCHEMA_VERSION && parts.last().toLongOrNull() != null
    }
}
