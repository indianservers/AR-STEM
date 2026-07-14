package com.indianservers.ai_stem.feature.games.equationescape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun EquationEscapeScreen(onBack: () -> Unit) {
    val engine = remember { EquationEscapeEngine() }
    var levelIndex by rememberSaveable { mutableIntStateOf(0) }
    var solvedIdsRaw by rememberSaveable { mutableStateOf("") }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("Scan the room and open the first AR lock.") }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }

    val level = engine.levels[levelIndex]
    val progress = EscapeProgress(
        levelId = level.id,
        solvedPuzzleIds = solvedIdsRaw.split("|").filter { it.isNotBlank() }.toSet(),
        score = score,
        attempts = attempts,
        arState = if (paused) EscapeArState.Paused else EscapeArState.AnchorReady
    )
    val puzzle = engine.currentPuzzle(level, progress)
    val complete = engine.isLevelComplete(level, progress)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("Back") }
                OutlinedButton(onClick = { paused = !paused }) { Text(if (paused) "Resume" else "Pause") }
            }
            Text("Equation Escape AR", style = MaterialTheme.typography.headlineMedium)
            Text(level.story, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Score $score") })
                AssistChip(onClick = {}, label = { Text("Attempts $attempts") })
                AssistChip(onClick = {}, label = { Text(progress.arState.name) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.levels.forEachIndexed { index, candidate ->
                    FilterChip(
                        selected = index == levelIndex,
                        onClick = {
                            levelIndex = index
                            solvedIdsRaw = ""
                            score = 0
                            attempts = 0
                            answer = ""
                            feedback = "New room loaded. Start with the first AR lock."
                        },
                        label = { Text(candidate.title) }
                    )
                }
            }
            Divider()
            if (complete) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Room Escaped", style = MaterialTheme.typography.titleLarge)
                        Text("Final lock solved with $score points. Switch rooms for another escape path.")
                    }
                }
            } else if (puzzle != null) {
                Card(modifier = Modifier.testTag("equation_escape_active_lock")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(puzzle.title, style = MaterialTheme.typography.titleLarge)
                        Text(puzzle.prompt)
                        Text("AR anchor: ${puzzle.arPlacementLabel}", style = MaterialTheme.typography.labelLarge)
                        if (showHint) Text(puzzle.roleHint, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            enabled = !paused,
                            label = { Text("Answer") },
                            supportingText = { Text("Use 7, 3/4, (2, 5), or A,B,C depending on the lock.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !paused,
                                onClick = {
                                    val result = engine.validate(level, progress, puzzle.id, answer)
                                    val updated = engine.apply(progress, puzzle, result)
                                    solvedIdsRaw = updated.solvedPuzzleIds.joinToString("|")
                                    score = updated.score
                                    attempts = updated.attempts
                                    feedback = result.message
                                    if (result.correct) {
                                        answer = ""
                                        showHint = false
                                    }
                                }
                            ) { Text("Submit Lock") }
                            OutlinedButton(onClick = { showHint = !showHint }) { Text(if (showHint) "Hide Hint" else "Hint") }
                        }
                    }
                }
            }
            Text(feedback, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text("Solved locks: ${progress.solvedPuzzleIds.size}/${level.puzzles.size}", style = MaterialTheme.typography.labelLarge)
        }
    }
}
