package com.indianservers.ai_stem.domain.scene

enum class ArWorkflowGoal {
    Scan,
    Place,
    Calibrate,
    Equation,
    Analyze,
    PickPoint,
    Measure,
    Construct,
    Persist,
    Export
}

data class ArWorkflowStep(
    val id: String,
    val title: String,
    val actionHint: String,
    val goal: ArWorkflowGoal,
    val successLabel: String
)

data class ArWorkflowTemplate(
    val id: String,
    val title: String,
    val description: String,
    val recommendedMode: String,
    val equation: String,
    val comparisonEquation: String,
    val steps: List<ArWorkflowStep>
)

data class ArWorkflowProgress(
    val templateId: String = "surface-masterclass",
    val currentStepIndex: Int = 0,
    val completedStepIds: Set<String> = emptySet()
) {
    fun currentStep(template: ArWorkflowTemplate): ArWorkflowStep? =
        template.steps.getOrNull(currentStepIndex.coerceIn(0, template.steps.lastIndex.coerceAtLeast(0)))
}

data class ArWorkflowSignal(
    val mode: String,
    val hasPlacedObject: Boolean,
    val placementScore: Int,
    val paperCalibrated: Boolean,
    val equationValid: Boolean,
    val hasAnalysis: Boolean,
    val pickedPointCount: Int,
    val rulerAnchorCount: Int,
    val constructionObjectCount: Int,
    val anchorSaved: Boolean,
    val exportReady: Boolean,
    val depthEnabled: Boolean
)

data class ArWorkflowEvaluation(
    val completionPercent: Int = 0,
    val readinessScore: Int = 0,
    val nextAction: String = "Choose an AR workflow.",
    val badges: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ArActivitySharePackage(
    val fileName: String,
    val mimeType: String,
    val payload: String,
    val summary: String
)

object ArWorkflowTemplates {
    val templates: List<ArWorkflowTemplate> = listOf(
        ArWorkflowTemplate(
            id = "surface-masterclass",
            title = "Surface Masterclass",
            description = "Build, analyze, measure, persist and share a markerless 3D graph.",
            recommendedMode = "Indoor",
            equation = "z = sin(x) * cos(y)",
            comparisonEquation = "y = x",
            steps = listOf(
                ArWorkflowStep("scan", "Scan a stable surface", "Move slowly until tracking confidence is good.", ArWorkflowGoal.Scan, "Surface tracking is ready"),
                ArWorkflowStep("place", "Place the graph", "Tap a plane and anchor the graph.", ArWorkflowGoal.Place, "Graph is anchored"),
                ArWorkflowStep("equation", "Edit the equation", "Use a live equation with parameters or a surface.", ArWorkflowGoal.Equation, "Equation is valid"),
                ArWorkflowStep("analyze", "Show roots/tangent/area", "Open graph analysis and move the tangent focus.", ArWorkflowGoal.Analyze, "Analysis is visible"),
                ArWorkflowStep("measure", "Add ruler anchors", "Place two AR ruler anchors for distance, angle and slope.", ArWorkflowGoal.Measure, "Measurement overlay is attached"),
                ArWorkflowStep("persist", "Save the AR anchor", "Capture a persistent anchor and production settings.", ArWorkflowGoal.Persist, "Scene can be restored"),
                ArWorkflowStep("export", "Export activity pack", "Create a shareable AR activity package.", ArWorkflowGoal.Export, "Activity pack is ready")
            )
        ),
        ArWorkflowTemplate(
            id = "paper-graph-to-3d",
            title = "Paper Graph to 3D",
            description = "Lock axes to paper, calibrate, lift the drawing into a 3D graph and slice it.",
            recommendedMode = "PaperGraph",
            equation = "z = x^2 - y^2",
            comparisonEquation = "y = 0",
            steps = listOf(
                ArWorkflowStep("scan-paper", "Lock worksheet", "Scan the printed graph/image target.", ArWorkflowGoal.Scan, "Paper image is locked"),
                ArWorkflowStep("calibrate", "Calibrate axes", "Tap origin, X-axis point and Y-axis point.", ArWorkflowGoal.Calibrate, "Graph coordinate system is calibrated"),
                ArWorkflowStep("equation", "Lift equation", "Choose surface, extrusion, revolution or cross-section mode.", ArWorkflowGoal.Equation, "3D graph expression is valid"),
                ArWorkflowStep("pick", "Pick graph points", "Tap the graph to create coordinate labels.", ArWorkflowGoal.PickPoint, "Graph points are labeled"),
                ArWorkflowStep("analyze", "Analyze slices", "Move cross-section and tangent controls.", ArWorkflowGoal.Analyze, "Slice/tangent analysis is visible"),
                ArWorkflowStep("export", "Export worksheet AR", "Package the paper-locked activity.", ArWorkflowGoal.Export, "Paper activity pack is ready")
            )
        ),
        ArWorkflowTemplate(
            id = "outdoor-building-lab",
            title = "Outdoor Building Lab",
            description = "Use real buildings for height, slope, volume, mesh quality and geospatial coordinate lessons.",
            recommendedMode = "OutdoorGeospatialMath",
            equation = "y = x",
            comparisonEquation = "y = 0",
            steps = listOf(
                ArWorkflowStep("scan-geo", "Find building mesh", "Scan outside in a VPS/Street View covered area.", ArWorkflowGoal.Scan, "Outdoor mesh is visible"),
                ArWorkflowStep("place", "Anchor to building", "Tap building or terrain geometry.", ArWorkflowGoal.Place, "Building anchor is placed"),
                ArWorkflowStep("measure", "Read measurements", "Show height, angle, slope, surface area and volume.", ArWorkflowGoal.Measure, "Building measurements are attached"),
                ArWorkflowStep("construct", "Add vectors/axes", "Create vectors, coordinate grid or construction lines.", ArWorkflowGoal.Construct, "Outdoor construction objects are linked"),
                ArWorkflowStep("persist", "Save geospatial anchor", "Persist the outdoor AR scene.", ArWorkflowGoal.Persist, "Geospatial restore hint is ready"),
                ArWorkflowStep("export", "Export outdoor pack", "Share the geospatial math activity.", ArWorkflowGoal.Export, "Outdoor activity pack is ready")
            )
        )
    )
}

class ArWorkflowEngine {
    fun evaluate(
        template: ArWorkflowTemplate,
        progress: ArWorkflowProgress,
        signal: ArWorkflowSignal
    ): ArWorkflowEvaluation {
        val autoCompleted = autoCompletedStepIds(template, signal)
        val completed = progress.completedStepIds + autoCompleted
        val completionPercent = if (template.steps.isEmpty()) 0 else (completed.size * 100 / template.steps.size).coerceIn(0, 100)
        val readiness = readinessScore(signal, completionPercent)
        val nextStep = template.steps.firstOrNull { it.id !in completed }
        return ArWorkflowEvaluation(
            completionPercent = completionPercent,
            readinessScore = readiness,
            nextAction = nextStep?.actionHint ?: "Workflow complete. Export or start another AR activity.",
            badges = buildBadges(signal, completed),
            warnings = buildWarnings(signal, template),
        )
    }

    fun advance(progress: ArWorkflowProgress, template: ArWorkflowTemplate): ArWorkflowProgress =
        progress.copy(currentStepIndex = (progress.currentStepIndex + 1).coerceAtMost(template.steps.lastIndex.coerceAtLeast(0)))

    fun completeCurrent(progress: ArWorkflowProgress, template: ArWorkflowTemplate): ArWorkflowProgress {
        val step = progress.currentStep(template) ?: return progress
        val nextIndex = (progress.currentStepIndex + 1).coerceAtMost(template.steps.lastIndex.coerceAtLeast(0))
        return progress.copy(
            currentStepIndex = nextIndex,
            completedStepIds = progress.completedStepIds + step.id
        )
    }

    fun exportActivity(
        template: ArWorkflowTemplate,
        progress: ArWorkflowProgress,
        evaluation: ArWorkflowEvaluation,
        scenePackage: ArSceneSharePackage?
    ): ArActivitySharePackage {
        val payload = buildString {
            append("{")
            append("\"schema\":1,")
            append("\"activityId\":\"${template.id.escapeJson()}\",")
            append("\"title\":\"${template.title.escapeJson()}\",")
            append("\"mode\":\"${template.recommendedMode.escapeJson()}\",")
            append("\"equation\":\"${template.equation.escapeJson()}\",")
            append("\"completion\":${evaluation.completionPercent},")
            append("\"readiness\":${evaluation.readinessScore},")
            append("\"completedSteps\":[${progress.completedStepIds.joinToString(",") { "\"${it.escapeJson()}\"" }}],")
            append("\"scenePackage\":\"${scenePackage?.fileName?.escapeJson().orEmpty()}\"")
            append("}")
        }
        return ArActivitySharePackage(
            fileName = "${template.title.sanitizedFileName()}.aistem-activity.json",
            mimeType = "application/vnd.aistem.activity+json",
            payload = payload,
            summary = "${template.title}: ${evaluation.completionPercent}% complete, readiness ${evaluation.readinessScore}%"
        )
    }

    private fun autoCompletedStepIds(template: ArWorkflowTemplate, signal: ArWorkflowSignal): Set<String> =
        template.steps.filter { step ->
            when (step.goal) {
                ArWorkflowGoal.Scan -> signal.placementScore >= 55 || signal.paperCalibrated || signal.mode == "OutdoorGeospatialMath"
                ArWorkflowGoal.Place -> signal.hasPlacedObject
                ArWorkflowGoal.Calibrate -> signal.paperCalibrated
                ArWorkflowGoal.Equation -> signal.equationValid
                ArWorkflowGoal.Analyze -> signal.hasAnalysis
                ArWorkflowGoal.PickPoint -> signal.pickedPointCount > 0
                ArWorkflowGoal.Measure -> signal.rulerAnchorCount >= 2
                ArWorkflowGoal.Construct -> signal.constructionObjectCount > 0
                ArWorkflowGoal.Persist -> signal.anchorSaved
                ArWorkflowGoal.Export -> signal.exportReady
            }
        }.map { it.id }.toSet()

    private fun readinessScore(signal: ArWorkflowSignal, completionPercent: Int): Int {
        val tracking = signal.placementScore.coerceIn(0, 100)
        val equation = if (signal.equationValid) 15 else 0
        val depth = if (signal.depthEnabled) 8 else 0
        val anchor = if (signal.anchorSaved) 12 else 0
        return ((tracking * 0.45f) + (completionPercent * 0.35f) + equation + depth + anchor).toInt().coerceIn(0, 100)
    }

    private fun buildBadges(signal: ArWorkflowSignal, completed: Set<String>): List<String> = buildList {
        if (signal.equationValid) add("Equation ready")
        if (signal.hasAnalysis) add("Analysis active")
        if (signal.rulerAnchorCount >= 2) add("Measured")
        if (signal.constructionObjectCount > 0) add("Constructed")
        if (signal.anchorSaved) add("Persistent")
        if (signal.exportReady) add("Shareable")
        if (completed.size >= 5) add("AR workflow strong")
    }

    private fun buildWarnings(signal: ArWorkflowSignal, template: ArWorkflowTemplate): List<String> = buildList {
        if (signal.placementScore < 45) add("Tracking confidence is low. Move slower and scan textured surfaces.")
        if (template.recommendedMode == "PaperGraph" && !signal.paperCalibrated) add("Paper axes are not calibrated yet.")
        if (!signal.equationValid) add("Equation needs a valid parser result before export.")
        if (!signal.anchorSaved) add("Save an anchor before sharing a restorable AR activity.")
    }
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
private fun String.sanitizedFileName(): String =
    replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "ar-activity" }.take(48)
