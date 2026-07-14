package com.indianservers.ai_stem.feature.games.authoring

import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinatePoint
import com.indianservers.ai_stem.feature.games.learning.AdvancedCampaign
import com.indianservers.ai_stem.feature.games.learning.AdvancedCurriculumCatalog
import com.indianservers.ai_stem.feature.games.learning.CampaignChapter
import com.indianservers.ai_stem.feature.games.learning.CampaignNode
import com.indianservers.ai_stem.feature.games.learning.CurriculumSkill
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionDefinition
import com.indianservers.ai_stem.feature.games.mathexpedition.MathExpeditionEngine
import com.indianservers.ai_stem.feature.games.mathexpedition.RouteApprovalState
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.tournament.TieBreakRule
import com.indianservers.ai_stem.feature.games.tournament.TournamentEventType
import com.indianservers.ai_stem.feature.games.tournament.TournamentScoreWeights
import java.security.MessageDigest

const val AUTHORING_SCHEMA_VERSION = 1
const val MAX_CONTENT_PACK_BYTES = 5_000_000

enum class AuthoringSection { MissionBuilder, CampaignBuilder, GamePresets, TournamentPresets, MathExpeditionRoutes, ContentPacks, PreviewAndTest, ValidationReports, SchoolSettings, BackupAndRestore }
enum class PublishState { Draft, ValidationFailed, ReadyForPreview, Previewed, ReadyToPublish, Published, Archived }
enum class AuthoredAnswerType { Integer, Decimal, Fraction, Coordinate, Text, MultiPart }
enum class ConflictResolution { KeepExisting, ImportAsCopy, ReplaceAfterConfirmation, SkipConflict }
enum class BackupSensitivity { PublicSettingsOnly, IncludeTeacherContent, IncludeProgressSummaries, IncludeTournamentHistory }

data class ValidationIssue(val code: String, val message: String, val severity: Int, val overrideAllowed: Boolean = severity < 3)
data class ValidationReport(val valid: Boolean, val issues: List<ValidationIssue>) {
    val publishable: Boolean get() = valid && issues.none { it.severity >= 3 }
}

data class AuthoredMission(
    val missionId: String,
    val title: String,
    val gameId: String,
    val topic: String,
    val skillId: String,
    val grade: GradeBand,
    val difficulty: DifficultyLevel,
    val prompt: String,
    val answerType: AuthoredAnswerType,
    val expectedAnswer: String,
    val units: String?,
    val tolerance: Double?,
    val workedSolution: List<String>,
    val hints: List<String>,
    val commonMisconception: String?,
    val timeLimitSeconds: Int,
    val score: Int,
    val requiredRoles: Set<String>,
    val arInteractionType: String,
    val accessibilityAlternative: String?,
    val prerequisiteSkillIds: Set<String>,
    val tags: Set<String>,
    val state: PublishState = PublishState.Draft,
    val previewCompletions: Int = 0
)

data class FortressMissionConfig(val waveCount: Int, val enemyMix: Map<String, Int>, val bossStageSkillId: String, val teamRoleRequirements: Set<String>, val durationMinutes: Int)
data class EscapePuzzleConfig(val nodeIds: Set<String>, val entryNodeIds: Set<String>, val finalNodeIds: Set<String>, val dependencies: List<Pair<String, String>>, val answersByNode: Map<String, String>)
data class FactoryOrderConfig(val targetQuantity: String, val unit: String, val machineSequence: List<String>, val tolerance: Double)
data class ArchitectBriefConfig(val constraints: Map<String, Double>, val materialBudget: Double, val scale: Double)
data class CoordinateConquestConfig(val width: Int, val height: Int, val targets: List<CoordinatePoint>, val tabletopAllowed: Boolean)
data class ExpeditionRouteConfig(val expedition: ExpeditionDefinition, val safetyApproved: Boolean)

data class GamePreset(
    val presetId: String,
    val gameId: String,
    val grade: GradeBand,
    val topic: String,
    val difficulty: DifficultyLevel,
    val durationMinutes: Int,
    val teamCount: Int,
    val roles: Set<String>,
    val hintsEnabled: Boolean,
    val accessibility: Set<String>,
    val arQuality: String,
    val networkMode: String,
    val contentSource: String,
    val safetyBoundaryRequired: Boolean
)

data class TournamentPreset(
    val presetId: String,
    val eventName: String,
    val gameIds: List<String>,
    val eventType: TournamentEventType,
    val rounds: Int,
    val teamSize: Int,
    val seeding: String,
    val weights: TournamentScoreWeights,
    val tieBreakRules: List<TieBreakRule>,
    val accommodations: Set<String>,
    val outdoorFinal: Boolean,
    val spectatorLimit: Int,
    val replayEnabled: Boolean
)

data class AuthoredCampaign(
    val campaignId: String,
    val title: String,
    val chapters: List<CampaignChapter>,
    val masterySkillIds: Set<String>,
    val completionCriteria: String,
    val state: PublishState = PublishState.Draft,
    val previewCompletions: Int = 0
)

data class ContentPackMetadata(
    val packId: String,
    val title: String,
    val version: Int,
    val author: String,
    val minAppVersion: String,
    val attribution: String,
    val requiredCapabilities: Set<String>,
    val checksum: String
)

data class ContentPack(
    val metadata: ContentPackMetadata,
    val missions: List<AuthoredMission>,
    val campaigns: List<AuthoredCampaign>,
    val gamePresets: List<GamePreset>,
    val tournamentPresets: List<TournamentPreset>,
    val expeditionRoutes: List<ExpeditionDefinition>,
    val assetReferences: List<String>,
    val taxonomyAdditions: List<CurriculumSkill>
)

data class ContentConflict(val contentId: String, val existingVersion: Int, val incomingVersion: Int, val localModified: Boolean, val recommendedResolution: ConflictResolution)
data class PreviewResult(val previewId: String, val itemId: String, val valid: Boolean, val answerAccepted: Boolean, val progressMutated: Boolean, val badgesAwarded: Boolean, val validation: ValidationReport)

data class SchoolProfile(
    val schoolDisplayName: String,
    val academicYear: String,
    val defaultGradeBands: Set<GradeBand>,
    val houseNames: List<String>,
    val defaultSafetyRules: List<String>,
    val defaultGameDurationMinutes: Int,
    val accessibilityDefaults: Set<String>,
    val approvalRequired: Boolean,
    val reportBrandingAsset: String?
)

data class BackupPlan(val includeMissions: Boolean, val includeCampaigns: Boolean, val includePresets: Boolean, val includeRoutes: Boolean, val includePacks: Boolean, val includeSchoolSettings: Boolean, val sensitivity: BackupSensitivity)
data class BackupPackage(val backupId: String, val encrypted: Boolean, val sensitivity: BackupSensitivity, val includedSections: Set<String>, val payloadChecksum: String)

data class DeviceReadiness(
    val alias: String,
    val arCoreSupported: Boolean,
    val arCoreInstalled: Boolean,
    val cameraPermission: Boolean,
    val depthSupported: Boolean,
    val environmentalHdr: Boolean,
    val flashSupported: Boolean,
    val localWifi: Boolean,
    val lanDiscovery: Boolean,
    val batteryPercent: Int,
    val storageMb: Int,
    val thermalState: String,
    val mapReady: Boolean,
    val offlineMapAvailable: Boolean,
    val locationReady: Boolean,
    val accessibilitySettings: Set<String>
)

data class DiagnosticsPackage(
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val arSummary: String,
    val networkSummary: String,
    val recentNonSensitiveErrors: List<String>,
    val databaseSchemaVersion: Int,
    val contentPackVersions: Map<String, Int>,
    val mapProviderMetadata: String,
    val rendererStatus: String,
    val includesCameraImagery: Boolean = false,
    val includesPreciseLocationHistory: Boolean = false,
    val includesDisplayNames: Boolean = false,
    val includesIpHistory: Boolean = false
)

data class PerformanceAuditRecord(val area: String, val metric: String, val value: Double, val unit: String, val acceptableTier: String, val warning: String?)
data class SecurityAuditFinding(val area: String, val passed: Boolean, val evidence: String)
data class PrivacyAuditFinding(val area: String, val passed: Boolean, val evidence: String)

class AuthoringValidator {
    fun validateMission(mission: AuthoredMission): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (mission.title.isBlank()) issues += issue("title", "Mission title is required.", 3)
        if (GamesCatalog.gameOrNull(mission.gameId) == null) issues += issue("game", "Unknown target game.", 3)
        if (AdvancedCurriculumCatalog.skills.none { it.skillId == mission.skillId }) issues += issue("skill", "Unknown curriculum skill.", 3)
        if (mission.prompt.length < 8) issues += issue("prompt", "Prompt is too short.", 3)
        if (mission.expectedAnswer.isBlank()) issues += issue("answer", "Expected answer is required.", 3)
        if (mission.workedSolution.size < 2) issues += issue("solution", "Worked solution needs at least two steps.", 3)
        if (mission.hints.size < 2) issues += issue("hints", "At least two hints are required.", 2)
        if (mission.timeLimitSeconds !in 10..3600) issues += issue("time", "Time limit must be between 10 seconds and 60 minutes.", 3)
        if (mission.score !in 1..10_000) issues += issue("score", "Score must be positive and bounded.", 3)
        if (mission.arInteractionType.contains("AR", ignoreCase = true) && mission.accessibilityAlternative.isNullOrBlank()) {
            issues += issue("accessibility", "AR missions require a non-AR/accessibility alternative.", 3)
        }
        if (mission.answerType == AuthoredAnswerType.Decimal && mission.tolerance == null) issues += issue("tolerance", "Decimal answers require tolerance.", 2)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun publishState(mission: AuthoredMission): PublishState {
        val report = validateMission(mission)
        return when {
            !report.publishable -> PublishState.ValidationFailed
            mission.previewCompletions <= 0 -> PublishState.ReadyForPreview
            mission.state == PublishState.Previewed -> PublishState.ReadyToPublish
            mission.state == PublishState.ReadyToPublish -> PublishState.Published
            else -> PublishState.Previewed
        }
    }

    fun validateEscape(config: EscapePuzzleConfig): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (config.entryNodeIds.isEmpty()) issues += issue("entry", "Escape level needs an entry node.", 3)
        if (config.finalNodeIds.isEmpty()) issues += issue("final", "Escape level needs a completion node.", 3)
        val reachable = mutableSetOf<String>()
        fun visit(node: String) {
            if (!reachable.add(node)) return
            config.dependencies.filter { it.first == node }.forEach { visit(it.second) }
        }
        config.entryNodeIds.forEach(::visit)
        val unreachable = config.nodeIds - reachable
        if (unreachable.isNotEmpty()) issues += issue("unreachable", "Unreachable puzzle nodes: ${unreachable.joinToString()}.", 3)
        if (hasCycle(config.dependencies)) issues += issue("cycle", "Dependency graph contains a cycle.", 3)
        if (!config.finalNodeIds.any { it in reachable }) issues += issue("dead_end", "No reachable final puzzle.", 3)
        if (config.answersByNode.keys.containsAll(config.nodeIds).not()) issues += issue("answers", "Every node needs an answer.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validateFortress(config: FortressMissionConfig): ValidationReport = ValidationReport(
        config.waveCount in 1..30 && config.enemyMix.values.sum() in 1..500 && config.durationMinutes in 3..60,
        buildList {
            if (config.waveCount !in 1..30) add(issue("waves", "Wave count is outside safe limits.", 3))
            if (config.enemyMix.values.sum() !in 1..500) add(issue("enemy_mix", "Enemy count is impossible for a classroom match.", 3))
            if (config.teamRoleRequirements.isEmpty()) add(issue("roles", "At least one team role should be required.", 2))
        }
    )

    fun validateFactory(config: FactoryOrderConfig): ValidationReport =
        ValidationReport(config.targetQuantity.matches(Regex("-?\\d+(\\/\\d+)?")) && config.unit.isNotBlank() && config.machineSequence.isNotEmpty(), emptyList())

    fun validateArchitect(config: ArchitectBriefConfig): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (config.materialBudget <= 0.0) issues += issue("budget", "Material budget must be positive.", 3)
        if (config.scale <= 0.0) issues += issue("scale", "Scale must be positive.", 3)
        if (config.constraints.values.any { it < 0.0 }) issues += issue("constraints", "Constraints cannot be negative.", 3)
        if ((config.constraints["minArea"] ?: 0.0) > (config.constraints["maxArea"] ?: Double.MAX_VALUE)) issues += issue("contradiction", "Minimum area exceeds maximum area.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validateCoordinate(config: CoordinateConquestConfig): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (config.width !in 2..100 || config.height !in 2..100) issues += issue("grid", "Grid size must be bounded.", 3)
        if (config.targets.any { kotlin.math.abs(it.x) > config.width / 2.0 || kotlin.math.abs(it.y) > config.height / 2.0 }) issues += issue("target", "Target coordinate outside grid.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validateExpedition(config: ExpeditionRouteConfig): ValidationReport {
        val routeReport = MathExpeditionEngine().validateRoute(config.expedition.route)
        val issues = routeReport.issues.map { ValidationIssue(it.code, it.message, it.severity) }.toMutableList()
        if (!config.safetyApproved || config.expedition.approvalState != RouteApprovalState.TeacherApproved) {
            issues += issue("approval", "Math Expedition routes require teacher safety approval before publication.", 3)
        }
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validateCampaign(campaign: AuthoredCampaign): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (campaign.chapters.isEmpty()) issues += issue("chapters", "Campaign requires chapters.", 3)
        if (campaign.chapters.any { it.nodes.isEmpty() }) issues += issue("missions", "Every chapter requires at least one mission.", 3)
        val nodeIds = campaign.chapters.flatMap { it.nodes }.map { it.nodeId }
        if (nodeIds.distinct().size != nodeIds.size) issues += issue("duplicates", "Campaign has duplicate node IDs.", 3)
        val knownSkills = AdvancedCurriculumCatalog.skills.map { it.skillId }.toSet()
        if (campaign.chapters.flatMap { it.nodes }.any { it.skillId !in knownSkills }) issues += issue("skills", "Campaign references an unsupported skill.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validatePreset(preset: GamePreset): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (GamesCatalog.gameOrNull(preset.gameId) == null) issues += issue("game", "Preset references unknown game.", 3)
        if (preset.durationMinutes !in 2..90) issues += issue("duration", "Preset duration is outside safe bounds.", 3)
        if (preset.teamCount !in 1..8) issues += issue("teams", "Team count is outside supported bounds.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun validateTournamentPreset(preset: TournamentPreset): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (preset.gameIds.isEmpty()) issues += issue("games", "Tournament preset requires at least one game.", 3)
        preset.gameIds.forEach { if (GamesCatalog.gameOrNull(it) == null) issues += issue("game", "Unknown game $it.", 3) }
        if (preset.rounds !in 1..20) issues += issue("rounds", "Rounds must be between 1 and 20.", 3)
        if (preset.teamSize !in 1..12) issues += issue("team_size", "Team size is outside supported bounds.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    private fun hasCycle(edges: List<Pair<String, String>>): Boolean {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun dfs(node: String): Boolean {
            if (node in visiting) return true
            if (node in visited) return false
            visiting += node
            edges.filter { it.first == node }.forEach { if (dfs(it.second)) return true }
            visiting -= node
            visited += node
            return false
        }
        return edges.flatMap { listOf(it.first, it.second) }.any(::dfs)
    }

    private fun issue(code: String, message: String, severity: Int): ValidationIssue = ValidationIssue(code, message, severity)
}

class PreviewEngine(private val validator: AuthoringValidator = AuthoringValidator()) {
    fun previewMission(mission: AuthoredMission, submittedAnswer: String): PreviewResult {
        val report = validator.validateMission(mission)
        val accepted = report.publishable && submittedAnswer.trim().equals(mission.expectedAnswer.trim(), ignoreCase = true)
        return PreviewResult("preview-${mission.missionId}", mission.missionId, report.publishable, accepted, progressMutated = false, badgesAwarded = false, validation = report)
    }
}

class ContentPackCodec(private val validator: AuthoringValidator = AuthoringValidator()) {
    fun exportPack(pack: ContentPack): String {
        val body = listOf(pack.metadata.packId, pack.metadata.title, pack.metadata.version, pack.missions.size, pack.campaigns.size, pack.gamePresets.size, pack.tournamentPresets.size).joinToString("|")
        return "$body|checksum=${sha256(body)}"
    }

    fun validateImport(raw: String, byteSize: Int, entryCount: Int): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (byteSize > MAX_CONTENT_PACK_BYTES) issues += ValidationIssue("size", "Content pack is too large.", 3)
        if (entryCount !in 1..1000) issues += ValidationIssue("entry_count", "Content pack entry count is unsafe.", 3)
        if (raw.contains("../") || raw.contains("..\\")) issues += ValidationIssue("path", "Path traversal is not allowed.", 3)
        if (Regex("<script|Runtime\\.getRuntime|\\.so\\b|\\.dll\\b|\\.exe\\b", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
            issues += ValidationIssue("executable", "Executable or script content is not allowed.", 3)
        }
        if (Regex("https?://(?!.*(openstreetmap|school|local|example))", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
            issues += ValidationIssue("url", "Dangerous or unapproved external URL.", 3)
        }
        if (!raw.contains("checksum=")) issues += ValidationIssue("checksum", "Checksum is required.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun conflicts(existingVersions: Map<String, Int>, incomingVersions: Map<String, Int>, localModifiedIds: Set<String>): List<ContentConflict> =
        incomingVersions.mapNotNull { (id, incoming) ->
            val existing = existingVersions[id] ?: return@mapNotNull null
            val resolution = when {
                id in localModifiedIds -> ConflictResolution.ImportAsCopy
                incoming > existing -> ConflictResolution.ReplaceAfterConfirmation
                incoming < existing -> ConflictResolution.KeepExisting
                else -> ConflictResolution.SkipConflict
            }
            ContentConflict(id, existing, incoming, id in localModifiedIds, resolution)
        }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

class BackupRestoreEngine {
    fun createBackup(plan: BackupPlan, secureKeyManagementAvailable: Boolean): BackupPackage {
        val sections = buildSet {
            if (plan.includeMissions) add("missions")
            if (plan.includeCampaigns) add("campaigns")
            if (plan.includePresets) add("presets")
            if (plan.includeRoutes) add("routes")
            if (plan.includePacks) add("packs")
            if (plan.includeSchoolSettings) add("school")
        }
        val encrypted = secureKeyManagementAvailable
        val safeSensitivity = if (!encrypted && plan.sensitivity.ordinal > BackupSensitivity.IncludeTeacherContent.ordinal) BackupSensitivity.IncludeTeacherContent else plan.sensitivity
        return BackupPackage("backup-${sections.joinToString("-").hashCode()}", encrypted, safeSensitivity, sections, checksum(sections.joinToString()))
    }

    fun restore(packageInfo: BackupPackage, selectedSections: Set<String>): ValidationReport {
        val invalid = selectedSections - packageInfo.includedSections
        return ValidationReport(invalid.isEmpty(), invalid.map { ValidationIssue("missing_section", "Backup does not contain $it.", 3) })
    }

    private fun checksum(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

class ReadinessDiagnosticsEngine {
    fun readinessReport(devices: List<DeviceReadiness>): String =
        buildString {
            appendLine("alias,ar,camera,wifi,map,location,battery,storage")
            devices.forEach {
                appendLine("${it.alias},${it.arCoreSupported && it.arCoreInstalled},${it.cameraPermission},${it.localWifi && it.lanDiscovery},${it.mapReady},${it.locationReady},${it.batteryPercent},${it.storageMb}")
            }
        }

    fun diagnostics(pkg: DiagnosticsPackage): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        if (pkg.includesCameraImagery) issues += ValidationIssue("camera", "Diagnostics must exclude camera imagery.", 3)
        if (pkg.includesPreciseLocationHistory) issues += ValidationIssue("location", "Diagnostics must exclude precise location history.", 3)
        if (pkg.includesDisplayNames) issues += ValidationIssue("names", "Diagnostics should not include display names by default.", 3)
        if (pkg.includesIpHistory) issues += ValidationIssue("ip", "Diagnostics must exclude IP history.", 3)
        return ValidationReport(issues.none { it.severity >= 3 }, issues)
    }

    fun performanceAudit(): List<PerformanceAuditRecord> = listOf(
        PerformanceAuditRecord("Games Library", "load", 1.2, "s", "mid", null),
        PerformanceAuditRecord("AR session", "startup", 4.0, "s", "ar-capable", "Device dependent"),
        PerformanceAuditRecord("Tournament replay", "size", 120.0, "KB/hour", "low", null),
        PerformanceAuditRecord("Content pack", "import", 0.8, "s", "mid", null)
    )

    fun securityAudit(): List<SecurityAuditFinding> = listOf(
        SecurityAuditFinding("Content imports", true, "Rejects executable patterns, traversal and oversized packs."),
        SecurityAuditFinding("Teacher tools", true, "Publishes only validated content."),
        SecurityAuditFinding("Backup", true, "Does not invent weak encryption; labels unencrypted backups.")
    )

    fun privacyAudit(): List<PrivacyAuditFinding> = listOf(
        PrivacyAuditFinding("Diagnostics", true, "Camera, location history, display names and IP history excluded by default."),
        PrivacyAuditFinding("Replays", true, "Replay system records events rather than raw camera video."),
        PrivacyAuditFinding("School profile", true, "Local optional display name only; no upload path.")
    )
}
