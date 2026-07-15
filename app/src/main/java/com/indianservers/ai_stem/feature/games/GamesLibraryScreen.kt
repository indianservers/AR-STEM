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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.games.api.GameArRequirement
import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.api.GameHeroArtwork
import com.indianservers.ai_stem.feature.games.api.GameHowToPlay
import com.indianservers.ai_stem.feature.games.api.GamePlayerMode
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.interaction.TopArEnhancementRegistry

@Composable
fun GamesLibraryScreen(
    onBack: () -> Unit,
    onOpenTournamentHub: () -> Unit,
    onOpenTeacherAuthoringStudio: () -> Unit,
    onOpenGame: (GameDefinition) -> Unit,
    onOpenDetails: (GameDefinition) -> Unit,
    onHowToPlay: (GameDefinition) -> Unit,
    onComingSoon: (GameDefinition) -> Unit
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    Scaffold(
        topBar = { GamesHeader(title = "Games Library", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("games-library-screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Games", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Six catalog-driven mathematics AR games. Play what is verified, preview what is coming next.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                SharedArEnhancementsCard()
            }
            item {
                TournamentHubCard(onOpen = onOpenTournamentHub)
            }
            item {
                TeacherAuthoringStudioCard(onOpen = onOpenTeacherAuthoringStudio)
            }
            items(GamesCatalog.games, key = { it.id }) { game ->
                GameLibraryCard(
                    game = game,
                    onOpen = { onOpenGame(game) },
                    onDetails = { onOpenDetails(game) },
                    onHowToPlay = { onHowToPlay(game) },
                    onComingSoon = { onComingSoon(game) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedArEnhancementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("shared-ar-enhancements-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Shared AR Engine Upgrades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("These interaction systems are available to every Maths AR game.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TopArEnhancementRegistry.labels.take(3).forEach { label -> BadgePill(label) }
                BadgePill("+2 more")
            }
        }
    }
}

@Composable
private fun TeacherAuthoringStudioCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("teacher-authoring-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Route, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Teacher Authoring Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Create missions, campaigns, routes, presets, content packs, backups and readiness reports for Maths AR games.")
                }
            }
            Button(onClick = onOpen, modifier = Modifier.testTag("open-teacher-authoring")) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text("Open Studio")
            }
        }
    }
}

@Composable
private fun TournamentHubCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("tournament-hub-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Maths AR Tournament Hub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Run brackets, leagues, mixed-game events, spectators, replays and classroom reports without starting ARCore.")
                }
            }
            Button(onClick = onOpen, modifier = Modifier.testTag("open-tournament-hub")) {
                Icon(Icons.Outlined.SportsEsports, contentDescription = null)
                Text("Open Hub")
            }
        }
    }
}

@Composable
fun GameDetailsScreen(
    gameId: String,
    onBack: () -> Unit,
    onPlay: (GameDefinition) -> Unit,
    onHowToPlay: (GameDefinition) -> Unit
) {
    val game = GamesCatalog.gameOrNull(gameId)
    if (game == null) {
        UnknownGameScreen(onBack = onBack)
        return
    }
    Scaffold(topBar = { GamesHeader(title = "Game Details", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("game-details-${game.id}"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GameHero(game, Modifier.fillMaxWidth()) }
            item { GameSection("Concept", listOf(game.description)) }
            item { GameSection("Player Modes", game.playerModes.map { it.label() }) }
            item { GameSection("Mathematics Topics", game.supportedTopics) }
            item { GameSection("Recommended", listOf(game.recommendedGrade, game.recommendedPlayers)) }
            item { GameSection("Environment", listOf(if (game.outdoorRequired) "Outdoor" else "Indoor", game.arRequirement.label())) }
            item { GameSection("Network", listOf(if (game.localWifiSupported) "Local Wi-Fi supported" else "No network required yet")) }
            item { GameSection("Learning Outcomes", game.learningOutcomes) }
            item { GameSection("Main Game Loop", game.mainGameLoop) }
            item { GameSection("Device Requirements", game.deviceRequirements) }
            item { GameSection("Accessibility", game.accessibilitySupport) }
            item { AvailabilityCard(game) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onHowToPlay(game) }, modifier = Modifier.testTag("details-howto-${game.id}")) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                        Text("How to Play")
                    }
                    if (game.playable) {
                        Button(onClick = { onPlay(game) }, modifier = Modifier.testTag("details-play-${game.id}")) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Text("Play")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameHowToPlayScreen(
    gameId: String,
    onBack: () -> Unit,
    onDetails: (GameDefinition) -> Unit,
    onPlay: (GameDefinition) -> Unit
) {
    val game = GamesCatalog.gameOrNull(gameId)
    if (game == null) {
        UnknownGameScreen(onBack = onBack)
        return
    }
    val sections = game.howToPlay.sections()
    var selectedSection by rememberSaveable(game.id) { mutableStateOf(0) }
    selectedSection = selectedSection.coerceIn(0, sections.lastIndex)
    Scaffold(topBar = { GamesHeader(title = "How to Play", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("game-howto-${game.id}"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GameHero(game, Modifier.fillMaxWidth()) }
            item {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text("Estimated reading time: ${game.howToPlay.estimatedReadingMinutes} min", Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                HowToPlaySectionNavigation(
                    sections = sections,
                    selectedIndex = selectedSection,
                    onSelect = { selectedSection = it }
                )
            }
            item { HowToPlaySectionCard(section = sections[selectedSection]) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { selectedSection = (selectedSection - 1).coerceAtLeast(0) },
                        enabled = selectedSection > 0,
                        modifier = Modifier.testTag("howto-prev-${game.id}")
                    ) { Text("Previous") }
                    Button(
                        onClick = { selectedSection = (selectedSection + 1).coerceAtMost(sections.lastIndex) },
                        enabled = selectedSection < sections.lastIndex,
                        modifier = Modifier.testTag("howto-next-${game.id}")
                    ) { Text("Next") }
                }
            }
            item { AvailabilityCard(game) }
            item {
                OutlinedButton(onClick = { onDetails(game) }, modifier = Modifier.fillMaxWidth().testTag("howto-details-${game.id}")) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Text("Return to Game Details")
                }
            }
            if (game.playable) {
                item {
                    Button(onClick = { onPlay(game) }, modifier = Modifier.fillMaxWidth().testTag("howto-play-${game.id}")) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Text("Play ${game.title}")
                    }
                }
            }
        }
    }
}

@Composable
fun GameComingSoonScreen(
    gameId: String,
    onBack: () -> Unit,
    onHowToPlay: (GameDefinition) -> Unit
) {
    val game = GamesCatalog.gameOrNull(gameId)
    if (game == null) {
        UnknownGameScreen(onBack = onBack)
        return
    }
    Scaffold(topBar = { GamesHeader(title = "Coming Soon", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("game-coming-soon-${game.id}"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GameHero(game, Modifier.fillMaxWidth()) }
            item { AvailabilityCard(game) }
            item { GameSection("Concept", listOf(game.description)) }
            item { GameSection("Planned Learning Areas", game.supportedTopics) }
            item { GameSection("Planned Player Modes", game.playerModes.map { it.label() }) }
            item { GameSection("Planned Technology", game.capabilities.map { it.label() }) }
            item { GameSection("Device Requirements", game.deviceRequirements) }
            item {
                OutlinedButton(
                    onClick = { onHowToPlay(game) },
                    modifier = Modifier.fillMaxWidth().testTag("coming-soon-howto-${game.id}")
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Text("View How to Play")
                }
            }
        }
    }
}

private data class HowToPlaySection(val title: String, val lines: List<String>)

private fun GameHowToPlay.sections(): List<HowToPlaySection> = listOf(
    HowToPlaySection("What Is This Game?", listOf(overview)),
    HowToPlaySection("Quick Start", quickStartSteps),
    HowToPlaySection("What You Will Learn", learningObjectives),
    HowToPlaySection("Player Modes", playerModes),
    HowToPlaySection("Before You Start", setupSteps),
    HowToPlaySection("How a Match Works", playSteps),
    HowToPlaySection("Controls", controls),
    HowToPlaySection("Team Roles", roles),
    HowToPlaySection("Scoring", scoring),
    HowToPlaySection("How to Win", listOf(winCondition)),
    HowToPlaySection("Safety", safetyNotes),
    HowToPlaySection("Device Requirements", deviceRequirements),
    HowToPlaySection("Accessibility", accessibilityNotes),
    HowToPlaySection("Practice and Tutorial", listOf(tutorialAvailability))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HowToPlaySectionNavigation(
    sections: List<HowToPlaySection>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEachIndexed { index, section ->
                    if (index == selectedIndex) {
                        Button(onClick = { onSelect(index) }, modifier = Modifier.testTag("howto-section-$index")) { Text("${index + 1}. ${section.title}") }
                    } else {
                        OutlinedButton(onClick = { onSelect(index) }, modifier = Modifier.testTag("howto-section-$index")) { Text("${index + 1}. ${section.title}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToPlaySectionCard(section: HowToPlaySection) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            section.lines.forEachIndexed { index, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("${index + 1}", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Text(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun UnknownGameScreen(onBack: () -> Unit) {
    Scaffold(topBar = { GamesHeader(title = "Game Unavailable", onBack = onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
            Text("This game entry is no longer available.", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GamesHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameLibraryCard(
    game: GameDefinition,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    onHowToPlay: () -> Unit,
    onComingSoon: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game-card-${game.id}")
            .semantics {
                contentDescription = "${game.title}. ${game.tagline}. ${game.availability.label()}."
                role = Role.Button
            },
        colors = CardDefaults.cardColors(
            containerColor = if (game.playable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                GameArtwork(game.heroArtwork, Modifier.size(92.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(game.tagline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(game.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.playerModes.take(1).forEach { BadgePill(it.label()) }
                BadgePill(game.arRequirement.label())
                if (game.outdoorRequired) BadgePill("Outdoor")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                game.supportedTopics.take(3).forEach { TopicPill(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Text(game.recommendedPlayers, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onDetails, modifier = Modifier.testTag("view-game-${game.id}")) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Text("View Game")
                }
                OutlinedButton(onClick = onHowToPlay, modifier = Modifier.testTag("howto-game-${game.id}")) {
                    Icon(Icons.Outlined.Route, contentDescription = null)
                    Text("How to Play")
                }
                if (game.playable) {
                    Button(onClick = onOpen, modifier = Modifier.testTag("open-game-${game.id}")) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Text("Play")
                    }
                } else {
                    OutlinedButton(onClick = onComingSoon, modifier = Modifier.testTag("coming-soon-game-${game.id}")) {
                        Text("Coming Soon")
                    }
                }
            }
            if (!game.playable) Text(game.availabilityMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GameHero(game: GameDefinition, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GameArtwork(game.heroArtwork, Modifier.size(104.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(game.tagline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(game.availabilityMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GameSection(title: String, lines: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun AvailabilityCard(game: GameDefinition) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(game.availability.label(), fontWeight = FontWeight.SemiBold)
            Text(game.availabilityMessage)
        }
    }
}

@Composable
private fun BadgePill(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TopicPill(label: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun GamesIndexIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
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
private fun GameArtwork(hero: GameHeroArtwork, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        drawCircle(primary.copy(alpha = 0.14f), radius = size.minDimension * 0.48f, center = center)
        when (hero) {
            GameHeroArtwork.Fortress -> {
                drawRect(primary.copy(alpha = 0.76f), Offset(size.width * 0.26f, size.height * 0.38f), Size(size.width * 0.48f, size.height * 0.34f))
                drawRect(secondary.copy(alpha = 0.78f), Offset(size.width * 0.34f, size.height * 0.24f), Size(size.width * 0.12f, size.height * 0.18f))
                drawRect(secondary.copy(alpha = 0.78f), Offset(size.width * 0.54f, size.height * 0.24f), Size(size.width * 0.12f, size.height * 0.18f))
                drawLine(outline, Offset(size.width * 0.2f, size.height * 0.78f), Offset(size.width * 0.8f, size.height * 0.78f), strokeWidth = 3.dp.toPx())
            }
            GameHeroArtwork.EscapeRoom -> {
                drawRect(primary.copy(alpha = 0.7f), Offset(size.width * 0.28f, size.height * 0.2f), Size(size.width * 0.44f, size.height * 0.6f), style = Stroke(width = 5.dp.toPx()))
                drawCircle(secondary, size.minDimension * 0.06f, Offset(size.width * 0.62f, size.height * 0.5f))
            }
            GameHeroArtwork.GeometryStudio -> {
                drawLine(outline, Offset(size.width * 0.2f, size.height * 0.72f), Offset(size.width * 0.76f, size.height * 0.72f), strokeWidth = 4.dp.toPx())
                drawLine(primary, Offset(size.width * 0.3f, size.height * 0.7f), Offset(size.width * 0.5f, size.height * 0.25f), strokeWidth = 4.dp.toPx())
                drawLine(primary, Offset(size.width * 0.5f, size.height * 0.25f), Offset(size.width * 0.72f, size.height * 0.7f), strokeWidth = 4.dp.toPx())
            }
            GameHeroArtwork.FactoryLine -> {
                drawRect(primary.copy(alpha = 0.65f), Offset(size.width * 0.16f, size.height * 0.58f), Size(size.width * 0.68f, size.height * 0.12f))
                repeat(3) { index -> drawCircle(secondary, size.minDimension * 0.08f, Offset(size.width * (0.28f + index * 0.22f), size.height * 0.44f)) }
            }
            GameHeroArtwork.CoordinateField -> {
                repeat(4) { index ->
                    val p = size.width * (0.25f + index * 0.15f)
                    drawLine(outline.copy(alpha = 0.35f), Offset(p, size.height * 0.18f), Offset(p, size.height * 0.82f), strokeWidth = 1.dp.toPx())
                    drawLine(outline.copy(alpha = 0.35f), Offset(size.width * 0.18f, p), Offset(size.width * 0.82f, p), strokeWidth = 1.dp.toPx())
                }
                drawLine(primary, Offset(size.width * 0.22f, size.height * 0.66f), Offset(size.width * 0.72f, size.height * 0.34f), strokeWidth = 4.dp.toPx())
                drawCircle(secondary, size.minDimension * 0.07f, Offset(size.width * 0.72f, size.height * 0.34f))
            }
            GameHeroArtwork.ExpeditionMap -> {
                drawCircle(secondary.copy(alpha = 0.8f), size.minDimension * 0.07f, Offset(size.width * 0.3f, size.height * 0.62f))
                drawCircle(primary.copy(alpha = 0.8f), size.minDimension * 0.07f, Offset(size.width * 0.7f, size.height * 0.34f))
                drawLine(outline, Offset(size.width * 0.3f, size.height * 0.62f), Offset(size.width * 0.48f, size.height * 0.42f), strokeWidth = 3.dp.toPx())
                drawLine(outline, Offset(size.width * 0.48f, size.height * 0.42f), Offset(size.width * 0.7f, size.height * 0.34f), strokeWidth = 3.dp.toPx())
                drawCircle(primary.copy(alpha = 0.12f), size.minDimension * 0.28f, Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

private fun GamePlayerMode.label(): String = when (this) {
    GamePlayerMode.SinglePlayer -> "Single Player"
    GamePlayerMode.Team -> "Team Game"
    GamePlayerMode.OutdoorMap -> "Outdoor Map"
}

private fun GameArRequirement.label(): String = when (this) {
    GameArRequirement.Required -> "AR Required"
    GameArRequirement.Optional -> "AR Optional"
    GameArRequirement.NotRequired -> "No AR Required"
}

private fun GameAvailability.label(): String = when (this) {
    GameAvailability.Available -> "Available"
    GameAvailability.Beta -> "Beta"
    GameAvailability.ComingSoon -> "Coming Soon"
    GameAvailability.DeviceUnsupported -> "Device Unsupported"
    GameAvailability.UpdateRequired -> "Update Required"
    GameAvailability.TemporarilyDisabled -> "Temporarily Disabled"
    GameAvailability.RequiresSupportedDevice -> "Requires Supported Device"
}

@Suppress("unused")
private fun GameCapability.label(): String = when (this) {
    GameCapability.AugmentedReality -> "Augmented Reality"
    GameCapability.LocalWifi -> "Local Wi-Fi"
    GameCapability.TeamGame -> "Team Game"
    GameCapability.SinglePlayer -> "Single Player"
    GameCapability.Mathematics -> "Mathematics"
    GameCapability.OfflineMatch -> "Offline Match"
    GameCapability.Outdoor -> "Outdoor"
    GameCapability.OpenMap -> "Open Map"
    GameCapability.Diagnostics -> "Diagnostics"
    GameCapability.SharedSpatialOrigin -> "Shared Origin"
    GameCapability.HostAuthoritative -> "Host Authority"
    GameCapability.SingleDeviceHost -> "Single Device Host"
    GameCapability.OptionalDepth -> "Optional Depth"
    GameCapability.OptionalEnvironmentalHdr -> "Optional HDR"
    GameCapability.MarkerInteractions -> "Marker Interactions"
    GameCapability.PuzzleSequencing -> "Puzzle Sequencing"
    GameCapability.PlaneDetection -> "Plane Detection"
    GameCapability.Measurement -> "Measurement"
    GameCapability.ObjectManipulation -> "Object Manipulation"
    GameCapability.FloorPlane -> "Floor Plane"
    GameCapability.SafeMovementArea -> "Safe Movement"
    GameCapability.FutureLocationPermission -> "Future Location"
    GameCapability.OfflineMaps -> "Offline Maps"
    GameCapability.TeacherApprovedRoute -> "Teacher Route"
}
