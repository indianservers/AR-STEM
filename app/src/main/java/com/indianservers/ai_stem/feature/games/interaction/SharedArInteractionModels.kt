package com.indianservers.ai_stem.feature.games.interaction

import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

enum class ArGestureAction { GrabMove, Rotate, Scale, LiftHeight, StretchWidth, StretchDepth, LockToggle, Duplicate, Reset }
enum class ArInteractionAxis { X, Y, Z, Uniform }
enum class ArGameContext { Indoor, Surface, PaperGraph, OutdoorGeo, Air }
enum class CalibrationWizardStep { ChooseMode, FindSurfaceOrMarker, SetScale, SetAxes, SafetyBoundary, QualityCheck, Ready }
enum class SnapTargetKind { GridLine, Axis, Plane, PaperAxis, BuildingEdge, ObjectVertex, RouteCheckpoint, FunctionPoint }
enum class MissionTemplateKind { Measure, Solve, Build, Compare, Defend, Route, Transform, Estimate }
enum class CompactHudTone { Neutral, Success, Warning, Error }

data class ArObjectState(
    val objectId: String,
    val gameId: String,
    val label: String,
    val transform: SharedTransform,
    val locked: Boolean = false,
    val dimensionsMetres: Map<String, Float> = emptyMap(),
    val formulas: List<String> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
    val dependencies: Set<String> = emptySet(),
    val hints: List<String> = emptyList()
)

data class ArGestureCommand(
    val action: ArGestureAction,
    val axis: ArInteractionAxis = ArInteractionAxis.Uniform,
    val translationMetres: Vector3Dto = Vector3Dto(0f, 0f, 0f),
    val rotationDegrees: Float = 0f,
    val scaleFactor: Float = 1f
)

data class ArGestureResult(
    val objectState: ArObjectState,
    val duplicatedObject: ArObjectState? = null,
    val changed: Boolean,
    val message: String
)

class SharedArGestureEngine {
    fun apply(objectState: ArObjectState, command: ArGestureCommand): ArGestureResult {
        if (objectState.locked && command.action !in setOf(ArGestureAction.LockToggle, ArGestureAction.Duplicate)) {
            return ArGestureResult(objectState, changed = false, message = "${objectState.label} is locked.")
        }
        return when (command.action) {
            ArGestureAction.GrabMove -> update(objectState, objectState.transform.copy(positionMetres = objectState.transform.positionMetres + command.translationMetres), "Moved")
            ArGestureAction.Rotate -> update(objectState, objectState.transform.copy(rotation = yaw(command.rotationDegrees)), "Rotated ${command.rotationDegrees} degrees")
            ArGestureAction.Scale -> update(objectState, objectState.transform.copy(scale = objectState.transform.scale * command.scaleFactor.coerceIn(0.1f, 10f)), "Scaled")
            ArGestureAction.LiftHeight -> update(objectState, objectState.transform.copy(positionMetres = objectState.transform.positionMetres + Vector3Dto(0f, command.translationMetres.y, 0f)), "Lifted")
            ArGestureAction.StretchWidth -> update(objectState, objectState.transform.copy(scale = objectState.transform.scale.copy(x = (objectState.transform.scale.x * command.scaleFactor).coerceIn(0.1f, 10f))), "Width stretched")
            ArGestureAction.StretchDepth -> update(objectState, objectState.transform.copy(scale = objectState.transform.scale.copy(z = (objectState.transform.scale.z * command.scaleFactor).coerceIn(0.1f, 10f))), "Depth stretched")
            ArGestureAction.LockToggle -> ArGestureResult(objectState.copy(locked = !objectState.locked), changed = true, message = if (objectState.locked) "Unlocked" else "Locked")
            ArGestureAction.Duplicate -> ArGestureResult(
                objectState,
                duplicatedObject = objectState.copy(
                    objectId = "${objectState.objectId}-copy",
                    transform = objectState.transform.copy(positionMetres = objectState.transform.positionMetres + Vector3Dto(0.25f, 0f, 0.25f)),
                    locked = false
                ),
                changed = true,
                message = "Duplicated"
            )
            ArGestureAction.Reset -> update(objectState, objectState.transform.copy(rotation = QuaternionDto(0f, 0f, 0f, 1f), scale = Vector3Dto(1f, 1f, 1f)), "Reset rotation and scale")
        }
    }

    private fun update(objectState: ArObjectState, transform: SharedTransform, message: String): ArGestureResult =
        ArGestureResult(objectState.copy(transform = transform), changed = true, message = message)

    private fun yaw(degrees: Float): QuaternionDto {
        val radians = Math.toRadians(degrees.toDouble() / 2.0)
        return QuaternionDto(0f, kotlin.math.sin(radians).toFloat(), 0f, kotlin.math.cos(radians).toFloat())
    }
}

data class CalibrationWizardInput(
    val context: ArGameContext,
    val surfaceFound: Boolean = false,
    val imageLocked: Boolean = false,
    val buildingMeshFound: Boolean = false,
    val scaleReferenceMetres: Float? = null,
    val axesConfirmed: Boolean = false,
    val safetyBoundarySet: Boolean = false,
    val trackingScore: Int = 0,
    val lightScore: Int = 0,
    val locationReady: Boolean = false
)

data class CalibrationWizardState(
    val step: CalibrationWizardStep,
    val progress: Float,
    val canStartGame: Boolean,
    val instruction: String,
    val checklist: List<String>
)

class UniversalCalibrationWizard {
    fun evaluate(input: CalibrationWizardInput): CalibrationWizardState {
        val anchorReady = when (input.context) {
            ArGameContext.Indoor, ArGameContext.Surface, ArGameContext.Air -> input.surfaceFound || input.imageLocked
            ArGameContext.PaperGraph -> input.imageLocked
            ArGameContext.OutdoorGeo -> input.buildingMeshFound && input.locationReady
        }
        val scaleReady = input.scaleReferenceMetres != null && input.scaleReferenceMetres in 0.05f..1000f
        val qualityReady = input.trackingScore >= 70 && input.lightScore >= 55
        val safetyNeeded = input.context != ArGameContext.PaperGraph
        val safetyReady = !safetyNeeded || input.safetyBoundarySet
        val checks = listOf(anchorReady, scaleReady, input.axesConfirmed, safetyReady, qualityReady)
        val progress = checks.count { it } / checks.size.toFloat()
        val step = when {
            !anchorReady -> CalibrationWizardStep.FindSurfaceOrMarker
            !scaleReady -> CalibrationWizardStep.SetScale
            !input.axesConfirmed -> CalibrationWizardStep.SetAxes
            !safetyReady -> CalibrationWizardStep.SafetyBoundary
            !qualityReady -> CalibrationWizardStep.QualityCheck
            else -> CalibrationWizardStep.Ready
        }
        return CalibrationWizardState(
            step = step,
            progress = progress,
            canStartGame = step == CalibrationWizardStep.Ready,
            instruction = instructionFor(step, input.context),
            checklist = listOf(
                "Anchor: ${if (anchorReady) "ready" else "needed"}",
                "Scale: ${if (scaleReady) "ready" else "needed"}",
                "Axes: ${if (input.axesConfirmed) "ready" else "needed"}",
                "Safety: ${if (safetyReady) "ready" else "needed"}",
                "Quality: ${if (qualityReady) "ready" else "needed"}"
            )
        )
    }

    private fun instructionFor(step: CalibrationWizardStep, context: ArGameContext): String = when (step) {
        CalibrationWizardStep.ChooseMode -> "Choose the AR mode for this activity."
        CalibrationWizardStep.FindSurfaceOrMarker -> when (context) {
            ArGameContext.PaperGraph -> "Point at the worksheet until the paper graph locks."
            ArGameContext.OutdoorGeo -> "Scan buildings or terrain until a mesh is found."
            else -> "Move slowly until a surface or marker is found."
        }
        CalibrationWizardStep.SetScale -> "Set a real-world scale reference."
        CalibrationWizardStep.SetAxes -> "Confirm origin, X axis and Y/Z direction."
        CalibrationWizardStep.SafetyBoundary -> "Mark the safe play boundary."
        CalibrationWizardStep.QualityCheck -> "Improve tracking or lighting before play."
        CalibrationWizardStep.Ready -> "Ready to start."
    }
}

data class ArObjectInspection(
    val objectId: String,
    val title: String,
    val formulaSummary: String,
    val measurementSummary: String,
    val dependencySummary: String,
    val hints: List<String>,
    val warnings: List<String>
)

class ArObjectInspectorEngine {
    fun inspect(objectState: ArObjectState): ArObjectInspection {
        val formulas = objectState.formulas.takeIf { it.isNotEmpty() }?.joinToString() ?: "No formula attached"
        val measurements = objectState.dimensionsMetres.entries.joinToString { "${it.key}=${"%.2f".format(it.value)}m" }.ifBlank { "No dimensions attached" }
        val warnings = buildList {
            if (objectState.dependencies.any { it == objectState.objectId }) add("Object cannot depend on itself.")
            if (objectState.formulas.isEmpty() && objectState.dimensionsMetres.isEmpty()) add("Add a formula or measurement for full inspection.")
        }
        return ArObjectInspection(
            objectId = objectState.objectId,
            title = objectState.label,
            formulaSummary = formulas,
            measurementSummary = measurements,
            dependencySummary = if (objectState.dependencies.isEmpty()) "Independent object" else "Depends on ${objectState.dependencies.joinToString()}",
            hints = objectState.hints,
            warnings = warnings
        )
    }
}

data class SnapCandidate(
    val candidateId: String,
    val kind: SnapTargetKind,
    val positionMetres: Vector3Dto,
    val priority: Int,
    val label: String
)

data class SnapRequest(
    val positionMetres: Vector3Dto,
    val toleranceMetres: Float,
    val enabledKinds: Set<SnapTargetKind> = SnapTargetKind.entries.toSet(),
    val gridStepMetres: Float? = null
)

data class SnapResult(
    val snapped: Boolean,
    val positionMetres: Vector3Dto,
    val target: SnapCandidate?,
    val message: String
)

class SmartArSnapEngine {
    fun snap(request: SnapRequest, candidates: List<SnapCandidate>): SnapResult {
        val allowed = candidates.filter { it.kind in request.enabledKinds }
        val nearest = allowed
            .map { it to distance(request.positionMetres, it.positionMetres) }
            .filter { it.second <= request.toleranceMetres }
            .sortedWith(compareBy<Pair<SnapCandidate, Float>> { -it.first.priority }.thenBy { it.second })
            .firstOrNull()
        if (nearest != null) {
            return SnapResult(true, nearest.first.positionMetres, nearest.first, "Snapped to ${nearest.first.label}")
        }
        val grid = request.gridStepMetres
        if (grid != null && grid > 0f && SnapTargetKind.GridLine in request.enabledKinds) {
            val snapped = Vector3Dto(round(request.positionMetres.x / grid) * grid, round(request.positionMetres.y / grid) * grid, round(request.positionMetres.z / grid) * grid)
            if (distance(request.positionMetres, snapped) <= request.toleranceMetres) {
                return SnapResult(true, snapped, SnapCandidate("grid", SnapTargetKind.GridLine, snapped, 0, "grid"), "Snapped to grid")
            }
        }
        return SnapResult(false, request.positionMetres, null, "No snap target in range")
    }

    fun candidatesForObject(objectState: ArObjectState): List<SnapCandidate> {
        val p = objectState.transform.positionMetres
        return listOf(
            SnapCandidate("${objectState.objectId}-origin", SnapTargetKind.ObjectVertex, p, 4, "${objectState.label} origin"),
            SnapCandidate("${objectState.objectId}-x-axis", SnapTargetKind.Axis, Vector3Dto(p.x, 0f, 0f), 2, "${objectState.label} X axis"),
            SnapCandidate("${objectState.objectId}-plane", SnapTargetKind.Plane, Vector3Dto(p.x, 0f, p.z), 1, "${objectState.label} floor plane")
        )
    }

    private fun distance(a: Vector3Dto, b: Vector3Dto): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class CrossGameMissionTemplate(
    val templateId: String,
    val kind: MissionTemplateKind,
    val title: String,
    val supportedGameIds: Set<String>,
    val topic: String,
    val grade: GradeBand,
    val difficulty: DifficultyLevel,
    val arActions: List<ArGestureAction>,
    val requiredSnapKinds: Set<SnapTargetKind>,
    val promptPattern: String,
    val expectedEvidence: List<String>
)

data class InstantiatedMissionPlan(
    val planId: String,
    val gameId: String,
    val title: String,
    val prompt: String,
    val arActions: List<ArGestureAction>,
    val inspectionFields: List<String>,
    val validationChecks: List<String>
)

object CrossGameMissionTemplateRegistry {
    val templates: List<CrossGameMissionTemplate> = listOf(
        template("measure-height", MissionTemplateKind.Measure, "Measure A Real Object", "Measurement", listOf("geometry_architect_ar", "math_expedition_ar", "ar-math-arena"), listOf(ArGestureAction.GrabMove, ArGestureAction.LiftHeight), setOf(SnapTargetKind.Plane, SnapTargetKind.BuildingEdge), "Measure {object} and explain the units."),
        template("solve-lock", MissionTemplateKind.Solve, "Solve To Unlock", "Algebra", listOf("equation_escape_ar", "ar-math-arena"), listOf(ArGestureAction.GrabMove, ArGestureAction.LockToggle), setOf(SnapTargetKind.ObjectVertex), "Solve {equation} to unlock the AR object."),
        template("build-solid", MissionTemplateKind.Build, "Build A Solid", "Geometry", listOf("geometry_architect_ar", "fraction_factory_ar", "ar-math-arena"), listOf(ArGestureAction.Scale, ArGestureAction.StretchWidth, ArGestureAction.StretchDepth), setOf(SnapTargetKind.GridLine, SnapTargetKind.Plane), "Build a {solid} with the requested dimensions."),
        template("compare-models", MissionTemplateKind.Compare, "Compare Two Models", "Reasoning", listOf("coordinate_conquest_ar", "geometry_architect_ar", "math_expedition_ar"), listOf(ArGestureAction.Duplicate, ArGestureAction.Scale), setOf(SnapTargetKind.Axis, SnapTargetKind.GridLine), "Compare {a} and {b} using a visible AR model."),
        template("defend-zone", MissionTemplateKind.Defend, "Defend A Math Zone", "Arithmetic", listOf("ar-math-arena", "coordinate_conquest_ar"), listOf(ArGestureAction.GrabMove, ArGestureAction.Rotate), setOf(SnapTargetKind.GridLine, SnapTargetKind.FunctionPoint), "Place defences only where the math rule is true."),
        template("route-checkpoint", MissionTemplateKind.Route, "Route Checkpoint", "Route mathematics", listOf("math_expedition_ar", "coordinate_conquest_ar"), listOf(ArGestureAction.GrabMove), setOf(SnapTargetKind.RouteCheckpoint), "Navigate to {checkpoint} and justify distance or bearing."),
        template("transform-shape", MissionTemplateKind.Transform, "Transform A Shape", "Transformations", listOf("coordinate_conquest_ar", "geometry_architect_ar"), listOf(ArGestureAction.Rotate, ArGestureAction.Scale, ArGestureAction.GrabMove), setOf(SnapTargetKind.Axis, SnapTargetKind.GridLine), "Transform {shape} and compare before/after coordinates."),
        template("estimate-volume", MissionTemplateKind.Estimate, "Estimate Volume", "Mensuration", listOf("geometry_architect_ar", "math_expedition_ar", "fraction_factory_ar"), listOf(ArGestureAction.Scale, ArGestureAction.StretchDepth), setOf(SnapTargetKind.Plane, SnapTargetKind.BuildingEdge), "Estimate volume using visible dimensions.")
    )

    fun templatesForGame(gameId: String): List<CrossGameMissionTemplate> =
        templates.filter { gameId in it.supportedGameIds }

    fun instantiate(templateId: String, gameId: String, variables: Map<String, String>): InstantiatedMissionPlan {
        require(GamesCatalog.gameOrNull(gameId) != null) { "Unknown game ID." }
        val template = templates.first { it.templateId == templateId && gameId in it.supportedGameIds }
        val prompt = variables.entries.fold(template.promptPattern) { text, (key, value) -> text.replace("{$key}", value) }
        require(!prompt.contains(Regex("\\{[a-zA-Z]+}"))) { "Template variables are missing." }
        return InstantiatedMissionPlan(
            planId = "$gameId-${template.templateId}-${abs(prompt.hashCode())}",
            gameId = gameId,
            title = template.title,
            prompt = prompt,
            arActions = template.arActions,
            inspectionFields = listOf("Formula", "Dimensions", "Dependencies", "Hints"),
            validationChecks = template.expectedEvidence + template.requiredSnapKinds.map { "snap:$it" }
        )
    }

    private fun template(
        id: String,
        kind: MissionTemplateKind,
        title: String,
        topic: String,
        gameIds: List<String>,
        actions: List<ArGestureAction>,
        snapKinds: Set<SnapTargetKind>,
        prompt: String
    ): CrossGameMissionTemplate = CrossGameMissionTemplate(
        templateId = id,
        kind = kind,
        title = title,
        supportedGameIds = gameIds.toSet(),
        topic = topic,
        grade = GradeBand.Mixed,
        difficulty = DifficultyLevel.Adaptive,
        arActions = actions,
        requiredSnapKinds = snapKinds,
        promptPattern = prompt,
        expectedEvidence = listOf("answer", "work", "ar-placement")
    )
}

object TopArEnhancementRegistry {
    val labels: List<String> = listOf(
        "Shared AR Interaction Layer",
        "Universal AR Calibration Wizard",
        "AR Object Inspector",
        "Smart Snap System",
        "Cross-Game Mission Templates"
    )
}

data class CompactArHudInput(
    val surfaceStatus: String,
    val primaryAction: String,
    val trackingStatus: String,
    val warnings: List<String> = emptyList(),
    val details: List<String> = emptyList(),
    val expanded: Boolean = false
)

data class CompactArHudState(
    val title: String,
    val chips: List<String>,
    val primaryAction: String,
    val tone: CompactHudTone,
    val hiddenDetailCount: Int
)

object CompactArHudReducer {
    fun reduce(input: CompactArHudInput): CompactArHudState {
        val tone = when {
            input.warnings.any { it.contains("lost", ignoreCase = true) || it.contains("error", ignoreCase = true) } -> CompactHudTone.Error
            input.warnings.isNotEmpty() -> CompactHudTone.Warning
            input.surfaceStatus.contains("ready", ignoreCase = true) || input.surfaceStatus.contains("locked", ignoreCase = true) -> CompactHudTone.Success
            else -> CompactHudTone.Neutral
        }
        val compact = buildList {
            add(input.surfaceStatus)
            add(input.trackingStatus)
            input.warnings.firstOrNull()?.let(::add)
            if (input.expanded) addAll(input.details.take(3))
        }.distinct().take(if (input.expanded) 6 else 3)
        val hidden = (input.warnings.drop(1) + input.details).size - if (input.expanded) input.details.take(3).size else 0
        return CompactArHudState(
            title = when (tone) {
                CompactHudTone.Success -> "AR Ready"
                CompactHudTone.Warning -> "Check AR"
                CompactHudTone.Error -> "AR Paused"
                CompactHudTone.Neutral -> "Scanning"
            },
            chips = compact,
            primaryAction = input.primaryAction,
            tone = tone,
            hiddenDetailCount = hidden.coerceAtLeast(0)
        )
    }
}
