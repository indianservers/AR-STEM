package com.indianservers.ai_stem.feature.mathematics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun MathematicsHomeScreen(onBack: () -> Unit, onOpenAr: () -> Unit) {
    val modules = listOf("Graphs", "Two-Dimensional Shapes", "Three-Dimensional Solids", "Coordinate Geometry", "Transformations", "Vectors")
    Scaffold(topBar = { SimpleHeader("Mathematics", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.ViewInAr, contentDescription = null)
                    Text("AR Mathematics Playground", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Place and manipulate a mathematical object on a real surface.")
                    Button(onClick = onOpenAr, modifier = Modifier.fillMaxWidth()) {
                        Text("Open AR Playground")
                    }
                }
            }
            Text("Future mathematics modules", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modules.forEach {
                    FilterChip(selected = false, enabled = false, onClick = {}, label = { Text("$it - future module") })
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Phase 1 focuses on one real AR playground. Later categories are visible but intentionally disabled.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
