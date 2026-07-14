package com.indianservers.ai_stem.feature.games.coordinateconquest

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
import androidx.compose.material3.LinearProgressIndicator
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
fun CoordinateConquestScreen(onBack: () -> Unit) {
    val engine = remember { CoordinateConquestEngine() }
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var chapterIndex by rememberSaveable { mutableIntStateOf(0) }
    var missionIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var completedRaw by rememberSaveable { mutableStateOf("") }
    var xInput by rememberSaveable { mutableStateOf("") }
    var yInput by rememberSaveable { mutableStateOf("") }
    var slopeInput by rememberSaveable { mutableStateOf("") }
    var interceptInput by rememberSaveable { mutableStateOf("") }
    var tableTop by rememberSaveable { mutableStateOf(false) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("Scan the shared marker, confirm the safe boundary, then start the grid mission.") }
    var boundaryState by rememberSaveable { mutableStateOf(ConquestTrackingState.Ready.name) }

    val mode = engine.modes[modeIndex]
    val chapter = engine.soloChapters[chapterIndex]
    val missions = if (mode == ConquestMode.SoloMissionCampaign || mode == ConquestMode.SoloCoordinateTraining) chapter.missions else engine.missionCatalog
    val mission = missions[missionIndex.coerceIn(0, missions.lastIndex)]
    val completed = completedRaw.split("|").filter { it.isNotBlank() }.toSet()
    val interaction = if (tableTop || mode == ConquestMode.SeatedTabletopMode) InteractionMode.TabletopPlacement else InteractionMode.PhysicalMovement
    val grid = engine.defaultGrid()
    val warning = engine.boundaryWarning(grid, engine.gridToShared(grid, CoordinatePoint(xInput.toDoubleOrNull() ?: 0.0, yInput.toDoubleOrNull() ?: 0.0)), 0.4, markerVisible = true)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
                OutlinedButton(onClick = { paused = !paused }) { Text(if (paused) "Resume" else "Host Pause") }
            }
            Text("Coordinate Conquest AR", style = MaterialTheme.typography.headlineMedium)
            Text("Plot. Navigate. Capture.", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Score $score") })
                AssistChip(onClick = {}, label = { Text("Attempts $attempts") })
                AssistChip(onClick = {}, label = { Text(if (interaction == InteractionMode.TabletopPlacement) "Tabletop" else "Physical") })
                AssistChip(onClick = {}, label = { Text(boundaryState) })
            }
            LinearProgressIndicator(
                progress = { (missionIndex + 1).toFloat() / missions.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Modes", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.modes.take(4).forEachIndexed { index, candidate ->
                    FilterChip(selected = modeIndex == index, onClick = { modeIndex = index; missionIndex = 0 }, label = { Text(candidate.shortName()) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.modes.drop(4).forEachIndexed { index, candidate ->
                    val actualIndex = index + 4
                    FilterChip(selected = modeIndex == actualIndex, onClick = { modeIndex = actualIndex; missionIndex = 0 }, label = { Text(candidate.shortName()) })
                }
            }
            Text("Solo chapters", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.soloChapters.forEachIndexed { index, candidate ->
                    FilterChip(
                        selected = chapterIndex == index,
                        onClick = {
                            chapterIndex = index
                            missionIndex = 0
                        },
                        label = { Text(candidate.title.substringBefore(" ")) }
                    )
                }
            }
            Card(modifier = Modifier.testTag("coordinate_conquest_mission")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(mission.type.name.spaced(), style = MaterialTheme.typography.titleLarge)
                    Text(mission.prompt)
                    Text("Chapter: ${chapter.title} | Skill: ${mission.skill.name} | Difficulty: ${mission.difficulty.name}")
                    Text("Shared grid: ${grid.widthUnits} x ${grid.heightUnits}, ${grid.unitSizeMetres} m per unit, visible ${grid.visibleRange}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !tableTop, onClick = { tableTop = false }, label = { Text("Physical") })
                        FilterChip(selected = tableTop, onClick = { tableTop = true }, label = { Text("Tabletop") })
                    }
                    OutlinedTextField(
                        value = xInput,
                        onValueChange = { xInput = it },
                        enabled = !paused,
                        label = { Text("X / value / slope") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = yInput,
                        onValueChange = { yInput = it },
                        enabled = !paused,
                        label = { Text("Y / intercept") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mission.targetLine != null) {
                        OutlinedTextField(
                            value = slopeInput,
                            onValueChange = { slopeInput = it },
                            enabled = !paused,
                            label = { Text("Line slope, blank for vertical") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = interceptInput,
                            onValueChange = { interceptInput = it },
                            enabled = !paused,
                            label = { Text("Line intercept or x constant") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !paused,
                            onClick = {
                                val submittedPoint = CoordinatePoint(xInput.toDoubleOrNull() ?: Double.NaN, yInput.toDoubleOrNull() ?: Double.NaN)
                                val submittedLine = if (mission.targetLine != null) {
                                    val slope = slopeInput.toDoubleOrNull()
                                    val second = interceptInput.toDoubleOrNull()
                                    if (slope == null) LineEquation(null, null, second) else LineEquation(slope, second)
                                } else {
                                    null
                                }
                                val submittedShape = if (mission.targetShape.isNotEmpty()) mission.targetShape else emptyList()
                                val result = engine.validateMission(
                                    mission = mission,
                                    submittedPoint = submittedPoint.takeIf { it.x.isFinite() && it.y.isFinite() },
                                    submittedLine = submittedLine,
                                    submittedShape = submittedShape,
                                    stable = true,
                                    interactionMode = interaction
                                )
                                attempts += 1
                                if (result.correct) {
                                    score += result.score
                                    completedRaw = (completed + mission.missionId).joinToString("|")
                                    missionIndex = (missionIndex + 1).coerceAtMost(missions.lastIndex)
                                    xInput = ""
                                    yInput = ""
                                    slopeInput = ""
                                    interceptInput = ""
                                }
                                boundaryState = warning.state.name
                                feedback = result.message + (result.mathematicalDifference?.let { " $it" } ?: "")
                            }
                        ) { Text("Validate Capture") }
                        OutlinedButton(
                            enabled = !paused,
                            onClick = {
                                xInput = mission.targetPoint?.x?.toString() ?: ""
                                yInput = mission.targetPoint?.y?.toString() ?: ""
                                slopeInput = mission.targetLine?.slope?.toString().orEmpty()
                                interceptInput = (mission.targetLine?.intercept ?: mission.targetLine?.xConstant)?.toString().orEmpty()
                                feedback = mission.workedSolution.joinToString(" ")
                            }
                        ) { Text("Worked Solution") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(feedback, style = MaterialTheme.typography.bodyLarge)
                    Text("Safety: ${warning.message}")
                    Text("Roles rotate: ${engine.roles.joinToString { it.name }}")
                    Text("Completed: ${completed.size}/${missions.size}")
                }
            }
        }
    }
}

private fun ConquestMode.shortName(): String = when (this) {
    ConquestMode.SoloCoordinateTraining -> "Training"
    ConquestMode.SoloMissionCampaign -> "Campaign"
    ConquestMode.TeamTerritoryCapture -> "Territory"
    ConquestMode.CooperativeGridDefence -> "Defence"
    ConquestMode.VectorRelay -> "Relay"
    ConquestMode.TransformationBattle -> "Battle"
    ConquestMode.TeacherChallengeMode -> "Teacher"
    ConquestMode.SeatedTabletopMode -> "Seated"
}

private fun String.spaced(): String = replace(Regex("([a-z])([A-Z])"), "$1 $2")
