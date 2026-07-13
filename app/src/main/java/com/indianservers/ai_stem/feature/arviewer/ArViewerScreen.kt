package com.indianservers.ai_stem.feature.arviewer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathematicsCatalogue
import com.indianservers.ai_stem.domain.mathematics.SineCurveSampler
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.NodeScope
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArViewerScreen(onBack: () -> Unit, viewModel: ArViewerViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var askedPermission by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    val haptic = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val permanentlyDenied = !granted && askedPermission && activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
        viewModel.onPermissionResult(granted, permanentlyDenied)
    }

    LaunchedEffect(Unit) { viewModel.checkAvailability(context) }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            anchor?.detach()
            anchor = null
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.availability) {
                ArAvailabilityState.Checking -> StatusPanel("Checking AR compatibility", "Preparing camera and ARCore services.")
                ArAvailabilityState.PermissionRequired -> PermissionPanel(
                    permission = state.permission,
                    onRequest = {
                        askedPermission = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onSettings = { openAppSettings(context.packageName, context) },
                    onBack = onBack
                )
                ArAvailabilityState.ArServicesInstallationRequired -> UnsupportedPanel(
                    title = "Google Play Services for AR is required",
                    body = "Install Google Play Services for AR, then return to the playground.",
                    onRetry = { viewModel.checkAvailability(context) },
                    onBack = onBack
                )
                ArAvailabilityState.ArServicesUpdateRequired -> UnsupportedPanel(
                    title = "Google Play Services for AR needs an update",
                    body = "Update ARCore services to continue.",
                    onRetry = { viewModel.checkAvailability(context) },
                    onBack = onBack
                )
                ArAvailabilityState.UnsupportedDevice -> UnsupportedPanel(
                    title = "AR is not supported on this device",
                    body = "An ARCore-compatible physical Android device is required for the AR playground.",
                    onRetry = { viewModel.checkAvailability(context) },
                    onBack = onBack
                )
                is ArAvailabilityState.Error -> UnsupportedPanel(
                    title = "AR setup problem",
                    body = (state.availability as ArAvailabilityState.Error).userMessage,
                    onRetry = { viewModel.checkAvailability(context) },
                    onBack = onBack
                )
                ArAvailabilityState.Supported -> {
                    ArRuntime(
                        state = state,
                        anchor = anchor,
                        moving = moving,
                        onAnchorChanged = {
                            anchor?.detach()
                            anchor = it
                            viewModel.placeSelected()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPlaneStatus = viewModel::onPlaneStatus,
                    )
                    ArChrome(
                        state = state,
                        onBack = onBack,
                        onHelp = { showHelp = true },
                        onSelectObject = viewModel::selectObject,
                        onReplace = {
                            anchor?.detach()
                            anchor = null
                            viewModel.deleteObject()
                        },
                        onMove = {
                            moving = true
                            snackbarHostState.currentSnackbarData?.dismiss()
                        },
                        onReset = {
                            viewModel.resetTransform()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDelete = {
                            anchor?.detach()
                            anchor = null
                            viewModel.deleteObject()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onTogglePlanes = viewModel::togglePlanes
                    )
                    AnimatedVisibility(visible = moving, modifier = Modifier.align(Alignment.Center)) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("Tap a new surface to move", Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        ModalBottomSheet(onDismissRequest = { showHelp = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AR Playground Help", style = MaterialTheme.typography.titleLarge)
                listOf(
                    "Move the phone slowly to detect a surface.",
                    "Select a mathematical object.",
                    "Tap the surface to place it.",
                    "Drag to rotate.",
                    "Pinch to resize.",
                    "Walk around the object to inspect it."
                ).forEach { Text(it) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ArRuntime(
    state: ArViewerUiState,
    anchor: Anchor?,
    moving: Boolean,
    onAnchorChanged: (Anchor) -> Unit,
    onPlaneStatus: (Boolean) -> Unit
) {
    var latestFrame by remember { mutableStateOf<Frame?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    Box(Modifier.fillMaxSize()) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            planeRenderer = state.planesVisible,
            sessionConfiguration = { session: Session, config: Config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
            },
            onSessionUpdated = { session, frame ->
                latestFrame = frame
                val hasPlane = session.getAllTrackables(Plane::class.java).any {
                    it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.trackingState == TrackingState.TRACKING
                }
                onPlaneStatus(hasPlane)
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { event: MotionEvent, _ ->
                    if (anchor != null && !moving) return@rememberOnGestureListener
                    val frame = latestFrame ?: return@rememberOnGestureListener
                    val hit = frame.hitTest(event).firstOrNull {
                        val plane = it.trackable as? Plane
                        plane != null &&
                            plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            plane.trackingState == TrackingState.TRACKING &&
                            plane.isPoseInPolygon(it.hitPose)
                    }
                    if (hit != null) onAnchorChanged(hit.createAnchor())
                }
            )
        ) {
            anchor?.let {
                AnchorNode(anchor = it) {
                    val placed = state.placedObject
                    if (placed != null) {
                        key(placed.transformRevision) {
                            Node(
                                rotation = Rotation(y = placed.rotationDegrees),
                                scale = Scale(placed.scaleFactor),
                                isEditable = true
                            ) {
                                MathObjectNode(placed.type, materialLoader)
                            }
                        }
                    }
                }
            }
        }
        Reticle(state)
    }
}

@Composable
private fun NodeScope.MathObjectNode(type: MathObjectType, materialLoader: MaterialLoader) {
    val cyan = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF4EE7FF)) }
    val teal = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAA4FD1C5), roughness = 0.35f) }
    val amber = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    when (type) {
        MathObjectType.Cube -> {
            CubeNode(size = Size(0.24f, 0.24f, 0.24f), center = Position(0f, 0.12f, 0f), materialInstance = teal)
            CubeEdges(cyan)
        }
        MathObjectType.CoordinatePlane -> CoordinatePlaneNode(cyan, amber)
        MathObjectType.SineCurve -> SineCurveNode(cyan, amber)
    }
}

@Composable
private fun NodeScope.CoordinatePlaneNode(lineMaterial: com.google.android.filament.MaterialInstance, pointMaterial: com.google.android.filament.MaterialInstance) {
    val range = -5..5
    range.forEach { i ->
        val p = i * 0.04f
        LineNode(Position(-0.22f, 0.002f, p), Position(0.22f, 0.002f, p), lineMaterial)
        LineNode(Position(p, 0.002f, -0.22f), Position(p, 0.002f, 0.22f), lineMaterial)
    }
    LineNode(Position(-0.25f, 0.006f, 0f), Position(0.25f, 0.006f, 0f), pointMaterial)
    LineNode(Position(0f, 0.006f, -0.25f), Position(0f, 0.006f, 0.25f), pointMaterial)
    CubeNode(Size(0.018f, 0.018f, 0.018f), center = Position(0.08f, 0.018f, 0.08f), materialInstance = pointMaterial)
}

@Composable
private fun NodeScope.SineCurveNode(lineMaterial: com.google.android.filament.MaterialInstance, accent: com.google.android.filament.MaterialInstance) {
    LineNode(Position(-0.26f, 0.004f, 0f), Position(0.26f, 0.004f, 0f), accent)
    LineNode(Position(0f, 0.004f, -0.12f), Position(0f, 0.004f, 0.12f), accent)
    val samples = SineCurveSampler.sample(64)
    samples.zipWithNext().forEach { (a, b) ->
        LineNode(
            start = Position((a.x / (2f * PI.toFloat())) * 0.24f, 0.012f, a.y * 0.08f),
            end = Position((b.x / (2f * PI.toFloat())) * 0.24f, 0.012f, b.y * 0.08f),
            materialInstance = lineMaterial
        )
    }
}

@Composable
private fun NodeScope.CubeEdges(material: com.google.android.filament.MaterialInstance) {
    val a = -0.12f
    val b = 0.12f
    val ys = listOf(0f, 0.24f)
    ys.forEach { y ->
        LineNode(Position(a, y, a), Position(b, y, a), material)
        LineNode(Position(b, y, a), Position(b, y, b), material)
        LineNode(Position(b, y, b), Position(a, y, b), material)
        LineNode(Position(a, y, b), Position(a, y, a), material)
    }
    listOf(a, b).forEach { x ->
        listOf(a, b).forEach { z -> LineNode(Position(x, 0f, z), Position(x, 0.24f, z), material) }
    }
}

@Composable
private fun ArChrome(
    state: ArViewerUiState,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onSelectObject: (MathObjectType) -> Unit,
    onReplace: () -> Unit,
    onMove: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onTogglePlanes: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Row {
                IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Help") }
                IconButton(onClick = onTogglePlanes) { Icon(Icons.Outlined.Visibility, "Show or hide planes") }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(statusText(state), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MathematicsCatalogue.phaseOneObjects) { item ->
                    FilterChip(
                        selected = state.selectedObjectType == item.type,
                        onClick = { onSelectObject(item.type) },
                        label = { Text(item.displayName) },
                        leadingIcon = { ObjectPreviewDot(item.type) },
                        modifier = Modifier.semantics { contentDescription = "Select ${item.displayName}" }
                    )
                }
            }
            if (state.placedObject != null && state.placedObject.type != state.selectedObjectType) {
                Button(onClick = onReplace, modifier = Modifier.fillMaxWidth()) { Text("Replace Object") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onMove, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.OpenInFull, null)
                    Text("Move")
                }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Text("Reset")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), enabled = state.placedObject != null) {
                    Icon(Icons.Outlined.Delete, null)
                    Text("Delete")
                }
            }
            state.placedObject?.let {
                AssistChip(onClick = {}, label = { Text("${it.type.displayName}: scale ${"%.2f".format(it.scaleFactor)}x") })
                if (it.type == MathObjectType.Cube) {
                    ElevatedAssistChip(onClick = {}, label = { Text("Cube - 6 faces - 12 edges - 8 vertices") })
                }
            }
        }
    }
}

@Composable
private fun Reticle(state: ArViewerUiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(52.dp)) {
            val color = if (state.hasValidPlacementHit) Color(0xFF62D6C7) else Color(0xFFFFC857)
            drawCircle(color.copy(alpha = 0.18f), radius = size.minDimension / 2f)
            drawCircle(color, radius = size.minDimension / 7f)
        }
    }
}

@Composable
private fun ObjectPreviewDot(type: MathObjectType) {
    val color = when (type) {
        MathObjectType.Cube -> Color(0xFF62D6C7)
        MathObjectType.CoordinatePlane -> Color(0xFF6EDBFF)
        MathObjectType.SineCurve -> Color(0xFFFFC857)
    }
    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
}

@Composable
private fun StatusPanel(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body)
        } }
    }
}

@Composable
private fun PermissionPanel(permission: CameraPermissionState, onRequest: () -> Unit, onSettings: () -> Unit, onBack: () -> Unit) {
    UnsupportedPanel(
        title = "Camera permission needed",
        body = "The AR playground uses the camera to detect flat surfaces and anchor mathematical objects in your surroundings.",
        onRetry = onRequest,
        onBack = onBack,
        retryText = if (permission == CameraPermissionState.PermanentlyDenied) "Open App Settings" else "Try Again",
        retryOverride = if (permission == CameraPermissionState.PermanentlyDenied) onSettings else null
    )
}

@Composable
private fun UnsupportedPanel(
    title: String,
    body: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    retryText: String = "Retry compatibility check",
    retryOverride: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body)
            Button(onClick = retryOverride ?: onRetry, modifier = Modifier.fillMaxWidth()) { Text(retryText) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Mathematics") }
        }
    }
}

private fun statusText(state: ArViewerUiState): String =
    when {
        state.placedObject != null -> "Drag to rotate, pinch to resize, or walk around to inspect."
        state.hasValidPlacementHit -> "Surface found - tap to place"
        else -> "Move your phone slowly to find a flat surface"
    }

private fun openAppSettings(packageName: String, context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )
}
