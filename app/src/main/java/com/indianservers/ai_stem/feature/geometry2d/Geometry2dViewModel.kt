package com.indianservers.ai_stem.feature.geometry2d

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.ai_stem.data.workspace.WorkspacePreferencesRepository
import com.indianservers.ai_stem.domain.geometry.ConstructionDependencyKind
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryEngine
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState
import com.indianservers.ai_stem.domain.geometry.ConstructionObject
import com.indianservers.ai_stem.domain.geometry.ConstructionObjectKind
import com.indianservers.ai_stem.domain.geometry.ResolvedConstructionObject
import com.indianservers.ai_stem.domain.scene.Vector3Value
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class Geometry2dTool(val label: String, val pointsRequired: Int) {
    Select("Select", 0), Point("Point", 1), Segment("Segment", 2), Line("Line", 2), Ray("Ray", 2),
    Vector("Vector", 2), Circle("Circle", 2), Triangle("Triangle", 3), Square("Square", 2),
    Polygon("Polygon", 3), Midpoint("Midpoint", 2), Parallel("Parallel", 3), Perpendicular("Perpendicular", 3)
}

data class Geometry2dViewport(val centerX: Double = 0.0, val centerY: Double = 0.0, val spanX: Double = 12.0)

data class Geometry2dUiState(
    val construction: ConstructionGeometryState = ConstructionGeometryState(),
    val resolvedObjects: List<ResolvedConstructionObject> = emptyList(),
    val activeTool: Geometry2dTool = Geometry2dTool.Point,
    val environmentMode: WorkspaceEnvironmentMode = WorkspaceEnvironmentMode.White,
    val viewport: Geometry2dViewport = Geometry2dViewport(),
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val showMeasurements: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val message: String = "Tap the workspace to place a point"
)

class Geometry2dViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = ConstructionGeometryEngine()
    private val preferences = WorkspacePreferencesRepository(application)
    private val undo = ArrayDeque<ConstructionGeometryState>()
    private val redo = ArrayDeque<ConstructionGeometryState>()
    private val _uiState = MutableStateFlow(Geometry2dUiState())
    val uiState: StateFlow<Geometry2dUiState> = _uiState

    init {
        viewModelScope.launch {
            preferences.environmentMode.collect { mode ->
                if (mode != WorkspaceEnvironmentMode.AR) _uiState.update { it.copy(environmentMode = mode) }
            }
        }
    }

    fun selectTool(tool: Geometry2dTool) {
        _uiState.update { it.copy(activeTool = tool, message = instruction(tool)) }
    }

    fun setEnvironment(mode: WorkspaceEnvironmentMode) {
        if (mode == WorkspaceEnvironmentMode.AR) return
        _uiState.update { it.copy(environmentMode = mode) }
        viewModelScope.launch { preferences.setEnvironmentMode(mode) }
    }

    fun tapWorkspace(x: Double, y: Double) {
        val current = _uiState.value
        val nearest = current.construction.points.minByOrNull { hypot(it.position.x - x, it.position.z - y) }
        if (current.activeTool == Geometry2dTool.Select && nearest != null && hypot(nearest.position.x - x, nearest.position.z - y) < 0.35) {
            setConstruction(engine.selectPoint(current.construction, nearest.id, multi = false), recordHistory = false)
            return
        }
        val snappedX = if (current.snapToGrid) (x * 2.0).toInt() / 2.0 else x
        val snappedY = if (current.snapToGrid) (y * 2.0).toInt() / 2.0 else y
        mutate { state ->
            val withPoint = engine.addPoint(state, Vector3Value(snappedX, 0.0, snappedY))
            completeActiveTool(withPoint, current.activeTool)
        }
    }

    fun movePoint(pointId: String, x: Double, y: Double) {
        val current = _uiState.value
        val snappedX = if (current.snapToGrid) (x * 10.0).toInt() / 10.0 else x
        val snappedY = if (current.snapToGrid) (y * 10.0).toInt() / 10.0 else y
        setConstruction(engine.movePoint(current.construction, pointId, Vector3Value(snappedX, 0.0, snappedY)), recordHistory = false)
    }

    fun beginPointDrag() {
        undo.addLast(_uiState.value.construction)
        redo.clear()
        updateHistoryFlags()
    }

    fun deleteSelection() = mutate { state ->
        val selectedObject = state.selectedObjectId
        val selectedPoints = state.selectedPointIds.toSet()
        state.copy(
            objects = state.objects.filterNot { it.id == selectedObject || it.pointIds.any(selectedPoints::contains) },
            points = state.points.filterNot { it.id in selectedPoints },
            selectedPointIds = emptyList(),
            selectedObjectId = null,
            revision = state.revision + 1
        )
    }

    fun undo() {
        val previous = undo.removeLastOrNull() ?: return
        redo.addLast(_uiState.value.construction)
        setConstruction(previous, false)
        updateHistoryFlags()
    }

    fun redo() {
        val next = redo.removeLastOrNull() ?: return
        undo.addLast(_uiState.value.construction)
        setConstruction(next, false)
        updateHistoryFlags()
    }

    fun reset() {
        mutate { ConstructionGeometryState() }
    }

    fun toggleGrid() = _uiState.update { it.copy(showGrid = !it.showGrid) }
    fun toggleSnap() = _uiState.update { it.copy(snapToGrid = !it.snapToGrid) }
    fun toggleMeasurements() = _uiState.update { it.copy(showMeasurements = !it.showMeasurements) }

    fun pan(dxFraction: Double, dyFraction: Double) = _uiState.update { state ->
        val viewport = state.viewport
        state.copy(viewport = viewport.copy(
            centerX = viewport.centerX - dxFraction * viewport.spanX,
            centerY = viewport.centerY + dyFraction * viewport.spanX
        ))
    }

    fun zoom(scale: Double) = _uiState.update { state ->
        state.copy(viewport = state.viewport.copy(spanX = (state.viewport.spanX / scale).coerceIn(2.0, 60.0)))
    }

    private fun mutate(change: (ConstructionGeometryState) -> ConstructionGeometryState) {
        val before = _uiState.value.construction
        val after = runCatching { change(before) }.getOrElse { return }
        if (after == before) return
        undo.addLast(before)
        if (undo.size > 100) undo.removeFirst()
        redo.clear()
        setConstruction(after, false)
        updateHistoryFlags()
    }

    private fun setConstruction(construction: ConstructionGeometryState, recordHistory: Boolean) {
        if (recordHistory) undo.addLast(_uiState.value.construction)
        _uiState.update { state ->
            state.copy(
                construction = construction,
                resolvedObjects = engine.resolve(construction),
                message = instruction(state.activeTool)
            )
        }
    }

    private fun completeActiveTool(state: ConstructionGeometryState, tool: Geometry2dTool): ConstructionGeometryState {
        if (state.selectedPointIds.size < tool.pointsRequired) return state
        return when (tool) {
            Geometry2dTool.Select,
            Geometry2dTool.Point -> state
            Geometry2dTool.Segment -> engine.createSegmentThroughSelected(state)
            Geometry2dTool.Line -> engine.createLineThroughSelected(state)
            Geometry2dTool.Vector -> engine.createVectorBetweenSelected(state)
            Geometry2dTool.Circle -> engine.createCircleFromSelected(state)
            Geometry2dTool.Triangle,
            Geometry2dTool.Polygon -> engine.createPolygonFromSelected(state)
            Geometry2dTool.Midpoint -> engine.createMidpoint(state)
            Geometry2dTool.Parallel -> engine.createParallelThroughSelected(state)
            Geometry2dTool.Perpendicular -> engine.createPerpendicularThroughSelected(state)
            Geometry2dTool.Ray -> addObject(state, ConstructionObjectKind.Ray, ConstructionDependencyKind.ThroughTwoPoints, 2)
            Geometry2dTool.Square -> createSquare(state)
        }
    }

    private fun addObject(state: ConstructionGeometryState, kind: ConstructionObjectKind, dependency: ConstructionDependencyKind, count: Int): ConstructionGeometryState {
        val objectValue = ConstructionObject(
            id = "geometry-${System.nanoTime()}",
            label = "${kind.name} ${state.objects.count { it.kind == kind } + 1}",
            kind = kind,
            pointIds = state.selectedPointIds.takeLast(count),
            dependencyKind = dependency
        )
        return state.copy(objects = state.objects + objectValue, selectedObjectId = objectValue.id, revision = state.revision + 1)
    }

    private fun createSquare(state: ConstructionGeometryState): ConstructionGeometryState {
        val ids = state.selectedPointIds.takeLast(2)
        val a = state.pointMap[ids[0]] ?: return state
        val c = state.pointMap[ids[1]] ?: return state
        val withB = engine.addPoint(state, Vector3Value(c.position.x, 0.0, a.position.z))
        val bId = withB.points.last().id
        val withD = engine.addPoint(withB, Vector3Value(a.position.x, 0.0, c.position.z))
        val dId = withD.points.last().id
        val polygon = ConstructionObject(
            id = "square-${System.nanoTime()}",
            label = "Square ${state.objects.count { it.kind == ConstructionObjectKind.Polygon } + 1}",
            kind = ConstructionObjectKind.Polygon,
            pointIds = listOf(a.id, bId, c.id, dId),
            dependencyKind = ConstructionDependencyKind.PolygonThroughPoints
        )
        return withD.copy(objects = withD.objects + polygon, selectedObjectId = polygon.id, revision = withD.revision + 1)
    }

    private fun updateHistoryFlags() = _uiState.update { it.copy(canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty()) }

    private fun instruction(tool: Geometry2dTool): String = when (tool) {
        Geometry2dTool.Select -> "Tap an object or drag a point"
        Geometry2dTool.Point -> "Tap to place a point"
        else -> "${tool.label}: place ${tool.pointsRequired} control points"
    }
}
