package com.indianservers.ai_stem.feature.geometry2d

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.ai_stem.domain.geometry.ConstructionObjectKind
import com.indianservers.ai_stem.domain.scene.Vector3Value
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import com.indianservers.ai_stem.feature.subjects.SimpleHeader
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class GeometryPalette(val background: Color, val panel: Color, val text: Color, val grid: Color, val axes: Color, val cyan: Color, val violet: Color)

@Composable
fun Geometry2dWorkspaceScreen(onBack: () -> Unit, viewModel: Geometry2dViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val palette = geometryPalette(state.environmentMode)
    Scaffold(topBar = { SimpleHeader("2D Geometry", onBack) }, containerColor = palette.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(palette.background)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    FilterChip(state.environmentMode == WorkspaceEnvironmentMode.White, { viewModel.setEnvironment(WorkspaceEnvironmentMode.White) }, { Text("White") })
                    FilterChip(state.environmentMode == WorkspaceEnvironmentMode.Black, { viewModel.setEnvironment(WorkspaceEnvironmentMode.Black) }, { Text("Black") })
                    Box(Modifier.weight(1f))
                    IconButton(viewModel::undo, enabled = state.canUndo) { Icon(Icons.Outlined.Undo, "Undo", tint = palette.text) }
                    IconButton(viewModel::redo, enabled = state.canRedo) { Icon(Icons.Outlined.Redo, "Redo", tint = palette.text) }
                }
                Text(state.message, color = palette.text.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GeometryCanvas(state, palette, viewModel)
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Geometry2dTool.entries.forEach { tool ->
                            FilterChip(selected = state.activeTool == tool, onClick = { viewModel.selectTool(tool) }, label = { Text(tool.label) })
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.showGrid, viewModel::toggleGrid, { Icon(Icons.Outlined.GridOn, null); Text("Grid") })
                        FilterChip(state.snapToGrid, viewModel::toggleSnap, { Text("Snap") })
                        FilterChip(state.showMeasurements, viewModel::toggleMeasurements, { Icon(Icons.Outlined.Straighten, null); Text("Measure") })
                        IconButton(viewModel::deleteSelection) { Icon(Icons.Outlined.Delete, "Delete selection", tint = palette.text) }
                        IconButton(viewModel::reset) { Icon(Icons.Outlined.Refresh, "Reset", tint = palette.text) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometryCanvas(state: Geometry2dUiState, palette: GeometryPalette, viewModel: Geometry2dViewModel) {
    var draggingPointId by remember { mutableStateOf<String?>(null) }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(state.viewport) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (pan != Offset.Zero) viewModel.pan((pan.x / size.width).toDouble(), (pan.y / size.height).toDouble())
                    if (kotlin.math.abs(zoom - 1f) > 0.01f) viewModel.zoom(zoom.toDouble())
                }
            }
            .pointerInput(state.construction, state.viewport) {
                detectTapGestures { tap ->
                    val world = screenToWorld(tap, size.width.toFloat(), size.height.toFloat(), state.viewport)
                    viewModel.tapWorkspace(world.x.toDouble(), world.y.toDouble())
                }
            }
            .pointerInput(state.construction, state.viewport) {
                detectDragGestures(
                    onDragStart = { tap ->
                        val world = screenToWorld(tap, size.width.toFloat(), size.height.toFloat(), state.viewport)
                        draggingPointId = state.construction.points.minByOrNull { hypot(it.position.x - world.x, it.position.z - world.y) }
                            ?.takeIf { hypot(it.position.x - world.x, it.position.z - world.y) < state.viewport.spanX * 0.04 }?.id
                        if (draggingPointId != null) viewModel.beginPointDrag()
                    },
                    onDragEnd = { draggingPointId = null },
                    onDragCancel = { draggingPointId = null }
                ) { change, _ ->
                    draggingPointId?.let { id ->
                        val world = screenToWorld(change.position, size.width.toFloat(), size.height.toFloat(), state.viewport)
                        viewModel.movePoint(id, world.x.toDouble(), world.y.toDouble())
                        change.consume()
                    }
                }
            }
    ) {
        fun map(value: Vector3Value) = worldToScreen(value.x, value.z, size.width, size.height, state.viewport)
        if (state.showGrid) {
            val halfY = state.viewport.spanX * size.height / size.width / 2.0
            var x = ceil((state.viewport.centerX - state.viewport.spanX / 2.0) * 2.0) / 2.0
            while (x <= state.viewport.centerX + state.viewport.spanX / 2.0) {
                val a = worldToScreen(x, state.viewport.centerY - halfY, size.width, size.height, state.viewport)
                val b = worldToScreen(x, state.viewport.centerY + halfY, size.width, size.height, state.viewport)
                drawLine(palette.grid, a, b, if (x % 1.0 == 0.0) 1.5f else 0.7f)
                x += 0.5
            }
            var y = ceil((state.viewport.centerY - halfY) * 2.0) / 2.0
            while (y <= state.viewport.centerY + halfY) {
                val a = worldToScreen(state.viewport.centerX - state.viewport.spanX / 2.0, y, size.width, size.height, state.viewport)
                val b = worldToScreen(state.viewport.centerX + state.viewport.spanX / 2.0, y, size.width, size.height, state.viewport)
                drawLine(palette.grid, a, b, if (y % 1.0 == 0.0) 1.5f else 0.7f)
                y += 0.5
            }
        }
        drawLine(palette.axes, worldToScreen(-100.0, 0.0, size.width, size.height, state.viewport), worldToScreen(100.0, 0.0, size.width, size.height, state.viewport), 3f)
        drawLine(palette.axes, worldToScreen(0.0, -100.0, size.width, size.height, state.viewport), worldToScreen(0.0, 100.0, size.width, size.height, state.viewport), 3f)

        state.resolvedObjects.filter { it.visible }.forEach { obj ->
            val selected = obj.id == state.construction.selectedObjectId
            val color = if (selected) palette.violet else palette.cyan
            when (obj.kind) {
                ConstructionObjectKind.Circle -> if (obj.points.size >= 2) {
                    val center = map(obj.points[0]); val edge = map(obj.points[1])
                    drawCircle(color, (center - edge).getDistance(), center, style = Stroke(if (selected) 6f else 4f))
                }
                ConstructionObjectKind.Polygon,
                ConstructionObjectKind.Plane -> if (obj.points.size >= 3) {
                    val path = Path(); obj.points.forEachIndexed { i, p -> val s = map(p); if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y) }; path.close()
                    drawPath(path, color.copy(alpha = 0.16f)); drawPath(path, color, style = Stroke(if (selected) 6f else 4f))
                }
                ConstructionObjectKind.Midpoint,
                ConstructionObjectKind.Point,
                ConstructionObjectKind.Intersection -> Unit
                else -> if (obj.points.size >= 2) drawLine(color, map(obj.points[0]), map(obj.points[1]), if (selected) 6f else 4f)
            }
            if (state.showMeasurements && obj.id == state.construction.selectedObjectId && obj.points.isNotEmpty()) {
                val p = map(obj.points.last())
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply { this.color = palette.text.toArgbValue(); textSize = 30f; isAntiAlias = true }
                    canvas.nativeCanvas.drawText(obj.valueLabel, p.x + 10f, p.y - 10f, paint)
                }
            }
        }
        state.construction.points.forEach { point ->
            val screen = map(point.position)
            val selected = point.id in state.construction.selectedPointIds
            if (selected) drawCircle(palette.violet.copy(alpha = 0.24f), 18f, screen)
            drawCircle(palette.background, 11f, screen)
            drawCircle(if (selected) palette.violet else palette.cyan, 8f, screen)
            if (state.showMeasurements && selected) drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply { color = palette.text.toArgbValue(); textSize = 29f; isAntiAlias = true }
                canvas.nativeCanvas.drawText("${point.label} (${format(point.position.x)}, ${format(point.position.z)})", screen.x + 12f, screen.y - 10f, paint)
            }
        }
    }
}

private fun worldToScreen(x: Double, y: Double, width: Float, height: Float, viewport: Geometry2dViewport): Offset {
    val spanY = viewport.spanX * height / width
    return Offset(
        (((x - (viewport.centerX - viewport.spanX / 2.0)) / viewport.spanX) * width).toFloat(),
        (height - ((y - (viewport.centerY - spanY / 2.0)) / spanY) * height).toFloat()
    )
}

private fun screenToWorld(point: Offset, width: Float, height: Float, viewport: Geometry2dViewport): Offset {
    val spanY = viewport.spanX * height / width
    return Offset(
        (viewport.centerX - viewport.spanX / 2.0 + point.x / width * viewport.spanX).toFloat(),
        (viewport.centerY - spanY / 2.0 + (height - point.y) / height * spanY).toFloat()
    )
}

private fun geometryPalette(mode: WorkspaceEnvironmentMode) = if (mode == WorkspaceEnvironmentMode.Black) {
    GeometryPalette(Color(0xFF070A10), Color(0xEE121722), Color(0xFFF5F7FF), Color(0x334B5A72), Color(0xFFF5F7FF), Color(0xFF00D7F2), Color(0xFF9966FF))
} else {
    GeometryPalette(Color(0xFFFDFEFF), Color.White, Color(0xFF111827), Color(0x3395A1B2), Color(0xFF111827), Color(0xFF00AFC7), Color(0xFF7047EB))
}

private fun Color.toArgbValue(): Int = android.graphics.Color.argb((alpha * 255).roundToInt(), (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
private fun format(value: Double): String = "%.2f".format(value).trimEnd('0').trimEnd('.')
