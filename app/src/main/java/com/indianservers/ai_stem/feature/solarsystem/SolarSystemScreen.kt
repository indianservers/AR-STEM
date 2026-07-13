package com.indianservers.ai_stem.feature.solarsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun SolarSystemScreen(
    onBack: () -> Unit,
    onOpenAr: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(SolarSystemMode.MarkerlessOrrery) }
    Scaffold(topBar = { SimpleHeader("Solar System", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Public, contentDescription = null)
                    Text("Markerless Solar System", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Place an orbit model on a desk or floor, then compare planet scale, orbital paths, rotation and distance.")
                    SolarSystemPreview()
                    Button(onClick = onOpenAr, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.ViewInAr, contentDescription = null)
                        Text("Open in Markerless AR")
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Explore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SolarSystemMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    Text(selectedMode.description, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("Scale") })
                        AssistChip(onClick = {}, label = { Text("Orbit") })
                        AssistChip(onClick = {}, label = { Text("Rotation") })
                        AssistChip(onClick = {}, label = { Text("Distance") })
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coming AR Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Pinch to scale the orbit model, tap a planet for facts, scrub time to animate orbits, and switch between true scale and classroom scale.")
                    AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) }, label = { Text("Orbit timeline") })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SolarSystemPreview() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(160.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitColor = Color(0xFF6EDBFF).copy(alpha = 0.48f)
            listOf(26f, 42f, 58f, 74f).forEach { radius ->
                drawCircle(orbitColor, radius = radius, center = center, style = Stroke(width = 2f))
            }
            drawCircle(Color(0xFFFFC857), radius = 13f, center = center)
            drawCircle(Color(0xFF62D6C7), radius = 5f, center = Offset(center.x + 42f, center.y))
            drawCircle(Color(0xFF42A5F5), radius = 6f, center = Offset(center.x - 58f, center.y - 8f))
            drawCircle(Color(0xFFFF6B8A), radius = 4f, center = Offset(center.x + 74f, center.y + 12f))
        }
        Text(
            "AR-ready orbit model",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private enum class SolarSystemMode(val label: String, val description: String) {
    MarkerlessOrrery("Markerless", "Place the Solar System directly on a detected surface without any image marker."),
    ScaleCompare("Scale", "Compare classroom scale, real distance scale and planet size scale."),
    OrbitTimeline("Timeline", "Scrub time forward and backward to see orbital motion and rotation.")
}
