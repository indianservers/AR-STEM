package com.indianservers.ai_stem.feature.geometry3d

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.ai_stem.data.workspace.WorkspacePreferencesRepository
import com.indianservers.ai_stem.domain.geometry3d.SolidGeometryEngine
import com.indianservers.ai_stem.domain.geometry3d.SolidMeasurement
import com.indianservers.ai_stem.domain.geometry3d.SolidObject
import com.indianservers.ai_stem.domain.geometry3d.SolidParameters
import com.indianservers.ai_stem.domain.geometry3d.SolidTransform
import com.indianservers.ai_stem.domain.geometry3d.SolidType
import com.indianservers.ai_stem.domain.geometry3d.SolidVector
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Geometry3dUiState(
    val objects: List<SolidObject> = listOf(SolidObject("solid-1", SolidType.Cube)),
    val selectedId: String? = "solid-1",
    val environmentMode: WorkspaceEnvironmentMode = WorkspaceEnvironmentMode.Black,
    val cameraYaw: Double = 0.0,
    val cameraPitch: Double = 0.0,
    val zoom: Double = 1.0,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val xRay: Boolean = false,
    val explode: Double = 0.0,
    val slice: Double = 1.0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val selected: SolidObject? get() = objects.firstOrNull { it.id == selectedId }
    val measurements: List<SolidMeasurement> get() = selected?.let(SolidGeometryEngine::measurements).orEmpty()
}

class Geometry3dViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = WorkspacePreferencesRepository(application)
    private val undo = ArrayDeque<List<SolidObject>>()
    private val redo = ArrayDeque<List<SolidObject>>()
    private val _uiState = MutableStateFlow(Geometry3dUiState())
    val uiState: StateFlow<Geometry3dUiState> = _uiState

    init {
        viewModelScope.launch {
            preferences.environmentMode.collect { mode -> if (mode != WorkspaceEnvironmentMode.AR) _uiState.update { it.copy(environmentMode = mode) } }
        }
    }

    fun setEnvironment(mode: WorkspaceEnvironmentMode) {
        if (mode == WorkspaceEnvironmentMode.AR) return
        _uiState.update { it.copy(environmentMode = mode) }
        viewModelScope.launch { preferences.setEnvironmentMode(mode) }
    }

    fun add(type: SolidType) = mutate { objects ->
        val offset = (objects.size % 4 - 1.5) * 1.15
        objects + SolidObject(
            id = "solid-${System.nanoTime()}",
            type = type,
            parameters = defaults(type),
            transform = SolidTransform(position = SolidVector(offset, 0.0, 0.0))
        )
    }.also { _uiState.update { state -> state.copy(selectedId = state.objects.lastOrNull()?.id) } }

    fun select(id: String) = _uiState.update { it.copy(selectedId = id) }

    fun rotateSelected(deltaX: Double, deltaY: Double) = updateSelected(record = false) { objectValue ->
        if (objectValue.locked) objectValue else objectValue.copy(transform = objectValue.transform.copy(rotation = objectValue.transform.rotation.copy(
            x = objectValue.transform.rotation.x + deltaY,
            y = objectValue.transform.rotation.y + deltaX
        )))
    }

    fun beginTransform() {
        undo.addLast(_uiState.value.objects)
        redo.clear()
        updateFlags()
    }

    fun orbit(deltaX: Double, deltaY: Double) = _uiState.update { it.copy(cameraYaw = it.cameraYaw + deltaX, cameraPitch = (it.cameraPitch + deltaY).coerceIn(-80.0, 80.0)) }
    fun zoom(scale: Double) = _uiState.update { it.copy(zoom = (it.zoom * scale).coerceIn(0.35, 4.0)) }

    fun moveSelected(axis: Char, delta: Double) = updateSelected { objectValue ->
        if (objectValue.locked) return@updateSelected objectValue
        val p = objectValue.transform.position
        val moved = when (axis) { 'x' -> p.copy(x = p.x + delta); 'y' -> p.copy(y = p.y + delta); else -> p.copy(z = p.z + delta) }
        objectValue.copy(transform = objectValue.transform.copy(position = moved))
    }

    fun scaleSelected(scale: Double) = updateSelected { objectValue ->
        if (objectValue.locked) return@updateSelected objectValue
        objectValue.copy(transform = objectValue.transform.copy(scale = (objectValue.transform.scale * scale).coerceIn(0.2, 4.0)))
    }

    fun setParameter(name: String, value: Double) = updateSelected { objectValue ->
        if (objectValue.locked) return@updateSelected objectValue
        val p = objectValue.parameters
        val next = when (name) {
            "length" -> p.copy(length = value)
            "width" -> p.copy(width = value)
            "height" -> p.copy(height = value)
            "radius" -> p.copy(radius = value)
            "topRadius" -> p.copy(topRadius = value)
            else -> p
        }
        objectValue.copy(parameters = next)
    }

    fun duplicate() {
        val selected = _uiState.value.selected ?: return
        mutate { it + selected.copy(id = "solid-${System.nanoTime()}", transform = selected.transform.copy(position = selected.transform.position.copy(x = selected.transform.position.x + 0.6))) }
        _uiState.update { it.copy(selectedId = it.objects.last().id) }
    }

    fun delete() {
        val id = _uiState.value.selectedId ?: return
        mutate { objects -> objects.filterNot { it.id == id } }
        _uiState.update { it.copy(selectedId = it.objects.lastOrNull()?.id) }
    }

    fun toggleLock() = updateSelected { it.copy(locked = !it.locked) }
    fun toggleVisibility() = updateSelected { it.copy(visible = !it.visible) }
    fun toggleGrid() = _uiState.update { it.copy(showGrid = !it.showGrid) }
    fun toggleAxes() = _uiState.update { it.copy(showAxes = !it.showAxes) }
    fun toggleXRay() = _uiState.update { it.copy(xRay = !it.xRay) }
    fun setExplode(value: Double) = _uiState.update { it.copy(explode = value.coerceIn(0.0, 1.0)) }
    fun setSlice(value: Double) = _uiState.update { it.copy(slice = value.coerceIn(0.0, 1.0)) }
    fun resetView() = _uiState.update { it.copy(cameraYaw = 0.0, cameraPitch = 0.0, zoom = 1.0, explode = 0.0, slice = 1.0) }

    fun undo() {
        val previous = undo.removeLastOrNull() ?: return
        redo.addLast(_uiState.value.objects)
        _uiState.update { it.copy(objects = previous, selectedId = previous.lastOrNull()?.id) }
        updateFlags()
    }

    fun redo() {
        val next = redo.removeLastOrNull() ?: return
        undo.addLast(_uiState.value.objects)
        _uiState.update { it.copy(objects = next, selectedId = next.lastOrNull()?.id) }
        updateFlags()
    }

    private fun updateSelected(record: Boolean = true, transform: (SolidObject) -> SolidObject) {
        val id = _uiState.value.selectedId ?: return
        if (record) mutate { objects -> objects.map { if (it.id == id) transform(it) else it } }
        else _uiState.update { state -> state.copy(objects = state.objects.map { if (it.id == id) transform(it) else it }) }
    }

    private fun mutate(change: (List<SolidObject>) -> List<SolidObject>) {
        val before = _uiState.value.objects
        val after = change(before)
        if (before == after) return
        undo.addLast(before)
        if (undo.size > 80) undo.removeFirst()
        redo.clear()
        _uiState.update { it.copy(objects = after) }
        updateFlags()
    }

    private fun updateFlags() = _uiState.update { it.copy(canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty()) }

    private fun defaults(type: SolidType): SolidParameters = when (type) {
        SolidType.Cube -> SolidParameters(length = 2.0)
        SolidType.Cuboid -> SolidParameters(length = 2.5, width = 1.5, height = 1.8)
        SolidType.Sphere -> SolidParameters(radius = 1.2)
        SolidType.Cylinder -> SolidParameters(radius = 1.0, height = 2.4)
        SolidType.Cone -> SolidParameters(radius = 1.1, height = 2.5)
        SolidType.Pyramid -> SolidParameters(length = 2.2, width = 2.0, height = 2.4)
        SolidType.TriangularPrism -> SolidParameters(length = 2.2, width = 2.0, height = 1.8)
        SolidType.Tetrahedron -> SolidParameters(length = 2.3)
        SolidType.Torus -> SolidParameters(radius = 1.2, width = 0.38)
        SolidType.Frustum -> SolidParameters(radius = 1.2, topRadius = 0.65, height = 2.3)
    }
}
