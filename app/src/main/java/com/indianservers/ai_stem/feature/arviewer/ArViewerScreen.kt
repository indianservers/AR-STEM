package com.indianservers.ai_stem.feature.arviewer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.opengl.Matrix
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
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
import com.indianservers.ai_stem.core.ar.CommonArCoreSurfaceDetector
import com.indianservers.ai_stem.core.ar.CommonArSurfaceEngine
import com.indianservers.ai_stem.core.ar.CommonArSurfaceHitKind
import com.indianservers.ai_stem.core.ar.CommonArSurfaceMode
import com.indianservers.ai_stem.core.ar.CommonArSurfaceQuality
import com.indianservers.ai_stem.core.ar.CommonArSurfaceState
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
import com.indianservers.ai_stem.domain.graph.ArCompiledExpression
import com.indianservers.ai_stem.domain.graph.ArAdvancedMathTools
import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import com.indianservers.ai_stem.domain.graph.label
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val AR_LOG_TAG = "AiStemAR"
private const val RETICLE_HIT_TEST_INTERVAL_NANOS = 250_000_000L
private const val MARKER_STATUS_UPDATE_FRAME_INTERVAL = 15
private const val G01_MARKER_WIDTH_METERS = 0.16f
private const val MARKER_SAFE_HALF_EXTENT_METERS = 0.06f
private const val MARKER_SAFE_SOLID_HEIGHT_METERS = 0.085f
private const val MIN_STABLE_PLANE_FRAMES = 8
private const val MIN_PLANE_EXTENT_METERS = 0.18f

data class RadiantPointStyle(
    val centreRadius: Float = 0.011f,
    val haloRadius: Float = 0.026f,
    val glowIntensity: Float = 1f,
    val isSelected: Boolean = false,
    val isDraggable: Boolean = false,
    val isLocked: Boolean = false,
    val isInvalid: Boolean = false
)

data class ThickLineStyle(
    val widthDp: Float = 5f,
    val glowWidthDp: Float = 9f,
    val opacity: Float = 1f,
    val isDashed: Boolean = false,
    val isSelected: Boolean = false
)

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
            if (!state.isQuietMarkerMode()) {
                snackbarHostState.showSnackbar(it.text)
            }
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.arEngineMode, state.markerMathActivity) {
        if (state.isQuietMarkerMode()) {
            snackbarHostState.currentSnackbarData?.dismiss()
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
                            onMarker2dWorkspaceTap = viewModel::onMarker2dWorkspaceTap,
                            onMarker3dWorkspaceTap = viewModel::onMarker3dWorkspaceTap,
                            onMarkerCoordinateWorkspaceTap = viewModel::onMarkerCoordinateWorkspaceTap,
                            onPlacementPoint = viewModel::recordPlacementPoint,
                            onOutdoorGeospatialFrame = viewModel::onOutdoorGeospatialFrame,
                            onPlacementMissed = viewModel::onPlacementMissed,
                            onRuntimeError = viewModel::onRuntimeError
                        )
                    }
                    SupportedArChrome(
                        state = state,
                        viewModel = viewModel,
                        onBack = onBack,
                        onHelp = { showHelp = true },
                        onRequireLocation = {
                            askedLocationPermission = true
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onDetachAnchor = {
                            anchor?.detach()
                            anchor = null
                        },
                        onMoving = { moving = it },
                        saveName = saveName,
                        haptic = haptic,
                        snackbarHostState = snackbarHostState
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
private fun SupportedArChrome(
    state: ArViewerUiState,
    viewModel: ArViewerViewModel,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onRequireLocation: () -> Unit,
    onDetachAnchor: () -> Unit,
    onMoving: (Boolean) -> Unit,
    saveName: String,
    haptic: HapticFeedback,
    snackbarHostState: SnackbarHostState
) {
    SupportedQuietMarkerChrome(
        state = state,
        viewModel = viewModel,
        onBack = onBack,
        onHelp = onHelp,
        haptic = haptic
    )
    return
    ArChrome(
        state = state,
        onBack = onBack,
        onHelp = onHelp,
        onSelectObject = viewModel::selectObject,
        onSelectArEngineMode = { mode ->
            if (mode == ArEngineMode.OutdoorGeospatialMath && state.locationPermission != LocationPermissionState.Granted) {
                onRequireLocation()
            } else {
                viewModel.selectArEngineMode(mode)
            }
        },
        onSelectMathExperience = { experience ->
            if (experience == MathArExperience.OutdoorGeometry && state.locationPermission != LocationPermissionState.Granted) {
                onRequireLocation()
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
        onAdvancedMathTool = viewModel::applyAdvancedMathTool,
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
            onDetachAnchor()
            viewModel.deleteObject()
        },
        onMove = {
            onMoving(true)
            snackbarHostState.currentSnackbarData?.dismiss()
        },
        onRePlace = {
            onDetachAnchor()
            onMoving(true)
            viewModel.requestRePlacement()
            snackbarHostState.currentSnackbarData?.dismiss()
        },
        onReset = {
            viewModel.resetTransform()
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        onDelete = {
            onDetachAnchor()
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
            onDetachAnchor()
            viewModel.clearScene()
        },
        onMarkerZoom = viewModel::zoomMarkerLesson,
        onMarkerRotate = viewModel::rotateMarkerLesson,
        onMarkerExpand = viewModel::toggleMarkerLessonExpanded,
        onMarkerNext = viewModel::focusNextMarkerLessonElement,
        onMarkerReset = viewModel::resetMarkerLessonInteraction,
        onMarkerActivity = viewModel::selectMarkerMathActivity,
        onMarker2dShape = viewModel::addMarker2dShape,
        onMarker2dFinishDraft = viewModel::finishMarker2dDraft,
        onMarker2dCancelDraft = viewModel::cancelMarker2dDraft,
        onMarker2dSelectNext = viewModel::selectNextMarker2dObject,
        onMarker2dMove = viewModel::moveSelectedMarker2dObject,
        onMarker2dRotate = viewModel::rotateSelectedMarker2dObject,
        onMarker2dScale = viewModel::scaleSelectedMarker2dObject,
        onMarker2dDuplicate = viewModel::duplicateSelectedMarker2dObject,
        onMarker2dHide = viewModel::hideSelectedMarker2dObject,
        onMarker2dDelete = viewModel::deleteSelectedMarker2dObject,
        onMarker2dToggleDisplay = viewModel::toggleMarker2dDisplay,
        onMarker2dConstraint = viewModel::selectMarker2dConstraint,
        onMarker3dObject = viewModel::addMarker3dObject,
        onMarker3dPlacePreview = viewModel::placeMarker3dPreview,
        onMarker3dCancelPreview = viewModel::cancelMarker3dPreview,
        onMarker3dMovePreview = viewModel::moveMarker3dPreview,
        onMarker3dSelectNext = viewModel::selectNextMarker3dSolid,
        onMarker3dMove = viewModel::moveSelectedMarker3dSolid,
        onMarker3dRotate = viewModel::rotateSelectedMarker3dSolid,
        onMarker3dScale = viewModel::scaleSelectedMarker3dSolid,
        onMarker3dParameter = viewModel::updateSelectedMarker3dParameter,
        onMarker3dDuplicate = viewModel::duplicateSelectedMarker3dSolid,
        onMarker3dHide = viewModel::toggleSelectedMarker3dVisibility,
        onMarker3dLock = viewModel::toggleSelectedMarker3dLock,
        onMarker3dDelete = viewModel::deleteSelectedMarker3dSolid,
        onMarker3dReset = viewModel::resetSelectedMarker3dSolid,
        onMarker3dOverlay = viewModel::toggleMarker3dOverlay,
        onMarker3dExplode = viewModel::toggleSelectedMarker3dExploded,
        onMarker3dSection = viewModel::setSelectedMarker3dSection,
        onMarker3dClipping = viewModel::setSelectedMarker3dClipping,
        onMarker3dNet = viewModel::toggleSelectedMarker3dNet,
        onMarkerCoordinateTool = viewModel::selectMarkerCoordinateTool,
        onMarkerCoordinateAddPoint = viewModel::addMarkerCoordinatePoint,
        onMarkerCoordinateSelectNext = viewModel::selectNextMarkerCoordinatePoint,
        onMarkerCoordinateMovePoint = viewModel::moveSelectedMarkerCoordinatePoint,
        onMarkerCoordinateSetPoint = viewModel::setSelectedMarkerCoordinatePoint,
        onMarkerCoordinateApplyTool = viewModel::applyMarkerCoordinateTool,
        onMarkerCoordinateSnap = viewModel::toggleMarkerCoordinateSnap,
        onMarkerCoordinateFormat = viewModel::toggleMarkerCoordinateFormat,
        onMarkerCoordinateSlopeTriangle = viewModel::toggleMarkerCoordinateSlopeTriangle,
        onMarkerCoordinateTransform = viewModel::transformMarkerCoordinateSelection,
        onMarkerTransformationTool = viewModel::selectMarkerTransformationTool,
        onMarkerTransformShape = viewModel::selectMarkerTransformShape,
        onMarkerTransformationSequenceStep = viewModel::addMarkerTransformationSequenceStep,
        onClearMarkerTransformationSequence = viewModel::clearMarkerTransformationSequence,
        onMarkerTransformProgress = viewModel::setMarkerTransformProgress,
        onMarkerTranslationX = viewModel::setMarkerTranslationX,
        onMarkerTranslationY = viewModel::setMarkerTranslationY,
        onMarkerRotationDegrees = viewModel::setMarkerRotationDegrees,
        onMarkerRotationCenterX = viewModel::setMarkerRotationCenterX,
        onMarkerRotationCenterY = viewModel::setMarkerRotationCenterY,
        onMarkerRotationDirection = viewModel::toggleMarkerRotationDirection,
        onMarkerReflectionLine = viewModel::setMarkerReflectionLine,
        onMarkerDilationScale = viewModel::setMarkerDilationScale,
        onMarkerDilationCenterX = viewModel::setMarkerDilationCenterX,
        onMarkerDilationCenterY = viewModel::setMarkerDilationCenterY,
        onMarkerHorizontalStretch = viewModel::setMarkerHorizontalStretch,
        onMarkerVerticalStretch = viewModel::setMarkerVerticalStretch,
        onMarkerShear = viewModel::setMarkerShear,
        onUndoMarkerTransformation = viewModel::undoMarkerTransformation,
        onMarkerTrigAngle = viewModel::setMarkerTrigAngle,
        onAddMarkerGraphFunction = viewModel::addMarkerGraphFunction,
        onSelectMarkerGraphFunction = viewModel::selectMarkerGraphFunction,
        onToggleSelectedMarkerGraphVisibility = viewModel::toggleSelectedMarkerGraphVisibility,
        onDeleteSelectedMarkerGraphFunction = viewModel::deleteSelectedMarkerGraphFunction,
        onDuplicateSelectedMarkerGraphFunction = viewModel::duplicateSelectedMarkerGraphFunction,
        onMarkerGraphParameter = viewModel::updateSelectedMarkerGraphParameter,
        onMarkerGraphTrace = viewModel::toggleMarkerGraphTrace,
        onMarkerGraphTraceProgress = viewModel::setMarkerGraphTraceProgress,
        onMarkerGraphZoom = viewModel::zoomMarkerGraph,
        onMarkerGraphPan = viewModel::panMarkerGraph,
        onMarkerGraphFit = viewModel::fitMarkerGraphView,
        onMarkerGraphReset = viewModel::resetMarkerGraphView,
        onMarkerGraphXMin = viewModel::updateMarkerGraphDomainXMin,
        onMarkerGraphXMax = viewModel::updateMarkerGraphDomainXMax,
        onMarkerGraphYMin = viewModel::updateMarkerGraphDomainYMin,
        onMarkerGraphYMax = viewModel::updateMarkerGraphDomainYMax,
        onToggleMarkerGraphGrid = viewModel::toggleMarkerGraphGrid,
        onToggleMarkerGraphLabels = viewModel::toggleMarkerGraphLabels,
        onToggleMarkerGraphIntercepts = viewModel::toggleMarkerGraphIntercepts,
        onToggleMarkerGraphExtrema = viewModel::toggleMarkerGraphExtrema,
        onToggleMarkerGraphDiscontinuities = viewModel::toggleMarkerGraphDiscontinuities,
        onToggleMarkerGraphDerivative = viewModel::toggleMarkerGraphDerivative,
        onToggleMarkerGraphIntegralArea = viewModel::toggleMarkerGraphIntegralArea,
        onClearMarkerWorkspace = viewModel::clearMarkerWorkspace
    )
}

@Composable
private fun SupportedQuietMarkerChrome(
    state: ArViewerUiState,
    viewModel: ArViewerViewModel,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    haptic: HapticFeedback
) {
    QuietMarkerChrome(
        state = state,
        onBack = onBack,
        onMarkerZoom = viewModel::zoomMarkerLesson,
        onMarkerRotate = viewModel::rotateMarkerLesson,
        onMarkerExpand = viewModel::toggleMarkerLessonExpanded,
        onMarkerNext = viewModel::focusNextMarkerLessonElement,
        onMarkerReset = viewModel::resetMarkerLessonInteraction,
        onLiveEquation = viewModel::updateLiveEquation,
        onGraphColorMap = viewModel::setGraphColorMap,
        onFunction3dTransform = viewModel::setFunction3dTransformMode,
        onAnimationProgress = viewModel::setGraphAnimationProgress,
        onSlicePosition = viewModel::setGraphSlicePosition,
        onMarkerActivity = viewModel::selectMarkerMathActivity,
        onMarker2dShape = viewModel::addMarker2dShape,
        onMarker2dFinishDraft = viewModel::finishMarker2dDraft,
        onMarker2dCancelDraft = viewModel::cancelMarker2dDraft,
        onMarker2dSelectNext = viewModel::selectNextMarker2dObject,
        onMarker2dMove = viewModel::moveSelectedMarker2dObject,
        onMarker2dRotate = viewModel::rotateSelectedMarker2dObject,
        onMarker2dScale = viewModel::scaleSelectedMarker2dObject,
        onMarker2dDuplicate = viewModel::duplicateSelectedMarker2dObject,
        onMarker2dHide = viewModel::hideSelectedMarker2dObject,
        onMarker2dDelete = viewModel::deleteSelectedMarker2dObject,
        onMarker2dToggleDisplay = viewModel::toggleMarker2dDisplay,
        onMarker2dConstraint = viewModel::selectMarker2dConstraint,
        onMarker3dObject = viewModel::addMarker3dObject,
        onMarker3dPlacePreview = viewModel::placeMarker3dPreview,
        onMarker3dCancelPreview = viewModel::cancelMarker3dPreview,
        onMarker3dMovePreview = viewModel::moveMarker3dPreview,
        onMarker3dSelectNext = viewModel::selectNextMarker3dSolid,
        onMarker3dMove = viewModel::moveSelectedMarker3dSolid,
        onMarker3dRotate = viewModel::rotateSelectedMarker3dSolid,
        onMarker3dScale = viewModel::scaleSelectedMarker3dSolid,
        onMarker3dParameter = viewModel::updateSelectedMarker3dParameter,
        onMarker3dDuplicate = viewModel::duplicateSelectedMarker3dSolid,
        onMarker3dHide = viewModel::toggleSelectedMarker3dVisibility,
        onMarker3dLock = viewModel::toggleSelectedMarker3dLock,
        onMarker3dDelete = viewModel::deleteSelectedMarker3dSolid,
        onMarker3dReset = viewModel::resetSelectedMarker3dSolid,
        onMarker3dOverlay = viewModel::toggleMarker3dOverlay,
        onMarker3dExplode = viewModel::toggleSelectedMarker3dExploded,
        onMarker3dSection = viewModel::setSelectedMarker3dSection,
        onMarker3dClipping = viewModel::setSelectedMarker3dClipping,
        onMarker3dNet = viewModel::toggleSelectedMarker3dNet,
        onMarkerCoordinateTool = viewModel::selectMarkerCoordinateTool,
        onMarkerCoordinateAddPoint = viewModel::addMarkerCoordinatePoint,
        onMarkerCoordinateSelectNext = viewModel::selectNextMarkerCoordinatePoint,
        onMarkerCoordinateMovePoint = viewModel::moveSelectedMarkerCoordinatePoint,
        onMarkerCoordinateSetPoint = viewModel::setSelectedMarkerCoordinatePoint,
        onMarkerCoordinateApplyTool = viewModel::applyMarkerCoordinateTool,
        onMarkerCoordinateSnap = viewModel::toggleMarkerCoordinateSnap,
        onMarkerCoordinateFormat = viewModel::toggleMarkerCoordinateFormat,
        onMarkerCoordinateSlopeTriangle = viewModel::toggleMarkerCoordinateSlopeTriangle,
        onMarkerCoordinateTransform = viewModel::transformMarkerCoordinateSelection,
        onMarkerTransformationTool = viewModel::selectMarkerTransformationTool,
        onMarkerTransformShape = viewModel::selectMarkerTransformShape,
        onMarkerTransformationSequenceStep = viewModel::addMarkerTransformationSequenceStep,
        onClearMarkerTransformationSequence = viewModel::clearMarkerTransformationSequence,
        onMarkerTransformProgress = viewModel::setMarkerTransformProgress,
        onMarkerTranslationX = viewModel::setMarkerTranslationX,
        onMarkerTranslationY = viewModel::setMarkerTranslationY,
        onMarkerRotationDegrees = viewModel::setMarkerRotationDegrees,
        onMarkerRotationCenterX = viewModel::setMarkerRotationCenterX,
        onMarkerRotationCenterY = viewModel::setMarkerRotationCenterY,
        onMarkerRotationDirection = viewModel::toggleMarkerRotationDirection,
        onMarkerReflectionLine = viewModel::setMarkerReflectionLine,
        onMarkerDilationScale = viewModel::setMarkerDilationScale,
        onMarkerDilationCenterX = viewModel::setMarkerDilationCenterX,
        onMarkerDilationCenterY = viewModel::setMarkerDilationCenterY,
        onMarkerHorizontalStretch = viewModel::setMarkerHorizontalStretch,
        onMarkerVerticalStretch = viewModel::setMarkerVerticalStretch,
        onMarkerShear = viewModel::setMarkerShear,
        onUndoMarkerTransformation = viewModel::undoMarkerTransformation,
        onMarkerTrigAngle = viewModel::setMarkerTrigAngle,
        onAddMarkerGraphFunction = viewModel::addMarkerGraphFunction,
        onSelectMarkerGraphFunction = viewModel::selectMarkerGraphFunction,
        onToggleSelectedMarkerGraphVisibility = viewModel::toggleSelectedMarkerGraphVisibility,
        onDeleteSelectedMarkerGraphFunction = viewModel::deleteSelectedMarkerGraphFunction,
        onDuplicateSelectedMarkerGraphFunction = viewModel::duplicateSelectedMarkerGraphFunction,
        onMarkerGraphParameter = viewModel::updateSelectedMarkerGraphParameter,
        onMarkerGraphTrace = viewModel::toggleMarkerGraphTrace,
        onMarkerGraphTraceProgress = viewModel::setMarkerGraphTraceProgress,
        onMarkerGraphZoom = viewModel::zoomMarkerGraph,
        onMarkerGraphPan = viewModel::panMarkerGraph,
        onMarkerGraphFit = viewModel::fitMarkerGraphView,
        onMarkerGraphReset = viewModel::resetMarkerGraphView,
        onMarkerGraphXMin = viewModel::updateMarkerGraphDomainXMin,
        onMarkerGraphXMax = viewModel::updateMarkerGraphDomainXMax,
        onMarkerGraphYMin = viewModel::updateMarkerGraphDomainYMin,
        onMarkerGraphYMax = viewModel::updateMarkerGraphDomainYMax,
        onToggleMarkerGraphGrid = viewModel::toggleMarkerGraphGrid,
        onToggleMarkerGraphLabels = viewModel::toggleMarkerGraphLabels,
        onToggleMarkerGraphIntercepts = viewModel::toggleMarkerGraphIntercepts,
        onToggleMarkerGraphExtrema = viewModel::toggleMarkerGraphExtrema,
        onToggleMarkerGraphDiscontinuities = viewModel::toggleMarkerGraphDiscontinuities,
        onToggleMarkerGraphDerivative = viewModel::toggleMarkerGraphDerivative,
        onToggleMarkerGraphIntegralArea = viewModel::toggleMarkerGraphIntegralArea,
        onClearMarkerWorkspace = viewModel::clearMarkerWorkspace
    )
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
    onMarker2dWorkspaceTap: (Float, Float) -> Unit,
    onMarker3dWorkspaceTap: (Float, Float) -> Unit,
    onMarkerCoordinateWorkspaceTap: (Float, Float) -> Unit,
    onPlacementPoint: (Float, Float, Float) -> Unit,
    onOutdoorGeospatialFrame: (OutdoorGeospatialFrameState) -> Unit,
    onPlacementMissed: () -> Unit,
    onRuntimeError: (String, Throwable) -> Unit
) {
    val context = LocalContext.current
    val latestFrameRef = remember { arrayOfNulls<Frame>(1) }
    var viewportWidth by remember { mutableStateOf(0) }
    var viewportHeight by remember { mutableStateOf(0) }
    var runtimeBlocked by remember { mutableStateOf(false) }
    val frameCounter = remember { intArrayOf(0) }
    val lastReticleHitTestTimestamp = remember { longArrayOf(0L) }
    val lastReticleHitKind = remember { arrayOf(PlacementHitKind.None) }
    var nativeSensorSample by remember { mutableStateOf<NativeArSensorSample?>(null) }
    var nativeSensorReceivedAtMs by remember { mutableStateOf(0L) }
    var semanticsSupported by remember { mutableStateOf(false) }
    var semanticsEnabled by remember { mutableStateOf(false) }
    var depthSupported by remember { mutableStateOf(false) }
    var depthEnabled by remember { mutableStateOf(false) }
    var lastOutdoorGeometryHit by remember { mutableStateOf<HitResult?>(null) }
    var outdoorMeshAnchor by remember { mutableStateOf<Anchor?>(null) }
    var outdoorWireframeEdges by remember { mutableStateOf<List<OutdoorWireframeEdge>>(emptyList()) }
    val latestLockedMarkerImage = remember { arrayOfNulls<AugmentedImage>(1) }
    val anchoredMarkerId = remember { arrayOfNulls<String>(1) }
    val markerLostFrames = remember { intArrayOf(0) }
    val markerStableFrames = remember { intArrayOf(0) }
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
    val commonSurfaceDetector = remember { CommonArCoreSurfaceDetector() }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    DisposableEffect(context, paperGraphMode) {
        if (paperGraphMode) {
            // ARCore already fuses camera and IMU data for image tracking. Running the
            // additional app sensor monitor in marker mode only adds heat and callbacks.
            onDispose { }
        } else {
            val monitor = NativeArSensorMonitor(context.applicationContext) { sample ->
                nativeSensorSample = sample
                nativeSensorReceivedAtMs = SystemClock.elapsedRealtime()
            }
            monitor.start()
            onDispose { monitor.stop() }
        }
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
            // SceneView otherwise selects the highest-resolution 30 FPS camera stream.
            // ARCore's default camera config is substantially cooler for an interactive
            // education session and is sufficient for the known-size G01 marker.
            sessionCameraConfig = null,
            lifecycle = arLifecycleOwner.lifecycle,
            planeRenderer = state.planesVisible && !state.isQuietMarkerMode(),
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
                    depthSupported = !paperGraphMode && runCatching {
                        session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                    }.getOrDefault(false)
                    if (paperGraphMode) {
                        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                        config.depthMode = Config.DepthMode.DISABLED
                        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                        config.semanticMode = Config.SemanticMode.DISABLED
                        depthEnabled = false
                        semanticsSupported = false
                        semanticsEnabled = false
                    } else {
                        CommonArSurfaceEngine.configureSession(
                            session = session,
                            config = config,
                            mode = state.arEngineMode.toCommonSurfaceMode(),
                            preferDepth = depthSupported,
                            instantPreview = state.arEngineMode == ArEngineMode.AirPlacement
                        )
                        depthEnabled = config.depthMode == Config.DepthMode.AUTOMATIC
                    }
                    val outdoorConfig = OutdoorGeospatialArEngine.configureSession(session, config, outdoorMode)
                    val paperConfig = AugmentedImageArEngine.configureSession(context, session, config, paperGraphMode)
                    depthEnabled = if (outdoorConfig.enabled) outdoorConfig.geospatialDepthEnabled else depthEnabled
                    if (!paperGraphMode) {
                        semanticsSupported = runCatching {
                            session.isSemanticModeSupported(Config.SemanticMode.ENABLED)
                        }.getOrDefault(false)
                        semanticsEnabled = semanticsSupported
                        config.semanticMode = if (semanticsEnabled) Config.SemanticMode.ENABLED else Config.SemanticMode.DISABLED
                    }
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
                    latestFrameRef[0] = frame
                    frameCounter[0] += 1
                    val cameraStatus = frame.camera.trackingState.toTrackingStatus()
                    val anchorStatus = anchor?.trackingState.toTrackingStatus()
                    if (outdoorMode) {
                        OutdoorGeospatialArEngine.observeFrame(session, frame, lastOutdoorGeometryHit).also(onOutdoorGeospatialFrame)
                    }
                    if (paperGraphMode) {
                        val updatedMarkerImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                        val shouldPublishMarkerFrame = updatedMarkerImages.isNotEmpty() ||
                            frameCounter[0] % MARKER_STATUS_UPDATE_FRAME_INTERVAL == 0
                        if (shouldPublishMarkerFrame) {
                            AugmentedImageArEngine.observeFrame(session, frame).also(onPaperGraphFrame)
                        }
                        val lockedImage = updatedMarkerImages
                            .filter { image ->
                                image.trackingState == TrackingState.TRACKING &&
                                    image.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
                            }
                            .sortedWith(
                                compareByDescending<AugmentedImage> {
                                    it.extentX.coerceAtLeast(0f) * it.extentZ.coerceAtLeast(0f)
                                }
                                    .thenBy { it.index }
                            )
                            .firstOrNull()
                            ?: latestLockedMarkerImage[0]?.takeIf { image ->
                                image.trackingState == TrackingState.TRACKING &&
                                    image.trackingMethod != AugmentedImage.TrackingMethod.NOT_TRACKING
                            }
                        latestLockedMarkerImage[0] = lockedImage
                        lockedImage?.let { image ->
                            val markerId = image.name.orEmpty().extractArMarkerId()
                            if (markerId != null) {
                                markerLostFrames[0] = 0
                                markerStableFrames[0] += 1
                                val anchorNeedsRestore = anchor == null || anchor.trackingState == TrackingState.STOPPED
                                if ((anchorNeedsRestore && markerStableFrames[0] >= 3) || markerId != anchoredMarkerId[0]) {
                                    if (markerId != anchoredMarkerId[0]) {
                                        Log.d(AR_LOG_TAG, "Switching marker anchor previous=${anchoredMarkerId[0]} next=$markerId image=${image.name}")
                                        anchor?.detach()
                                    } else {
                                        Log.d(AR_LOG_TAG, "Restoring marker anchor marker=$markerId image=${image.name}")
                                    }
                                    anchoredMarkerId[0] = markerId
                                    onAnchorChanged(image.createAnchor(image.centerPose))
                                }
                            }
                        } ?: run {
                            markerLostFrames[0] += 1
                            markerStableFrames[0] = 0
                        }
                        if (shouldPublishMarkerFrame) {
                            onPlaneStatus(false, PlacementHitKind.None)
                            onTrackingStatus(
                                cameraStatus,
                                anchorStatus,
                                trackingGuidance(frame.camera.trackingState, frame.camera.trackingFailureReason, anchor?.trackingState)
                            )
                        }
                        return@runCatching
                    } else {
                        latestLockedMarkerImage[0] = null
                        anchoredMarkerId[0] = null
                        markerLostFrames[0] = 0
                        markerStableFrames[0] = 0
                    }
                    val shouldProbeReticle = !paperGraphMode &&
                        viewportWidth > 0 &&
                        viewportHeight > 0 &&
                        frame.camera.trackingState == TrackingState.TRACKING &&
                        frame.timestamp - lastReticleHitTestTimestamp[0] > RETICLE_HIT_TEST_INTERVAL_NANOS
                    if (shouldProbeReticle) {
                        val commonSurface = commonSurfaceDetector.observe(
                            frame = frame,
                            screenX = viewportWidth / 2f,
                            screenY = viewportHeight / 2f,
                            mode = state.arEngineMode.toCommonSurfaceMode()
                        )
                        lastReticleHitKind[0] = commonSurface.toPlacementHitKind()
                        lastReticleHitTestTimestamp[0] = frame.timestamp
                    } else if (frame.camera.trackingState != TrackingState.TRACKING) {
                        lastReticleHitKind[0] = PlacementHitKind.None
                    } else {
                        lastReticleHitKind[0]
                    }
                    val hitKind = if (paperGraphMode) PlacementHitKind.None else lastReticleHitKind[0]
                    val pose = frame.camera.pose
                    poseSamples.addLast(ArPoseSample(pose.tx(), pose.ty(), pose.tz(), frame.timestamp))
                    while (poseSamples.size > 18) poseSamples.removeFirst()
                    val agedNativeSensorSample = nativeSensorSample?.copy(
                        sensorAgeMillis = (SystemClock.elapsedRealtime() - nativeSensorReceivedAtMs).coerceAtLeast(0L)
                    )
                    val sceneUnderstanding = if (paperGraphMode) {
                        ArSceneUnderstandingSample(
                            semanticsSupported = false,
                            semanticsEnabled = false,
                            semanticsAvailable = false,
                            depthSupported = false,
                            depthEnabled = false
                        )
                    } else {
                        frame.readSceneUnderstanding(
                            semanticsSupported = semanticsSupported,
                            semanticsEnabled = semanticsEnabled,
                            depthSupported = depthSupported,
                            depthEnabled = depthEnabled,
                            viewportWidth = viewportWidth,
                            viewportHeight = viewportHeight
                        )
                    }
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
                    if (frameCounter[0] % 30 == 0) {
                        Log.d(
                            AR_LOG_TAG,
                            "Frame camera=$cameraStatus anchor=$anchorStatus hit=$hitKind score=${fusion.score} native=${agedNativeSensorSample?.compactLog()} scene=${sceneUnderstanding.compactLog()}"
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
                        val frame = latestFrameRef[0]
                        if (frame == null) {
                            Log.w(AR_LOG_TAG, "Tap ignored because latest AR frame is null")
                            return@rememberOnGestureListener
                        }
                        if (paperGraphMode && anchor == null) {
                            val image = latestLockedMarkerImage[0] ?: frame.findLockedMarkerImage()
                            if (image != null) {
                                Log.d(AR_LOG_TAG, "Creating marker anchor from image=${image.name}")
                                anchoredMarkerId[0] = image.name.orEmpty().extractArMarkerId()
                                onAnchorChanged(image.createAnchor(image.centerPose))
                                return@rememberOnGestureListener
                            }
                        }
                        if (paperGraphMode && anchor != null && state.markerMathActivity == MarkerMathActivity.Geometry2D) {
                            val (localX, localY) = frame.markerLocalPointFromScreen(
                                image = latestLockedMarkerImage[0],
                                screenX = event.x,
                                screenY = event.y,
                                viewportWidth = viewportWidth,
                                viewportHeight = viewportHeight
                            )
                            onMarker2dWorkspaceTap(localX, localY)
                            return@rememberOnGestureListener
                        }
                        if (paperGraphMode && anchor != null && state.markerMathActivity == MarkerMathActivity.Geometry3D) {
                            val (localX, localZ) = frame.markerLocalPointFromScreen(
                                image = latestLockedMarkerImage[0],
                                screenX = event.x,
                                screenY = event.y,
                                viewportWidth = viewportWidth,
                                viewportHeight = viewportHeight
                            )
                            onMarker3dWorkspaceTap(localX, localZ)
                            return@rememberOnGestureListener
                        }
                        if (paperGraphMode && anchor != null && state.markerMathActivity == MarkerMathActivity.CoordinateLab) {
                            val (localX, localY) = frame.markerLocalPointFromScreen(
                                image = latestLockedMarkerImage[0],
                                screenX = event.x,
                                screenY = event.y,
                                viewportWidth = viewportWidth,
                                viewportHeight = viewportHeight
                            )
                            onMarkerCoordinateWorkspaceTap(localX, localY)
                            return@rememberOnGestureListener
                        }
                        val hit = findBestPlacementHit(
                            frame = frame,
                            x = event.x,
                            y = event.y,
                            mode = state.arEngineMode,
                            allowFeaturePoint = state.arEngineMode == ArEngineMode.AirPlacement,
                            allowInstant = state.arEngineMode == ArEngineMode.AirPlacement
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
                            Log.d(AR_LOG_TAG, "Tap missed all placement hits mode=${state.arEngineMode}")
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
                AnchorNode(
                    anchor = it,
                    visibleTrackingStates = setOf(TrackingState.TRACKING, TrackingState.PAUSED),
                    apply = {
                        visibleCameraTrackingStates = setOf(TrackingState.TRACKING, TrackingState.PAUSED)
                        isSmoothTransformEnabled = true
                        smoothTransformSpeed = 10f
                        isEditable = false
                    }
                ) {
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.FunctionGraph) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom.coerceIn(0.75f, 1.15f))
                        ) {
                            MarkerGraphWorkspaceNode(materialLoader)
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.Transformations) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom * if (interaction.expanded) 1.35f else 1f)
                        ) {
                            MarkerTransformationWorkspaceNode(materialLoader, state)
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.Trigonometry) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom * if (interaction.expanded) 1.35f else 1f)
                        ) {
                            MarkerTrigonometryWorkspaceNode(materialLoader, state)
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.MeasurementLab) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom * if (interaction.expanded) 1.35f else 1f)
                        ) {
                            MarkerMeasurementWorkspaceNode(materialLoader, state)
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.Geometry3D) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom.coerceIn(0.7f, 1.1f))
                        ) {
                            Marker3dSolidsWorkspaceNode(materialLoader, state)
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.CoordinateLab) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom * if (interaction.expanded) 1.35f else 1f)
                        ) {
                            MarkerCoordinateWorkspaceNode(materialLoader, state)
                        }
                    }
                    state.mathScene.objects
                        .filter { !state.isQuietMarkerMode() }
                        .filter { objectState -> objectState.visibility.visible }
                        .forEach { objectState ->
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
                                if (objectState.interactionState.selected && !state.isQuietMarkerMode()) SelectionHighlight(materialLoader)
                                if (objectState.interactionState.selected && !state.isQuietMarkerMode() && MathArFeature.GestureHandles in state.enabledMathArFeatures) {
                                    GestureHandleNodes(materialLoader)
                                }
                                if (objectState.interactionState.selected && state.compareModeEnabled) {
                                    CompareGhostNode(objectState.objectType, materialLoader, state)
                                }
                            }
                        }
                    }
                    if (state.isQuietMarkerMode() && state.markerMathActivity == MarkerMathActivity.Geometry2D) {
                        val interaction = state.markerLessonInteraction
                        Node(
                            rotation = Rotation(y = interaction.rotationDegrees),
                            scale = Scale(interaction.zoom.coerceIn(0.75f, 1.1f))
                        ) {
                            ConstructionGeometryNodes(state, materialLoader)
                        }
                    } else if (!state.isQuietMarkerMode()) {
                        ConstructionGeometryNodes(state, materialLoader)
                    }
                }
            }
            outdoorMeshAnchor?.let { meshAnchor ->
                AnchorNode(anchor = meshAnchor) {
                    OutdoorStreetscapeWireframe(outdoorWireframeEdges, materialLoader)
                }
            }
        }
        if (!state.isQuietMarkerMode()) {
            Reticle(state)
            TrackingHealthOverlay(state)
            MathInteractionOverlay(state)
        }
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

@Composable
private fun NodeScope.Marker3dSolidsWorkspaceNode(materialLoader: MaterialLoader, state: ArViewerUiState) {
    val edge = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathCyan) }
    val selected = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathViolet) }
    val fill = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x3342E8FF)) }
    val preview = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x55A45CFF)) }
    val section = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    state.marker3dSolids.filter { it.visible }.forEach { solid ->
        key(solid.id) {
            val dimensions = solid.dimensions()
            val fittedScale = solid.markerFittedScale(dimensions)
            Node(
                position = Position(
                    solid.x.coerceIn(-MARKER_SAFE_HALF_EXTENT_METERS, MARKER_SAFE_HALF_EXTENT_METERS),
                    solid.lift.coerceIn(0.004f, 0.03f),
                    solid.z.coerceIn(-MARKER_SAFE_HALF_EXTENT_METERS, MARKER_SAFE_HALF_EXTENT_METERS)
                ),
                rotation = Rotation(x = solid.rotationX, y = solid.rotationY, z = solid.rotationZ),
                scale = Scale(fittedScale)
            ) {
                Marker3dSolidNode(solid, state.marker3dOverlays, if (solid.selected) selected else edge, fill, section)
                if (solid.selected) Marker3dGizmoNode(selected, section)
            }
        }
    }
    state.marker3dPreviewTool?.let { tool ->
        val previewSolid = Marker3dSolidState(
            id = "preview",
            tool = tool,
            label = tool.uiLabel(),
            parameters = tool.defaultUiParameters()
        )
        Node(
            position = Position(
                state.marker3dPreviewX.coerceIn(-MARKER_SAFE_HALF_EXTENT_METERS, MARKER_SAFE_HALF_EXTENT_METERS),
                state.marker3dPreviewLift.coerceIn(0.004f, 0.03f),
                state.marker3dPreviewZ.coerceIn(-MARKER_SAFE_HALF_EXTENT_METERS, MARKER_SAFE_HALF_EXTENT_METERS)
            ),
            scale = Scale(previewSolid.markerFittedScale(previewSolid.dimensions()))
        ) {
            Marker3dSolidNode(
                solid = previewSolid,
                overlays = setOf(Marker3dOverlayOption.Edges, Marker3dOverlayOption.Faces),
                edge = preview,
                fill = preview,
                section = section
            )
        }
    }
}

@Composable
private fun NodeScope.Marker3dSolidNode(
    solid: Marker3dSolidState,
    overlays: Set<Marker3dOverlayOption>,
    edge: MaterialInstance,
    fill: MaterialInstance,
    section: MaterialInstance
) {
    val dimensions = solid.dimensions()
    if (Marker3dOverlayOption.Faces in overlays && solid.tool in setOf(
            Marker3dShapeTool.Cube,
            Marker3dShapeTool.Cuboid,
            Marker3dShapeTool.RectangularPrism,
            Marker3dShapeTool.CustomPrism
        )
    ) {
        CubeNode(
            Size(dimensions.width * 0.82f, dimensions.height * 0.82f, dimensions.depth * 0.82f),
            center = Position(0f, dimensions.height * 0.5f, 0f),
            materialInstance = fill
        )
    }
    when (solid.tool) {
        Marker3dShapeTool.Cube,
        Marker3dShapeTool.Cuboid,
        Marker3dShapeTool.RectangularPrism,
        Marker3dShapeTool.CustomPrism -> BoxEdges(edge, dimensions.width / 2f, dimensions.height, dimensions.depth / 2f)
        Marker3dShapeTool.Sphere -> SphereWire(edge, dimensions.width / 2f)
        Marker3dShapeTool.Hemisphere -> HemisphereWire(edge, dimensions.width / 2f)
        Marker3dShapeTool.Cylinder -> CylinderWire(edge, dimensions.width / 2f, dimensions.height)
        Marker3dShapeTool.Cone -> ConeWire(edge, dimensions.width / 2f, dimensions.height)
        Marker3dShapeTool.Pyramid,
        Marker3dShapeTool.CustomPyramid -> PyramidWire(edge, dimensions.width / 2f, dimensions.depth / 2f, dimensions.height, solid.exploded)
        Marker3dShapeTool.TriangularPrism -> TriangularPrismWire(edge, dimensions.width, dimensions.height, dimensions.depth)
        Marker3dShapeTool.Tetrahedron -> TetrahedronWire(edge, dimensions.width)
        Marker3dShapeTool.Torus -> TorusWire(edge, dimensions.width / 2f, dimensions.height / 3f)
        Marker3dShapeTool.Frustum -> FrustumWire(edge, dimensions.width / 2f, dimensions.width / 3f, dimensions.height)
    }
    if (Marker3dOverlayOption.Vertices in overlays) Marker3dVertexNodes(solid, dimensions, section)
    if (Marker3dOverlayOption.CrossSection in overlays || solid.sectionMode != Marker3dSectionMode.None) {
        val y = dimensions.height * solid.clipping
        when (solid.sectionMode) {
            Marker3dSectionMode.Vertical -> {
                LineNode(Position(0f, 0f, -dimensions.depth * 0.62f), Position(0f, dimensions.height, -dimensions.depth * 0.62f), section)
                LineNode(Position(0f, dimensions.height, -dimensions.depth * 0.62f), Position(0f, dimensions.height, dimensions.depth * 0.62f), section)
                LineNode(Position(0f, dimensions.height, dimensions.depth * 0.62f), Position(0f, 0f, dimensions.depth * 0.62f), section)
                LineNode(Position(0f, 0f, dimensions.depth * 0.62f), Position(0f, 0f, -dimensions.depth * 0.62f), section)
            }
            else -> {
                LineNode(Position(-dimensions.width * 0.62f, y, -dimensions.depth * 0.62f), Position(dimensions.width * 0.62f, y, -dimensions.depth * 0.62f), section)
                LineNode(Position(dimensions.width * 0.62f, y, -dimensions.depth * 0.62f), Position(dimensions.width * 0.62f, y, dimensions.depth * 0.62f), section)
                LineNode(Position(dimensions.width * 0.62f, y, dimensions.depth * 0.62f), Position(-dimensions.width * 0.62f, y, dimensions.depth * 0.62f), section)
                LineNode(Position(-dimensions.width * 0.62f, y, dimensions.depth * 0.62f), Position(-dimensions.width * 0.62f, y, -dimensions.depth * 0.62f), section)
            }
        }
    }
    if (solid.showNet || Marker3dOverlayOption.Net in overlays) Marker3dNetNode(solid, edge, section)
}

@Composable
private fun NodeScope.Marker3dGizmoNode(edge: MaterialInstance, lift: MaterialInstance) {
    val length = 0.042f
    ThickLineRenderer(Position(0f, 0f, 0f), Position(length, 0f, 0f), edge, edge, ThickLineStyle(widthDp = 2.2f, glowWidthDp = 3.4f))
    LineNode(Position(0f, 0f, 0f), Position(0f, length, 0f), lift)
    ThickLineRenderer(Position(0f, 0f, 0f), Position(0f, 0f, length), edge, edge, ThickLineStyle(widthDp = 2.2f, glowWidthDp = 3.4f))
    RadiantPointRenderer(Position(length, 0f, 0f), edge, edge, edge, RadiantPointStyle(isDraggable = true))
    RadiantPointRenderer(Position(0f, length, 0f), lift, lift, lift, RadiantPointStyle(isDraggable = true))
    RadiantPointRenderer(Position(0f, 0f, length), edge, edge, edge, RadiantPointStyle(isDraggable = true))
}

private data class MarkerSolidDimensions(val width: Float, val height: Float, val depth: Float)

private fun Marker3dSolidState.markerFittedScale(dimensions: MarkerSolidDimensions): Float = minOf(
    scale.coerceAtLeast(0.35f),
    (MARKER_SAFE_HALF_EXTENT_METERS * 1.75f) / dimensions.width.coerceAtLeast(0.001f),
    (MARKER_SAFE_HALF_EXTENT_METERS * 1.75f) / dimensions.depth.coerceAtLeast(0.001f),
    MARKER_SAFE_SOLID_HEIGHT_METERS / dimensions.height.coerceAtLeast(0.001f)
)

private fun Marker3dSolidState.dimensions(): MarkerSolidDimensions = when (tool) {
    Marker3dShapeTool.Cube -> {
        val side = parameters["side"] ?: 0.16f
        MarkerSolidDimensions(side, side, side)
    }
    Marker3dShapeTool.Cuboid,
    Marker3dShapeTool.RectangularPrism -> MarkerSolidDimensions(parameters["length"] ?: 0.22f, parameters["height"] ?: 0.16f, parameters["width"] ?: 0.14f)
    Marker3dShapeTool.Sphere,
    Marker3dShapeTool.Hemisphere,
    Marker3dShapeTool.Torus -> {
        val diameter = (parameters["radius"] ?: 0.1f) * 2f
        MarkerSolidDimensions(diameter, diameter, diameter)
    }
    Marker3dShapeTool.Cylinder,
    Marker3dShapeTool.Cone,
    Marker3dShapeTool.Frustum -> {
        val diameter = (parameters["radius"] ?: 0.09f) * 2f
        MarkerSolidDimensions(diameter, parameters["height"] ?: 0.2f, diameter)
    }
    Marker3dShapeTool.Pyramid,
    Marker3dShapeTool.CustomPyramid -> {
        val base = parameters["base"] ?: 0.2f
        MarkerSolidDimensions(base, parameters["height"] ?: 0.2f, base)
    }
    Marker3dShapeTool.TriangularPrism,
    Marker3dShapeTool.CustomPrism -> MarkerSolidDimensions(parameters["base"] ?: 0.18f, parameters["height"] ?: 0.16f, parameters["length"] ?: 0.24f)
    Marker3dShapeTool.Tetrahedron -> {
        val side = parameters["side"] ?: 0.18f
        MarkerSolidDimensions(side, side * 0.82f, side)
    }
}

private fun MathObjectType.isGraphLike(): Boolean =
    this in setOf(MathObjectType.SineCurve, MathObjectType.CoordinatePlane, MathObjectType.NumberLine, MathObjectType.VectorArrow)

@Composable
private fun NodeScope.MarkerGraphWorkspaceNode(
    materialLoader: MaterialLoader
) {
    val axis = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.94f))
    }
    val axisGlow = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.10f))
    }
    val surfacePalette = remember(materialLoader) {
        listOf(
            Color(0xFF42FFD9),
            Color(0xFF28E9FF),
            Color(0xFF4EA7FF),
            Color(0xFF7968FF),
            Color(0xFFC34DFF)
        ).map { materialLoader.createUnlitColorInstance(it.copy(alpha = 0.88f)) }
    }
    MarkerParaboloidSurface(surfacePalette)
    MarkerSimpleGraphAxes(axis, axisGlow)
}

@Composable
private fun NodeScope.MarkerParaboloidSurface(palette: List<MaterialInstance>) {
    val domain = (-3..3).map { it * (2f / 3f) }
    fun point(x: Float, y: Float): Position {
        val height = 0.005f + (x * x + y * y) * 0.0062f
        return Position(x * 0.026f, height, -y * 0.026f)
    }
    fun material(a: Position, b: Position): MaterialInstance {
        val normalizedHeight = (((a.y + b.y) * 0.5f - 0.005f) / 0.05f).coerceIn(0f, 0.999f)
        return palette[(normalizedHeight * palette.size).toInt().coerceIn(0, palette.lastIndex)]
    }
    domain.forEach { x ->
        domain.zipWithNext().forEach { (y0, y1) ->
            val start = point(x, y0)
            val end = point(x, y1)
            val line = material(start, end)
            ThickLineRenderer(start, end, line, line, ThickLineStyle(widthDp = 1.35f, glowWidthDp = 2.1f, opacity = 0.88f))
        }
    }
    domain.forEach { y ->
        domain.zipWithNext().forEach { (x0, x1) ->
            val start = point(x0, y)
            val end = point(x1, y)
            val line = material(start, end)
            ThickLineRenderer(start, end, line, line, ThickLineStyle(widthDp = 1.35f, glowWidthDp = 2.1f, opacity = 0.88f))
        }
    }
    RadiantPointRenderer(
        Position(0f, 0.007f, 0f),
        palette.first(),
        palette.first(),
        palette.first(),
        RadiantPointStyle(centreRadius = 0.0035f, haloRadius = 0.007f)
    )
}

@Composable
private fun NodeScope.MarkerSimpleGraphAxes(
    axis: MaterialInstance,
    axisGlow: MaterialInstance
) {
    // G01 is registered as a 16 cm target. Keep this first graph workspace
    // inside the marker instead of rendering the previous 84 cm-wide scene.
    val half = 0.062f
    val tickStep = 0.026f
    val axisHeight = 0.0035f
    ThickLineRenderer(
        Position(-half, axisHeight, 0f),
        Position(half, axisHeight, 0f),
        axis,
        axisGlow,
        ThickLineStyle(widthDp = 2.1f, glowWidthDp = 3.4f)
    )
    ThickLineRenderer(
        Position(0f, axisHeight, half),
        Position(0f, axisHeight, -half),
        axis,
        axisGlow,
        ThickLineStyle(widthDp = 2.1f, glowWidthDp = 3.4f)
    )
    ThickLineRenderer(
        Position(0f, axisHeight, 0f),
        Position(0f, 0.066f, 0f),
        axis,
        axisGlow,
        ThickLineStyle(widthDp = 2.1f, glowWidthDp = 3.4f)
    )

    val arrow = 0.006f
    LineNode(Position(half, axisHeight, 0f), Position(half - arrow, axisHeight, arrow * 0.6f), axis)
    LineNode(Position(half, axisHeight, 0f), Position(half - arrow, axisHeight, -arrow * 0.6f), axis)
    LineNode(Position(0f, axisHeight, -half), Position(arrow * 0.6f, axisHeight, -half + arrow), axis)
    LineNode(Position(0f, axisHeight, -half), Position(-arrow * 0.6f, axisHeight, -half + arrow), axis)

    (-2..2).forEach { value ->
        val p = value * tickStep
        LineNode(Position(p, axisHeight, -0.0022f), Position(p, axisHeight, 0.0022f), axis)
        LineNode(Position(-0.0022f, axisHeight, -p), Position(0.0022f, axisHeight, -p), axis)
        if (value != 0) {
            MarkerAxisNumber(
                value = value,
                centerX = p,
                centerZ = 0.0085f,
                height = axisHeight + 0.0005f,
                material = axis
            )
            MarkerAxisNumber(
                value = value,
                centerX = -0.009f,
                centerZ = -p,
                height = axisHeight + 0.0005f,
                material = axis
            )
        }
    }
    MarkerAxisNumber(0, -0.006f, 0.0085f, axisHeight + 0.0005f, axis)
    MarkerAxisLetterX(half + 0.009f, 0f, axisHeight + 0.0005f, axis)
    MarkerAxisLetterY(0f, -half - 0.010f, axisHeight + 0.0005f, axis)
    MarkerAxisLetterZ(0f, 0.069f, 0f, axis)
}

@Composable
private fun NodeScope.MarkerAxisNumber(
    value: Int,
    centerX: Float,
    centerZ: Float,
    height: Float,
    material: MaterialInstance
) {
    val glyphs = value.toString()
    val glyphWidth = 0.0042f
    val glyphHeight = 0.0068f
    val gap = 0.0012f
    val totalWidth = glyphs.length * glyphWidth + (glyphs.length - 1).coerceAtLeast(0) * gap
    glyphs.forEachIndexed { index, glyph ->
        MarkerAxisGlyph(
            glyph = glyph,
            left = centerX - totalWidth / 2f + index * (glyphWidth + gap),
            centerZ = centerZ,
            width = glyphWidth,
            height = glyphHeight,
            y = height,
            material = material
        )
    }
}

@Composable
private fun NodeScope.MarkerAxisGlyph(
    glyph: Char,
    left: Float,
    centerZ: Float,
    width: Float,
    height: Float,
    y: Float,
    material: MaterialInstance
) {
    val segments = when (glyph) {
        '0' -> "abcdef"
        '1' -> "bc"
        '2' -> "abdeg"
        '3' -> "abcdg"
        '4' -> "bcfg"
        '5' -> "acdfg"
        '6' -> "acdefg"
        '7' -> "abc"
        '8' -> "abcdefg"
        '9' -> "abcdfg"
        '-' -> "g"
        else -> ""
    }
    val right = left + width
    val top = centerZ - height / 2f
    val middle = centerZ
    val bottom = centerZ + height / 2f
    if ('a' in segments) LineNode(Position(left, y, top), Position(right, y, top), material)
    if ('b' in segments) LineNode(Position(right, y, top), Position(right, y, middle), material)
    if ('c' in segments) LineNode(Position(right, y, middle), Position(right, y, bottom), material)
    if ('d' in segments) LineNode(Position(left, y, bottom), Position(right, y, bottom), material)
    if ('e' in segments) LineNode(Position(left, y, middle), Position(left, y, bottom), material)
    if ('f' in segments) LineNode(Position(left, y, top), Position(left, y, middle), material)
    if ('g' in segments) LineNode(Position(left, y, middle), Position(right, y, middle), material)
}

@Composable
private fun NodeScope.MarkerAxisLetterX(x: Float, z: Float, y: Float, material: MaterialInstance) {
    val r = 0.0032f
    LineNode(Position(x - r, y, z - r), Position(x + r, y, z + r), material)
    LineNode(Position(x - r, y, z + r), Position(x + r, y, z - r), material)
}

@Composable
private fun NodeScope.MarkerAxisLetterY(x: Float, z: Float, y: Float, material: MaterialInstance) {
    val r = 0.0032f
    LineNode(Position(x - r, y, z - r), Position(x, y, z), material)
    LineNode(Position(x + r, y, z - r), Position(x, y, z), material)
    LineNode(Position(x, y, z), Position(x, y, z + r), material)
}

@Composable
private fun NodeScope.MarkerAxisLetterZ(x: Float, y: Float, z: Float, material: MaterialInstance) {
    val r = 0.0032f
    LineNode(Position(x - r, y + r, z), Position(x + r, y + r, z), material)
    LineNode(Position(x + r, y + r, z), Position(x - r, y - r, z), material)
    LineNode(Position(x - r, y - r, z), Position(x + r, y - r, z), material)
}

@Composable
private fun NodeScope.MarkerGraphBaseGrid(
    axis: MaterialInstance,
    gridMinor: MaterialInstance,
    gridMajor: MaterialInstance,
    zAxis: MaterialInstance,
    origin: MaterialInstance,
    planeFill: MaterialInstance,
    axisGlow: MaterialInstance,
    pointGlow: MaterialInstance,
    state: ArViewerUiState
) {
    val half = 0.42f
    if (state.markerGraphShowGrid) {
        (-8..8).forEach { i ->
            val p = i * (half / 8f)
            val material = if (i % 4 == 0) gridMajor else gridMinor
            ThickLineRenderer(Position(-half, 0.004f, p), Position(half, 0.004f, p), material, material, ThickLineStyle(widthDp = if (i % 4 == 0) 1.6f else 0.8f, glowWidthDp = if (i % 4 == 0) 2.2f else 1f))
            ThickLineRenderer(Position(p, 0.004f, -half), Position(p, 0.004f, half), material, material, ThickLineStyle(widthDp = if (i % 4 == 0) 1.6f else 0.8f, glowWidthDp = if (i % 4 == 0) 2.2f else 1f))
        }
    }
    ThickLineRenderer(Position(-half * 1.08f, 0.018f, 0f), Position(half * 1.08f, 0.018f, 0f), axis, axisGlow, ThickLineStyle(widthDp = 6.2f, glowWidthDp = 10f))
    ThickLineRenderer(Position(0f, 0.018f, -half * 1.08f), Position(0f, 0.018f, half * 1.08f), axis, axisGlow, ThickLineStyle(widthDp = 6.2f, glowWidthDp = 10f))
    LineNode(Position(0f, 0.012f, 0f), Position(0f, 0.42f, 0f), zAxis)
    ThickLineRenderer(Position(half * 1.08f, 0.018f, 0f), Position(half * 0.96f, 0.018f, 0.028f), axis, axisGlow, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(half * 1.08f, 0.018f, 0f), Position(half * 0.96f, 0.018f, -0.028f), axis, axisGlow, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.018f, half * 1.08f), Position(0.028f, 0.018f, half * 0.96f), axis, axisGlow, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.018f, half * 1.08f), Position(-0.028f, 0.018f, half * 0.96f), axis, axisGlow, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    LineNode(Position(0f, 0.42f, 0f), Position(0.018f, 0.38f, 0f), zAxis)
    LineNode(Position(0f, 0.42f, 0f), Position(-0.018f, 0.38f, 0f), zAxis)
    (-2..2).forEach { tick ->
        val p = tick * 0.16f
        ThickLineRenderer(Position(p, 0.018f, -0.014f), Position(p, 0.018f, 0.014f), axis, axisGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3f))
        ThickLineRenderer(Position(-0.014f, 0.018f, p), Position(0.014f, 0.018f, p), axis, axisGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3f))
        if (tick != 0) {
            val tickSize = if (state.markerGraphShowLabels) 0.014f else 0.01f
            RadiantPointRenderer(Position(p, 0.022f, 0f), gridMajor, pointGlow, gridMajor, RadiantPointStyle(centreRadius = tickSize * 0.45f, haloRadius = tickSize))
            RadiantPointRenderer(Position(0f, 0.022f, p), gridMajor, pointGlow, gridMajor, RadiantPointStyle(centreRadius = tickSize * 0.45f, haloRadius = tickSize))
        }
    }
    RadiantPointRenderer(Position(0f, 0.024f, 0f), origin, pointGlow, origin, RadiantPointStyle(isSelected = true))
}

@Composable
private fun NodeScope.MarkerSurfaceGraph(
    compiled: ArCompiledExpression,
    palette: List<MaterialInstance>,
    accent: MaterialInstance
) {
    val samples = (-8..8).map { it / 4f }
    samples.forEachIndexed { row, y ->
        samples.zipWithNext().forEachIndexed { col, (x0, x1) ->
            val p0 = markerSurfacePoint(compiled, x0, y)
            val p1 = markerSurfacePoint(compiled, x1, y)
            LineNode(p0, p1, palette[(row + col).floorMod(palette.size)])
        }
    }
    samples.forEachIndexed { col, x ->
        samples.zipWithNext().forEachIndexed { row, (y0, y1) ->
            val p0 = markerSurfacePoint(compiled, x, y0)
            val p1 = markerSurfacePoint(compiled, x, y1)
            LineNode(p0, p1, palette[(row + col + 2).floorMod(palette.size)])
        }
    }
    listOf(-2f, 0f, 2f).forEach { value ->
        LineNode(markerSurfacePoint(compiled, -2f, value), markerSurfacePoint(compiled, 2f, value), accent)
        LineNode(markerSurfacePoint(compiled, value, -2f), markerSurfacePoint(compiled, value, 2f), accent)
    }
}

@Composable
private fun NodeScope.MarkerCurveGraph(
    graph: MarkerGraphFunctionState,
    state: ArViewerUiState,
    palette: List<MaterialInstance>,
    accent: MaterialInstance
) {
    val material = palette[graph.colorIndex.floorMod(palette.size)]
    val glow = material
    val samples = markerFunctionSamples(graph.compiled, state.arGraphDomain, 220)
    samples.zipWithNext().forEachIndexed { index, (a, b) ->
        if (a.y.isFinite() && b.y.isFinite() && abs(a.y - b.y) <= discontinuityThreshold(state.arGraphDomain)) {
            ThickLineRenderer(
                Position(markerGraphX(a.x.toFloat(), state.arGraphDomain), 0.075f, markerGraphY(a.y.toFloat(), state.arGraphDomain)),
                Position(markerGraphX(b.x.toFloat(), state.arGraphDomain), 0.075f, markerGraphY(b.y.toFloat(), state.arGraphDomain)),
                material,
                glow,
                ThickLineStyle(widthDp = if (graph.selected) 8.5f else 7f, glowWidthDp = if (graph.selected) 16f else 12f, isSelected = graph.selected)
            )
            if (graph.selected && index % 26 == 0) {
                RadiantPointRenderer(
                    Position(markerGraphX(a.x.toFloat(), state.arGraphDomain), 0.092f, markerGraphY(a.y.toFloat(), state.arGraphDomain)),
                    material,
                    glow,
                    material,
                    RadiantPointStyle(centreRadius = 0.01f, haloRadius = 0.022f)
                )
            }
        }
    }
}

@Composable
private fun NodeScope.MarkerGraphTraceNode(
    graph: MarkerGraphFunctionState,
    state: ArViewerUiState,
    line: MaterialInstance,
    pointMaterial: MaterialInstance,
    pointGlow: MaterialInstance
) {
    if (state.markerGraphTraceMode == MarkerGraphTraceMode.Off) return
    val x = state.arGraphDomain.xMin + (state.arGraphDomain.xMax - state.arGraphDomain.xMin) * state.markerGraphTraceProgress
    val y = arRenderMathEngine.evaluate2dUnclamped(graph.compiled, x)
    if (!y.isFinite() || y !in state.arGraphDomain.yMin..state.arGraphDomain.yMax) return
    val point = Position(markerGraphX(x.toFloat(), state.arGraphDomain), 0.075f, markerGraphY(y.toFloat(), state.arGraphDomain))
    RadiantPointRenderer(point, pointMaterial, pointGlow, pointMaterial, RadiantPointStyle(isSelected = true, isDraggable = true))
    if (state.markerGraphTraceMode == MarkerGraphTraceMode.Tangent || state.markerGraphShowDerivative) {
        val slope = state.arGraphAnalysis.tangent?.tangentSlope ?: 0.0
        val dx = (state.arGraphDomain.xMax - state.arGraphDomain.xMin) * 0.08
        val y0 = y - slope * dx
        val y1 = y + slope * dx
        ThickLineRenderer(
            Position(markerGraphX((x - dx).toFloat(), state.arGraphDomain), 0.062f, markerGraphY(y0.toFloat(), state.arGraphDomain)),
            Position(markerGraphX((x + dx).toFloat(), state.arGraphDomain), 0.062f, markerGraphY(y1.toFloat(), state.arGraphDomain)),
            line,
            pointGlow,
            ThickLineStyle(widthDp = 4.5f, glowWidthDp = 7f)
        )
    }
    if (state.markerGraphTraceMode == MarkerGraphTraceMode.Integral || state.markerGraphShowIntegralArea) {
        val lower = min(state.arGraphDomain.xMin, x)
        val upper = max(state.arGraphDomain.xMin, x)
        markerFunctionSamples(graph.compiled, state.arGraphDomain.copy(xMin = lower, xMax = upper), 48).forEach { sample ->
            if (sample.y.isFinite()) {
                ThickLineRenderer(
                    Position(markerGraphX(sample.x.toFloat(), state.arGraphDomain), 0.024f, 0f),
                    Position(markerGraphX(sample.x.toFloat(), state.arGraphDomain), 0.024f, markerGraphY(sample.y.toFloat(), state.arGraphDomain)),
                    line,
                    pointGlow,
                    ThickLineStyle(widthDp = 2.2f, glowWidthDp = 3f, opacity = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun NodeScope.MarkerGraphAnalysisNodes(
    graph: MarkerGraphFunctionState,
    state: ArViewerUiState,
    markerMaterial: MaterialInstance,
    warningMaterial: MaterialInstance,
    pointGlow: MaterialInstance
) {
    if (state.markerGraphShowIntercepts) {
        state.arGraphAnalysis.roots.forEach { point ->
            if (point.x in state.arGraphDomain.xMin..state.arGraphDomain.xMax) {
                RadiantPointRenderer(Position(markerGraphX(point.x.toFloat(), state.arGraphDomain), 0.07f, markerGraphY(0f, state.arGraphDomain)), markerMaterial, pointGlow, markerMaterial)
            }
        }
        val yIntercept = arRenderMathEngine.evaluate2dUnclamped(graph.compiled, 0.0)
        if (yIntercept.isFinite() && yIntercept in state.arGraphDomain.yMin..state.arGraphDomain.yMax) {
            RadiantPointRenderer(Position(markerGraphX(0f, state.arGraphDomain), 0.07f, markerGraphY(yIntercept.toFloat(), state.arGraphDomain)), markerMaterial, pointGlow, markerMaterial)
        }
    }
    if (state.markerGraphShowExtrema) {
        state.arGraphAnalysis.extrema.forEach { point ->
            if (point.x in state.arGraphDomain.xMin..state.arGraphDomain.xMax && point.y in state.arGraphDomain.yMin..state.arGraphDomain.yMax) {
                RadiantPointRenderer(Position(markerGraphX(point.x.toFloat(), state.arGraphDomain), 0.085f, markerGraphY(point.y.toFloat(), state.arGraphDomain)), markerMaterial, pointGlow, markerMaterial, RadiantPointStyle(isSelected = true))
            }
        }
    }
    if (state.markerGraphShowDiscontinuities) {
        markerDiscontinuityCandidates(graph.compiled, state.arGraphDomain).forEach { x ->
            ThickLineRenderer(
                Position(markerGraphX(x.toFloat(), state.arGraphDomain), 0.018f, -0.32f),
                Position(markerGraphX(x.toFloat(), state.arGraphDomain), 0.018f, 0.32f),
                warningMaterial,
                pointGlow,
                ThickLineStyle(widthDp = 3f, glowWidthDp = 5f, isDashed = true)
            )
        }
    }
}

private fun markerSurfacePoint(compiled: ArCompiledExpression, x: Float, y: Float): Position {
    val z = arRenderMathEngine.evaluate3d(compiled, x.toDouble(), y.toDouble()).toArFloat()
    return Position(x * 0.12f, 0.035f + ((z + 1.6f) / 3.2f) * 0.32f, y * 0.12f)
}

private data class MarkerGraphSample(val x: Double, val y: Double)

private fun markerFunctionSamples(compiled: ArCompiledExpression, domain: ArGraphDomain, samples: Int): List<MarkerGraphSample> =
    (0..samples).map { index ->
        val x = domain.xMin + (domain.xMax - domain.xMin) * index / samples
        MarkerGraphSample(x, arRenderMathEngine.evaluate2dUnclamped(compiled, x))
    }

private fun discontinuityThreshold(domain: ArGraphDomain): Double =
    max(3.0, (domain.yMax - domain.yMin) * 0.65)

private fun markerDiscontinuityCandidates(compiled: ArCompiledExpression, domain: ArGraphDomain): List<Double> {
    val samples = markerFunctionSamples(compiled, domain, 160)
    return samples.zipWithNext().mapNotNull { (a, b) ->
        when {
            !a.y.isFinite() && b.y.isFinite() -> b.x
            a.y.isFinite() && !b.y.isFinite() -> a.x
            a.y.isFinite() && b.y.isFinite() && abs(a.y - b.y) > discontinuityThreshold(domain) -> (a.x + b.x) / 2.0
            else -> null
        }
    }.distinctBy { "%.2f".format(it) }.take(10)
}

private data class MarkerGraphAnnotation(
    val x: Double,
    val y: Double,
    val label: String,
    val colorIndex: Int,
    val priority: Int = 0
)

@Composable
private fun NodeScope.MarkerGraphWorkspaceAnnotations(
    state: ArViewerUiState,
    palette: List<MaterialInstance>,
    markerMaterial: MaterialInstance,
    markerGlow: MaterialInstance
) {
    val graphs = state.markerGraphFunctions.filter { it.visible && it.isValid && it.compiled.kind != GraphExpressionKind.ExplicitSurface3D }
    val annotations = remember(graphs, state.arGraphDomain, state.markerGraphShowIntercepts, state.markerGraphShowExtrema) {
        markerGraphAnnotations(graphs, state.arGraphDomain, state.markerGraphShowIntercepts, state.markerGraphShowExtrema)
    }
    annotations.take(12).forEach { annotation ->
        val x = markerGraphX(annotation.x.toFloat(), state.arGraphDomain)
        val z = markerGraphY(annotation.y.toFloat(), state.arGraphDomain)
        val material = palette.getOrElse(annotation.colorIndex.floorMod(palette.size)) { markerMaterial }
        RadiantPointRenderer(
            center = Position(x, 0.09f, z),
            materialInstance = material,
            glowMaterialInstance = markerGlow,
            haloMaterialInstance = material,
            style = RadiantPointStyle(isSelected = annotation.priority >= 2)
        )
    }
}

private fun markerGraphAnnotations(
    graphs: List<MarkerGraphFunctionState>,
    domain: ArGraphDomain,
    showIntercepts: Boolean,
    showExtrema: Boolean
): List<MarkerGraphAnnotation> {
    val annotations = mutableListOf<MarkerGraphAnnotation>()
    if (showIntercepts) {
        graphs.forEachIndexed { graphIndex, graph ->
            markerGraphRoots(graph.compiled, domain).take(4).forEach { x ->
                annotations += MarkerGraphAnnotation(x, 0.0, "(${x.graphLabel()}, 0)\nx-intercept", graph.colorIndex, priority = 1)
            }
            val yIntercept = arRenderMathEngine.evaluate2dUnclamped(graph.compiled, 0.0)
            if (yIntercept.isFinite() && yIntercept in domain.yMin..domain.yMax) {
                annotations += MarkerGraphAnnotation(0.0, yIntercept, "(0, ${yIntercept.graphLabel()})\ny-intercept", graph.colorIndex, priority = 1)
            }
            markerGraphPairIntersections(graphs.drop(graphIndex + 1), graph, domain).take(3).forEach { (x, y) ->
                annotations += MarkerGraphAnnotation(x, y, "(${x.graphLabel()}, ${y.graphLabel()})\nintersection", graph.colorIndex, priority = 2)
            }
        }
    }
    if (showExtrema) {
        graphs.forEach { graph ->
            markerGraphExtrema(graph.compiled, domain).take(3).forEach { (x, y) ->
                annotations += MarkerGraphAnnotation(x, y, "(${x.graphLabel()}, ${y.graphLabel()})\nvertex", graph.colorIndex, priority = 2)
            }
        }
    }
    return annotations
        .filter { it.x in domain.xMin..domain.xMax && it.y in domain.yMin..domain.yMax }
        .distinctBy { "${it.label}-${it.x.graphLabel()}-${it.y.graphLabel()}" }
        .sortedWith(compareByDescending<MarkerGraphAnnotation> { it.priority }.thenBy { abs(it.x) + abs(it.y) })
}

private fun markerGraphRoots(compiled: ArCompiledExpression, domain: ArGraphDomain): List<Double> {
    val samples = markerFunctionSamples(compiled, domain, 260)
    return samples.zipWithNext().mapNotNull { (a, b) ->
        when {
            !a.y.isFinite() || !b.y.isFinite() -> null
            abs(a.y) < 0.04 -> a.x
            a.y.signChangedWith(b.y) -> interpolateZero(a.x, a.y, b.x, b.y)
            else -> null
        }
    }.distinctClose()
}

private fun markerGraphExtrema(compiled: ArCompiledExpression, domain: ArGraphDomain): List<Pair<Double, Double>> {
    val samples = markerFunctionSamples(compiled, domain, 320).filter { it.y.isFinite() }
    return samples.windowed(3).mapNotNull { (a, b, c) ->
        val isMin = b.y <= a.y && b.y <= c.y && (a.y - b.y > 0.03 || c.y - b.y > 0.03)
        val isMax = b.y >= a.y && b.y >= c.y && (b.y - a.y > 0.03 || b.y - c.y > 0.03)
        if (isMin || isMax) b.x to b.y else null
    }.distinctBy { it.first.graphLabel() }.take(6)
}

private fun markerGraphPairIntersections(
    others: List<MarkerGraphFunctionState>,
    graph: MarkerGraphFunctionState,
    domain: ArGraphDomain
): List<Pair<Double, Double>> =
    others.flatMap { other ->
        val samples = (0..280).map { index ->
            val x = domain.xMin + (domain.xMax - domain.xMin) * index / 280.0
            val yA = arRenderMathEngine.evaluate2dUnclamped(graph.compiled, x)
            val yB = arRenderMathEngine.evaluate2dUnclamped(other.compiled, x)
            MarkerGraphSample(x, if (yA.isFinite() && yB.isFinite()) yA - yB else Double.NaN)
        }
        samples.zipWithNext().mapNotNull { (a, b) ->
            if (!a.y.isFinite() || !b.y.isFinite() || !a.y.signChangedWith(b.y)) return@mapNotNull null
            val x = interpolateZero(a.x, a.y, b.x, b.y)
            val y = arRenderMathEngine.evaluate2dUnclamped(graph.compiled, x)
            if (y.isFinite()) x to y else null
        }
    }.distinctBy { it.first.graphLabel() }

private fun Double.signChangedWith(other: Double): Boolean = (this < 0.0 && other > 0.0) || (this > 0.0 && other < 0.0)

private fun interpolateZero(x0: Double, y0: Double, x1: Double, y1: Double): Double =
    x0 + (0.0 - y0) * (x1 - x0) / (y1 - y0).let { if (abs(it) > 1e-9) it else 1e-9 }

private fun List<Double>.distinctClose(): List<Double> =
    sorted().fold(emptyList()) { acc, value -> if (acc.any { abs(it - value) < 0.08 }) acc else acc + value }

private fun Double.graphLabel(): String =
    when {
        abs(this) < 0.0001 -> "0"
        abs(this - kotlin.math.round(this)) < 0.015 -> kotlin.math.round(this).toInt().toString()
        else -> "%.3f".format(this).trimEnd('0').trimEnd('.')
    }

private fun markerGraphLabelColor(colorIndex: Int): Int = when (colorIndex.floorMod(6)) {
    0 -> android.graphics.Color.rgb(70, 235, 255)
    1 -> android.graphics.Color.rgb(180, 110, 255)
    2 -> android.graphics.Color.rgb(88, 242, 194)
    3 -> android.graphics.Color.rgb(255, 200, 87)
    4 -> android.graphics.Color.rgb(255, 123, 203)
    else -> android.graphics.Color.rgb(255, 255, 255)
}

private fun markerGraphX(x: Float): Float = (x / (2f * PI.toFloat())).coerceIn(-1f, 1f) * 0.28f

private fun markerGraphX(x: Float, domain: ArGraphDomain): Float =
    (((x - domain.xMin.toFloat()) / (domain.xMax - domain.xMin).toFloat()) - 0.5f).coerceIn(-0.6f, 0.6f) * 0.72f

private fun markerGraphY(y: Float): Float = y.coerceIn(-1.6f, 1.6f) * 0.14f

private fun markerGraphY(y: Float, domain: ArGraphDomain): Float =
    (((y - domain.yMin.toFloat()) / (domain.yMax - domain.yMin).toFloat()) - 0.5f).coerceIn(-0.6f, 0.6f) * 0.72f

@Composable
private fun NodeScope.MarkerTransformationWorkspaceNode(
    materialLoader: MaterialLoader,
    state: ArViewerUiState
) {
    val axis = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.88f)) }
    val gridMinor = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF70E6FF).copy(alpha = 0.18f)) }
    val gridMajor = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFB88CFF).copy(alpha = 0.35f)) }
    val origin = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF5CFFF0)) }
    val originalMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF53F2FF)) }
    val transformedMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFA45CFF)) }
    val pathMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val matrixMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF58F2C2)) }
    val centerMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFF176)) }
    MarkerFlatGrid(axis, gridMinor, gridMajor, origin)
    val original = markerTransformShapePoints(state.markerTransformShape)
    val target = original.map { it.transformedBy(state) }
    val current = original.zip(target).map { (start, end) -> start.lerpTo(end, state.markerTransformProgress) }
    MarkerShape(original, state.markerTransformShape, originalMaterial, height = 0.034f)
    MarkerShape(current, state.markerTransformShape, transformedMaterial, height = 0.062f)
    original.zip(current).forEach { (start, end) ->
        ThickLineRenderer(start.toPosition(0.048f), end.toPosition(0.048f), pathMaterial, pathMaterial, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f, isDashed = true))
        VectorHead(start.toPosition(0.048f), end.toPosition(0.048f), pathMaterial)
        RadiantPointRenderer(end.toPosition(0.068f), transformedMaterial, transformedMaterial, transformedMaterial, RadiantPointStyle(isSelected = true))
    }
    target.forEach { point ->
        ThickLineRenderer(point.toPosition(0.02f), point.toPosition(0.09f), matrixMaterial, matrixMaterial, ThickLineStyle(widthDp = 2f, glowWidthDp = 3f))
    }
    MarkerTransformCenterNode(state, centerMaterial)
    MarkerTransformationMatrixGlyph(state, matrixMaterial)
}

@Composable
private fun NodeScope.MarkerTrigonometryWorkspaceNode(
    materialLoader: MaterialLoader,
    state: ArViewerUiState
) {
    val axis = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.9f)) }
    val circle = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF53F2FF)) }
    val radius = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFA45CFF)) }
    val projection = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF58F2C2)) }
    val pointMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    MarkerFlatGrid(axis, circle, circle, pointMaterial)
    val r = 0.2f
    val radians = state.markerTrigAngleDegrees * PI.toFloat() / 180f
    val p = MarkerPlanePoint(cos(radians) * r, sin(radians) * r)
    val xFoot = MarkerPlanePoint(p.x, 0f)
    val samples = (0..96).map { index ->
        val a = index * 2f * PI.toFloat() / 96f
        MarkerPlanePoint(cos(a) * r, sin(a) * r)
    }
    samples.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a.toPosition(0.045f), b.toPosition(0.045f), circle, circle, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f)) }
    ThickLineRenderer(MarkerPlanePoint(0f, 0f).toPosition(0.07f), p.toPosition(0.07f), radius, radius, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(MarkerPlanePoint(0f, 0f).toPosition(0.052f), xFoot.toPosition(0.052f), projection, projection, ThickLineStyle(widthDp = 3.5f, glowWidthDp = 6f))
    ThickLineRenderer(xFoot.toPosition(0.052f), p.toPosition(0.052f), projection, projection, ThickLineStyle(widthDp = 3.5f, glowWidthDp = 6f))
    RadiantPointRenderer(p.toPosition(0.078f), pointMaterial, pointMaterial, pointMaterial, RadiantPointStyle(isSelected = true, isDraggable = true))
    val wave = (0..72).map { index ->
        val theta = index * 2f * PI.toFloat() / 72f
        MarkerPlanePoint(-0.27f + index * (0.54f / 72f), -0.27f + sin(theta) * 0.055f)
    }
    wave.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a.toPosition(0.042f), b.toPosition(0.042f), radius, radius, ThickLineStyle(widthDp = 4.5f, glowWidthDp = 7f)) }
    val markerX = -0.27f + (state.markerTrigAngleDegrees / 360f) * 0.54f
    ThickLineRenderer(Position(markerX, 0.045f, -0.34f), Position(markerX, 0.12f, -0.34f), projection, projection, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f))
}

@Composable
private fun NodeScope.MarkerMeasurementWorkspaceNode(
    materialLoader: MaterialLoader,
    state: ArViewerUiState
) {
    val axis = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.85f)) }
    val grid = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF70E6FF).copy(alpha = 0.2f)) }
    val shape = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF58F2C2)) }
    val measure = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val angle = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFA45CFF)) }
    MarkerFlatGrid(axis, grid, grid, measure)
    val a = MarkerPlanePoint(-0.18f, -0.1f)
    val b = MarkerPlanePoint(0.18f, -0.1f)
    val c = MarkerPlanePoint(0.04f, 0.15f)
    MarkerPolygon(listOf(a, b, c), shape, height = 0.055f)
    val foot = MarkerPlanePoint(c.x, a.y)
    ThickLineRenderer(c.toPosition(0.075f), foot.toPosition(0.075f), measure, measure, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
    ThickLineRenderer(a.toPosition(0.075f), b.toPosition(0.075f), measure, measure, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
    ThickLineRenderer(MarkerPlanePoint(-0.02f, -0.1f).toPosition(0.09f), MarkerPlanePoint(0.03f, -0.05f).toPosition(0.09f), angle, angle, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
    ThickLineRenderer(MarkerPlanePoint(0.03f, -0.05f).toPosition(0.09f), MarkerPlanePoint(0.08f, -0.1f).toPosition(0.09f), angle, angle, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
    listOf(a, b, c, foot).forEach {
        RadiantPointRenderer(it.toPosition(0.084f), measure, measure, measure, RadiantPointStyle(isDraggable = true))
    }
}

@Composable
private fun NodeScope.MarkerFlatGrid(
    axis: MaterialInstance,
    gridMinor: MaterialInstance,
    gridMajor: MaterialInstance,
    origin: MaterialInstance
) {
    val half = 0.32f
    (-8..8).forEach { i ->
        val p = i * (half / 8f)
        val material = if (i % 4 == 0) gridMajor else gridMinor
        ThickLineRenderer(Position(-half, 0.012f, p), Position(half, 0.012f, p), material, material, ThickLineStyle(widthDp = if (i % 4 == 0) 2f else 1f, glowWidthDp = if (i % 4 == 0) 3f else 1.4f))
        ThickLineRenderer(Position(p, 0.012f, -half), Position(p, 0.012f, half), material, material, ThickLineStyle(widthDp = if (i % 4 == 0) 2f else 1f, glowWidthDp = if (i % 4 == 0) 3f else 1.4f))
    }
    ThickLineRenderer(Position(-0.36f, 0.02f, 0f), Position(0.36f, 0.02f, 0f), axis, axis, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.02f, -0.36f), Position(0f, 0.02f, 0.36f), axis, axis, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    RadiantPointRenderer(Position(0f, 0.03f, 0f), origin, origin, origin)
}

@Composable
private fun NodeScope.MarkerPolygon(points: List<MarkerPlanePoint>, material: MaterialInstance, height: Float) {
    points.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a.toPosition(height), b.toPosition(height), material, material, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f)) }
    if (points.size > 2) ThickLineRenderer(points.last().toPosition(height), points.first().toPosition(height), material, material, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
}

@Composable
private fun NodeScope.MarkerShape(points: List<MarkerPlanePoint>, shape: MarkerTransformShape, material: MaterialInstance, height: Float) {
    when (shape) {
        MarkerTransformShape.Point -> {
            points.forEach { RadiantPointRenderer(it.toPosition(height), material, material, material, RadiantPointStyle(isSelected = true, isDraggable = true)) }
        }
        MarkerTransformShape.Circle -> {
            val center = points.firstOrNull() ?: MarkerPlanePoint(0f, 0f)
            val edge = points.getOrNull(1) ?: MarkerPlanePoint(0.12f, 0f)
            val radius = sqrt((center.x - edge.x) * (center.x - edge.x) + (center.y - edge.y) * (center.y - edge.y)).coerceAtLeast(0.02f)
            val ring = (0..72).map { index ->
                val angle = index * 2f * PI.toFloat() / 72f
                MarkerPlanePoint(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            }
            ring.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a.toPosition(height), b.toPosition(height), material, material, ThickLineStyle(widthDp = 4.5f, glowWidthDp = 7f)) }
            RadiantPointRenderer(center.toPosition(height + 0.01f), material, material, material, RadiantPointStyle(centreRadius = 0.008f, haloRadius = 0.017f))
        }
        else -> MarkerPolygon(points, material, height)
    }
}

@Composable
private fun NodeScope.MarkerTransformationMatrixGlyph(state: ArViewerUiState, material: MaterialInstance) {
    val baseX = 0.24f
    val baseZ = -0.29f
    val height = 0.055f
    val width = 0.09f
    val rows = 3
    val cols = 3
    (0..rows).forEach { row ->
        val z = baseZ + row * (width / rows)
        LineNode(Position(baseX, height, z), Position(baseX + width, height, z), material)
    }
    (0..cols).forEach { col ->
        val x = baseX + col * (width / cols)
        LineNode(Position(x, height, baseZ), Position(x, height, baseZ + width), material)
    }
    val pulse = 0.012f + state.markerTransformProgress * 0.018f
    RadiantPointRenderer(Position(baseX + width + 0.03f, height, baseZ + width), material, material, material, RadiantPointStyle(centreRadius = pulse * 0.42f, haloRadius = pulse))
}

@Composable
private fun NodeScope.MarkerTransformCenterNode(state: ArViewerUiState, material: MaterialInstance) {
    val center = when (state.markerTransformTool) {
        MarkerTransformationTool.Rotation,
        MarkerTransformationTool.Composite -> MarkerPlanePoint(state.markerRotationCenterX, state.markerRotationCenterY)
        MarkerTransformationTool.Dilation -> MarkerPlanePoint(state.markerDilationCenterX, state.markerDilationCenterY)
        else -> null
    } ?: return
    RadiantPointRenderer(center.toPosition(0.09f), material, material, material, RadiantPointStyle(isSelected = true))
    ThickLineRenderer(MarkerPlanePoint(center.x - 0.035f, center.y).toPosition(0.09f), MarkerPlanePoint(center.x + 0.035f, center.y).toPosition(0.09f), material, material, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f))
    ThickLineRenderer(MarkerPlanePoint(center.x, center.y - 0.035f).toPosition(0.09f), MarkerPlanePoint(center.x, center.y + 0.035f).toPosition(0.09f), material, material, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f))
}

private data class MarkerPlanePoint(val x: Float, val y: Float) {
    fun toPosition(height: Float): Position = Position(x, height, y)
    fun lerpTo(other: MarkerPlanePoint, progress: Float): MarkerPlanePoint =
        MarkerPlanePoint(
            x = x + (other.x - x) * progress.coerceIn(0f, 1f),
            y = y + (other.y - y) * progress.coerceIn(0f, 1f)
        )

    fun transformedBy(state: ArViewerUiState): MarkerPlanePoint = when (state.markerTransformTool) {
        MarkerTransformationTool.Translation -> MarkerPlanePoint(x + state.markerTranslationX, y + state.markerTranslationY)
        MarkerTransformationTool.Rotation -> rotateAround(
            MarkerPlanePoint(state.markerRotationCenterX, state.markerRotationCenterY),
            state.markerRotationDegrees * if (state.markerRotationClockwise) -1f else 1f
        )
        MarkerTransformationTool.Reflection -> reflectedBy(state.markerReflectionLine)
        MarkerTransformationTool.Dilation -> dilateFrom(MarkerPlanePoint(state.markerDilationCenterX, state.markerDilationCenterY), state.markerDilationScale)
        MarkerTransformationTool.HorizontalStretch -> MarkerPlanePoint(x * state.markerHorizontalStretch, y)
        MarkerTransformationTool.VerticalStretch -> MarkerPlanePoint(x, y * state.markerVerticalStretch)
        MarkerTransformationTool.Shear -> MarkerPlanePoint(x + state.markerShear * y, y)
        MarkerTransformationTool.Composite -> applyCompositeSequence(state)
    }

    fun rotateBy(degrees: Float): MarkerPlanePoint {
        val radians = degrees * PI.toFloat() / 180f
        return MarkerPlanePoint(x * cos(radians) - y * sin(radians), x * sin(radians) + y * cos(radians))
    }

    private fun rotateAround(center: MarkerPlanePoint, degrees: Float): MarkerPlanePoint {
        val shifted = MarkerPlanePoint(x - center.x, y - center.y).rotateBy(degrees)
        return MarkerPlanePoint(shifted.x + center.x, shifted.y + center.y)
    }

    private fun dilateFrom(center: MarkerPlanePoint, scale: Float): MarkerPlanePoint =
        MarkerPlanePoint(center.x + (x - center.x) * scale, center.y + (y - center.y) * scale)

    fun reflectedBy(line: MarkerReflectionLine): MarkerPlanePoint = when (line) {
        MarkerReflectionLine.XAxis -> MarkerPlanePoint(x, -y)
        MarkerReflectionLine.YAxis -> MarkerPlanePoint(-x, y)
        MarkerReflectionLine.YEqualsX -> MarkerPlanePoint(y, x)
        MarkerReflectionLine.YEqualsNegativeX -> MarkerPlanePoint(-y, -x)
        MarkerReflectionLine.UserLine -> reflectAcrossLine(angleRadians = atan2(0.5f, 1f))
    }

    private fun applyCompositeSequence(state: ArViewerUiState): MarkerPlanePoint {
        val steps = state.markerTransformationSequence.ifEmpty {
            listOf(
                MarkerTransformationStepState(MarkerTransformSequenceAction.Rotate90, "Rotate 90 deg"),
                MarkerTransformationStepState(MarkerTransformSequenceAction.Translate32, "Translate (3, 2)"),
                MarkerTransformationStepState(MarkerTransformSequenceAction.ReflectYAxis, "Reflect Y-axis")
            )
        }
        return steps.fold(this) { point, step ->
            when (step.action) {
                MarkerTransformSequenceAction.Rotate90 -> point.rotateBy(90f)
                MarkerTransformSequenceAction.Translate32 -> MarkerPlanePoint(point.x + 0.18f, point.y + 0.12f)
                MarkerTransformSequenceAction.ReflectYAxis -> point.reflectedBy(MarkerReflectionLine.YAxis)
            }
        }
    }

    private fun reflectAcrossLine(angleRadians: Float): MarkerPlanePoint {
        val rotated = rotateBy(-angleRadians * 180f / PI.toFloat())
        val reflected = MarkerPlanePoint(rotated.x, -rotated.y)
        return reflected.rotateBy(angleRadians * 180f / PI.toFloat())
    }
}

private fun markerTransformShapePoints(shape: MarkerTransformShape): List<MarkerPlanePoint> = when (shape) {
    MarkerTransformShape.Point -> listOf(MarkerPlanePoint(-0.04f, 0.04f))
    MarkerTransformShape.Segment -> listOf(MarkerPlanePoint(-0.16f, -0.08f), MarkerPlanePoint(0.16f, 0.08f))
    MarkerTransformShape.Triangle -> listOf(MarkerPlanePoint(-0.13f, -0.1f), MarkerPlanePoint(0.13f, -0.08f), MarkerPlanePoint(-0.02f, 0.13f))
    MarkerTransformShape.Square -> listOf(MarkerPlanePoint(-0.12f, -0.12f), MarkerPlanePoint(0.12f, -0.12f), MarkerPlanePoint(0.12f, 0.12f), MarkerPlanePoint(-0.12f, 0.12f))
    MarkerTransformShape.Rectangle -> listOf(MarkerPlanePoint(-0.18f, -0.1f), MarkerPlanePoint(0.18f, -0.1f), MarkerPlanePoint(0.18f, 0.1f), MarkerPlanePoint(-0.18f, 0.1f))
    MarkerTransformShape.Polygon -> listOf(MarkerPlanePoint(-0.15f, -0.08f), MarkerPlanePoint(-0.04f, -0.15f), MarkerPlanePoint(0.14f, -0.07f), MarkerPlanePoint(0.11f, 0.12f), MarkerPlanePoint(-0.1f, 0.14f))
    MarkerTransformShape.Circle -> listOf(MarkerPlanePoint(0f, 0f), MarkerPlanePoint(0.13f, 0f))
}

private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size

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
    if (mode == ArEngineMode.PaperGraph) return null
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

    val depthAllowed = mode !in setOf(ArEngineMode.Indoor, ArEngineMode.SurfacePlacement)
    val depthHit = trackedHits.firstOrNull { result ->
        (result.trackable as? DepthPoint)?.trackingState == TrackingState.TRACKING
    }?.takeIf { depthAllowed }?.let { PlacementCandidate(it, PlacementHitKind.DepthPoint) }
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

private fun Frame.findLockedMarkerImage(): AugmentedImage? =
    getUpdatedTrackables(AugmentedImage::class.java)
        .filter { image ->
            image.trackingState == TrackingState.TRACKING &&
                image.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING
        }
        .maxByOrNull { image -> image.extentX.coerceAtLeast(0f) * image.extentZ.coerceAtLeast(0f) }

private fun Frame.markerLocalPointFromScreen(
    image: AugmentedImage?,
    screenX: Float,
    screenY: Float,
    viewportWidth: Int,
    viewportHeight: Int
): Pair<Float, Float> {
    val width = viewportWidth.coerceAtLeast(1).toFloat()
    val height = viewportHeight.coerceAtLeast(1).toFloat()
    val fallback = Pair(
        ((screenX / width) - 0.5f) * MARKER_SAFE_HALF_EXTENT_METERS * 2f,
        (0.5f - (screenY / height)) * MARKER_SAFE_HALF_EXTENT_METERS * 2f
    )
    image ?: return fallback

    val halfX = (image.extentX.takeIf { it.isFinite() && it > 0.02f } ?: G01_MARKER_WIDTH_METERS) * 0.5f
    val halfZ = (image.extentZ.takeIf { it.isFinite() && it > 0.02f } ?: G01_MARKER_WIDTH_METERS) * 0.5f
    val pose = image.centerPose
    val center = projectWorldPoint(pose.translation, width, height) ?: return fallback
    val xPoint = projectWorldPoint(pose.transformPoint(floatArrayOf(halfX, 0f, 0f)), width, height) ?: return fallback
    val zPoint = projectWorldPoint(pose.transformPoint(floatArrayOf(0f, 0f, halfZ)), width, height) ?: return fallback
    val basisX = xPoint.first - center.first
    val basisY = xPoint.second - center.second
    val depthX = zPoint.first - center.first
    val depthY = zPoint.second - center.second
    val determinant = basisX * depthY - basisY * depthX
    if (!determinant.isFinite() || kotlin.math.abs(determinant) < 0.001f) return fallback

    val dx = screenX - center.first
    val dy = screenY - center.second
    val markerX = ((dx * depthY - dy * depthX) / determinant) * halfX
    val markerZ = ((basisX * dy - basisY * dx) / determinant) * halfZ
    val safeX = minOf(MARKER_SAFE_HALF_EXTENT_METERS, halfX * 0.78f)
    val safeZ = minOf(MARKER_SAFE_HALF_EXTENT_METERS, halfZ * 0.78f)
    return Pair(markerX.coerceIn(-safeX, safeX), markerZ.coerceIn(-safeZ, safeZ))
}

private fun Frame.projectWorldPoint(
    worldPoint: FloatArray,
    viewportWidth: Float,
    viewportHeight: Float
): Pair<Float, Float>? {
    val view = FloatArray(16)
    val projection = FloatArray(16)
    val viewProjection = FloatArray(16)
    val clip = FloatArray(4)
    camera.getViewMatrix(view, 0)
    camera.getProjectionMatrix(projection, 0, 0.05f, 25f)
    Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
    Matrix.multiplyMV(clip, 0, viewProjection, 0, floatArrayOf(worldPoint[0], worldPoint[1], worldPoint[2], 1f), 0)
    val w = clip[3]
    if (!w.isFinite() || w <= 0.0001f) return null
    val ndcX = clip[0] / w
    val ndcY = clip[1] / w
    if (!ndcX.isFinite() || !ndcY.isFinite()) return null
    return Pair((ndcX + 1f) * 0.5f * viewportWidth, (1f - ndcY) * 0.5f * viewportHeight)
}

private fun String.extractArMarkerId(): String? =
    Regex("""[Gg]01""")
        .find(this)
        ?.value
        ?.uppercase()

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

private fun ArEngineMode.toCommonSurfaceMode(): CommonArSurfaceMode = when (this) {
    ArEngineMode.SurfacePlacement -> CommonArSurfaceMode.SurfaceOnly
    ArEngineMode.AirPlacement -> CommonArSurfaceMode.AirPlacement
    ArEngineMode.PaperGraph -> CommonArSurfaceMode.PaperGraph
    ArEngineMode.OutdoorGeospatialMath -> CommonArSurfaceMode.OutdoorGeospatial
    ArEngineMode.Indoor -> CommonArSurfaceMode.Markerless
}

private fun CommonArSurfaceState.toPlacementHitKind(): PlacementHitKind = when (hitKind) {
    CommonArSurfaceHitKind.Plane -> PlacementHitKind.Plane
    CommonArSurfaceHitKind.DepthPoint -> PlacementHitKind.DepthPoint
    CommonArSurfaceHitKind.FeaturePoint -> PlacementHitKind.FeaturePoint
    CommonArSurfaceHitKind.InstantPreview -> PlacementHitKind.Instant
    CommonArSurfaceHitKind.StreetscapeGeometry -> PlacementHitKind.StreetscapeGeometry
    CommonArSurfaceHitKind.None -> PlacementHitKind.None
}

private const val APPROXIMATE_PLACEMENT_DISTANCE_METERS = 0.8f

@Composable
private fun NodeScope.SelectionHighlight(materialLoader: MaterialLoader) {
    val material = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFF176)) }
    BoxEdges(material, 0.2f, 0.28f, 0.2f)
}

@Composable
private fun NodeScope.RadiantPointRenderer(
    center: Position,
    materialInstance: MaterialInstance,
    glowMaterialInstance: MaterialInstance,
    haloMaterialInstance: MaterialInstance,
    style: RadiantPointStyle = RadiantPointStyle()
) {
    val selectedBoost = if (style.isSelected) 1.28f else 1f
    val draggableBoost = if (style.isDraggable) 1.16f else 1f
    val lockedScale = if (style.isLocked) 0.92f else 1f
    val invalidBoost = if (style.isInvalid) 1.18f else 1f
    val centre = style.centreRadius * selectedBoost * draggableBoost * lockedScale
    val halo = style.haloRadius * selectedBoost * draggableBoost * invalidBoost
    SphereNode(
        radius = halo * style.glowIntensity.coerceIn(0.6f, 1.4f),
        center = center,
        stacks = 16,
        slices = 24,
        materialInstance = glowMaterialInstance
    )
    SphereNode(
        radius = halo * 0.74f,
        center = center.copy(y = center.y + 0.0008f),
        stacks = 12,
        slices = 20,
        materialInstance = haloMaterialInstance
    )
    SphereNode(
        radius = centre,
        center = center.copy(y = center.y + 0.0016f),
        stacks = 16,
        slices = 24,
        materialInstance = materialInstance
    )
    if (style.isSelected || style.isDraggable || style.isInvalid) {
        SphereNode(
            radius = halo * 1.18f,
            center = center.copy(y = center.y + 0.0024f),
            stacks = 10,
            slices = 18,
            materialInstance = haloMaterialInstance
        )
    }
}

@Composable
private fun NodeScope.ThickLineRenderer(
    start: Position,
    end: Position,
    materialInstance: MaterialInstance,
    glowMaterialInstance: MaterialInstance,
    style: ThickLineStyle = ThickLineStyle()
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val dz = end.z - start.z
    val planarLength = sqrt(dx * dx + dz * dz)
    val lineWidth = style.widthDp.dpToMarkerMeters() * if (style.isSelected) 1.55f else 1f
    val glowWidth = style.glowWidthDp.dpToMarkerMeters() * if (style.isSelected) 1.35f else 1f
    if (planarLength > 0.001f && abs(dy) < 0.04f) {
        val center = Position((start.x + end.x) / 2f, max(start.y, end.y), (start.z + end.z) / 2f)
        val yaw = Math.toDegrees(atan2(-dz.toDouble(), dx.toDouble())).toFloat()
        if (!style.isDashed) {
            CubeNode(
                size = Size(planarLength, 0.004f, glowWidth),
                center = center.copy(y = center.y - 0.0008f),
                materialInstance = glowMaterialInstance,
                rotation = Rotation(y = yaw)
            )
            CubeNode(
                size = Size(planarLength, 0.006f, lineWidth),
                center = center.copy(y = center.y + 0.0012f),
                materialInstance = materialInstance,
                rotation = Rotation(y = yaw)
            )
        } else {
            val segments = max(3, (planarLength / 0.035f).toInt())
            (0 until segments step 2).forEach { index ->
                val t0 = index / segments.toFloat()
                val t1 = ((index + 1).coerceAtMost(segments)) / segments.toFloat()
                val a = start.lerpTo(end, t0)
                val b = start.lerpTo(end, t1)
                ThickLineRenderer(a, b, materialInstance, glowMaterialInstance, style.copy(isDashed = false, glowWidthDp = style.glowWidthDp * 0.75f))
            }
        }
    } else {
        LineNode(start, end, glowMaterialInstance)
        LineNode(start.copy(y = start.y + 0.001f), end.copy(y = end.y + 0.001f), materialInstance)
    }
}

@Composable
private fun NodeScope.GestureHandleNodes(materialLoader: MaterialLoader) {
    val rotate = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val scale = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF3DFF9F)) }
    val lift = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF42A5F5)) }
    val glow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.22f)) }
    RadiantPointRenderer(Position(0.22f, 0.12f, 0f), rotate, glow, rotate, RadiantPointStyle(isDraggable = true))
    RadiantPointRenderer(Position(0f, 0.28f, 0f), lift, glow, lift, RadiantPointStyle(isDraggable = true))
    RadiantPointRenderer(Position(-0.22f, 0.12f, 0f), scale, glow, scale, RadiantPointStyle(isDraggable = true))
    ThickLineRenderer(Position(0f, 0.12f, 0f), Position(0.22f, 0.12f, 0f), rotate, glow, ThickLineStyle(widthDp = 4f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.12f, 0f), Position(0f, 0.28f, 0f), lift, glow, ThickLineStyle(widthDp = 4f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.12f, 0f), Position(-0.22f, 0.12f, 0f), scale, glow, ThickLineStyle(widthDp = 4f, glowWidthDp = 8f))
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
    if (MathArFeature.MultiObjectConstraints !in state.enabledMathArFeatures && state.resolvedConstructions.isEmpty() && state.marker2dDraftPoints.isEmpty()) return
    val pointMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFECFFFF)) }
    val lineMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF28E9FF)) }
    val constraintMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF42FFD9)) }
    val selectedMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFC34DFF)) }
    val draftMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFF42FFD9)) }
    val pointGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x4428E9FF)) }
    val lineGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x3828E9FF)) }
    val selectedGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x55C34DFF)) }
    val constraintGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x4442FFD9)) }
    if (state.marker2dShowVertices) state.constructionGeometry.points.forEach { point ->
        RadiantPointRenderer(
            center = point.position.toPosition().copy(y = point.position.y.toFloat() + 0.006f),
            materialInstance = pointMaterial,
            glowMaterialInstance = pointGlow,
            haloMaterialInstance = pointMaterial,
            style = RadiantPointStyle(centreRadius = 0.0032f, haloRadius = 0.0065f, isDraggable = true)
        )
    }
    if (state.marker2dShowConstructionLines) state.resolvedConstructions.filter { it.visible }.forEach { construction ->
        val material = when {
            construction.id == state.marker2dSelectedObjectId -> selectedMaterial
            construction.kind in setOf(ConstructionObjectKind.Parallel, ConstructionObjectKind.Perpendicular) -> constraintMaterial
            else -> lineMaterial
        }
        when (construction.kind) {
            ConstructionObjectKind.Line,
            ConstructionObjectKind.Segment,
            ConstructionObjectKind.Ray,
            ConstructionObjectKind.Vector,
            ConstructionObjectKind.Parallel,
            ConstructionObjectKind.Perpendicular -> {
                if (construction.points.size >= 2) {
                    ThickLineRenderer(
                        construction.points[0].toPosition(),
                        construction.points[1].toPosition(),
                        material,
                        if (construction.id == state.marker2dSelectedObjectId) selectedGlow else lineGlow,
                        ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f, isSelected = construction.id == state.marker2dSelectedObjectId)
                    )
                    if (construction.kind in setOf(ConstructionObjectKind.Vector, ConstructionObjectKind.Ray)) {
                        VectorHead(construction.points[0].toPosition(), construction.points[1].toPosition(), material)
                    }
                }
            }
            ConstructionObjectKind.Plane,
            ConstructionObjectKind.Polygon -> {
                construction.points.zipWithNext().forEach { (a, b) ->
                    ThickLineRenderer(a.toPosition(), b.toPosition(), material, if (construction.id == state.marker2dSelectedObjectId) selectedGlow else lineGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f, isSelected = construction.id == state.marker2dSelectedObjectId))
                }
                if (construction.points.size > 2) ThickLineRenderer(construction.points.last().toPosition(), construction.points.first().toPosition(), material, if (construction.id == state.marker2dSelectedObjectId) selectedGlow else lineGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f, isSelected = construction.id == state.marker2dSelectedObjectId))
            }
            ConstructionObjectKind.Circle -> {
                if (construction.points.size >= 2) {
                    val center = construction.points[0]
                    val edge = construction.points[1]
                    val radius = sqrt((center.x - edge.x) * (center.x - edge.x) + (center.z - edge.z) * (center.z - edge.z)).toFloat()
                    val ring = (0..32).map { index ->
                        val angle = index * (2f * PI.toFloat() / 32f)
                        Position(center.x.toFloat() + cos(angle) * radius, center.y.toFloat() + 0.006f, center.z.toFloat() + sin(angle) * radius)
                    }
                    ring.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a, b, material, lineGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f)) }
                }
            }
            ConstructionObjectKind.Midpoint,
            ConstructionObjectKind.Point,
            ConstructionObjectKind.Intersection -> {
                construction.points.forEach { point ->
                    RadiantPointRenderer(
                        center = point.toPosition().copy(y = point.y.toFloat() + 0.006f),
                        materialInstance = constraintMaterial,
                        glowMaterialInstance = constraintGlow,
                        haloMaterialInstance = constraintMaterial,
                        style = RadiantPointStyle(centreRadius = 0.0032f, haloRadius = 0.0065f, isSelected = construction.id == state.marker2dSelectedObjectId)
                    )
                }
            }
        }
    }
    val draft = state.marker2dDraftPoints.map { Vector3Value(it.x.toDouble(), 0.0, it.y.toDouble()).toPosition() }
    draft.forEach { point ->
        RadiantPointRenderer(
            center = point.copy(y = point.y + 0.007f),
            materialInstance = draftMaterial,
            glowMaterialInstance = pointGlow,
            haloMaterialInstance = draftMaterial,
            style = RadiantPointStyle(centreRadius = 0.0032f, haloRadius = 0.0065f, isSelected = true, isDraggable = true)
        )
    }
    draft.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a, b, draftMaterial, pointGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f, isDashed = true)) }
    if (state.marker2dActiveTool in setOf(Marker2dShapeTool.Polygon, Marker2dShapeTool.Triangle, Marker2dShapeTool.Square, Marker2dShapeTool.Rectangle, Marker2dShapeTool.RegularPolygon) && draft.size > 2) {
        ThickLineRenderer(draft.last(), draft.first(), draftMaterial, pointGlow, ThickLineStyle(widthDp = 2f, glowWidthDp = 3.2f, isDashed = true))
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
    ThickLineRenderer(end, left, material, material, ThickLineStyle(widthDp = 3.5f, glowWidthDp = 6f))
    ThickLineRenderer(end, right, material, material, ThickLineStyle(widthDp = 3.5f, glowWidthDp = 6f))
}

private fun Vector3Value.toPosition(): Position = Position(x.toFloat(), y.toFloat() + 0.018f, z.toFloat())

private fun Float.dpToMarkerMeters(): Float = this * 0.0017f

private fun Position.lerpTo(other: Position, t: Float): Position =
    Position(
        x = x + (other.x - x) * t,
        y = y + (other.y - y) * t,
        z = z + (other.z - z) * t
    )

@Composable
private fun NodeScope.CoordinatePlaneNode(lineMaterial: com.google.android.filament.MaterialInstance, pointMaterial: com.google.android.filament.MaterialInstance) {
    val range = -5..5
    range.forEach { i ->
        val p = i * 0.04f
        ThickLineRenderer(Position(-0.22f, 0.002f, p), Position(0.22f, 0.002f, p), lineMaterial, lineMaterial, ThickLineStyle(widthDp = 1.2f, glowWidthDp = 2f))
        ThickLineRenderer(Position(p, 0.002f, -0.22f), Position(p, 0.002f, 0.22f), lineMaterial, lineMaterial, ThickLineStyle(widthDp = 1.2f, glowWidthDp = 2f))
    }
    ThickLineRenderer(Position(-0.25f, 0.006f, 0f), Position(0.25f, 0.006f, 0f), pointMaterial, pointMaterial, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.006f, -0.25f), Position(0f, 0.006f, 0.25f), pointMaterial, pointMaterial, ThickLineStyle(widthDp = 5f, glowWidthDp = 8f))
    RadiantPointRenderer(Position(0.08f, 0.018f, 0.08f), pointMaterial, pointMaterial, pointMaterial)
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
            RadiantPointRenderer(
                Position(graphX(state, point.x.toFloat()), 0.028f, point.y.toFloat().coerceIn(-1.6f, 1.6f) * 0.08f),
                accent,
                accent,
                accent,
                RadiantPointStyle(isSelected = true)
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
        RadiantPointRenderer(Position(x, 0.03f, z), accent, accent, accent, RadiantPointStyle(isSelected = true, isDraggable = true))
        ThickLineRenderer(Position(x - 0.08f, 0.026f, z - slope * 0.08f), Position(x + 0.08f, 0.026f, z + slope * 0.08f), accent, accent, ThickLineStyle(widthDp = 4.5f, glowWidthDp = 7f))
        ThickLineRenderer(Position(x - 0.045f, 0.026f, z - normalSlope * 0.045f), Position(x + 0.045f, 0.026f, z + normalSlope * 0.045f), lineMaterial, lineMaterial, ThickLineStyle(widthDp = 3.5f, glowWidthDp = 5f))
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
        RadiantPointRenderer(Position(0f, 0.025f, 0f), accent, accent, accent, RadiantPointStyle(isSelected = true))
    }
    if (calibration.xAxisLocked) {
        RadiantPointRenderer(Position(0.12f, 0.025f, 0f), accent, accent, accent)
        ThickLineRenderer(Position(0f, 0.018f, 0f), Position(0.12f, 0.018f, 0f), accent, accent, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
    }
    if (calibration.yAxisLocked) {
        RadiantPointRenderer(Position(0f, 0.025f, 0.09f), accent, accent, accent)
        ThickLineRenderer(Position(0f, 0.018f, 0f), Position(0f, 0.018f, 0.09f), accent, accent, ThickLineStyle(widthDp = 4f, glowWidthDp = 7f))
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
    RadiantPointRenderer(Position(graphX(state, x0), f0, 0f), accent, accent, accent, RadiantPointStyle(isSelected = true))
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
private fun NodeScope.MarkerCoordinateWorkspaceNode(materialLoader: MaterialLoader, state: ArViewerUiState) {
    val axis = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color.White.copy(alpha = 0.88f)) }
    val gridMinor = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathCyan.copy(alpha = 0.26f)) }
    val gridMajor = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathCyan.copy(alpha = 0.46f)) }
    val pointMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFF176)) }
    val selectedMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathViolet) }
    val lineMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathMint) }
    val helperMaterial = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0xFFFFC857)) }
    val pointGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x66FFF176)) }
    val selectedGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathViolet.copy(alpha = 0.42f)) }
    val lineGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(arMathMint.copy(alpha = 0.28f)) }
    val helperGlow = remember(materialLoader) { materialLoader.createUnlitColorInstance(Color(0x66FFC857)) }
    MarkerCoordinateGrid(axis, gridMinor, gridMajor)
    val pointMap = state.markerCoordinatePoints.associateBy { it.id }
    state.markerCoordinateShapes.filter { it.visible }.forEach { shape ->
        val points = shape.pointIds.mapNotNull { pointMap[it] }
        MarkerCoordinateShapeNode(
            shape,
            points,
            if (shape.id == state.markerCoordinateSelectedShapeId) selectedMaterial else lineMaterial,
            if (shape.id == state.markerCoordinateSelectedShapeId) selectedGlow else lineGlow,
            shape.id == state.markerCoordinateSelectedShapeId
        )
    }
    val selected = state.markerCoordinateSelectedPointIds.mapNotNull { pointMap[it] }
    if (state.markerCoordinateShowSlopeTriangle && selected.size >= 2) {
        val a = selected[selected.size - 2]
        val b = selected.last()
        val corner = MarkerCoordinatePointState("corner", "", b.x, a.y)
        ThickLineRenderer(a.coordPosition(0.035f), corner.coordPosition(0.035f), helperMaterial, helperGlow, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f, isDashed = true))
        ThickLineRenderer(corner.coordPosition(0.035f), b.coordPosition(0.035f), helperMaterial, helperGlow, ThickLineStyle(widthDp = 3f, glowWidthDp = 5f, isDashed = true))
        RadiantPointRenderer(corner.coordPosition(0.045f), helperMaterial, helperGlow, helperMaterial, RadiantPointStyle(centreRadius = 0.008f, haloRadius = 0.018f))
        val intercepts = coordinateLineAnalysis(a, b)
        intercepts.xIntercept?.let { RadiantPointRenderer(MarkerCoordinatePointState("x", "", it, 0f).coordPosition(0.045f), helperMaterial, helperGlow, helperMaterial, RadiantPointStyle(centreRadius = 0.008f, haloRadius = 0.018f)) }
        intercepts.yIntercept?.let { RadiantPointRenderer(MarkerCoordinatePointState("y", "", 0f, it).coordPosition(0.045f), helperMaterial, helperGlow, helperMaterial, RadiantPointStyle(centreRadius = 0.008f, haloRadius = 0.018f)) }
    }
    state.markerCoordinatePoints.forEach { point ->
        val selectedPoint = point.id in state.markerCoordinateSelectedPointIds
        RadiantPointRenderer(
            center = point.coordPosition(0.055f),
            materialInstance = if (selectedPoint) selectedMaterial else pointMaterial,
            glowMaterialInstance = if (selectedPoint) selectedGlow else pointGlow,
            haloMaterialInstance = if (selectedPoint) selectedMaterial else pointMaterial,
            style = RadiantPointStyle(isSelected = selectedPoint, isDraggable = selectedPoint)
        )
    }
}

@Composable
private fun NodeScope.MarkerCoordinateGrid(axis: MaterialInstance, gridMinor: MaterialInstance, gridMajor: MaterialInstance) {
    (-8..8).forEach { i ->
        val p = i * MARKER_COORDINATE_UNIT_UI
        val material = if (i % 2 == 0) gridMajor else gridMinor
        ThickLineRenderer(Position(-8 * MARKER_COORDINATE_UNIT_UI, 0.014f, p), Position(8 * MARKER_COORDINATE_UNIT_UI, 0.014f, p), material, material, ThickLineStyle(widthDp = if (i % 2 == 0) 2.2f else 1.1f, glowWidthDp = if (i % 2 == 0) 3.2f else 1.6f, opacity = 0.55f))
        ThickLineRenderer(Position(p, 0.014f, -8 * MARKER_COORDINATE_UNIT_UI), Position(p, 0.014f, 8 * MARKER_COORDINATE_UNIT_UI), material, material, ThickLineStyle(widthDp = if (i % 2 == 0) 2.2f else 1.1f, glowWidthDp = if (i % 2 == 0) 3.2f else 1.6f, opacity = 0.55f))
    }
    ThickLineRenderer(Position(-0.36f, 0.02f, 0f), Position(0.36f, 0.02f, 0f), axis, axis, ThickLineStyle(widthDp = 5.5f, glowWidthDp = 8f))
    ThickLineRenderer(Position(0f, 0.02f, -0.36f), Position(0f, 0.02f, 0.36f), axis, axis, ThickLineStyle(widthDp = 5.5f, glowWidthDp = 8f))
}

@Composable
private fun NodeScope.MarkerCoordinateShapeNode(shape: MarkerCoordinateShapeState, points: List<MarkerCoordinatePointState>, material: MaterialInstance, glow: MaterialInstance, selected: Boolean) {
    when (shape.kind) {
        MarkerCoordinateShapeKind.Line,
        MarkerCoordinateShapeKind.Parallel,
        MarkerCoordinateShapeKind.Perpendicular -> {
            if (points.size >= 2) {
                val (start, end) = extendedCoordinateLine(points[0], points[1])
                ThickLineRenderer(start.coordPosition(0.032f), end.coordPosition(0.032f), material, glow, ThickLineStyle(widthDp = 5f, glowWidthDp = 9f, isSelected = selected))
            }
        }
        MarkerCoordinateShapeKind.Segment -> if (points.size >= 2) ThickLineRenderer(points[0].coordPosition(0.032f), points[1].coordPosition(0.032f), material, glow, ThickLineStyle(widthDp = 5f, glowWidthDp = 9f, isSelected = selected))
        MarkerCoordinateShapeKind.Triangle,
        MarkerCoordinateShapeKind.Polygon -> {
            points.zipWithNext().forEach { (a, b) -> ThickLineRenderer(a.coordPosition(0.032f), b.coordPosition(0.032f), material, glow, ThickLineStyle(widthDp = 5f, glowWidthDp = 9f, isSelected = selected)) }
            if (points.size > 2) ThickLineRenderer(points.last().coordPosition(0.032f), points.first().coordPosition(0.032f), material, glow, ThickLineStyle(widthDp = 5f, glowWidthDp = 9f, isSelected = selected))
        }
    }
}

private const val MARKER_COORDINATE_UNIT_UI = 0.04f

private fun MarkerCoordinatePointState.coordPosition(height: Float = 0.03f): Position =
    Position(x * MARKER_COORDINATE_UNIT_UI, height, y * MARKER_COORDINATE_UNIT_UI)

private fun extendedCoordinateLine(a: MarkerCoordinatePointState, b: MarkerCoordinatePointState): Pair<MarkerCoordinatePointState, MarkerCoordinatePointState> {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val length = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val ux = dx / length
    val uy = dy / length
    return MarkerCoordinatePointState("l1", "", a.x - ux * 9f, a.y - uy * 9f) to MarkerCoordinatePointState("l2", "", a.x + ux * 9f, a.y + uy * 9f)
}

private data class CoordinateLineAnalysis(val slope: Float?, val yIntercept: Float?, val xIntercept: Float?, val equation: String)

private fun coordinateLineAnalysis(a: MarkerCoordinatePointState, b: MarkerCoordinatePointState): CoordinateLineAnalysis {
    val dx = b.x - a.x
    val dy = b.y - a.y
    if (abs(dx) < 1e-4f) {
        return CoordinateLineAnalysis(null, null, a.x, "x = ${a.x.coordinateText(false)}")
    }
    val slope = dy / dx
    val intercept = a.y - slope * a.x
    val xIntercept = if (abs(slope) < 1e-4f) null else -intercept / slope
    val sign = if (intercept >= 0f) "+" else "-"
    return CoordinateLineAnalysis(slope, intercept, xIntercept, "y = ${slope.coordinateText(false)}x $sign ${abs(intercept).coordinateText(false)}")
}

@Composable
private fun NodeScope.SphereWire(edge: MaterialInstance, radius: Float) {
    CircleLines(radius, radius, edge, y = radius)
    (0..24).forEach { i ->
        val a = i * (2f * PI.toFloat() / 24f)
        LineNode(Position(cos(a) * radius, radius, sin(a) * radius), Position(cos(a) * radius * 0.35f, radius * 2f, sin(a) * radius * 0.35f), edge)
        LineNode(Position(cos(a) * radius, radius, sin(a) * radius), Position(cos(a) * radius * 0.35f, 0f, sin(a) * radius * 0.35f), edge)
    }
    CircleLines(radius * 0.7f, radius * 0.7f, edge, y = radius * 1.45f)
    CircleLines(radius * 0.7f, radius * 0.7f, edge, y = radius * 0.55f)
}

@Composable
private fun NodeScope.HemisphereWire(edge: MaterialInstance, radius: Float) {
    CircleLines(radius, radius, edge, y = 0f)
    (0..12).forEach { i ->
        val a = i * (2f * PI.toFloat() / 12f)
        LineNode(Position(cos(a) * radius, 0f, sin(a) * radius), Position(0f, radius, 0f), edge)
    }
    CircleLines(radius * 0.7f, radius * 0.7f, edge, y = radius * 0.5f)
}

@Composable
private fun NodeScope.CylinderWire(edge: MaterialInstance, radius: Float, height: Float) {
    CircleLines(radius, radius, edge, y = 0f)
    CircleLines(radius, radius, edge, y = height)
    (0 until 16).forEach { i ->
        val a = i * (2f * PI.toFloat() / 16f)
        LineNode(Position(cos(a) * radius, 0f, sin(a) * radius), Position(cos(a) * radius, height, sin(a) * radius), edge)
    }
}

@Composable
private fun NodeScope.ConeWire(edge: MaterialInstance, radius: Float, height: Float) {
    CircleLines(radius, radius, edge, y = 0f)
    (0 until 16 step 2).forEach { i ->
        val a = i * (2f * PI.toFloat() / 16f)
        LineNode(Position(cos(a) * radius, 0f, sin(a) * radius), Position(0f, height, 0f), edge)
    }
}

@Composable
private fun NodeScope.PyramidWire(edge: MaterialInstance, halfX: Float, halfZ: Float, height: Float, exploded: Boolean) {
    val explode = if (exploded) 0.035f else 0f
    val corners = listOf(
        Position(-halfX - explode, 0f, -halfZ - explode),
        Position(halfX + explode, 0f, -halfZ - explode),
        Position(halfX + explode, 0f, halfZ + explode),
        Position(-halfX - explode, 0f, halfZ + explode)
    )
    corners.zipWithNext().forEach { (a, b) -> LineNode(a, b, edge) }
    LineNode(corners.last(), corners.first(), edge)
    val apex = Position(0f, height + explode, 0f)
    corners.forEach { LineNode(it, apex, edge) }
}

@Composable
private fun NodeScope.TriangularPrismWire(edge: MaterialInstance, width: Float, height: Float, depth: Float) {
    val left = -depth / 2f
    val right = depth / 2f
    val triA = listOf(Position(-width / 2f, 0f, left), Position(width / 2f, 0f, left), Position(0f, height, left))
    val triB = listOf(Position(-width / 2f, 0f, right), Position(width / 2f, 0f, right), Position(0f, height, right))
    triA.zipWithNext().forEach { (a, b) -> LineNode(a, b, edge) }
    LineNode(triA.last(), triA.first(), edge)
    triB.zipWithNext().forEach { (a, b) -> LineNode(a, b, edge) }
    LineNode(triB.last(), triB.first(), edge)
    triA.zip(triB).forEach { (a, b) -> LineNode(a, b, edge) }
}

@Composable
private fun NodeScope.TetrahedronWire(edge: MaterialInstance, side: Float) {
    val h = side * 0.82f
    val a = Position(-side / 2f, 0f, -side / 3f)
    val b = Position(side / 2f, 0f, -side / 3f)
    val c = Position(0f, 0f, side / 2f)
    val d = Position(0f, h, 0f)
    listOf(a to b, b to c, c to a, a to d, b to d, c to d).forEach { (p, q) -> LineNode(p, q, edge) }
}

@Composable
private fun NodeScope.TorusWire(edge: MaterialInstance, majorRadius: Float, tubeRadius: Float) {
    CircleLines(majorRadius, majorRadius, edge, y = tubeRadius)
    CircleLines(majorRadius + tubeRadius, majorRadius + tubeRadius, edge, y = tubeRadius)
    CircleLines((majorRadius - tubeRadius).coerceAtLeast(0.02f), (majorRadius - tubeRadius).coerceAtLeast(0.02f), edge, y = tubeRadius)
    (0 until 12).forEach { i ->
        val a = i * (2f * PI.toFloat() / 12f)
        val cx = cos(a) * majorRadius
        val cz = sin(a) * majorRadius
        LineNode(Position(cx - cos(a) * tubeRadius, tubeRadius, cz - sin(a) * tubeRadius), Position(cx + cos(a) * tubeRadius, tubeRadius, cz + sin(a) * tubeRadius), edge)
    }
}

@Composable
private fun NodeScope.FrustumWire(edge: MaterialInstance, bottomRadius: Float, topRadius: Float, height: Float) {
    CircleLines(bottomRadius, bottomRadius, edge, y = 0f)
    CircleLines(topRadius, topRadius, edge, y = height)
    (0 until 16 step 2).forEach { i ->
        val a = i * (2f * PI.toFloat() / 16f)
        LineNode(Position(cos(a) * bottomRadius, 0f, sin(a) * bottomRadius), Position(cos(a) * topRadius, height, sin(a) * topRadius), edge)
    }
}

@Composable
private fun NodeScope.Marker3dVertexNodes(solid: Marker3dSolidState, dimensions: MarkerSolidDimensions, material: MaterialInstance) {
    val points = when (solid.tool) {
        Marker3dShapeTool.Sphere,
        Marker3dShapeTool.Hemisphere,
        Marker3dShapeTool.Cylinder,
        Marker3dShapeTool.Cone,
        Marker3dShapeTool.Torus,
        Marker3dShapeTool.Frustum -> listOf(
            Position(-dimensions.width / 2f, 0f, 0f),
            Position(dimensions.width / 2f, 0f, 0f),
            Position(0f, dimensions.height, 0f)
        )
        else -> listOf(
            Position(-dimensions.width / 2f, 0f, -dimensions.depth / 2f),
            Position(dimensions.width / 2f, 0f, -dimensions.depth / 2f),
            Position(dimensions.width / 2f, 0f, dimensions.depth / 2f),
            Position(-dimensions.width / 2f, 0f, dimensions.depth / 2f),
            Position(0f, dimensions.height, 0f)
        )
    }
    points.forEach {
        RadiantPointRenderer(
            center = it,
            materialInstance = material,
            glowMaterialInstance = material,
            haloMaterialInstance = material,
            style = RadiantPointStyle(centreRadius = 0.009f, haloRadius = 0.021f, isDraggable = true)
        )
    }
}

@Composable
private fun NodeScope.Marker3dNetNode(solid: Marker3dSolidState, edge: MaterialInstance, accent: MaterialInstance) {
    Node(position = Position(0.28f, 0.012f, 0.2f), scale = Scale(0.55f)) {
        when (solid.tool) {
            Marker3dShapeTool.Cylinder,
            Marker3dShapeTool.Cone,
            Marker3dShapeTool.Sphere,
            Marker3dShapeTool.Hemisphere,
            Marker3dShapeTool.Torus,
            Marker3dShapeTool.Frustum -> {
                CircleLines(0.08f, 0.08f, edge, y = 0f)
                BoxEdges(accent, 0.16f, 0.002f, 0.06f)
            }
            else -> {
                BoxEdges(edge, 0.08f, 0.002f, 0.08f)
                listOf(-0.16f, 0.16f).forEach { x -> BoxEdges(accent, 0.06f, 0.002f, 0.06f) }
            }
        }
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
    onAdvancedMathTool: (String) -> Unit,
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
    onClearScene: () -> Unit,
    onMarkerZoom: (Float) -> Unit,
    onMarkerRotate: (Float) -> Unit,
    onMarkerExpand: () -> Unit,
    onMarkerNext: () -> Unit,
    onMarkerReset: () -> Unit,
    onMarkerActivity: (MarkerMathActivity) -> Unit,
    onMarker2dShape: (Marker2dShapeTool) -> Unit,
    onMarker2dFinishDraft: () -> Unit,
    onMarker2dCancelDraft: () -> Unit,
    onMarker2dSelectNext: () -> Unit,
    onMarker2dMove: (Float, Float) -> Unit,
    onMarker2dRotate: (Float) -> Unit,
    onMarker2dScale: (Float) -> Unit,
    onMarker2dDuplicate: () -> Unit,
    onMarker2dHide: () -> Unit,
    onMarker2dDelete: () -> Unit,
    onMarker2dToggleDisplay: (Marker2dDisplayOption) -> Unit,
    onMarker2dConstraint: (Marker2dConstraintTool) -> Unit,
    onMarker3dObject: (String) -> Unit,
    onMarker3dPlacePreview: () -> Unit,
    onMarker3dCancelPreview: () -> Unit,
    onMarker3dMovePreview: (Float, Float) -> Unit,
    onMarker3dSelectNext: () -> Unit,
    onMarker3dMove: (Float, Float, Float) -> Unit,
    onMarker3dRotate: (Marker3dAxis, Float) -> Unit,
    onMarker3dScale: (Float) -> Unit,
    onMarker3dParameter: (String, Float) -> Unit,
    onMarker3dDuplicate: () -> Unit,
    onMarker3dHide: () -> Unit,
    onMarker3dLock: () -> Unit,
    onMarker3dDelete: () -> Unit,
    onMarker3dReset: () -> Unit,
    onMarker3dOverlay: (Marker3dOverlayOption) -> Unit,
    onMarker3dExplode: () -> Unit,
    onMarker3dSection: (Marker3dSectionMode) -> Unit,
    onMarker3dClipping: (Float) -> Unit,
    onMarker3dNet: () -> Unit,
    onMarkerCoordinateTool: (MarkerCoordinateTool) -> Unit,
    onMarkerCoordinateAddPoint: (Float, Float) -> Unit,
    onMarkerCoordinateSelectNext: () -> Unit,
    onMarkerCoordinateMovePoint: (Float, Float) -> Unit,
    onMarkerCoordinateSetPoint: (Float, Float) -> Unit,
    onMarkerCoordinateApplyTool: () -> Unit,
    onMarkerCoordinateSnap: () -> Unit,
    onMarkerCoordinateFormat: () -> Unit,
    onMarkerCoordinateSlopeTriangle: () -> Unit,
    onMarkerCoordinateTransform: (MarkerCoordinateTool, Float) -> Unit,
    onMarkerTransformationTool: (MarkerTransformationTool) -> Unit,
    onMarkerTransformShape: (MarkerTransformShape) -> Unit,
    onMarkerTransformationSequenceStep: (MarkerTransformSequenceAction) -> Unit,
    onClearMarkerTransformationSequence: () -> Unit,
    onMarkerTransformProgress: (Float) -> Unit,
    onMarkerTranslationX: (Float) -> Unit,
    onMarkerTranslationY: (Float) -> Unit,
    onMarkerRotationDegrees: (Float) -> Unit,
    onMarkerRotationCenterX: (Float) -> Unit,
    onMarkerRotationCenterY: (Float) -> Unit,
    onMarkerRotationDirection: () -> Unit,
    onMarkerReflectionLine: (MarkerReflectionLine) -> Unit,
    onMarkerDilationScale: (Float) -> Unit,
    onMarkerDilationCenterX: (Float) -> Unit,
    onMarkerDilationCenterY: (Float) -> Unit,
    onMarkerHorizontalStretch: (Float) -> Unit,
    onMarkerVerticalStretch: (Float) -> Unit,
    onMarkerShear: (Float) -> Unit,
    onUndoMarkerTransformation: () -> Unit,
    onMarkerTrigAngle: (Float) -> Unit,
    onAddMarkerGraphFunction: () -> Unit,
    onSelectMarkerGraphFunction: (String) -> Unit,
    onToggleSelectedMarkerGraphVisibility: () -> Unit,
    onDeleteSelectedMarkerGraphFunction: () -> Unit,
    onDuplicateSelectedMarkerGraphFunction: () -> Unit,
    onMarkerGraphParameter: (String, Float) -> Unit,
    onMarkerGraphTrace: (MarkerGraphTraceMode) -> Unit,
    onMarkerGraphTraceProgress: (Float) -> Unit,
    onMarkerGraphZoom: (Double) -> Unit,
    onMarkerGraphPan: (Double, Double) -> Unit,
    onMarkerGraphFit: () -> Unit,
    onMarkerGraphReset: () -> Unit,
    onMarkerGraphXMin: (Float) -> Unit,
    onMarkerGraphXMax: (Float) -> Unit,
    onMarkerGraphYMin: (Float) -> Unit,
    onMarkerGraphYMax: (Float) -> Unit,
    onToggleMarkerGraphGrid: () -> Unit,
    onToggleMarkerGraphLabels: () -> Unit,
    onToggleMarkerGraphIntercepts: () -> Unit,
    onToggleMarkerGraphExtrema: () -> Unit,
    onToggleMarkerGraphDiscontinuities: () -> Unit,
    onToggleMarkerGraphDerivative: () -> Unit,
    onToggleMarkerGraphIntegralArea: () -> Unit,
    onClearMarkerWorkspace: () -> Unit
) {
    var controlsExpanded by remember { mutableStateOf(state.mathScene.objects.isEmpty()) }
    var activeMenu by remember { mutableStateOf(ArPlacementMenu.Functions) }
    if (state.isQuietMarkerMode()) {
        QuietMarkerChrome(
            state = state,
            onBack = onBack,
            onMarkerZoom = onMarkerZoom,
            onMarkerRotate = onMarkerRotate,
            onMarkerExpand = onMarkerExpand,
            onMarkerNext = onMarkerNext,
            onMarkerReset = onMarkerReset,
            onLiveEquation = onLiveEquation,
            onGraphColorMap = onGraphColorMap,
            onFunction3dTransform = onFunction3dTransform,
            onAnimationProgress = onAnimationProgress,
            onSlicePosition = onSlicePosition,
            onMarkerActivity = onMarkerActivity,
            onMarker2dShape = onMarker2dShape,
            onMarker2dFinishDraft = onMarker2dFinishDraft,
            onMarker2dCancelDraft = onMarker2dCancelDraft,
            onMarker2dSelectNext = onMarker2dSelectNext,
            onMarker2dMove = onMarker2dMove,
            onMarker2dRotate = onMarker2dRotate,
            onMarker2dScale = onMarker2dScale,
            onMarker2dDuplicate = onMarker2dDuplicate,
            onMarker2dHide = onMarker2dHide,
            onMarker2dDelete = onMarker2dDelete,
            onMarker2dToggleDisplay = onMarker2dToggleDisplay,
            onMarker2dConstraint = onMarker2dConstraint,
            onMarker3dObject = onMarker3dObject,
            onMarker3dPlacePreview = onMarker3dPlacePreview,
            onMarker3dCancelPreview = onMarker3dCancelPreview,
            onMarker3dMovePreview = onMarker3dMovePreview,
            onMarker3dSelectNext = onMarker3dSelectNext,
            onMarker3dMove = onMarker3dMove,
            onMarker3dRotate = onMarker3dRotate,
            onMarker3dScale = onMarker3dScale,
            onMarker3dParameter = onMarker3dParameter,
            onMarker3dDuplicate = onMarker3dDuplicate,
            onMarker3dHide = onMarker3dHide,
            onMarker3dLock = onMarker3dLock,
            onMarker3dDelete = onMarker3dDelete,
            onMarker3dReset = onMarker3dReset,
            onMarker3dOverlay = onMarker3dOverlay,
            onMarker3dExplode = onMarker3dExplode,
            onMarker3dSection = onMarker3dSection,
            onMarker3dClipping = onMarker3dClipping,
            onMarker3dNet = onMarker3dNet,
            onMarkerCoordinateTool = onMarkerCoordinateTool,
            onMarkerCoordinateAddPoint = onMarkerCoordinateAddPoint,
            onMarkerCoordinateSelectNext = onMarkerCoordinateSelectNext,
            onMarkerCoordinateMovePoint = onMarkerCoordinateMovePoint,
            onMarkerCoordinateSetPoint = onMarkerCoordinateSetPoint,
            onMarkerCoordinateApplyTool = onMarkerCoordinateApplyTool,
            onMarkerCoordinateSnap = onMarkerCoordinateSnap,
            onMarkerCoordinateFormat = onMarkerCoordinateFormat,
            onMarkerCoordinateSlopeTriangle = onMarkerCoordinateSlopeTriangle,
            onMarkerCoordinateTransform = onMarkerCoordinateTransform,
            onMarkerTransformationTool = onMarkerTransformationTool,
            onMarkerTransformShape = onMarkerTransformShape,
            onMarkerTransformationSequenceStep = onMarkerTransformationSequenceStep,
            onClearMarkerTransformationSequence = onClearMarkerTransformationSequence,
            onMarkerTransformProgress = onMarkerTransformProgress,
            onMarkerTranslationX = onMarkerTranslationX,
            onMarkerTranslationY = onMarkerTranslationY,
            onMarkerRotationDegrees = onMarkerRotationDegrees,
            onMarkerRotationCenterX = onMarkerRotationCenterX,
            onMarkerRotationCenterY = onMarkerRotationCenterY,
            onMarkerRotationDirection = onMarkerRotationDirection,
            onMarkerReflectionLine = onMarkerReflectionLine,
            onMarkerDilationScale = onMarkerDilationScale,
            onMarkerDilationCenterX = onMarkerDilationCenterX,
            onMarkerDilationCenterY = onMarkerDilationCenterY,
            onMarkerHorizontalStretch = onMarkerHorizontalStretch,
            onMarkerVerticalStretch = onMarkerVerticalStretch,
            onMarkerShear = onMarkerShear,
            onUndoMarkerTransformation = onUndoMarkerTransformation,
            onMarkerTrigAngle = onMarkerTrigAngle,
            onAddMarkerGraphFunction = onAddMarkerGraphFunction,
            onSelectMarkerGraphFunction = onSelectMarkerGraphFunction,
            onToggleSelectedMarkerGraphVisibility = onToggleSelectedMarkerGraphVisibility,
            onDeleteSelectedMarkerGraphFunction = onDeleteSelectedMarkerGraphFunction,
            onDuplicateSelectedMarkerGraphFunction = onDuplicateSelectedMarkerGraphFunction,
            onMarkerGraphParameter = onMarkerGraphParameter,
            onMarkerGraphTrace = onMarkerGraphTrace,
            onMarkerGraphTraceProgress = onMarkerGraphTraceProgress,
            onMarkerGraphZoom = onMarkerGraphZoom,
            onMarkerGraphPan = onMarkerGraphPan,
            onMarkerGraphFit = onMarkerGraphFit,
            onMarkerGraphReset = onMarkerGraphReset,
            onMarkerGraphXMin = onMarkerGraphXMin,
            onMarkerGraphXMax = onMarkerGraphXMax,
            onMarkerGraphYMin = onMarkerGraphYMin,
            onMarkerGraphYMax = onMarkerGraphYMax,
            onToggleMarkerGraphGrid = onToggleMarkerGraphGrid,
            onToggleMarkerGraphLabels = onToggleMarkerGraphLabels,
            onToggleMarkerGraphIntercepts = onToggleMarkerGraphIntercepts,
            onToggleMarkerGraphExtrema = onToggleMarkerGraphExtrema,
            onToggleMarkerGraphDiscontinuities = onToggleMarkerGraphDiscontinuities,
            onToggleMarkerGraphDerivative = onToggleMarkerGraphDerivative,
            onToggleMarkerGraphIntegralArea = onToggleMarkerGraphIntegralArea,
            onClearMarkerWorkspace = onClearMarkerWorkspace
        )
        return
    }
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
                        onAdvancedMathTool = onAdvancedMathTool,
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
    ArEngineMode.PaperGraph -> "Marker AR"
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
    ArEngineMode.PaperGraph -> listOf(ArPlacementMenu.Objects, ArPlacementMenu.Graphs, ArPlacementMenu.Functions)
    ArEngineMode.OutdoorGeospatialMath -> listOf(ArPlacementMenu.Graphs, ArPlacementMenu.Objects)
    ArEngineMode.SurfacePlacement -> listOf(ArPlacementMenu.Objects, ArPlacementMenu.Graphs, ArPlacementMenu.Scene)
    ArEngineMode.AirPlacement -> listOf(ArPlacementMenu.Functions, ArPlacementMenu.Graphs, ArPlacementMenu.Solar)
    ArEngineMode.Indoor -> mathArExperience.placementMenus()
}

private fun ArViewerUiState.isQuietMarkerMode(): Boolean =
    arEngineMode == ArEngineMode.PaperGraph

private fun MathArExperience.label(): String = when (this) {
    MathArExperience.MarkerlessObjects -> "Markerless Math"
    MathArExperience.Drawing2dTo3d -> "Drawing 2D to 3D"
    MathArExperience.MarkerBasedGraph -> "Marker-Based AR"
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

private val arMathPurple = Color(0xFF8B4DFF)
private val arMathViolet = Color(0xFFA45CFF)
private val arMathCyan = Color(0xFF42E8FF)
private val arMathMint = Color(0xFF58F2C2)
private val arMathGlass = Color.Black.copy(alpha = 0.58f)
private val arMathBorder = Color.White.copy(alpha = 0.16f)

@Composable
private fun QuietMarkerChrome(
    state: ArViewerUiState,
    onBack: () -> Unit,
    onMarkerZoom: (Float) -> Unit,
    onMarkerRotate: (Float) -> Unit,
    onMarkerExpand: () -> Unit,
    onMarkerNext: () -> Unit,
    onMarkerReset: () -> Unit,
    onLiveEquation: (String) -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onSlicePosition: (Float) -> Unit,
    onMarkerActivity: (MarkerMathActivity) -> Unit,
    onMarker2dShape: (Marker2dShapeTool) -> Unit,
    onMarker2dFinishDraft: () -> Unit,
    onMarker2dCancelDraft: () -> Unit,
    onMarker2dSelectNext: () -> Unit,
    onMarker2dMove: (Float, Float) -> Unit,
    onMarker2dRotate: (Float) -> Unit,
    onMarker2dScale: (Float) -> Unit,
    onMarker2dDuplicate: () -> Unit,
    onMarker2dHide: () -> Unit,
    onMarker2dDelete: () -> Unit,
    onMarker2dToggleDisplay: (Marker2dDisplayOption) -> Unit,
    onMarker2dConstraint: (Marker2dConstraintTool) -> Unit,
    onMarker3dObject: (String) -> Unit,
    onMarker3dPlacePreview: () -> Unit,
    onMarker3dCancelPreview: () -> Unit,
    onMarker3dMovePreview: (Float, Float) -> Unit,
    onMarker3dSelectNext: () -> Unit,
    onMarker3dMove: (Float, Float, Float) -> Unit,
    onMarker3dRotate: (Marker3dAxis, Float) -> Unit,
    onMarker3dScale: (Float) -> Unit,
    onMarker3dParameter: (String, Float) -> Unit,
    onMarker3dDuplicate: () -> Unit,
    onMarker3dHide: () -> Unit,
    onMarker3dLock: () -> Unit,
    onMarker3dDelete: () -> Unit,
    onMarker3dReset: () -> Unit,
    onMarker3dOverlay: (Marker3dOverlayOption) -> Unit,
    onMarker3dExplode: () -> Unit,
    onMarker3dSection: (Marker3dSectionMode) -> Unit,
    onMarker3dClipping: (Float) -> Unit,
    onMarker3dNet: () -> Unit,
    onMarkerCoordinateTool: (MarkerCoordinateTool) -> Unit,
    onMarkerCoordinateAddPoint: (Float, Float) -> Unit,
    onMarkerCoordinateSelectNext: () -> Unit,
    onMarkerCoordinateMovePoint: (Float, Float) -> Unit,
    onMarkerCoordinateSetPoint: (Float, Float) -> Unit,
    onMarkerCoordinateApplyTool: () -> Unit,
    onMarkerCoordinateSnap: () -> Unit,
    onMarkerCoordinateFormat: () -> Unit,
    onMarkerCoordinateSlopeTriangle: () -> Unit,
    onMarkerCoordinateTransform: (MarkerCoordinateTool, Float) -> Unit,
    onMarkerTransformationTool: (MarkerTransformationTool) -> Unit,
    onMarkerTransformShape: (MarkerTransformShape) -> Unit,
    onMarkerTransformationSequenceStep: (MarkerTransformSequenceAction) -> Unit,
    onClearMarkerTransformationSequence: () -> Unit,
    onMarkerTransformProgress: (Float) -> Unit,
    onMarkerTranslationX: (Float) -> Unit,
    onMarkerTranslationY: (Float) -> Unit,
    onMarkerRotationDegrees: (Float) -> Unit,
    onMarkerRotationCenterX: (Float) -> Unit,
    onMarkerRotationCenterY: (Float) -> Unit,
    onMarkerRotationDirection: () -> Unit,
    onMarkerReflectionLine: (MarkerReflectionLine) -> Unit,
    onMarkerDilationScale: (Float) -> Unit,
    onMarkerDilationCenterX: (Float) -> Unit,
    onMarkerDilationCenterY: (Float) -> Unit,
    onMarkerHorizontalStretch: (Float) -> Unit,
    onMarkerVerticalStretch: (Float) -> Unit,
    onMarkerShear: (Float) -> Unit,
    onUndoMarkerTransformation: () -> Unit,
    onMarkerTrigAngle: (Float) -> Unit,
    onAddMarkerGraphFunction: () -> Unit,
    onSelectMarkerGraphFunction: (String) -> Unit,
    onToggleSelectedMarkerGraphVisibility: () -> Unit,
    onDeleteSelectedMarkerGraphFunction: () -> Unit,
    onDuplicateSelectedMarkerGraphFunction: () -> Unit,
    onMarkerGraphParameter: (String, Float) -> Unit,
    onMarkerGraphTrace: (MarkerGraphTraceMode) -> Unit,
    onMarkerGraphTraceProgress: (Float) -> Unit,
    onMarkerGraphZoom: (Double) -> Unit,
    onMarkerGraphPan: (Double, Double) -> Unit,
    onMarkerGraphFit: () -> Unit,
    onMarkerGraphReset: () -> Unit,
    onMarkerGraphXMin: (Float) -> Unit,
    onMarkerGraphXMax: (Float) -> Unit,
    onMarkerGraphYMin: (Float) -> Unit,
    onMarkerGraphYMax: (Float) -> Unit,
    onToggleMarkerGraphGrid: () -> Unit,
    onToggleMarkerGraphLabels: () -> Unit,
    onToggleMarkerGraphIntercepts: () -> Unit,
    onToggleMarkerGraphExtrema: () -> Unit,
    onToggleMarkerGraphDiscontinuities: () -> Unit,
    onToggleMarkerGraphDerivative: () -> Unit,
    onToggleMarkerGraphIntegralArea: () -> Unit,
    onClearMarkerWorkspace: () -> Unit
) {
    val lockedTarget = state.paperGraph?.bestLockedTarget
    val interaction = state.markerLessonInteraction
    val recognized = lockedTarget != null || state.activeMarkerLessonId != null
    val lessonTitle = state.markerLessonTitle.ifBlank { lockedTarget?.name ?: "Scan an AR Maths marker" }
    val markerSubtitle = when {
        !recognized -> "Scan G01"
        lockedTarget?.markerQualityScore != null && lockedTarget.markerQualityScore < 90 -> "Hold G01 steady"
        else -> lessonTitle
    }
    val haptic = LocalHapticFeedback.current
    var toolsExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(state.markerMathActivity) {
        if (state.markerMathActivity == MarkerMathActivity.FunctionGraph) toolsExpanded = false
    }
    Box(Modifier.fillMaxSize()) {
        MinimalMarkerTopBar(
            title = state.activeMarkerLessonId ?: "AR Math",
            subtitle = markerSubtitle,
            onBack = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        )
        if (recognized) {
            MarkerWorkspacePanel(
                state = state,
                onMarkerActivity = onMarkerActivity,
                onMarker2dShape = onMarker2dShape,
                onMarker2dFinishDraft = onMarker2dFinishDraft,
                onMarker2dCancelDraft = onMarker2dCancelDraft,
                onMarker2dSelectNext = onMarker2dSelectNext,
                onMarker2dMove = onMarker2dMove,
                onMarker2dRotate = onMarker2dRotate,
                onMarker2dScale = onMarker2dScale,
                onMarker2dDuplicate = onMarker2dDuplicate,
                onMarker2dHide = onMarker2dHide,
                onMarker2dDelete = onMarker2dDelete,
                onMarker2dToggleDisplay = onMarker2dToggleDisplay,
                onMarker2dConstraint = onMarker2dConstraint,
                onMarker3dObject = onMarker3dObject,
                onMarker3dPlacePreview = onMarker3dPlacePreview,
                onMarker3dCancelPreview = onMarker3dCancelPreview,
                onMarker3dMovePreview = onMarker3dMovePreview,
                onMarker3dSelectNext = onMarker3dSelectNext,
                onMarker3dMove = onMarker3dMove,
                onMarker3dRotate = onMarker3dRotate,
                onMarker3dScale = onMarker3dScale,
                onMarker3dParameter = onMarker3dParameter,
                onMarker3dDuplicate = onMarker3dDuplicate,
                onMarker3dHide = onMarker3dHide,
                onMarker3dLock = onMarker3dLock,
                onMarker3dDelete = onMarker3dDelete,
                onMarker3dReset = onMarker3dReset,
                onMarker3dOverlay = onMarker3dOverlay,
                onMarker3dExplode = onMarker3dExplode,
                onMarker3dSection = onMarker3dSection,
                onMarker3dClipping = onMarker3dClipping,
                onMarker3dNet = onMarker3dNet,
                onMarkerCoordinateTool = onMarkerCoordinateTool,
                onMarkerCoordinateAddPoint = onMarkerCoordinateAddPoint,
                onMarkerCoordinateSelectNext = onMarkerCoordinateSelectNext,
                onMarkerCoordinateMovePoint = onMarkerCoordinateMovePoint,
                onMarkerCoordinateSetPoint = onMarkerCoordinateSetPoint,
                onMarkerCoordinateApplyTool = onMarkerCoordinateApplyTool,
                onMarkerCoordinateSnap = onMarkerCoordinateSnap,
                onMarkerCoordinateFormat = onMarkerCoordinateFormat,
                onMarkerCoordinateSlopeTriangle = onMarkerCoordinateSlopeTriangle,
                onMarkerCoordinateTransform = onMarkerCoordinateTransform,
                onLiveEquation = onLiveEquation,
                onGraphColorMap = onGraphColorMap,
                onFunction3dTransform = onFunction3dTransform,
                onAnimationProgress = onAnimationProgress,
                onSlicePosition = onSlicePosition,
                onMarkerTransformationTool = onMarkerTransformationTool,
                onMarkerTransformShape = onMarkerTransformShape,
                onMarkerTransformationSequenceStep = onMarkerTransformationSequenceStep,
                onClearMarkerTransformationSequence = onClearMarkerTransformationSequence,
                onMarkerTransformProgress = onMarkerTransformProgress,
                onMarkerTranslationX = onMarkerTranslationX,
                onMarkerTranslationY = onMarkerTranslationY,
                onMarkerRotationDegrees = onMarkerRotationDegrees,
                onMarkerRotationCenterX = onMarkerRotationCenterX,
                onMarkerRotationCenterY = onMarkerRotationCenterY,
                onMarkerRotationDirection = onMarkerRotationDirection,
                onMarkerReflectionLine = onMarkerReflectionLine,
                onMarkerDilationScale = onMarkerDilationScale,
                onMarkerDilationCenterX = onMarkerDilationCenterX,
                onMarkerDilationCenterY = onMarkerDilationCenterY,
                onMarkerHorizontalStretch = onMarkerHorizontalStretch,
                onMarkerVerticalStretch = onMarkerVerticalStretch,
                onMarkerShear = onMarkerShear,
                onUndoMarkerTransformation = onUndoMarkerTransformation,
                onMarkerTrigAngle = onMarkerTrigAngle,
                onAddMarkerGraphFunction = onAddMarkerGraphFunction,
                onSelectMarkerGraphFunction = onSelectMarkerGraphFunction,
                onToggleSelectedMarkerGraphVisibility = onToggleSelectedMarkerGraphVisibility,
                onDeleteSelectedMarkerGraphFunction = onDeleteSelectedMarkerGraphFunction,
                onDuplicateSelectedMarkerGraphFunction = onDuplicateSelectedMarkerGraphFunction,
                onMarkerGraphParameter = onMarkerGraphParameter,
                onMarkerGraphTrace = onMarkerGraphTrace,
                onMarkerGraphTraceProgress = onMarkerGraphTraceProgress,
                onMarkerGraphZoom = onMarkerGraphZoom,
                onMarkerGraphPan = onMarkerGraphPan,
                onMarkerGraphFit = onMarkerGraphFit,
                onMarkerGraphReset = onMarkerGraphReset,
                onMarkerGraphXMin = onMarkerGraphXMin,
                onMarkerGraphXMax = onMarkerGraphXMax,
                onMarkerGraphYMin = onMarkerGraphYMin,
                onMarkerGraphYMax = onMarkerGraphYMax,
                onToggleMarkerGraphGrid = onToggleMarkerGraphGrid,
                onToggleMarkerGraphLabels = onToggleMarkerGraphLabels,
                onToggleMarkerGraphIntercepts = onToggleMarkerGraphIntercepts,
                onToggleMarkerGraphExtrema = onToggleMarkerGraphExtrema,
                onToggleMarkerGraphDiscontinuities = onToggleMarkerGraphDiscontinuities,
                onToggleMarkerGraphDerivative = onToggleMarkerGraphDerivative,
                onToggleMarkerGraphIntegralArea = onToggleMarkerGraphIntegralArea,
                onClearMarkerWorkspace = onClearMarkerWorkspace,
                expanded = toolsExpanded,
                onToggleExpanded = { toolsExpanded = !toolsExpanded },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            )
        }
        if (!recognized) {
            MinimalScanHint(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 18.dp)
            )
        }
        if (recognized && state.markerMathActivity != MarkerMathActivity.FunctionGraph) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 78.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalRoundButton("-", "Zoom out", { onMarkerZoom(0.9f) })
                MinimalRoundButton("+", "Zoom in", { onMarkerZoom(1.1f) })
                MinimalRoundButton("R", "Reset", {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (state.markerMathActivity) {
                        MarkerMathActivity.Geometry3D -> onMarker3dReset()
                        MarkerMathActivity.FunctionGraph -> onMarkerGraphReset()
                        else -> onMarkerReset()
                    }
                })
            }
        }
    }
}

@Composable
private fun MarkerWorkspacePanel(
    state: ArViewerUiState,
    onMarkerActivity: (MarkerMathActivity) -> Unit,
    onMarker2dShape: (Marker2dShapeTool) -> Unit,
    onMarker2dFinishDraft: () -> Unit,
    onMarker2dCancelDraft: () -> Unit,
    onMarker2dSelectNext: () -> Unit,
    onMarker2dMove: (Float, Float) -> Unit,
    onMarker2dRotate: (Float) -> Unit,
    onMarker2dScale: (Float) -> Unit,
    onMarker2dDuplicate: () -> Unit,
    onMarker2dHide: () -> Unit,
    onMarker2dDelete: () -> Unit,
    onMarker2dToggleDisplay: (Marker2dDisplayOption) -> Unit,
    onMarker2dConstraint: (Marker2dConstraintTool) -> Unit,
    onMarker3dObject: (String) -> Unit,
    onMarker3dPlacePreview: () -> Unit,
    onMarker3dCancelPreview: () -> Unit,
    onMarker3dMovePreview: (Float, Float) -> Unit,
    onMarker3dSelectNext: () -> Unit,
    onMarker3dMove: (Float, Float, Float) -> Unit,
    onMarker3dRotate: (Marker3dAxis, Float) -> Unit,
    onMarker3dScale: (Float) -> Unit,
    onMarker3dParameter: (String, Float) -> Unit,
    onMarker3dDuplicate: () -> Unit,
    onMarker3dHide: () -> Unit,
    onMarker3dLock: () -> Unit,
    onMarker3dDelete: () -> Unit,
    onMarker3dReset: () -> Unit,
    onMarker3dOverlay: (Marker3dOverlayOption) -> Unit,
    onMarker3dExplode: () -> Unit,
    onMarker3dSection: (Marker3dSectionMode) -> Unit,
    onMarker3dClipping: (Float) -> Unit,
    onMarker3dNet: () -> Unit,
    onMarkerCoordinateTool: (MarkerCoordinateTool) -> Unit,
    onMarkerCoordinateAddPoint: (Float, Float) -> Unit,
    onMarkerCoordinateSelectNext: () -> Unit,
    onMarkerCoordinateMovePoint: (Float, Float) -> Unit,
    onMarkerCoordinateSetPoint: (Float, Float) -> Unit,
    onMarkerCoordinateApplyTool: () -> Unit,
    onMarkerCoordinateSnap: () -> Unit,
    onMarkerCoordinateFormat: () -> Unit,
    onMarkerCoordinateSlopeTriangle: () -> Unit,
    onMarkerCoordinateTransform: (MarkerCoordinateTool, Float) -> Unit,
    onLiveEquation: (String) -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onSlicePosition: (Float) -> Unit,
    onMarkerTransformationTool: (MarkerTransformationTool) -> Unit,
    onMarkerTransformShape: (MarkerTransformShape) -> Unit,
    onMarkerTransformationSequenceStep: (MarkerTransformSequenceAction) -> Unit,
    onClearMarkerTransformationSequence: () -> Unit,
    onMarkerTransformProgress: (Float) -> Unit,
    onMarkerTranslationX: (Float) -> Unit,
    onMarkerTranslationY: (Float) -> Unit,
    onMarkerRotationDegrees: (Float) -> Unit,
    onMarkerRotationCenterX: (Float) -> Unit,
    onMarkerRotationCenterY: (Float) -> Unit,
    onMarkerRotationDirection: () -> Unit,
    onMarkerReflectionLine: (MarkerReflectionLine) -> Unit,
    onMarkerDilationScale: (Float) -> Unit,
    onMarkerDilationCenterX: (Float) -> Unit,
    onMarkerDilationCenterY: (Float) -> Unit,
    onMarkerHorizontalStretch: (Float) -> Unit,
    onMarkerVerticalStretch: (Float) -> Unit,
    onMarkerShear: (Float) -> Unit,
    onUndoMarkerTransformation: () -> Unit,
    onMarkerTrigAngle: (Float) -> Unit,
    onAddMarkerGraphFunction: () -> Unit,
    onSelectMarkerGraphFunction: (String) -> Unit,
    onToggleSelectedMarkerGraphVisibility: () -> Unit,
    onDeleteSelectedMarkerGraphFunction: () -> Unit,
    onDuplicateSelectedMarkerGraphFunction: () -> Unit,
    onMarkerGraphParameter: (String, Float) -> Unit,
    onMarkerGraphTrace: (MarkerGraphTraceMode) -> Unit,
    onMarkerGraphTraceProgress: (Float) -> Unit,
    onMarkerGraphZoom: (Double) -> Unit,
    onMarkerGraphPan: (Double, Double) -> Unit,
    onMarkerGraphFit: () -> Unit,
    onMarkerGraphReset: () -> Unit,
    onMarkerGraphXMin: (Float) -> Unit,
    onMarkerGraphXMax: (Float) -> Unit,
    onMarkerGraphYMin: (Float) -> Unit,
    onMarkerGraphYMax: (Float) -> Unit,
    onToggleMarkerGraphGrid: () -> Unit,
    onToggleMarkerGraphLabels: () -> Unit,
    onToggleMarkerGraphIntercepts: () -> Unit,
    onToggleMarkerGraphExtrema: () -> Unit,
    onToggleMarkerGraphDiscontinuities: () -> Unit,
    onToggleMarkerGraphDerivative: () -> Unit,
    onToggleMarkerGraphIntegralArea: () -> Unit,
    onClearMarkerWorkspace: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val graphMode = state.markerMathActivity == MarkerMathActivity.FunctionGraph
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (graphMode) Color.Transparent else Color.Black.copy(alpha = 0.18f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (graphMode) Color.Transparent else Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        listOf(
                            MarkerMathActivity.Geometry2D,
                            MarkerMathActivity.Geometry3D,
                            MarkerMathActivity.FunctionGraph
                        )
                    ) { activity ->
                        RealGlassChip(
                            label = activity.shortLabel(),
                            selected = state.markerMathActivity == activity,
                            accent = if (activity == MarkerMathActivity.Geometry3D) arMathViolet else arMathCyan,
                            onClick = { onMarkerActivity(activity) }
                        )
                    }
                }
                if (!graphMode) {
                    RealGlassChip(label = if (expanded) "Close" else "Tools", selected = expanded, accent = arMathMint, onClick = onToggleExpanded)
                    RealGlassChip(label = "Clear", selected = false, accent = Color(0xFFFF6B8A), onClick = onClearMarkerWorkspace)
                }
            }
            if (expanded && !graphMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (state.markerMathActivity) {
                        MarkerMathActivity.Geometry2D -> Marker2dToolTray(
                            state = state,
                            onMarker2dShape = onMarker2dShape,
                            onFinishDraft = onMarker2dFinishDraft,
                            onCancelDraft = onMarker2dCancelDraft,
                            onSelectNext = onMarker2dSelectNext,
                            onMove = onMarker2dMove,
                            onRotate = onMarker2dRotate,
                            onScale = onMarker2dScale,
                            onDuplicate = onMarker2dDuplicate,
                            onHide = onMarker2dHide,
                            onDelete = onMarker2dDelete,
                            onToggleDisplay = onMarker2dToggleDisplay,
                            onConstraint = onMarker2dConstraint
                        )
                        MarkerMathActivity.Geometry3D -> Marker3dToolTray(
                            state = state,
                            onMarker3dObject = onMarker3dObject,
                            onPlacePreview = onMarker3dPlacePreview,
                            onCancelPreview = onMarker3dCancelPreview,
                            onMovePreview = onMarker3dMovePreview,
                            onSelectNext = onMarker3dSelectNext,
                            onMove = onMarker3dMove,
                            onRotate = onMarker3dRotate,
                            onScale = onMarker3dScale,
                            onParameter = onMarker3dParameter,
                            onDuplicate = onMarker3dDuplicate,
                            onHide = onMarker3dHide,
                            onLock = onMarker3dLock,
                            onDelete = onMarker3dDelete,
                            onReset = onMarker3dReset,
                            onOverlay = onMarker3dOverlay,
                            onExplode = onMarker3dExplode,
                            onSection = onMarker3dSection,
                            onClipping = onMarker3dClipping,
                            onNet = onMarker3dNet
                        )
                        MarkerMathActivity.FunctionGraph -> MarkerGraphToolTray(
                            state = state,
                            onLiveEquation = onLiveEquation,
                            onAddMarkerGraphFunction = onAddMarkerGraphFunction,
                            onSelectMarkerGraphFunction = onSelectMarkerGraphFunction,
                            onToggleSelectedMarkerGraphVisibility = onToggleSelectedMarkerGraphVisibility,
                            onDeleteSelectedMarkerGraphFunction = onDeleteSelectedMarkerGraphFunction,
                            onDuplicateSelectedMarkerGraphFunction = onDuplicateSelectedMarkerGraphFunction,
                            onMarkerGraphParameter = onMarkerGraphParameter,
                            onMarkerGraphTrace = onMarkerGraphTrace,
                            onMarkerGraphTraceProgress = onMarkerGraphTraceProgress,
                            onMarkerGraphZoom = onMarkerGraphZoom,
                            onMarkerGraphPan = onMarkerGraphPan,
                            onMarkerGraphFit = onMarkerGraphFit,
                            onMarkerGraphReset = onMarkerGraphReset,
                            onMarkerGraphXMin = onMarkerGraphXMin,
                            onMarkerGraphXMax = onMarkerGraphXMax,
                            onMarkerGraphYMin = onMarkerGraphYMin,
                            onMarkerGraphYMax = onMarkerGraphYMax,
                            onToggleMarkerGraphGrid = onToggleMarkerGraphGrid,
                            onToggleMarkerGraphLabels = onToggleMarkerGraphLabels,
                            onToggleMarkerGraphIntercepts = onToggleMarkerGraphIntercepts,
                            onToggleMarkerGraphExtrema = onToggleMarkerGraphExtrema,
                            onToggleMarkerGraphDiscontinuities = onToggleMarkerGraphDiscontinuities,
                            onToggleMarkerGraphDerivative = onToggleMarkerGraphDerivative,
                            onToggleMarkerGraphIntegralArea = onToggleMarkerGraphIntegralArea,
                            onGraphColorMap = onGraphColorMap,
                            onFunction3dTransform = onFunction3dTransform,
                            onAnimationProgress = onAnimationProgress,
                            onSlicePosition = onSlicePosition
                        )
                        MarkerMathActivity.CoordinateLab -> MarkerCoordinateToolTray(
                            state = state,
                            onTool = onMarkerCoordinateTool,
                            onAddPoint = onMarkerCoordinateAddPoint,
                            onSelectNext = onMarkerCoordinateSelectNext,
                            onMovePoint = onMarkerCoordinateMovePoint,
                            onSetPoint = onMarkerCoordinateSetPoint,
                            onApplyTool = onMarkerCoordinateApplyTool,
                            onSnap = onMarkerCoordinateSnap,
                            onFormat = onMarkerCoordinateFormat,
                            onSlopeTriangle = onMarkerCoordinateSlopeTriangle,
                            onTransform = onMarkerCoordinateTransform
                        )
                        MarkerMathActivity.Transformations -> MarkerTransformationToolTray(
                            state = state,
                            onTool = onMarkerTransformationTool,
                            onShape = onMarkerTransformShape,
                            onSequenceStep = onMarkerTransformationSequenceStep,
                            onClearSequence = onClearMarkerTransformationSequence,
                            onProgress = onMarkerTransformProgress,
                            onTranslationX = onMarkerTranslationX,
                            onTranslationY = onMarkerTranslationY,
                            onRotationDegrees = onMarkerRotationDegrees,
                            onRotationCenterX = onMarkerRotationCenterX,
                            onRotationCenterY = onMarkerRotationCenterY,
                            onRotationDirection = onMarkerRotationDirection,
                            onReflectionLine = onMarkerReflectionLine,
                            onDilationScale = onMarkerDilationScale,
                            onDilationCenterX = onMarkerDilationCenterX,
                            onDilationCenterY = onMarkerDilationCenterY,
                            onHorizontalStretch = onMarkerHorizontalStretch,
                            onVerticalStretch = onMarkerVerticalStretch,
                            onShear = onMarkerShear,
                            onUndo = onUndoMarkerTransformation
                        )
                        MarkerMathActivity.MeasurementLab -> MarkerMeasurementChallengeTray(onMarker2dShape, onMarker3dObject)
                        MarkerMathActivity.Trigonometry -> MarkerTrigonometryToolTray(state, onMarkerTrigAngle)
                    }
                }
            }
        }
    }
}

@Composable
private fun Marker2dToolTray(
    state: ArViewerUiState,
    onMarker2dShape: (Marker2dShapeTool) -> Unit,
    onFinishDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    onSelectNext: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onRotate: (Float) -> Unit,
    onScale: (Float) -> Unit,
    onDuplicate: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onToggleDisplay: (Marker2dDisplayOption) -> Unit,
    onConstraint: (Marker2dConstraintTool) -> Unit
) {
    val tools = listOf(
        Marker2dShapeTool.Point,
        Marker2dShapeTool.Line,
        Marker2dShapeTool.Segment,
        Marker2dShapeTool.Ray,
        Marker2dShapeTool.Triangle,
        Marker2dShapeTool.Square,
        Marker2dShapeTool.Rectangle,
        Marker2dShapeTool.Circle,
        Marker2dShapeTool.Ellipse,
        Marker2dShapeTool.Polygon,
        Marker2dShapeTool.RegularPolygon,
        Marker2dShapeTool.Angle,
        Marker2dShapeTool.Arc,
        Marker2dShapeTool.Perpendicular,
        Marker2dShapeTool.Parallel
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val active = state.marker2dActiveTool
        Text(
            text = active?.let { "${it.label}: tap G01 (${state.marker2dDraftPoints.size} point${if (state.marker2dDraftPoints.size == 1) "" else "s"})" }
                ?: "Choose a 2D tool, then tap points on G01",
            style = MaterialTheme.typography.labelLarge,
            color = if (active == null) Color.White.copy(alpha = 0.82f) else arMathMint
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(tools) { shape ->
                RealGlassChip(
                    label = shape.label,
                    selected = state.marker2dActiveTool == shape,
                    accent = arMathMint,
                    onClick = { onMarker2dShape(shape) }
                )
            }
        }
        if (active != null || state.marker2dDraftPoints.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item { RealGlassChip(label = "Finish", selected = false, accent = arMathViolet, onClick = onFinishDraft) }
                item { RealGlassChip(label = "Cancel", selected = false, accent = Color(0xFFFF8A65), onClick = onCancelDraft) }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip(label = "Select", selected = state.marker2dSelectedObjectId != null, accent = arMathCyan, onClick = onSelectNext) }
            item { RealGlassChip(label = "Left", selected = false, accent = arMathCyan, onClick = { onMove(-0.01f, 0f) }) }
            item { RealGlassChip(label = "Right", selected = false, accent = arMathCyan, onClick = { onMove(0.01f, 0f) }) }
            item { RealGlassChip(label = "Up", selected = false, accent = arMathCyan, onClick = { onMove(0f, -0.01f) }) }
            item { RealGlassChip(label = "Down", selected = false, accent = arMathCyan, onClick = { onMove(0f, 0.01f) }) }
            item { RealGlassChip(label = "Rotate", selected = false, accent = arMathViolet, onClick = { onRotate(15f) }) }
            item { RealGlassChip(label = "Grow", selected = false, accent = arMathViolet, onClick = { onScale(1.1f) }) }
            item { RealGlassChip(label = "Shrink", selected = false, accent = arMathViolet, onClick = { onScale(0.9f) }) }
            item { RealGlassChip(label = "Copy", selected = false, accent = arMathMint, onClick = onDuplicate) }
            item { RealGlassChip(label = "Hide", selected = false, accent = Color(0xFFFFC857), onClick = onHide) }
            item { RealGlassChip(label = "Delete", selected = false, accent = Color(0xFFFF6B8A), onClick = onDelete) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip("Labels", state.marker2dShowLabels, arMathMint) { onToggleDisplay(Marker2dDisplayOption.Labels) } }
            item { RealGlassChip("Vertices", state.marker2dShowVertices, arMathMint) { onToggleDisplay(Marker2dDisplayOption.Vertices) } }
            item { RealGlassChip("Measure", state.marker2dShowMeasurements, arMathMint) { onToggleDisplay(Marker2dDisplayOption.Measurements) } }
            item { RealGlassChip("Grid snap", state.marker2dSnapToGrid, arMathCyan) { onToggleDisplay(Marker2dDisplayOption.SnapGrid) } }
            item { RealGlassChip("Point snap", state.marker2dSnapToPoints, arMathCyan) { onToggleDisplay(Marker2dDisplayOption.SnapPoints) } }
            item { RealGlassChip("Lock", state.marker2dLockShape, Color(0xFFFFC857)) { onToggleDisplay(Marker2dDisplayOption.LockShape) } }
            item { RealGlassChip("Construct", state.marker2dShowConstructionLines, arMathViolet) { onToggleDisplay(Marker2dDisplayOption.ConstructionLines) } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(Marker2dConstraintTool.entries) { constraint ->
                RealGlassChip(
                    label = constraint.shortLabel(),
                    selected = state.marker2dActiveConstraint == constraint,
                    accent = Color(0xFFFFC857),
                    onClick = { onConstraint(constraint) }
                )
            }
        }
        state.marker2dSelectedObjectId?.let { selectedId ->
            val selected = state.resolvedConstructions.firstOrNull { it.id == selectedId }
            selected?.let {
                Text(
                    text = "${it.label}  ${if (state.marker2dShowMeasurements) it.valueLabel else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = arMathCyan
                )
            }
        }
    }
}

@Composable
private fun Marker3dToolTray(
    state: ArViewerUiState,
    onMarker3dObject: (String) -> Unit,
    onPlacePreview: () -> Unit,
    onCancelPreview: () -> Unit,
    onMovePreview: (Float, Float) -> Unit,
    onSelectNext: () -> Unit,
    onMove: (Float, Float, Float) -> Unit,
    onRotate: (Marker3dAxis, Float) -> Unit,
    onScale: (Float) -> Unit,
    onParameter: (String, Float) -> Unit,
    onDuplicate: () -> Unit,
    onHide: () -> Unit,
    onLock: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    onOverlay: (Marker3dOverlayOption) -> Unit,
    onExplode: () -> Unit,
    onSection: (Marker3dSectionMode) -> Unit,
    onClipping: (Float) -> Unit,
    onNet: () -> Unit
) {
    val solids = listOf(
        "cube" to "Cube",
        "cuboid" to "Cuboid",
        "sphere" to "Sphere",
        "hemisphere" to "Hemisphere",
        "cylinder" to "Cylinder",
        "cone" to "Cone",
        "pyramid" to "Pyramid",
        "triangular-prism" to "Triangular prism",
        "rectangular-prism" to "Rectangular prism",
        "tetrahedron" to "Tetrahedron",
        "torus" to "Torus",
        "frustum" to "Frustum",
        "custom-prism" to "Custom prism",
        "custom-pyramid" to "Custom pyramid"
    )
    val selected = state.marker3dSolids.firstOrNull { it.selected } ?: state.marker3dSolids.firstOrNull { it.id == state.marker3dSelectedSolidId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = state.marker3dPreviewTool?.let { "${it.uiLabel()} preview: tap G01 or Place" }
                ?: selected?.let { "${it.label}: edit selected solid only" }
                ?: "Choose a 3D solid",
            style = MaterialTheme.typography.labelLarge,
            color = if (state.marker3dPreviewTool == null) Color.White.copy(alpha = 0.82f) else arMathViolet
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(solids) { (id, label) ->
                RealGlassChip(label = label, selected = state.marker3dPreviewTool?.uiId() == id, accent = arMathViolet, onClick = { onMarker3dObject(id) })
            }
        }
        if (state.marker3dPreviewTool != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item { RealGlassChip("Place", false, arMathMint, onPlacePreview) }
                item { RealGlassChip("Cancel", false, Color(0xFFFF8A65), onCancelPreview) }
                item { RealGlassChip("Preview L", false, arMathCyan) { onMovePreview(-0.01f, 0f) } }
                item { RealGlassChip("Preview R", false, arMathCyan) { onMovePreview(0.01f, 0f) } }
                item { RealGlassChip("Preview Up", false, arMathCyan) { onMovePreview(0f, -0.01f) } }
                item { RealGlassChip("Preview Down", false, arMathCyan) { onMovePreview(0f, 0.01f) } }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip("Select", selected != null, arMathCyan, onSelectNext) }
            item { RealGlassChip("X-", false, arMathCyan) { onMove(-0.01f, 0f, 0f) } }
            item { RealGlassChip("X+", false, arMathCyan) { onMove(0.01f, 0f, 0f) } }
            item { RealGlassChip("Z-", false, arMathCyan) { onMove(0f, -0.01f, 0f) } }
            item { RealGlassChip("Z+", false, arMathCyan) { onMove(0f, 0.01f, 0f) } }
            item { RealGlassChip("Lift", false, arMathMint) { onMove(0f, 0f, 0.008f) } }
            item { RealGlassChip("Lower", false, arMathMint) { onMove(0f, 0f, -0.008f) } }
            item { RealGlassChip("Rot X", false, arMathViolet) { onRotate(Marker3dAxis.X, 15f) } }
            item { RealGlassChip("Rot Y", false, arMathViolet) { onRotate(Marker3dAxis.Y, 15f) } }
            item { RealGlassChip("Rot Z", false, arMathViolet) { onRotate(Marker3dAxis.Z, 15f) } }
            item { RealGlassChip("Grow", false, arMathViolet) { onScale(1.1f) } }
            item { RealGlassChip("Shrink", false, arMathViolet) { onScale(0.9f) } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip("Copy", false, arMathMint, onDuplicate) }
            item { RealGlassChip("Hide", false, Color(0xFFFFC857), onHide) }
            item { RealGlassChip("Lock", selected?.locked == true, Color(0xFFFFC857), onLock) }
            item { RealGlassChip("Delete", false, Color(0xFFFF6B8A), onDelete) }
            item { RealGlassChip("Reset", false, arMathCyan, onReset) }
            item { RealGlassChip("Explode", selected?.exploded == true, arMathViolet, onExplode) }
            item { RealGlassChip("Net", selected?.showNet == true, arMathMint, onNet) }
            item { RealGlassChip("H section", selected?.sectionMode == Marker3dSectionMode.Horizontal, arMathCyan) { onSection(Marker3dSectionMode.Horizontal) } }
            item { RealGlassChip("V section", selected?.sectionMode == Marker3dSectionMode.Vertical, arMathCyan) { onSection(Marker3dSectionMode.Vertical) } }
            item { RealGlassChip("No section", selected?.sectionMode == Marker3dSectionMode.None, arMathCyan) { onSection(Marker3dSectionMode.None) } }
        }
        selected?.let { solid ->
            solid.parameterRows().forEach { (id, label, min, max) ->
                MarkerSliderRow(
                    label = label,
                    value = solid.parameters[id] ?: min,
                    valueRange = min..max,
                    valueText = "${((solid.parameters[id] ?: min) * 100).toInt()} cm",
                    onValueChange = { onParameter(id, it) }
                )
            }
            MarkerSliderRow(
                label = "Clip",
                value = solid.clipping,
                valueRange = 0f..1f,
                valueText = "${(solid.clipping * 100).toInt()}%",
                onValueChange = onClipping
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(Marker3dOverlayOption.entries) { option ->
                RealGlassChip(option.shortLabel(), option in state.marker3dOverlays, arMathMint) { onOverlay(option) }
            }
        }
        selected?.let {
            Text(
                text = "${it.metricSummary()}  rot(${it.rotationX.toInt()}, ${it.rotationY.toInt()}, ${it.rotationZ.toInt()})",
                style = MaterialTheme.typography.labelMedium,
                color = arMathCyan
            )
        }
    }
}

@Composable
private fun MarkerGraphToolTray(
    state: ArViewerUiState,
    onLiveEquation: (String) -> Unit,
    onAddMarkerGraphFunction: () -> Unit,
    onSelectMarkerGraphFunction: (String) -> Unit,
    onToggleSelectedMarkerGraphVisibility: () -> Unit,
    onDeleteSelectedMarkerGraphFunction: () -> Unit,
    onDuplicateSelectedMarkerGraphFunction: () -> Unit,
    onMarkerGraphParameter: (String, Float) -> Unit,
    onMarkerGraphTrace: (MarkerGraphTraceMode) -> Unit,
    onMarkerGraphTraceProgress: (Float) -> Unit,
    onMarkerGraphZoom: (Double) -> Unit,
    onMarkerGraphPan: (Double, Double) -> Unit,
    onMarkerGraphFit: () -> Unit,
    onMarkerGraphReset: () -> Unit,
    onMarkerGraphXMin: (Float) -> Unit,
    onMarkerGraphXMax: (Float) -> Unit,
    onMarkerGraphYMin: (Float) -> Unit,
    onMarkerGraphYMax: (Float) -> Unit,
    onToggleMarkerGraphGrid: () -> Unit,
    onToggleMarkerGraphLabels: () -> Unit,
    onToggleMarkerGraphIntercepts: () -> Unit,
    onToggleMarkerGraphExtrema: () -> Unit,
    onToggleMarkerGraphDiscontinuities: () -> Unit,
    onToggleMarkerGraphDerivative: () -> Unit,
    onToggleMarkerGraphIntegralArea: () -> Unit,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onSlicePosition: (Float) -> Unit
) {
    var graphPanelMode by remember { mutableStateOf("Plot") }
    val selectedGraph = state.markerGraphFunctions.firstOrNull { it.selected }
    val presetFunctions = listOf("y = x^2 - 2x - 1", "y = 2x + 3", "y = sin(x)", "y = |x|")
        .filterNot { preset -> state.markerGraphFunctions.any { it.source == preset } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Graph",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            listOf("Plot", "Sliders", "Trace").forEach { mode ->
                RealGlassChip(mode, graphPanelMode == mode, if (mode == "Plot") arMathViolet else arMathCyan) { graphPanelMode = mode }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.liveEquation,
                onValueChange = onLiveEquation,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                singleLine = true,
                label = { Text("Function") },
                placeholder = { Text("y = x^2 - 2x - 1") },
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
            Button(onClick = onAddMarkerGraphFunction) { Text("Plot") }
            OutlinedButton(onClick = onMarkerGraphFit) { Text("Fit") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(state.markerGraphFunctions) { graph ->
                RealGlassChip(
                    label = graph.source,
                    selected = graph.selected,
                    accent = markerGraphPaletteColor(graph.colorIndex),
                    onClick = { onSelectMarkerGraphFunction(graph.id) }
                )
            }
            items(presetFunctions) { expression ->
                RealGlassChip(expression, state.liveEquation == expression, markerGraphPaletteColor(if (expression.contains("2x + 3")) 1 else 0)) {
                    onLiveEquation(expression)
                }
            }
        }
        when (graphPanelMode) {
            "Sliders" -> {
                if (selectedGraph?.sliders?.isNotEmpty() == true) {
                    selectedGraph.sliders.take(4).forEach { slider ->
                        MarkerSlider(
                            label = slider.symbol.parameterLabel(),
                            value = slider.value.toFloat(),
                            minValue = slider.minimum.toFloat(),
                            maxValue = slider.maximum.toFloat(),
                            onValue = { onMarkerGraphParameter(slider.symbol, it) }
                        )
                    }
                } else {
                    Text("No parameters for this graph.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
                }
                MarkerGraphDomainControls(
                    state = state,
                    onXMin = onMarkerGraphXMin,
                    onXMax = onMarkerGraphXMax,
                    onYMin = onMarkerGraphYMin,
                    onYMax = onMarkerGraphYMax
                )
            }
            "Trace" -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item { RealGlassChip("Trace", state.markerGraphTraceMode == MarkerGraphTraceMode.Trace, arMathMint) { onMarkerGraphTrace(MarkerGraphTraceMode.Trace) } }
                    item { RealGlassChip("Tangent", state.markerGraphTraceMode == MarkerGraphTraceMode.Tangent, arMathMint) { onMarkerGraphTrace(MarkerGraphTraceMode.Tangent) } }
                    item { RealGlassChip("Integral", state.markerGraphTraceMode == MarkerGraphTraceMode.Integral, arMathMint) { onMarkerGraphTrace(MarkerGraphTraceMode.Integral) } }
                    item { RealGlassChip("Derivative", state.markerGraphShowDerivative, arMathViolet, onToggleMarkerGraphDerivative) }
                    item { RealGlassChip("Area", state.markerGraphShowIntegralArea, arMathViolet, onToggleMarkerGraphIntegralArea) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Trace", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), modifier = Modifier.width(72.dp))
                    Slider(value = state.markerGraphTraceProgress, onValueChange = onMarkerGraphTraceProgress, modifier = Modifier.weight(1f))
                }
                MarkerGraphSelectedSummary(state)
            }
            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item { RealGlassChip("Grid", state.markerGraphShowGrid, arMathCyan, onToggleMarkerGraphGrid) }
                    item { RealGlassChip("Labels", state.markerGraphShowLabels, arMathCyan, onToggleMarkerGraphLabels) }
                    item { RealGlassChip("Intercepts", state.markerGraphShowIntercepts, arMathMint, onToggleMarkerGraphIntercepts) }
                    item { RealGlassChip("Extrema", state.markerGraphShowExtrema, arMathMint, onToggleMarkerGraphExtrema) }
                    item { RealGlassChip("Breaks", state.markerGraphShowDiscontinuities, Color(0xFFFFC857), onToggleMarkerGraphDiscontinuities) }
                    item { RealGlassChip("Zoom +", false, arMathViolet) { onMarkerGraphZoom(0.75) } }
                    item { RealGlassChip("Zoom -", false, arMathViolet) { onMarkerGraphZoom(1.35) } }
                    item { RealGlassChip("Pan L", false, arMathCyan) { onMarkerGraphPan(-0.75, 0.0) } }
                    item { RealGlassChip("Pan R", false, arMathCyan) { onMarkerGraphPan(0.75, 0.0) } }
                    item { RealGlassChip("Reset", false, arMathViolet, onMarkerGraphReset) }
                }
                MarkerGraphDomainControls(
                    state = state,
                    onXMin = onMarkerGraphXMin,
                    onXMax = onMarkerGraphXMax,
                    onYMin = onMarkerGraphYMin,
                    onYMax = onMarkerGraphYMax
                )
            }
        }
    }
}

@Composable
private fun MarkerGraphFunctionRow(
    graph: MarkerGraphFunctionState,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        color = Color.White.copy(alpha = if (graph.selected) 0.13f else 0.06f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, markerGraphPaletteColor(graph.colorIndex).copy(alpha = if (graph.selected) 0.8f else 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(markerGraphPaletteColor(graph.colorIndex))
            )
            Text(
                graph.source,
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = if (graph.visible) 0.95f else 0.45f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onSelect()
                        onToggleVisible()
                    },
                color = if (graph.visible) markerGraphPaletteColor(graph.colorIndex).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, markerGraphPaletteColor(graph.colorIndex).copy(alpha = 0.7f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (graph.visible) "✓" else "", color = markerGraphPaletteColor(graph.colorIndex), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MarkerGraphInsightsPanel(state: ArViewerUiState, modifier: Modifier = Modifier) {
    val graphs = state.markerGraphFunctions.filter { it.visible && it.isValid && it.compiled.kind != GraphExpressionKind.ExplicitSurface3D }
    val annotations = remember(graphs, state.arGraphDomain, state.markerGraphShowIntercepts, state.markerGraphShowExtrema) {
        markerGraphAnnotations(graphs, state.arGraphDomain, true, true)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Graph Insights", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelLarge)
        if (graphs.isEmpty()) {
            Text("Plot a function to show intercepts.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
        } else {
            annotations.take(3).forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 5.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(markerGraphPaletteColor(item.colorIndex))
                    )
                    Text(item.label.replace("\n", "  "), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MarkerGraphDomainControls(
    state: ArViewerUiState,
    onXMin: (Float) -> Unit,
    onXMax: (Float) -> Unit,
    onYMin: (Float) -> Unit,
    onYMax: (Float) -> Unit
) {
    val domain = state.arGraphDomain
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        item {
            Text(
                "x ${domain.xMin.short()}..${domain.xMax.short()}",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        item {
            Text(
                "y ${domain.yMin.short()}..${domain.yMax.short()}",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        item { RealGlassChip("x-", false, arMathCyan) { onXMin((domain.xMin - 1.0).coerceAtLeast(-12.0).toFloat()) } }
        item { RealGlassChip("x+", false, arMathCyan) { onXMax((domain.xMax + 1.0).coerceAtMost(12.0).toFloat()) } }
        item { RealGlassChip("y-", false, arMathMint) { onYMin((domain.yMin - 1.0).coerceAtLeast(-12.0).toFloat()) } }
        item { RealGlassChip("y+", false, arMathMint) { onYMax((domain.yMax + 1.0).coerceAtMost(12.0).toFloat()) } }
    }
}

@Composable
private fun MarkerGraphSelectedSummary(state: ArViewerUiState) {
    val selected = state.markerGraphFunctions.firstOrNull { it.selected }
    val trace = markerTraceSummary(state, selected)
    Text(
        listOfNotNull(
            selected?.source,
            "x ${state.arGraphDomain.xMin.short()}..${state.arGraphDomain.xMax.short()}",
            "y ${state.arGraphDomain.yMin.short()}..${state.arGraphDomain.yMax.short()}",
            trace
        ).joinToString("  |  "),
        color = Color.White.copy(alpha = 0.78f),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2
    )
}

@Composable
private fun MarkerCoordinateToolTray(
    state: ArViewerUiState,
    onTool: (MarkerCoordinateTool) -> Unit,
    onAddPoint: (Float, Float) -> Unit,
    onSelectNext: () -> Unit,
    onMovePoint: (Float, Float) -> Unit,
    onSetPoint: (Float, Float) -> Unit,
    onApplyTool: () -> Unit,
    onSnap: () -> Unit,
    onFormat: () -> Unit,
    onSlopeTriangle: () -> Unit,
    onTransform: (MarkerCoordinateTool, Float) -> Unit
) {
    var xText by remember { mutableStateOf("0") }
    var yText by remember { mutableStateOf("0") }
    val selected = state.markerCoordinateSelectedPointIds.mapNotNull { id -> state.markerCoordinatePoints.firstOrNull { it.id == id } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Coordinate plane: enter coordinates or tap G01",
            style = MaterialTheme.typography.labelLarge,
            color = arMathCyan
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(MarkerCoordinateTool.entries) { tool ->
                RealGlassChip(label = tool.shortLabel(), selected = state.markerCoordinateTool == tool, accent = arMathCyan, onClick = { onTool(tool) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = xText,
                onValueChange = { xText = it },
                label = { Text("x") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = yText,
                onValueChange = { yText = it },
                label = { Text("y") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onAddPoint(xText.toFloatOrNull() ?: 0f, yText.toFloatOrNull() ?: 0f) }) { Text("Add") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip("Select", selected.isNotEmpty(), arMathViolet, onSelectNext) }
            item { RealGlassChip("Apply", false, arMathMint, onApplyTool) }
            item { RealGlassChip("Left", false, arMathCyan) { onMovePoint(-1f, 0f) } }
            item { RealGlassChip("Right", false, arMathCyan) { onMovePoint(1f, 0f) } }
            item { RealGlassChip("Up", false, arMathCyan) { onMovePoint(0f, 1f) } }
            item { RealGlassChip("Down", false, arMathCyan) { onMovePoint(0f, -1f) } }
            item { RealGlassChip("Set", false, arMathMint) { onSetPoint(xText.toFloatOrNull() ?: 0f, yText.toFloatOrNull() ?: 0f) } }
            item { RealGlassChip("Snap Z", state.markerCoordinateSnapToInteger, Color(0xFFFFC857), onSnap) }
            item { RealGlassChip("Fractions", state.markerCoordinateFractional, arMathMint, onFormat) }
            item { RealGlassChip("Slope tri", state.markerCoordinateShowSlopeTriangle, arMathViolet, onSlopeTriangle) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { RealGlassChip("Translate", false, arMathMint) { onTransform(MarkerCoordinateTool.Translation, 1f) } }
            item { RealGlassChip("Reflect Y", false, arMathMint) { onTransform(MarkerCoordinateTool.Reflection, 1f) } }
            item { RealGlassChip("Rotate 90", false, arMathMint) { onTransform(MarkerCoordinateTool.Rotation, 90f) } }
            item { RealGlassChip("Dilate 2x", false, arMathMint) { onTransform(MarkerCoordinateTool.Dilation, 2f) } }
            item { RealGlassChip("Dilate 1/2", false, arMathMint) { onTransform(MarkerCoordinateTool.Dilation, 0.5f) } }
        }
        MarkerCoordinateSummary(state)
    }
}

@Composable
private fun MarkerTransformationToolTray(
    state: ArViewerUiState,
    onTool: (MarkerTransformationTool) -> Unit,
    onShape: (MarkerTransformShape) -> Unit,
    onSequenceStep: (MarkerTransformSequenceAction) -> Unit,
    onClearSequence: () -> Unit,
    onProgress: (Float) -> Unit,
    onTranslationX: (Float) -> Unit,
    onTranslationY: (Float) -> Unit,
    onRotationDegrees: (Float) -> Unit,
    onRotationCenterX: (Float) -> Unit,
    onRotationCenterY: (Float) -> Unit,
    onRotationDirection: () -> Unit,
    onReflectionLine: (MarkerReflectionLine) -> Unit,
    onDilationScale: (Float) -> Unit,
    onDilationCenterX: (Float) -> Unit,
    onDilationCenterY: (Float) -> Unit,
    onHorizontalStretch: (Float) -> Unit,
    onVerticalStretch: (Float) -> Unit,
    onShear: (Float) -> Unit,
    onUndo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(MarkerTransformShape.entries) { shape ->
                RealGlassChip(
                    label = shape.shortLabel(),
                    selected = state.markerTransformShape == shape,
                    accent = arMathMint,
                    onClick = { onShape(shape) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(MarkerTransformationTool.entries) { tool ->
                RealGlassChip(
                    label = tool.shortLabel(),
                    selected = state.markerTransformTool == tool,
                    accent = if (tool == MarkerTransformationTool.Composite) arMathMint else arMathViolet,
                    onClick = { onTool(tool) }
                )
            }
            item { RealGlassChip(label = "Undo", selected = false, accent = Color(0xFFFFC857), onClick = onUndo) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(MarkerTransformSequenceAction.entries) { action ->
                RealGlassChip(
                    label = action.shortLabel(),
                    selected = state.markerTransformationSequence.any { it.action == action },
                    accent = arMathCyan,
                    onClick = { onSequenceStep(action) }
                )
            }
            item { RealGlassChip(label = "Clear steps", selected = false, accent = Color(0xFFFF6B8A), onClick = onClearSequence) }
        }
        when (state.markerTransformTool) {
            MarkerTransformationTool.Translation -> {
                MarkerSlider("X", state.markerTranslationX, -0.3f, 0.3f, onTranslationX)
                MarkerSlider("Y", state.markerTranslationY, -0.3f, 0.3f, onTranslationY)
            }
            MarkerTransformationTool.Rotation -> {
                MarkerSlider("Center X", state.markerRotationCenterX, -0.3f, 0.3f, onRotationCenterX)
                MarkerSlider("Center Y", state.markerRotationCenterY, -0.3f, 0.3f, onRotationCenterY)
                MarkerSlider("Angle", state.markerRotationDegrees, -180f, 180f, onRotationDegrees)
                RealGlassChip(
                    label = if (state.markerRotationClockwise) "Clockwise" else "Anticlockwise",
                    selected = true,
                    accent = arMathMint,
                    onClick = onRotationDirection
                )
            }
            MarkerTransformationTool.Reflection -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(MarkerReflectionLine.entries) { line ->
                        RealGlassChip(
                            label = line.shortLabel(),
                            selected = state.markerReflectionLine == line,
                            accent = arMathCyan,
                            onClick = { onReflectionLine(line) }
                        )
                    }
                }
            }
            MarkerTransformationTool.Dilation -> {
                MarkerSlider("Center X", state.markerDilationCenterX, -0.3f, 0.3f, onDilationCenterX)
                MarkerSlider("Center Y", state.markerDilationCenterY, -0.3f, 0.3f, onDilationCenterY)
                MarkerSlider("Scale", state.markerDilationScale, 0.2f, 2.6f, onDilationScale)
            }
            MarkerTransformationTool.HorizontalStretch -> MarkerSlider("Horizontal", state.markerHorizontalStretch, 0.2f, 2.8f, onHorizontalStretch)
            MarkerTransformationTool.VerticalStretch -> MarkerSlider("Vertical", state.markerVerticalStretch, 0.2f, 2.8f, onVerticalStretch)
            MarkerTransformationTool.Shear -> MarkerSlider("Shear", state.markerShear, -1.4f, 1.4f, onShear)
            MarkerTransformationTool.Composite -> {
                MarkerSlider("Center X", state.markerRotationCenterX, -0.3f, 0.3f, onRotationCenterX)
                MarkerSlider("Center Y", state.markerRotationCenterY, -0.3f, 0.3f, onRotationCenterY)
                MarkerSlider("Rotation", state.markerRotationDegrees, -180f, 180f, onRotationDegrees)
                MarkerSlider("Scale", state.markerDilationScale, 0.2f, 2.6f, onDilationScale)
                MarkerSlider("X", state.markerTranslationX, -0.3f, 0.3f, onTranslationX)
            }
        }
        MarkerSlider("Animate", state.markerTransformProgress, 0f, 1f, onProgress, valueFormatter = { "${(it * 100f).toInt()}%" })
        Text(
            transformationMatrixLabel(state),
            color = arMathMint,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Text(
            transformationCoordinateSummary(state),
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun MarkerMeasurementChallengeTray(
    onMarker2dShape: (Marker2dShapeTool) -> Unit,
    onMarker3dObject: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        items(
            listOf(
                "Distance" to { onMarker2dShape(Marker2dShapeTool.Line) },
                "Slope" to { onMarker2dShape(Marker2dShapeTool.Line) },
                "Angle" to { onMarker2dShape(Marker2dShapeTool.Triangle) },
                "Area" to { onMarker2dShape(Marker2dShapeTool.Square) },
                "Perimeter" to { onMarker2dShape(Marker2dShapeTool.Square) },
                "Volume" to { onMarker3dObject("cube") }
            )
        ) { (label, action) ->
            RealGlassChip(label = label, selected = label == "Distance", accent = arMathMint, onClick = action)
        }
    }
}

@Composable
private fun MarkerTrigonometryToolTray(state: ArViewerUiState, onMarkerTrigAngle: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MarkerSlider("Angle", state.markerTrigAngleDegrees, 0f, 360f, onMarkerTrigAngle) { "${it.toInt()} deg" }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(listOf(0f, 30f, 45f, 60f, 90f, 180f, 270f, 360f)) { angle ->
                RealGlassChip(
                    label = "${angle.toInt()}",
                    selected = abs(state.markerTrigAngleDegrees - angle) < 0.5f,
                    accent = arMathCyan,
                    onClick = { onMarkerTrigAngle(angle) }
                )
            }
        }
    }
}

@Composable
private fun MarkerSlider(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    onValue: (Float) -> Unit,
    valueFormatter: (Float) -> String = { "%.2f".format(it) }
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), modifier = Modifier.width(72.dp))
        Slider(
            value = rangeToSlider(value, minValue, maxValue),
            onValueChange = { onValue(sliderToRange(it, minValue, maxValue)) },
            modifier = Modifier.weight(1f)
        )
        Text(valueFormatter(value), style = MaterialTheme.typography.labelSmall, color = arMathMint, modifier = Modifier.width(54.dp))
    }
}

private fun MarkerMathActivity.shortLabel(): String = when (this) {
    MarkerMathActivity.Geometry2D -> "2D"
    MarkerMathActivity.Geometry3D -> "3D"
    MarkerMathActivity.FunctionGraph -> "Graph"
    MarkerMathActivity.CoordinateLab -> "Coord"
    MarkerMathActivity.Transformations -> "Transform"
    MarkerMathActivity.MeasurementLab -> "Measure"
    MarkerMathActivity.Trigonometry -> "Trig"
}

private fun Marker2dConstraintTool.shortLabel(): String = when (this) {
    Marker2dConstraintTool.EqualLengths -> "Equal len"
    Marker2dConstraintTool.EqualAngles -> "Equal ang"
    Marker2dConstraintTool.Parallel -> "Parallel"
    Marker2dConstraintTool.Perpendicular -> "Perp"
    Marker2dConstraintTool.Horizontal -> "Horiz"
    Marker2dConstraintTool.Vertical -> "Vert"
    Marker2dConstraintTool.FixedRadius -> "Fix r"
    Marker2dConstraintTool.FixedLength -> "Fix d"
    Marker2dConstraintTool.PointOnLine -> "On line"
    Marker2dConstraintTool.PointOnCircle -> "On circle"
    Marker2dConstraintTool.Midpoint -> "Midpoint"
    Marker2dConstraintTool.Tangency -> "Tangent"
}

private fun MarkerCoordinateTool.shortLabel(): String = when (this) {
    MarkerCoordinateTool.PlotPoint -> "Plot"
    MarkerCoordinateTool.PlotMultiplePoints -> "Multi"
    MarkerCoordinateTool.JoinPoints -> "Join"
    MarkerCoordinateTool.LineThroughTwoPoints -> "Line"
    MarkerCoordinateTool.Midpoint -> "Midpoint"
    MarkerCoordinateTool.Distance -> "Distance"
    MarkerCoordinateTool.Slope -> "Slope"
    MarkerCoordinateTool.SectionFormula -> "Section"
    MarkerCoordinateTool.EquationOfLine -> "Equation"
    MarkerCoordinateTool.ParallelLine -> "Parallel"
    MarkerCoordinateTool.PerpendicularLine -> "Perp"
    MarkerCoordinateTool.TriangleFromCoordinates -> "Triangle"
    MarkerCoordinateTool.PolygonFromCoordinates -> "Polygon"
    MarkerCoordinateTool.Reflection -> "Reflect"
    MarkerCoordinateTool.Translation -> "Translate"
    MarkerCoordinateTool.Rotation -> "Rotate"
    MarkerCoordinateTool.Dilation -> "Dilate"
}

@Composable
private fun MarkerCoordinateSummary(state: ArViewerUiState) {
    val points = state.markerCoordinateSelectedPointIds.mapNotNull { id -> state.markerCoordinatePoints.firstOrNull { it.id == id } }
    val allPointText = state.markerCoordinatePoints.takeLast(6).joinToString("  ") {
        "${it.label}(${it.x.coordinateText(state.markerCoordinateFractional)}, ${it.y.coordinateText(state.markerCoordinateFractional)})"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (allPointText.isNotBlank()) {
            Text(allPointText, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
        }
        if (points.size >= 2) {
            val a = points[points.size - 2]
            val b = points.last()
            val dx = b.x - a.x
            val dy = b.y - a.y
            val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val midpointX = (a.x + b.x) / 2f
            val midpointY = (a.y + b.y) / 2f
            val line = coordinateLineAnalysis(a, b)
            Text(
                "dx=${dx.coordinateText(state.markerCoordinateFractional)}  dy=${dy.coordinateText(state.markerCoordinateFractional)}  d=sqrt(dx^2+dy^2)=${distance.coordinateText(state.markerCoordinateFractional)}",
                style = MaterialTheme.typography.labelMedium,
                color = arMathCyan
            )
            Text(
                "M=((x1+x2)/2,(y1+y2)/2)=(${midpointX.coordinateText(state.markerCoordinateFractional)}, ${midpointY.coordinateText(state.markerCoordinateFractional)})  slope=${line.slope?.coordinateText(state.markerCoordinateFractional) ?: "undefined"}",
                style = MaterialTheme.typography.labelMedium,
                color = arMathMint
            )
            Text(
                "${line.equation}  x-int=${line.xIntercept?.coordinateText(state.markerCoordinateFractional) ?: "--"}  y-int=${line.yIntercept?.coordinateText(state.markerCoordinateFractional) ?: "--"}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFC857)
            )
        }
    }
}

private fun Float.coordinateText(fractional: Boolean): String {
    if (!isFinite()) return "--"
    if (fractional) return toSimpleFraction()
    return when {
        abs(this) >= 100 -> "%.0f".format(this)
        abs(this) >= 10 -> "%.1f".format(this)
        else -> "%.2f".format(this)
    }.trimEnd('0').trimEnd('.')
}

private fun Float.toSimpleFraction(): String {
    val sign = if (this < 0) "-" else ""
    val value = abs(this)
    val whole = value.toInt()
    val fraction = value - whole
    val denominators = listOf(2, 3, 4, 5, 8, 10)
    val best = denominators.map { denominator -> denominator to kotlin.math.round(fraction * denominator).toInt() }
        .minByOrNull { (denominator, numerator) -> abs(fraction - numerator.toFloat() / denominator) }
    val numerator = best?.second ?: 0
    val denominator = best?.first ?: 1
    return when {
        numerator == 0 -> "$sign$whole"
        numerator == denominator -> "$sign${whole + 1}"
        whole == 0 -> "$sign$numerator/$denominator"
        else -> "$sign$whole $numerator/$denominator"
    }
}

private fun Marker3dShapeTool.uiLabel(): String = when (this) {
    Marker3dShapeTool.Cube -> "Cube"
    Marker3dShapeTool.Cuboid -> "Cuboid"
    Marker3dShapeTool.Sphere -> "Sphere"
    Marker3dShapeTool.Hemisphere -> "Hemisphere"
    Marker3dShapeTool.Cylinder -> "Cylinder"
    Marker3dShapeTool.Cone -> "Cone"
    Marker3dShapeTool.Pyramid -> "Pyramid"
    Marker3dShapeTool.TriangularPrism -> "Triangular prism"
    Marker3dShapeTool.RectangularPrism -> "Rectangular prism"
    Marker3dShapeTool.Tetrahedron -> "Tetrahedron"
    Marker3dShapeTool.Torus -> "Torus"
    Marker3dShapeTool.Frustum -> "Frustum"
    Marker3dShapeTool.CustomPrism -> "Custom prism"
    Marker3dShapeTool.CustomPyramid -> "Custom pyramid"
}

private fun Marker3dShapeTool.uiId(): String = when (this) {
    Marker3dShapeTool.Cube -> "cube"
    Marker3dShapeTool.Cuboid -> "cuboid"
    Marker3dShapeTool.Sphere -> "sphere"
    Marker3dShapeTool.Hemisphere -> "hemisphere"
    Marker3dShapeTool.Cylinder -> "cylinder"
    Marker3dShapeTool.Cone -> "cone"
    Marker3dShapeTool.Pyramid -> "pyramid"
    Marker3dShapeTool.TriangularPrism -> "triangular-prism"
    Marker3dShapeTool.RectangularPrism -> "rectangular-prism"
    Marker3dShapeTool.Tetrahedron -> "tetrahedron"
    Marker3dShapeTool.Torus -> "torus"
    Marker3dShapeTool.Frustum -> "frustum"
    Marker3dShapeTool.CustomPrism -> "custom-prism"
    Marker3dShapeTool.CustomPyramid -> "custom-pyramid"
}

private fun Marker3dShapeTool.defaultUiParameters(): Map<String, Float> = when (this) {
    Marker3dShapeTool.Cube -> mapOf("side" to 0.065f)
    Marker3dShapeTool.Cuboid,
    Marker3dShapeTool.RectangularPrism -> mapOf("length" to 0.085f, "width" to 0.055f, "height" to 0.065f)
    Marker3dShapeTool.Sphere,
    Marker3dShapeTool.Hemisphere,
    Marker3dShapeTool.Torus -> mapOf("radius" to 0.034f)
    Marker3dShapeTool.Cylinder,
    Marker3dShapeTool.Cone,
    Marker3dShapeTool.Frustum -> mapOf("radius" to 0.032f, "height" to 0.075f)
    Marker3dShapeTool.Pyramid,
    Marker3dShapeTool.CustomPyramid -> mapOf("base" to 0.075f, "height" to 0.08f)
    Marker3dShapeTool.TriangularPrism -> mapOf("base" to 0.07f, "height" to 0.06f, "length" to 0.085f)
    Marker3dShapeTool.Tetrahedron -> mapOf("side" to 0.075f)
    Marker3dShapeTool.CustomPrism -> mapOf("base" to 0.07f, "height" to 0.06f, "length" to 0.09f)
}

private fun Marker3dOverlayOption.shortLabel(): String = when (this) {
    Marker3dOverlayOption.Vertices -> "Vertices"
    Marker3dOverlayOption.Edges -> "Edges"
    Marker3dOverlayOption.Faces -> "Faces"
    Marker3dOverlayOption.FaceNames -> "Face names"
    Marker3dOverlayOption.Dimensions -> "Dims"
    Marker3dOverlayOption.SurfaceArea -> "SA"
    Marker3dOverlayOption.CurvedSurfaceArea -> "CSA"
    Marker3dOverlayOption.TotalSurfaceArea -> "TSA"
    Marker3dOverlayOption.Volume -> "Volume"
    Marker3dOverlayOption.CrossSection -> "Section"
    Marker3dOverlayOption.Net -> "Net"
}

private fun Marker3dSolidState.parameterRows(): List<Marker3dParameterRow> = when (tool) {
    Marker3dShapeTool.Cube -> listOf(Marker3dParameterRow("side", "Side", 0.025f, 0.105f))
    Marker3dShapeTool.Cuboid,
    Marker3dShapeTool.RectangularPrism -> listOf(
        Marker3dParameterRow("length", "Length", 0.03f, 0.11f),
        Marker3dParameterRow("width", "Width", 0.025f, 0.105f),
        Marker3dParameterRow("height", "Height", 0.03f, 0.095f)
    )
    Marker3dShapeTool.Sphere,
    Marker3dShapeTool.Hemisphere,
    Marker3dShapeTool.Torus -> listOf(Marker3dParameterRow("radius", "Radius", 0.015f, 0.052f))
    Marker3dShapeTool.Cylinder,
    Marker3dShapeTool.Cone,
    Marker3dShapeTool.Frustum -> listOf(
        Marker3dParameterRow("radius", "Radius", 0.015f, 0.052f),
        Marker3dParameterRow("height", "Height", 0.03f, 0.095f)
    )
    Marker3dShapeTool.Pyramid,
    Marker3dShapeTool.CustomPyramid -> listOf(
        Marker3dParameterRow("base", "Base", 0.03f, 0.105f),
        Marker3dParameterRow("height", "Height", 0.03f, 0.095f)
    )
    Marker3dShapeTool.TriangularPrism,
    Marker3dShapeTool.CustomPrism -> listOf(
        Marker3dParameterRow("base", "Base", 0.03f, 0.10f),
        Marker3dParameterRow("height", "Base height", 0.025f, 0.09f),
        Marker3dParameterRow("length", "Prism length", 0.035f, 0.11f)
    )
    Marker3dShapeTool.Tetrahedron -> listOf(Marker3dParameterRow("side", "Side", 0.03f, 0.105f))
}

private data class Marker3dParameterRow(val id: String, val label: String, val min: Float, val max: Float)

private fun Marker3dSolidState.metricSummary(): String {
    val d = dimensions()
    val volume = when (tool) {
        Marker3dShapeTool.Sphere -> 4f / 3f * PI.toFloat() * (d.width / 2f).pow3()
        Marker3dShapeTool.Hemisphere -> 2f / 3f * PI.toFloat() * (d.width / 2f).pow3()
        Marker3dShapeTool.Cylinder -> PI.toFloat() * (d.width / 2f).pow2() * d.height
        Marker3dShapeTool.Cone,
        Marker3dShapeTool.Pyramid,
        Marker3dShapeTool.CustomPyramid -> d.width * d.depth * d.height / 3f
        Marker3dShapeTool.TriangularPrism,
        Marker3dShapeTool.CustomPrism -> d.width * d.height * d.depth / 2f
        Marker3dShapeTool.Tetrahedron -> d.width.pow3() / 8.49f
        Marker3dShapeTool.Torus -> 2f * PI.toFloat() * PI.toFloat() * (d.width / 2f) * (d.height / 6f).pow2()
        Marker3dShapeTool.Frustum -> PI.toFloat() * d.height * ((d.width / 2f).pow2() + (d.width / 2f) * (d.width / 3f) + (d.width / 3f).pow2()) / 3f
        else -> d.width * d.height * d.depth
    }
    val surface = 2f * (d.width * d.depth + d.width * d.height + d.depth * d.height)
    return "V ${"%.1f".format(volume * 1_000_000)} cm3  SA ${"%.1f".format(surface * 10_000)} cm2"
}

private fun Float.pow2(): Float = this * this
private fun Float.pow3(): Float = this * this * this

@Composable
private fun MarkerSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = arMathCyan)
        }
        Slider(value = value.coerceIn(valueRange.start, valueRange.endInclusive), onValueChange = onValueChange, valueRange = valueRange)
    }
}

private fun markerGraphPaletteColor(index: Int): Color =
    listOf(
        Color(0xFF25F4FF),
        Color(0xFFB45CFF),
        Color(0xFF58F2C2),
        Color(0xFFFFC857),
        Color(0xFFFF6B8A),
        Color(0xFF74FF8B)
    )[index.floorMod(6)]

private fun String.parameterLabel(): String = when (this) {
    "a" -> "Amplitude a"
    "b" -> "Frequency b"
    "c" -> "Phase c"
    "d" -> "Shift d"
    "m" -> "Slope m"
    "r" -> "Radius r"
    "k" -> "k"
    else -> this
}

private fun markerTraceSummary(state: ArViewerUiState, selected: MarkerGraphFunctionState?): String? {
    selected ?: return null
    val x = state.arGraphDomain.xMin + (state.arGraphDomain.xMax - state.arGraphDomain.xMin) * state.markerGraphTraceProgress
    val y = arRenderMathEngine.evaluate2dUnclamped(selected.compiled, x)
    if (!y.isFinite()) return "trace undefined"
    val slope = state.arGraphAnalysis.tangent?.tangentSlope
    return buildString {
        append("P(${x.short()}, ${y.short()})")
        append(" f(x)=${y.short()}")
        if (state.markerGraphShowDerivative || state.markerGraphTraceMode == MarkerGraphTraceMode.Tangent) {
            append(" slope=${slope?.short() ?: "--"}")
        }
    }
}

private fun Double.short(): String =
    when {
        !isFinite() -> "--"
        abs(this) >= 100 -> "%.0f".format(this)
        abs(this) >= 10 -> "%.1f".format(this)
        else -> "%.2f".format(this)
    }.trimEnd('0').trimEnd('.')

private fun MarkerTransformationTool.shortLabel(): String = when (this) {
    MarkerTransformationTool.Translation -> "Translate"
    MarkerTransformationTool.Rotation -> "Rotate"
    MarkerTransformationTool.Reflection -> "Reflect"
    MarkerTransformationTool.Dilation -> "Dilate"
    MarkerTransformationTool.HorizontalStretch -> "H Stretch"
    MarkerTransformationTool.VerticalStretch -> "V Stretch"
    MarkerTransformationTool.Shear -> "Shear"
    MarkerTransformationTool.Composite -> "Composite"
}

private fun MarkerTransformShape.shortLabel(): String = when (this) {
    MarkerTransformShape.Point -> "Point"
    MarkerTransformShape.Segment -> "Segment"
    MarkerTransformShape.Triangle -> "Triangle"
    MarkerTransformShape.Square -> "Square"
    MarkerTransformShape.Rectangle -> "Rectangle"
    MarkerTransformShape.Polygon -> "Polygon"
    MarkerTransformShape.Circle -> "Circle"
}

private fun MarkerTransformSequenceAction.shortLabel(): String = when (this) {
    MarkerTransformSequenceAction.Rotate90 -> "Rotate 90"
    MarkerTransformSequenceAction.Translate32 -> "Translate (3,2)"
    MarkerTransformSequenceAction.ReflectYAxis -> "Reflect Y"
}

private fun MarkerReflectionLine.shortLabel(): String = when (this) {
    MarkerReflectionLine.XAxis -> "x-axis"
    MarkerReflectionLine.YAxis -> "y-axis"
    MarkerReflectionLine.YEqualsX -> "y=x"
    MarkerReflectionLine.YEqualsNegativeX -> "y=-x"
    MarkerReflectionLine.UserLine -> "custom"
}

private fun rangeToSlider(value: Float, minValue: Float, maxValue: Float): Float =
    ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)

private fun sliderToRange(value: Float, minValue: Float, maxValue: Float): Float =
    minValue + value.coerceIn(0f, 1f) * (maxValue - minValue)

private fun transformationMatrixLabel(state: ArViewerUiState): String = when (state.markerTransformTool) {
    MarkerTransformationTool.Translation -> "T = [1 0 ${"%.2f".format(state.markerTranslationX)}; 0 1 ${"%.2f".format(state.markerTranslationY)}; 0 0 1]"
    MarkerTransformationTool.Rotation -> "R(${state.markerRotationDegrees.toInt()} deg) about (${state.markerRotationCenterX.coord()}, ${state.markerRotationCenterY.coord()})"
    MarkerTransformationTool.Reflection -> "Reflect over ${state.markerReflectionLine.shortLabel()}"
    MarkerTransformationTool.Dilation -> "D = ${"%.2f".format(state.markerDilationScale)} about (${state.markerDilationCenterX.coord()}, ${state.markerDilationCenterY.coord()})"
    MarkerTransformationTool.HorizontalStretch -> "Sx = ${"%.2f".format(state.markerHorizontalStretch)}"
    MarkerTransformationTool.VerticalStretch -> "Sy = ${"%.2f".format(state.markerVerticalStretch)}"
    MarkerTransformationTool.Shear -> "H = [1 ${"%.2f".format(state.markerShear)}; 0 1]"
    MarkerTransformationTool.Composite -> state.markerTransformationSequence
        .ifEmpty {
            listOf(
                MarkerTransformationStepState(MarkerTransformSequenceAction.Rotate90, "Rotate 90 deg"),
                MarkerTransformationStepState(MarkerTransformSequenceAction.Translate32, "Translate (3, 2)"),
                MarkerTransformationStepState(MarkerTransformSequenceAction.ReflectYAxis, "Reflect Y-axis")
            )
        }
        .joinToString(" -> ") { it.label }
}

private fun transformationCoordinateSummary(state: ArViewerUiState): String {
    val before = markerTransformShapePoints(state.markerTransformShape).firstOrNull() ?: return ""
    val after = before.transformedBy(state)
    return "A (${before.x.coord()}, ${before.y.coord()}) -> A' (${after.x.coord()}, ${after.y.coord()})"
}

private fun Float.coord(): String = "%.2f".format(this)

@Composable
private fun QuietActionButton(label: String, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            modifier = Modifier
                .semantics { this.contentDescription = contentDescription }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MinimalMarkerTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MinimalRoundButton(label = "<", contentDescription = "Back", onClick = onBack)
        Surface(
            color = Color.Black.copy(alpha = 0.18f),
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), maxLines = 1)
            }
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(arMathMint))
        }
    }
}

@Composable
private fun MinimalMarkerConceptChip(
    label: String,
    formula: String,
    markerId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.38f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, arMathCyan.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(markerId, color = arMathMint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(formattedMathExpression(formula), color = arMathCyan, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MarkerExpressionEditor(
    state: ArViewerUiState,
    markerId: String,
    lessonTitle: String,
    focus: MarkerLessonElement?,
    onLiveEquation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, arMathCyan.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(arMathMint))
                Text(markerId, color = arMathMint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(lessonTitle, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            OutlinedTextField(
                value = state.liveEquation,
                onValueChange = onLiveEquation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(focus?.label ?: "Expression") },
                supportingText = focus?.formula?.let { formula ->
                    { Text(formattedMathExpression(formula), color = arMathCyan) }
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MarkerGraphControlPanel(
    state: ArViewerUiState,
    onGraphColorMap: (GraphColorMap) -> Unit,
    onFunction3dTransform: (Function3dTransformMode) -> Unit,
    onAnimationProgress: (Float) -> Unit,
    onSlicePosition: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.34f),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                items(Function3dTransformMode.entries) { mode ->
                    RealGlassChip(
                        label = mode.label(),
                        selected = state.function3dTransformMode == mode,
                        onClick = { onFunction3dTransform(mode) }
                    )
                }
                items(GraphColorMap.entries) { map ->
                    RealGlassChip(
                        label = map.label(),
                        selected = state.graphColorMap == map,
                        accent = when (map) {
                            GraphColorMap.Height -> arMathViolet
                            GraphColorMap.Slope -> arMathMint
                            GraphColorMap.Curvature -> Color(0xFFFFB86B)
                            GraphColorMap.XValue -> arMathCyan
                            GraphColorMap.YValue -> Color(0xFFFF6B8A)
                        },
                        onClick = { onGraphColorMap(map) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Slice", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                Slider(
                    value = state.graphSlicePosition,
                    onValueChange = onSlicePosition,
                    modifier = Modifier.weight(1f)
                )
                Text("${(state.graphSlicePosition * 100f).toInt()}%", style = MaterialTheme.typography.labelSmall, color = arMathMint)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Animate", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                Slider(
                    value = state.graphAnimationProgress,
                    onValueChange = onAnimationProgress,
                    modifier = Modifier.weight(1f)
                )
                Text("${(state.graphAnimationProgress * 100f).toInt()}%", style = MaterialTheme.typography.labelSmall, color = arMathViolet)
            }
        }
    }
}

@Composable
private fun RealGlassChip(
    label: String,
    selected: Boolean,
    accent: Color = arMathViolet,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (selected) accent.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.045f),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.10f))
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun MinimalMarkerControls(
    zoomPercent: Int,
    expanded: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.44f),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MinimalRoundButton("-", "Zoom out", onZoomOut)
            MinimalRoundButton("+", "Zoom in", onZoomIn)
            MinimalRoundButton("L", "Rotate left", onRotateLeft)
            MinimalRoundButton("R", "Rotate right", onRotateRight)
            MinimalRoundButton("Next", "Next concept", onNext, wide = true, selected = true)
            MinimalRoundButton(if (expanded) "Fit" else "Max", "Expand or fit", onExpand, wide = true)
            MinimalRoundButton("Reset", "Reset marker lesson", onReset, wide = true)
            Text("$zoomPercent%", color = arMathMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MinimalScanHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.34f),
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFB86B)))
            Text("Point camera at a marker", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MinimalRoundButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    wide: Boolean = false,
    selected: Boolean = false
) {
    Surface(
        modifier = Modifier
            .height(42.dp)
            .width(if (wide) 68.dp else 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        color = if (selected) arMathPurple.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.10f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) arMathViolet.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.10f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    selected: Boolean = false,
    disabled: Boolean = false,
    error: Boolean = false,
    content: @Composable () -> Unit
) {
    val borderColor = when {
        error -> Color(0xFFFF6B8A).copy(alpha = 0.42f)
        selected -> arMathViolet.copy(alpha = 0.58f)
        disabled -> Color.White.copy(alpha = 0.07f)
        else -> arMathBorder
    }
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(borderColor, Color.White.copy(alpha = 0.05f))),
                shape = RoundedCornerShape(cornerRadius)
            ),
        color = arMathGlass.copy(alpha = if (disabled) 0.34f else 0.58f),
        contentColor = Color.White,
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 10.dp else 2.dp
    ) {
        Box(Modifier.padding(10.dp)) { content() }
    }
}

@Composable
private fun GlassIconButton(
    label: String,
    contentDescription: String,
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !disabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        color = if (selected) arMathPurple.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.10f),
        contentColor = if (disabled) Color.White.copy(alpha = 0.32f) else Color.White,
        border = BorderStroke(1.dp, if (selected) arMathViolet.copy(alpha = 0.65f) else arMathBorder),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (selected) 10.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArMathTopHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(label = "≡", contentDescription = "Menu", onClick = onBack)
        GlassPanel(cornerRadius = 24.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(arMathPurple, arMathCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AR", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
                Column {
                    Text("AR Math", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Learn • Visualize • Solve", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(label = "?", contentDescription = "Help", onClick = {})
            GlassIconButton(label = "⚙", contentDescription = "Settings", onClick = {})
        }
    }
}

@Composable
private fun NeonSegmentedControl(
    items: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit = {}
) {
    GlassPanel(modifier = modifier, cornerRadius = 28.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val active = item == selected
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelected(item) },
                    color = if (active) Color.Transparent else Color.Transparent,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) Brush.horizontalGradient(listOf(arMathPurple, arMathViolet)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item, color = if (active) Color.White else Color.White.copy(alpha = 0.76f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MathCategoryRail(selected: String, modifier: Modifier = Modifier) {
    val categories = listOf(
        Triple("A", "Algebra", "x²"),
        Triple("G", "Geometry", "△"),
        Triple("T", "Trig", "~"),
        Triple("F", "Functions", "ƒ"),
        Triple("C", "Coord", "(x,y)"),
        Triple("M", "Measure", "π"),
        Triple("More", "More", "•••")
    )
    GlassPanel(modifier = modifier, cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            categories.forEach { (id, label, icon) ->
                val active = id == selected
                Column(
                    modifier = Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) arMathPurple.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, if (active) arMathViolet.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, color = if (active) Color.White else Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(label, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ARControlRail(
    zoomPercent: Int,
    expanded: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onExpand: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier, cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassIconButton("3D", "3D view", selected = true, onClick = {})
            GlassIconButton("2D", "2D view", onClick = onExpand)
            GlassIconButton("AR", "AR view", selected = true, onClick = {})
            GlassIconButton("Grid", "Grid", onClick = onZoomOut)
            GlassIconButton("Layer", "Layers", selected = expanded, onClick = onExpand)
            GlassIconButton("R", "Reset", onClick = onReset)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniGlassButton("+", "Zoom in", onZoomIn)
                MiniGlassButton("-", "Zoom out", onZoomOut)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniGlassButton("L", "Rotate left", onRotateLeft)
                MiniGlassButton("R", "Rotate right", onRotateRight)
            }
            Text("$zoomPercent%", color = arMathMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniGlassButton(label: String, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        color = Color.White.copy(alpha = 0.10f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(11.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecognizedExpressionCard(
    title: String,
    expression: String,
    subtitle: String,
    recognized: Boolean,
    confidence: Int,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp, selected = recognized) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (recognized) arMathMint else Color(0xFFFFB86B)))
                Text(title, color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${confidence.coerceIn(0, 100)}%", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(18.dp))
            }
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
            Text(
                formattedMathExpression(expression),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ARObjectLabel(label: String, formula: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 22.dp, selected = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(formattedMathExpression(formula), color = arMathCyan, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MathInsightCard(title: String, rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier.width(220.dp), cornerRadius = 22.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            rows.forEach { (key, value) ->
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(key, color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall)
                    Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SolutionStepsCard(state: ArViewerUiState, modifier: Modifier = Modifier) {
    val elements = state.markerLessonInteraction.elements.take(5)
    MathInsightCard(
        title = "Solution Steps",
        rows = elements.mapIndexed { index, element -> "${index + 1}" to "${element.label}: ${element.formula.orEmpty()}" },
        modifier = modifier
    )
}

@Composable
private fun GraphInsightCard(state: ArViewerUiState, modifier: Modifier = Modifier) {
    val focus = state.markerLessonInteraction.focusedElement
    MathInsightCard(
        title = "Graph Insights",
        rows = listOf(
            "Marker" to (state.activeMarkerLessonId ?: "Scanning"),
            "Type" to markerCategoryName(state.activeMarkerLessonId),
            "Focus" to (focus?.label ?: "None"),
            "Rule" to (focus?.formula ?: "Lock marker"),
            "View" to if (state.markerLessonInteraction.expanded) "Expanded" else "Fit"
        ),
        modifier = modifier
    )
}

@Composable
private fun EquationTileTray(
    elements: List<MarkerLessonElement>,
    focusedIndex: Int,
    modifier: Modifier = Modifier
) {
    if (elements.isEmpty()) return
    GlassPanel(modifier = modifier, cornerRadius = 22.dp) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(elements.take(8)) { element ->
                EquationTile(
                    text = element.formula ?: element.label,
                    selected = element == elements.getOrNull(focusedIndex)
                )
            }
        }
    }
}

@Composable
private fun BottomGlassNavigation() {
    GlassPanel(cornerRadius = 28.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Home", "History", "Camera", "Saved", "Profile").forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (item == "Camera") "[] " else "•", color = if (item == "Camera") arMathViolet else Color.White.copy(alpha = 0.55f))
                    Text(item, color = if (item == "Camera") Color.White else Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EquationTile(text: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    GlassPanel(modifier = modifier, cornerRadius = 16.dp, selected = selected) {
        Text(formattedMathExpression(text), color = if (selected) arMathMint else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AngleSlider(angle: Float, modifier: Modifier = Modifier, onAngle: (Float) -> Unit = {}) {
    GlassPanel(modifier = modifier, cornerRadius = 22.dp) {
        Column {
            Text("Angle ${angle.toInt()} deg", color = Color.White, fontWeight = FontWeight.SemiBold)
            Slider(value = angle, onValueChange = onAngle, valueRange = 0f..360f)
        }
    }
}

@Composable
private fun GraphLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (label, color) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(label, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ExpandableAnalysisPanel(title: String, expanded: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassPanel(modifier = modifier, cornerRadius = 22.dp, selected = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            if (expanded) content()
        }
    }
}

private fun formattedMathExpression(text: String) = buildAnnotatedString {
    var exponent = false
    text.forEach { char ->
        when (char) {
            '^' -> exponent = true
            else -> {
                if (exponent) {
                    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 14.sp)) { append(char) }
                    exponent = false
                } else {
                    append(char)
                }
            }
        }
    }
}

private fun markerCategoryName(markerId: String?): String =
    when (markerId?.take(1)) {
        "A" -> "Algebra"
        "G" -> "Geometry"
        "C" -> "Coordinate"
        "F" -> "Function"
        "M" -> "Mensuration"
        else -> "Marker AR"
    }

@Composable
private fun QuietMarkerCard(
    markerId: String,
    locked: Boolean
) {
    val context = LocalContext.current
    val bitmap = remember(markerId) {
        runCatching {
            context.assets.open("ar_markers/geometry/$markerId.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (locked) Color(0xFF16A34A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.14f))
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = markerId,
                modifier = Modifier
                    .size(58.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(markerId, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text = markerId,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
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
    onAdvancedMathTool: (String) -> Unit,
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
            onAdvancedMathTool = onAdvancedMathTool,
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
    onAdvancedMathTool: (String) -> Unit,
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
                    onAnalysisFocus = onAnalysisFocus,
                    onAdvancedMathTool = onAdvancedMathTool
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
    onAnalysisFocus: (Float) -> Unit,
    onAdvancedMathTool: (String) -> Unit
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
        AdvancedMathToolsPanel(
            state = state,
            onAdvancedMathTool = onAdvancedMathTool
        )
    }
}

@Composable
private fun AdvancedMathToolsPanel(
    state: ArViewerUiState,
    onAdvancedMathTool: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Advanced AR Math", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArAdvancedMathTools.tools) { tool ->
                FilterChip(
                    selected = state.selectedAdvancedToolId == tool.id,
                    onClick = { onAdvancedMathTool(tool.id) },
                    label = { Text(tool.title) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ElevatedAssistChip(onClick = {}, label = { Text("Parity ${state.advancedCapabilityReport.strengthScore}%") }) }
            item { AssistChip(onClick = {}, label = { Text(state.compiledArExpression.kind.label) }) }
            state.advancedCapabilityReport.supportedTools.take(5).forEach { tool ->
                item { AssistChip(onClick = {}, label = { Text(tool.evidenceLabel) }) }
            }
        }
        state.advancedCapabilityReport.missingWorkflowHints.take(3).forEach { hint ->
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
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
        MathArFeature.MultiObjectConstraints,
        MathArFeature.ParametricGraphing,
        MathArFeature.ImplicitRelations,
        MathArFeature.LocusTrace,
        MathArFeature.CasCommands
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
    ) + listOf(
        MathArFeature.ProofChecker,
        MathArFeature.MacroTools
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
    MathArFeature.ParametricGraphing -> "Parametric"
    MathArFeature.ImplicitRelations -> "Implicit"
    MathArFeature.LocusTrace -> "Trace"
    MathArFeature.ProofChecker -> "Proof"
    MathArFeature.MacroTools -> "Macro"
    MathArFeature.CasCommands -> "CAS"
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
                    if (state.paperGraph?.hasLockedTarget == true) "Marker locked" else "Scan a geometry marker",
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
        if (state.advancedCapabilityReport.strengthScore > 0) {
            add("Advanced parity ${state.advancedCapabilityReport.strengthScore}%")
        }
        if (MathArFeature.ParametricGraphing in state.enabledMathArFeatures) add("Parametric trace")
        if (MathArFeature.ImplicitRelations in state.enabledMathArFeatures) add("Implicit relation")
        if (MathArFeature.ProofChecker in state.enabledMathArFeatures) add("Proof evidence")
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
        state.arEngineMode == ArEngineMode.PaperGraph && state.paperGraph?.hasLockedTarget == true -> "Marker locked - place 3D shapes"
        state.arEngineMode == ArEngineMode.PaperGraph -> "Scan G01 geometry marker"
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
