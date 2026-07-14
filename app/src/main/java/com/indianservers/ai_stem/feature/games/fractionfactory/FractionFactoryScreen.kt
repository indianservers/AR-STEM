package com.indianservers.ai_stem.feature.games.fractionfactory

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
fun FractionFactoryScreen(onBack: () -> Unit) {
    val engine = remember { FractionFactoryEngine() }
    var orderIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var completedRaw by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("Anchor the factory on a table and produce the first order.") }
    var paused by rememberSaveable { mutableStateOf(false) }

    val progress = FactoryProgress(
        orderIndex = orderIndex,
        score = score,
        completedOrders = completedRaw.split("|").filter { it.isNotBlank() }.toSet(),
        attempts = attempts,
        arState = if (paused) FactoryArState.Paused else FactoryArState.FactoryAnchored
    )
    val order = engine.currentOrder(progress)

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
            Text("Fraction Factory AR", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Score $score") })
                AssistChip(onClick = {}, label = { Text("${orderIndex + 1}/${engine.orders.size}") })
                AssistChip(onClick = {}, label = { Text(progress.arState.name) })
            }
            LinearProgressIndicator(
                progress = { (orderIndex + 1).toFloat() / engine.orders.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Card(modifier = Modifier.testTag("fraction_factory_order")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(order.title, style = MaterialTheme.typography.titleLarge)
                    Text(order.prompt)
                    Text("Machine: ${order.machine.name} | Skill: ${order.skill}", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        enabled = !paused,
                        label = { Text("Produced quantity") },
                        supportingText = { Text("Accepted: 3/4, 1 1/2, 0.75, 75%") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        enabled = !paused,
                        label = { Text("Unit") },
                        supportingText = { Text("Target unit: ${order.target.unit}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !paused,
                            onClick = {
                                val result = engine.validate(order, amount, unit)
                                val updated = engine.apply(progress, order, result)
                                orderIndex = updated.orderIndex
                                score = updated.score
                                attempts = updated.attempts
                                completedRaw = updated.completedOrders.joinToString("|")
                                feedback = result.feedback.joinToString(" ")
                                if (result.status == QualityInspectionStatus.Pass) {
                                    amount = ""
                                    unit = ""
                                }
                            }
                        ) { Text("Inspect Batch") }
                        OutlinedButton(
                            enabled = !paused,
                            onClick = {
                                amount = order.target.amount.toString()
                                unit = order.target.unit
                                feedback = "Calibration sample loaded. Inspect it to see the pass criteria."
                            }
                        ) { Text("Load Sample") }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(feedback, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
