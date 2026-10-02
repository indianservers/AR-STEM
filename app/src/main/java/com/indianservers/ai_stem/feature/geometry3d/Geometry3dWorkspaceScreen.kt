package com.indianservers.ai_stem.feature.geometry3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.ai_stem.domain.geometry3d.SolidGeometryEngine
import com.indianservers.ai_stem.domain.geometry3d.SolidMesh
import com.indianservers.ai_stem.domain.geometry3d.SolidObject
import com.indianservers.ai_stem.domain.geometry3d.SolidType
import com.indianservers.ai_stem.domain.geometry3d.SolidVector
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import com.indianservers.ai_stem.feature.subjects.SimpleHeader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private data class SolidPalette(val background: Color, val panel: Color, val text: Color, val grid: Color, val cyan: Color, val violet: Color)
private data class ProjectedVertex(val point: Offset, val depth: Double)

@Composable
fun Geometry3dWorkspaceScreen(onBack: () -> Unit, viewModel: Geometry3dViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val palette = solidPalette(state.environmentMode)
    Scaffold(topBar = { SimpleHeader("3D Geometry", onBack) }, containerColor = palette.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(palette.background)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(state.environmentMode == WorkspaceEnvironmentMode.White, { viewModel.setEnvironment(WorkspaceEnvironmentMode.White) }, { Text("White") })
                FilterChip(state.environmentMode == WorkspaceEnvironmentMode.Black, { viewModel.setEnvironment(WorkspaceEnvironmentMode.Black) }, { Text("Black") })
                Text("${state.objects.size} solids", color = palette.text, modifier = Modifier.weight(1f).padding(10.dp))
                IconButton(viewModel::undo, enabled = state.canUndo) { Icon(Icons.Outlined.Undo, "Undo", tint = palette.text) }
                IconButton(viewModel::redo, enabled = state.canRedo) { Icon(Icons.Outlined.Redo, "Redo", tint = palette.text) }
                IconButton(viewModel::resetView) { Icon(Icons.Outlined.Refresh, "Reset view", tint = palette.text) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { SolidCanvas(state, palette, viewModel) }
            SolidControls(state, palette, viewModel)
        }
    }
}

@Composable
private fun SolidControls(state: Geometry3dUiState, palette: SolidPalette, viewModel: Geometry3dViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SolidType.entries.forEach { type -> FilterChip(false, { viewModel.add(type) }, { Text(type.displayName) }) }
            }
            state.selected?.let { selected ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.objects.forEach { objectValue ->
                        FilterChip(objectValue.id == selected.id, { viewModel.select(objectValue.id) }, { Text(objectValue.type.displayName) })
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    IconButton(viewModel::duplicate) { Icon(Icons.Outlined.ContentCopy, "Duplicate", tint = palette.text) }
                    IconButton(viewModel::toggleVisibility) { Icon(if (selected.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "Visibility", tint = palette.text) }
                    IconButton(viewModel::toggleLock) { Icon(if (selected.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, "Lock", tint = palette.text) }
                    IconButton(viewModel::delete) { Icon(Icons.Outlined.Delete, "Delete", tint = palette.text) }
                    FilterChip(state.showGrid, viewModel::toggleGrid, { Icon(Icons.Outlined.GridOn, null); Text("Grid") })
                    FilterChip(state.showAxes, viewModel::toggleAxes, { Text("Axes") })
                    FilterChip(state.xRay, viewModel::toggleXRay, { Text("X-ray") })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "X-" to { viewModel.moveSelected('x', -0.2) }, "X+" to { viewModel.moveSelected('x', 0.2) },
                        "Y-" to { viewModel.moveSelected('y', -0.2) }, "Y+" to { viewModel.moveSelected('y', 0.2) },
                        "Z-" to { viewModel.moveSelected('z', -0.2) }, "Z+" to { viewModel.moveSelected('z', 0.2) },
                        "Scale-" to { viewModel.scaleSelected(0.9) }, "Scale+" to { viewModel.scaleSelected(1.1) }
                    ).forEach { (label, action) -> FilterChip(false, action, { Text(label) }) }
                }
                ParameterSliders(selected, palette, viewModel)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Explode", color = palette.text)
                    Slider(state.explode.toFloat(), { viewModel.setExplode(it.toDouble()) }, modifier = Modifier.weight(1f))
                    Text("Slice", color = palette.text, modifier = Modifier.padding(start = 8.dp))
                    Slider(state.slice.toFloat(), { viewModel.setSlice(it.toDouble()) }, modifier = Modifier.weight(1f))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.measurements.forEach { measurement ->
                        Text("${measurement.label}: ${format(measurement.value)} ${measurement.unit}  ${measurement.formula}", color = palette.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSliders(selected: SolidObject, palette: SolidPalette, viewModel: Geometry3dViewModel) {
    val fields = when (selected.type) {
        SolidType.Cube,
        SolidType.Tetrahedron -> listOf("length" to selected.parameters.length)
        SolidType.Cuboid,
        SolidType.Pyramid,
        SolidType.TriangularPrism -> listOf("length" to selected.parameters.length, "width" to selected.parameters.width, "height" to selected.parameters.height)
        SolidType.Sphere -> listOf("radius" to selected.parameters.radius)
        SolidType.Cylinder,
        SolidType.Cone -> listOf("radius" to selected.parameters.radius, "height" to selected.parameters.height)
        SolidType.Torus -> listOf("radius" to selected.parameters.radius, "width" to selected.parameters.width)
        SolidType.Frustum -> listOf("radius" to selected.parameters.radius, "topRadius" to selected.parameters.topRadius, "height" to selected.parameters.height)
    }
    fields.forEach { (name, value) ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(name.replaceFirstChar { it.uppercase() }, color = palette.text, modifier = Modifier.padding(end = 8.dp))
            Slider(value.toFloat(), { viewModel.setParameter(name, it.toDouble()) }, valueRange = 0.2f..4f, modifier = Modifier.weight(1f))
            Text(format(value), color = palette.text, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SolidCanvas(state: Geometry3dUiState, palette: SolidPalette, viewModel: Geometry3dViewModel) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(state.zoom) {
                detectTransformGestures { _, _, zoom, rotation ->
                    if (abs(zoom - 1f) > 0.01f) viewModel.zoom(zoom.toDouble())
                    if (abs(rotation) > 0.2f) viewModel.rotateSelected(rotation.toDouble(), 0.0)
                }
            }
            .pointerInput(state.selectedId) {
                detectDragGestures(
                    onDragStart = { viewModel.beginTransform() }
                ) { change, drag ->
                    viewModel.rotateSelected(drag.x * 0.35, drag.y * 0.35)
                    change.consume()
                }
            }
            .pointerInput(state.objects, state.cameraYaw, state.cameraPitch, state.zoom) {
                detectTapGestures { tap ->
                    state.objects.filter { it.visible }.minByOrNull { objectValue ->
                        val projected = project(objectValue.transform.position, size.width.toFloat(), size.height.toFloat(), state)
                        (projected.point - tap).getDistance()
                    }?.let { viewModel.select(it.id) }
                }
            }
    ) {
        if (state.showGrid) {
            for (i in -8..8) {
                val a = project(SolidVector(i.toDouble(), -1.7, -8.0), size.width, size.height, state).point
                val b = project(SolidVector(i.toDouble(), -1.7, 8.0), size.width, size.height, state).point
                val c = project(SolidVector(-8.0, -1.7, i.toDouble()), size.width, size.height, state).point
                val d = project(SolidVector(8.0, -1.7, i.toDouble()), size.width, size.height, state).point
                drawLine(palette.grid, a, b, if (i == 0) 2f else 1f)
                drawLine(palette.grid, c, d, if (i == 0) 2f else 1f)
            }
        }
        if (state.showAxes) {
            drawAxis(SolidVector(0.0,0.0,0.0), SolidVector(3.0,0.0,0.0), Color(0xFFFF5A70), state, "X")
            drawAxis(SolidVector(0.0,0.0,0.0), SolidVector(0.0,3.0,0.0), Color(0xFF52D890), state, "Y")
            drawAxis(SolidVector(0.0,0.0,0.0), SolidVector(0.0,0.0,3.0), Color(0xFF57A7FF), state, "Z")
        }
        state.objects.filter { it.visible }.forEach { objectValue ->
            val mesh = SolidGeometryEngine.mesh(objectValue)
            val selected = objectValue.id == state.selectedId
            val color = if (selected) palette.violet else palette.cyan
            val transformed = mesh.vertices.map { vertex -> transform(vertex, objectValue, state.explode) }
            val yValues = transformed.map { it.y }
            val threshold = (yValues.minOrNull() ?: -1.0) + ((yValues.maxOrNull() ?: 1.0) - (yValues.minOrNull() ?: -1.0)) * state.slice
            val projected = transformed.map { project(it, size.width, size.height, state) }
            if (!state.xRay && mesh.faces.isNotEmpty()) {
                mesh.faces.mapNotNull { face ->
                    if (face.indices.any { transformed[it].y > threshold }) null else face to face.indices.map { projected[it] }
                }.sortedByDescending { (_, vertices) -> vertices.map { it.depth }.average() }.forEach { (_, faceVertices) ->
                    val path = Path(); faceVertices.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.point.x,p.point.y) else path.lineTo(p.point.x,p.point.y) }; path.close()
                    drawPath(path, color.copy(alpha = if (selected) 0.20f else 0.11f))
                }
            }
            mesh.edges.forEach { edge ->
                if (transformed[edge.from].y <= threshold && transformed[edge.to].y <= threshold) {
                    drawLine(color, projected[edge.from].point, projected[edge.to].point, if (selected) 5f else 3f)
                }
            }
            if (selected) projected.filterIndexed { index, _ -> transformed[index].y <= threshold }.forEach { drawCircle(palette.background, 7f, it.point); drawCircle(color, 4.5f, it.point) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxis(start: SolidVector, end: SolidVector, color: Color, state: Geometry3dUiState, label: String) {
    val a = project(start, size.width, size.height, state).point; val b = project(end, size.width, size.height, state).point
    drawLine(color, a, b, 4f)
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply { this.color = color.toArgbValue(); textSize = 31f; isAntiAlias = true }
        canvas.nativeCanvas.drawText(label, b.x + 6f, b.y, paint)
    }
}

private fun transform(vertex: SolidVector, objectValue: SolidObject, explode: Double): SolidVector {
    var value = vertex.copy(x = vertex.x * (1.0 + explode * 0.18), y = vertex.y * (1.0 + explode * 0.18), z = vertex.z * (1.0 + explode * 0.18))
    value = rotateX(value, objectValue.transform.rotation.x); value = rotateY(value, objectValue.transform.rotation.y); value = rotateZ(value, objectValue.transform.rotation.z)
    val s = objectValue.transform.scale
    return SolidVector(value.x * s + objectValue.transform.position.x, value.y * s + objectValue.transform.position.y, value.z * s + objectValue.transform.position.z)
}

private fun project(valueRaw: SolidVector, width: Float, height: Float, state: Geometry3dUiState): ProjectedVertex {
    var value = rotateY(valueRaw, state.cameraYaw); value = rotateX(value, state.cameraPitch)
    val distance = 8.5 / state.zoom
    val denominator = (distance + value.z).coerceAtLeast(1.2)
    val scale = min(width, height) * 0.68 / denominator
    return ProjectedVertex(Offset(width / 2f + (value.x * scale).toFloat(), height / 2f - (value.y * scale).toFloat()), denominator)
}

private fun rotateX(v: SolidVector, degrees: Double): SolidVector { val a=degrees*PI/180; return SolidVector(v.x,v.y*cos(a)-v.z*sin(a),v.y*sin(a)+v.z*cos(a)) }
private fun rotateY(v: SolidVector, degrees: Double): SolidVector { val a=degrees*PI/180; return SolidVector(v.x*cos(a)+v.z*sin(a),v.y,-v.x*sin(a)+v.z*cos(a)) }
private fun rotateZ(v: SolidVector, degrees: Double): SolidVector { val a=degrees*PI/180; return SolidVector(v.x*cos(a)-v.y*sin(a),v.x*sin(a)+v.y*cos(a),v.z) }

private fun solidPalette(mode: WorkspaceEnvironmentMode) = if (mode == WorkspaceEnvironmentMode.Black) {
    SolidPalette(Color(0xFF060910), Color(0xF0121722), Color(0xFFF5F7FF), Color(0x33495A73), Color(0xFF00D8F4), Color(0xFF9A64FF))
} else {
    SolidPalette(Color(0xFFFDFEFF), Color.White, Color(0xFF111827), Color(0x338F9BAD), Color(0xFF00AFC8), Color(0xFF7047EB))
}
private fun Color.toArgbValue(): Int = android.graphics.Color.argb((alpha*255).roundToInt(),(red*255).roundToInt(),(green*255).roundToInt(),(blue*255).roundToInt())
private fun format(value: Double): String = "%.3f".format(value).trimEnd('0').trimEnd('.')
