package com.indianservers.ai_stem.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.core.model.StemSubject
import com.indianservers.ai_stem.core.model.disabledMessage
import com.indianservers.ai_stem.feature.games.GamesIndexIcon
import kotlinx.coroutines.launch

@Composable
fun SubjectSelectionScreen(
    onBack: () -> Unit,
    onMathematics: () -> Unit,
    onSolarSystem: () -> Unit,
    onGames: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleHeader(title = "Choose a STEM Subject", onBack = onBack)
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(StemSubject.entries) { subject ->
                SubjectCard(
                    subject = subject,
                    icon = subject.icon(),
                    onClick = {
                        when {
                            subject == StemSubject.Mathematics -> onMathematics()
                            subject == StemSubject.SolarSystem -> onSolarSystem()
                            subject.enabled -> onMathematics()
                            else -> scope.launch { snackbarHostState.showSnackbar(subject.disabledMessage()) }
                        }
                    }
                )
            }
            item {
                GamesEntryCard(onClick = onGames)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("games-entry-card")
            .semantics {
                role = Role.Button
                contentDescription = "Games"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GamesIndexIcon(Modifier.size(48.dp))
            Text("Games", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Play STEM challenges, team activities and AR math games.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectCard(subject: StemSubject, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        colors = CardDefaults.cardColors(
            containerColor = if (subject.enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = subject.title)
            Text(subject.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subject.description, style = MaterialTheme.typography.bodyMedium)
            if (!subject.enabled) {
                Text("Coming in a future phase", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SimpleHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            title,
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun StemSubject.icon(): ImageVector = when (this) {
    StemSubject.Mathematics -> Icons.Outlined.Calculate
    StemSubject.SolarSystem -> Icons.Outlined.Public
    StemSubject.Physics -> Icons.Outlined.Speed
    StemSubject.Chemistry -> Icons.Outlined.Science
    StemSubject.Biology -> Icons.Outlined.Biotech
}
