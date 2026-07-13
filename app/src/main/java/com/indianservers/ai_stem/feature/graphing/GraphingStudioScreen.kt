package com.indianservers.ai_stem.feature.graphing

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphRenderPrimitive
import com.indianservers.ai_stem.domain.graph.GraphRenderMode
import com.indianservers.ai_stem.domain.graph.GraphSamples
import com.indianservers.ai_stem.domain.graph.GraphViewport
import com.indianservers.ai_stem.domain.interaction.PickShape
import com.indianservers.ai_stem.domain.interaction.PickableItem
import com.indianservers.ai_stem.domain.interaction.PickableKind
import com.indianservers.ai_stem.domain.interaction.PickingEngine
import com.indianservers.ai_stem.domain.interaction.ScreenPoint
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphingStudioScreen(onBack: () -> Unit, viewModel: GraphingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingArRequest by remember { mutableStateOf(false) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExportLabel by remember { mutableStateOf("Export") }
    val exportPngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            writeBytesToUri(context, uri, pendingExportBytes, pendingExportLabel)
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            writeBytesToUri(context, uri, pendingExportBytes, pendingExportLabel)
        }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                viewModel.importCsv(input.readBytes().decodeToString())
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingArRequest = false
        if (granted) {
            viewModel.setRenderMode(GraphRenderMode.AR)
        } else {
            viewModel.onArPermissionDenied()
        }
    }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        topBar = { SimpleHeader("Graphing Studio", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.renderMode == GraphRenderMode.Screen, onClick = { viewModel.setRenderMode(GraphRenderMode.Screen) }, label = { Text("Screen") })
                FilterChip(
                    selected = state.renderMode == GraphRenderMode.AR,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setRenderMode(GraphRenderMode.AR)
                        } else {
                            pendingArRequest = true
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    label = { Text(if (pendingArRequest) "Requesting Camera" else "AR") }
                )
                FilterChip(selected = state.experienceMode == GraphingExperienceMode.Beginner, onClick = { viewModel.setExperienceMode(GraphingExperienceMode.Beginner) }, label = { Text("Beginner") })
                FilterChip(selected = state.experienceMode == GraphingExperienceMode.Learn, onClick = { viewModel.setExperienceMode(GraphingExperienceMode.Learn) }, label = { Text("Learn") })
                FilterChip(selected = state.experienceMode == GraphingExperienceMode.Advanced, onClick = { viewModel.setExperienceMode(GraphingExperienceMode.Advanced) }, label = { Text("Advanced") })
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics {
                        contentDescription = "Graph viewport. ${state.analysisSummary}"
                    }
            ) {
                GraphCanvas(
                    primitives = state.renderPrimitives,
                    viewport = state.project.viewport,
                    selectedExpressionId = state.selectedExpressionId,
                    onPick = viewModel::handleGraphPick
                )
                if (state.renderMode == GraphRenderMode.AR) {
                    Text(
                        "AR viewing needs an ARCore device. Screen graphing remains fully available.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(state.analysisSummary, style = MaterialTheme.typography.bodyMedium)
            if (state.experienceMode == GraphingExperienceMode.Learn) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Method note: graphs are sampled from the formula, slopes use a central-difference derivative, and integrals use Simpson's numerical rule. Results are estimates unless marked exact.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Slider(
                value = state.traceX.toFloat(),
                onValueChange = { viewModel.setTraceX(it.toDouble()) },
                valueRange = state.project.viewport.xMin.toFloat()..state.project.viewport.xMax.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.expressionText,
                onValueChange = viewModel::setExpressionText,
                label = { Text("Formula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            MathKeyboard(
                advanced = !state.beginnerMode,
                onToken = viewModel::appendToken,
                onBackspace = viewModel::backspace,
                onDone = { viewModel.addExpression() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.addExpression(GraphExpressionKind.Explicit2D) }, modifier = Modifier.weight(1f)) {
                    Text("Show Graph")
                }
                OutlinedButton(onClick = viewModel::resetView, modifier = Modifier.weight(1f)) {
                    Text("Reset View")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pendingExportBytes = viewModel.exportPngBytes(transparent = false)
                    pendingExportLabel = "PNG"
                    exportPngLauncher.launch("graph.png")
                }) {
                    Text("Export PNG")
                }
                OutlinedButton(onClick = {
                    pendingExportBytes = viewModel.exportPngBytes(transparent = true)
                    pendingExportLabel = "Transparent PNG"
                    exportPngLauncher.launch("graph-transparent.png")
                }) {
                    Text("Transparent PNG")
                }
                OutlinedButton(onClick = {
                    pendingExportBytes = viewModel.exportCsvBytes()
                    pendingExportLabel = "CSV"
                    exportCsvLauncher.launch("graph-samples.csv")
                }) {
                    Text("Export CSV")
                }
                OutlinedButton(onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) }) {
                    Text("Import CSV")
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                item {
                    Text("Templates", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphSamples.templates().take(if (state.beginnerMode) 8 else 12).forEach { template ->
                            AssistChip(onClick = { viewModel.applyTemplate(template) }, label = { Text(template.displayName) })
                        }
                    }
                }
                item { Text("Expressions", style = MaterialTheme.typography.titleMedium) }
                items(state.project.expressions) { expression ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.toggleExpression(expression.id) }) {
                                Icon(if (expression.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = "Toggle visibility")
                            }
                            Column(Modifier.weight(1f)) {
                                Text(expression.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(expression.source, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { viewModel.deleteExpression(expression.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete expression")
                            }
                        }
                    }
                }
            }
        }
    }
    if (state.showObjectChooser) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissObjectChooser) {
            Column(Modifier.padding(bottom = 18.dp)) {
                Text(
                    "Choose an Object",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                state.pickCandidates.forEach { candidate ->
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Functions, contentDescription = null) },
                        headlineContent = { Text(candidate.name) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    candidate.type,
                                    candidate.groupName,
                                    if (candidate.visible) "Visible" else "Hidden",
                                    if (candidate.locked) "Locked" else "Editable",
                                    "${"%.1f".format(candidate.distancePx)} px"
                                ).joinToString(" - ")
                            )
                        },
                        trailingContent = {
                            Button(onClick = { viewModel.selectPickCandidate(candidate.id) }) {
                                Text("Select")
                            }
                        }
                    )
                }
                Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::selectNextPickCandidate, modifier = Modifier.weight(1f)) {
                        Text("Select Next")
                    }
                    OutlinedButton(onClick = viewModel::dismissObjectChooser, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphCanvas(
    primitives: List<GraphRenderPrimitive>,
    viewport: GraphViewport,
    selectedExpressionId: String?,
    onPick: (List<GraphPickCandidate>) -> Unit
) {
    val pickingEngine = remember { PickingEngine() }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(primitives, viewport) {
                detectTapGestures { tap ->
                    val polylines = primitives.filterIsInstance<GraphRenderPrimitive.Polyline>()
                    val items = polylines.mapIndexedNotNull { index, primitive ->
                        val mapped = primitive.vertices.map { point ->
                            ScreenPoint(
                                x = ((point.x - viewport.xMin) / (viewport.xMax - viewport.xMin)) * size.width,
                                y = size.height - ((point.y - viewport.yMin) / (viewport.yMax - viewport.yMin)) * size.height
                            )
                        }
                        if (mapped.size >= 2) {
                            PickableItem(
                                id = primitive.sourceExpressionId,
                                name = primitive.picking.label,
                                kind = PickableKind.GraphCurve,
                                shape = PickShape.Polyline(mapped, tolerancePx = primitive.picking.tolerancePx),
                                layer = index
                            )
                        } else {
                            null
                        }
                    }
                    val result = pickingEngine.pick(ScreenPoint(tap.x.toDouble(), tap.y.toDouble()), items)
                    onPick(
                        result.candidates.map { hit ->
                            GraphPickCandidate(
                                id = hit.item.id,
                                name = hit.item.name,
                                type = hit.item.kind.name,
                                groupName = hit.item.groupId,
                                visible = hit.item.visible,
                                locked = hit.item.locked,
                                distancePx = hit.distancePx
                            )
                        }
                    )
                }
            }
    ) {
        fun map(pointX: Double, pointY: Double): Offset {
            val x = ((pointX - viewport.xMin) / (viewport.xMax - viewport.xMin)).toFloat() * size.width
            val y = size.height - ((pointY - viewport.yMin) / (viewport.yMax - viewport.yMin)).toFloat() * size.height
            return Offset(x, y)
        }
        val grid = Color(0x33808080)
        for (i in -10..10) {
            val x = map(i.toDouble(), 0.0).x
            val y = map(0.0, i.toDouble()).y
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        drawLine(Color(0xFF202124), map(viewport.xMin, 0.0), map(viewport.xMax, 0.0), strokeWidth = 2f)
        drawLine(Color(0xFF202124), map(0.0, viewport.yMin), map(0.0, viewport.yMax), strokeWidth = 2f)
        primitives.filterIsInstance<GraphRenderPrimitive.Polyline>().forEachIndexed { index, primitive ->
            val path = Path()
            primitive.vertices.forEachIndexed { pointIndex, point ->
                val mapped = map(point.x, point.y)
                if (pointIndex == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
            }
            if (primitive.sourceExpressionId == selectedExpressionId) {
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 9f, cap = StrokeCap.Round)
                )
            }
            drawPath(
                path = path,
                color = palette[index % palette.size],
                style = Stroke(width = if (primitive.sourceExpressionId == selectedExpressionId) 6f else 4f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun MathKeyboard(advanced: Boolean, onToken: (String) -> Unit, onBackspace: () -> Unit, onDone: () -> Unit) {
    val beginner = listOf("7", "8", "9", "+", "-", "4", "5", "6", "*", "/", "1", "2", "3", "^2", "sqrt(", "0", ".", "x", "sin(", "cos(", "tan(", "pi", "(", ")")
    val advancedTokens = beginner + listOf("theta", "abs(", "ln(", "log(", "exp(", "min(", "max(", "<", ">", "<=", ">=", "!")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (if (advanced) advancedTokens else beginner).forEach { token ->
            OutlinedButton(onClick = { onToken(token) }) { Text(token) }
        }
        OutlinedButton(onClick = onBackspace) {
            Icon(Icons.Outlined.Backspace, contentDescription = null)
            Text("Back")
        }
        Button(onClick = onDone) { Text("Done") }
    }
}

private val palette = listOf(
    Color(0xFF2E7DFF),
    Color(0xFFE53935),
    Color(0xFF43A047),
    Color(0xFFFFA000),
    Color(0xFF8E24AA),
    Color(0xFF00897B)
)

private fun writeBytesToUri(context: android.content.Context, uri: Uri, bytes: ByteArray?, label: String) {
    if (bytes == null) return
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(bytes)
        output.flush()
    }
}
