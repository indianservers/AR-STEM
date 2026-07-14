package com.indianservers.ai_stem.feature.games.mathexpedition

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
fun MathExpeditionScreen(onBack: () -> Unit) {
    val engine = remember { MathExpeditionEngine() }
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var expeditionIndex by rememberSaveable { mutableIntStateOf(0) }
    var checkpointIndex by rememberSaveable { mutableIntStateOf(0) }
    var permission by rememberSaveable { mutableStateOf(LocationPermissionState.NotRequested.name) }
    var answer by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var accuracy by rememberSaveable { mutableStateOf("15") }
    var dwell by rememberSaveable { mutableStateOf("3") }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var completed by rememberSaveable { mutableIntStateOf(0) }
    var feedback by rememberSaveable { mutableStateOf("Open a teacher-approved route. Location is requested only when real expedition play begins.") }
    var emergencyStopped by rememberSaveable { mutableStateOf(false) }

    val mode = engine.modes[modeIndex]
    val expedition = engine.sampleTemplates[expeditionIndex]
    val route = expedition.route
    val checkpoint = route.checkpoints[checkpointIndex.coerceIn(0, route.checkpoints.lastIndex)]
    val validation = engine.validateRoute(route)
    val progress = completed.toFloat() / route.checkpoints.size.toFloat()

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
                OutlinedButton(onClick = {
                    emergencyStopped = true
                    feedback = "Emergency stop active. Pause teams and return to start."
                }) { Text("Emergency Stop") }
            }
            Text("Math Expedition AR", style = MaterialTheme.typography.headlineMedium)
            Text("Explore the World Through Mathematics.", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Score $score") })
                AssistChip(onClick = {}, label = { Text("${completed}/${route.checkpoints.size}") })
                AssistChip(onClick = {}, label = { Text(LocationPermissionState.valueOf(permission).name) })
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("Game modes", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.modes.take(4).forEachIndexed { index, candidate ->
                    FilterChip(selected = modeIndex == index, onClick = { modeIndex = index }, label = { Text(candidate.shortName()) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.modes.drop(4).forEachIndexed { index, candidate ->
                    val actual = index + 4
                    FilterChip(selected = modeIndex == actual, onClick = { modeIndex = actual }, label = { Text(candidate.shortName()) })
                }
            }
            Text("Expedition templates", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                engine.sampleTemplates.forEachIndexed { index, candidate ->
                    FilterChip(
                        selected = expeditionIndex == index,
                        onClick = {
                            expeditionIndex = index
                            checkpointIndex = 0
                            completed = 0
                            score = 0
                            feedback = "Template loaded. Teacher must place/approve checkpoints before real-world play."
                        },
                        label = { Text(candidate.title.substringBefore(" ")) }
                    )
                }
            }
            Card(modifier = Modifier.testTag("math_expedition_route")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(route.title, style = MaterialTheme.typography.titleLarge)
                    Text("Mode: ${mode.name.spaced()} | Provider: ${expedition.mapProvider.displayName}")
                    Text(expedition.mapProvider.attributionText)
                    Text("Route length: ${engine.routeLength(route).round()} m | Direct: ${engine.directDistance(route).round()} m | Est. ${route.estimatedDurationMinutes} min")
                    Text("Validation: ${if (validation.valid) "route passes local checks" else validation.issues.joinToString { it.message }}")
                    Text("Offline: ${expedition.offlineRegion?.let { if (it.downloaded) "map downloaded" else "route saved; provider offline download not active" } ?: "provider-specific"}")
                }
            }
            Card(modifier = Modifier.testTag("math_expedition_checkpoint")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(checkpoint.title, style = MaterialTheme.typography.titleLarge)
                    Text(checkpoint.mathMission.prompt)
                    Text("Checkpoint radius ${checkpoint.verificationRule.radiusMetres} m, dwell ${checkpoint.verificationRule.requiredDwellSeconds}s")
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Mission numeric answer") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val ok = answer.toDoubleOrNull()?.let { engine.validateMathMission(checkpoint.mathMission, it) } == true
                            if (ok) {
                                score += checkpoint.mathMission.points
                                feedback = "Mission answer accepted. Verify checkpoint with location or teacher indoor mode."
                            } else {
                                feedback = "Check units and route geometry before submitting again."
                            }
                        }) { Text("Submit Mission") }
                        OutlinedButton(onClick = {
                            answer = checkpoint.mathMission.expectedNumericAnswer?.round()?.toString().orEmpty()
                            feedback = checkpoint.mathMission.workedSolution.joinToString(" ")
                        }) { Text("Worked Solution") }
                    }
                    Text("Location verification")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            permission = LocationPermissionState.ExplanationRequired.name
                            feedback = engine.permissionMessage(LocationPermissionState.ExplanationRequired)
                        }) { Text("Explain Permission") }
                        Button(onClick = {
                            permission = LocationPermissionState.GrantedPrecise.name
                            feedback = engine.permissionMessage(LocationPermissionState.GrantedPrecise)
                        }) { Text("Grant Foreground") }
                    }
                    OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Current latitude") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Current longitude") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = accuracy, onValueChange = { accuracy = it }, label = { Text("Accuracy metres") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dwell, onValueChange = { dwell = it }, label = { Text("Dwell seconds") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !emergencyStopped,
                            onClick = {
                                val sample = LocationSample(
                                    coordinate = latitude.toDoubleOrNull()?.let { lat -> longitude.toDoubleOrNull()?.let { lon -> MapCoordinate(lat, lon) } },
                                    accuracyMetres = accuracy.toDoubleOrNull(),
                                    speedMetresPerSecond = 0.6,
                                    elapsedDwellSeconds = dwell.toIntOrNull() ?: 0,
                                    providerAvailable = permission == LocationPermissionState.GrantedPrecise.name || permission == LocationPermissionState.GrantedApproximate.name,
                                    approximateOnly = permission == LocationPermissionState.GrantedApproximate.name,
                                    mockFlaggedByAndroid = false
                                )
                                val result = engine.verifyCheckpoint(route, checkpoint, sample, hostAuthoritative = mode != ExpeditionMode.SoloExplorer)
                                feedback = result.message
                                if (result.canComplete) {
                                    completed = (completed + 1).coerceAtMost(route.checkpoints.size)
                                    checkpointIndex = (checkpointIndex + 1).coerceAtMost(route.checkpoints.lastIndex)
                                    score += 50
                                }
                            }
                        ) { Text("Verify Checkpoint") }
                        OutlinedButton(onClick = {
                            latitude = checkpoint.coordinate.latitude.toString()
                            longitude = checkpoint.coordinate.longitude.toString()
                            accuracy = "10"
                            dwell = checkpoint.verificationRule.requiredDwellSeconds.toString()
                            feedback = "Checkpoint coordinates loaded for teacher authoring or QA, not automatic real play."
                        }) { Text("Authoring QA Fill") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(feedback, style = MaterialTheme.typography.bodyLarge)
                    Text("Safety: walking only, stop before interacting, adult supervision required, weather and road risk must be checked by the teacher.")
                    Text("Privacy: route summaries and checkpoint completion are stored; detailed location history is not required.")
                }
            }
        }
    }
}

private fun ExpeditionMode.shortName(): String = when (this) {
    ExpeditionMode.SoloExplorer -> "Solo"
    ExpeditionMode.TeamExpedition -> "Team"
    ExpeditionMode.CampusSurvey -> "Survey"
    ExpeditionMode.MathematicsTreasureRoute -> "Treasure"
    ExpeditionMode.RouteOptimisationChallenge -> "Route"
    ExpeditionMode.StatisticsFieldMission -> "Stats"
    ExpeditionMode.TeacherLedClassExpedition -> "Teacher"
    ExpeditionMode.IndoorCampusMapMode -> "Indoor"
}

private fun String.spaced(): String = replace(Regex("([a-z])([A-Z])"), "$1 $2")
private fun Double.round(): Double = kotlin.math.round(this * 10.0) / 10.0
