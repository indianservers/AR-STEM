package com.indianservers.ai_stem.feature.games.authoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.games.learning.AdvancedCurriculumCatalog
import com.indianservers.ai_stem.feature.games.learning.CampaignChapter
import com.indianservers.ai_stem.feature.games.learning.CampaignNode
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.tournament.TieBreakRule
import com.indianservers.ai_stem.feature.games.tournament.TournamentEventType
import com.indianservers.ai_stem.feature.games.tournament.TournamentScoreWeights

@Composable
fun TeacherAuthoringStudioScreen(onBack: () -> Unit) {
    val validator = AuthoringValidator()
    val previewEngine = PreviewEngine(validator)
    val packCodec = ContentPackCodec(validator)
    val backupEngine = BackupRestoreEngine()
    val diagnosticsEngine = ReadinessDiagnosticsEngine()
    val mission = sampleMission()
    val campaign = sampleCampaign()
    val gamePreset = sampleGamePreset()
    val tournamentPreset = sampleTournamentPreset()
    val pack = samplePack(mission, campaign, gamePreset, tournamentPreset)
    val backup = backupEngine.createBackup(sampleBackupPlan(), secureKeyManagementAvailable = false)
    val preview = previewEngine.previewMission(mission.copy(previewCompletions = 1), "24")
    val missionReport = validator.validateMission(mission)
    val packExport = packCodec.exportPack(pack)
    val importReport = packCodec.validateImport(packExport, byteSize = packExport.length, entryCount = 6)
    val readiness = diagnosticsEngine.readinessReport(sampleDevices())
    val diagnosticReport = diagnosticsEngine.diagnostics(sampleDiagnostics())
    var selectedSection by rememberSaveable { mutableStateOf(AuthoringSection.MissionBuilder.name) }
    val section = AuthoringSection.valueOf(selectedSection)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Teacher Authoring Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Build, preview, validate and package Maths AR game content.")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("teacher-authoring-studio"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AuthoringOverview(
                    missionValid = missionReport.publishable,
                    importValid = importReport.publishable,
                    diagnosticsValid = diagnosticReport.publishable
                )
            }
            item {
                SectionPicker(selected = section, onSelect = { selectedSection = it.name })
            }
            item {
                when (section) {
                    AuthoringSection.MissionBuilder -> MissionBuilderPanel(mission, missionReport)
                    AuthoringSection.CampaignBuilder -> CampaignBuilderPanel(campaign, validator.validateCampaign(campaign))
                    AuthoringSection.GamePresets -> GamePresetPanel(gamePreset, validator.validatePreset(gamePreset))
                    AuthoringSection.TournamentPresets -> TournamentPresetPanel(tournamentPreset, validator.validateTournamentPreset(tournamentPreset))
                    AuthoringSection.MathExpeditionRoutes -> ExpeditionAuthoringPanel()
                    AuthoringSection.ContentPacks -> ContentPackPanel(pack, packExport, importReport)
                    AuthoringSection.PreviewAndTest -> PreviewPanel(preview)
                    AuthoringSection.ValidationReports -> ValidationPanel(missionReport, importReport, diagnosticReport)
                    AuthoringSection.SchoolSettings -> SchoolSettingsPanel(sampleSchoolProfile())
                    AuthoringSection.BackupAndRestore -> BackupPanel(backup, backupEngine.restore(backup, setOf("missions", "routes")))
                }
            }
            item {
                ReadinessPanel(readiness, diagnosticsEngine.performanceAudit().size)
            }
        }
    }
}

@Composable
private fun AuthoringOverview(missionValid: Boolean, importValid: Boolean, diagnosticsValid: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SportsEsports, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Author playable Maths AR content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("One studio covers missions, game presets, campaigns, route packs, tournaments, device checks and publish readiness.")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Mission", missionValid)
                StatusChip("Pack", importValid)
                StatusChip("Privacy", diagnosticsValid)
            }
            LinearProgressIndicator(progress = { 0.84f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionPicker(selected: AuthoringSection, onSelect: (AuthoringSection) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AuthoringSection.values().forEach { section ->
            AssistChip(
                onClick = { onSelect(section) },
                label = { Text(section.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")) },
                leadingIcon = { if (section == selected) Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.testTag("authoring-section-${section.name}")
            )
        }
    }
}

@Composable
private fun MissionBuilderPanel(mission: AuthoredMission, report: ValidationReport) {
    TwoColumnCard(
        title = mission.title,
        subtitle = "${mission.gameId} • ${mission.grade} • ${mission.difficulty}",
        left = {
            Text(mission.prompt, fontWeight = FontWeight.SemiBold)
            Text("Expected: ${mission.expectedAnswer} ${mission.units.orEmpty()}")
            Text("AR interaction: ${mission.arInteractionType}")
            Text("Accessibility: ${mission.accessibilityAlternative}")
        },
        right = {
            MiniArPreview()
            Text("Publish state: ${AuthoringValidator().publishState(mission)}")
            StatusChip("Validation", report.publishable)
        }
    )
}

@Composable
private fun CampaignBuilderPanel(campaign: AuthoredCampaign, report: ValidationReport) {
    InfoCard("Campaign Builder", "${campaign.chapters.size} chapters • ${campaign.masterySkillIds.size} mastery skills") {
        campaign.chapters.forEach { chapter ->
            Text("${chapter.title}: ${chapter.nodes.joinToString { it.title }}")
        }
        StatusChip("Campaign graph", report.publishable)
    }
}

@Composable
private fun GamePresetPanel(preset: GamePreset, report: ValidationReport) {
    InfoCard("Game Preset", "${preset.gameId} • ${preset.durationMinutes} min • ${preset.teamCount} teams") {
        Text("Roles: ${preset.roles.joinToString()}")
        Text("Network: ${preset.networkMode}; AR quality: ${preset.arQuality}")
        StatusChip("Preset", report.publishable)
    }
}

@Composable
private fun TournamentPresetPanel(preset: TournamentPreset, report: ValidationReport) {
    InfoCard("Tournament Preset", "${preset.eventName} • ${preset.eventType}") {
        Text("Games: ${preset.gameIds.joinToString()}")
        Text("Rounds: ${preset.rounds}; replay: ${preset.replayEnabled}")
        Text("Tie breaks: ${preset.tieBreakRules.joinToString()}")
        StatusChip("Tournament", report.publishable)
    }
}

@Composable
private fun ExpeditionAuthoringPanel() {
    InfoCard("Route Editor Integration", "Outdoor routes stay approval-gated before publishing.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Route, contentDescription = null)
            Text("Start zone, checkpoints, finish zone, no-go areas, offline map metadata and teacher approval are validated together.")
        }
        Text("Preview mode uses route distance and mission checkpoints without recording real student location history.")
    }
}

@Composable
private fun ContentPackPanel(pack: ContentPack, raw: String, report: ValidationReport) {
    InfoCard("Content Pack", "${pack.metadata.title} v${pack.metadata.version}") {
        Text("Export payload: ${raw.take(72)}...")
        Text("Assets: ${pack.assetReferences.joinToString()}")
        StatusChip("Import security", report.publishable)
    }
}

@Composable
private fun PreviewPanel(preview: PreviewResult) {
    InfoCard("Preview And Test Mode", preview.previewId) {
        Text("Answer accepted: ${preview.answerAccepted}")
        Text("Progress mutated: ${preview.progressMutated}")
        Text("Badges awarded: ${preview.badgesAwarded}")
        StatusChip("Preview", preview.valid)
    }
}

@Composable
private fun ValidationPanel(vararg reports: ValidationReport) {
    InfoCard("Validation Reports", "${reports.count { it.publishable }}/${reports.size} publishable") {
        reports.forEachIndexed { index, report ->
            Text("Report ${index + 1}: ${if (report.publishable) "Clear" else report.issues.joinToString { it.code }}")
        }
    }
}

@Composable
private fun SchoolSettingsPanel(profile: SchoolProfile) {
    InfoCard("School Profile", profile.schoolDisplayName) {
        Text("Year: ${profile.academicYear}; houses: ${profile.houseNames.joinToString()}")
        Text("Default duration: ${profile.defaultGameDurationMinutes} minutes")
        Text("Approval required: ${profile.approvalRequired}")
    }
}

@Composable
private fun BackupPanel(backup: BackupPackage, restoreReport: ValidationReport) {
    InfoCard("Backup And Restore", backup.backupId) {
        Text("Encrypted: ${backup.encrypted}; sensitivity: ${backup.sensitivity}")
        Text("Sections: ${backup.includedSections.joinToString()}")
        StatusChip("Selective restore", restoreReport.publishable)
    }
}

@Composable
private fun ReadinessPanel(report: String, auditCount: Int) {
    InfoCard("Device Readiness", "$auditCount performance checkpoints") {
        Text(report.lines().take(3).joinToString("\n"))
    }
}

@Composable
private fun TwoColumnCard(title: String, subtitle: String, left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), content = left)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), content = right)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Map, contentDescription = null)
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle)
                }
            }
            content()
        }
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Surface(
        color = if (ok) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$label: ${if (ok) "Ready" else "Needs work"}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniArPreview() {
    Box(
        Modifier.fillMaxWidth().height(120.dp).background(
            Brush.linearGradient(listOf(Color(0xFF102A43), Color(0xFF4ECDC4), Color(0xFFFFD166)))
        )
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val origin = Offset(size.width * 0.18f, size.height * 0.78f)
            drawLine(Color.White, origin, Offset(size.width * 0.9f, origin.y), strokeWidth = 4f)
            drawLine(Color.White, origin, Offset(origin.x, size.height * 0.12f), strokeWidth = 4f)
            drawArc(Color(0xFFFF6B6B), 190f, 135f, false, topLeft = Offset(size.width * 0.18f, size.height * 0.10f), size = Size(size.width * 0.66f, size.height * 0.8f), style = Stroke(width = 5f))
            drawCircle(Color(0xFFFFF3B0), radius = 10f, center = Offset(size.width * 0.76f, size.height * 0.34f))
        }
        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(36.dp))
    }
}

private fun sampleMission(): AuthoredMission = AuthoredMission(
    missionId = "authored-slope-bridge-001",
    title = "Slope Bridge Builder",
    gameId = "coordinate_conquest_ar",
    topic = "Coordinate geometry",
    skillId = "coordinate-slope",
    grade = GradeBand.Grade8,
    difficulty = DifficultyLevel.Medium,
    prompt = "Build an AR bridge from (0, 0) to (6, 4) and calculate the slope.",
    answerType = AuthoredAnswerType.Fraction,
    expectedAnswer = "2/3",
    units = null,
    tolerance = null,
    workedSolution = listOf("Rise = 4 - 0 = 4", "Run = 6 - 0 = 6", "Slope = 4/6 = 2/3"),
    hints = listOf("Use rise over run.", "Simplify the fraction."),
    commonMisconception = "Students may invert run and rise.",
    timeLimitSeconds = 180,
    score = 240,
    requiredRoles = setOf("Navigator", "Verifier"),
    arInteractionType = "Markerless AR coordinate placement",
    accessibilityAlternative = "Use tabletop coordinate input with the same points.",
    prerequisiteSkillIds = emptySet(),
    tags = setOf("slope", "coordinates")
)

private fun sampleCampaign(): AuthoredCampaign {
    val nodes = listOf(
        CampaignNode("node-slope", "Slope Bridge", "coordinate_conquest_ar", "coordinate-slope", 12L, setOf("line-intersection"), false, false, DifficultyLevel.Medium),
        CampaignNode("node-line", "Intersection Gate", "coordinate_conquest_ar", "line-intersection", 18L, emptySet(), false, true, DifficultyLevel.Hard)
    )
    return AuthoredCampaign(
        campaignId = "campaign-coordinate-foundations",
        title = "Coordinate Foundations",
        chapters = listOf(CampaignChapter("chapter-1", "Slope And Intersections", nodes)),
        masterySkillIds = setOf("coordinate-slope", "line-intersection"),
        completionCriteria = "Complete every final challenge in preview and production."
    )
}

private fun sampleGamePreset(): GamePreset = GamePreset(
    presetId = "preset-slope-team",
    gameId = "coordinate_conquest_ar",
    grade = GradeBand.Grade8,
    topic = "Coordinate geometry",
    difficulty = DifficultyLevel.Medium,
    durationMinutes = 12,
    teamCount = 3,
    roles = setOf("Navigator", "Plotter", "Verifier"),
    hintsEnabled = true,
    accessibility = setOf("tabletop-mode", "large-labels"),
    arQuality = "balanced",
    networkMode = "local-wifi",
    contentSource = "teacher-pack",
    safetyBoundaryRequired = true
)

private fun sampleTournamentPreset(): TournamentPreset = TournamentPreset(
    presetId = "tournament-coordinate-cup",
    eventName = "Coordinate Cup",
    gameIds = listOf("coordinate_conquest_ar", "ar-math-arena"),
    eventType = TournamentEventType.MixedGameChampionship,
    rounds = 4,
    teamSize = 4,
    seeding = "skill-balanced",
    weights = TournamentScoreWeights(),
    tieBreakRules = listOf(TieBreakRule.HigherAccuracy, TieBreakRule.FewerHints),
    accommodations = setOf("tabletop-mode", "extended-time"),
    outdoorFinal = false,
    spectatorLimit = 20,
    replayEnabled = true
)

private fun samplePack(mission: AuthoredMission, campaign: AuthoredCampaign, preset: GamePreset, tournament: TournamentPreset): ContentPack = ContentPack(
    metadata = ContentPackMetadata(
        packId = "pack-coordinate-slope",
        title = "Coordinate Slope Starter Pack",
        version = 1,
        author = "Local Teacher",
        minAppVersion = "1.0",
        attribution = "School authored content",
        requiredCapabilities = setOf("markerless-ar", "tabletop-mode"),
        checksum = "generated"
    ),
    missions = listOf(mission),
    campaigns = listOf(campaign),
    gamePresets = listOf(preset),
    tournamentPresets = listOf(tournament),
    expeditionRoutes = emptyList(),
    assetReferences = listOf("local://coordinate-grid.png"),
    taxonomyAdditions = AdvancedCurriculumCatalog.skills.take(1)
)

private fun sampleSchoolProfile(): SchoolProfile = SchoolProfile(
    schoolDisplayName = "Local School",
    academicYear = "2026-2027",
    defaultGradeBands = setOf(GradeBand.Grade7, GradeBand.Grade8, GradeBand.Grade9),
    houseNames = listOf("Euler", "Noether", "Ramanujan", "Hypatia"),
    defaultSafetyRules = listOf("Clear floor boundary", "Tabletop fallback enabled"),
    defaultGameDurationMinutes = 12,
    accessibilityDefaults = setOf("large-labels", "non-ar-alternative"),
    approvalRequired = true,
    reportBrandingAsset = null
)

private fun sampleBackupPlan(): BackupPlan = BackupPlan(
    includeMissions = true,
    includeCampaigns = true,
    includePresets = true,
    includeRoutes = true,
    includePacks = true,
    includeSchoolSettings = true,
    sensitivity = BackupSensitivity.IncludeProgressSummaries
)

private fun sampleDevices(): List<DeviceReadiness> = listOf(
    DeviceReadiness("Device A", true, true, true, true, true, false, true, true, 82, 2048, "nominal", true, true, true, setOf("large-labels")),
    DeviceReadiness("Device B", true, false, true, false, true, false, true, true, 54, 1500, "warm", false, false, false, emptySet())
)

private fun sampleDiagnostics(): DiagnosticsPackage = DiagnosticsPackage(
    appVersion = "local-debug",
    androidVersion = "14",
    deviceModel = "AR test device",
    arSummary = "ARCore supported; depth optional",
    networkSummary = "Local Wi-Fi ready",
    recentNonSensitiveErrors = emptyList(),
    databaseSchemaVersion = AUTHORING_SCHEMA_VERSION,
    contentPackVersions = mapOf("pack-coordinate-slope" to 1),
    mapProviderMetadata = "offline-ready",
    rendererStatus = "stable"
)
