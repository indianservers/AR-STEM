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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.indianservers.ai_stem.domain.mathematics.MathematicsCatalogue
import com.indianservers.ai_stem.domain.mathematics.MathObjectCategory
import com.indianservers.ai_stem.domain.mathematics.MathObjectDefinition
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MeasurementFormatter
import com.indianservers.ai_stem.domain.mathematics.SineCurveSampler
import com.indianservers.ai_stem.domain.mathematics.parameterNumber
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.MathSceneObject
import com.indianservers.ai_stem.domain.scene.SceneInteractionMode
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
import kotlin.math.cos
import kotlin.math.sin

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
                    depthEnabled = if (outdoorConfig.enabled) outdoorConfig.geospatialDepthEnabled else depthEnabled
                    semanticsSupported = runCatching { session.isSemanticModeSupported(Config.SemanticMode.ENABLED) }.getOrDefault(false)
                    semanticsEnabled = semanticsSupported
                    config.semanticMode = if (semanticsEnabled) Config.SemanticMode.ENABLED else Config.SemanticMode.DISABLED
                    Log.d(
                        AR_LOG_TAG,
                        "Session configured mode=${state.arEngineMode} planes=${config.planeFindingMode} depth=${config.depthMode}/supported=$depthSupported semantic=${config.semanticMode}/supported=$semanticsSupported instant=${config.instantPlacementMode} geospatial=${config.geospatialMode} streetscape=${config.streetscapeGeometryMode}"
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
                        if (anchor != null && !moving) return@rememberOnGestureListener
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
                        if (hit != null) {
                            Log.d(AR_LOG_TAG, "Creating anchor from hit trackable=${hit.trackable.javaClass.simpleName}")
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
                                rotation = Rotation(y = objectState.transform.rotation.y.toFloat()),
                                scale = Scale(objectState.transform.scale.x.toFloat()),
                                isEditable = !objectState.interactionState.locked
                            ) {
                                MathObjectNode(objectState.objectType, materialLoader)
                                if (objectState.interactionState.selected) SelectionHighlight(materialLoader)
                            }
                        }
                    }
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
    }
}

@Composable
private fun NodeScope.MathObjectNode(type: MathObjectType, materialLoader: MaterialLoader) {
    val cyan = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF4EE7FF)) }
    val teal = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAA4FD1C5), roughness = 0.35f) }
    val amber = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val rose = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAAFF6B8A), roughness = 0.45f) }
    val violet = remember(materialLoader) { materialLoader.createColorInstance(Color(0xAA9B8CFF), roughness = 0.45f) }
    when (type) {
        MathObjectType.Cube -> {
            CubeNode(size = Size(0.24f, 0.24f, 0.24f), center = Position(0f, 0.12f, 0f), materialInstance = teal)
            CubeEdges(cyan)
        }
        MathObjectType.CoordinatePlane -> CoordinatePlaneNode(cyan, amber)
        MathObjectType.SineCurve -> SineCurveNode(cyan, amber)
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
        val menuDefinitions = activeMenu.definitions()
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
            if (controlsExpanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ArEngineMode.entries) { mode ->
                            FilterChip(
                                selected = state.arEngineMode == mode,
                                onClick = { onSelectArEngineMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    if (state.arEngineMode == ArEngineMode.OutdoorGeospatialMath) {
                        OutdoorGeospatialMathPanel(state)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ArPlacementMenu.entries) { menu ->
                            FilterChip(
                                selected = activeMenu == menu,
                                onClick = { activeMenu = menu },
                                label = { Text(menu.label) }
                            )
                        }
                    }
                    if (activeMenu != ArPlacementMenu.Scene) {
                        Text(activeMenu.description, style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(menuDefinitions) { item ->
                                FilterChip(
                                    selected = state.selectedDefinitionId == item.definitionId,
                                    onClick = { onSelectObject(item.type) },
                                    label = { Text(item.displayName) },
                                    leadingIcon = { ObjectPreviewDot(item.type) },
                                    modifier = Modifier.semantics { contentDescription = "Select ${item.displayName}" }
                                )
                            }
                        }
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
    Scene("Scene", "Select, transform, save, and diagnose the AR scene.");

    fun definitions(): List<MathObjectDefinition> = when (this) {
        Functions -> DefaultMathObjectRegistry.getDefinitionsByCategory(MathObjectCategory.Graphs)
            .filter { it.type == MathObjectType.SineCurve }
        Graphs -> DefaultMathObjectRegistry.getAllDefinitions()
            .filter { it.category in setOf(MathObjectCategory.Graphs, MathObjectCategory.Coordinates, MathObjectCategory.NumberTools, MathObjectCategory.Vectors) }
        Objects -> DefaultMathObjectRegistry.getAllDefinitions()
            .filter { it.category in setOf(MathObjectCategory.Shapes, MathObjectCategory.Solids) }
        Scene -> emptyList()
    }
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
    val needsGuidance = state.trackingStatus != TrackingStatus.Tracking ||
        (state.mathScene.objects.isNotEmpty() && state.anchorTrackingStatus != TrackingStatus.Tracking)
    if (!needsGuidance) return
    val containerColor = when (state.guidanceSeverity) {
        ArGuidanceSeverity.Info -> MaterialTheme.colorScheme.secondaryContainer
        ArGuidanceSeverity.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        ArGuidanceSeverity.ActionRequired -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (state.guidanceSeverity) {
        ArGuidanceSeverity.Info -> MaterialTheme.colorScheme.onSecondaryContainer
        ArGuidanceSeverity.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        ArGuidanceSeverity.ActionRequired -> MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = containerColor.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                "${state.guidanceHeadline}: ${state.guidanceInstruction}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun ArGuidanceSeverity.label(): String = when (this) {
    ArGuidanceSeverity.Info -> "Info"
    ArGuidanceSeverity.Warning -> "Warning"
    ArGuidanceSeverity.ActionRequired -> "Action"
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
