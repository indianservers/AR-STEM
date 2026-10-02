package com.indianservers.ai_stem.feature.graphing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.ai_stem.data.graph.CsvParseResult
import com.indianservers.ai_stem.data.graph.GraphCsvCodec
import com.indianservers.ai_stem.data.graph.GraphPngExportOptions
import com.indianservers.ai_stem.data.graph.GraphPngExporter
import com.indianservers.ai_stem.domain.graph.GraphAnalysisEngine
import com.indianservers.ai_stem.domain.graph.GraphCurve
import com.indianservers.ai_stem.domain.graph.GraphExpression
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphEquationInsight
import com.indianservers.ai_stem.domain.graph.GraphFeaturePoint
import com.indianservers.ai_stem.domain.graph.GraphInsightsEngine
import com.indianservers.ai_stem.domain.graph.GraphProject
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitiveBuilder
import com.indianservers.ai_stem.domain.graph.GraphRenderMode
import com.indianservers.ai_stem.domain.graph.GraphSampler
import com.indianservers.ai_stem.domain.graph.GraphSamples
import com.indianservers.ai_stem.domain.graph.GraphStyle
import com.indianservers.ai_stem.domain.graph.GraphSlider
import com.indianservers.ai_stem.domain.graph.ParseOutcome
import com.indianservers.ai_stem.domain.graph.GraphViewport
import com.indianservers.ai_stem.data.workspace.WorkspacePreferencesRepository
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GraphingExperienceMode { Beginner, Learn, Advanced }

data class GraphPickCandidate(
    val id: String,
    val name: String,
    val type: String,
    val groupName: String?,
    val visible: Boolean,
    val locked: Boolean,
    val distancePx: Double
)

data class GraphingUiState(
    val project: GraphProject = GraphSamples.starterProject(),
    val renderMode: GraphRenderMode = GraphRenderMode.Screen,
    val expressionText: String = "y = x^2",
    val workspaceMode: WorkspaceEnvironmentMode = WorkspaceEnvironmentMode.White,
    val experienceMode: GraphingExperienceMode = GraphingExperienceMode.Beginner,
    val beginnerMode: Boolean = true,
    val curves: List<GraphCurve> = emptyList(),
    val renderPrimitives: List<GraphRenderPrimitive> = emptyList(),
    val selectedExpressionId: String? = null,
    val pickCandidates: List<GraphPickCandidate> = emptyList(),
    val showObjectChooser: Boolean = false,
    val traceX: Double = 0.0,
    val showGrid: Boolean = true,
    val showLabels: Boolean = true,
    val traceEnabled: Boolean = false,
    val equationInsights: List<GraphEquationInsight> = emptyList(),
    val intersections: List<GraphFeaturePoint> = emptyList(),
    val analysisSummary: String = "Add a graph to begin.",
    val updating: Boolean = false,
    val userMessage: String? = null
)

class GraphingViewModel(application: Application) : AndroidViewModel(application) {
    private val sampler = GraphSampler()
    private val analysis = GraphAnalysisEngine()
    private val primitiveBuilder = GraphRenderPrimitiveBuilder()
    private val csvCodec = GraphCsvCodec()
    private val pngExporter = GraphPngExporter()
    private val insightsEngine = GraphInsightsEngine()
    private val preferences = WorkspacePreferencesRepository(application)
    private var computeJob: Job? = null
    private val _uiState = MutableStateFlow(GraphingUiState())
    val uiState: StateFlow<GraphingUiState> = _uiState

    init {
        viewModelScope.launch {
            preferences.environmentMode.collect { mode ->
                if (mode != WorkspaceEnvironmentMode.AR) {
                    _uiState.update { it.copy(workspaceMode = mode, renderMode = GraphRenderMode.Screen) }
                }
            }
        }
        recompute()
    }

    fun setWorkspaceMode(mode: WorkspaceEnvironmentMode) {
        if (mode == WorkspaceEnvironmentMode.AR) return
        _uiState.update { it.copy(workspaceMode = mode, renderMode = GraphRenderMode.Screen) }
        viewModelScope.launch { preferences.setEnvironmentMode(mode) }
    }

    fun setExpressionText(value: String) {
        _uiState.update { it.copy(expressionText = value) }
    }

    fun appendToken(token: String) {
        _uiState.update { it.copy(expressionText = it.expressionText + token) }
    }

    fun backspace() {
        _uiState.update { it.copy(expressionText = it.expressionText.dropLast(1)) }
    }

    fun setRenderMode(mode: GraphRenderMode) {
        _uiState.update {
            it.copy(
                renderMode = mode,
                userMessage = if (mode == GraphRenderMode.AR) "AR graph placement uses the AR Playground foundation and requires an ARCore device." else null
            )
        }
    }

    fun onArPermissionDenied() {
        _uiState.update {
            it.copy(
                renderMode = GraphRenderMode.Screen,
                userMessage = "Camera permission is needed only for AR. Screen graphing still works."
            )
        }
    }

    fun setBeginnerMode(beginner: Boolean) {
        _uiState.update {
            it.copy(
                beginnerMode = beginner,
                experienceMode = if (beginner) GraphingExperienceMode.Beginner else GraphingExperienceMode.Advanced
            )
        }
    }

    fun setExperienceMode(mode: GraphingExperienceMode) {
        _uiState.update {
            it.copy(
                experienceMode = mode,
                beginnerMode = mode == GraphingExperienceMode.Beginner,
                userMessage = when (mode) {
                    GraphingExperienceMode.Beginner -> "Beginner mode"
                    GraphingExperienceMode.Learn -> "Learn mode shows explanations and method notes."
                    GraphingExperienceMode.Advanced -> "Advanced mode"
                }
            )
        }
    }

    fun addExpression(kind: GraphExpressionKind = inferKind(_uiState.value.expressionText)) {
        val parsed = sampler.parseExpression(
            GraphExpression(
                id = "expr-${System.currentTimeMillis()}",
                kind = kind,
                source = _uiState.value.expressionText,
                displayName = defaultName(kind),
                style = GraphStyle(color = palette[_uiState.value.project.expressions.size % palette.size])
            )
        )
        if (parsed.parse is ParseOutcome.Failure) {
            _uiState.update { it.copy(userMessage = parsed.parse.message) }
            return
        }
        _uiState.update {
            val newSliders = detectedSliders(parsed, it.project.sliders)
            it.copy(
                project = it.project.copy(
                    expressions = it.project.expressions + parsed,
                    sliders = it.project.sliders + newSliders,
                    modifiedAt = System.currentTimeMillis()
                ),
                selectedExpressionId = parsed.id,
                userMessage = "Graph added"
            )
        }
        recompute()
    }

    fun applyTemplate(expression: GraphExpression) {
        _uiState.update { it.copy(expressionText = expression.source) }
    }

    fun deleteExpression(id: String) {
        _uiState.update {
            it.copy(project = it.project.copy(expressions = it.project.expressions.filterNot { expr -> expr.id == id }))
        }
        recompute()
    }

    fun duplicateExpression(id: String) {
        val source = _uiState.value.project.expressions.firstOrNull { it.id == id } ?: return
        val copy = source.copy(
            id = "expr-${System.currentTimeMillis()}",
            displayName = "${source.displayName} copy",
            style = source.style.copy(color = palette[_uiState.value.project.expressions.size % palette.size])
        )
        _uiState.update { it.copy(project = it.project.copy(expressions = it.project.expressions + copy), selectedExpressionId = copy.id) }
        recompute()
    }

    fun editExpression(id: String) {
        val expression = _uiState.value.project.expressions.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(selectedExpressionId = id, expressionText = expression.source) }
    }

    fun replaceSelectedExpression() {
        val id = _uiState.value.selectedExpressionId ?: return addExpression()
        val current = _uiState.value.project.expressions.firstOrNull { it.id == id } ?: return addExpression()
        val parsed = sampler.parseExpression(current.copy(source = _uiState.value.expressionText))
        if (parsed.parse is ParseOutcome.Failure) {
            _uiState.update { it.copy(userMessage = parsed.parse.message) }
            return
        }
        _uiState.update { state ->
            state.copy(project = state.project.copy(expressions = state.project.expressions.map { if (it.id == id) parsed else it }))
        }
        recompute()
    }

    fun toggleExpression(id: String) {
        _uiState.update {
            it.copy(project = it.project.copy(expressions = it.project.expressions.map { expr ->
                if (expr.id == id) expr.copy(visible = !expr.visible) else expr
            }))
        }
        recompute()
    }

    fun selectExpressionFromGraph(expressionId: String, overlapCount: Int) {
        val expression = _uiState.value.project.expressions.firstOrNull { it.id == expressionId }
        _uiState.update {
            it.copy(
                selectedExpressionId = expressionId,
                showObjectChooser = false,
                userMessage = if (overlapCount > 1) {
                    "Choose an object: selected ${expression?.displayName ?: "graph"} from $overlapCount nearby graphs."
                } else {
                    "Selected ${expression?.displayName ?: "graph"}"
                }
            )
        }
    }

    fun handleGraphPick(candidates: List<GraphPickCandidate>) {
        if (candidates.isEmpty()) return
        if (candidates.size == 1) {
            selectExpressionFromGraph(candidates.first().id, 1)
            return
        }
        val current = _uiState.value
        val sameCandidates = current.pickCandidates.map { it.id } == candidates.map { it.id }
        val selected = if (sameCandidates) {
            nextCandidate(current.selectedExpressionId, candidates)
        } else {
            candidates.first()
        }
        _uiState.update {
            it.copy(
                selectedExpressionId = selected.id,
                pickCandidates = candidates,
                showObjectChooser = true,
                userMessage = null
            )
        }
    }

    fun selectPickCandidate(candidateId: String) {
        selectExpressionFromGraph(candidateId, _uiState.value.pickCandidates.size)
    }

    fun selectNextPickCandidate() {
        val candidates = _uiState.value.pickCandidates
        val next = nextCandidate(_uiState.value.selectedExpressionId, candidates)
        _uiState.update { it.copy(selectedExpressionId = next.id) }
    }

    fun dismissObjectChooser() {
        _uiState.update { it.copy(showObjectChooser = false) }
    }

    fun setTraceX(x: Double) {
        _uiState.update { it.copy(traceX = x) }
        updateAnalysis()
    }

    fun toggleGrid() = _uiState.update { it.copy(showGrid = !it.showGrid) }
    fun toggleLabels() = _uiState.update { it.copy(showLabels = !it.showLabels) }
    fun toggleTrace() = _uiState.update { it.copy(traceEnabled = !it.traceEnabled) }

    fun panViewport(deltaXFraction: Double, deltaYFraction: Double) {
        _uiState.update { state ->
            val viewport = state.project.viewport
            val dx = (viewport.xMax - viewport.xMin) * deltaXFraction
            val dy = (viewport.yMax - viewport.yMin) * deltaYFraction
            state.copy(project = state.project.copy(viewport = viewport.copy(
                xMin = viewport.xMin - dx,
                xMax = viewport.xMax - dx,
                yMin = viewport.yMin + dy,
                yMax = viewport.yMax + dy
            )))
        }
        recompute()
    }

    fun zoomViewport(scale: Double, focusXFraction: Double = 0.5, focusYFraction: Double = 0.5) {
        if (!scale.isFinite() || scale <= 0.0) return
        _uiState.update { state ->
            val viewport = state.project.viewport
            val bounded = scale.coerceIn(0.5, 2.0)
            val xFocus = viewport.xMin + (viewport.xMax - viewport.xMin) * focusXFraction
            val yFocus = viewport.yMax - (viewport.yMax - viewport.yMin) * focusYFraction
            val left = (xFocus - viewport.xMin) / bounded
            val right = (viewport.xMax - xFocus) / bounded
            val bottom = (yFocus - viewport.yMin) / bounded
            val top = (viewport.yMax - yFocus) / bounded
            state.copy(project = state.project.copy(viewport = GraphViewport(
                xMin = xFocus - left,
                xMax = xFocus + right,
                yMin = yFocus - bottom,
                yMax = yFocus + top,
                zMin = viewport.zMin,
                zMax = viewport.zMax
            )))
        }
        recompute()
    }

    fun setSliderValue(id: String, value: Double) {
        _uiState.update { state ->
            state.copy(project = state.project.copy(sliders = state.project.sliders.map { slider ->
                if (slider.id == id) slider.copy(value = value.coerceIn(slider.minimum, slider.maximum)) else slider
            }))
        }
        recompute()
    }

    fun resetView() {
        _uiState.update { it.copy(project = it.project.copy(viewport = com.indianservers.ai_stem.domain.graph.GraphViewport())) }
        recompute()
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun exportCsvBytes(): ByteArray =
        csvCodec.exportPrimitives(_uiState.value.renderPrimitives).encodeToByteArray()

    fun exportPngBytes(transparent: Boolean = false): ByteArray =
        pngExporter.export(
            primitives = _uiState.value.renderPrimitives,
            viewport = _uiState.value.project.viewport,
            options = GraphPngExportOptions(
                transparent = transparent,
                title = _uiState.value.project.name
            )
        )

    fun importCsv(raw: String) {
        when (val result = csvCodec.parse(raw, hasHeader = true)) {
            is CsvParseResult.Success -> {
                val dataTable = com.indianservers.ai_stem.domain.graph.GraphDataTable(
                    id = "table-${System.currentTimeMillis()}",
                    name = "Imported CSV",
                    columns = result.table.headers,
                    rows = result.table.rows.map { row ->
                        row.mapNotNull { it.toDoubleOrNull() }
                    }.filter { it.isNotEmpty() }
                )
                _uiState.update {
                    it.copy(
                        project = it.project.copy(dataTables = it.project.dataTables + dataTable, modifiedAt = System.currentTimeMillis()),
                        userMessage = "Imported ${dataTable.rows.size} CSV rows"
                    )
                }
            }
            is CsvParseResult.Failure -> _uiState.update { it.copy(userMessage = result.message) }
        }
    }

    private fun recompute() {
        computeJob?.cancel()
        computeJob = viewModelScope.launch {
            _uiState.update { it.copy(updating = true) }
            val state = _uiState.value
            val curves = withContext(Dispatchers.Default) {
                val variables = state.project.sliders.associate { it.symbol to it.value }
                state.project.expressions.filter { it.visible }.flatMap { expression ->
                    sampler.sampleExpression(expression, state.project.viewport, state.project.appearance.qualityPreset, variables)
                }
            }
            val primitives = withContext(Dispatchers.Default) {
                primitiveBuilder.fromCurves(curves, state.project.expressions, System.currentTimeMillis())
            }
            val visibleExpressions = state.project.expressions.filter { it.visible && it.kind == GraphExpressionKind.Explicit2D }
            val equationInsights = withContext(Dispatchers.Default) { visibleExpressions.mapNotNull(insightsEngine::analyze) }
            val intersections = withContext(Dispatchers.Default) {
                visibleExpressions.indices.flatMap { firstIndex ->
                    ((firstIndex + 1) until visibleExpressions.size).flatMap { secondIndex ->
                        insightsEngine.intersections(visibleExpressions[firstIndex], visibleExpressions[secondIndex])
                    }
                }
            }
            _uiState.update {
                it.copy(
                    curves = curves,
                    renderPrimitives = primitives,
                    equationInsights = equationInsights,
                    intersections = intersections,
                    updating = false
                )
            }
            updateAnalysis()
        }
    }

    private fun updateAnalysis() {
        val state = _uiState.value
        val expression = state.project.expressions.firstOrNull { it.visible && it.kind == GraphExpressionKind.Explicit2D }
        if (expression == null) {
            _uiState.update { it.copy(analysisSummary = "No explicit 2D graph selected for analysis.") }
            return
        }
        val roots = analysis.roots(expression.source, state.project.viewport).take(3)
        val extrema = analysis.extrema(expression.source, state.project.viewport).take(3)
        val derivative = analysis.derivative(expression.source, state.traceX)
        val integral = analysis.definiteIntegral(expression.source, -1.0, 1.0)
        _uiState.update {
            it.copy(
                analysisSummary = "At x=${"%.2f".format(state.traceX)}, slope=${"%.3f".format(derivative)}. Roots: ${roots.size}. Extrema: ${extrema.size}. Integral [-1,1]≈${"%.3f".format(integral)}."
            )
        }
    }

    private fun inferKind(source: String): GraphExpressionKind =
        when {
            source.trim().startsWith("r") -> GraphExpressionKind.Polar
            source.contains("z") -> GraphExpressionKind.ExplicitSurface3D
            source.contains("=") && !source.trim().startsWith("y") -> GraphExpressionKind.Implicit2D
            else -> GraphExpressionKind.Explicit2D
        }

    private fun defaultName(kind: GraphExpressionKind): String = when (kind) {
        GraphExpressionKind.Explicit2D -> "Function"
        GraphExpressionKind.Polar -> "Polar Graph"
        GraphExpressionKind.Parametric2D -> "Parametric Curve"
        GraphExpressionKind.Implicit2D -> "Implicit Graph"
        GraphExpressionKind.Inequality2D -> "Inequality"
        GraphExpressionKind.Points -> "Points"
        GraphExpressionKind.Sequence -> "Sequence"
        GraphExpressionKind.DataTable -> "Data Table"
        GraphExpressionKind.ExplicitSurface3D -> "3D Surface"
        GraphExpressionKind.ParametricSurface3D -> "Parametric Surface"
        GraphExpressionKind.SpaceCurve -> "Space Curve"
        GraphExpressionKind.Vector -> "Vector"
        GraphExpressionKind.VectorField -> "Vector Field"
    }

    private fun nextCandidate(currentId: String?, candidates: List<GraphPickCandidate>): GraphPickCandidate {
        val index = candidates.indexOfFirst { it.id == currentId }
        return candidates[(index + 1).floorMod(candidates.size)]
    }

    private fun detectedSliders(expression: GraphExpression, existing: List<GraphSlider>): List<GraphSlider> {
        val parsed = expression.parse as? ParseOutcome.Success ?: return emptyList()
        val reserved = setOf("x", "y", "z", "t", "theta", "pi", "e", "phi")
        val existingSymbols = existing.mapTo(mutableSetOf()) { it.symbol }
        return parsed.variables
            .filterNot { it in reserved || it in existingSymbols }
            .sorted()
            .map { symbol -> GraphSlider("slider-$symbol", symbol, -10.0, 10.0, 0.1, 1.0) }
    }

    companion object {
        private val palette = listOf(0xFF2E7DFF, 0xFFE53935, 0xFF43A047, 0xFFFFA000, 0xFF8E24AA, 0xFF00897B)
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
