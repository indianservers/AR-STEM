package com.indianservers.ai_stem.feature.games.matharena

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.ArCoreApk
import com.indianservers.ai_stem.feature.games.arcore.ArAvailabilityResult
import com.indianservers.ai_stem.feature.games.arcore.ArFeatureCapability
import com.indianservers.ai_stem.feature.games.arcore.ArGameCapabilityRole
import com.indianservers.ai_stem.feature.games.diagnostics.ArDiagnosticsReport
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaConnectionState
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomState
import com.indianservers.ai_stem.feature.games.multiplayer.network.DiscoveredArenaRoom
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MissionProgress
import com.indianservers.ai_stem.feature.games.mission.RoleClue
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.classroom.MatchSummaryRecord
import com.indianservers.ai_stem.feature.games.spatial.AR_MATH_ARENA_MARKER_NAME
import com.indianservers.ai_stem.feature.games.spatial.AR_MATH_ARENA_MARKER_WIDTH_METRES
import com.indianservers.ai_stem.feature.games.spatial.SpatialSessionState

@Composable
fun ArMathArenaScreen(
    onBack: () -> Unit,
    onHowToPlay: (() -> Unit)? = null,
    viewModel: ArenaLobbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshDiagnostics()
    }
    val installAr = remember(activity) {
        {
            if (activity != null) {
                runCatching { ArCoreApk.getInstance().requestInstall(activity, true) }
                viewModel.refreshDiagnostics()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("AR Math Arena", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("ar-math-arena-screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ArenaHero(state) }
            item { StatusCard(state.status) }
            when (state.mode) {
                ArenaShellMode.Landing -> item { LandingMenu(viewModel, onHowToPlay) }
                ArenaShellMode.HostSetup -> item { HostSetupCard(state, viewModel) }
                ArenaShellMode.JoinGame -> {
                    item { JoinCard(state, viewModel) }
                    items(state.discoveredRooms) { room -> DiscoveredRoomRow(room, viewModel) }
                }
                ArenaShellMode.HostLobby,
                ArenaShellMode.PlayerLobby -> item { LobbyCard(state.roomState, state.qrJoinText, state.isHosting, viewModel) }
                ArenaShellMode.CalibrationLobby -> item { CalibrationLobbyCard(state.roomState, state.spatialSession, state.isHosting, viewModel) }
                ArenaShellMode.MissionTest -> item { MissionTestCard(state, viewModel) }
                ArenaShellMode.BaseDefense -> item { BaseDefenseCard(state.baseDefense, viewModel) }
                ArenaShellMode.TeacherDashboard -> item { TeacherDashboardCard(state, viewModel) }
                ArenaShellMode.HowToPlay -> item { HowToPlayCard() }
                ArenaShellMode.DeviceCheck -> {
                    item {
                        DeviceActions(
                            report = state.diagnostics,
                            onRefresh = viewModel::refreshDiagnostics,
                            onCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            onInstall = installAr
                        )
                    }
                    state.diagnostics?.let { report ->
                        item { DiagnosticsSummary(report) }
                        items(report.capabilityMatrix.features) { feature -> CapabilityRow(feature) }
                    }
                }
                ArenaShellMode.NetworkTest -> item { NetworkCard(state, viewModel) }
                ArenaShellMode.TeacherMode -> item { TeacherControlPanel(state, viewModel) }
                ArenaShellMode.Accessibility -> item { AccessibilityPanel(state, viewModel) }
                ArenaShellMode.Settings -> item { GameSettingsPanel(state, viewModel) }
            }
            item { PrivacyDisclosureCard() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArenaHero(state: ArenaLobbyUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("ar-math-arena-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SportsEsports, contentDescription = null)
                Column {
                    Text("AR Math Arena", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Local Wi-Fi multiplayer. Host authoritative. AR optional until match starts.")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Offline LAN", "Host Game", "Join Game", "AR Check", "Network Test").forEach {
                    StatusPill(it)
                }
            }
            Text("Player: ${state.profile.displayName}  Room: ${state.roomState?.roomCode ?: "none"}")
        }
    }
}

@Composable
private fun LandingMenu(viewModel: ArenaLobbyViewModel, onHowToPlay: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Play on Local Wi-Fi" to ArenaShellMode.JoinGame,
                "Host Game" to ArenaShellMode.HostSetup,
                "Join Game" to ArenaShellMode.JoinGame,
                "How to Play" to ArenaShellMode.HowToPlay,
                "AR Device Check" to ArenaShellMode.DeviceCheck,
                "Local Network Test" to ArenaShellMode.NetworkTest,
                "Mission Test" to ArenaShellMode.MissionTest,
                "Base Defence" to ArenaShellMode.BaseDefense,
                "Teacher Dashboard" to ArenaShellMode.TeacherDashboard,
                "Teacher Mode" to ArenaShellMode.TeacherMode,
                "Accessibility" to ArenaShellMode.Accessibility,
                "Game Settings" to ArenaShellMode.Settings
            ).forEach { (label, mode) ->
                OutlinedButton(
                    onClick = {
                        if (mode == ArenaShellMode.HowToPlay && onHowToPlay != null) onHowToPlay() else viewModel.setMode(mode)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(label) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherDashboardCard(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Teacher Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Host-only classroom controls, accessibility, safety and local analytics.")
            val match = state.baseDefense
            Text("Grade ${state.teacherSettings.gradeBand} | ${state.teacherSettings.difficulty} | Teams ${state.teacherSettings.teamCount}")
            Text("Match: ${match?.stage ?: "not started"} | Wave ${match?.currentWave ?: 0} | Time ${match?.matchClockSeconds ?: 0}s")
            Text("Accessibility: large text ${state.accessibility.largeText}, seated ${state.accessibility.seatedPlayMode}, reduced motion ${state.accessibility.reducedMotion}")
            Text("Boundary: ${state.boundaryWarning}")
            state.adaptiveDecision?.let { Text("Adaptive: ${it.previous} -> ${it.next}. ${it.reason}", style = MaterialTheme.typography.bodySmall) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::teacherPauseResume, label = { Text(if (match?.paused == true) "Resume" else "Pause") })
                AssistChip(onClick = viewModel::teacherReduceDifficulty, label = { Text("Adapt Difficulty") })
                AssistChip(onClick = viewModel::toggleLargeText, label = { Text("Large Text") })
                AssistChip(onClick = viewModel::toggleSeatedPlay, label = { Text("Seated Play") })
                AssistChip(onClick = viewModel::evaluateBoundaryAtBase, label = { Text("Boundary Check") })
                AssistChip(onClick = viewModel::generateLocalSummary, label = { Text("Summary") })
                AssistChip(onClick = viewModel::teacherEndMatch, label = { Text("End Match") })
            }
            Text("Interventions logged: ${state.interventions.size}")
            state.matchSummary?.let { MatchSummaryView(it) }
            state.exportedReportPreview?.let {
                Text("CSV export preview", fontWeight = FontWeight.SemiBold)
                Text(it.lines().take(4).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@Composable
private fun MatchSummaryView(summary: MatchSummaryRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Local Analytics", fontWeight = FontWeight.SemiBold)
        summary.teamSummaries.forEach {
            Text("${it.teamName} ${it.symbol}: score ${it.finalScore}, accuracy ${"%.0f".format(it.accuracy * 100)}%, health ${it.baseHealth}", style = MaterialTheme.typography.bodySmall)
        }
        summary.topicPerformance.take(4).forEach {
            Text("${it.topic}: ${it.correct}/${it.attempts} correct", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaseDefenseCard(match: BaseDefenseMatchState?, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AR Math Base Defence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Host-authoritative 10-15 minute match loop. AR renderers consume the same shared transforms.")
            if (match == null) {
                Button(onClick = { viewModel.startBaseDefense(shortMatch = true) }) { Text("Start Short Match") }
                return@Column
            }
            Text("Stage: ${match.stage} | Wave ${match.currentWave} | ${if (match.paused) "Paused" else "Live"}")
            Text("Bases: ${match.bases.size} | Resources: ${match.resources.count { it.lifecycle.name == "Active" }} | Defences: ${match.defenses.size} | Enemies: ${match.enemies.count { it.lifecycle.name == "Active" }}")
            match.bases.forEach {
                Text("${it.teamName}: HP ${it.health}/${it.maxHealth}, energy ${it.energy}, score ${it.score}", style = MaterialTheme.typography.bodySmall)
            }
            match.boss?.let {
                Text("Boss ${it.phase}: HP ${it.health}, roles ${it.completedRoles.size}/${it.requiredRoles.size}", fontWeight = FontWeight.SemiBold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::placeDemoBase, label = { Text("Place Base") })
                AssistChip(onClick = viewModel::advanceBaseDefenseStage, label = { Text("Next Stage") })
                AssistChip(onClick = viewModel::spawnBaseDefenseResources, label = { Text("Spawn Resources") })
                AssistChip(onClick = viewModel::solveFirstResource, label = { Text("Solve Resource") })
                AssistChip(onClick = viewModel::buildDemoDefense, label = { Text("Build Defence") })
                AssistChip(onClick = viewModel::spawnWaveOne, label = { Text("Spawn Wave") })
                AssistChip(onClick = viewModel::tickBaseDefense, label = { Text("Tick") })
                AssistChip(onClick = viewModel::startBossBattle, label = { Text("Boss") })
                AssistChip(onClick = viewModel::pauseOrResumeBaseDefense, label = { Text(if (match.paused) "Resume" else "Pause") })
            }
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionTestCard(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Non-AR Mission Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Teacher/developer verification for generation, validation, hints, role clues and scoring. No AR renderer required.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MathSkill.entries.take(8).forEach { skill ->
                    AssistChip(onClick = { viewModel.generateMissionTest(skill) }, label = { Text(skill.name.take(16)) })
                }
            }
            Button(onClick = { viewModel.generateMissionTest() }) { Text("Generate Mission") }
            state.testProgress?.let { progress -> MissionProgressView(progress, state.testRoleClues) }
            OutlinedTextField(
                value = state.testAnswer,
                onValueChange = viewModel::setMissionTestAnswer,
                label = { Text("Test answer") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::startMissionTest) { Text("Start") }
                Button(onClick = viewModel::submitMissionTestAnswer) { Text("Submit") }
            }
            state.testValidation?.let { Text("${it.status}: ${it.message}") }
            state.testScore?.let { Text("Host score: $it", fontWeight = FontWeight.SemiBold) }
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@Composable
private fun MissionProgressView(progress: MissionProgress, clues: List<RoleClue>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(progress.mission.prompt.studentText, fontWeight = FontWeight.SemiBold)
        Text("${progress.mission.topic} | ${progress.mission.skill} | ${progress.mission.difficulty} | ${progress.state}")
        Text("Hints", fontWeight = FontWeight.SemiBold)
        progress.mission.hints.take(2).forEach { Text("${it.order}. ${it.text}", style = MaterialTheme.typography.bodySmall) }
        Text("Role clues", fontWeight = FontWeight.SemiBold)
        clues.forEach { Text("${it.role}: ${it.clue}", style = MaterialTheme.typography.bodySmall) }
        Text("Solution preview", fontWeight = FontWeight.SemiBold)
        progress.mission.solutionSteps.forEach { Text("${it.order}. ${it.text}", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun HostSetupCard(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Host Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.profile.displayName,
                onValueChange = viewModel::updateDisplayName,
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Room: ${state.hostSettings.roomName}  Code: ${state.hostSettings.roomCode}")
            Text("Grade ${state.hostSettings.gradeLevel} | ${state.hostSettings.difficulty} | ${state.hostSettings.teamCount} teams | ${state.hostSettings.maxPlayers} players")
            Button(onClick = viewModel::hostGame, modifier = Modifier.fillMaxWidth()) { Text("Create Local Wi-Fi Room") }
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

@Composable
private fun JoinCard(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Join Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.profile.displayName,
                onValueChange = viewModel::updateDisplayName,
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.joinInput,
                onValueChange = viewModel::setJoinInput,
                label = { Text("Room code or QR text") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::joinFromQr) { Text("Join QR") }
                OutlinedButton(onClick = viewModel::saveProfile) { Text("Save Profile") }
                OutlinedButton(onClick = viewModel::refreshNetwork) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text("Scan")
                }
            }
            Text("Discovered local rooms appear below. QR text carries the temporary room token.")
        }
    }
}

@Composable
private fun DiscoveredRoomRow(room: DiscoveredArenaRoom, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(room.roomName, fontWeight = FontWeight.SemiBold)
                Text("${room.roomCode}  ${room.hostAddress ?: "resolving"}:${room.port}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { viewModel.joinSelectedRoom(room) }) { Text("Join") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LobbyCard(room: ArenaRoomState?, qrJoinText: String?, isHost: Boolean, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Text(if (isHost) "Host Lobby" else "Player Lobby", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (room == null) {
                Text("Waiting for host snapshot...")
            } else {
                Text("Room ${room.roomCode} | Version ${room.roomVersion} | ${if (room.locked) "Locked" else "Open"}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    room.players.forEach { player ->
                        StatusPill("${player.displayName} ${player.teamId ?: "-"} ${player.role ?: "-"} ${if (player.ready) "ready" else "not ready"}")
                    }
                }
                if (isHost) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::autoAssign) { Text("Auto Teams") }
                        OutlinedButton(onClick = viewModel::lockRoom) { Text(if (room.locked) "Unlock" else "Lock") }
                        Button(onClick = viewModel::startMatch, enabled = room.canStartMatch) { Text("Start") }
                    }
                }
            }
            qrJoinText?.let {
                Text("QR join payload", fontWeight = FontWeight.SemiBold)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArenaRole.entries.forEach { role -> AssistChip(onClick = { viewModel.chooseRole(role) }, label = { Text(role.name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.toggleReady() }) { Text("Toggle Ready") }
                OutlinedButton(onClick = viewModel::openCalibrationLobby) { Text("Calibrate AR") }
                OutlinedButton(onClick = viewModel::leaveRoom) { Text("Leave") }
            }
        }
    }
}

@Composable
private fun CalibrationLobbyCard(
    room: ArenaRoomState?,
    spatial: SpatialSessionState?,
    isHost: Boolean,
    viewModel: ArenaLobbyViewModel
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Shared AR Origin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Recommended mode: scan the same printed $AR_MATH_ARENA_MARKER_NAME marker on every device.")
            Text("Marker width: ${(AR_MATH_ARENA_MARKER_WIDTH_METRES * 100).toInt()} cm. Origin center, +X right, +Y outward, +Z arrow forward.")
            Text("State: ${spatial?.originState ?: "Undefined"} | Origin version: ${spatial?.originVersion ?: 0}")
            if (isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::startMarkerCalibration) { Text("Marker Origin") }
                    OutlinedButton(onClick = viewModel::startSurfaceFallbackCalibration) { Text("Surface Fallback") }
                }
            }
            Text("Player calibration", fontWeight = FontWeight.SemiBold)
            val statuses = spatial?.playerStatuses.orEmpty()
            room?.players.orEmpty().forEach { player ->
                val status = statuses[player.playerId]
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(player.displayName, fontWeight = FontWeight.SemiBold)
                        Text("${status?.calibrationState ?: "Searching"} | score ${status?.qualityScore ?: 0}")
                        Text(status?.instruction ?: "Open camera calibration, show the full marker, move slowly, reduce glare, and keep the marker flat.")
                    }
                }
            }
            Text("Surface fallback is less precise. The host places a tracked plane origin; clients align manually to the same reference.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::markCurrentPlayerAsNonArAnalyst) { Text("Non-AR Analyst") }
                Button(onClick = viewModel::startMatch, enabled = room?.canStartMatch == true) { Text("Start If Calibrated") }
            }
            OutlinedButton(onClick = { viewModel.setMode(if (isHost) ArenaShellMode.HostLobby else ArenaShellMode.PlayerLobby) }) { Text("Back to Lobby") }
        }
    }
}

@Composable
private fun NetworkCard(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    val network = state.networkDiagnostics
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local Network Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(network?.message ?: "Checking network...")
            Text("Wi-Fi/LAN active: ${network?.wifiOrLanActive == true}")
            Text("Metered: ${network?.metered == true}")
            Text("Addresses: ${network?.activeAddresses?.joinToString().orEmpty().ifBlank { "none" }}")
            Text("LAN sockets use Android INTERNET permission; no remote server is used.")
            Button(onClick = viewModel::refreshNetwork) { Text("Run Test Again") }
        }
    }
}

@Composable
private fun DeviceActions(report: ArDiagnosticsReport?, onRefresh: () -> Unit, onCamera: () -> Unit, onInstall: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AR Device Check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                when (report?.arCoreAvailability) {
                    ArAvailabilityResult.CameraPermissionRequired -> Button(onClick = onCamera) { Text("Allow Camera") }
                    ArAvailabilityResult.SupportedInstallRequired,
                    ArAvailabilityResult.SupportedUpdateRequired -> Button(onClick = onInstall) { Text("Install / Update AR") }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun HowToPlayCard() = InfoCard(
    "How to Play",
    "Host creates a local room, players join on the same Wi-Fi, teams choose roles, everyone readies up, then the host starts the AR match."
)

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body)
        }
    }
}

@Composable
private fun StatusPill(label: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherControlPanel(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Teacher Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Host controls for a live local match. These actions update the same reducer state used by the classroom dashboard.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::teacherPauseResume, label = { Text(if (state.baseDefense?.paused == true) "Resume Match" else "Pause Match") })
                AssistChip(onClick = viewModel::teacherReduceDifficulty, label = { Text("Lower Difficulty") })
                AssistChip(onClick = viewModel::generateLocalSummary, label = { Text("Build Summary") })
                AssistChip(onClick = viewModel::teacherEndMatch, label = { Text("End Match") })
            }
            Text("Room locked: ${state.roomState?.locked ?: false} | Ready players: ${state.roomState?.players?.count { it.ready } ?: 0}")
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccessibilityPanel(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Player comfort settings are local-first and can support seated or non-AR analyst participation.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::toggleLargeText, label = { Text("Large Text: ${state.accessibility.largeText}") })
                AssistChip(onClick = viewModel::toggleSeatedPlay, label = { Text("Seated Play: ${state.accessibility.seatedPlayMode}") })
                AssistChip(onClick = viewModel::markCurrentPlayerAsNonArAnalyst, label = { Text("Use Non-AR Analyst") })
            }
            Text("Reduced motion: ${state.accessibility.reducedMotion} | High contrast: ${state.accessibility.highContrast}")
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@Composable
private fun GameSettingsPanel(state: ArenaLobbyUiState, viewModel: ArenaLobbyViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Game Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Current host setup is applied before room creation and carried into the local match snapshot.")
            Text("Room: ${state.hostSettings.roomName} (${state.hostSettings.roomCode})")
            Text("Grade ${state.hostSettings.gradeLevel} | ${state.hostSettings.difficulty} | ${state.hostSettings.teamCount} teams")
            Text("Players ${state.hostSettings.maxPlayers} | Hints ${state.hostSettings.hintsEnabled} | Marker calibration ${state.hostSettings.sharedMarkerMode}")
            Button(onClick = { viewModel.setMode(ArenaShellMode.HostSetup) }, modifier = Modifier.fillMaxWidth()) { Text("Edit Host Setup") }
            OutlinedButton(onClick = { viewModel.setMode(ArenaShellMode.Landing) }) { Text("Back") }
        }
    }
}

@Composable
private fun StatusCard(status: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(status, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrivacyDisclosureCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, contentDescription = null)
                Text("Privacy and network", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("Camera is used only for AR placement after the lobby. LAN multiplayer uses local Wi-Fi sockets and NSD discovery. No Firebase, internet server, Cloud Anchors, or camera upload is part of the Phase 2 lobby.")
        }
    }
}

@Composable
private fun DiagnosticsSummary(report: ArDiagnosticsReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Device diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DiagnosticLine("Device model", report.deviceModel)
            DiagnosticLine("Android version", report.androidVersion)
            DiagnosticLine("ARCore availability", report.arCoreAvailability.name)
            DiagnosticLine("Google Play Services for AR", report.playServicesForArStatus)
            DiagnosticLine("Camera permission", report.cameraPermission.name)
            DiagnosticLine("Renderer readiness", if (report.rendererReady) "Ready" else "Not ready")
            DiagnosticLine("AR session state", report.sessionState.name)
            DiagnosticLine("Last AR error", report.lastError ?: "None")
        }
    }
}

@Composable
private fun CapabilityRow(feature: ArFeatureCapability) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when (feature.role) {
            ArGameCapabilityRole.RequiredForBaseGameplay -> MaterialTheme.colorScheme.secondaryContainer
            ArGameCapabilityRole.OptionalEnhancement -> MaterialTheme.colorScheme.surfaceVariant
            ArGameCapabilityRole.FutureOutdoorFeature -> MaterialTheme.colorScheme.tertiaryContainer
            ArGameCapabilityRole.CloudDependentFeature,
            ArGameCapabilityRole.Unsupported -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(feature.feature.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("${feature.role.name} - ${if (feature.available) "Available" else "Unavailable"}", style = MaterialTheme.typography.bodySmall)
            Text(feature.note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
