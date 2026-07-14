package com.indianservers.ai_stem.feature.games.tournament

import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import kotlin.math.abs

const val TOURNAMENT_SCHEMA_VERSION = 1

enum class TournamentEventType {
    SingleGameKnockout,
    SingleGameRoundRobin,
    MixedGameChampionship,
    TeamLeague,
    CooperativeClassQuest,
    SkillDecathlon,
    HouseCompetition,
    TeacherCreatedCustomEvent
}

enum class TournamentFormat { Knockout, RoundRobin, Swiss, Cooperative }
enum class TournamentState { Draft, Registration, ReadyCheck, Scheduled, MatchPreparing, MatchActive, MatchPaused, ResultPending, ResultConfirmed, BetweenRounds, Completed, Cancelled, RecoveryRequired }
enum class TournamentRole { Host, Player, Spectator, ClassroomDisplay }
enum class ReplayEventType { MatchStart, MissionAssignment, ValidAnswerResult, ResourceCollection, Construction, TerritoryCapture, CheckpointCompletion, ScoreChange, Pause, Reconnection, MatchCompletion, TeacherOverride }

data class TournamentPlayer(
    val playerId: String,
    val displayName: String,
    val temporaryGuest: Boolean = false,
    val accessibilityNeeds: Set<String> = emptySet(),
    val connected: Boolean = true,
    val reconnectToken: String? = null
)

data class TournamentTeam(
    val teamId: String,
    val name: String,
    val icon: String,
    val houseGroup: String? = null,
    val playerIds: List<String>,
    val withdrawn: Boolean = false
)

data class TournamentGameAdapter(
    val gameId: String,
    val title: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val minTeams: Int,
    val maxTeams: Int,
    val matchDurationMinutes: Int,
    val scoringInputs: Set<String>,
    val tieBreakRules: List<TieBreakRule>,
    val replaySupported: Boolean,
    val safetyRestrictions: List<String>,
    val deviceRequirements: List<String>
)

data class TournamentScoreWeights(
    val accuracy: Double = 0.28,
    val completion: Double = 0.22,
    val difficulty: Double = 0.16,
    val timeEfficiency: Double = 0.12,
    val hintIndependence: Double = 0.08,
    val teamParticipation: Double = 0.08,
    val gameObjective: Double = 0.04,
    val safetyCompliance: Double = 0.02
) {
    init {
        val total = listOf(accuracy, completion, difficulty, timeEfficiency, hintIndependence, teamParticipation, gameObjective, safetyCompliance).sum()
        require(abs(total - 1.0) < 0.001) { "Tournament weights must total 1.0." }
        require(listOf(accuracy, completion, difficulty, timeEfficiency, hintIndependence, teamParticipation, gameObjective, safetyCompliance).all { it in 0.0..0.5 }) {
            "Each score weight must stay within validated ranges."
        }
    }
}

data class GameRoundResultInput(
    val accuracy: Double,
    val completion: Double,
    val difficulty: Double,
    val timeEfficiency: Double,
    val hintIndependence: Double,
    val teamParticipation: Double,
    val gameObjective: Double,
    val safetyCompliance: Double,
    val hintsUsed: Int,
    val validatedCompletionSeconds: Int,
    val teacherConfirmed: Boolean
)

data class NormalizedScore(
    val teamId: String,
    val matchId: String,
    val gameId: String,
    val total: Double,
    val formula: String,
    val input: GameRoundResultInput
)

enum class TieBreakRule { HigherAccuracy, HigherDifficulty, FewerHints, BetterParticipationBalance, FasterValidatedCompletion, TeacherApprovedTieBreakMission, ExplicitRandomDraw }

data class TournamentMatch(
    val matchId: String,
    val round: Int,
    val gameId: String,
    val teamIds: List<String>,
    val scheduledOrder: Int,
    val state: TournamentState = TournamentState.Scheduled,
    val resultIds: List<String> = emptyList(),
    val byeTeamId: String? = null
)

data class TournamentRound(val round: Int, val title: String, val matches: List<TournamentMatch>)
data class TournamentBracket(val eventId: String, val format: TournamentFormat, val rounds: List<TournamentRound>)

data class TournamentEvent(
    val eventId: String,
    val eventCode: String,
    val title: String,
    val type: TournamentEventType,
    val format: TournamentFormat,
    val selectedGameIds: List<String>,
    val teams: List<TournamentTeam>,
    val players: List<TournamentPlayer>,
    val bracket: TournamentBracket,
    val state: TournamentState,
    val scoreWeights: TournamentScoreWeights,
    val spectatorLimit: Int,
    val replayRecordingEnabled: Boolean,
    val createdAtEpochMs: Long
)

data class SignedMatchResult(
    val resultId: String,
    val matchId: String,
    val gameId: String,
    val teamId: String,
    val normalizedScore: NormalizedScore,
    val hostPlayerId: String,
    val hostValidated: Boolean,
    val sequence: Long
)

data class ReplayEvent(
    val eventId: String,
    val matchId: String,
    val sequence: Long,
    val type: ReplayEventType,
    val teamId: String?,
    val publicSummary: String,
    val hiddenPayloadHash: String? = null,
    val timestampMs: Long
)

data class ReplayTimeline(val replayId: String, val matchId: String, val events: List<ReplayEvent>) {
    init {
        require(events.map { it.sequence }.distinct().size == events.size) { "Replay events must have unique sequence numbers." }
    }
}

data class SpectatorView(
    val eventTitle: String,
    val currentRound: Int,
    val safeScores: Map<String, Double>,
    val timerSeconds: Int,
    val bracketSummary: String,
    val standings: List<String>,
    val hiddenAnswerVisible: Boolean,
    val teacherControlsVisible: Boolean,
    val futureMissionContentVisible: Boolean
)

data class ClassroomDisplayState(
    val title: String,
    val round: Int,
    val teamNames: List<String>,
    val safeScoreDisplay: List<String>,
    val timerSeconds: Int,
    val nextMatch: String,
    val cooperativeProgress: Double,
    val individualWeaknessesVisible: Boolean = false
)

data class RecoverySnapshot(
    val event: TournamentEvent,
    val completedResults: List<SignedMatchResult>,
    val replayEvents: List<ReplayEvent>,
    val currentMatchId: String?,
    val originalHostId: String
)

data class TournamentReport(
    val eventId: String,
    val eventSummary: String,
    val standings: List<String>,
    val matchResults: List<String>,
    val gamePerformance: Map<String, Double>,
    val topicPerformance: Map<String, Double>,
    val interruptions: List<String>,
    val teacherOverrides: List<String>,
    val recommendations: List<String>
) {
    fun toCsv(): String = buildString {
        appendLine("section,value")
        appendLine("summary,\"${eventSummary.replace("\"", "'")}\"")
        standings.forEach { appendLine("standing,\"${it.replace("\"", "'")}\"") }
        matchResults.forEach { appendLine("match,\"${it.replace("\"", "'")}\"") }
    }

    fun toJson(): String =
        """{"eventId":"$eventId","summary":"${eventSummary.replace("\"", "'")}","standings":[${standings.joinToString { "\"${it.replace("\"", "'")}\"" }}]}"""

    fun toPrintableHtml(): String =
        "<html><body><h1>$eventSummary</h1><ul>${standings.joinToString("") { "<li>$it</li>" }}</ul></body></html>"
}

class TournamentAdapterRegistry {
    val adapters: List<TournamentGameAdapter> = GamesCatalog.games.map { it.toAdapter() }
    fun requireAdapter(gameId: String): TournamentGameAdapter = adapters.firstOrNull { it.gameId == gameId } ?: error("Missing tournament adapter for $gameId")

    private fun GameDefinition.toAdapter(): TournamentGameAdapter = TournamentGameAdapter(
        gameId = id,
        title = title,
        minPlayers = if (playerModes.any { it.name == "Team" }) 2 else 1,
        maxPlayers = if (playerModes.any { it.name == "Team" }) 30 else 6,
        minTeams = if (playerModes.any { it.name == "Team" }) 2 else 1,
        maxTeams = if (id == "math_expedition_ar") 6 else 4,
        matchDurationMinutes = when (id) {
            "math_expedition_ar" -> 45
            "ar-math-arena" -> 18
            else -> 12
        },
        scoringInputs = setOf("accuracy", "completion", "difficulty", "time", "hints", "participation", "objective", "safety"),
        tieBreakRules = listOf(TieBreakRule.HigherAccuracy, TieBreakRule.HigherDifficulty, TieBreakRule.FewerHints, TieBreakRule.BetterParticipationBalance, TieBreakRule.FasterValidatedCompletion, TieBreakRule.TeacherApprovedTieBreakMission),
        replaySupported = true,
        safetyRestrictions = buildList {
            if (outdoorRequired) add("Teacher-approved outdoor route required.")
            if (arRequirement.name == "Required") add("AR readiness check required before match launch.")
            add("Accessible alternatives remain tournament-valid through normalized scoring.")
        },
        deviceRequirements = deviceRequirements
    )
}

class TournamentEngine(
    private val adapters: TournamentAdapterRegistry = TournamentAdapterRegistry()
) {
    fun createEvent(
        eventId: String,
        title: String,
        type: TournamentEventType,
        selectedGameIds: List<String>,
        teams: List<TournamentTeam>,
        players: List<TournamentPlayer>,
        weights: TournamentScoreWeights = TournamentScoreWeights(),
        spectatorLimit: Int = 12,
        replayRecordingEnabled: Boolean = true
    ): TournamentEvent {
        require(selectedGameIds.isNotEmpty()) { "At least one game is required." }
        selectedGameIds.forEach { adapters.requireAdapter(it) }
        val format = formatFor(type)
        val bracket = when (format) {
            TournamentFormat.Knockout -> knockout(eventId, selectedGameIds.first(), teams)
            TournamentFormat.RoundRobin -> roundRobin(eventId, selectedGameIds.first(), teams)
            TournamentFormat.Swiss -> swiss(eventId, selectedGameIds, teams, 3)
            TournamentFormat.Cooperative -> cooperative(eventId, selectedGameIds, teams)
        }
        return TournamentEvent(
            eventId = eventId,
            eventCode = safeCode(eventId),
            title = title,
            type = type,
            format = format,
            selectedGameIds = selectedGameIds,
            teams = teams,
            players = players,
            bracket = bracket,
            state = TournamentState.Registration,
            scoreWeights = weights,
            spectatorLimit = spectatorLimit.coerceIn(0, 50),
            replayRecordingEnabled = replayRecordingEnabled,
            createdAtEpochMs = System.currentTimeMillis()
        )
    }

    fun normalize(teamId: String, matchId: String, gameId: String, input: GameRoundResultInput, weights: TournamentScoreWeights): NormalizedScore {
        require(input.teacherConfirmed) { "Tournament results require host/teacher confirmation." }
        val total = (
            input.accuracy.clamp() * weights.accuracy +
                input.completion.clamp() * weights.completion +
                input.difficulty.clamp() * weights.difficulty +
                input.timeEfficiency.clamp() * weights.timeEfficiency +
                input.hintIndependence.clamp() * weights.hintIndependence +
                input.teamParticipation.clamp() * weights.teamParticipation +
                input.gameObjective.clamp() * weights.gameObjective +
                input.safetyCompliance.clamp() * weights.safetyCompliance
            ) * 100.0
        val formula = "100 * (${weights.accuracy}*accuracy + ${weights.completion}*completion + ${weights.difficulty}*difficulty + ${weights.timeEfficiency}*time + ${weights.hintIndependence}*hint + ${weights.teamParticipation}*participation + ${weights.gameObjective}*objective + ${weights.safetyCompliance}*safety)"
        return NormalizedScore(teamId, matchId, gameId, "%.2f".format(total).toDouble(), formula, input)
    }

    fun confirmResult(result: SignedMatchResult, hostPlayerId: String, seenResultIds: Set<String>): Result<SignedMatchResult> = runCatching {
        require(result.hostValidated) { "Result must be host validated." }
        require(result.hostPlayerId == hostPlayerId) { "Only the event host can confirm this result." }
        require(result.resultId !in seenResultIds) { "Duplicate result rejected." }
        result
    }

    fun compareTie(a: SignedMatchResult, b: SignedMatchResult, rules: List<TieBreakRule>): Int {
        rules.forEach { rule ->
            val diff = when (rule) {
                TieBreakRule.HigherAccuracy -> a.normalizedScore.input.accuracy.compareTo(b.normalizedScore.input.accuracy)
                TieBreakRule.HigherDifficulty -> a.normalizedScore.input.difficulty.compareTo(b.normalizedScore.input.difficulty)
                TieBreakRule.FewerHints -> b.normalizedScore.input.hintsUsed.compareTo(a.normalizedScore.input.hintsUsed)
                TieBreakRule.BetterParticipationBalance -> a.normalizedScore.input.teamParticipation.compareTo(b.normalizedScore.input.teamParticipation)
                TieBreakRule.FasterValidatedCompletion -> b.normalizedScore.input.validatedCompletionSeconds.compareTo(a.normalizedScore.input.validatedCompletionSeconds)
                TieBreakRule.TeacherApprovedTieBreakMission,
                TieBreakRule.ExplicitRandomDraw -> 0
            }
            if (diff != 0) return diff
        }
        return a.teamId.compareTo(b.teamId) * -1
    }

    fun spectatorView(event: TournamentEvent, results: List<SignedMatchResult>, timerSeconds: Int): SpectatorView =
        SpectatorView(
            eventTitle = event.title,
            currentRound = event.bracket.rounds.firstOrNull()?.round ?: 1,
            safeScores = results.groupBy { it.teamId }.mapValues { (_, values) -> values.sumOf { it.normalizedScore.total } },
            timerSeconds = timerSeconds,
            bracketSummary = "${event.format} | ${event.bracket.rounds.size} rounds",
            standings = standings(event, results),
            hiddenAnswerVisible = false,
            teacherControlsVisible = false,
            futureMissionContentVisible = false
        )

    fun classroomDisplay(event: TournamentEvent, results: List<SignedMatchResult>, timerSeconds: Int): ClassroomDisplayState =
        ClassroomDisplayState(
            title = event.title,
            round = event.bracket.rounds.firstOrNull { round -> round.matches.any { it.state != TournamentState.ResultConfirmed } }?.round ?: event.bracket.rounds.size,
            teamNames = event.teams.map { it.name },
            safeScoreDisplay = standings(event, results),
            timerSeconds = timerSeconds,
            nextMatch = event.bracket.rounds.flatMap { it.matches }.firstOrNull { it.state == TournamentState.Scheduled }?.matchId ?: "Between rounds",
            cooperativeProgress = if (event.format == TournamentFormat.Cooperative) results.sumOf { it.normalizedScore.input.completion } / event.teams.size.coerceAtLeast(1) else 0.0
        )

    fun replay(matchId: String, events: List<ReplayEvent>): ReplayTimeline =
        ReplayTimeline("replay-$matchId", matchId, events.sortedBy { it.sequence })

    fun reconstructScores(timeline: ReplayTimeline): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        timeline.events.filter { it.type == ReplayEventType.ScoreChange }.forEach { event ->
            val delta = event.publicSummary.substringAfter("+", "0").substringBefore(" ").toIntOrNull() ?: 0
            val team = event.teamId ?: return@forEach
            scores[team] = (scores[team] ?: 0) + delta
        }
        return scores
    }

    fun recovery(snapshot: RecoverySnapshot, requestingHostId: String): TournamentState =
        if (requestingHostId == snapshot.originalHostId) TournamentState.RecoveryRequired else TournamentState.Cancelled

    fun report(event: TournamentEvent, results: List<SignedMatchResult>, replayEvents: List<ReplayEvent>, overrides: List<String>): TournamentReport =
        TournamentReport(
            eventId = event.eventId,
            eventSummary = "${event.title}: ${event.type} with ${event.teams.size} teams",
            standings = standings(event, results),
            matchResults = results.map { "${it.matchId} ${it.teamId} ${it.normalizedScore.total}" },
            gamePerformance = results.groupBy { it.gameId }.mapValues { it.value.map { result -> result.normalizedScore.total }.average() },
            topicPerformance = emptyMap(),
            interruptions = replayEvents.filter { it.type == ReplayEventType.Reconnection || it.type == ReplayEventType.Pause }.map { it.publicSummary },
            teacherOverrides = overrides,
            recommendations = listOf("Use normalized component feedback for future practice.")
        )

    fun standings(event: TournamentEvent, results: List<SignedMatchResult>): List<String> =
        event.teams.map { team ->
            val score = results.filter { it.teamId == team.teamId }.sumOf { it.normalizedScore.total }
            team.name to score
        }.sortedByDescending { it.second }.mapIndexed { index, (name, score) -> "${index + 1}. $name ${"%.1f".format(score)}" }

    private fun formatFor(type: TournamentEventType): TournamentFormat = when (type) {
        TournamentEventType.SingleGameKnockout -> TournamentFormat.Knockout
        TournamentEventType.SingleGameRoundRobin,
        TournamentEventType.TeamLeague,
        TournamentEventType.HouseCompetition -> TournamentFormat.RoundRobin
        TournamentEventType.CooperativeClassQuest -> TournamentFormat.Cooperative
        TournamentEventType.MixedGameChampionship,
        TournamentEventType.SkillDecathlon,
        TournamentEventType.TeacherCreatedCustomEvent -> TournamentFormat.Swiss
    }

    private fun knockout(eventId: String, gameId: String, teams: List<TournamentTeam>): TournamentBracket {
        val seeded = teams.sortedBy { it.teamId }
        val firstRoundMatches = seeded.chunked(2).mapIndexed { index, pair ->
            TournamentMatch("$eventId-k1-${index + 1}", 1, gameId, pair.map { it.teamId }, index + 1, byeTeamId = pair.singleOrNull()?.teamId)
        }
        val finalRound = TournamentRound(2, "Final", listOf(TournamentMatch("$eventId-final", 2, gameId, emptyList(), 1)))
        return TournamentBracket(eventId, TournamentFormat.Knockout, listOf(TournamentRound(1, "Opening Round", firstRoundMatches), finalRound))
    }

    private fun roundRobin(eventId: String, gameId: String, teams: List<TournamentTeam>): TournamentBracket {
        val matches = mutableListOf<TournamentMatch>()
        teams.forEachIndexed { i, a ->
            teams.drop(i + 1).forEach { b ->
                matches += TournamentMatch("$eventId-rr-${matches.size + 1}", matches.size + 1, gameId, listOf(a.teamId, b.teamId), matches.size + 1)
            }
        }
        return TournamentBracket(eventId, TournamentFormat.RoundRobin, matches.map { TournamentRound(it.round, "Round ${it.round}", listOf(it)) })
    }

    private fun swiss(eventId: String, gameIds: List<String>, teams: List<TournamentTeam>, rounds: Int): TournamentBracket {
        val tournamentRounds = (1..rounds).map { round ->
            val rotated = teams.drop((round - 1) % teams.size) + teams.take((round - 1) % teams.size)
            TournamentRound(
                round,
                "Swiss Round $round",
                rotated.chunked(2).mapIndexed { index, pair ->
                    TournamentMatch("$eventId-s$round-${index + 1}", round, gameIds[(round - 1) % gameIds.size], pair.map { it.teamId }, index + 1, byeTeamId = pair.singleOrNull()?.teamId)
                }
            )
        }
        return TournamentBracket(eventId, TournamentFormat.Swiss, tournamentRounds)
    }

    private fun cooperative(eventId: String, gameIds: List<String>, teams: List<TournamentTeam>): TournamentBracket =
        TournamentBracket(
            eventId,
            TournamentFormat.Cooperative,
            gameIds.mapIndexed { index, gameId ->
                TournamentRound(index + 1, "Cooperative Stage ${index + 1}", listOf(TournamentMatch("$eventId-coop-${index + 1}", index + 1, gameId, teams.map { it.teamId }, 1)))
            }
        )

    private fun safeCode(eventId: String): String = eventId.uppercase().filter { it.isLetterOrDigit() }.padEnd(6, '2').take(6)
    private fun Double.clamp(): Double = coerceIn(0.0, 1.0)
}
