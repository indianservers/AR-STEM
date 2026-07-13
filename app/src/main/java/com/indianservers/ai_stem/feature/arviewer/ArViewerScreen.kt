package com.indianservers.ai_stem.feature.arviewer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.SemanticLabel
import com.google.ar.core.Session
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.core.ar.AugmentedImageArEngine
import com.indianservers.ai_stem.core.ar.GraphColorMap
import com.indianservers.ai_stem.core.ar.ArPoseSample
import com.indianservers.ai_stem.core.ar.ArGuidanceSeverity
import com.indianservers.ai_stem.core.ar.ArSensorFusionEngine
import com.indianservers.ai_stem.core.ar.ArSensorFusionInput
import com.indianservers.ai_stem.core.ar.ArSensorFusionResult
import com.indianservers.ai_stem.core.ar.ArSceneUnderstandingSample
import com.indianservers.ai_stem.core.ar.NativeArSensorMonitor
import com.indianservers.ai_stem.core.ar.NativeArSensorSample
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialArEngine
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialFrameState
import com.indianservers.ai_stem.core.ar.OutdoorGeometryObservation
import com.indianservers.ai_stem.core.ar.OutdoorGeometryType
import com.indianservers.ai_stem.core.ar.OutdoorMeshQuality
import com.indianservers.ai_stem.core.ar.OutdoorWireframeEdge
import com.indianservers.ai_stem.core.ar.PaperGraphFrameState
import com.indianservers.ai_stem.domain.mathematics.MathematicsCatalogue
import com.indianservers.ai_stem.domain.mathematics.MathObjectCategory
import com.indianservers.ai_stem.domain.mathematics.MathObjectDefinition
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MeasurementFormatter
import com.indianservers.ai_stem.domain.mathematics.parameterNumber
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import com.indianservers.ai_stem.domain.graph.ParseOutcome
import com.indianservers.ai_stem.domain.geometry.ConstructionConstraintKind
import com.indianservers.ai_stem.domain.geometry.ConstructionObjectKind
import com.indianservers.ai_stem.domain.geometry.ResolvedConstructionObject
import com.indianservers.ai_stem.domain.interaction.ArGestureHandle
import com.indianservers.ai_stem.domain.scene.ArDepthOcclusionMode
import com.indianservers.ai_stem.domain.scene.ArPerformanceProfile
import com.indianservers.ai_stem.domain.scene.ArSceneTemplates
import com.indianservers.ai_stem.domain.scene.ArWorkflowTemplates
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.MathSceneObject
import com.indianservers.ai_stem.domain.scene.SceneInteractionMode
import com.indianservers.ai_stem.domain.scene.Vector3Value
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
import kotlinx.coroutines.delay
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val AR_LOG_TAG = "AiStemAR"
private const val RETICLE_HIT_TEST_INTERVAL_NANOS = 250_000_000L
private const val MIN_STABLE_PLANE_FRAMES = 8
private const val MIN_PLANE_EXTENT_METERS = 0.18f

private fun NativeArSensorSample.compactLog(): String =
    "acc=${"%.2f".format(linearAccelerationMagnitude)} gyro=${"%.2f".format(gyroscopeMagnitude)} pitch=${pitchDegrees?.let { "%.0f".format(it) }} roll=${rollDegrees?.let { "%.0f".format(it) }} lux=${ambientLightLux?.let { "%.0f".format(it) }}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArViewerScreen(onBack: () -> Unit, viewModel: ArViewerViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var askedPermission by remember { mutableStateOf(false) }
    var askedLocationPermission by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    var saveName by remember { mutableStateOf("AR Mathematics Scene") }
    val haptic = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(AR_LOG_TAG, "Camera permission result granted=$granted askedBefore=$askedPermission")
        val permanentlyDenied = !granted && askedPermission && activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
        viewModel.onPermissionResult(granted, permanentlyDenied)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(AR_LOG_TAG, "Location permission result granted=$granted askedBefore=$askedLocationPermission")
        val permanentlyDenied = !granted && askedLocationPermission && activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false
        viewModel.onLocationPermissionResult(granted, permanentlyDenied)
        if (granted) viewModel.selectArEngineMode(ArEngineMode.OutdoorGeospatialMath)
    }

    LaunchedEffect(Unit) {
        Log.d(AR_LOG_TAG, "Opening AR viewer")
        viewModel.checkAvailability(context)
    }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }
    DisposableEffect(Unit) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(AR_LOG_TAG, "Uncaught crash while AR viewer is active on thread=${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        onDispose {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
            anchor?.detach()
            anchor = null
            Log.d(AR_LOG_TAG, "Closing AR viewer and detaching anchor")
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
                    key(state.arEngineMode) {
                        DisposableEffect(state.arEngineMode) {
                            anchor?.detach()
                            anchor = null
                            moving = false
                            onDispose { }
                        }
                        ArRuntime(
                            state = state,
                            anchor = anchor,
                            moving = moving,
                            onAnchorChanged = {
                                anchor?.detach()
                                anchor = it
                                moving = false
                                viewModel.onAnchorEstablished()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onPlaneStatus = viewModel::onPlaneStatus,
                            onTrackingStatus = viewModel::onTrackingStatus,
                            onSensorFusion = viewModel::onSensorFusion,
                            onPaperGraphFrame = viewModel::onPaperGraphFrame,
                            onPaperGraphCalibrationTap = viewModel::onPaperGraphCalibrationTap,
                            onPointLabelTap = viewModel::addPointLabelAt,
                            onRulerAnchorTap = viewModel::addRulerAnchorAt,
                            onPlacementPoint = viewModel::recordPlacementPoint,
                            onOutdoorGeospatialFrame = viewModel::onOutdoorGeospatialFrame,
                            onPlacementMissed = viewModel::onPlacementMissed,
                            onRuntimeError = viewModel::onRuntimeError
                        )
                    }
                    ArChrome(
                        state = state,
                        onBack = onBack,
                        onHelp = { showHelp = true },
                        onSelectObject = viewModel::selectObject,
                        onSelectArEngineMode = { mode ->
                            if (mode == ArEngineMode.OutdoorGeospatialMath && state.locationPermission != LocationPermissionState.Granted) {
                                askedLocationPermission = true
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                viewModel.selectArEngineMode(mode)
                            }
                        },
                        onSelectMathExperience = { experience ->
                            if (experience == MathArExperience.OutdoorGeometry && state.locationPermission != LocationPermissionState.Granted) {
                                askedLocationPermission = true
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                viewModel.selectMathArExperience(experience)
                            }
                        },
                        onFloatingTool = viewModel::selectFloatingTool,
                        onGraphColorMap = viewModel::setGraphColorMap,
                        onFunction3dTransform = viewModel::setFunction3dTransformMode,
                        onAnimationProgress = viewModel::setGraphAnimationProgress,
                        onAnimationMode = viewModel::setGraphAnimationMode,
                        onSlicePosition = viewModel::setGraphSlicePosition,
                        onCalibrationStep = viewModel::advancePaperGraphCalibration,
                        onPaperGraphLayer = viewModel::togglePaperGraphLayer,
                        onFeaturePhase = viewModel::selectFeaturePhase,
                        onToggleMathArFeature = viewModel::toggleMathArFeature,
                        onLiveEquation = viewModel::updateLiveEquation,
                        onGraphSlider = viewModel::updateArGraphSlider,
                        onGraphQuality = viewModel::setArGraphQualityPreset,
                        onGraphDomain = viewModel::updateArGraphDomain,
                        onComparisonEquation = viewModel::updateComparisonEquation,
                        onAnalysisFocus = viewModel::setArGraphAnalysisFocus,
                        onAddPointLabel = viewModel::addGeneratedPointLabel,
                        onAddRulerAnchor = viewModel::addRulerAnchor,
                        onCompareOffset = viewModel::setCompareOffset,
                        onGestureHandle = viewModel::selectGestureHandle,
                        onTransformStep = viewModel::setTransformHandleStep,
                        onApplyGestureHandle = viewModel::applySelectedGestureHandle,
                        onAddConstructionPoint = viewModel::addConstructionPointFromSelection,
                        onConstructionLine = viewModel::createConstructionLine,
                        onConstructionSegment = viewModel::createConstructionSegment,
                        onConstructionVector = viewModel::createConstructionVector,
                        onConstructionPlane = viewModel::createConstructionPlane,
                        onConstructionCircle = viewModel::createConstructionCircle,
                        onConstructionPolygon = viewModel::createConstructionPolygon,
                        onConstructionMidpoint = viewModel::createConstructionMidpoint,
                        onConstructionParallel = viewModel::createConstructionParallel,
                        onConstructionPerpendicular = viewModel::createConstructionPerpendicular,
                        onConstructionConstraint = viewModel::addConstructionConstraint,
                        onCapturePersistentAnchor = viewModel::capturePersistentAnchor,
                        onDepthOcclusionMode = viewModel::setDepthOcclusionMode,
                        onPerformanceProfile = viewModel::setPerformanceProfile,
                        onMeshDensity = viewModel::setMeshDensity,
                        onMaxSceneObjects = viewModel::setMaxSceneObjects,
                        onMarkScreenshotReady = viewModel::markScreenshotReady,
                        onExportArScene = viewModel::exportArScenePackage,
                        onApplyArTemplate = viewModel::applyArSceneTemplate,
                        onSelectArWorkflow = viewModel::selectArWorkflow,
                        onCompleteWorkflowStep = viewModel::completeWorkflowStep,
                        onAdvanceWorkflowStep = viewModel::advanceWorkflowStep,
                        onRefreshWorkflow = viewModel::refreshWorkflowEvaluation,
                        onExportArActivity = viewModel::exportArActivityPackage,
                        onCapture = viewModel::acknowledgeCapture,
                        onReplace = {
                            anchor?.detach()
                            anchor = null
                            viewModel.deleteObject()
                        },
                        onMove = {
                            moving = true
                            snackbarHostState.currentSnackbarData?.dismiss()
                        },
                        onRePlace = {
                            anchor?.detach()
                            anchor = null
                            moving = true
                            viewModel.requestRePlacement()
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
                        onTogglePlanes = viewModel::togglePlanes,
                        onSelectSceneObject = viewModel::selectSceneObject,
                        onExperienceMode = viewModel::setExperienceMode,
                        onMode = viewModel::setSceneInteractionMode,
                        onDuplicate = viewModel::duplicateSelected,
                        onLock = viewModel::toggleLockSelected,
                        onHide = viewModel::toggleHideSelected,
                        onGroup = viewModel::groupSelected,
                        onUngroup = viewModel::ungroupSelected,
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                        onInspector = viewModel::toggleInspector,
                        onDiagnostics = viewModel::toggleDiagnostics,
                        onLayers = viewModel::toggleLayers,
                        onSavedScenes = viewModel::toggleSavedScenes,
                        onSave = { viewModel.saveScene(saveName) },
                        onClearScene = {
                            anchor?.detach()
                            anchor = null
                            viewModel.clearScene()
                        }
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
    if (state.inspectorVisible) {
        ObjectInspectorSheet(
            state = state,
            onDismiss = viewModel::toggleInspector,
            onRename = viewModel::renameSelected,
            onParameter = viewModel::updateSelectedParameter,
            onReset = viewModel::resetTransform
        )
    }
    if (state.layersVisible) {
        SceneLayersSheet(
            state = state,
            onDismiss = viewModel::toggleLayers,
            onSelect = { viewModel.selectSceneObject(it, state.experienceMode == ExperienceMode.Advanced) },
            onHide = { viewModel.selectSceneObject(it); viewModel.toggleHideSelected() },
            onLock = { viewModel.selectSceneObject(it); viewModel.toggleLockSelected() },
            onDelete = { viewModel.selectSceneObject(it); viewModel.deleteObject() }
        )
    }
    if (state.savedScenesVisible) {
        SavedScenesSheet(
            state = state,
            saveName = saveName,
            onSaveName = { saveName = it },
            onDismiss = viewModel::toggleSavedScenes,
            onSave = { viewModel.saveScene(saveName) },
            onOpen = viewModel::loadScene,
            onDelete = viewModel::deleteSavedScene
        )
    }
}

@Composable
private fun ArRuntime(
    state: ArViewerUiState,
    anchor: Anchor?,
    moving: Boolean,
    onAnchorChanged: (Anchor) -> Unit,
    onPlaneStatus: (Boolean, PlacementHitKind) -> Unit,
    onTrackingStatus: (TrackingStatus, TrackingStatus, String) -> Unit,
    onSensorFusion: (ArSensorFusionResult) -> Unit,
    onPaperGraphFrame: (PaperGraphFrameState) -> Unit,
    onPaperGraphCalibrationTap: (PaperGraphCalibrationPoint) -> Unit,
    onPointLabelTap: (Float, Float, Float) -> Unit,
    onRulerAnchorTap: (Float, Float, Float) -> Unit,
    onPlacementPoint: (Float, Float, Float) -> Unit,
    onOutdoorGeospatialFrame: (OutdoorGeospatialFrameState) -> Unit,
    onPlacementMissed: () -> Unit,
    onRuntimeError: (String, Throwable) -> Unit
) {
    val context = LocalContext.current
    var latestFrame by remember { mutableStateOf<Frame?>(null) }
    var viewportWidth by remember { mutableStateOf(0) }
    var viewportHeight by remember { mutableStateOf(0) }
    var runtimeBlocked by remember { mutableStateOf(false) }
    var frameCounter by remember { mutableStateOf(0) }
    var lastReticleHitTestTimestamp by remember { mutableStateOf(0L) }
    var lastReticleHitKind by remember { mutableStateOf(PlacementHitKind.None) }
    var nativeSensorSample by remember { mutableStateOf<NativeArSensorSample?>(null) }
    var nativeSensorReceivedAtMs by remember { mutableStateOf(0L) }
    var semanticsSupported by remember { mutableStateOf(false) }
    var semanticsEnabled by remember { mutableStateOf(false) }
    var depthSupported by remember { mutableStateOf(false) }
    var depthEnabled by remember { mutableStateOf(false) }
    var lastOutdoorGeometryHit by remember { mutableStateOf<HitResult?>(null) }
    var outdoorMeshAnchor by remember { mutableStateOf<Anchor?>(null) }
    var outdoorWireframeEdges by remember { mutableStateOf<List<OutdoorWireframeEdge>>(emptyList()) }
    val outdoorMode = state.arEngineMode == ArEngineMode.OutdoorGeospatialMath
    val paperGraphMode = state.arEngineMode == ArEngineMode.PaperGraph
    val stablePlaneFrames = remember { mutableMapOf<Int, Int>() }
    val arLifecycleOwner = remember {
        object : LifecycleOwner {
            private val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.CREATED
            }

            override val lifecycle: Lifecycle
                get() = registry

            fun moveTo(state: Lifecycle.State) {
                registry.currentState = state
            }
        }
    }
    val poseSamples = remember { ArrayDeque<ArPoseSample>() }
    val fusionEngine = remember { ArSensorFusionEngine() }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    DisposableEffect(context) {
        val monitor = NativeArSensorMonitor(context.applicationContext) { sample ->
            nativeSensorSample = sample
            nativeSensorReceivedAtMs = SystemClock.elapsedRealtime()
        }
        monitor.start()
        onDispose { monitor.stop() }
    }

    LaunchedEffect(runtimeBlocked) {
        if (!runtimeBlocked) {
            delay(250)
            runCatching {
                Log.d(AR_LOG_TAG, "Resuming controlled AR lifecycle")
                arLifecycleOwner.moveTo(Lifecycle.State.RESUMED)
            }.onFailure {
                Log.e(AR_LOG_TAG, "ARCore resume failed from controlled lifecycle", it)
                runtimeBlocked = true
                onRuntimeError("ARCore sensor/session resume failed", it)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                Log.d(AR_LOG_TAG, "Destroying controlled AR lifecycle")
                arLifecycleOwner.moveTo(Lifecycle.State.DESTROYED)
                outdoorMeshAnchor?.detach()
            }.onFailure {
                Log.e(AR_LOG_TAG, "AR lifecycle destroy failed", it)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (runtimeBlocked) {
            FatalArRuntimePanel(
                title = "AR sensors could not start",
                body = "ARCore failed while starting device motion sensors. Close other camera/AR apps, restart the phone, then try again."
            )
            return@Box
        }
        ARSceneView(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    viewportWidth = it.width
                    viewportHeight = it.height
                },
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            lifecycle = arLifecycleOwner.lifecycle,
            planeRenderer = state.planesVisible,
            onSessionCreated = {
                Log.d(AR_LOG_TAG, "AR session created")
            },
            onSessionResumed = {
                Log.d(AR_LOG_TAG, "AR session resumed")
            },
            onSessionPaused = {
                Log.d(AR_LOG_TAG, "AR session paused")
            },
            onSessionFailed = {
                Log.e(AR_LOG_TAG, "AR session failed", it)
                runtimeBlocked = true
                onRuntimeError("AR session failed", it)
            },
            sessionConfiguration = { session: Session, config: Config ->
                runCatching {
                    config.planeFindingMode = when (state.arEngineMode) {
                        ArEngineMode.SurfacePlacement -> Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                        ArEngineMode.OutdoorGeospatialMath -> Config.PlaneFindingMode.DISABLED
                        ArEngineMode.PaperGraph -> Config.PlaneFindingMode.HORIZONTAL
                        else -> Config.PlaneFindingMode.HORIZONTAL
                    }
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.focusMode = Config.FocusMode.AUTO
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.instantPlacementMode = if (state.arEngineMode == ArEngineMode.SurfacePlacement || outdoorMode) {
                        Config.InstantPlacementMode.DISABLED
                    } else {
                        Config.InstantPlacementMode.LOCAL_Y_UP
                    }
                    depthSupported = runCatching { session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) }.getOrDefault(false)
                    depthEnabled = depthSupported
                    config.depthMode = if (depthEnabled) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                    val outdoorConfig = OutdoorGeospatialArEngine.configureSession(session, config, outdoorMode)
                    val paperConfig = AugmentedImageArEngine.configureSession(session, config, paperGraphMode)
                    depthEnabled = if (outdoorConfig.enabled) outdoorConfig.geospatialDepthEnabled else depthEnabled
                    semanticsSupported = runCatching { session.isSemanticModeSupported(Config.SemanticMode.ENABLED) }.getOrDefault(false)
                    semanticsEnabled = semanticsSupported
                    config.semanticMode = if (semanticsEnabled) Config.SemanticMode.ENABLED else Config.SemanticMode.DISABLED
                    Log.d(
                        AR_LOG_TAG,
                        "Session configured mode=${state.arEngineMode} planes=${config.planeFindingMode} depth=${config.depthMode}/supported=$depthSupported semantic=${config.semanticMode}/supported=$semanticsSupported instant=${config.instantPlacementMode} geospatial=${config.geospatialMode} streetscape=${config.streetscapeGeometryMode} imageDb=${paperConfig.enabled}/${paperConfig.referenceImageCount}"
                    )
                }.onFailure {
                    Log.e(AR_LOG_TAG, "AR session configuration failed", it)
                    depthEnabled = false
                    semanticsEnabled = false
                    config.depthMode = Config.DepthMode.DISABLED
                    config.semanticMode = Config.SemanticMode.DISABLED
                    onRuntimeError("AR session configuration failed", it)
                }
            },
            onSessionUpdated = { session, frame ->
                runCatching {
                    latestFrame = frame
                    frameCounter += 1
                    val cameraStatus = frame.camera.trackingState.toTrackingStatus()
                    val anchorStatus = anchor?.trackingState.toTrackingStatus()
                    val surfaceState = observeStableSurfaces(session, stablePlaneFrames)
                    if (outdoorMode) {
                        OutdoorGeospatialArEngine.observeFrame(session, frame, lastOutdoorGeometryHit).also(onOutdoorGeospatialFrame)
                    }
                    if (paperGraphMode) {
                        AugmentedImageArEngine.observeFrame(frame).also(onPaperGraphFrame)
                    }
                    val shouldProbeReticle = viewportWidth > 0 &&
                        viewportHeight > 0 &&
                        frame.camera.trackingState == TrackingState.TRACKING &&
                        frame.timestamp - lastReticleHitTestTimestamp > RETICLE_HIT_TEST_INTERVAL_NANOS
                    if (shouldProbeReticle) {
                        val centerHit = findBestPlacementHit(
                            frame = frame,
                            x = viewportWidth / 2f,
                            y = viewportHeight / 2f,
                            mode = state.arEngineMode,
                            allowFeaturePoint = true,
                            allowInstant = true
                        )
                        lastReticleHitKind = centerHit?.placementHitKind ?: PlacementHitKind.None
                        lastReticleHitTestTimestamp = frame.timestamp
                    } else if (frame.camera.trackingState != TrackingState.TRACKING) {
                        lastReticleHitKind = PlacementHitKind.None
                    } else {
                        lastReticleHitKind
                    }
                    val hitKind = lastReticleHitKind
                    val pose = frame.camera.pose
                    poseSamples.addLast(ArPoseSample(pose.tx(), pose.ty(), pose.tz(), frame.timestamp))
                    while (poseSamples.size > 18) poseSamples.removeFirst()
                    val agedNativeSensorSample = nativeSensorSample?.copy(
                        sensorAgeMillis = (SystemClock.elapsedRealtime() - nativeSensorReceivedAtMs).coerceAtLeast(0L)
                    )
                    val sceneUnderstanding = frame.readSceneUnderstanding(
                        semanticsSupported = semanticsSupported,
                        semanticsEnabled = semanticsEnabled,
                        depthSupported = depthSupported,
                        depthEnabled = depthEnabled,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight
                    )
                    val fusion = fusionEngine.evaluate(
                        ArSensorFusionInput(
                            cameraTracking = cameraStatus,
                            anchorTracking = anchorStatus,
                            placementHitKind = hitKind,
                            lightIntensity = agedNativeSensorSample?.ambientLightLux?.let { (it / 500f).coerceIn(0f, 1f) }
                                ?: frame.lightEstimate.pixelIntensity.takeIf { it.isFinite() },
                            poseSamples = poseSamples.toList(),
                            nativeSensor = agedNativeSensorSample,
                            sceneUnderstanding = sceneUnderstanding,
                            hasDepthSupport = sceneUnderstanding.depthEnabled,
                            hasPlacedObject = state.mathScene.objects.isNotEmpty()
                        )
                    )
                    if (frameCounter % 30 == 0) {
                        Log.d(
                            AR_LOG_TAG,
                            "Frame camera=$cameraStatus anchor=$anchorStatus hit=$hitKind score=${fusion.score} planes=${surfaceState.candidatePlaneCount} stable=${surfaceState.stablePlaneCount} native=${agedNativeSensorSample?.compactLog()} scene=${sceneUnderstanding.compactLog()}"
                        )
                    }
                    onPlaneStatus(hitKind != PlacementHitKind.None, hitKind)
                    onTrackingStatus(cameraStatus, anchorStatus, trackingGuidance(frame.camera.trackingState, frame.camera.trackingFailureReason, anchor?.trackingState))
                    onSensorFusion(fusion)
                }.onFailure {
                    Log.e(AR_LOG_TAG, "AR frame update failed", it)
                    onRuntimeError("AR frame update failed", it)
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { event: MotionEvent, _ ->
                    runCatching {
                        Log.d(AR_LOG_TAG, "Tap x=${event.x} y=${event.y} moving=$moving hasAnchor=${anchor != null} delay=${state.shouldDelayPlacement}")
                        val frame = latestFrame
                        if (frame == null) {
                            Log.w(AR_LOG_TAG, "Tap ignored because latest AR frame is null")
                            return@rememberOnGestureListener
                        }
                        val hit = findBestPlacementHit(
                            frame = frame,
                            x = event.x,
                            y = event.y,
                            mode = state.arEngineMode,
                            allowFeaturePoint = true,
                            allowInstant = true
                        )?.hit
                        if (anchor != null && !moving) {
                            val pose = hit?.hitPose ?: frame.camera.pose
                            when {
                                MathArFeature.MeasurementRulerAnchors in state.enabledMathArFeatures &&
                                    state.selectedFeaturePhase == ArFeaturePhase.DirectInteraction -> {
                                    onRulerAnchorTap(pose.tx(), pose.ty(), pose.tz())
                                }
                                MathArFeature.PointPicker in state.enabledMathArFeatures -> {
                                    onPointLabelTap(pose.tx(), pose.ty(), pose.tz())
                                }
                            }
                            return@rememberOnGestureListener
                        }
                        if (hit != null) {
                            Log.d(AR_LOG_TAG, "Creating anchor from hit trackable=${hit.trackable.javaClass.simpleName}")
                            if (paperGraphMode && state.paperGraph?.hasLockedTarget == true && !state.paperGraphCalibration.isComplete) {
                                val pose = hit.hitPose
                                onPaperGraphCalibrationTap(
                                    PaperGraphCalibrationPoint(
                                        screenX = event.x / viewportWidth.coerceAtLeast(1),
                                        screenY = event.y / viewportHeight.coerceAtLeast(1),
                                        worldX = pose.tx(),
                                        worldY = pose.ty(),
                                        worldZ = pose.tz()
                                    )
                                )
                                if (anchor == null) {
                                    onAnchorChanged(hit.createAnchor())
                                }
                                return@rememberOnGestureListener
                            }
                            lastOutdoorGeometryHit = hit.takeIf { it.trackable is StreetscapeGeometry }
                            val geometry = hit.trackable as? StreetscapeGeometry
                            if (geometry != null) {
                                outdoorMeshAnchor?.detach()
                                outdoorMeshAnchor = geometry.createAnchor(geometry.meshPose)
                                outdoorWireframeEdges = OutdoorGeospatialArEngine.wireframeEdges(geometry)
                            } else {
                                outdoorMeshAnchor?.detach()
                                outdoorMeshAnchor = null
                                outdoorWireframeEdges = emptyList()
                            }
                            hit.hitPose.let { pose -> onPlacementPoint(pose.tx(), pose.ty(), pose.tz()) }
                            onAnchorChanged(hit.createAnchor())
                        } else {
                            Log.d(AR_LOG_TAG, "Tap missed all placement hits")
                            onPlacementMissed()
                        }
                    }.onFailure {
                        Log.e(AR_LOG_TAG, "AR tap placement failed", it)
                        onRuntimeError("AR placement failed", it)
                    }
                }
            )
        ) {
            anchor?.let {
                AnchorNode(anchor = it) {
                    state.mathScene.objects.filter { objectState -> objectState.visibility.visible }.forEach { objectState ->
                        key(objectState.id, objectState.updatedAt) {
                            Node(
                                position = Position(
                                    objectState.transform.position.x.toFloat(),
                                    objectState.transform.position.y.toFloat(),
                                    objectState.transform.position.z.toFloat()
                                ),
                                rotation = Rotation(
                                    y = objectState.transform.rotation.y.toFloat() +
                                        if (objectState.objectType.isGraphLike() && state.graphAnimationEnabled && state.graphAnimationMode == GraphAnimationMode.RotateGraph) {
                                            state.graphAnimationProgress * 360f
                                        } else {
                                            0f
                                        }
                                ),
                                scale = Scale(objectState.transform.scale.x.toFloat()),
                                isEditable = !objectState.interactionState.locked
                            ) {
                                MathObjectNode(objectState.objectType, materialLoader, state)
                                if (objectState.interactionState.selected) SelectionHighlight(materialLoader)
                                if (objectState.interactionState.selected && MathArFeature.GestureHandles in state.enabledMathArFeatures) {
                                    GestureHandleNodes(materialLoader)
                                }
                                if (objectState.interactionState.selected && state.compareModeEnabled) {
                                    CompareGhostNode(objectState.objectType, materialLoader, state)
                                }
                            }
                        }
                    }
                    ConstructionGeometryNodes(state, materialLoader)
                }
            }
            outdoorMeshAnchor?.let { meshAnchor ->
                AnchorNode(anchor = meshAnchor) {
                    OutdoorStreetscapeWireframe(outdoorWireframeEdges, materialLoader)
                }
            }
        }
        Reticle(state)
        TrackingHealthOverlay(state)
        MathInteractionOverlay(state)
    }
}

@Composable
private fun NodeScope.MathObjectNode(type: MathObjectType, materialLoader: MaterialLoader, state: ArViewerUiState) {
    val cyan = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF4EE7FF)) }
    val teal = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAA4FD1C5), roughness = 0.35f) }
    val amber = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val rose = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAAFF6B8A), roughness = 0.45f) }
    val violet = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAA9B8CFF), roughness = 0.45f) }
    val graphPrimary = remember(materialLoader, state.graphColorMap) { materialLoader.createUnlitColorInstance(state.graphColorMap.primaryColor()) }
    val graphSecondary = remember(materialLoader, state.graphColorMap) { materialLoader.createUnlitColorInstance(state.graphColorMap.secondaryColor()) }
    val graphPalette = remember(materialLoader, state.graphColorMap) {
        state.graphColorMap.palette().map { materialLoader.createUnlitColorInstance(it) }
    }
    when (type) {
        MathObjectType.Cube -> {
            CubeNode(size = Size(0.24f, 0.24f, 0.24f), center = Position(0f, 0.12f, 0f), materialInstance = teal)
            CubeEdges(cyan)
        }
        MathObjectType.CoordinatePlane -> CoordinatePlaneNode(graphPrimary, graphSecondary)
        MathObjectType.SineCurve -> SineCurveNode(graphPrimary, graphSecondary, graphPalette, state)
        MathObjectType.Sphere -> SphereApproxNode(cyan, teal)
        MathObjectType.Cylinder -> CylinderApproxNode(cyan, teal)
        MathObjectType.Cone -> ConeApproxNode(cyan, amber)
        MathObjectType.RectangularPrism -> {
            CubeNode(size = Size(0.34f, 0.22f, 0.18f), center = Position(0f, 0.11f, 0f), materialInstance = violet)
            BoxEdges(cyan, 0.17f, 0.22f, 0.09f)
        }
        MathObjectType.Triangle -> TriangleNode(cyan, rose)
        MathObjectType.Circle -> CircleNode(cyan, teal)
        MathObjectType.NumberLine -> NumberLineNode(cyan, amber)
        MathObjectType.VectorArrow -> VectorArrowNode(cyan, amber)
    }
}

private fun MathObjectType.isGraphLike(): Boolean =
    this in setOf(MathObjectType.SineCurve, MathObjectType.CoordinatePlane, MathObjectType.NumberLine, MathObjectType.VectorArrow)

private fun GraphColorMap.palette(): List<Color> = when (this) {
    GraphColorMap.Height -> listOf(
        Color(0xFF14213D),
        Color(0xFF1B9AAA),
        Color(0xFF3DFF9F),
        Color(0xFFFFD166),
        Color(0xFFFF4D6D)
    )
    GraphColorMap.Slope -> listOf(
        Color(0xFF0B132B),
        Color(0xFF5BC0BE),
        Color(0xFFFFF275),
        Color(0xFFFF8C42),
        Color(0xFFE71D36)
    )
    GraphColorMap.Curvature -> listOf(
        Color(0xFF240046),
        Color(0xFF7B2CBF),
        Color(0xFFFF6DCD),
        Color(0xFFFFD6FF),
        Color(0xFF4CC9F0)
    )
    GraphColorMap.XValue -> listOf(
        Color(0xFF003049),
        Color(0xFF118AB2),
        Color(0xFF06D6A0),
        Color(0xFFFFD166),
        Color(0xFFEF476F)
    )
    GraphColorMap.YValue -> listOf(
        Color(0xFF2D00F7),
        Color(0xFF6A00F4),
        Color(0xFFB100E8),
        Color(0xFFFF6D00),
        Color(0xFFFFEA00)
    )
}

private fun GraphColorMap.primaryColor(): Color = palette()[2]

private fun GraphColorMap.secondaryColor(): Color = palette()[4]

private data class PlacementCandidate(val hit: HitResult, val placementHitKind: PlacementHitKind)

private data class SurfaceUnderstandingState(
    val hasCandidatePlane: Boolean,
    val hasStablePlane: Boolean,
    val candidatePlaneCount: Int,
    val stablePlaneCount: Int
)

private fun Frame.readSceneUnderstanding(
    semanticsSupported: Boolean,
    semanticsEnabled: Boolean,
    depthSupported: Boolean,
    depthEnabled: Boolean,
    viewportWidth: Int,
    viewportHeight: Int
): ArSceneUnderstandingSample {
    val depthSample = readCenterDepthSample(depthEnabled, viewportWidth, viewportHeight)
    if (!semanticsEnabled) {
        return ArSceneUnderstandingSample(
            semanticsSupported = semanticsSupported,
            semanticsEnabled = false,
            semanticsAvailable = false,
            depthSupported = depthSupported,
            depthEnabled = depthEnabled,
            depthImageAvailable = depthSample.available,
            centerDepthMeters = depthSample.centerDepthMeters
        )
    }
    val sky = semanticFractionOrNull(SemanticLabel.SKY)
    val building = semanticFractionOrNull(SemanticLabel.BUILDING)
    val tree = semanticFractionOrNull(SemanticLabel.TREE)
    val road = semanticFractionOrNull(SemanticLabel.ROAD)
    val sidewalk = semanticFractionOrNull(SemanticLabel.SIDEWALK)
    val terrain = semanticFractionOrNull(SemanticLabel.TERRAIN)
    val structure = semanticFractionOrNull(SemanticLabel.STRUCTURE)
    val water = semanticFractionOrNull(SemanticLabel.WATER)
    val vehicle = semanticFractionOrNull(SemanticLabel.VEHICLE)
    val person = semanticFractionOrNull(SemanticLabel.PERSON)
    val objectLabel = semanticFractionOrNull(SemanticLabel.OBJECT)
    val available = listOf(sky, building, tree, road, sidewalk, terrain, structure, water, vehicle, person, objectLabel).any { it != null }
    return ArSceneUnderstandingSample(
        semanticsSupported = semanticsSupported,
        semanticsEnabled = semanticsEnabled,
        semanticsAvailable = available,
        depthSupported = depthSupported,
        depthEnabled = depthEnabled,
        depthImageAvailable = depthSample.available,
        centerDepthMeters = depthSample.centerDepthMeters,
        skyFraction = sky ?: 0f,
        buildingFraction = building ?: 0f,
        treeFraction = tree ?: 0f,
        roadFraction = road ?: 0f,
        sidewalkFraction = sidewalk ?: 0f,
        terrainFraction = terrain ?: 0f,
        structureFraction = structure ?: 0f,
        waterFraction = water ?: 0f,
        vehicleFraction = vehicle ?: 0f,
        personFraction = person ?: 0f,
        objectFraction = objectLabel ?: 0f
    )
}

private data class DepthFrameSample(
    val available: Boolean,
    val centerDepthMeters: Float?
)

private fun Frame.readCenterDepthSample(
    depthEnabled: Boolean,
    viewportWidth: Int,
    viewportHeight: Int
): DepthFrameSample {
    if (!depthEnabled || viewportWidth <= 0 || viewportHeight <= 0) return DepthFrameSample(false, null)
    return try {
        acquireDepthImage16Bits().use { depthImage ->
            val depthCoordinates = depthCoordinatesForScreenCenter(depthImage, viewportWidth, viewportHeight)
            val millimeters = depthCoordinates?.let { (x, y) -> depthMillimetersAt(depthImage, x, y) } ?: 0
            DepthFrameSample(
                available = true,
                centerDepthMeters = millimeters.takeIf { it > 0 }?.let { it / 1000f }
            )
        }
    } catch (_: NotYetAvailableException) {
        DepthFrameSample(false, null)
    } catch (error: RuntimeException) {
        Log.d(AR_LOG_TAG, "Depth image unavailable: ${error.message}")
        DepthFrameSample(false, null)
    }
}

private fun Frame.depthCoordinatesForScreenCenter(
    depthImage: Image,
    viewportWidth: Int,
    viewportHeight: Int
): Pair<Int, Int>? {
    val textureCoordinates = FloatArray(2)
    transformCoordinates2d(
        Coordinates2d.VIEW,
        floatArrayOf(viewportWidth / 2f, viewportHeight / 2f),
        Coordinates2d.TEXTURE_NORMALIZED,
        textureCoordinates
    )
    val u = textureCoordinates[0]
    val v = textureCoordinates[1]
    if (u !in 0f..1f || v !in 0f..1f) return null
    val x = (u * depthImage.width).toInt().coerceIn(0, depthImage.width - 1)
    val y = (v * depthImage.height).toInt().coerceIn(0, depthImage.height - 1)
    return x to y
}

private fun depthMillimetersAt(depthImage: Image, x: Int, y: Int): Int {
    val plane = depthImage.planes[0]
    val byteIndex = x * plane.pixelStride + y * plane.rowStride
    val buffer = plane.buffer.order(ByteOrder.nativeOrder())
    if (byteIndex < 0 || byteIndex + 1 >= buffer.limit()) return 0
    return buffer.getShort(byteIndex).toInt() and 0xFFFF
}

private fun Frame.semanticFractionOrNull(label: SemanticLabel): Float? =
    try {
        getSemanticLabelFraction(label)
    } catch (_: NotYetAvailableException) {
        null
    } catch (error: RuntimeException) {
        Log.d(AR_LOG_TAG, "Scene semantics fraction unavailable for $label: ${error.message}")
        null
    }

private fun ArSceneUnderstandingSample.compactLog(): String =
    "sem=$semanticsEnabled/$semanticsAvailable depth=$depthEnabled/$depthImageAvailable center=${centerDepthMeters?.let { "%.2f".format(it) }}m surface=${(placementSurfaceFraction * 100f).toInt()}% obstacle=${(dynamicObstacleFraction * 100f).toInt()}% outdoor=${(outdoorContextFraction * 100f).toInt()}%"

private fun observeStableSurfaces(
    session: Session,
    stablePlaneFrames: MutableMap<Int, Int>
): SurfaceUnderstandingState {
    val planes = session.getAllTrackables(Plane::class.java)
        .filter { plane ->
            plane.trackingState == TrackingState.TRACKING &&
                plane.subsumedBy == null &&
                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                plane.extentX >= MIN_PLANE_EXTENT_METERS &&
                plane.extentZ >= MIN_PLANE_EXTENT_METERS
        }
    val activeKeys = planes.map { System.identityHashCode(it) }.toSet()
    stablePlaneFrames.keys.retainAll(activeKeys)
    planes.forEach { plane ->
        val key = System.identityHashCode(plane)
        stablePlaneFrames[key] = (stablePlaneFrames[key] ?: 0) + 1
    }
    val stableCount = planes.count { plane ->
        (stablePlaneFrames[System.identityHashCode(plane)] ?: 0) >= MIN_STABLE_PLANE_FRAMES
    }
    return SurfaceUnderstandingState(
        hasCandidatePlane = planes.isNotEmpty(),
        hasStablePlane = stableCount > 0,
        candidatePlaneCount = planes.size,
        stablePlaneCount = stableCount
    )
}

private fun findBestPlacementHit(
    frame: Frame,
    x: Float,
    y: Float,
    mode: ArEngineMode,
    allowFeaturePoint: Boolean,
    allowInstant: Boolean
): PlacementCandidate? {
    if (mode == ArEngineMode.OutdoorGeospatialMath) {
        return OutdoorGeospatialArEngine.geometryHit(frame, x, y)
            ?.let { PlacementCandidate(it, PlacementHitKind.StreetscapeGeometry) }
    }
    val trackedHits = runCatching { frame.hitTest(x, y) }.getOrDefault(emptyList())
    val planeHit = trackedHits.firstOrNull { result ->
        val plane = result.trackable as? Plane
        plane != null &&
            plane.trackingState == TrackingState.TRACKING &&
            (mode == ArEngineMode.SurfacePlacement || plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING) &&
            plane.subsumedBy == null &&
            plane.extentX >= MIN_PLANE_EXTENT_METERS &&
            plane.extentZ >= MIN_PLANE_EXTENT_METERS &&
            plane.isPoseInPolygon(result.hitPose)
    }?.let { PlacementCandidate(it, PlacementHitKind.Plane) }
    if (planeHit != null) return planeHit

    val depthHit = trackedHits.firstOrNull { result ->
        (result.trackable as? DepthPoint)?.trackingState == TrackingState.TRACKING
    }?.let { PlacementCandidate(it, PlacementHitKind.DepthPoint) }
    if (depthHit != null) return depthHit

    if (!allowFeaturePoint || mode == ArEngineMode.SurfacePlacement) return null
    val pointHit = trackedHits.firstOrNull { result ->
        val point = result.trackable as? Point
        point != null &&
            point.trackingState == TrackingState.TRACKING &&
            point.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
    }?.let { PlacementCandidate(it, PlacementHitKind.FeaturePoint) }
    if (pointHit != null) return pointHit

    if (!allowInstant || mode == ArEngineMode.SurfacePlacement || frame.camera.trackingState != TrackingState.TRACKING) return null
    return runCatching {
        frame.hitTestInstantPlacement(x, y, APPROXIMATE_PLACEMENT_DISTANCE_METERS)
            .firstOrNull { result ->
                (result.trackable as? InstantPlacementPoint)?.trackingState == TrackingState.TRACKING
            }
            ?.let { PlacementCandidate(it, PlacementHitKind.Instant) }
    }.getOrNull()
}

private fun TrackingState?.toTrackingStatus(): TrackingStatus = when (this) {
    TrackingState.TRACKING -> TrackingStatus.Tracking
    TrackingState.PAUSED -> TrackingStatus.Limited
    TrackingState.STOPPED -> TrackingStatus.Paused
    null -> TrackingStatus.Unknown
}

private fun trackingGuidance(cameraState: TrackingState, reason: TrackingFailureReason, anchorState: TrackingState?): String =
    when {
        cameraState == TrackingState.TRACKING && anchorState == TrackingState.TRACKING -> "Tracking stable."
        cameraState == TrackingState.TRACKING && anchorState == null -> "Tracking ready."
        cameraState == TrackingState.TRACKING && anchorState == TrackingState.PAUSED -> "Object anchor is recovering. Keep the phone pointed at the same area."
        cameraState == TrackingState.TRACKING && anchorState == TrackingState.STOPPED -> "Object anchor was lost. Use Move to place it again."
        reason == TrackingFailureReason.INSUFFICIENT_LIGHT -> "Tracking limited: add more light and avoid shadows."
        reason == TrackingFailureReason.EXCESSIVE_MOTION -> "Tracking limited: move the phone more slowly."
        reason == TrackingFailureReason.INSUFFICIENT_FEATURES -> "Tracking limited: aim at textured edges, corners, or a printed surface."
        reason == TrackingFailureReason.CAMERA_UNAVAILABLE -> "Camera unavailable. Close other camera apps and reopen AR."
        cameraState == TrackingState.PAUSED -> "Tracking limited. Move slowly and keep the surface in view."
        cameraState == TrackingState.STOPPED -> "Tracking stopped. Reopen the AR view."
        else -> "Initializing AR tracking."
    }

private const val APPROXIMATE_PLACEMENT_DISTANCE_METERS = 0.8f

@Composable
private fun NodeScope.SelectionHighlight(materialLoader: MaterialLoader) {
    val material = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFF176)) }
    BoxEdges(material, 0.2f, 0.28f, 0.2f)
}

@Composable
private fun NodeScope.GestureHandleNodes(materialLoader: MaterialLoader) {
    val rotate = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val scale = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF3DFF9F)) }
    val lift = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF42A5F5)) }
    CubeNode(Size(0.026f, 0.026f, 0.026f), center = Position(0.22f, 0.12f, 0f), materialInstance = rotate)
    CubeNode(Size(0.026f, 0.026f, 0.026f), center = Position(0f, 0.28f, 0f), materialInstance = lift)
    CubeNode(Size(0.026f, 0.026f, 0.026f), center = Position(-0.22f, 0.12f, 0f), materialInstance = scale)
    LineNode(Position(0f, 0.12f, 0f), Position(0.22f, 0.12f, 0f), rotate)
    LineNode(Position(0f, 0.12f, 0f), Position(0f, 0.28f, 0f), lift)
    LineNode(Position(0f, 0.12f, 0f), Position(-0.22f, 0.12f, 0f), scale)
}

@Composable
private fun NodeScope.CompareGhostNode(type: MathObjectType, materialLoader: MaterialLoader, state: ArViewerUiState) {
    val ghost = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x886EDBFF)) }
    Node(position = Position(state.compareOffsetMeters, 0f, 0f), scale = Scale(0.82f)) {
        when (type) {
            MathObjectType.SineCurve -> CoordinatePlaneNode(ghost, ghost)
            MathObjectType.Cube, MathObjectType.RectangularPrism -> BoxEdges(ghost, 0.14f, 0.18f, 0.08f)
            MathObjectType.Sphere -> CircleLines(0.12f, 0.12f, ghost, y = 0.12f)
            else -> CoordinatePlaneNode(ghost, ghost)
        }
    }
}

@Composable
private fun NodeScope.ConstructionGeometryNodes(state: ArViewerUiState, materialLoader: MaterialLoader) {
    if (MathArFeature.MultiObjectConstraints !in state.enabledMathArFeatures && state.resolvedConstructions.isEmpty()) return
    val pointMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFF176)) }
    val lineMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF6EDBFF)) }
    val constraintMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFF8A65)) }
    state.constructionGeometry.points.forEach { point ->
        CubeNode(
            size = Size(0.018f, 0.018f, 0.018f),
            center = point.position.toPosition().copy(y = point.position.y.toFloat() + 0.024f),
            materialInstance = pointMaterial
        )
    }
    state.resolvedConstructions.filter { it.visible }.forEach { construction ->
        val material = if (construction.kind in setOf(ConstructionObjectKind.Parallel, ConstructionObjectKind.Perpendicular)) constraintMaterial else lineMaterial
        when (construction.kind) {
            ConstructionObjectKind.Line,
            ConstructionObjectKind.Segment,
            ConstructionObjectKind.Ray,
            ConstructionObjectKind.Vector,
            ConstructionObjectKind.Parallel,
            ConstructionObjectKind.Perpendicular -> {
                if (construction.points.size >= 2) {
                    LineNode(construction.points[0].toPosition(), construction.points[1].toPosition(), material)
                    if (construction.kind == ConstructionObjectKind.Vector) {
                        VectorHead(construction.points[0].toPosition(), construction.points[1].toPosition(), material)
                    }
                }
            }
            ConstructionObjectKind.Plane,
            ConstructionObjectKind.Polygon -> {
                construction.points.zipWithNext().forEach { (a, b) -> LineNode(a.toPosition(), b.toPosition(), material) }
                if (construction.points.size > 2) LineNode(construction.points.last().toPosition(), construction.points.first().toPosition(), material)
            }
            ConstructionObjectKind.Circle -> {
                if (construction.points.size >= 2) {
                    val center = construction.points[0]
                    val edge = construction.points[1]
                    val radius = sqrt((center.x - edge.x) * (center.x - edge.x) + (center.z - edge.z) * (center.z - edge.z)).toFloat()
                    val ring = (0..48).map { index ->
                        val angle = index * (2f * PI.toFloat() / 48f)
                        Position(center.x.toFloat() + cos(angle) * radius, center.y.toFloat() + 0.018f, center.z.toFloat() + sin(angle) * radius)
                    }
                    ring.zipWithNext().forEach { (a, b) -> LineNode(a, b, material) }
                }
            }
            ConstructionObjectKind.Midpoint,
            ConstructionObjectKind.Point,
            ConstructionObjectKind.Intersection -> {
                construction.points.forEach { point ->
                    CubeNode(Size(0.022f, 0.022f, 0.022f), center = point.toPosition().copy(y = point.y.toFloat() + 0.028f), materialInstance = constraintMaterial)
                }
            }
        }
    }
}

@Composable
private fun NodeScope.VectorHead(start: Position, end: Position, material: MaterialInstance) {
    val dx = end.x - start.x
    val dz = end.z - start.z
    val length = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
    val ux = dx / length
    val uz = dz / length
    val size = 0.035f
    val left = Position(end.x - ux * size - uz * size * 0.45f, end.y, end.z - uz * size + ux * size * 0.45f)
    val right = Position(end.x - ux * size + uz * size * 0.45f, end.y, end.z - uz * size - ux * size * 0.45f)
    LineNode(end, left, material)
    LineNode(end, right, material)
}

private fun Vector3Value.toPosition(): Position = Position(x.toFloat(), y.toFloat() + 0.018f, z.toFloat())

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
private fun NodeScope.SineCurveNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState
) {
    val phase = if (state.graphAnimationEnabled && state.graphAnimationMode == GraphAnimationMode.AnimateSineWave) {
        state.graphAnimationProgress * 2f * PI.toFloat()
    } else {
        0f
    }
    if (state.mathArExperience == MathArExperience.MarkerBasedGraph) {
        PaperGraphWorksheetNode(lineMaterial, accent, palette, state, phase)
        return
    }
    LineNode(Position(-0.26f, 0.004f, 0f), Position(0.26f, 0.004f, 0f), accent)
    LineNode(Position(0f, 0.004f, -0.12f), Position(0f, 0.004f, 0.12f), accent)
    if (state.mathArExperience == MathArExperience.Drawing2dTo3d) {
        Function3dTransformNode(lineMaterial, accent, palette, state, phase)
        return
    }
    val progress = if (state.graphAnimationEnabled && state.graphAnimationMode in setOf(GraphAnimationMode.SweepArea, GraphAnimationMode.BuildVolume)) {
        state.graphAnimationProgress.coerceIn(0.08f, 1f)
    } else {
        1f
    }
    val samples = functionXSamples(state, 64, progress)
    samples.zipWithNext().forEachIndexed { index, (a, b) ->
        val ay = functionValue(state, a, phase)
        val by = functionValue(state, b, phase)
        val material = graphSegmentMaterial(state, index, a, ay, by, palette)
        LineNode(
            start = Position(graphX(state, a), 0.012f, ay * 0.08f),
            end = Position(graphX(state, b), 0.012f, by * 0.08f),
            materialInstance = material
        )
    }
    if (MathArFeature.RootVisualizer in state.enabledMathArFeatures) {
        state.arGraphAnalysis.highlightedPoints.forEach { point ->
            CubeNode(
                Size(0.018f, 0.018f, 0.018f),
                center = Position(graphX(state, point.x.toFloat()), 0.028f, point.y.toFloat().coerceIn(-1.6f, 1.6f) * 0.08f),
                materialInstance = accent
            )
        }
    }
    if (MathArFeature.AreaUnderCurve in state.enabledMathArFeatures) {
        samples.filterIndexed { index, sample -> index % 8 == 0 && functionValue(state, sample, phase) > 0f }.forEach { sample ->
            val x = graphX(state, sample)
            val z = functionValue(state, sample, phase) * 0.08f
            LineNode(Position(x, 0.012f, 0f), Position(x, 0.012f, z), accent)
        }
    }
    if (state.graphAnimationEnabled && state.graphAnimationMode == GraphAnimationMode.BuildVolume || MathArFeature.VolumeBuilder in state.enabledMathArFeatures) {
        samples.filterIndexed { index, _ -> index % 8 == 0 }.forEach { sample ->
            val x = graphX(state, sample)
            val z = functionValue(state, sample, phase) * 0.08f
            LineNode(Position(x, 0.012f, z), Position(x, 0.12f * state.graphAnimationProgress, z), accent)
        }
    }
    if (state.graphAnimationEnabled && state.graphAnimationMode == GraphAnimationMode.MoveTangentPoint || MathArFeature.TangentNormalTool in state.enabledMathArFeatures) {
        val tangent = state.arGraphAnalysis.tangent
        val xValue = tangent?.point?.x?.toFloat()
            ?: (state.arGraphDomain.xMin.toFloat() + (state.arGraphDomain.xMax - state.arGraphDomain.xMin).toFloat() * state.graphAnimationProgress)
        val yValue = tangent?.point?.y?.toFloat() ?: functionValue(state, xValue, phase)
        val x = graphX(state, xValue)
        val z = yValue * 0.08f
        val slope = tangent?.tangentSlope?.toFloat()?.coerceIn(-8f, 8f) ?: derivativeValue(state, xValue, phase)
        val normalSlope = tangent?.normalSlope?.toFloat()?.takeIf { it.isFinite() }?.coerceIn(-8f, 8f) ?: -1f / slope.coerceAwayFromZero()
        CubeNode(Size(0.028f, 0.028f, 0.028f), center = Position(x, 0.03f, z), materialInstance = accent)
        LineNode(Position(x - 0.08f, 0.026f, z - slope * 0.08f), Position(x + 0.08f, 0.026f, z + slope * 0.08f), accent)
        LineNode(Position(x - 0.045f, 0.026f, z - normalSlope * 0.045f), Position(x + 0.045f, 0.026f, z + normalSlope * 0.045f), lineMaterial)
    }
}

@Composable
private fun NodeScope.PaperGraphWorksheetNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    if (PaperGraphLayer.Axes in state.paperGraphLayers) {
        LineNode(Position(-0.26f, 0.006f, 0f), Position(0.26f, 0.006f, 0f), accent)
        LineNode(Position(0f, 0.006f, -0.15f), Position(0f, 0.006f, 0.15f), accent)
        calibrationMarkers(state, accent)
    }
    if (PaperGraphLayer.Scale in state.paperGraphLayers) {
        (-5..5).forEach { tick ->
            val x = tick * 0.048f
            LineNode(Position(x, 0.008f, -0.008f), Position(x, 0.008f, 0.008f), lineMaterial)
            val z = tick * 0.028f
            LineNode(Position(-0.008f, 0.008f, z), Position(0.008f, 0.008f, z), lineMaterial)
        }
    }
    if (PaperGraphLayer.Graph in state.paperGraphLayers) {
        val samples = functionXSamples(state, 64, 1f)
        samples.zipWithNext().forEachIndexed { index, (a, b) ->
            LineNode(
                Position(graphX(state, a), 0.018f, functionValue(state, a, phase) * 0.085f),
                Position(graphX(state, b), 0.018f, functionValue(state, b, phase) * 0.085f),
                graphSegmentMaterial(state, index, a, functionValue(state, a, phase), functionValue(state, b, phase), palette)
            )
        }
    }
    if (PaperGraphLayer.Surface3d in state.paperGraphLayers) {
        FunctionSurfaceNode(lineMaterial, accent, palette, state.copy(graphAnimationProgress = 1f), phase)
    }
    if (PaperGraphLayer.CrossSection in state.paperGraphLayers) {
        FunctionCrossSectionNode(lineMaterial, accent, palette, state.copy(graphAnimationProgress = 1f), phase)
    }
}

@Composable
private fun NodeScope.calibrationMarkers(
    state: ArViewerUiState,
    accent: MaterialInstance
) {
    val calibration = state.paperGraphCalibration
    if (calibration.originLocked) {
        CubeNode(Size(0.018f, 0.018f, 0.018f), center = Position(0f, 0.025f, 0f), materialInstance = accent)
    }
    if (calibration.xAxisLocked) {
        CubeNode(Size(0.016f, 0.016f, 0.016f), center = Position(0.12f, 0.025f, 0f), materialInstance = accent)
        LineNode(Position(0f, 0.018f, 0f), Position(0.12f, 0.018f, 0f), accent)
    }
    if (calibration.yAxisLocked) {
        CubeNode(Size(0.016f, 0.016f, 0.016f), center = Position(0f, 0.025f, 0.09f), materialInstance = accent)
        LineNode(Position(0f, 0.018f, 0f), Position(0f, 0.018f, 0.09f), accent)
    }
}

@Composable
private fun NodeScope.Function3dTransformNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    when (state.function3dTransformMode) {
        Function3dTransformMode.Surface -> FunctionSurfaceNode(lineMaterial, accent, palette, state, phase)
        Function3dTransformMode.Extrusion -> FunctionExtrusionNode(lineMaterial, accent, palette, state, phase)
        Function3dTransformMode.SolidOfRevolution -> FunctionRevolutionNode(lineMaterial, accent, palette, state, phase)
        Function3dTransformMode.TangentPlane -> FunctionTangentPlaneNode(lineMaterial, accent, palette, state, phase)
        Function3dTransformMode.CrossSectionSlices -> FunctionCrossSectionNode(lineMaterial, accent, palette, state, phase)
    }
}

@Composable
private fun NodeScope.FunctionSurfaceNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    val progress = transformProgress(state)
    val xSamples = functionXSamples(state, 36, progress)
    val zSamples = (-4..4).map { it * 0.032f }
    zSamples.forEachIndexed { zIndex, z ->
        xSamples.zipWithNext().forEachIndexed { xIndex, (a, b) ->
            val ya = functionValue(state, a, phase) + z * 2.5f
            val yb = functionValue(state, b, phase) + z * 2.5f
            val material = graphSegmentMaterial(state, xIndex + zIndex, a, ya, yb, palette)
            LineNode(
                graphPoint(state, a, z, phase),
                graphPoint(state, b, z, phase),
                material
            )
        }
    }
    xSamples.filterIndexed { index, _ -> index % 4 == 0 }.forEach { x ->
        zSamples.zipWithNext().forEach { (a, b) ->
            LineNode(graphPoint(state, x, a, phase), graphPoint(state, x, b, phase), accent)
        }
    }
}

@Composable
private fun NodeScope.FunctionExtrusionNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    val xSamples = functionXSamples(state, 42, transformProgress(state))
    val depths = listOf(-0.07f, 0.07f)
    depths.forEach { z ->
        xSamples.zipWithNext().forEachIndexed { index, (a, b) ->
            LineNode(graphPoint(state, a, z, phase), graphPoint(state, b, z, phase), graphSegmentMaterial(state, index, a, functionValue(state, a, phase), functionValue(state, b, phase), palette))
        }
    }
    xSamples.filterIndexed { index, _ -> index % 3 == 0 }.forEach { x ->
        val y = functionHeight(state, x, phase)
        depths.forEach { z ->
            LineNode(Position(graphX(state, x), 0.012f, z), Position(graphX(state, x), y, z), accent)
        }
        LineNode(Position(graphX(state, x), y, depths.first()), Position(graphX(state, x), y, depths.last()), lineMaterial)
    }
}

@Composable
private fun NodeScope.FunctionRevolutionNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    val xSamples = functionXSamples(state, 26, transformProgress(state))
    val angleSamples = (0..16).map { it * (2f * PI.toFloat() / 16f) }
    xSamples.forEachIndexed { index, x ->
        val radius = 0.025f + abs(functionValue(state, x, phase)) * 0.085f
        angleSamples.zipWithNext().forEach { (a, b) ->
            LineNode(
                Position(graphX(state, x), 0.09f + radius * cos(a), radius * sin(a)),
                Position(graphX(state, x), 0.09f + radius * cos(b), radius * sin(b)),
                graphSegmentMaterial(state, index, x, radius, radius * cos(a), palette)
            )
        }
    }
    angleSamples.filterIndexed { index, _ -> index % 4 == 0 }.forEach { angle ->
        xSamples.zipWithNext().forEach { (a, b) ->
            val ra = 0.025f + abs(functionValue(state, a, phase)) * 0.085f
            val rb = 0.025f + abs(functionValue(state, b, phase)) * 0.085f
            LineNode(
                Position(graphX(state, a), 0.09f + ra * cos(angle), ra * sin(angle)),
                Position(graphX(state, b), 0.09f + rb * cos(angle), rb * sin(angle)),
                accent
            )
        }
    }
}

@Composable
private fun NodeScope.FunctionTangentPlaneNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    FunctionSurfaceNode(lineMaterial, accent, palette, state.copy(graphAnimationProgress = 1f), phase)
    val x0 = state.arGraphDomain.xMin.toFloat() + (state.arGraphDomain.xMax - state.arGraphDomain.xMin).toFloat() * state.graphAnimationProgress.coerceIn(0f, 1f)
    val f0 = functionHeight(state, x0, phase)
    val slope = derivativeValue(state, x0, phase) * 0.045f
    val xOffsets = (-3..3).map { it * 0.026f }
    val zOffsets = (-3..3).map { it * 0.026f }
    xOffsets.forEach { dx ->
        zOffsets.zipWithNext().forEach { (za, zb) ->
            LineNode(tangentPlanePoint(state, x0, f0, slope, dx, za), tangentPlanePoint(state, x0, f0, slope, dx, zb), accent)
        }
    }
    zOffsets.forEach { z ->
        xOffsets.zipWithNext().forEach { (a, b) ->
            LineNode(tangentPlanePoint(state, x0, f0, slope, a, z), tangentPlanePoint(state, x0, f0, slope, b, z), accent)
        }
    }
    CubeNode(Size(0.022f, 0.022f, 0.022f), center = Position(graphX(state, x0), f0, 0f), materialInstance = accent)
}

@Composable
private fun NodeScope.FunctionCrossSectionNode(
    lineMaterial: MaterialInstance,
    accent: MaterialInstance,
    palette: List<MaterialInstance>,
    state: ArViewerUiState,
    phase: Float
) {
    FunctionSurfaceNode(lineMaterial, accent, palette, state.copy(graphAnimationProgress = 1f), phase)
    val z = -0.128f + 0.256f * state.graphSlicePosition.coerceIn(0f, 1f)
    val xSamples = functionXSamples(state, 42, 1f)
    xSamples.zipWithNext().forEach { (a, b) ->
        LineNode(graphPoint(state, a, z, phase), graphPoint(state, b, z, phase), accent)
    }
    xSamples.filterIndexed { index, _ -> index % 6 == 0 }.forEach { x ->
        LineNode(Position(graphX(state, x), 0.012f, z), graphPoint(state, x, z, phase), accent)
    }
    LineNode(Position(-0.24f, 0.012f, z), Position(0.24f, 0.012f, z), accent)
}

private fun functionXSamples(state: ArViewerUiState, segments: Int, progress: Float): List<Float> {
    val rangeMin = state.arGraphDomain.xMin.toFloat()
    val fullRangeMax = state.arGraphDomain.xMax.toFloat()
    val rangeMax = rangeMin + (fullRangeMax - rangeMin) * progress.coerceIn(0.08f, 1f)
    val step = (rangeMax - rangeMin) / segments
    return (0..segments).map { rangeMin + step * it }
}

private val arRenderMathEngine = ArMathEngine()

private fun graphX(state: ArViewerUiState, x: Float): Float {
    val min = state.arGraphDomain.xMin.toFloat()
    val max = state.arGraphDomain.xMax.toFloat()
    return (((x - min) / (max - min)) - 0.5f) * 0.48f
}

private fun functionValue(state: ArViewerUiState, x: Float, phase: Float): Float =
    arRenderMathEngine.evaluate2d(state.compiledArExpression, x.toDouble(), phase.toDouble()).toArFloat()

private fun functionSurfaceValue(state: ArViewerUiState, x: Float, y: Float, phase: Float): Float {
    val compiled = state.compiledArExpression
    return if (compiled.kind == GraphExpressionKind.ExplicitSurface3D) {
        arRenderMathEngine.evaluate3d(compiled, x.toDouble(), y.toDouble(), phase.toDouble()).toArFloat()
    } else {
        functionValue(state, x, phase) + (y / 0.032f) * 0.08f
    }
}

private fun derivativeValue(state: ArViewerUiState, x: Float, phase: Float): Float {
    val h = 0.025f
    val left = functionValue(state, x - h, phase)
    val right = functionValue(state, x + h, phase)
    return ((right - left) / (2f * h)).coerceIn(-8f, 8f)
}

private fun functionHeight(state: ArViewerUiState, x: Float, phase: Float): Float = 0.08f + functionValue(state, x, phase) * 0.06f

private fun graphPoint(state: ArViewerUiState, x: Float, z: Float, phase: Float): Position =
    Position(graphX(state, x), 0.08f + functionSurfaceValue(state, x, z / 0.032f, phase) * 0.04f, z)

private fun Double.toArFloat(): Float = if (isFinite()) toFloat().coerceIn(-1.6f, 1.6f) else 0f

private fun tangentPlanePoint(state: ArViewerUiState, x0: Float, f0: Float, slope: Float, dx: Float, z: Float): Position =
    Position(graphX(state, x0) + dx, f0 + dx * slope + z * 0.12f, z)

private fun transformProgress(state: ArViewerUiState): Float =
    if (state.graphAnimationEnabled && state.graphAnimationMode in setOf(GraphAnimationMode.SweepArea, GraphAnimationMode.BuildVolume)) {
        state.graphAnimationProgress.coerceIn(0.08f, 1f)
    } else {
        1f
    }

private fun graphSegmentMaterial(
    state: ArViewerUiState,
    index: Int,
    xValue: Float,
    yValue: Float,
    nextYValue: Float,
    palette: List<MaterialInstance>
): MaterialInstance {
    val normalized = when (state.graphColorMap) {
        GraphColorMap.Height -> ((yValue + 1f) / 2f).coerceIn(0f, 1f)
        GraphColorMap.Slope -> (abs(nextYValue - yValue) * 2.8f).coerceIn(0f, 1f)
        GraphColorMap.Curvature -> abs(nextYValue - 2f * yValue + functionValue(state, xValue - 0.04f, 0f)).coerceIn(0f, 1f)
        GraphColorMap.XValue -> ((xValue - state.arGraphDomain.xMin.toFloat()) / (state.arGraphDomain.xMax - state.arGraphDomain.xMin).toFloat()).coerceIn(0f, 1f)
        GraphColorMap.YValue -> ((nextYValue + 1f) / 2f).coerceIn(0f, 1f)
    }
    val animatedShift = if (state.graphAnimationEnabled) state.graphAnimationProgress * 0.18f else 0f
    val paletteIndex = (((normalized + animatedShift).coerceIn(0f, 1f)) * (palette.lastIndex)).toInt().coerceIn(0, palette.lastIndex)
    return palette.getOrElse(paletteIndex) { palette.first() }
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
private fun NodeScope.BoxEdges(material: com.google.android.filament.MaterialInstance, halfX: Float, height: Float, halfZ: Float) {
    val ys = listOf(0f, height)
    ys.forEach { y ->
        LineNode(Position(-halfX, y, -halfZ), Position(halfX, y, -halfZ), material)
        LineNode(Position(halfX, y, -halfZ), Position(halfX, y, halfZ), material)
        LineNode(Position(halfX, y, halfZ), Position(-halfX, y, halfZ), material)
        LineNode(Position(-halfX, y, halfZ), Position(-halfX, y, -halfZ), material)
    }
    listOf(-halfX, halfX).forEach { x ->
        listOf(-halfZ, halfZ).forEach { z -> LineNode(Position(x, 0f, z), Position(x, height, z), material) }
    }
}

@Composable
private fun NodeScope.SphereApproxNode(line: com.google.android.filament.MaterialInstance, fill: com.google.android.filament.MaterialInstance) {
    CubeNode(Size(0.08f, 0.08f, 0.08f), center = Position(0f, 0.12f, 0f), materialInstance = fill)
    CircleLines(0.14f, 0.14f, line, y = 0.12f)
    (0 until 24).forEach { i ->
        val a = i * (2f * PI.toFloat() / 24f)
        LineNode(Position(cos(a) * 0.14f, 0.12f, sin(a) * 0.14f), Position(cos(a) * 0.07f, 0.24f, sin(a) * 0.07f), line)
        LineNode(Position(cos(a) * 0.14f, 0.12f, sin(a) * 0.14f), Position(cos(a) * 0.07f, 0f, sin(a) * 0.07f), line)
    }
}

@Composable
private fun NodeScope.CylinderApproxNode(line: com.google.android.filament.MaterialInstance, fill: com.google.android.filament.MaterialInstance) {
    CubeNode(Size(0.18f, 0.28f, 0.18f), center = Position(0f, 0.14f, 0f), materialInstance = fill)
    CircleLines(0.12f, 0.12f, line, y = 0f)
    CircleLines(0.12f, 0.12f, line, y = 0.28f)
    (0 until 12).forEach { i ->
        val a = i * (2f * PI.toFloat() / 12f)
        LineNode(Position(cos(a) * 0.12f, 0f, sin(a) * 0.12f), Position(cos(a) * 0.12f, 0.28f, sin(a) * 0.12f), line)
    }
}

@Composable
private fun NodeScope.ConeApproxNode(line: com.google.android.filament.MaterialInstance, fill: com.google.android.filament.MaterialInstance) {
    CubeNode(Size(0.12f, 0.04f, 0.12f), center = Position(0f, 0.02f, 0f), materialInstance = fill)
    CircleLines(0.14f, 0.14f, line, y = 0f)
    (0 until 16).forEach { i ->
        val a = i * (2f * PI.toFloat() / 16f)
        LineNode(Position(cos(a) * 0.14f, 0f, sin(a) * 0.14f), Position(0f, 0.32f, 0f), line)
    }
}

@Composable
private fun NodeScope.TriangleNode(line: com.google.android.filament.MaterialInstance, fill: com.google.android.filament.MaterialInstance) {
    CubeNode(Size(0.04f, 0.018f, 0.04f), center = Position(0f, 0.018f, 0f), materialInstance = fill)
    val a = Position(-0.16f, 0.01f, -0.1f)
    val b = Position(0.16f, 0.01f, -0.1f)
    val c = Position(0.02f, 0.01f, 0.16f)
    LineNode(a, b, line)
    LineNode(b, c, line)
    LineNode(c, a, line)
}

@Composable
private fun NodeScope.CircleNode(line: com.google.android.filament.MaterialInstance, fill: com.google.android.filament.MaterialInstance) {
    CubeNode(Size(0.04f, 0.012f, 0.04f), center = Position(0f, 0.012f, 0f), materialInstance = fill)
    CircleLines(0.18f, 0.18f, line, y = 0.014f)
}

@Composable
private fun NodeScope.NumberLineNode(line: com.google.android.filament.MaterialInstance, accent: com.google.android.filament.MaterialInstance) {
    LineNode(Position(-0.28f, 0.012f, 0f), Position(0.28f, 0.012f, 0f), line)
    (-5..5).forEach { tick ->
        val x = tick * 0.056f
        LineNode(Position(x, 0.012f, -0.025f), Position(x, 0.012f, 0.025f), if (tick == 0) accent else line)
    }
}

@Composable
private fun NodeScope.VectorArrowNode(line: com.google.android.filament.MaterialInstance, accent: com.google.android.filament.MaterialInstance) {
    LineNode(Position(0f, 0.02f, 0f), Position(0.24f, 0.18f, 0.08f), line)
    LineNode(Position(0.24f, 0.18f, 0.08f), Position(0.18f, 0.16f, 0.02f), accent)
    LineNode(Position(0.24f, 0.18f, 0.08f), Position(0.18f, 0.11f, 0.1f), accent)
}

@Composable
private fun NodeScope.CircleLines(radiusX: Float, radiusZ: Float, material: com.google.android.filament.MaterialInstance, y: Float) {
    val points = (0..48).map { i ->
        val a = i * (2f * PI.toFloat() / 48f)
        Position(cos(a) * radiusX, y, sin(a) * radiusZ)
    }
    points.zipWithNext().forEach { (a, b) -> LineNode(a, b, material) }
}

@Composable
private fun ArChrome(
    state: ArViewerUiState,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onSelectObject: (MathObjectType) -> Unit,
    onSelectArEngineMode: (ArEngineMode) -> Unit,
    onSelectMathExperience: (MathArExperience) -> Unit,
    onFloatingTool: (FloatingMathTool) -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onAnimationMode: (GraphAnimationMode) -> Unit,
    onSlicePosition: (Float) -> Unit,
    onCalibrationStep: () -> Unit,
    onPaperGraphLayer: (PaperGraphLayer) -> Unit,
    onFeaturePhase: (ArFeaturePhase) -> Unit,
    onToggleMathArFeature: (MathArFeature) -> Unit,
    onLiveEquation: (String) -> Unit,
    onGraphSlider: (String, Float) -> Unit,
    onGraphQuality: (GraphQualityPreset) -> Unit,
    onGraphDomain: (ArGraphDomain) -> Unit,
    onComparisonEquation: (String) -> Unit,
    onAnalysisFocus: (Float) -> Unit,
    onAddPointLabel: () -> Unit,
    onAddRulerAnchor: () -> Unit,
    onCompareOffset: (Float) -> Unit,
    onGestureHandle: (ArGestureHandle) -> Unit,
    onTransformStep: (Float) -> Unit,
    onApplyGestureHandle: (Float) -> Unit,
    onAddConstructionPoint: () -> Unit,
    onConstructionLine: () -> Unit,
    onConstructionSegment: () -> Unit,
    onConstructionVector: () -> Unit,
    onConstructionPlane: () -> Unit,
    onConstructionCircle: () -> Unit,
    onConstructionPolygon: () -> Unit,
    onConstructionMidpoint: () -> Unit,
    onConstructionParallel: () -> Unit,
    onConstructionPerpendicular: () -> Unit,
    onConstructionConstraint: (ConstructionConstraintKind) -> Unit,
    onCapturePersistentAnchor: () -> Unit,
    onDepthOcclusionMode: (ArDepthOcclusionMode) -> Unit,
    onPerformanceProfile: (ArPerformanceProfile) -> Unit,
    onMeshDensity: (Float) -> Unit,
    onMaxSceneObjects: (Float) -> Unit,
    onMarkScreenshotReady: () -> Unit,
    onExportArScene: () -> Unit,
    onApplyArTemplate: (String) -> Unit,
    onSelectArWorkflow: (String) -> Unit,
    onCompleteWorkflowStep: () -> Unit,
    onAdvanceWorkflowStep: () -> Unit,
    onRefreshWorkflow: () -> Unit,
    onExportArActivity: () -> Unit,
    onCapture: () -> Unit,
    onReplace: () -> Unit,
    onMove: () -> Unit,
    onRePlace: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onTogglePlanes: () -> Unit,
    onSelectSceneObject: (String, Boolean) -> Unit,
    onExperienceMode: (ExperienceMode) -> Unit,
    onMode: (SceneInteractionMode) -> Unit,
    onDuplicate: () -> Unit,
    onLock: () -> Unit,
    onHide: () -> Unit,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInspector: () -> Unit,
    onDiagnostics: () -> Unit,
    onLayers: () -> Unit,
    onSavedScenes: () -> Unit,
    onSave: () -> Unit,
    onClearScene: () -> Unit
) {
    var controlsExpanded by remember { mutableStateOf(state.mathScene.objects.isEmpty()) }
    var activeMenu by remember { mutableStateOf(ArPlacementMenu.Functions) }
    LaunchedEffect(state.mathScene.objects.size) {
        if (state.mathScene.objects.isNotEmpty()) controlsExpanded = false
    }
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
                IconButton(onClick = onSavedScenes) { Icon(Icons.Outlined.Save, "Saved scenes") }
            }
        }
        val selected = state.mathScene.primarySelectedObject
        val availableMenus = state.availablePlacementMenus()
        LaunchedEffect(state.arEngineMode, state.mathArExperience) {
            if (activeMenu !in availableMenus) activeMenu = availableMenus.first()
        }
        val menuDefinitions = activeMenu.definitions(state.mathArExperience)
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactArControlBar(
                state = state,
                expanded = controlsExpanded,
                onToggle = { controlsExpanded = !controlsExpanded },
                onRePlace = onRePlace
            )
            FloatingMathToolbar(
                state = state,
                onTool = {
                    if (it == FloatingMathTool.Capture) onCapture()
                    onFloatingTool(it)
                }
            )
            if (controlsExpanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArModeSwitcher(
                        selected = state.arEngineMode,
                        onSelect = onSelectArEngineMode
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.availableMathExperiences()) { experience ->
                            FilterChip(
                                selected = state.mathArExperience == experience,
                                onClick = { onSelectMathExperience(experience) },
                                label = { Text(experience.label()) }
                            )
                        }
                    }
                    MathExperiencePanel(
                        state = state,
                        onCalibrationStep = onCalibrationStep,
                        onGraphColorMap = onGraphColorMap,
                        onFunction3dTransform = onFunction3dTransform,
                        onAnimationProgress = onAnimationProgress,
                        onAnimationMode = onAnimationMode,
                        onSlicePosition = onSlicePosition,
                        onLayers = onLayers,
                        onPaperGraphLayer = onPaperGraphLayer,
                        onFeaturePhase = onFeaturePhase,
                        onToggleMathArFeature = onToggleMathArFeature,
                        onLiveEquation = onLiveEquation,
                        onGraphSlider = onGraphSlider,
                        onGraphQuality = onGraphQuality,
                        onGraphDomain = onGraphDomain,
                        onComparisonEquation = onComparisonEquation,
                        onAnalysisFocus = onAnalysisFocus,
                        onAddPointLabel = onAddPointLabel,
                        onAddRulerAnchor = onAddRulerAnchor,
                        onCompareOffset = onCompareOffset,
                        onGestureHandle = onGestureHandle,
                        onTransformStep = onTransformStep,
                        onApplyGestureHandle = onApplyGestureHandle,
                        onAddConstructionPoint = onAddConstructionPoint,
                        onConstructionLine = onConstructionLine,
                        onConstructionSegment = onConstructionSegment,
                        onConstructionVector = onConstructionVector,
                        onConstructionPlane = onConstructionPlane,
                        onConstructionCircle = onConstructionCircle,
                        onConstructionPolygon = onConstructionPolygon,
                        onConstructionMidpoint = onConstructionMidpoint,
                        onConstructionParallel = onConstructionParallel,
                        onConstructionPerpendicular = onConstructionPerpendicular,
                        onConstructionConstraint = onConstructionConstraint,
                        onCapturePersistentAnchor = onCapturePersistentAnchor,
                        onDepthOcclusionMode = onDepthOcclusionMode,
                        onPerformanceProfile = onPerformanceProfile,
                        onMeshDensity = onMeshDensity,
                        onMaxSceneObjects = onMaxSceneObjects,
                        onMarkScreenshotReady = onMarkScreenshotReady,
                        onExportArScene = onExportArScene,
                        onApplyArTemplate = onApplyArTemplate,
                        onSelectArWorkflow = onSelectArWorkflow,
                        onCompleteWorkflowStep = onCompleteWorkflowStep,
                        onAdvanceWorkflowStep = onAdvanceWorkflowStep,
                        onRefreshWorkflow = onRefreshWorkflow,
                        onExportArActivity = onExportArActivity,
                        onCapture = {
                            onCapture()
                            onFloatingTool(FloatingMathTool.Capture)
                        }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableMenus) { menu ->
                            FilterChip(
                                selected = activeMenu == menu,
                                onClick = { activeMenu = menu },
                                label = { Text(menu.label) }
                            )
                        }
                    }
                    if (activeMenu != ArPlacementMenu.Scene) {
                        Text(activeMenu.description, style = MaterialTheme.typography.bodySmall)
                        VisualObjectTray(
                            items = activeMenu.visualTrayItems(state.mathArExperience, menuDefinitions),
                            selectedDefinitionId = state.selectedDefinitionId,
                            onSelectObject = onSelectObject
                        )
                    }
                    if (activeMenu == ArPlacementMenu.Functions) {
                        FunctionGraphDetails(state)
                    }
                    if (activeMenu == ArPlacementMenu.Graphs) {
                        GraphToolDetails(state)
                    }
                    if (activeMenu == ArPlacementMenu.Scene) {
                        SceneMenuContent(
                            state = state,
                            selected = selected,
                            onSelectSceneObject = onSelectSceneObject,
                            onMode = onMode,
                            onMove = onMove,
                            onReset = onReset,
                            onDelete = onDelete,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onDuplicate = onDuplicate,
                            onLock = onLock,
                            onHide = onHide,
                            onInspector = onInspector,
                            onDiagnostics = onDiagnostics,
                            onLayers = onLayers,
                            onGroup = onGroup,
                            onUngroup = onUngroup,
                            onSave = onSave,
                            onClearScene = onClearScene
                        )
                    }
                    if (state.recoveryMode != ArRecoveryMode.Normal) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(state.trackingMessage, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = onRePlace, enabled = state.canRePlaceObject || state.mathScene.objects.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                                    Text("Re-place Object")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ArPlacementMenu(val label: String, val description: String) {
    Functions("Functions", "Place and inspect sine functions, expressions, and curve behavior."),
    Graphs("Graphs", "Place coordinate planes, number lines, and graphing tools."),
    Objects("Objects", "Place geometric solids and shapes."),
    Solar("Solar", "Build orbit models, scale comparisons, and planet paths."),
    Scene("Scene", "Select, transform, save, and diagnose the AR scene.");

    fun definitions(experience: MathArExperience): List<MathObjectDefinition> = when (this) {
        Functions -> DefaultMathObjectRegistry.getDefinitionsByCategory(MathObjectCategory.Graphs)
            .filter { it.type == MathObjectType.SineCurve }
        Graphs -> DefaultMathObjectRegistry.getAllDefinitions()
            .filter { it.category in setOf(MathObjectCategory.Graphs, MathObjectCategory.Coordinates, MathObjectCategory.NumberTools, MathObjectCategory.Vectors) }
        Objects -> DefaultMathObjectRegistry.getAllDefinitions()
            .filter {
                it.category in setOf(MathObjectCategory.Shapes, MathObjectCategory.Solids) &&
                    (experience != MathArExperience.Drawing2dTo3d || it.type in setOf(MathObjectType.RectangularPrism, MathObjectType.Cylinder, MathObjectType.Cone, MathObjectType.Sphere))
            }
        Solar -> DefaultMathObjectRegistry.getAllDefinitions()
            .filter { it.type in setOf(MathObjectType.Sphere, MathObjectType.Circle, MathObjectType.VectorArrow, MathObjectType.NumberLine) }
        Scene -> emptyList()
    }
}

private enum class ObjectPreviewKind { Cube, Sphere, SineWave, Parabola, Vector, CoordinatePlane, Surface, Cylinder, Cone, Prism, Circle, NumberLine, Triangle }

private data class VisualTrayItem(
    val label: String,
    val type: MathObjectType,
    val definitionId: String,
    val preview: ObjectPreviewKind
)

private fun ArPlacementMenu.visualTrayItems(
    experience: MathArExperience,
    definitions: List<MathObjectDefinition>
): List<VisualTrayItem> {
    if (this == ArPlacementMenu.Functions) {
        val sine = definitions.firstOrNull { it.type == MathObjectType.SineCurve }
        if (sine != null) {
            return buildList {
                add(VisualTrayItem("Sine wave", sine.type, sine.definitionId, ObjectPreviewKind.SineWave))
                add(VisualTrayItem("Parabola", sine.type, sine.definitionId, ObjectPreviewKind.Parabola))
                if (experience in setOf(MathArExperience.Drawing2dTo3d, MathArExperience.MarkerBasedGraph)) {
                    add(VisualTrayItem("Surface", sine.type, sine.definitionId, ObjectPreviewKind.Surface))
                }
            }
        }
    }
    return definitions.map {
        VisualTrayItem(
            label = it.displayName,
            type = it.type,
            definitionId = it.definitionId,
            preview = it.type.previewKind()
        )
    }
}

private fun MathObjectType.previewKind(): ObjectPreviewKind = when (this) {
    MathObjectType.Cube -> ObjectPreviewKind.Cube
    MathObjectType.Sphere -> ObjectPreviewKind.Sphere
    MathObjectType.SineCurve -> ObjectPreviewKind.SineWave
    MathObjectType.VectorArrow -> ObjectPreviewKind.Vector
    MathObjectType.CoordinatePlane -> ObjectPreviewKind.CoordinatePlane
    MathObjectType.Cylinder -> ObjectPreviewKind.Cylinder
    MathObjectType.Cone -> ObjectPreviewKind.Cone
    MathObjectType.RectangularPrism -> ObjectPreviewKind.Prism
    MathObjectType.Circle -> ObjectPreviewKind.Circle
    MathObjectType.NumberLine -> ObjectPreviewKind.NumberLine
    MathObjectType.Triangle -> ObjectPreviewKind.Triangle
}

@Composable
private fun VisualObjectTray(
    items: List<VisualTrayItem>,
    selectedDefinitionId: String,
    onSelectObject: (MathObjectType) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items) { item ->
            VisualObjectCard(
                item = item,
                selected = selectedDefinitionId == item.definitionId,
                onSelect = { onSelectObject(item.type) }
            )
        }
    }
}

@Composable
private fun VisualObjectCard(
    item: VisualTrayItem,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val accent = item.preview.previewColor()
    Surface(
        modifier = Modifier
            .width(112.dp)
            .height(116.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = "Select ${item.label}" },
        color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ObjectPreviewCanvas(item.preview, accent, Modifier.fillMaxWidth().height(64.dp))
            Text(item.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ObjectPreviewCanvas(kind: ObjectPreviewKind, accent: Color, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 3.2f
        when (kind) {
            ObjectPreviewKind.Cube -> {
                drawRect(accent.copy(alpha = 0.22f), topLeft = Offset(w * 0.24f, h * 0.28f), size = DrawSize(w * 0.36f, h * 0.36f))
                drawRect(accent, topLeft = Offset(w * 0.24f, h * 0.28f), size = DrawSize(w * 0.36f, h * 0.36f), style = Stroke(stroke))
                drawRect(muted, topLeft = Offset(w * 0.38f, h * 0.16f), size = DrawSize(w * 0.36f, h * 0.36f), style = Stroke(stroke))
                listOf(Offset(w * 0.24f, h * 0.28f) to Offset(w * 0.38f, h * 0.16f), Offset(w * 0.60f, h * 0.28f) to Offset(w * 0.74f, h * 0.16f), Offset(w * 0.60f, h * 0.64f) to Offset(w * 0.74f, h * 0.52f)).forEach { (a, b) -> drawLine(muted, a, b, stroke) }
            }
            ObjectPreviewKind.Sphere -> {
                drawCircle(accent.copy(alpha = 0.24f), radius = h * 0.28f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(accent, radius = h * 0.28f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
                drawOval(muted, topLeft = Offset(w * 0.23f, h * 0.42f), size = DrawSize(w * 0.54f, h * 0.16f), style = Stroke(stroke))
            }
            ObjectPreviewKind.SineWave -> drawWave(accent, muted, stroke, parabola = false)
            ObjectPreviewKind.Parabola -> drawWave(accent, muted, stroke, parabola = true)
            ObjectPreviewKind.Vector -> {
                drawLine(muted, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.26f), stroke * 1.4f)
                drawLine(accent, Offset(w * 0.82f, h * 0.26f), Offset(w * 0.64f, h * 0.26f), stroke * 1.4f)
                drawLine(accent, Offset(w * 0.82f, h * 0.26f), Offset(w * 0.75f, h * 0.44f), stroke * 1.4f)
            }
            ObjectPreviewKind.CoordinatePlane -> {
                for (i in 1..4) {
                    val x = w * (0.18f + i * 0.16f)
                    val y = h * (0.18f + i * 0.16f)
                    drawLine(muted.copy(alpha = 0.45f), Offset(x, h * 0.16f), Offset(x, h * 0.84f), 1.4f)
                    drawLine(muted.copy(alpha = 0.45f), Offset(w * 0.16f, y), Offset(w * 0.84f, y), 1.4f)
                }
                drawLine(accent, Offset(w * 0.16f, h * 0.5f), Offset(w * 0.84f, h * 0.5f), stroke)
                drawLine(accent, Offset(w * 0.5f, h * 0.84f), Offset(w * 0.5f, h * 0.16f), stroke)
            }
            ObjectPreviewKind.Surface -> {
                for (row in 0..4) {
                    val y = h * (0.28f + row * 0.1f)
                    drawLine(if (row % 2 == 0) accent else muted, Offset(w * 0.18f, y + row * 2f), Offset(w * 0.82f, y - row * 3f), stroke * 0.75f)
                }
                for (col in 0..5) {
                    val x = w * (0.2f + col * 0.12f)
                    drawLine(muted, Offset(x, h * 0.28f), Offset(x + w * 0.08f, h * 0.68f), stroke * 0.65f)
                }
            }
            ObjectPreviewKind.Cylinder -> {
                drawOval(accent, Offset(w * 0.28f, h * 0.18f), DrawSize(w * 0.44f, h * 0.18f), style = Stroke(stroke))
                drawOval(accent, Offset(w * 0.28f, h * 0.62f), DrawSize(w * 0.44f, h * 0.18f), style = Stroke(stroke))
                drawLine(muted, Offset(w * 0.28f, h * 0.27f), Offset(w * 0.28f, h * 0.71f), stroke)
                drawLine(muted, Offset(w * 0.72f, h * 0.27f), Offset(w * 0.72f, h * 0.71f), stroke)
            }
            ObjectPreviewKind.Cone -> {
                drawOval(accent, Offset(w * 0.25f, h * 0.66f), DrawSize(w * 0.5f, h * 0.16f), style = Stroke(stroke))
                drawLine(muted, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.25f, h * 0.74f), stroke)
                drawLine(muted, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.75f, h * 0.74f), stroke)
            }
            ObjectPreviewKind.Prism -> {
                drawRect(accent.copy(alpha = 0.18f), Offset(w * 0.22f, h * 0.36f), DrawSize(w * 0.52f, h * 0.26f))
                drawRect(accent, Offset(w * 0.22f, h * 0.36f), DrawSize(w * 0.52f, h * 0.26f), style = Stroke(stroke))
                drawRect(muted, Offset(w * 0.34f, h * 0.22f), DrawSize(w * 0.52f, h * 0.26f), style = Stroke(stroke))
            }
            ObjectPreviewKind.Circle -> {
                drawCircle(accent.copy(alpha = 0.18f), h * 0.28f, Offset(w * 0.5f, h * 0.5f))
                drawCircle(accent, h * 0.28f, Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
            }
            ObjectPreviewKind.NumberLine -> {
                drawLine(accent, Offset(w * 0.16f, h * 0.54f), Offset(w * 0.84f, h * 0.54f), stroke)
                for (i in 0..6) {
                    val x = w * (0.18f + i * 0.105f)
                    drawLine(muted, Offset(x, h * 0.44f), Offset(x, h * 0.64f), stroke * 0.75f)
                }
            }
            ObjectPreviewKind.Triangle -> {
                val a = Offset(w * 0.5f, h * 0.2f)
                val b = Offset(w * 0.22f, h * 0.76f)
                val c = Offset(w * 0.8f, h * 0.76f)
                drawLine(accent, a, b, stroke)
                drawLine(accent, b, c, stroke)
                drawLine(accent, c, a, stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    accent: Color,
    muted: Color,
    stroke: Float,
    parabola: Boolean
) {
    drawLine(muted, Offset(size.width * 0.12f, size.height * 0.62f), Offset(size.width * 0.88f, size.height * 0.62f), stroke * 0.6f)
    drawLine(muted, Offset(size.width * 0.18f, size.height * 0.84f), Offset(size.width * 0.18f, size.height * 0.16f), stroke * 0.6f)
    val points = (0..28).map { i ->
        val t = i / 28f
        val x = size.width * (0.18f + t * 0.68f)
        val y = if (parabola) {
            val gx = (t - 0.5f) * 2f
            size.height * (0.72f - gx * gx * 0.42f)
        } else {
            size.height * (0.5f - sin(t * 2f * PI.toFloat()) * 0.25f)
        }
        Offset(x, y)
    }
    points.zipWithNext().forEach { (a, b) -> drawLine(accent, a, b, stroke) }
}

private fun ObjectPreviewKind.previewColor(): Color = when (this) {
    ObjectPreviewKind.Cube -> Color(0xFF62D6C7)
    ObjectPreviewKind.Sphere -> Color(0xFF9B8CFF)
    ObjectPreviewKind.SineWave -> Color(0xFFFFC857)
    ObjectPreviewKind.Parabola -> Color(0xFFFF6B8A)
    ObjectPreviewKind.Vector -> Color(0xFFEC407A)
    ObjectPreviewKind.CoordinatePlane -> Color(0xFF6EDBFF)
    ObjectPreviewKind.Surface -> Color(0xFF3DFF9F)
    ObjectPreviewKind.Cylinder -> Color(0xFF4FD1C5)
    ObjectPreviewKind.Cone -> Color(0xFFFFA726)
    ObjectPreviewKind.Prism -> Color(0xFF7E57C2)
    ObjectPreviewKind.Circle -> Color(0xFF66BB6A)
    ObjectPreviewKind.NumberLine -> Color(0xFF42A5F5)
    ObjectPreviewKind.Triangle -> Color(0xFFFF6B8A)
}

@Composable
private fun ArModeSwitcher(
    selected: ArEngineMode,
    onSelect: (ArEngineMode) -> Unit
) {
    val modes = listOf(
        ArEngineMode.Indoor,
        ArEngineMode.PaperGraph,
        ArEngineMode.OutdoorGeospatialMath,
        ArEngineMode.SurfacePlacement,
        ArEngineMode.AirPlacement
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("AR Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(modes) { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = { Text(mode.segmentLabel()) }
                )
            }
        }
    }
}

private fun ArEngineMode.segmentLabel(): String = when (this) {
    ArEngineMode.Indoor -> "Indoor AR"
    ArEngineMode.PaperGraph -> "Paper Graph"
    ArEngineMode.OutdoorGeospatialMath -> "Outdoor Geo"
    ArEngineMode.SurfacePlacement -> "Surface"
    ArEngineMode.AirPlacement -> "Air"
}

private fun ArViewerUiState.availableMathExperiences(): List<MathArExperience> = when (arEngineMode) {
    ArEngineMode.PaperGraph -> listOf(MathArExperience.MarkerBasedGraph)
    ArEngineMode.OutdoorGeospatialMath -> listOf(MathArExperience.OutdoorGeometry)
    ArEngineMode.SurfacePlacement,
    ArEngineMode.AirPlacement,
    ArEngineMode.Indoor -> listOf(
        MathArExperience.MarkerlessObjects,
        MathArExperience.Drawing2dTo3d,
        MathArExperience.SolarSystem,
        MathArExperience.SceneTools
    )
}

private fun ArViewerUiState.availablePlacementMenus(): List<ArPlacementMenu> = when (arEngineMode) {
    ArEngineMode.PaperGraph -> listOf(ArPlacementMenu.Graphs, ArPlacementMenu.Functions)
    ArEngineMode.OutdoorGeospatialMath -> listOf(ArPlacementMenu.Graphs, ArPlacementMenu.Objects)
    ArEngineMode.SurfacePlacement -> listOf(ArPlacementMenu.Objects, ArPlacementMenu.Graphs, ArPlacementMenu.Scene)
    ArEngineMode.AirPlacement -> listOf(ArPlacementMenu.Functions, ArPlacementMenu.Graphs, ArPlacementMenu.Solar)
    ArEngineMode.Indoor -> mathArExperience.placementMenus()
}

private fun MathArExperience.label(): String = when (this) {
    MathArExperience.MarkerlessObjects -> "Markerless Math"
    MathArExperience.Drawing2dTo3d -> "Drawing 2D to 3D"
    MathArExperience.MarkerBasedGraph -> "Marker-Based Graph"
    MathArExperience.OutdoorGeometry -> "Outdoor Geometry"
    MathArExperience.SolarSystem -> "Solar System"
    MathArExperience.SceneTools -> "Scene Tools"
}

private fun MathArExperience.placementMenus(): List<ArPlacementMenu> = when (this) {
    MathArExperience.MarkerlessObjects -> listOf(ArPlacementMenu.Functions, ArPlacementMenu.Graphs, ArPlacementMenu.Objects)
    MathArExperience.Drawing2dTo3d -> listOf(ArPlacementMenu.Functions, ArPlacementMenu.Graphs, ArPlacementMenu.Objects)
    MathArExperience.MarkerBasedGraph -> listOf(ArPlacementMenu.Graphs, ArPlacementMenu.Functions, ArPlacementMenu.Objects)
    MathArExperience.OutdoorGeometry -> listOf(ArPlacementMenu.Graphs, ArPlacementMenu.Objects)
    MathArExperience.SolarSystem -> listOf(ArPlacementMenu.Solar, ArPlacementMenu.Objects)
    MathArExperience.SceneTools -> listOf(ArPlacementMenu.Scene)
}

@Composable
private fun CompactArControlBar(
    state: ArViewerUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRePlace: () -> Unit
) {
    val score = state.placementScore.coerceIn(0, 100)
    val scoreColor = placementScoreColor(score)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(statusText(state), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("$score", color = scoreColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = scoreColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess, if (expanded) "Collapse controls" else "Expand controls")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = {}, label = { Text(state.placementQuality.name) })
            AssistChip(onClick = {}, label = { Text(if (state.motionStable) "Motion steady" else "Hold steady") })
            AssistChip(onClick = {}, label = { Text(if (state.lightStable) "Light OK" else "Need light") })
            if (state.canRePlaceObject || state.mathScene.objects.isNotEmpty()) {
                AssistChip(onClick = onRePlace, label = { Text("Re-place") })
            }
        }
    }
}

private fun placementScoreColor(score: Int): Color =
    if (score < 50) {
        lerp(Color(0xFFE53935), Color(0xFFFFC857), (score / 50f).coerceIn(0f, 1f))
    } else {
        lerp(Color(0xFFFFC857), Color(0xFF2ECC71), ((score - 50) / 50f).coerceIn(0f, 1f))
    }

@Composable
private fun FunctionGraphDetails(state: ArViewerUiState) {
    val definition = DefaultMathObjectRegistry.getDefinition("sine-curve")
    val selected = state.mathScene.primarySelectedObject?.takeIf { it.definitionId == "sine-curve" }
    val parameters = selected?.parameters ?: definition?.defaultParameters.orEmpty()
    val amplitude = parameterNumber(parameters, "amplitude", 1.0)
    val frequency = parameterNumber(parameters, "frequency", 1.0)
    val phase = parameterNumber(parameters, "phaseShift", 0.0)
    val shift = parameterNumber(parameters, "verticalShift", 0.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("y = ${MeasurementFormatter.number(amplitude)} sin(${MeasurementFormatter.number(frequency)}x + ${MeasurementFormatter.number(phase)}) + ${MeasurementFormatter.number(shift)}", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    "Amplitude ${MeasurementFormatter.number(kotlin.math.abs(amplitude))}",
                    "Period ${MeasurementFormatter.number((2 * PI / frequency))}",
                    "Phase ${MeasurementFormatter.number(-phase / frequency)}",
                    "Midline ${MeasurementFormatter.number(shift)}"
                )
            ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
        }
        Text("Use Details after placement to tune amplitude, frequency, phase shift, X range, and vertical shift.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GraphToolDetails(state: ArViewerUiState) {
    val graphCount = state.mathScene.objects.count {
        DefaultMathObjectRegistry.getDefinition(it.definitionId)?.category in setOf(
            MathObjectCategory.Graphs,
            MathObjectCategory.Coordinates,
            MathObjectCategory.NumberTools,
            MathObjectCategory.Vectors
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AssistChip(onClick = {}, label = { Text("$graphCount graph tools") })
        AssistChip(onClick = {}, label = { Text("Axes") })
        AssistChip(onClick = {}, label = { Text("Functions") })
        AssistChip(onClick = {}, label = { Text("Vectors") })
    }
}

@Composable
private fun MathExperiencePanel(
    state: ArViewerUiState,
    onCalibrationStep: () -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onAnimationMode: (GraphAnimationMode) -> Unit,
    onSlicePosition: (Float) -> Unit,
    onLayers: () -> Unit,
    onPaperGraphLayer: (PaperGraphLayer) -> Unit,
    onFeaturePhase: (ArFeaturePhase) -> Unit,
    onToggleMathArFeature: (MathArFeature) -> Unit,
    onLiveEquation: (String) -> Unit,
    onGraphSlider: (String, Float) -> Unit,
    onGraphQuality: (GraphQualityPreset) -> Unit,
    onGraphDomain: (ArGraphDomain) -> Unit,
    onComparisonEquation: (String) -> Unit,
    onAnalysisFocus: (Float) -> Unit,
    onAddPointLabel: () -> Unit,
    onAddRulerAnchor: () -> Unit,
    onCompareOffset: (Float) -> Unit,
    onGestureHandle: (ArGestureHandle) -> Unit,
    onTransformStep: (Float) -> Unit,
    onApplyGestureHandle: (Float) -> Unit,
    onAddConstructionPoint: () -> Unit,
    onConstructionLine: () -> Unit,
    onConstructionSegment: () -> Unit,
    onConstructionVector: () -> Unit,
    onConstructionPlane: () -> Unit,
    onConstructionCircle: () -> Unit,
    onConstructionPolygon: () -> Unit,
    onConstructionMidpoint: () -> Unit,
    onConstructionParallel: () -> Unit,
    onConstructionPerpendicular: () -> Unit,
    onConstructionConstraint: (ConstructionConstraintKind) -> Unit,
    onCapturePersistentAnchor: () -> Unit,
    onDepthOcclusionMode: (ArDepthOcclusionMode) -> Unit,
    onPerformanceProfile: (ArPerformanceProfile) -> Unit,
    onMeshDensity: (Float) -> Unit,
    onMaxSceneObjects: (Float) -> Unit,
    onMarkScreenshotReady: () -> Unit,
    onExportArScene: () -> Unit,
    onApplyArTemplate: (String) -> Unit,
    onSelectArWorkflow: (String) -> Unit,
    onCompleteWorkflowStep: () -> Unit,
    onAdvanceWorkflowStep: () -> Unit,
    onRefreshWorkflow: () -> Unit,
    onExportArActivity: () -> Unit,
    onCapture: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.mathArExperience) {
            MathArExperience.MarkerlessObjects -> MarkerlessMathPanel(state)
            MathArExperience.Drawing2dTo3d -> Drawing2dTo3dPanel(
                state = state,
                onGraphColorMap = onGraphColorMap,
                onFunction3dTransform = onFunction3dTransform,
                onAnimationProgress = onAnimationProgress,
                onAnimationMode = onAnimationMode,
                onSlicePosition = onSlicePosition
            )
            MathArExperience.MarkerBasedGraph -> PaperGraphModePanel(
                state = state,
                onCalibrationStep = onCalibrationStep,
                onGraphColorMap = onGraphColorMap,
                onAnimationProgress = onAnimationProgress,
                onAnimationMode = onAnimationMode,
                onSlicePosition = onSlicePosition,
                onPaperGraphLayer = onPaperGraphLayer
            )
            MathArExperience.OutdoorGeometry -> OutdoorGeospatialMathPanel(state)
            MathArExperience.SolarSystem -> SolarSystemArPanel(state, onAnimationProgress)
            MathArExperience.SceneTools -> DirectUserEnhancementPanel(
                state = state,
                onLayers = onLayers,
                onCapture = onCapture
            )
        }
        MathArPhaseToolsPanel(
            state = state,
            onFeaturePhase = onFeaturePhase,
            onToggleMathArFeature = onToggleMathArFeature,
            onLiveEquation = onLiveEquation,
            onGraphSlider = onGraphSlider,
            onGraphQuality = onGraphQuality,
            onGraphDomain = onGraphDomain,
            onComparisonEquation = onComparisonEquation,
            onAnalysisFocus = onAnalysisFocus,
            onAddPointLabel = onAddPointLabel,
            onAddRulerAnchor = onAddRulerAnchor,
            onCompareOffset = onCompareOffset,
            onGestureHandle = onGestureHandle,
            onTransformStep = onTransformStep,
            onApplyGestureHandle = onApplyGestureHandle,
            onAddConstructionPoint = onAddConstructionPoint,
            onConstructionLine = onConstructionLine,
            onConstructionSegment = onConstructionSegment,
            onConstructionVector = onConstructionVector,
            onConstructionPlane = onConstructionPlane,
            onConstructionCircle = onConstructionCircle,
            onConstructionPolygon = onConstructionPolygon,
            onConstructionMidpoint = onConstructionMidpoint,
            onConstructionParallel = onConstructionParallel,
            onConstructionPerpendicular = onConstructionPerpendicular,
            onConstructionConstraint = onConstructionConstraint,
            onCapturePersistentAnchor = onCapturePersistentAnchor,
            onDepthOcclusionMode = onDepthOcclusionMode,
            onPerformanceProfile = onPerformanceProfile,
            onMeshDensity = onMeshDensity,
            onMaxSceneObjects = onMaxSceneObjects,
            onMarkScreenshotReady = onMarkScreenshotReady,
            onExportArScene = onExportArScene,
            onApplyArTemplate = onApplyArTemplate,
            onSelectArWorkflow = onSelectArWorkflow,
            onCompleteWorkflowStep = onCompleteWorkflowStep,
            onAdvanceWorkflowStep = onAdvanceWorkflowStep,
            onRefreshWorkflow = onRefreshWorkflow,
            onExportArActivity = onExportArActivity
        )
    }
}

@Composable
private fun MarkerlessMathPanel(state: ArViewerUiState) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(
            listOf(
                "Surface first",
                "No marker",
                "Objects ${state.mathScene.objects.size}",
                "Planes ${if (state.planesVisible) "on" else "off"}",
                "Labels ${onOff(state.labelsVisible)}"
            )
        ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
    }
}

@Composable
private fun MathArPhaseToolsPanel(
    state: ArViewerUiState,
    onFeaturePhase: (ArFeaturePhase) -> Unit,
    onToggleMathArFeature: (MathArFeature) -> Unit,
    onLiveEquation: (String) -> Unit,
    onGraphSlider: (String, Float) -> Unit,
    onGraphQuality: (GraphQualityPreset) -> Unit,
    onGraphDomain: (ArGraphDomain) -> Unit,
    onComparisonEquation: (String) -> Unit,
    onAnalysisFocus: (Float) -> Unit,
    onAddPointLabel: () -> Unit,
    onAddRulerAnchor: () -> Unit,
    onCompareOffset: (Float) -> Unit,
    onGestureHandle: (ArGestureHandle) -> Unit,
    onTransformStep: (Float) -> Unit,
    onApplyGestureHandle: (Float) -> Unit,
    onAddConstructionPoint: () -> Unit,
    onConstructionLine: () -> Unit,
    onConstructionSegment: () -> Unit,
    onConstructionVector: () -> Unit,
    onConstructionPlane: () -> Unit,
    onConstructionCircle: () -> Unit,
    onConstructionPolygon: () -> Unit,
    onConstructionMidpoint: () -> Unit,
    onConstructionParallel: () -> Unit,
    onConstructionPerpendicular: () -> Unit,
    onConstructionConstraint: (ConstructionConstraintKind) -> Unit,
    onCapturePersistentAnchor: () -> Unit,
    onDepthOcclusionMode: (ArDepthOcclusionMode) -> Unit,
    onPerformanceProfile: (ArPerformanceProfile) -> Unit,
    onMeshDensity: (Float) -> Unit,
    onMaxSceneObjects: (Float) -> Unit,
    onMarkScreenshotReady: () -> Unit,
    onExportArScene: () -> Unit,
    onApplyArTemplate: (String) -> Unit,
    onSelectArWorkflow: (String) -> Unit,
    onCompleteWorkflowStep: () -> Unit,
    onAdvanceWorkflowStep: () -> Unit,
    onRefreshWorkflow: () -> Unit,
    onExportArActivity: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArFeaturePhase.entries) { phase ->
                FilterChip(
                    selected = state.selectedFeaturePhase == phase,
                    onClick = { onFeaturePhase(phase) },
                    label = { Text(phase.label()) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.selectedFeaturePhase.features()) { feature ->
                FilterChip(
                    selected = feature in state.enabledMathArFeatures,
                    onClick = { onToggleMathArFeature(feature) },
                    label = { Text(feature.shortLabel()) }
                )
            }
        }
        when (state.selectedFeaturePhase) {
            ArFeaturePhase.DirectInteraction -> {
                OutlinedTextField(
                    value = state.liveEquation,
                    onValueChange = onLiveEquation,
                    label = { Text("Live AR equation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ArGraphEngineControls(
                    state = state,
                    onGraphSlider = onGraphSlider,
                    onGraphQuality = onGraphQuality,
                    onGraphDomain = onGraphDomain
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onAddPointLabel, modifier = Modifier.weight(1f)) { Text("Pick point") }
                    OutlinedButton(onClick = onAddRulerAnchor, modifier = Modifier.weight(1f)) { Text("Ruler anchor") }
                }
                ArGestureHandleControls(
                    state = state,
                    onGestureHandle = onGestureHandle,
                    onTransformStep = onTransformStep,
                    onApplyGestureHandle = onApplyGestureHandle
                )
            }
            ArFeaturePhase.GraphAnalysis -> {
                ArGraphAnalysisControls(
                    state = state,
                    onComparisonEquation = onComparisonEquation,
                    onAnalysisFocus = onAnalysisFocus
                )
            }
            ArFeaturePhase.EngineStrengthening -> {
                Text("Engine overlays: grid lock, occlusion polish, saved scene anchors, confidence HUD and compare mode.", style = MaterialTheme.typography.bodySmall)
                ConstructionGeometryPanel(
                    state = state,
                    onAddConstructionPoint = onAddConstructionPoint,
                    onConstructionLine = onConstructionLine,
                    onConstructionSegment = onConstructionSegment,
                    onConstructionVector = onConstructionVector,
                    onConstructionPlane = onConstructionPlane,
                    onConstructionCircle = onConstructionCircle,
                    onConstructionPolygon = onConstructionPolygon,
                    onConstructionMidpoint = onConstructionMidpoint,
                    onConstructionParallel = onConstructionParallel,
                    onConstructionPerpendicular = onConstructionPerpendicular,
                    onConstructionConstraint = onConstructionConstraint
                )
                ProductionArPanel(
                    state = state,
                    onCapturePersistentAnchor = onCapturePersistentAnchor,
                    onDepthOcclusionMode = onDepthOcclusionMode,
                    onPerformanceProfile = onPerformanceProfile,
                    onMeshDensity = onMeshDensity,
                    onMaxSceneObjects = onMaxSceneObjects,
                    onMarkScreenshotReady = onMarkScreenshotReady,
                    onExportArScene = onExportArScene,
                    onApplyArTemplate = onApplyArTemplate
                )
                if (state.compareModeEnabled) {
                    Text("Compare offset ${"%.2f".format(state.compareOffsetMeters)}m", style = MaterialTheme.typography.labelMedium)
                    Slider(value = state.compareOffsetMeters, onValueChange = onCompareOffset, valueRange = 0.18f..0.8f)
                }
            }
            ArFeaturePhase.WorkflowStudio -> {
                ArWorkflowStudioPanel(
                    state = state,
                    onSelectArWorkflow = onSelectArWorkflow,
                    onCompleteWorkflowStep = onCompleteWorkflowStep,
                    onAdvanceWorkflowStep = onAdvanceWorkflowStep,
                    onRefreshWorkflow = onRefreshWorkflow,
                    onExportArActivity = onExportArActivity
                )
            }
        }
    }
}

@Composable
private fun ArGraphEngineControls(
    state: ArViewerUiState,
    onGraphSlider: (String, Float) -> Unit,
    onGraphQuality: (GraphQualityPreset) -> Unit,
    onGraphDomain: (ArGraphDomain) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val parse = state.compiledArExpression.parse
        val status = when (parse) {
            ParseOutcome.NotParsed -> "Not parsed"
            is ParseOutcome.Success -> state.compiledArExpression.message
            is ParseOutcome.Failure -> parse.message
        }
        AssistChip(
            onClick = {},
            label = { Text(status, maxLines = 2) }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GraphQualityPreset.entries) { quality ->
                FilterChip(
                    selected = state.arGraphQualityPreset == quality,
                    onClick = { onGraphQuality(quality) },
                    label = { Text(quality.shortLabel()) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(arDomainPresets) { preset ->
                FilterChip(
                    selected = state.arGraphDomain == preset.domain,
                    onClick = { onGraphDomain(preset.domain) },
                    label = { Text(preset.label) }
                )
            }
        }
        state.arGraphSliders.forEach { slider ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${slider.symbol} coefficient", style = MaterialTheme.typography.labelMedium)
                    Text("%.2f".format(slider.value), style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = slider.value.toFloat(),
                    onValueChange = { onGraphSlider(slider.symbol, it) },
                    valueRange = slider.minimum.toFloat()..slider.maximum.toFloat(),
                    steps = slider.sliderSteps()
                )
            }
        }
        if (state.arGraphSliders.isEmpty()) {
            Text("Add coefficients like a, b, c or d to create AR sliders.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ArGestureHandleControls(
    state: ArViewerUiState,
    onGestureHandle: (ArGestureHandle) -> Unit,
    onTransformStep: (Float) -> Unit,
    onApplyGestureHandle: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArGestureHandle.entries) { handle ->
                FilterChip(
                    selected = state.selectedGestureHandle == handle,
                    onClick = { onGestureHandle(handle) },
                    label = { Text(handle.shortLabel()) }
                )
            }
        }
        Text("Handle step ${"%.2f".format(state.transformHandleStep)}m", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = state.transformHandleStep,
            onValueChange = onTransformStep,
            valueRange = 0.01f..0.3f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onApplyGestureHandle(-1f) }, modifier = Modifier.weight(1f)) { Text("-") }
            OutlinedButton(onClick = { onApplyGestureHandle(1f) }, modifier = Modifier.weight(1f)) { Text("+") }
        }
        state.lastSnapResult?.let { snap ->
            AssistChip(onClick = {}, label = { Text(if (snap.snapped) "Snap: ${snap.label}" else "Free placement") })
        }
    }
}

@Composable
private fun ArGraphAnalysisControls(
    state: ArViewerUiState,
    onComparisonEquation: (String) -> Unit,
    onAnalysisFocus: (Float) -> Unit
) {
    val report = state.arGraphAnalysis
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.comparisonEquation,
            onValueChange = onComparisonEquation,
            label = { Text("Compare / intersection equation") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text("Tangent focus x=${"%.2f".format(state.arGraphAnalysisFocusX)}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = state.analysisFocusProgress(),
            onValueChange = onAnalysisFocus,
            valueRange = 0f..1f
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AssistChip(onClick = {}, label = { Text("Roots ${report.roots.size}") }) }
            item { AssistChip(onClick = {}, label = { Text("Intersections ${report.intersections.size}") }) }
            item { AssistChip(onClick = {}, label = { Text("Extrema ${report.extrema.size}") }) }
            item { AssistChip(onClick = {}, label = { Text("Inflections ${report.inflections.size}") }) }
        }
        report.tangent?.let { tangent ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { AssistChip(onClick = {}, label = { Text("slope ${"%.3f".format(tangent.tangentSlope)}") }) }
                item { AssistChip(onClick = {}, label = { Text("normal ${formatSlope(tangent.normalSlope)}") }) }
                item { AssistChip(onClick = {}, label = { Text("angle ${"%.1f".format(tangent.tangentAngleDegrees)} deg") }) }
            }
        }
        report.integral?.let { integral ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { AssistChip(onClick = {}, label = { Text("area ${"%.3f".format(integral.absoluteArea)}") }) }
                item { AssistChip(onClick = {}, label = { Text("signed ${"%.3f".format(integral.signedArea)}") }) }
                item { AssistChip(onClick = {}, label = { Text("volume ${"%.3f".format(integral.volumeOfRevolution)}") }) }
                item { AssistChip(onClick = {}, label = { Text("arc ${"%.3f".format(integral.arcLength)}") }) }
            }
        }
        if (report.highlightedPoints.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(report.highlightedPoints.take(8)) { point ->
                    AssistChip(onClick = {}, label = { Text("${point.label} (${ "%.2f".format(point.x) }, ${ "%.2f".format(point.y) })") })
                }
            }
        }
        report.warnings.forEach { warning ->
            Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ConstructionGeometryPanel(
    state: ArViewerUiState,
    onAddConstructionPoint: () -> Unit,
    onConstructionLine: () -> Unit,
    onConstructionSegment: () -> Unit,
    onConstructionVector: () -> Unit,
    onConstructionPlane: () -> Unit,
    onConstructionCircle: () -> Unit,
    onConstructionPolygon: () -> Unit,
    onConstructionMidpoint: () -> Unit,
    onConstructionParallel: () -> Unit,
    onConstructionPerpendicular: () -> Unit,
    onConstructionConstraint: (ConstructionConstraintKind) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AssistChip(onClick = {}, label = { Text("Points ${state.constructionGeometry.points.size}") }) }
            item { AssistChip(onClick = {}, label = { Text("Objects ${state.constructionGeometry.objects.size}") }) }
            item { AssistChip(onClick = {}, label = { Text("Constraints ${state.constructionGeometry.constraints.size}") }) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedButton(onClick = onAddConstructionPoint) { Text("Point") } }
            item { OutlinedButton(onClick = onConstructionLine) { Text("Line") } }
            item { OutlinedButton(onClick = onConstructionSegment) { Text("Segment") } }
            item { OutlinedButton(onClick = onConstructionVector) { Text("Vector AB") } }
            item { OutlinedButton(onClick = onConstructionPlane) { Text("Plane ABC") } }
            item { OutlinedButton(onClick = onConstructionCircle) { Text("Circle") } }
            item { OutlinedButton(onClick = onConstructionPolygon) { Text("Polygon") } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedButton(onClick = onConstructionMidpoint) { Text("Midpoint") } }
            item { OutlinedButton(onClick = onConstructionParallel) { Text("Parallel") } }
            item { OutlinedButton(onClick = onConstructionPerpendicular) { Text("Perpendicular") } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    ConstructionConstraintKind.Parallel,
                    ConstructionConstraintKind.Perpendicular,
                    ConstructionConstraintKind.EqualLength,
                    ConstructionConstraintKind.FixedDistance
                )
            ) { constraint ->
                FilterChip(
                    selected = state.constructionGeometry.constraints.any { it.kind == constraint },
                    onClick = { onConstructionConstraint(constraint) },
                    label = { Text(constraint.shortLabel()) }
                )
            }
        }
        if (state.resolvedConstructions.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.resolvedConstructions.takeLast(8)) { construction ->
                    AssistChip(onClick = {}, label = { Text("${construction.label}: ${construction.valueLabel}") })
                }
            }
        }
    }
}

@Composable
private fun ProductionArPanel(
    state: ArViewerUiState,
    onCapturePersistentAnchor: () -> Unit,
    onDepthOcclusionMode: (ArDepthOcclusionMode) -> Unit,
    onPerformanceProfile: (ArPerformanceProfile) -> Unit,
    onMeshDensity: (Float) -> Unit,
    onMaxSceneObjects: (Float) -> Unit,
    onMarkScreenshotReady: () -> Unit,
    onExportArScene: () -> Unit,
    onApplyArTemplate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Production AR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArSceneTemplates.templates) { template ->
                FilterChip(
                    selected = state.selectedTemplateId == template.id,
                    onClick = { onApplyArTemplate(template.id) },
                    label = { Text(template.title) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AssistChip(onClick = {}, label = { Text("Anchor ${state.mathScene.persistentAnchor.kind}") }) }
            item { AssistChip(onClick = {}, label = { Text(state.mathScene.persistentAnchor.restoreHint) }) }
            item { AssistChip(onClick = {}, label = { Text("Mesh ${(state.meshDensity * 100f).toInt()}%") }) }
            item { AssistChip(onClick = {}, label = { Text("Max ${state.maxSceneObjects}") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCapturePersistentAnchor, modifier = Modifier.weight(1f)) { Text("Save anchor") }
            OutlinedButton(onClick = onExportArScene, modifier = Modifier.weight(1f)) { Text("Export") }
            OutlinedButton(onClick = onMarkScreenshotReady, modifier = Modifier.weight(1f)) { Text("Shot") }
        }
        Text("Depth / occlusion", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArDepthOcclusionMode.entries) { mode ->
                FilterChip(
                    selected = state.depthOcclusionMode == mode,
                    onClick = { onDepthOcclusionMode(mode) },
                    label = { Text(mode.shortLabel()) }
                )
            }
        }
        Text("Performance profile", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArPerformanceProfile.entries) { profile ->
                FilterChip(
                    selected = state.performanceProfile == profile,
                    onClick = { onPerformanceProfile(profile) },
                    label = { Text(profile.shortLabel()) }
                )
            }
        }
        Text("Mesh density ${(state.meshDensity * 100f).toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = state.meshDensity,
            onValueChange = onMeshDensity,
            valueRange = 0.1f..1f
        )
        Text("Scene object budget ${state.maxSceneObjects}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = state.maxSceneObjects.toFloat(),
            onValueChange = onMaxSceneObjects,
            valueRange = 4f..128f,
            steps = 30
        )
        state.exportedScenePackage?.let { share ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ElevatedAssistChip(onClick = {}, label = { Text(share.fileName) }) }
                item { AssistChip(onClick = {}, label = { Text(share.mimeType) }) }
                item { AssistChip(onClick = {}, label = { Text("${share.payload.length} bytes") }) }
                item { AssistChip(onClick = {}, label = { Text(share.summary) }) }
            }
        }
    }
}

@Composable
private fun ArWorkflowStudioPanel(
    state: ArViewerUiState,
    onSelectArWorkflow: (String) -> Unit,
    onCompleteWorkflowStep: () -> Unit,
    onAdvanceWorkflowStep: () -> Unit,
    onRefreshWorkflow: () -> Unit,
    onExportArActivity: () -> Unit
) {
    val template = ArWorkflowTemplates.templates.firstOrNull { it.id == state.activeWorkflowId }
        ?: ArWorkflowTemplates.templates.first()
    val step = state.workflowProgress.currentStep(template)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AR Workflow Studio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArWorkflowTemplates.templates) { item ->
                FilterChip(
                    selected = state.activeWorkflowId == item.id,
                    onClick = { onSelectArWorkflow(item.id) },
                    label = { Text(item.title) }
                )
            }
        }
        Text(template.description, style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ElevatedAssistChip(onClick = {}, label = { Text("${state.workflowEvaluation.completionPercent}% complete") }) }
            item { AssistChip(onClick = {}, label = { Text("Ready ${state.workflowEvaluation.readinessScore}%") }) }
            item { AssistChip(onClick = {}, label = { Text(template.recommendedMode) }) }
            item { AssistChip(onClick = {}, label = { Text(template.equation) }) }
        }
        LinearProgressIndicator(
            progress = { state.workflowEvaluation.completionPercent / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        step?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(it.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(it.actionHint, style = MaterialTheme.typography.bodySmall)
                    Text(it.successLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onCompleteWorkflowStep, modifier = Modifier.weight(1f)) { Text("Done") }
                        OutlinedButton(onClick = onAdvanceWorkflowStep, modifier = Modifier.weight(1f)) { Text("Next") }
                        OutlinedButton(onClick = onRefreshWorkflow, modifier = Modifier.weight(1f)) { Text("Check") }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onExportArActivity, modifier = Modifier.weight(1f)) { Text("Export Activity") }
        }
        if (state.workflowEvaluation.badges.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.workflowEvaluation.badges) { badge ->
                    ElevatedAssistChip(onClick = {}, label = { Text(badge) })
                }
            }
        }
        state.workflowEvaluation.warnings.take(3).forEach { warning ->
            Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        state.exportedActivityPackage?.let { activity ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ElevatedAssistChip(onClick = {}, label = { Text(activity.fileName) }) }
                item { AssistChip(onClick = {}, label = { Text(activity.mimeType) }) }
                item { AssistChip(onClick = {}, label = { Text("${activity.payload.length} bytes") }) }
                item { AssistChip(onClick = {}, label = { Text(activity.summary) }) }
            }
        }
    }
}

@Composable
private fun Drawing2dTo3dPanel(
    state: ArViewerUiState,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onAnimationMode: (GraphAnimationMode) -> Unit,
    onSlicePosition: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Draw graph", "Select function", "Transform", "Slice", "Color map")) { label ->
                AssistChip(onClick = {}, label = { Text(label) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Function3dTransformMode.entries) { mode ->
                FilterChip(
                    selected = state.function3dTransformMode == mode,
                    onClick = { onFunction3dTransform(mode) },
                    label = { Text(mode.label()) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onGraphColorMap(state.graphColorMap.next()) }, modifier = Modifier.weight(1f)) {
                Text(state.graphColorMap.label())
            }
            OutlinedButton(onClick = { onSlicePosition(state.graphSlicePosition) }, modifier = Modifier.weight(1f)) {
                Text("Slice ${(state.graphSlicePosition * 100f).toInt()}%")
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GraphColorMap.entries) { colorMap ->
                FilterChip(
                    selected = state.graphColorMap == colorMap,
                    onClick = { onGraphColorMap(colorMap) },
                    label = { ColorMapChip(colorMap) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GraphAnimationMode.entries) { mode ->
                FilterChip(
                    selected = state.graphAnimationMode == mode,
                    onClick = { onAnimationMode(mode) },
                    label = { Text(mode.label()) }
                )
            }
        }
        Text("${state.graphAnimationMode.label()} ${(state.graphAnimationProgress * 100f).toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = state.graphAnimationProgress, onValueChange = onAnimationProgress)
    }
}

@Composable
private fun SolarSystemArPanel(
    state: ArViewerUiState,
    onAnimationProgress: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Orrery", "Planet scale", "Orbit paths", "Distance", "Rotation")) { label ->
                AssistChip(onClick = {}, label = { Text(label) })
            }
        }
        Text("Orbit timeline ${(state.graphAnimationProgress * 100f).toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = state.graphAnimationProgress, onValueChange = onAnimationProgress)
    }
}

@Composable
private fun FloatingMathToolbar(
    state: ArViewerUiState,
    onTool: (FloatingMathTool) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FloatingMathTool.entries) { tool ->
            FilterChip(
                selected = state.activeFloatingTool == tool && tool.isEnabled(state),
                onClick = { onTool(tool) },
                label = { Text(tool.shortLabel()) }
            )
        }
    }
}

@Composable
private fun PaperGraphModePanel(
    state: ArViewerUiState,
    onCalibrationStep: () -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onAnimationMode: (GraphAnimationMode) -> Unit,
    onSlicePosition: (Float) -> Unit,
    onPaperGraphLayer: (PaperGraphLayer) -> Unit
) {
    val paper = state.paperGraph
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    if (paper?.hasLockedTarget == true) "Worksheet locked" else "Find worksheet",
                    "Target images ${paper?.configuredReferenceCount ?: 1}",
                    "Tap ${state.paperGraphCalibration.step.stepLabel()}",
                    if (state.paperGraphCalibration.isComplete) "Coordinate system ready" else "Calibration pending"
                )
            ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PaperGraphLayer.entries) { layer ->
                FilterChip(
                    selected = layer in state.paperGraphLayers,
                    onClick = { onPaperGraphLayer(layer) },
                    label = { Text(layer.label()) }
                )
            }
        }
        PaperGraphCalibrationStrip(state)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onCalibrationStep, modifier = Modifier.weight(1f)) {
                Text(if (state.paperGraphCalibration.isComplete) "Reset Calibration" else "Lock ${state.paperGraphCalibration.step.stepLabel()}")
            }
            OutlinedButton(onClick = { onGraphColorMap(state.graphColorMap.next()) }, modifier = Modifier.weight(1f)) {
                Text(state.graphColorMap.label())
            }
        }
        if (state.slicePlaneVisible) {
            Text("Slice plane ${"%.0f".format(state.graphSlicePosition * 100f)}%", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.graphSlicePosition, onValueChange = onSlicePosition)
        }
        if (state.graphAnimationEnabled) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GraphAnimationMode.entries) { mode ->
                    FilterChip(
                        selected = state.graphAnimationMode == mode,
                        onClick = { onAnimationMode(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
            Text("${state.graphAnimationMode.label()} ${"%.0f".format(state.graphAnimationProgress * 100f)}%", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.graphAnimationProgress, onValueChange = onAnimationProgress)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GraphColorMap.entries) { colorMap ->
                FilterChip(
                    selected = state.graphColorMap == colorMap,
                    onClick = { onGraphColorMap(colorMap) },
                    label = { ColorMapChip(colorMap) }
                )
            }
        }
        paper?.trackedTargets?.firstOrNull()?.let { target ->
            Text(
                "${target.name}: ${"%.2f".format(target.extentX)}m x ${"%.2f".format(target.extentZ)}m, ${target.trackingMethod}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PaperGraphCalibrationStrip(state: ArViewerUiState) {
    val calibration = state.paperGraphCalibration
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(
            listOf(
                "Origin" to calibration.originLocked,
                "X-axis point" to calibration.xAxisLocked,
                "Y-axis point" to calibration.yAxisLocked
            )
        ) { (label, locked) ->
            AssistChip(
                onClick = {},
                label = { Text("${if (locked) "Locked" else "Tap"} $label") }
            )
        }
    }
    val origin = calibration.origin
    val xAxis = calibration.xAxisPoint
    val yAxis = calibration.yAxisPoint
    if (origin != null && xAxis != null && yAxis != null) {
        val xScale = distance(origin, xAxis)
        val yScale = distance(origin, yAxis)
        Text(
            "Scale: X unit ${"%.2f".format(xScale)}m, Y unit ${"%.2f".format(yScale)}m",
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        Text(
            if (state.paperGraph?.hasLockedTarget == true) "Tap the drawn origin, then one point on X, then one point on Y." else "Point at the AI STEM Paper Graph target to begin.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DirectUserEnhancementPanel(
    state: ArViewerUiState,
    onLayers: () -> Unit,
    onCapture: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    "Axes ${onOff(state.axesVisible)}",
                    "Grid ${onOff(state.gridVisible)}",
                    "Labels ${onOff(state.labelsVisible)}",
                    "Formula ${if (state.formulaCardsCollapsed) "chip" else "card"}",
                    "Measure ${onOff(state.measurementsVisible)}",
                    "Slice ${onOff(state.slicePlaneVisible)}",
                    "Color ${state.graphColorMap.label()}"
                )
            ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onLayers, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Layers, null)
                Text("Scene")
            }
            OutlinedButton(onClick = onCapture, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Save, null)
                Text("Capture")
            }
        }
        state.mathScene.primarySelectedObject?.let { selected ->
            TapToExplainStrip(selected)
        }
    }
}

@Composable
private fun TapToExplainStrip(selected: MathSceneObject) {
    val labels = when (selected.objectType) {
        MathObjectType.Cube, MathObjectType.RectangularPrism -> listOf("vertex", "edge", "face", "volume")
        MathObjectType.Sphere -> listOf("radius", "diameter", "surface", "volume")
        MathObjectType.Cylinder, MathObjectType.Cone -> listOf("radius", "height", "base", "volume")
        MathObjectType.SineCurve -> listOf("amplitude", "period", "midline", "slope")
        MathObjectType.CoordinatePlane -> listOf("origin", "x-axis", "y-axis", "quadrant")
        MathObjectType.VectorArrow -> listOf("magnitude", "direction", "component", "displacement")
        MathObjectType.NumberLine -> listOf("origin", "unit", "distance", "interval")
        MathObjectType.Triangle, MathObjectType.Circle -> listOf("point", "angle", "area", "perimeter")
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels) { label -> ElevatedAssistChip(onClick = {}, label = { Text(label) }) }
    }
}

private fun FloatingMathTool.shortLabel(): String = when (this) {
    FloatingMathTool.Axes -> "Axes"
    FloatingMathTool.Grid -> "Grid"
    FloatingMathTool.Labels -> "Labels"
    FloatingMathTool.Formula -> "Formula"
    FloatingMathTool.Measure -> "Measure"
    FloatingMathTool.Slice -> "Slice"
    FloatingMathTool.Animate -> "Animate"
    FloatingMathTool.Capture -> "Capture"
}

private fun FloatingMathTool.isEnabled(state: ArViewerUiState): Boolean = when (this) {
    FloatingMathTool.Axes -> state.axesVisible
    FloatingMathTool.Grid -> state.gridVisible
    FloatingMathTool.Labels -> state.labelsVisible
    FloatingMathTool.Formula -> state.formulaCardsVisible
    FloatingMathTool.Measure -> state.measurementsVisible
    FloatingMathTool.Slice -> state.slicePlaneVisible
    FloatingMathTool.Animate -> state.graphAnimationEnabled
    FloatingMathTool.Capture -> state.captureRequested
}

private fun ArFeaturePhase.label(): String = when (this) {
    ArFeaturePhase.DirectInteraction -> "Phase 1: Interact"
    ArFeaturePhase.GraphAnalysis -> "Phase 2: Analyze"
    ArFeaturePhase.EngineStrengthening -> "Phase 3: Engine"
    ArFeaturePhase.WorkflowStudio -> "Phase 6: Workflow"
}

private fun ArFeaturePhase.features(): List<MathArFeature> = when (this) {
    ArFeaturePhase.DirectInteraction -> listOf(
        MathArFeature.ObjectSnapping,
        MathArFeature.GestureHandles,
        MathArFeature.LiveEquationEditing,
        MathArFeature.PointPicker,
        MathArFeature.MeasurementRulerAnchors
    )
    ArFeaturePhase.GraphAnalysis -> listOf(
        MathArFeature.RootVisualizer,
        MathArFeature.TangentNormalTool,
        MathArFeature.AreaUnderCurve,
        MathArFeature.VolumeBuilder,
        MathArFeature.MultiObjectConstraints
    )
    ArFeaturePhase.EngineStrengthening -> listOf(
        MathArFeature.CoordinateGridLocking,
        MathArFeature.DepthOcclusion,
        MathArFeature.ScenePersistence,
        MathArFeature.PrecisionConfidenceHud,
        MathArFeature.CompareMode
    )
    ArFeaturePhase.WorkflowStudio -> listOf(
        MathArFeature.GuidedWorkflow,
        MathArFeature.ActivityExport,
        MathArFeature.EvidenceRecorder,
        MathArFeature.PrecisionConfidenceHud,
        MathArFeature.ScenePersistence
    )
}

private fun MathArFeature.shortLabel(): String = when (this) {
    MathArFeature.ObjectSnapping -> "Snap"
    MathArFeature.GestureHandles -> "Handles"
    MathArFeature.LiveEquationEditing -> "Equation"
    MathArFeature.PointPicker -> "Point"
    MathArFeature.RootVisualizer -> "Roots"
    MathArFeature.TangentNormalTool -> "Tangent"
    MathArFeature.AreaUnderCurve -> "Area"
    MathArFeature.VolumeBuilder -> "Volume"
    MathArFeature.MeasurementRulerAnchors -> "Ruler"
    MathArFeature.CoordinateGridLocking -> "Grid Lock"
    MathArFeature.MultiObjectConstraints -> "Constraints"
    MathArFeature.DepthOcclusion -> "Occlusion"
    MathArFeature.ScenePersistence -> "Persist"
    MathArFeature.PrecisionConfidenceHud -> "Confidence"
    MathArFeature.CompareMode -> "Compare"
    MathArFeature.GuidedWorkflow -> "Workflow"
    MathArFeature.ActivityExport -> "Activity"
    MathArFeature.EvidenceRecorder -> "Evidence"
}

private fun ArGestureHandle.shortLabel(): String = when (this) {
    ArGestureHandle.MoveX -> "Move X"
    ArGestureHandle.MoveY -> "Move Y"
    ArGestureHandle.MoveZ -> "Move Z"
    ArGestureHandle.RotateY -> "Rotate"
    ArGestureHandle.UniformScale -> "Scale"
    ArGestureHandle.StretchX -> "Stretch X"
    ArGestureHandle.StretchZ -> "Stretch Z"
    ArGestureHandle.Lift -> "Lift"
}

private fun ConstructionConstraintKind.shortLabel(): String = when (this) {
    ConstructionConstraintKind.FixedDistance -> "Fixed distance"
    ConstructionConstraintKind.Parallel -> "Parallel"
    ConstructionConstraintKind.Perpendicular -> "Perpendicular"
    ConstructionConstraintKind.EqualLength -> "Equal length"
    ConstructionConstraintKind.FixedAngle -> "Fixed angle"
    ConstructionConstraintKind.Coincident -> "Coincident"
}

private fun ArDepthOcclusionMode.shortLabel(): String = when (this) {
    ArDepthOcclusionMode.Off -> "Off"
    ArDepthOcclusionMode.SoftDepth -> "Soft"
    ArDepthOcclusionMode.DepthTest -> "Depth test"
    ArDepthOcclusionMode.GeospatialDepth -> "Geo depth"
}

private fun ArPerformanceProfile.shortLabel(): String = when (this) {
    ArPerformanceProfile.BatterySaver -> "Battery"
    ArPerformanceProfile.Balanced -> "Balanced"
    ArPerformanceProfile.HighQuality -> "High"
    ArPerformanceProfile.Presentation -> "Present"
}

private fun PaperGraphCalibrationStep.stepLabel(): String = when (this) {
    PaperGraphCalibrationStep.Origin -> "Origin"
    PaperGraphCalibrationStep.XAxisPoint -> "X Axis"
    PaperGraphCalibrationStep.YAxisPoint -> "Y Axis"
    PaperGraphCalibrationStep.Complete -> "Ready"
}

private fun GraphColorMap.label(): String = when (this) {
    GraphColorMap.Height -> "Height"
    GraphColorMap.Slope -> "Slope"
    GraphColorMap.Curvature -> "Curvature"
    GraphColorMap.XValue -> "X value"
    GraphColorMap.YValue -> "Y value"
}

@Composable
private fun ColorMapChip(colorMap: GraphColorMap) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            colorMap.palette().forEach { color ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Text(colorMap.label())
    }
}

private fun GraphAnimationMode.label(): String = when (this) {
    GraphAnimationMode.RotateGraph -> "Rotate graph"
    GraphAnimationMode.SweepArea -> "Sweep area"
    GraphAnimationMode.BuildVolume -> "Build volume"
    GraphAnimationMode.MoveTangentPoint -> "Tangent point"
    GraphAnimationMode.AnimateSineWave -> "Sine wave"
}

private fun Function3dTransformMode.label(): String = when (this) {
    Function3dTransformMode.Surface -> "Surface"
    Function3dTransformMode.Extrusion -> "Extrusion"
    Function3dTransformMode.SolidOfRevolution -> "Revolution"
    Function3dTransformMode.TangentPlane -> "Tangent plane"
    Function3dTransformMode.CrossSectionSlices -> "Cross-sections"
}

private fun PaperGraphLayer.label(): String = when (this) {
    PaperGraphLayer.Axes -> "Axes"
    PaperGraphLayer.Scale -> "Scale"
    PaperGraphLayer.Graph -> "Graph"
    PaperGraphLayer.Surface3d -> "3D Surface"
    PaperGraphLayer.CrossSection -> "Cross Section"
}

private fun distance(a: PaperGraphCalibrationPoint, b: PaperGraphCalibrationPoint): Float =
    sqrt((a.worldX - b.worldX) * (a.worldX - b.worldX) + (a.worldY - b.worldY) * (a.worldY - b.worldY) + (a.worldZ - b.worldZ) * (a.worldZ - b.worldZ))

private fun GraphColorMap.next(): GraphColorMap {
    val values = GraphColorMap.entries
    return values[(values.indexOf(this) + 1) % values.size]
}

private fun GraphQualityPreset.shortLabel(): String = when (this) {
    GraphQualityPreset.BatterySaver -> "Lite"
    GraphQualityPreset.Balanced -> "Balanced"
    GraphQualityPreset.HighQuality -> "High"
    GraphQualityPreset.Presentation -> "Studio"
}

private data class ArDomainPreset(val label: String, val domain: ArGraphDomain)

private val arDomainPresets = listOf(
    ArDomainPreset("Close", ArGraphDomain(xMin = -3.0, xMax = 3.0, yMin = -3.0, yMax = 3.0, zMin = -3.0, zMax = 3.0, valueClamp = 3.0)),
    ArDomainPreset("Trig", ArGraphDomain()),
    ArDomainPreset("Wide", ArGraphDomain(xMin = -10.0, xMax = 10.0, yMin = -10.0, yMax = 10.0, zMin = -6.0, zMax = 6.0, valueClamp = 6.0)),
    ArDomainPreset("Surface", ArGraphDomain(xMin = -5.0, xMax = 5.0, yMin = -5.0, yMax = 5.0, zMin = -5.0, zMax = 5.0, valueClamp = 5.0))
)

private fun com.indianservers.ai_stem.domain.graph.GraphSlider.sliderSteps(): Int {
    val count = ((maximum - minimum) / step).toInt()
    return (count - 1).coerceIn(0, 80)
}

private fun ArViewerUiState.analysisFocusProgress(): Float {
    val span = (arGraphDomain.xMax - arGraphDomain.xMin).coerceAtLeast(0.001)
    return ((arGraphAnalysisFocusX - arGraphDomain.xMin) / span).toFloat().coerceIn(0f, 1f)
}

private fun Float.coerceAwayFromZero(): Float =
    if (abs(this) < 0.001f) 0.001f else this

private fun formatSlope(value: Double): String =
    if (value.isFinite()) "%.3f".format(value) else "vertical"

private fun onOff(enabled: Boolean): String = if (enabled) "on" else "off"

@Composable
private fun OutdoorGeospatialMathPanel(state: ArViewerUiState) {
    val outdoor = state.outdoorGeospatial
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.locationPermission != LocationPermissionState.Granted) {
            Text(
                "Precise location is required for VPS, Streetscape Geometry, building anchors, and geospatial depth.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    "Scan street/buildings",
                    "Show building mesh",
                    "Tap building/terrain",
                    "Measure height",
                    "Distance/angle",
                    "Outdoor graphs"
                )
            ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
        }
        if (outdoor == null || outdoor.totalMeshCount == 0) {
            Text(
                "Move outdoors and face nearby buildings or terrain. Streetscape Geometry depends on Geospatial API setup, VPS, and Street View coverage.",
                style = MaterialTheme.typography.bodySmall
            )
            return@Column
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    "Meshes ${outdoor.totalMeshCount}",
                    "Buildings ${outdoor.buildingCount}",
                    "Terrain ${outdoor.terrainCount}",
                    "LOD1 ${outdoor.lod1Count}",
                    "LOD2 ${outdoor.lod2Count}",
                    "Depth occlusion ${if (outdoor.enabled) "on" else "off"}"
                )
            ) { label -> AssistChip(onClick = {}, label = { Text(label) }) }
        }
        outdoor.selectedMetrics?.let { metrics ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "height ${formatMeters(metrics.estimatedHeightMeters)}",
                        "area ${formatSquareMeters(metrics.footprintAreaSquareMeters)}",
                        "volume ${formatCubicMeters(metrics.volumeApproxCubicMeters)}",
                        "angle ${"%.1f".format(metrics.angleOfElevationDegrees)} deg",
                        "slope ${"%.2f".format(metrics.slopeToRoofline)}"
                    )
                ) { label -> ElevatedAssistChip(onClick = {}, label = { Text(label) }) }
            }
        }
        outdoor.trackedMeshes.take(4).forEach { mesh ->
            OutdoorMeshRow(mesh)
        }
    }
}

@Composable
private fun OutdoorMeshRow(mesh: OutdoorGeometryObservation) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${mesh.type.label()} / ${mesh.quality.label()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${mesh.mesh.vertexCount}v ${mesh.mesh.triangleCount}tri",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun OutdoorGeometryType.label(): String = when (this) {
    OutdoorGeometryType.Building -> "Building"
    OutdoorGeometryType.Terrain -> "Terrain"
    OutdoorGeometryType.Unknown -> "Unknown"
}

private fun OutdoorMeshQuality.label(): String = when (this) {
    OutdoorMeshQuality.Lod1 -> "LOD 1"
    OutdoorMeshQuality.Lod2 -> "LOD 2"
    OutdoorMeshQuality.Terrain -> "Terrain"
    OutdoorMeshQuality.Unknown -> "Unknown"
}

private fun formatMeters(value: Float): String = "${"%.1f".format(value)} m"
private fun formatSquareMeters(value: Float): String = "${"%.1f".format(value)} m2"
private fun formatCubicMeters(value: Float): String = "${"%.1f".format(value)} m3"

@Composable
private fun NodeScope.OutdoorStreetscapeWireframe(
    edges: List<OutdoorWireframeEdge>,
    materialLoader: MaterialLoader
) {
    if (edges.isEmpty()) return
    val material = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x994EE7FF)) }
    edges.forEach { edge ->
        LineNode(
            Position(edge.startX, edge.startY, edge.startZ),
            Position(edge.endX, edge.endY, edge.endZ),
            material
        )
    }
}

@Composable
private fun SceneMenuContent(
    state: ArViewerUiState,
    selected: MathSceneObject?,
    onSelectSceneObject: (String, Boolean) -> Unit,
    onMode: (SceneInteractionMode) -> Unit,
    onMove: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDuplicate: () -> Unit,
    onLock: () -> Unit,
    onHide: () -> Unit,
    onInspector: () -> Unit,
    onDiagnostics: () -> Unit,
    onLayers: () -> Unit,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onSave: () -> Unit,
    onClearScene: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.mathScene.objects) { item ->
                val label = if (item.visibility.visible) item.displayName else "${item.displayName} (hidden)"
                FilterChip(
                    selected = item.interactionState.selected,
                    onClick = { onSelectSceneObject(item.id, state.experienceMode == ExperienceMode.Advanced) },
                    label = { Text(label) },
                    leadingIcon = { ObjectPreviewDot(item.objectType) }
                )
            }
        }
        if (state.experienceMode == ExperienceMode.Advanced) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(SceneInteractionMode.Select, SceneInteractionMode.Move, SceneInteractionMode.Rotate, SceneInteractionMode.Scale, SceneInteractionMode.Measure, SceneInteractionMode.MultiSelect)) { mode ->
                    FilterChip(selected = state.sceneInteractionMode == mode, onClick = { onMode(mode) }, label = { Text(mode.simpleName()) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onMove, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.OpenInFull, null); Text("Move") }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.RestartAlt, null); Text("Reset") }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), enabled = state.placedObject != null) { Icon(Icons.Outlined.Delete, null); Text("Delete") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onUndo, enabled = state.history.canUndo, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Undo, null); Text("Undo") }
            OutlinedButton(onClick = onRedo, enabled = state.history.canRedo, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Redo, null); Text("Redo") }
            OutlinedButton(onClick = onDuplicate, enabled = state.mathScene.selectedObjectIds.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.ContentCopy, null); Text("Copy") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onLock, enabled = selected != null, modifier = Modifier.weight(1f)) {
                Icon(if (selected?.interactionState?.locked == true) Icons.Outlined.LockOpen else Icons.Outlined.Lock, null)
                Text(if (selected?.interactionState?.locked == true) "Unlock" else "Lock")
            }
            OutlinedButton(onClick = onHide, enabled = selected != null, modifier = Modifier.weight(1f)) {
                Icon(if (selected?.visibility?.visible == true) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                Text(if (selected?.visibility?.visible == true) "Hide" else "Show")
            }
            OutlinedButton(onClick = onInspector, enabled = selected != null, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Info, null); Text("Details") }
        }
        OutlinedButton(onClick = onDiagnostics, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Info, null)
            Text(if (state.diagnosticsVisible) "Hide AR Diagnostics" else "Show AR Diagnostics")
        }
        if (state.diagnosticsVisible) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AR Diagnostics", style = MaterialTheme.typography.titleSmall)
                    state.arDiagnostics.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (state.experienceMode == ExperienceMode.Advanced) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onLayers, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Layers, null); Text("Layers") }
                OutlinedButton(onClick = onGroup, enabled = state.mathScene.selectedObjectIds.size >= 2, modifier = Modifier.weight(1f)) { Text("Group") }
                OutlinedButton(onClick = onUngroup, enabled = selected?.groupId != null, modifier = Modifier.weight(1f)) { Text("Ungroup") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Save, null); Text("Save") }
            OutlinedButton(onClick = onClearScene, enabled = state.mathScene.objects.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Delete, null); Text("Clear") }
        }
        selected?.let {
            AssistChip(onClick = {}, label = { Text("${it.displayName}: ${it.objectType.displayName}") })
            val definition = DefaultMathObjectRegistry.getDefinition(it.definitionId)
            val firstMeasurement = definition?.measurementProvider?.invoke(it.parameters)?.firstOrNull()
            if (firstMeasurement != null) {
                ElevatedAssistChip(onClick = {}, label = { Text("${firstMeasurement.label}: ${MeasurementFormatter.number(firstMeasurement.value)} ${firstMeasurement.unitLabel.orEmpty()}") })
            }
        }
    }
}

@Composable
private fun Reticle(state: ArViewerUiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(52.dp)) {
            val color = when (state.placementQuality) {
                PlacementQuality.Excellent -> Color(0xFF3DFF9F)
                PlacementQuality.Good -> Color(0xFF62D6C7)
                PlacementQuality.Weak -> Color(0xFFFFC857)
                PlacementQuality.Recovering -> Color(0xFFFFA726)
                PlacementQuality.Lost -> Color(0xFFFF6B6B)
                PlacementQuality.Unknown -> Color(0xFFFFC857)
            }
            drawCircle(color.copy(alpha = 0.18f), radius = size.minDimension / 2f)
            drawCircle(color, radius = size.minDimension / 7f)
        }
    }
}

@Composable
private fun TrackingHealthOverlay(state: ArViewerUiState) {
    val items = state.smartScanItems()
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp, start = 12.dp, end = 12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            shape = MaterialTheme.shapes.small
        ) {
            LazyRow(
                Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items) { item ->
                    SmartScanPill(item)
                }
            }
        }
    }
}

private data class SmartScanItem(
    val label: String,
    val color: Color,
    val active: Boolean
)

@Composable
private fun SmartScanPill(item: SmartScanItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(if (item.active) 9.dp else 7.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = if (item.active) 1f else 0.35f))
        )
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (item.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    }
}

private fun ArViewerUiState.smartScanItems(): List<SmartScanItem> {
    val surfaceFound = placementHitKind in setOf(PlacementHitKind.Plane, PlacementHitKind.DepthPoint)
    val imageLocked = paperGraph?.hasLockedTarget == true
    val buildingMeshFound = (outdoorGeospatial?.totalMeshCount ?: 0) > 0 || placementHitKind == PlacementHitKind.StreetscapeGeometry
    val moveSlower = !motionStable || trackingStatus == TrackingStatus.Limited || recoveryMode == ArRecoveryMode.TrackingLimited
    val needMoreLight = !lightStable || guidanceInstruction.contains("light", ignoreCase = true) || trackingMessage.contains("light", ignoreCase = true)
    return when (arEngineMode) {
        ArEngineMode.PaperGraph -> listOf(
            SmartScanItem("Image locked", if (imageLocked) Color(0xFF3DFF9F) else Color(0xFFFFC857), imageLocked),
            SmartScanItem("Move slower", Color(0xFFFFA726), moveSlower),
            SmartScanItem("Need more light", Color(0xFFFF6B6B), needMoreLight)
        )
        ArEngineMode.OutdoorGeospatialMath -> listOf(
            SmartScanItem("Building mesh found", if (buildingMeshFound) Color(0xFF3DFF9F) else Color(0xFFFFC857), buildingMeshFound),
            SmartScanItem("Move slower", Color(0xFFFFA726), moveSlower),
            SmartScanItem("Need more light", Color(0xFFFF6B6B), needMoreLight)
        )
        ArEngineMode.SurfacePlacement -> listOf(
            SmartScanItem("Surface found", if (surfaceFound) Color(0xFF3DFF9F) else Color(0xFFFFC857), surfaceFound),
            SmartScanItem("Move slower", Color(0xFFFFA726), moveSlower),
            SmartScanItem("Need more light", Color(0xFFFF6B6B), needMoreLight)
        )
        ArEngineMode.AirPlacement -> listOf(
            SmartScanItem("Air point found", if (hasValidPlacementHit) Color(0xFF3DFF9F) else Color(0xFFFFC857), hasValidPlacementHit),
            SmartScanItem("Move slower", Color(0xFFFFA726), moveSlower),
            SmartScanItem("Need more light", Color(0xFFFF6B6B), needMoreLight)
        )
        ArEngineMode.Indoor -> listOf(
            SmartScanItem("Surface found", if (surfaceFound) Color(0xFF3DFF9F) else Color(0xFFFFC857), surfaceFound),
            SmartScanItem("Move slower", Color(0xFFFFA726), moveSlower),
            SmartScanItem("Need more light", Color(0xFFFF6B6B), needMoreLight)
        )
    }
}

@Composable
private fun MathInteractionOverlay(state: ArViewerUiState) {
    Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterEnd) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.labelsVisible) {
                state.mathScene.primarySelectedObject?.let { selected ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(selected.displayName, style = MaterialTheme.typography.labelLarge)
                            Text(selected.objectType.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.measurementsVisible) {
                        MeasurementOverlayCards(selected = selected, state = state)
                    }
                    if (state.formulaCardsVisible) {
                        FormulaOverlayCards(
                            selected = selected,
                            state = state
                        )
                    }
                }
            }
            if (state.slicePlaneVisible) {
                AssistChip(onClick = {}, label = { Text("Slice ${(state.graphSlicePosition * 100f).toInt()}%") })
            }
            if (state.graphAnimationEnabled) {
                AssistChip(onClick = {}, label = { Text("${state.graphAnimationMode.label()} ${(state.graphAnimationProgress * 100f).toInt()}%") })
            }
            if (state.captureRequested) {
                ElevatedAssistChip(onClick = {}, label = { Text("Capture ready") })
            }
            MathArFeatureOverlay(state)
        }
    }
    if (state.arEngineMode == ArEngineMode.PaperGraph) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 104.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    if (state.paperGraph?.hasLockedTarget == true) "Paper graph locked" else "Scan graph paper",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MathArFeatureOverlay(state: ArViewerUiState) {
    val chips = buildList {
        if (state.snappingEnabled) add("Snap: ${state.snapTargetLabel()}")
        if (state.coordinateGridLocked) add("Grid locked")
        if (state.compareModeEnabled) add("Compare: original vs transformed")
        if (state.depthOcclusionPolishEnabled) add("Depth occlusion on")
        if (MathArFeature.ScenePersistence in state.enabledMathArFeatures) add("Anchor: ${state.mathScene.persistentAnchor.kind}")
        add("Profile: ${state.performanceProfile.shortLabel()}")
        if (state.exportedScenePackage != null) add("Export ready")
        if (MathArFeature.GuidedWorkflow in state.enabledMathArFeatures) {
            add("Workflow ${state.workflowEvaluation.completionPercent}% | Ready ${state.workflowEvaluation.readinessScore}%")
        }
        if (state.exportedActivityPackage != null) add("Activity pack ready")
        if (MathArFeature.MultiObjectConstraints in state.enabledMathArFeatures) {
            add("Construction ${state.constructionGeometry.points.size} pts | ${state.constructionGeometry.objects.size} objs")
            if (state.constructionGeometry.constraints.isNotEmpty()) add("Constraints ${state.constructionGeometry.constraints.size}")
        }
        if (MathArFeature.RootVisualizer in state.enabledMathArFeatures) add("Roots ${state.arGraphAnalysis.roots.size} | Intersections ${state.arGraphAnalysis.intersections.size}")
        state.arGraphAnalysis.tangent?.let { tangent ->
            if (MathArFeature.TangentNormalTool in state.enabledMathArFeatures) add("Slope ${"%.3f".format(tangent.tangentSlope)} | Normal ${formatSlope(tangent.normalSlope)}")
        }
        state.arGraphAnalysis.integral?.let { integral ->
            if (MathArFeature.AreaUnderCurve in state.enabledMathArFeatures) add("Area ${"%.3f".format(integral.absoluteArea)}")
            if (MathArFeature.VolumeBuilder in state.enabledMathArFeatures) add("Volume ${"%.3f".format(integral.volumeOfRevolution)}")
        }
    }
    if (MathArFeature.LiveEquationEditing in state.enabledMathArFeatures) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), shape = MaterialTheme.shapes.small) {
            Text(state.liveEquation, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
    if (state.pickedPoints.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.pickedPoints) { point ->
                AssistChip(onClick = {}, label = { Text(point.label) })
            }
        }
    }
    if (state.pickedGraphPoints.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.pickedGraphPoints.takeLast(4)) { point ->
                AssistChip(onClick = {}, label = { Text("Graph ${point.label}") })
            }
        }
    }
    if (state.rulerAnchors.isNotEmpty()) {
        AssistChip(onClick = {}, label = { Text(state.rulerMeasurementLabel()) })
    }
    state.rulerMeasurement?.let { measurement ->
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { AssistChip(onClick = {}, label = { Text("rise ${"%.2f".format(measurement.rise)}") }) }
            item { AssistChip(onClick = {}, label = { Text("run ${"%.2f".format(measurement.run)}") }) }
            item { AssistChip(onClick = {}, label = { Text("angle ${"%.1f".format(measurement.angleDegrees)} deg") }) }
            item { AssistChip(onClick = {}, label = { Text("slope ${formatSlope(measurement.slope)}") }) }
        }
    }
    state.lastPlacementPoint?.takeIf { state.snappingEnabled || state.coordinateGridLocked }?.let { point ->
        AssistChip(onClick = {}, label = { Text("Anchor ${state.snapTargetLabel()} (${ "%.2f".format(point.x) }, ${ "%.2f".format(point.z) })") })
    }
    if (MathArFeature.PrecisionConfidenceHud in state.enabledMathArFeatures) {
        ConfidenceHudStrip(state)
    }
    if (chips.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(chips) { chip -> ElevatedAssistChip(onClick = {}, label = { Text(chip) }) }
        }
    }
}

@Composable
private fun ConfidenceHudStrip(state: ArViewerUiState) {
    val values = listOf(
        "Tracking ${state.placementScore}%",
        "Scale ${state.scaleConfidence()}",
        "Image ${if (state.paperGraph?.hasLockedTarget == true) "locked" else "scan"}",
        "Mesh ${state.outdoorGeospatial?.totalMeshCount ?: 0}",
        "Calibration ${if (state.paperGraphCalibration.isComplete) "ready" else "pending"}",
        "Snap ${if (state.snappingEnabled || state.coordinateGridLocked) "on" else "off"}",
        "Depth ${if (state.depthOcclusionPolishEnabled && state.placementHitKind == PlacementHitKind.DepthPoint) "active" else if (state.depthOcclusionPolishEnabled) "ready" else "off"}",
        "Persist ${state.savedScenes.size}",
        "Workflow ${state.workflowEvaluation.readinessScore}%"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(values) { value ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f), shape = MaterialTheme.shapes.small) {
                Text(value, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun ArViewerUiState.snapTargetLabel(): String = when {
    arEngineMode == ArEngineMode.PaperGraph && paperGraphCalibration.isComplete -> "paper axes"
    arEngineMode == ArEngineMode.OutdoorGeospatialMath -> "building edge"
    placementHitKind == PlacementHitKind.Plane -> "surface plane"
    else -> "AR grid"
}

private fun ArViewerUiState.scaleConfidence(): String = when {
    paperGraphCalibration.isComplete -> "paper"
    placementQuality in setOf(PlacementQuality.Excellent, PlacementQuality.Good) -> "good"
    else -> "rough"
}

private fun ArViewerUiState.rulerMeasurementLabel(): String {
    if (rulerAnchors.size < 2) return "Ruler: add point ${rulerAnchors.size + 1}"
    val measurement = rulerMeasurement ?: return "Ruler ready"
    return "Ruler ${"%.2f".format(measurement.distance)}m | slope ${formatSlope(measurement.slope)}"
}

private fun ArGuidanceSeverity.label(): String = when (this) {
    ArGuidanceSeverity.Info -> "Info"
    ArGuidanceSeverity.Warning -> "Warning"
    ArGuidanceSeverity.ActionRequired -> "Action"
}

@Composable
private fun MeasurementOverlayCards(
    selected: MathSceneObject,
    state: ArViewerUiState
) {
    val measurements = selected.measurementOverlays(state)
    if (measurements.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(measurements) { measurement ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(measurement.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(measurement.formattedValue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

private data class MeasurementOverlay(
    val id: String,
    val label: String,
    val formattedValue: String
)

private fun MathSceneObject.measurementOverlays(state: ArViewerUiState): List<MeasurementOverlay> {
    val outdoor = state.outdoorGeospatial?.selectedMetrics
    if (state.mathArExperience == MathArExperience.OutdoorGeometry && outdoor != null) {
        return listOf(
            MeasurementOverlay("height", "Height", formatMeters(outdoor.estimatedHeightMeters)),
            MeasurementOverlay("distance", "Distance", formatMeters(outdoor.rooflineDistanceMeters)),
            MeasurementOverlay("angle", "Angle", "${MeasurementFormatter.number(outdoor.angleOfElevationDegrees.toDouble())} deg"),
            MeasurementOverlay("slope", "Slope", MeasurementFormatter.number(outdoor.slopeToRoofline.toDouble())),
            MeasurementOverlay("area", "Area", formatSquareMeters(outdoor.footprintAreaSquareMeters)),
            MeasurementOverlay("volume", "Volume", formatCubicMeters(outdoor.volumeApproxCubicMeters))
        )
    }

    val definitionMeasurements = DefaultMathObjectRegistry.getDefinition(definitionId)
        ?.measurementProvider
        ?.invoke(parameters)
        .orEmpty()
        .map { property ->
            MeasurementOverlay(
                id = property.id,
                label = property.label.measurementLabel(),
                formattedValue = "${MeasurementFormatter.number(property.value)} ${property.unitLabel.orEmpty()}".trim()
            )
        }

    val direct = when (objectType) {
        MathObjectType.Cube -> {
            val s = parameterNumber(parameters, "sideLength", 0.24)
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.length(s)),
                MeasurementOverlay("distance", "Distance", MeasurementFormatter.length(s * sqrt(3.0))),
                MeasurementOverlay("angle", "Angle", "90 deg"),
                MeasurementOverlay("slope", "Slope", "0"),
                MeasurementOverlay("area", "Area", "${MeasurementFormatter.number(6 * s * s)} m2"),
                MeasurementOverlay("volume", "Volume", "${MeasurementFormatter.number(s * s * s)} m3")
            )
        }
        MathObjectType.RectangularPrism -> {
            val l = parameterNumber(parameters, "length", 0.32)
            val w = parameterNumber(parameters, "width", 0.18)
            val h = parameterNumber(parameters, "height", 0.22)
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.length(h)),
                MeasurementOverlay("distance", "Distance", MeasurementFormatter.length(sqrt(l * l + w * w + h * h))),
                MeasurementOverlay("angle", "Angle", "90 deg"),
                MeasurementOverlay("slope", "Slope", MeasurementFormatter.number(h / l.coerceAtLeast(0.001))),
                MeasurementOverlay("area", "Area", "${MeasurementFormatter.number(2 * (l * w + l * h + w * h))} m2"),
                MeasurementOverlay("volume", "Volume", "${MeasurementFormatter.number(l * w * h)} m3")
            )
        }
        MathObjectType.Cylinder -> {
            val r = parameterNumber(parameters, "radius", 0.12)
            val h = parameterNumber(parameters, "height", 0.32)
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.length(h)),
                MeasurementOverlay("distance", "Diameter", MeasurementFormatter.length(2 * r)),
                MeasurementOverlay("angle", "Angle", "90 deg"),
                MeasurementOverlay("slope", "Slope", "0"),
                MeasurementOverlay("area", "Area", "${MeasurementFormatter.number(PI * r * r)} m2"),
                MeasurementOverlay("volume", "Volume", "${MeasurementFormatter.number(PI * r * r * h)} m3")
            )
        }
        MathObjectType.Cone -> {
            val r = parameterNumber(parameters, "radius", 0.12)
            val h = parameterNumber(parameters, "height", 0.32)
            val slant = sqrt(r * r + h * h)
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.length(h)),
                MeasurementOverlay("distance", "Slant", MeasurementFormatter.length(slant)),
                MeasurementOverlay("angle", "Angle", "${MeasurementFormatter.number(Math.toDegrees(atan2(h, r)))} deg"),
                MeasurementOverlay("slope", "Slope", MeasurementFormatter.number(h / r.coerceAtLeast(0.001))),
                MeasurementOverlay("area", "Area", "${MeasurementFormatter.number(PI * r * (r + slant))} m2"),
                MeasurementOverlay("volume", "Volume", "${MeasurementFormatter.number(PI * r * r * h / 3.0)} m3")
            )
        }
        MathObjectType.Sphere -> {
            val r = parameterNumber(parameters, "radius", 0.16)
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.length(2 * r)),
                MeasurementOverlay("distance", "Diameter", MeasurementFormatter.length(2 * r)),
                MeasurementOverlay("angle", "Angle", "360 deg"),
                MeasurementOverlay("slope", "Slope", "varies"),
                MeasurementOverlay("area", "Area", "${MeasurementFormatter.number(4 * PI * r * r)} m2"),
                MeasurementOverlay("volume", "Volume", "${MeasurementFormatter.number(4 * PI * r * r * r / 3.0)} m3")
            )
        }
        MathObjectType.SineCurve -> {
            val a = parameterNumber(parameters, "amplitude", 1.0)
            val b = parameterNumber(parameters, "frequency", 1.0)
            val slope = a * b
            listOf(
                MeasurementOverlay("height", "Height", MeasurementFormatter.number(kotlin.math.abs(2 * a))),
                MeasurementOverlay("distance", "Period", MeasurementFormatter.number((2 * PI) / b.coerceAtLeast(0.001))),
                MeasurementOverlay("angle", "Angle", "${MeasurementFormatter.number(Math.toDegrees(atan2(slope, 1.0)))} deg"),
                MeasurementOverlay("slope", "Slope", MeasurementFormatter.number(slope)),
                MeasurementOverlay("area", "Area", "scan"),
                MeasurementOverlay("volume", "Volume", "extrude")
            )
        }
        MathObjectType.VectorArrow -> {
            val x = parameterNumber(parameters, "x", 1.0)
            val y = parameterNumber(parameters, "y", 1.0)
            val z = parameterNumber(parameters, "z", 0.0)
            val magnitude = sqrt(x * x + y * y + z * z)
            listOf(
                MeasurementOverlay("height", "Rise", MeasurementFormatter.number(y)),
                MeasurementOverlay("distance", "Distance", MeasurementFormatter.number(magnitude)),
                MeasurementOverlay("angle", "Angle", "${MeasurementFormatter.number(Math.toDegrees(atan2(y, x)))} deg"),
                MeasurementOverlay("slope", "Slope", MeasurementFormatter.number(y / x.coerceAtLeast(0.001))),
                MeasurementOverlay("area", "Area", "n/a"),
                MeasurementOverlay("volume", "Volume", "n/a")
            )
        }
        MathObjectType.CoordinatePlane,
        MathObjectType.NumberLine,
        MathObjectType.Triangle,
        MathObjectType.Circle -> emptyList()
    }
    return (direct + definitionMeasurements).distinctBy { it.id }.take(6)
}

private fun String.measurementLabel(): String = when {
    contains("volume", ignoreCase = true) -> "Volume"
    contains("area", ignoreCase = true) -> "Area"
    contains("height", ignoreCase = true) -> "Height"
    contains("distance", ignoreCase = true) -> "Distance"
    contains("slope", ignoreCase = true) -> "Slope"
    contains("angle", ignoreCase = true) -> "Angle"
    else -> this
}

@Composable
private fun FormulaOverlayCards(
    selected: MathSceneObject,
    state: ArViewerUiState
) {
    val formulas = selected.formulaCards(state)
    if (formulas.isEmpty()) return
    if (state.formulaCardsCollapsed) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(formulas) { formula ->
                AssistChip(onClick = {}, label = { Text(formula.compact) })
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            formulas.take(3).forEach { formula ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(formula.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(formula.expression, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        formula.note?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}

private data class FormulaOverlay(
    val title: String,
    val expression: String,
    val compact: String,
    val note: String? = null
)

private fun MathSceneObject.formulaCards(state: ArViewerUiState): List<FormulaOverlay> {
    val base = when (objectType) {
        MathObjectType.Cube -> listOf(
            FormulaOverlay("Volume", "V = s x s x s", "V=s3"),
            FormulaOverlay("Surface area", "A = 6s2", "A=6s2")
        )
        MathObjectType.RectangularPrism -> listOf(
            FormulaOverlay("Volume", "V = l x w x h", "V=lwh"),
            FormulaOverlay("Surface area", "A = 2(lw + lh + wh)", "A=2(lw+lh+wh)")
        )
        MathObjectType.Cylinder -> listOf(
            FormulaOverlay("Volume", "V = pi r2 h", "V=pi r2h"),
            FormulaOverlay("Surface area", "A = 2pi r(r + h)", "A=2pi r(r+h)")
        )
        MathObjectType.Cone -> listOf(
            FormulaOverlay("Volume", "V = (1/3)pi r2 h", "V=1/3pi r2h"),
            FormulaOverlay("Slope side", "l = sqrt(r2 + h2)", "l=sqrt(r2+h2)")
        )
        MathObjectType.Sphere -> listOf(
            FormulaOverlay("Volume", "V = (4/3)pi r3", "V=4/3pi r3"),
            FormulaOverlay("Surface area", "A = 4pi r2", "A=4pi r2")
        )
        MathObjectType.SineCurve -> listOf(
            FormulaOverlay("Function", "y = a sin(bx + c) + d", "y=a sin(bx+c)+d"),
            FormulaOverlay("Slope", "dy/dx = ab cos(bx + c)", "dy/dx")
        )
        MathObjectType.CoordinatePlane -> listOf(
            FormulaOverlay("Coordinate rule", "(x, y, z)", "(x,y,z)"),
            FormulaOverlay("Slope", "slope = rise / run", "rise/run")
        )
        MathObjectType.VectorArrow -> listOf(
            FormulaOverlay("Magnitude", "|v| = sqrt(x2 + y2 + z2)", "|v|"),
            FormulaOverlay("Displacement", "d = end - start", "d=end-start")
        )
        MathObjectType.NumberLine -> listOf(
            FormulaOverlay("Distance", "d = |x2 - x1|", "d=|x2-x1|"),
            FormulaOverlay("Scale", "1 unit = 1 meter", "1u=1m")
        )
        MathObjectType.Triangle -> listOf(
            FormulaOverlay("Area", "A = (1/2)bh", "A=1/2bh"),
            FormulaOverlay("Angle sum", "a + b + c = 180 deg", "sum=180")
        )
        MathObjectType.Circle -> listOf(
            FormulaOverlay("Area", "A = pi r2", "A=pi r2"),
            FormulaOverlay("Circumference", "C = 2pi r", "C=2pi r")
        )
    }
    val experience = when (state.mathArExperience) {
        MathArExperience.Drawing2dTo3d -> listOf(
            FormulaOverlay("Surface", "z = f(x, y)", "z=f(x,y)"),
            FormulaOverlay("Extrude", "V approx area x depth", "V=A x d")
        )
        MathArExperience.OutdoorGeometry -> listOf(
            FormulaOverlay("Building volume", "V approx base area x height", "V=A_b h"),
            FormulaOverlay("Elevation", "slope = rise / run", "rise/run")
        )
        MathArExperience.SolarSystem -> listOf(
            FormulaOverlay("Orbit", "v = 2pi r / T", "v=2pi r/T"),
            FormulaOverlay("Gravity", "F = Gm1m2 / r2", "F=Gm1m2/r2")
        )
        MathArExperience.MarkerBasedGraph -> listOf(
            FormulaOverlay("Paper mapping", "paper (u, v) -> graph (x, y)", "u,v -> x,y"),
            FormulaOverlay("3D lift", "z = f(x, y)", "z=f(x,y)")
        )
        else -> emptyList()
    }
    return (experience + base).distinctBy { it.compact }.take(4)
}

@Composable
private fun FatalArRuntimePanel(title: String, body: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xEE08111F))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(body)
                Text(
                    "Logcat tag: AiStemAR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ObjectPreviewDot(type: MathObjectType) {
    val color = when (type) {
        MathObjectType.Cube -> Color(0xFF62D6C7)
        MathObjectType.CoordinatePlane -> Color(0xFF6EDBFF)
        MathObjectType.SineCurve -> Color(0xFFFFC857)
        MathObjectType.Sphere -> Color(0xFF9B8CFF)
        MathObjectType.Cylinder -> Color(0xFF4FD1C5)
        MathObjectType.Cone -> Color(0xFFFFA726)
        MathObjectType.RectangularPrism -> Color(0xFF7E57C2)
        MathObjectType.Triangle -> Color(0xFFFF6B8A)
        MathObjectType.Circle -> Color(0xFF66BB6A)
        MathObjectType.NumberLine -> Color(0xFF42A5F5)
        MathObjectType.VectorArrow -> Color(0xFFEC407A)
    }
    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectInspectorSheet(
    state: ArViewerUiState,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onParameter: (String, MathParameterValue) -> Unit,
    onReset: () -> Unit
) {
    val selected = state.mathScene.primarySelectedObject ?: return
    val definition = DefaultMathObjectRegistry.getDefinition(selected.definitionId) ?: return
    var name by remember(selected.id) { mutableStateOf(selected.displayName) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Object Details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Object name") },
                singleLine = true,
                trailingIcon = { TextButton(onClick = { onRename(name) }) { Text("Rename") } },
                modifier = Modifier.fillMaxWidth()
            )
            Text(selected.objectType.displayName, style = MaterialTheme.typography.titleMedium)
            definition.supportedParameters.forEach { parameter ->
                val current = selected.parameters[parameter.id] ?: parameter.defaultValue
                when (current) {
                    is MathParameterValue.NumberValue -> {
                        var value by remember(selected.id, parameter.id, current.value) { mutableStateOf(current.value.toString()) }
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                it.toDoubleOrNull()?.let { number -> onParameter(parameter.id, MathParameterValue.NumberValue(number)) }
                            },
                            label = { Text(parameter.label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is MathParameterValue.IntegerValue -> Text("${parameter.label}: ${current.value}")
                    is MathParameterValue.BooleanValue -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(parameter.label)
                        Switch(checked = current.value, onCheckedChange = { onParameter(parameter.id, MathParameterValue.BooleanValue(it)) })
                    }
                    is MathParameterValue.ChoiceValue -> Text("${parameter.label}: ${current.value}")
                    is MathParameterValue.TextValue -> Text("${parameter.label}: ${current.value}")
                }
            }
            Text("Measurements", style = MaterialTheme.typography.titleMedium)
            definition.measurementProvider(selected.parameters).forEach { property ->
                Text("${property.label}: ${MeasurementFormatter.number(property.value)} ${property.unitLabel.orEmpty()}")
            }
            Text("Position: ${MeasurementFormatter.number(selected.transform.position.x)}, ${MeasurementFormatter.number(selected.transform.position.y)}, ${MeasurementFormatter.number(selected.transform.position.z)}")
            Text("Scale: ${MeasurementFormatter.number(selected.transform.scale.x)}")
            Text("Lock state: ${if (selected.interactionState.locked) "Locked" else "Unlocked"}")
            Text("Visibility: ${if (selected.visibility.visible) "Shown" else "Hidden"}")
            selected.groupId?.let { Text("Group: $it") }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset Transform") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneLayersSheet(
    state: ArViewerUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onHide: (String) -> Unit,
    onLock: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Objects in Scene", style = MaterialTheme.typography.titleLarge)
            if (state.mathScene.objects.isEmpty()) {
                Text("No objects placed yet.")
            }
            state.mathScene.groups.forEach { group ->
                ElevatedAssistChip(onClick = {}, label = { Text("${group.displayName}: ${group.memberObjectIds.size} objects") })
            }
            state.mathScene.objects.forEach { item ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(item.objectType.displayName, style = MaterialTheme.typography.bodySmall)
                            }
                            ObjectPreviewDot(item.objectType)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { onSelect(item.id) }, modifier = Modifier.weight(1f)) { Text("Select") }
                            OutlinedButton(onClick = { onHide(item.id) }, modifier = Modifier.weight(1f)) { Text(if (item.visibility.visible) "Hide" else "Show") }
                            OutlinedButton(onClick = { onLock(item.id) }, modifier = Modifier.weight(1f)) { Text(if (item.interactionState.locked) "Unlock" else "Lock") }
                        }
                        OutlinedButton(onClick = { onDelete(item.id) }, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedScenesSheet(
    state: ArViewerUiState,
    saveName: String,
    onSaveName: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("My Mathematics Scenes", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = saveName, onValueChange = onSaveName, label = { Text("Scene name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save Scene") }
            if (state.savedScenes.isEmpty()) {
                Text("No saved scenes yet")
                Text("Create an AR mathematics scene and save it here.")
            }
            state.savedScenes.forEach { scene ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(scene.name, style = MaterialTheme.typography.titleMedium)
                        Text("${scene.objectCount} objects")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { onOpen(scene.id) }, modifier = Modifier.weight(1f)) { Text("Open") }
                            OutlinedButton(onClick = { onDelete(scene.id) }, modifier = Modifier.weight(1f)) { Text("Delete") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun SceneInteractionMode.simpleName(): String = when (this) {
    SceneInteractionMode.Select -> "Select"
    SceneInteractionMode.Place -> "Place"
    SceneInteractionMode.Move -> "Move"
    SceneInteractionMode.Rotate -> "Rotate"
    SceneInteractionMode.Scale -> "Scale"
    SceneInteractionMode.Measure -> "Measure"
    SceneInteractionMode.MultiSelect -> "Multi-select"
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
        state.placementHitKind == PlacementHitKind.StreetscapeGeometry -> "Building mesh found - tap to anchor math"
        state.placementHitKind == PlacementHitKind.Plane -> "Surface found - tap to place"
        state.placementHitKind == PlacementHitKind.DepthPoint -> "Depth surface found - tap to place"
        state.placementHitKind == PlacementHitKind.FeaturePoint -> "Air point found - tap to place"
        state.placementHitKind == PlacementHitKind.Instant -> "Air placement ready - tap to place"
        state.arEngineMode == ArEngineMode.PaperGraph && state.paperGraph?.hasLockedTarget == true -> "Paper graph locked - build in 3D"
        state.arEngineMode == ArEngineMode.PaperGraph -> "Scan a worksheet or graph target"
        state.arEngineMode == ArEngineMode.OutdoorGeospatialMath -> "Scan outdoor buildings or terrain"
        else -> "Move your phone slowly to place on a surface or in air"
    }

private fun openAppSettings(packageName: String, context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )
}
