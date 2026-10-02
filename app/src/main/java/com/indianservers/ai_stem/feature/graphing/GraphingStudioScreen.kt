package com.indianservers.ai_stem.feature.graphing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.ai_stem.domain.graph.GraphEquationInsight
import com.indianservers.ai_stem.domain.graph.GraphExpression
import com.indianservers.ai_stem.domain.graph.GraphFeaturePoint
import com.indianservers.ai_stem.domain.graph.GraphPoint
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import com.indianservers.ai_stem.domain.graph.GraphViewport
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import com.indianservers.ai_stem.feature.subjects.SimpleHeader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private data class GraphPalette(
    val background: Color,
    val panel: Color,
    val panelBorder: Color,
    val text: Color,
    val secondaryText: Color,
    val axes: Color,
    val majorGrid: Color,
    val minorGrid: Color,
    val selection: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingStudioScreen(onBack: () -> Unit, viewModel: GraphingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var exportName by remember { mutableStateOf("graph.png") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) writeBytesToUri(context, uri, exportBytes)
    }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    val palette = graphPalette(state.workspaceMode)
    Scaffold(
        topBar = { SimpleHeader("Graph Workspace", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }, containerColor = Color(0xFF7C4DFF)) {
                Icon(Icons.Outlined.Add, contentDescription = "Add expression", tint = Color.White)
            }
        },
        containerColor = palette.background
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.background)
        ) {
            val tablet = maxWidth >= 840.dp
            if (tablet) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpressionPanel(state, palette, viewModel, onAdd = { showEditor = true }, Modifier.weight(0.25f))
                    GraphStage(state, palette, viewModel, Modifier.weight(0.5f))
                    InsightsPanel(state.equationInsights, state.intersections, palette, Modifier.weight(0.25f))
                }
            } else {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactHeader(state, palette, viewModel)
                    GraphStage(state, palette, viewModel, Modifier.weight(1f))
                    ToolRow(
                        state = state,
                        palette = palette,
                        onTrace = viewModel::toggleTrace,
                        onGrid = viewModel::toggleGrid,
                        onLabels = viewModel::toggleLabels,
                        onInsights = { showInsights = true },
                        onReset = viewModel::resetView,
                        onExport = {
                            exportBytes = viewModel.exportPngBytes(transparent = false)
                            exportName = "graph-workspace.png"
                            exportLauncher.launch(exportName)
                        }
                    )
                    if (state.project.sliders.isNotEmpty()) ParameterPanel(state, palette, viewModel)
                }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(onDismissRequest = { showEditor = false }, containerColor = palette.panel) {
            ExpressionEditor(
                value = state.expressionText,
                palette = palette,
                editing = state.selectedExpressionId != null,
                onValueChange = viewModel::setExpressionText,
                onToken = viewModel::appendToken,
                onAdd = {
                    if (state.selectedExpressionId == null) viewModel.addExpression() else viewModel.replaceSelectedExpression()
                    showEditor = false
                }
            )
        }
    }
    if (showInsights) {
        ModalBottomSheet(onDismissRequest = { showInsights = false }, containerColor = palette.panel) {
            InsightsPanel(
                insights = state.equationInsights,
                intersections = state.intersections,
                palette = palette,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
        }
    }
}

@Composable
private fun CompactHeader(state: GraphingUiState, palette: GraphPalette, viewModel: GraphingViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.panelBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Functions", color = palette.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                WorkspaceToggle(state.workspaceMode, viewModel::setWorkspaceMode)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.project.expressions.forEach { expression ->
                    FilterChip(
                        selected = expression.id == state.selectedExpressionId,
                        onClick = { viewModel.editExpression(expression.id) },
                        label = { Text(expression.source) },
                        leadingIcon = {
                            Box(Modifier.size(10.dp).background(Color(expression.style.color), RoundedCornerShape(5.dp)))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressionPanel(
    state: GraphingUiState,
    palette: GraphPalette,
    viewModel: GraphingViewModel,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.panelBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Expressions", color = palette.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "Add expression", tint = palette.text) }
            }
            WorkspaceToggle(state.workspaceMode, viewModel::setWorkspaceMode)
            state.project.expressions.forEach { expression ->
                ExpressionRow(expression, selected = expression.id == state.selectedExpressionId, palette, viewModel)
            }
            if (state.project.sliders.isNotEmpty()) ParameterPanel(state, palette, viewModel)
        }
    }
}

@Composable
private fun ExpressionRow(expression: GraphExpression, selected: Boolean, palette: GraphPalette, viewModel: GraphingViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) palette.selection.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) palette.selection else palette.panelBorder, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.toggleExpression(expression.id) }) {
            Icon(if (expression.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "Toggle ${expression.displayName}", tint = Color(expression.style.color))
        }
        Column(Modifier.weight(1f)) {
            Text(expression.source, color = palette.text, style = MaterialTheme.typography.bodyMedium)
            Text(expression.displayName, color = palette.secondaryText, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { viewModel.editExpression(expression.id) }) { Icon(Icons.Outlined.Edit, "Edit", tint = palette.secondaryText) }
        IconButton(onClick = { viewModel.duplicateExpression(expression.id) }) { Icon(Icons.Outlined.ContentCopy, "Duplicate", tint = palette.secondaryText) }
        IconButton(onClick = { viewModel.deleteExpression(expression.id) }) { Icon(Icons.Outlined.Delete, "Delete", tint = palette.secondaryText) }
    }
}

@Composable
private fun WorkspaceToggle(mode: WorkspaceEnvironmentMode, onChange: (WorkspaceEnvironmentMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(selected = mode == WorkspaceEnvironmentMode.White, onClick = { onChange(WorkspaceEnvironmentMode.White) }, label = { Text("White") })
        FilterChip(selected = mode == WorkspaceEnvironmentMode.Black, onClick = { onChange(WorkspaceEnvironmentMode.Black) }, label = { Text("Black") })
    }
}

@Composable
private fun GraphStage(state: GraphingUiState, palette: GraphPalette, viewModel: GraphingViewModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 300.dp),
        colors = CardDefaults.cardColors(containerColor = palette.background),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.panelBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        GraphCanvas(
            expressions = state.project.expressions,
            primitives = state.renderPrimitives,
            viewport = state.project.viewport,
            selectedExpressionId = state.selectedExpressionId,
            features = state.equationInsights.flatMap { it.featurePoints } + state.intersections,
            showGrid = state.showGrid,
            showLabels = state.showLabels,
            traceEnabled = state.traceEnabled,
            traceX = state.traceX,
            palette = palette,
            onPan = viewModel::panViewport,
            onZoom = viewModel::zoomViewport,
            onTrace = viewModel::setTraceX,
            onReset = viewModel::resetView
        )
    }
}

@Composable
private fun ToolRow(
    state: GraphingUiState,
    palette: GraphPalette,
    onTrace: () -> Unit,
    onGrid: () -> Unit,
    onLabels: () -> Unit,
    onInsights: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolButton("Trace", Icons.Outlined.MyLocation, state.traceEnabled, palette, onTrace)
        ToolButton("Grid", Icons.Outlined.GridOn, state.showGrid, palette, onGrid)
        ToolButton("Labels", Icons.Outlined.Label, state.showLabels, palette, onLabels)
        ToolButton("Insights", Icons.Outlined.Insights, false, palette, onInsights)
        ToolButton("Reset", Icons.Outlined.Refresh, false, palette, onReset)
        ToolButton("Export", Icons.Outlined.SaveAlt, false, palette, onExport)
    }
}

@Composable
private fun ToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, palette: GraphPalette, action: () -> Unit) {
    OutlinedButton(onClick = action, border = androidx.compose.foundation.BorderStroke(1.dp, if (active) palette.selection else palette.panelBorder)) {
        Icon(icon, contentDescription = null, tint = if (active) palette.selection else palette.text)
        Text(label, color = if (active) palette.selection else palette.text, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ParameterPanel(state: GraphingUiState, palette: GraphPalette, viewModel: GraphingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.project.sliders.forEach { slider ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(slider.symbol, color = palette.text, fontWeight = FontWeight.Bold, modifier = Modifier.size(28.dp))
                Slider(
                    value = slider.value.toFloat(),
                    onValueChange = { viewModel.setSliderValue(slider.id, it.toDouble()) },
                    valueRange = slider.minimum.toFloat()..slider.maximum.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(formatNumber(slider.value), color = palette.text, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InsightsPanel(
    insights: List<GraphEquationInsight>,
    intersections: List<GraphFeaturePoint>,
    palette: GraphPalette,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.panelBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Graph Insights", color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            insights.forEach { insight ->
                Text(insight.equation, color = palette.selection, fontWeight = FontWeight.SemiBold)
                insight.properties.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label, color = palette.secondaryText, modifier = Modifier.weight(1f))
                        Text(value, color = palette.text)
                    }
                }
            }
            if (intersections.isNotEmpty()) {
                Text("Intersections", color = palette.secondaryText)
                intersections.forEach { Text(it.label, color = palette.text) }
            }
        }
    }
}

@Composable
private fun ExpressionEditor(
    value: String,
    palette: GraphPalette,
    editing: Boolean,
    onValueChange: (String) -> Unit,
    onToken: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editing) "Edit expression" else "Add expression", color = palette.text, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("y = f(x)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("x", "^2", "^3", "sqrt(", "abs(", "sin(", "cos(", "tan(", "log(", "exp(", "pi", "(", ")").forEach { token ->
                OutlinedButton(onClick = { onToken(token) }) { Text(token) }
            }
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text(if (editing) "Update graph" else "Plot graph") }
    }
}

@Composable
private fun GraphCanvas(
    expressions: List<GraphExpression>,
    primitives: List<GraphRenderPrimitive>,
    viewport: GraphViewport,
    selectedExpressionId: String?,
    features: List<GraphFeaturePoint>,
    showGrid: Boolean,
    showLabels: Boolean,
    traceEnabled: Boolean,
    traceX: Double,
    palette: GraphPalette,
    onPan: (Double, Double) -> Unit,
    onZoom: (Double, Double, Double) -> Unit,
    onTrace: (Double) -> Unit,
    onReset: () -> Unit
) {
    val expressionColors = expressions.associate { it.id to Color(it.style.color) }
    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Interactive Cartesian graph" }
            .pointerInput(viewport, traceEnabled) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (traceEnabled && abs(pan.x) > 0f) {
                        val x = viewport.xMin + centroid.x / size.width * (viewport.xMax - viewport.xMin)
                        onTrace(x)
                    } else {
                        if (pan != Offset.Zero) onPan((pan.x / size.width).toDouble(), (pan.y / size.height).toDouble())
                        if (abs(zoom - 1f) > 0.01f) onZoom(zoom.toDouble(), (centroid.x / size.width).toDouble(), (centroid.y / size.height).toDouble())
                    }
                }
            }
            .pointerInput(viewport) {
                detectTapGestures(
                    onDoubleTap = { onReset() },
                    onTap = { tap ->
                        if (traceEnabled) onTrace(viewport.xMin + tap.x / size.width * (viewport.xMax - viewport.xMin))
                    }
                )
            }
    ) {
        fun map(point: GraphPoint): Offset = Offset(
            (((point.x - viewport.xMin) / (viewport.xMax - viewport.xMin)) * size.width).toFloat(),
            (size.height - ((point.y - viewport.yMin) / (viewport.yMax - viewport.yMin)) * size.height).toFloat()
        )
        fun visible(point: GraphPoint) = point.x in viewport.xMin..viewport.xMax && point.y in viewport.yMin..viewport.yMax
        val xStep = niceStep((viewport.xMax - viewport.xMin) / 8.0)
        val yStep = niceStep((viewport.yMax - viewport.yMin) / 8.0)
        if (showGrid) {
            drawGrid(viewport, xStep / 5.0, yStep / 5.0, palette.minorGrid, 1f, ::map)
            drawGrid(viewport, xStep, yStep, palette.majorGrid, 1.5f, ::map)
        }
        if (0.0 in viewport.yMin..viewport.yMax) drawLine(palette.axes, map(GraphPoint(viewport.xMin, 0.0)), map(GraphPoint(viewport.xMax, 0.0)), 3f)
        if (0.0 in viewport.xMin..viewport.xMax) drawLine(palette.axes, map(GraphPoint(0.0, viewport.yMin)), map(GraphPoint(0.0, viewport.yMax)), 3f)
        if (showLabels) drawAxisLabels(viewport, xStep, yStep, palette, ::map)

        primitives.filterIsInstance<GraphRenderPrimitive.Polyline>().forEach { primitive ->
            val path = Path()
            primitive.vertices.forEachIndexed { index, point ->
                val screen = map(point)
                if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
            }
            val color = expressionColors[primitive.sourceExpressionId] ?: palette.selection
            if (primitive.sourceExpressionId == selectedExpressionId) drawPath(path, color.copy(alpha = 0.22f), style = Stroke(12f, cap = StrokeCap.Round))
            drawPath(path, color, style = Stroke(if (primitive.sourceExpressionId == selectedExpressionId) 5.5f else 4f, cap = StrokeCap.Round))
        }

        if (showLabels) {
            features.filter { visible(it.point) }.forEach { feature ->
                val point = map(feature.point)
                val color = if (feature.kind == "intersection") Color(0xFFFFC857) else palette.selection
                drawCircle(palette.background, radius = 8f, center = point)
                drawCircle(color, radius = 6f, center = point)
                drawLine(color.copy(alpha = 0.75f), point, point + Offset(12f, -14f), 1.5f)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = color.toArgbValue()
                        textSize = 30f
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText("${feature.label} ${feature.kind}", point.x + 15f, point.y - 16f, paint)
                }
            }
        }

        if (traceEnabled) {
            val tracePoints = primitives.filterIsInstance<GraphRenderPrimitive.Polyline>().mapNotNull { primitive ->
                primitive.vertices.minByOrNull { abs(it.x - traceX) }?.let { primitive.sourceExpressionId to it }
            }.filter { visible(it.second) }
            tracePoints.forEach { (id, point) ->
                val screen = map(point)
                val color = expressionColors[id] ?: palette.selection
                drawLine(color.copy(alpha = 0.5f), Offset(screen.x, 0f), Offset(screen.x, size.height), 1.5f)
                drawCircle(color, 8f, screen)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply { this.color = palette.text.toArgbValue(); textSize = 32f; isAntiAlias = true }
                    canvas.nativeCanvas.drawText("(${formatNumber(point.x)}, ${formatNumber(point.y)})", screen.x + 12f, screen.y - 12f, paint)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    viewport: GraphViewport,
    xStep: Double,
    yStep: Double,
    color: Color,
    stroke: Float,
    map: (GraphPoint) -> Offset
) {
    if (xStep <= 0.0 || yStep <= 0.0) return
    var x = ceil(viewport.xMin / xStep) * xStep
    while (x <= viewport.xMax) {
        drawLine(color, map(GraphPoint(x, viewport.yMin)), map(GraphPoint(x, viewport.yMax)), stroke)
        x += xStep
    }
    var y = ceil(viewport.yMin / yStep) * yStep
    while (y <= viewport.yMax) {
        drawLine(color, map(GraphPoint(viewport.xMin, y)), map(GraphPoint(viewport.xMax, y)), stroke)
        y += yStep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisLabels(
    viewport: GraphViewport,
    xStep: Double,
    yStep: Double,
    palette: GraphPalette,
    map: (GraphPoint) -> Offset
) {
    val paint = android.graphics.Paint().apply { color = palette.text.toArgbValue(); textSize = 27f; isAntiAlias = true }
    val xAxisY = map(GraphPoint(0.0, 0.0.coerceIn(viewport.yMin, viewport.yMax))).y
    val yAxisX = map(GraphPoint(0.0.coerceIn(viewport.xMin, viewport.xMax), 0.0)).x
    drawIntoCanvas { canvas ->
        var x = ceil(viewport.xMin / xStep) * xStep
        while (x <= viewport.xMax) {
            if (abs(x) > xStep * 0.2) {
                val p = map(GraphPoint(x, 0.0.coerceIn(viewport.yMin, viewport.yMax)))
                canvas.nativeCanvas.drawText(formatNumber(x), p.x - 10f, (xAxisY + 30f).coerceIn(30f, size.height - 4f), paint)
            }
            x += xStep
        }
        var y = ceil(viewport.yMin / yStep) * yStep
        while (y <= viewport.yMax) {
            if (abs(y) > yStep * 0.2) {
                val p = map(GraphPoint(0.0.coerceIn(viewport.xMin, viewport.xMax), y))
                canvas.nativeCanvas.drawText(formatNumber(y), (yAxisX + 8f).coerceIn(4f, size.width - 60f), p.y - 5f, paint)
            }
            y += yStep
        }
        canvas.nativeCanvas.drawText("x", size.width - 28f, (xAxisY - 10f).coerceIn(30f, size.height - 8f), paint)
        canvas.nativeCanvas.drawText("y", (yAxisX + 10f).coerceIn(4f, size.width - 28f), 30f, paint)
    }
}

private fun niceStep(raw: Double): Double {
    if (!raw.isFinite() || raw <= 0.0) return 1.0
    val exponent = floor(log10(raw))
    val fraction = raw / 10.0.pow(exponent)
    val nice = when { fraction < 1.5 -> 1.0; fraction < 3.0 -> 2.0; fraction < 7.0 -> 5.0; else -> 10.0 }
    return nice * 10.0.pow(exponent)
}

private fun graphPalette(mode: WorkspaceEnvironmentMode): GraphPalette = if (mode == WorkspaceEnvironmentMode.Black) {
    GraphPalette(Color(0xFF060910), Color(0xE6121722), Color(0xFF354052), Color(0xFFF5F7FF), Color(0xFFABB5C7), Color(0xFFF5F7FF), Color(0x664A5870), Color(0x2B4A5870), Color(0xFF00D9F5))
} else {
    GraphPalette(Color(0xFFFDFEFF), Color(0xF7FFFFFF), Color(0xFFD9DFEA), Color(0xFF101827), Color(0xFF5D687A), Color(0xFF111827), Color(0x669BA8BA), Color(0x269BA8BA), Color(0xFF00AFC9))
}

private fun Color.toArgbValue(): Int = android.graphics.Color.argb((alpha * 255).roundToInt(), (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
private fun formatNumber(value: Double): String = if (abs(value - value.roundToInt()) < 1e-8) value.roundToInt().toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
private fun writeBytesToUri(context: android.content.Context, uri: Uri, bytes: ByteArray?) {
    if (bytes == null) return
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
}
