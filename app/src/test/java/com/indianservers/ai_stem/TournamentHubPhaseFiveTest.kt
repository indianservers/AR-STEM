package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.tournament.GameRoundResultInput
import com.indianservers.ai_stem.feature.games.tournament.RecoverySnapshot
import com.indianservers.ai_stem.feature.games.tournament.ReplayEvent
import com.indianservers.ai_stem.feature.games.tournament.ReplayEventType
import com.indianservers.ai_stem.feature.games.tournament.SignedMatchResult
import com.indianservers.ai_stem.feature.games.tournament.TieBreakRule
import com.indianservers.ai_stem.feature.games.tournament.TournamentAdapterRegistry
import com.indianservers.ai_stem.feature.games.tournament.TournamentEngine
import com.indianservers.ai_stem.feature.games.tournament.TournamentEventType
import com.indianservers.ai_stem.feature.games.tournament.TournamentFormat
import com.indianservers.ai_stem.feature.games.tournament.TournamentPlayer
import com.indianservers.ai_stem.feature.games.tournament.TournamentScoreWeights
import com.indianservers.ai_stem.feature.games.tournament.TournamentState
import com.indianservers.ai_stem.feature.games.tournament.TournamentTeam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentHubPhaseFiveTest {
    private val engine = TournamentEngine()
    private val teams = listOf(
        TournamentTeam("team-1", "Alpha", "Star", playerIds = listOf("p1", "p2")),
        TournamentTeam("team-2", "Beta", "Triangle", playerIds = listOf("p3", "p4")),
        TournamentTeam("team-3", "Gamma", "Circle", playerIds = listOf("p5", "p6")),
        TournamentTeam("team-4", "Delta", "Square", playerIds = listOf("p7", "p8"))
    )
    private val players = teams.flatMap { team -> team.playerIds.map { TournamentPlayer(it, "Guest $it", temporaryGuest = true, reconnectToken = "r-$it") } }

    @Test
    fun adaptersExistForAllSixGamesWithReplayAndSafety() {
        val adapters = TournamentAdapterRegistry().adapters

        assertEquals(GamesCatalog.games.map { it.id }.toSet(), adapters.map { it.gameId }.toSet())
        assertTrue(adapters.all { it.replaySupported })
        assertTrue(adapters.all { it.tieBreakRules.contains(TieBreakRule.HigherAccuracy) })
        assertTrue(adapters.first { it.gameId == "math_expedition_ar" }.safetyRestrictions.any { it.contains("outdoor", ignoreCase = true) })
    }

    @Test
    fun createsKnockoutRoundRobinSwissAndCooperativeEvents() {
        val knockout = engine.createEvent("event-knock", "Cup", TournamentEventType.SingleGameKnockout, listOf("ar-math-arena"), teams, players)
        val robin = engine.createEvent("event-robin", "League", TournamentEventType.SingleGameRoundRobin, listOf("fraction_factory_ar"), teams, players)
        val swiss = engine.createEvent("event-swiss", "Mixed", TournamentEventType.MixedGameChampionship, listOf("fraction_factory_ar", "coordinate_conquest_ar"), teams, players)
        val coop = engine.createEvent("event-coop", "Quest", TournamentEventType.CooperativeClassQuest, listOf("equation_escape_ar", "geometry_architect_ar"), teams, players)

        assertEquals(TournamentFormat.Knockout, knockout.format)
        assertTrue(knockout.bracket.rounds.first().matches.size >= 2)
        assertEquals(6, robin.bracket.rounds.size)
        assertEquals(TournamentFormat.Swiss, swiss.format)
        assertEquals(3, swiss.bracket.rounds.size)
        assertEquals(TournamentFormat.Cooperative, coop.format)
    }

    @Test
    fun normalizesScoresTransparentlyAndRequiresTeacherConfirmation() {
        val input = GameRoundResultInput(0.9, 0.8, 0.7, 0.6, 0.9, 0.8, 0.75, 1.0, hintsUsed = 1, validatedCompletionSeconds = 300, teacherConfirmed = true)
        val score = engine.normalize("team-1", "match-1", "fraction_factory_ar", input, TournamentScoreWeights())

        assertTrue(score.total in 0.0..100.0)
        assertTrue(score.formula.contains("accuracy"))
        assertTrue(runCatching {
            engine.normalize("team-1", "match-1", "fraction_factory_ar", input.copy(teacherConfirmed = false), TournamentScoreWeights())
        }.isFailure)
    }

    @Test
    fun tieBreaksAreDeterministicAndNotRandomByDefault() {
        val base = GameRoundResultInput(0.8, 0.8, 0.7, 0.7, 0.8, 0.8, 0.8, 1.0, hintsUsed = 2, validatedCompletionSeconds = 300, teacherConfirmed = true)
        val aScore = engine.normalize("team-1", "match", "game", base.copy(accuracy = 0.9), TournamentScoreWeights())
        val bScore = engine.normalize("team-2", "match", "game", base.copy(accuracy = 0.85), TournamentScoreWeights())
        val a = SignedMatchResult("ra", "match", "game", "team-1", aScore, "host", true, 1)
        val b = SignedMatchResult("rb", "match", "game", "team-2", bScore, "host", true, 2)

        assertTrue(engine.compareTie(a, b, listOf(TieBreakRule.HigherAccuracy, TieBreakRule.FewerHints)) > 0)
    }

    @Test
    fun hostAuthorityRejectsClientOrDuplicateResults() {
        val score = engine.normalize("team-1", "match", "game", GameRoundResultInput(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0, 100, true), TournamentScoreWeights())
        val result = SignedMatchResult("r1", "match", "game", "team-1", score, "host", true, 1)

        assertTrue(engine.confirmResult(result, "host", emptySet()).isSuccess)
        assertTrue(engine.confirmResult(result.copy(hostValidated = false), "host", emptySet()).isFailure)
        assertTrue(engine.confirmResult(result, "other-host", emptySet()).isFailure)
        assertTrue(engine.confirmResult(result, "host", setOf("r1")).isFailure)
    }

    @Test
    fun spectatorAndClassroomDisplayHideSensitiveInformation() {
        val event = engine.createEvent("event", "Display Event", TournamentEventType.MixedGameChampionship, listOf("fraction_factory_ar", "coordinate_conquest_ar"), teams, players)
        val spectator = engine.spectatorView(event, emptyList(), 300)
        val display = engine.classroomDisplay(event, emptyList(), 300)

        assertFalse(spectator.hiddenAnswerVisible)
        assertFalse(spectator.teacherControlsVisible)
        assertFalse(spectator.futureMissionContentVisible)
        assertFalse(display.individualWeaknessesVisible)
    }

    @Test
    fun replayReconstructsScoresWithoutRawCameraData() {
        val replay = engine.replay(
            "match-1",
            listOf(
                ReplayEvent("e1", "match-1", 1, ReplayEventType.MatchStart, null, "Start", timestampMs = 1),
                ReplayEvent("e2", "match-1", 2, ReplayEventType.ScoreChange, "team-1", "+20 validated answer", timestampMs = 2),
                ReplayEvent("e3", "match-1", 3, ReplayEventType.Pause, null, "Teacher pause", timestampMs = 3)
            )
        )

        assertEquals(mapOf("team-1" to 20), engine.reconstructScores(replay))
        assertTrue(replay.events.none { it.publicSummary.contains("camera", ignoreCase = true) })
    }

    @Test
    fun recoveryRequiresOriginalHostAndReportExports() {
        val event = engine.createEvent("event", "Report Event", TournamentEventType.TeamLeague, listOf("geometry_architect_ar"), teams, players)
        val snapshot = RecoverySnapshot(event, emptyList(), emptyList(), currentMatchId = null, originalHostId = "host")
        val report = engine.report(event, emptyList(), emptyList(), listOf("Reduced movement requirement"))

        assertEquals(TournamentState.RecoveryRequired, engine.recovery(snapshot, "host"))
        assertEquals(TournamentState.Cancelled, engine.recovery(snapshot, "new-host"))
        assertTrue(report.toCsv().contains("summary"))
        assertTrue(report.toJson().contains(event.eventId))
        assertTrue(report.toPrintableHtml().contains("<html>"))
    }
}
