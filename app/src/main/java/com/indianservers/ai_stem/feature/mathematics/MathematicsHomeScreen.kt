package com.indianservers.ai_stem.feature.mathematics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

private enum class MathMenuSection(val label: String) {
    ArModes("AR Modes"),
    Graphs("Graphs"),
    Measurements("Measure"),
    Scenes("Scenes")
}

private data class MathFeatureCard(
    val title: String,
    val subtitle: String,
    val tags: List<String>,
    val icon: ImageVector,
    val color: Color,
    val actionLabel: String,
    val action: MathFeatureAction
)

private enum class MathFeatureAction { OpenAr, OpenGraphingStudio, OpenProjects }

@Composable
fun MathematicsHomeScreen(
    onBack: () -> Unit,
    onOpenAr: () -> Unit,
    onOpenGraphingStudio: () -> Unit,
    onOpenProjects: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(MathMenuSection.ArModes) }
    val features = selectedSection.features()

    Scaffold(topBar = { SimpleHeader("Mathematics", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ViewInAr, contentDescription = null)
                        Column {
                            Text("Maths AR Hub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Pick a direct AR workflow: markerless, paper graph, outdoor geometry, graph transforms, measurement, or saved scenes.")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onOpenAr, modifier = Modifier.weight(1f)) {
                            Text("Open AR")
                        }
                        OutlinedButton(onClick = onOpenGraphingStudio, modifier = Modifier.weight(1f)) {
                            Text("Graph Studio")
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MathMenuSection.entries) { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section.label) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEach { feature ->
                    MathFeatureCard(
                        feature = feature,
                        onOpenAr = onOpenAr,
                        onOpenGraphingStudio = onOpenGraphingStudio,
                        onOpenProjects = onOpenProjects
                    )
                }
            }
        }
    }
}

@Composable
private fun MathFeatureCard(
    feature: MathFeatureCard,
    onOpenAr: () -> Unit,
    onOpenGraphingStudio: () -> Unit,
    onOpenProjects: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (feature.action) {
                    MathFeatureAction.OpenAr -> onOpenAr()
                    MathFeatureAction.OpenGraphingStudio -> onOpenGraphingStudio()
                    MathFeatureAction.OpenProjects -> onOpenProjects()
                }
            }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = feature.color.copy(alpha = 0.18f), shape = CircleShape) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(feature.icon, contentDescription = null, tint = feature.color)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(feature.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(feature.tags) { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(tag, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Button(
                onClick = {
                    when (feature.action) {
                        MathFeatureAction.OpenAr -> onOpenAr()
                        MathFeatureAction.OpenGraphingStudio -> onOpenGraphingStudio()
                        MathFeatureAction.OpenProjects -> onOpenProjects()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feature.actionLabel)
            }
        }
    }
}

private fun MathMenuSection.features(): List<MathFeatureCard> = when (this) {
    MathMenuSection.ArModes -> listOf(
        MathFeatureCard(
            title = "Markerless Maths AR",
            subtitle = "Place graphs, solids, vectors, labels and formulas directly on real surfaces.",
            tags = listOf("Indoor AR", "Surface", "Air", "Visual tray"),
            icon = Icons.Outlined.ViewInAr,
            color = Color(0xFF42A5F5),
            actionLabel = "Open Markerless AR",
            action = MathFeatureAction.OpenAr
        ),
        MathFeatureCard(
            title = "Paper Graph AR",
            subtitle = "Scan the worksheet target, calibrate origin/X/Y, then lock axes and 3D graph layers to paper.",
            tags = listOf("Image target", "Origin", "X/Y calibration", "Cross sections"),
            icon = Icons.Outlined.AutoGraph,
            color = Color(0xFFFFC857),
            actionLabel = "Open Paper Graph Mode",
            action = MathFeatureAction.OpenAr
        ),
        MathFeatureCard(
            title = "Outdoor Geospatial Math",
            subtitle = "Use building and terrain meshes for height, slope, angle, volume and coordinate lessons.",
            tags = listOf("Outdoor Geo", "Buildings", "Terrain mesh", "Occlusion ready"),
            icon = Icons.Outlined.Layers,
            color = Color(0xFF3DFF9F),
            actionLabel = "Open Outdoor Geo AR",
            action = MathFeatureAction.OpenAr
        )
    )
    MathMenuSection.Graphs -> listOf(
        MathFeatureCard(
            title = "Graph Drawing Studio",
            subtitle = "Draw and edit expressions before using them as AR graphs or 3D transforms.",
            tags = listOf("Expressions", "Functions", "Export", "AR render"),
            icon = Icons.Outlined.Functions,
            color = Color(0xFF9B8CFF),
            actionLabel = "Open Graph Studio",
            action = MathFeatureAction.OpenGraphingStudio
        ),
        MathFeatureCard(
            title = "2D Drawing to 3D",
            subtitle = "Turn a drawn function into surface, extrusion, revolution, tangent plane or slices.",
            tags = listOf("Surface", "Extrusion", "Revolution", "Tangent"),
            icon = Icons.Outlined.AutoGraph,
            color = Color(0xFFFF6B8A),
            actionLabel = "Open 2D to 3D AR",
            action = MathFeatureAction.OpenAr
        ),
        MathFeatureCard(
            title = "Color Map Graphs",
            subtitle = "Use height, slope, curvature, X value and Y value gradients in AR graphs.",
            tags = listOf("Height", "Slope", "Curvature", "Gradient"),
            icon = Icons.Outlined.Functions,
            color = Color(0xFF06D6A0),
            actionLabel = "Open Graph AR",
            action = MathFeatureAction.OpenAr
        )
    )
    MathMenuSection.Measurements -> listOf(
        MathFeatureCard(
            title = "AR Measurement Overlay",
            subtitle = "Attach height, distance, angle, slope, area and volume values to real AR objects.",
            tags = listOf("Distance", "Angle", "Slope", "Volume"),
            icon = Icons.Outlined.Straighten,
            color = Color(0xFFFFA726),
            actionLabel = "Open Measure AR",
            action = MathFeatureAction.OpenAr
        ),
        MathFeatureCard(
            title = "Formula Overlay Cards",
            subtitle = "Show floating equation cards and collapsible chips beside graphs and 3D objects.",
            tags = listOf("V = l x w x h", "z = f(x,y)", "slope", "chips"),
            icon = Icons.Outlined.Functions,
            color = Color(0xFFEC407A),
            actionLabel = "Open Formula AR",
            action = MathFeatureAction.OpenAr
        )
    )
    MathMenuSection.Scenes -> listOf(
        MathFeatureCard(
            title = "Saved Maths AR Scenes",
            subtitle = "Continue saved AR scenes, inspect layers, reuse measurements and manage projects.",
            tags = listOf("Save", "Layers", "Scenes", "Projects"),
            icon = Icons.Outlined.Save,
            color = Color(0xFF66BB6A),
            actionLabel = "Open Saved Scenes",
            action = MathFeatureAction.OpenProjects
        ),
        MathFeatureCard(
            title = "AR Scene Tools",
            subtitle = "Use layers, visual object tray, diagnostics, capture, labels, formulas and animation tools.",
            tags = listOf("Layers", "Capture", "Diagnostics", "Animation"),
            icon = Icons.Outlined.Layers,
            color = Color(0xFF7E57C2),
            actionLabel = "Open Scene Tools",
            action = MathFeatureAction.OpenAr
        )
    )
}
