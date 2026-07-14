package com.indianservers.ai_stem.feature.games.geometryarchitect

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
fun GeometryArchitectScreen(onBack: () -> Unit) {
    val engine = remember { GeometryArchitectEngine() }
    var briefIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var completedRaw by rememberSaveable { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf("") }
    var width by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("Lock a surface grid and build the first design brief.") }
    var paused by rememberSaveable { mutableStateOf(false) }

    val progress = ArchitectProgress(
        briefIndex = briefIndex,
        score = score,
        completedBriefIds = completedRaw.split("|").filter { it.isNotBlank() }.toSet(),
        attempts = attempts,
        arState = if (paused) ArchitectArState.Paused else ArchitectArState.GridLocked
    )
    val template = engine.currentBrief(progress)

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
                OutlinedButton(onClick = { paused = !paused }) { Text(if (paused) "Resume" else "Pause") }
            }
            Text("Geometry Architect AR", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Score $score") })
                AssistChip(onClick = {}, label = { Text("${briefIndex + 1}/${engine.briefs.size}") })
                AssistChip(onClick = {}, label = { Text(progress.arState.name) })
            }
            LinearProgressIndicator(
                progress = { (briefIndex + 1).toFloat() / engine.briefs.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Card(modifier = Modifier.testTag("geometry_architect_brief")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(template.brief.title, style = MaterialTheme.typography.titleLarge)
                    Text(template.prompt)
                    Text("Material budget: ${template.brief.materialBudget.maxUnits} ${template.brief.materialBudget.materialName}")
                    DimensionField("Length metres", length, !paused) { length = it }
                    DimensionField("Width metres", width, !paused) { width = it }
                    DimensionField("Height metres", height, !paused) { height = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !paused,
                            onClick = {
                                val result = engine.validate(
                                    template,
                                    length.toDoubleOrNull() ?: Double.NaN,
                                    width.toDoubleOrNull() ?: Double.NaN,
                                    height.toDoubleOrNull() ?: Double.NaN
                                )
                                val updated = engine.apply(progress, template, result)
                                briefIndex = updated.briefIndex
                                score = updated.score
                                attempts = updated.attempts
                                completedRaw = updated.completedBriefIds.joinToString("|")
                                feedback = result.messages.joinToString(" ")
                                if (result.valid) {
                                    length = ""
                                    width = ""
                                    height = ""
                                }
                            }
                        ) { Text("Verify Design") }
                        OutlinedButton(
                            enabled = !paused,
                            onClick = {
                                length = template.expectedLength.toString()
                                width = template.expectedWidth.toString()
                                height = template.expectedHeight.toString()
                                feedback = "Exact scale model dimensions loaded for inspection."
                            }
                        ) { Text("Load Blueprint") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(feedback, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DimensionField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}
