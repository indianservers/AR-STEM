package com.indianservers.ai_stem.feature.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamesLibraryScreen(
    onBack: () -> Unit,
    onOpenGame: (GameDefinition) -> Unit
) {
    Scaffold(
        topBar = { GamesHeader(title = "Games Library", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("games-library-screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Games", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Catalog-driven STEM games ready for more entries without changing the app shell.", style = MaterialTheme.typography.bodyMedium)
            }
            items(GamesCatalog.games) { game ->
                GameLibraryCard(
                    game = game,
                    onOpen = { onOpenGame(game) }
                )
            }
        }
    }
}

@Composable
private fun GamesHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GameLibraryCard(game: GameDefinition, onOpen: () -> Unit) {
    val playable = game.availability == GameAvailability.Available
    Card(
        onClick = { if (playable) onOpen() },
        enabled = playable,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game-card-${game.id}")
            .semantics { contentDescription = "${game.title} game card" },
        colors = CardDefaults.cardColors(
            containerColor = if (playable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ArenaIllustration(Modifier.size(86.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(game.subtitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(game.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.capabilities.forEach { capability ->
                    CapabilityPill(capability.label())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Text("Recommended players: ${game.recommendedPlayers}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (playable) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier.testTag("open-game-${game.id}")
                    ) { Text("Open Game") }
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Future", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityPill(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun GamesIndexIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.primaryContainer)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ArenaIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier) {
        drawCircle(primary.copy(alpha = 0.18f), radius = size.minDimension * 0.48f, center = center)
        drawOval(
            color = secondary.copy(alpha = 0.62f),
            topLeft = Offset(size.width * 0.16f, size.height * 0.58f),
            size = Size(size.width * 0.68f, size.height * 0.24f),
            style = Stroke(width = 4.dp.toPx())
        )
        drawRect(
            color = primary.copy(alpha = 0.78f),
            topLeft = Offset(size.width * 0.31f, size.height * 0.32f),
            size = Size(size.width * 0.38f, size.height * 0.3f)
        )
        drawCircle(secondary, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.32f, size.height * 0.34f))
        drawCircle(secondary, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.68f, size.height * 0.34f))
        drawLine(outline, Offset(size.width * 0.2f, size.height * 0.77f), Offset(size.width * 0.8f, size.height * 0.77f), strokeWidth = 3.dp.toPx())
    }
}

private fun GameCapability.label(): String = when (this) {
    GameCapability.AugmentedReality -> "Augmented Reality"
    GameCapability.LocalWifi -> "Local Wi-Fi"
    GameCapability.TeamGame -> "Team Game"
    GameCapability.Mathematics -> "Mathematics"
    GameCapability.OfflineMatch -> "Offline Match"
    GameCapability.Diagnostics -> "Diagnostics"
    GameCapability.SharedSpatialOrigin -> "Shared Origin"
    GameCapability.HostAuthoritative -> "Host Authority"
}
