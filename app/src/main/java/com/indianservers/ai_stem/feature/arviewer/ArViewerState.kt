package com.indianservers.ai_stem.feature.arviewer

import com.indianservers.ai_stem.data.scene.SavedSceneSummary
import com.indianservers.ai_stem.core.ar.ArGuidanceSeverity
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.core.ar.ArPlacementMode
import com.indianservers.ai_stem.core.ar.GraphColorMap
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialFrameState
import com.indianservers.ai_stem.core.ar.PaperGraphFrameState
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.MathScene
import com.indianservers.ai_stem.domain.scene.SceneHistory
import com.indianservers.ai_stem.domain.scene.SceneInteractionMode
import kotlinx.coroutines.flow.Flow

sealed interface ArAvailabilityState {
    data object Checking : ArAvailabilityState
    data object Supported : ArAvailabilityState
    data object PermissionRequired : ArAvailabilityState
    data object ArServicesInstallationRequired : ArAvailabilityState
    data object ArServicesUpdateRequired : ArAvailabilityState
    data object UnsupportedDevice : ArAvailabilityState
    data class Error(val userMessage: String) : ArAvailabilityState
}

enum class CameraPermissionState { NotRequested, Granted, Denied, PermanentlyDenied }
enum class LocationPermissionState { NotRequested, Granted, Denied, PermanentlyDenied }
enum class ArSessionStatus { Initializing, Scanning, PlaneDetected, ReadyToPlace, ObjectPlaced, ObjectSelected, MovingObject, TrackingLost, Paused, FatalError }
enum class TrackingStatus { Unknown, Tracking, Limited, Paused }
enum class PlacementHitKind { None, Plane, DepthPoint, FeaturePoint, Instant, StreetscapeGeometry }
enum class PlacementQuality { Unknown, Excellent, Good, Weak, Recovering, Lost }
enum class ArRecoveryMode { Normal, TrackingLimited, AnchorRecovering, RePlacementRequired }
enum class InteractionMode { Placement, ObjectSelected, MovingObject }
enum class PaperGraphCalibrationStep { Origin, XAxisPoint, YAxisPoint, Complete }
enum class PaperGraphLayer { Axes, Scale, Graph, Surface3d, CrossSection }
enum class FloatingMathTool { Axes, Grid, Labels, Formula, Measure, Slice, Animate, Capture }
enum class GraphAnimationMode { RotateGraph, SweepArea, BuildVolume, MoveTangentPoint, AnimateSineWave }
enum class Function3dTransformMode { Surface, Extrusion, SolidOfRevolution, TangentPlane, CrossSectionSlices }
enum class ArFeaturePhase { DirectInteraction, GraphAnalysis, EngineStrengthening }
enum class MathArFeature {
    ObjectSnapping,
    GestureHandles,
    LiveEquationEditing,
    PointPicker,
    RootVisualizer,
    TangentNormalTool,
    AreaUnderCurve,
    VolumeBuilder,
    MeasurementRulerAnchors,
    CoordinateGridLocking,
    MultiObjectConstraints,
    DepthOcclusion,
    ScenePersistence,
    PrecisionConfidenceHud,
    CompareMode
}
enum class MathArExperience {
    MarkerlessObjects,
    Drawing2dTo3d,
    MarkerBasedGraph,
    OutdoorGeometry,
    SolarSystem,
    SceneTools
}

data class UiMessage(val text: String)

data class PaperGraphCalibrationState(
    val step: PaperGraphCalibrationStep = PaperGraphCalibrationStep.Origin,
    val originLocked: Boolean = false,
    val xAxisLocked: Boolean = false,
    val yAxisLocked: Boolean = false,
    val origin: PaperGraphCalibrationPoint? = null,
    val xAxisPoint: PaperGraphCalibrationPoint? = null,
    val yAxisPoint: PaperGraphCalibrationPoint? = null
) {
    val isComplete: Boolean
        get() = step == PaperGraphCalibrationStep.Complete
}

data class PaperGraphCalibrationPoint(
    val screenX: Float,
    val screenY: Float,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float
)

data class ArPointLabel(
    val label: String,
    val x: Float,
    val y: Float,
    val z: Float
)

data class PlacedMathObject(
    val id: String,
    val type: MathObjectType,
    val rotationDegrees: Float = 0f,
    val scaleFactor: Float = 1f,
    val selected: Boolean = true,
    val transformRevision: Int = 0
)

data class ArViewerUiState(
    val availability: ArAvailabilityState = ArAvailabilityState.Checking,
    val permission: CameraPermissionState = CameraPermissionState.NotRequested,
    val locationPermission: LocationPermissionState = LocationPermissionState.NotRequested,
    val arEngineMode: ArEngineMode = ArEngineMode.Indoor,
    val mathArExperience: MathArExperience = MathArExperience.MarkerlessObjects,
    val sessionStatus: ArSessionStatus = ArSessionStatus.Initializing,
    val trackingStatus: TrackingStatus = TrackingStatus.Unknown,
    val anchorTrackingStatus: TrackingStatus = TrackingStatus.Unknown,
    val trackingMessage: String = "Initializing AR tracking.",
    val placementQuality: PlacementQuality = PlacementQuality.Unknown,
    val recoveryMode: ArRecoveryMode = ArRecoveryMode.Normal,
    val canRePlaceObject: Boolean = false,
    val placementScore: Int = 0,
    val motionStable: Boolean = true,
    val lightStable: Boolean = true,
    val smartPlacementGuidance: String = "Move slowly to scan.",
    val shouldDelayPlacement: Boolean = false,
    val shouldPreferPlane: Boolean = false,
    val guidanceHeadline: String = "Initializing AR",
    val guidanceInstruction: String = "Move slowly to scan your space.",
    val guidanceSeverity: ArGuidanceSeverity = ArGuidanceSeverity.Info,
    val recommendedPlacementMode: ArPlacementMode = ArPlacementMode.Auto,
    val arDiagnostics: List<String> = emptyList(),
    val diagnosticsVisible: Boolean = false,
    val selectedDefinitionId: String = "cube",
    val selectedObjectType: MathObjectType = MathObjectType.Cube,
    val placedObject: PlacedMathObject? = null,
    val mathScene: MathScene = MathScene(name = "AR Mathematics Scene"),
    val history: SceneHistory = SceneHistory(),
    val experienceMode: ExperienceMode = ExperienceMode.Beginner,
    val sceneInteractionMode: SceneInteractionMode = SceneInteractionMode.Place,
    val hasValidPlacementHit: Boolean = false,
    val placementHitKind: PlacementHitKind = PlacementHitKind.None,
    val paperGraph: PaperGraphFrameState? = null,
    val paperGraphCalibration: PaperGraphCalibrationState = PaperGraphCalibrationState(),
    val paperGraphLayers: Set<PaperGraphLayer> = setOf(
        PaperGraphLayer.Axes,
        PaperGraphLayer.Scale,
        PaperGraphLayer.Graph,
        PaperGraphLayer.Surface3d,
        PaperGraphLayer.CrossSection
    ),
    val activeFloatingTool: FloatingMathTool = FloatingMathTool.Axes,
    val axesVisible: Boolean = true,
    val gridVisible: Boolean = true,
    val labelsVisible: Boolean = true,
    val formulaCardsVisible: Boolean = true,
    val formulaCardsCollapsed: Boolean = false,
    val measurementsVisible: Boolean = true,
    val slicePlaneVisible: Boolean = false,
    val graphAnimationEnabled: Boolean = false,
    val graphAnimationProgress: Float = 0f,
    val graphAnimationMode: GraphAnimationMode = GraphAnimationMode.RotateGraph,
    val graphSlicePosition: Float = 0.5f,
    val graphColorMap: GraphColorMap = GraphColorMap.Height,
    val function3dTransformMode: Function3dTransformMode = Function3dTransformMode.Surface,
    val selectedFeaturePhase: ArFeaturePhase = ArFeaturePhase.DirectInteraction,
    val enabledMathArFeatures: Set<MathArFeature> = setOf(
        MathArFeature.GestureHandles,
        MathArFeature.PointPicker,
        MathArFeature.LiveEquationEditing,
        MathArFeature.AreaUnderCurve,
        MathArFeature.PrecisionConfidenceHud
    ),
    val liveEquation: String = "y = sin(x)",
    val pickedPoints: List<ArPointLabel> = emptyList(),
    val rulerAnchors: List<ArPointLabel> = emptyList(),
    val snappingEnabled: Boolean = false,
    val coordinateGridLocked: Boolean = false,
    val compareModeEnabled: Boolean = false,
    val compareOffsetMeters: Float = 0.34f,
    val depthOcclusionPolishEnabled: Boolean = false,
    val lastPlacementPoint: ArPointLabel? = null,
    val captureRequested: Boolean = false,
    val outdoorGeospatial: OutdoorGeospatialFrameState? = null,
    val planesVisible: Boolean = true,
    val interactionMode: InteractionMode = InteractionMode.Placement,
    val inspectorVisible: Boolean = false,
    val layersVisible: Boolean = false,
    val savedScenesVisible: Boolean = false,
    val savedScenes: List<SavedSceneSummary> = emptyList(),
    val loadedSceneNeedsPlacement: Boolean = false,
    val userMessage: UiMessage? = null,
    val error: String? = null
)

sealed interface ObjectInteractionCommand {
    data class Select(val objectId: String) : ObjectInteractionCommand
    data class Rotate(val deltaDegrees: Float) : ObjectInteractionCommand
    data class Scale(val scaleFactor: Float) : ObjectInteractionCommand
    data class MoveTo(val placementId: String) : ObjectInteractionCommand
    data object ResetTransform : ObjectInteractionCommand
    data object DeleteSelected : ObjectInteractionCommand
}

interface ObjectInteractionSource {
    val commands: Flow<ObjectInteractionCommand>
}

class TouchInteractionSource(override val commands: Flow<ObjectInteractionCommand>) : ObjectInteractionSource

fun reduceInteraction(
    state: ArViewerUiState,
    command: ObjectInteractionCommand
): ArViewerUiState {
    val objectState = state.placedObject ?: return state
    return when (command) {
        is ObjectInteractionCommand.Select -> state.copy(
            placedObject = objectState.copy(selected = command.objectId == objectState.id),
            interactionMode = if (command.objectId == objectState.id) InteractionMode.ObjectSelected else InteractionMode.Placement
        )
        is ObjectInteractionCommand.Rotate -> state.copy(
            placedObject = objectState.copy(
                rotationDegrees = com.indianservers.ai_stem.domain.mathematics.normalizeRotationDegrees(objectState.rotationDegrees + command.deltaDegrees)
            )
        )
        is ObjectInteractionCommand.Scale -> state.copy(
            placedObject = objectState.copy(scaleFactor = com.indianservers.ai_stem.domain.mathematics.clampScale(objectState.scaleFactor * command.scaleFactor))
        )
        is ObjectInteractionCommand.MoveTo -> state.copy(
            sessionStatus = ArSessionStatus.ObjectPlaced,
            interactionMode = InteractionMode.ObjectSelected,
            userMessage = UiMessage("Object moved to the new surface.")
        )
        ObjectInteractionCommand.ResetTransform -> state.copy(
            placedObject = objectState.copy(
                rotationDegrees = 0f,
                scaleFactor = 1f,
                transformRevision = objectState.transformRevision + 1
            ),
            userMessage = UiMessage("Transform reset.")
        )
        ObjectInteractionCommand.DeleteSelected -> state.copy(
            placedObject = null,
            sessionStatus = ArSessionStatus.Scanning,
            interactionMode = InteractionMode.Placement,
            userMessage = UiMessage("Object deleted.")
        )
    }
}
