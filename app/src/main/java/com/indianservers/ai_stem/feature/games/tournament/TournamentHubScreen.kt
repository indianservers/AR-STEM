package com.indianservers.ai_stem.feature.games.tournament

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog

@Composable
fun TournamentHubScreen(onBack: () -> Unit) {
    val engine = remember { TournamentEngine() }
    var eventTypeIndex by rememberSaveable { mutableIntStateOf(2) }
    var selectedGamesRaw by rememberSaveable { mutableStateOf("fraction_factory_ar,coordinate_conquest_ar,equation_escape_ar,geometry_architect_ar,ar-math-arena") }
    var feedback by rememberSaveable { mutableStateOf("Create an offline LAN event. The hub does not start ARCore.") }
    val teams = remember {
        listOf(
            TournamentTeam("team-1", "Alpha", "Star", playerIds = listOf("p1", "p2")),
            TournamentTeam("team-2", "Beta", "Triangle", playerIds = listOf("p3", "p4")),
            TournamentTeam("team-3", "Gamma", "Circle", playerIds = listOf("p5", "p6")),
            TournamentTeam("team-4", "Delta", "Square", playerIds = listOf("p7", "p8"))
        )
    }
    val players = remember { teams.flatMap { team -> team.playerIds.map { TournamentPlayer(it, "Guest ${it.takeLast(1)}", temporaryGuest = true) } } }
    val selectedGames = selectedGamesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val event = remember(eventTypeIndex, selectedGamesRaw) {
        engine.createEvent(
            eventId = "class-event",
            title = "Maths AR Championship",
            type = TournamentEventType.entries[eventTypeIndex],
            selectedGameIds = selectedGames.ifEmpty { listOf("ar-math-arena") },
            teams = teams,
            players = players
        )
    }
    val sampleResults = remember(event) {
        event.teams.take(2).mapIndexed { index, team ->
            val input = GameRoundResultInput(0.82 - index * 0.04, 0.9, 0.75, 0.8, 0.7, 0.85, 0.8, 1.0, hintsUsed = index, validatedCompletionSeconds = 480 + index * 30, teacherConfirmed = true)
            val score = engine.normalize(team.teamId, event.bracket.rounds.first().matches.first().matchId, event.selectedGameIds.first(), input, event.scoreWeights)
            SignedMatchResult("result-${team.teamId}", score.matchId, score.gameId, team.teamId, score, "host", hostValidated = true, sequence = index.toLong())
        }
    }
    val spectator = engine.spectatorView(event, sampleResults, timerSeconds = 540)
    val display = engine.classroomDisplay(event, sampleResults, timerSeconds = 540)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("tournament-hub-screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Maths AR Tournament Hub", style = MaterialTheme.typography.headlineMedium)
            Text("Create events, brackets, schedules, spectators, replays and reports across the six existing games.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TournamentEventType.entries.take(4).forEachIndexed { index, type ->
                    FilterChip(selected = eventTypeIndex == index, onClick = { eventTypeIndex = index }, label = { Text(type.short()) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TournamentEventType.entries.drop(4).forEachIndexed { index, type ->
                    val actual = index + 4
                    FilterChip(selected = eventTypeIndex == actual, onClick = { eventTypeIndex = actual }, label = { Text(type.short()) })
                }
            }
            Card(modifier = Modifier.testTag("tournament-event-card")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(event.title, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(event.eventCode) })
                        AssistChip(onClick = {}, label = { Text(event.format.name) })
                        AssistChip(onClick = {}, label = { Text("${event.teams.size} teams") })
                    }
                    Text("Games: ${event.selectedGameIds.joinToString { GamesCatalog.requireGame(it).title }}")
                    Text("Formula: ${sampleResults.first().normalizedScore.formula}")
                    Text("Bracket rounds: ${event.bracket.rounds.size}")
                }
            }
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Schedule and Teams", style = MaterialTheme.typography.titleMedium)
                    event.bracket.rounds.take(3).forEach { round ->
                        Text("${round.title}: ${round.matches.joinToString { it.matchId + " " + it.gameId }}")
                    }
                    Text(event.teams.joinToString { "${it.name} (${it.icon})" })
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Spectator / Classroom Display", style = MaterialTheme.typography.titleMedium)
                    Text("Spectator scores: ${spectator.safeScores}")
                    Text("Hidden answers visible: ${spectator.hiddenAnswerVisible}")
                    Text("Classroom display: ${display.safeScoreDisplay.joinToString()}")
                    Text("Individual weaknesses visible: ${display.individualWeaknessesVisible}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val replay = engine.replay(
                        "match-1",
                        listOf(
                            ReplayEvent("e1", "match-1", 1, ReplayEventType.MatchStart, null, "Match started", timestampMs = 1),
                            ReplayEvent("e2", "match-1", 2, ReplayEventType.ScoreChange, "team-1", "+25 score for validated answer", timestampMs = 2)
                        )
                    )
                    feedback = "Replay ${replay.replayId} reconstructs ${engine.reconstructScores(replay)}"
                }) { Text("Build Replay") }
                OutlinedButton(onClick = {
                    val report = engine.report(event, sampleResults, emptyList(), emptyList())
                    feedback = "Report exports CSV ${report.toCsv().lines().size} lines and JSON ${report.toJson().length} chars."
                }) { Text("Export Report") }
            }
            Text(feedback)
        }
    }
}

private fun TournamentEventType.short(): String = when (this) {
    TournamentEventType.SingleGameKnockout -> "Knockout"
    TournamentEventType.SingleGameRoundRobin -> "Robin"
    TournamentEventType.MixedGameChampionship -> "Mixed"
    TournamentEventType.TeamLeague -> "League"
    TournamentEventType.CooperativeClassQuest -> "Quest"
    TournamentEventType.SkillDecathlon -> "Decathlon"
    TournamentEventType.HouseCompetition -> "House"
    TournamentEventType.TeacherCreatedCustomEvent -> "Custom"
}
