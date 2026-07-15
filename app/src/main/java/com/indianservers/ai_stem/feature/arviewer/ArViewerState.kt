package com.indianservers.ai_stem.feature.arviewer

import com.indianservers.ai_stem.data.scene.SavedSceneSummary
import com.indianservers.ai_stem.core.ar.ArGuidanceSeverity
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.core.ar.ArPlacementMode
import com.indianservers.ai_stem.core.ar.GraphColorMap
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialFrameState
import com.indianservers.ai_stem.core.ar.PaperGraphFrameState
import com.indianservers.ai_stem.domain.graph.ArCompiledExpression
import com.indianservers.ai_stem.domain.graph.ArAdvancedCapabilityReport
import com.indianservers.ai_stem.domain.graph.ArAdvancedMathTools
import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.ArGraphAnalysisReport
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.graph.GraphExpressionKind
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import com.indianservers.ai_stem.domain.graph.GraphSlider
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState
import com.indianservers.ai_stem.domain.geometry.ResolvedConstructionObject
import com.indianservers.ai_stem.domain.interaction.ArGestureHandle
import com.indianservers.ai_stem.domain.interaction.ArPickedMathPoint
import com.indianservers.ai_stem.domain.interaction.ArRulerMeasurement
import com.indianservers.ai_stem.domain.interaction.ArSnapResult
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.ArDepthOcclusionMode
import com.indianservers.ai_stem.domain.scene.ArActivitySharePackage
import com.indianservers.ai_stem.domain.scene.ArPerformanceProfile
import com.indianservers.ai_stem.domain.scene.ArWorkflowEvaluation
import com.indianservers.ai_stem.domain.scene.ArWorkflowProgress
import com.indianservers.ai_stem.domain.scene.ArSceneSharePackage
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
enum class MarkerMathActivity { Geometry2D, Geometry3D, FunctionGraph, CoordinateLab, Transformations, MeasurementLab, Trigonometry }
enum class MarkerTransformationTool { Translation, Rotation, Reflection, Dilation, HorizontalStretch, VerticalStretch, Shear, Composite }
enum class MarkerReflectionLine { XAxis, YAxis, YEqualsX, YEqualsNegativeX, UserLine }
enum class MarkerTransformShape { Point, Segment, Triangle, Square, Rectangle, Polygon, Circle }
enum class MarkerTransformSequenceAction { Rotate90, Translate32, ReflectYAxis }
enum class MarkerGraphTraceMode { Off, Trace, Tangent, Integral }
enum class Marker3dShapeTool {
    Cube,
    Cuboid,
    Sphere,
    Hemisphere,
    Cylinder,
    Cone,
    Pyramid,
    TriangularPrism,
    RectangularPrism,
    Tetrahedron,
    Torus,
    Frustum,
    CustomPrism,
    CustomPyramid
}
enum class Marker3dOverlayOption {
    Vertices,
    Edges,
    Faces,
    FaceNames,
    Dimensions,
    SurfaceArea,
    CurvedSurfaceArea,
    TotalSurfaceArea,
    Volume,
    CrossSection,
    Net
}
enum class Marker3dSectionMode { None, Horizontal, Vertical }
enum class MarkerCoordinateTool {
    PlotPoint,
    PlotMultiplePoints,
    JoinPoints,
    LineThroughTwoPoints,
    Midpoint,
    Distance,
    Slope,
    SectionFormula,
    EquationOfLine,
    ParallelLine,
    PerpendicularLine,
    TriangleFromCoordinates,
    PolygonFromCoordinates,
    Reflection,
    Translation,
    Rotation,
    Dilation
}
enum class MarkerCoordinateShapeKind { Segment, Line, Triangle, Polygon, Parallel, Perpendicular }
enum class Marker2dConstraintTool {
    EqualLengths,
    EqualAngles,
    Parallel,
    Perpendicular,
    Horizontal,
    Vertical,
    FixedRadius,
    FixedLength,
    PointOnLine,
    PointOnCircle,
    Midpoint,
    Tangency
}
enum class ArFeaturePhase { DirectInteraction, GraphAnalysis, EngineStrengthening, WorkflowStudio }
enum class MarkerLessonElementKind { Point, Line, Segment, Ray, Plane, Angle, Shape2d, Circle, Triangle, Transform, Algebra, Coordinate, FunctionGraph, Measurement2d, Solid3d }
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
    CompareMode,
    GuidedWorkflow,
    ActivityExport,
    EvidenceRecorder,
    ParametricGraphing,
    ImplicitRelations,
    LocusTrace,
    ProofChecker,
    MacroTools,
    CasCommands
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

data class MarkerGraphFunctionState(
    val id: String,
    val source: String,
    val compiled: ArCompiledExpression,
    val sliders: List<GraphSlider> = compiled.parameters,
    val visible: Boolean = true,
    val selected: Boolean = false,
    val colorIndex: Int = 0
) {
    val displayName: String
        get() = source.ifBlank { "Function" }

    val isValid: Boolean
        get() = compiled.isValid
}

data class MarkerTransformationStepState(
    val action: MarkerTransformSequenceAction,
    val label: String
)

data class Marker2dPointState(
    val x: Float,
    val y: Float
)

data class Marker3dSolidState(
    val id: String,
    val tool: Marker3dShapeTool,
    val label: String,
    val x: Float = 0f,
    val z: Float = 0f,
    val lift: Float = 0.08f,
    val scale: Float = 1f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val parameters: Map<String, Float> = emptyMap(),
    val selected: Boolean = true,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val exploded: Boolean = false,
    val sectionMode: Marker3dSectionMode = Marker3dSectionMode.None,
    val clipping: Float = 0.5f,
    val showNet: Boolean = false
)

data class MarkerCoordinatePointState(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val selected: Boolean = false
)

data class MarkerCoordinateShapeState(
    val id: String,
    val label: String,
    val kind: MarkerCoordinateShapeKind,
    val pointIds: List<String>,
    val visible: Boolean = true
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

data class MarkerLessonElement(
    val id: String,
    val label: String,
    val kind: MarkerLessonElementKind,
    val formula: String? = null
)

data class MarkerLessonInteractionState(
    val zoom: Float = 1f,
    val rotationDegrees: Float = 0f,
    val expanded: Boolean = false,
    val focusIndex: Int = 0,
    val step: Int = 0,
    val elements: List<MarkerLessonElement> = emptyList()
) {
    val focusedElement: MarkerLessonElement? get() = elements.getOrNull(focusIndex.coerceIn(0, (elements.size - 1).coerceAtLeast(0)))
}

data class ArViewerUiState(
    val availability: ArAvailabilityState = ArAvailabilityState.Checking,
    val permission: CameraPermissionState = CameraPermissionState.NotRequested,
    val locationPermission: LocationPermissionState = LocationPermissionState.NotRequested,
    val arEngineMode: ArEngineMode = ArEngineMode.PaperGraph,
    val mathArExperience: MathArExperience = MathArExperience.MarkerBasedGraph,
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
    val activeMarkerLessonId: String? = null,
    val markerLessonTitle: String = "",
    val markerLessonSubtitle: String = "",
    val markerLessonInteraction: MarkerLessonInteractionState = MarkerLessonInteractionState(),
    val markerMathActivity: MarkerMathActivity = MarkerMathActivity.Geometry2D,
    val markerTransformTool: MarkerTransformationTool = MarkerTransformationTool.Translation,
    val markerTransformShape: MarkerTransformShape = MarkerTransformShape.Triangle,
    val markerTransformationSequence: List<MarkerTransformationStepState> = emptyList(),
    val markerTransformProgress: Float = 1f,
    val markerTranslationX: Float = 0.16f,
    val markerTranslationY: Float = 0.1f,
    val markerRotationCenterX: Float = 0f,
    val markerRotationCenterY: Float = 0f,
    val markerRotationDegrees: Float = 45f,
    val markerRotationClockwise: Boolean = false,
    val markerReflectionLine: MarkerReflectionLine = MarkerReflectionLine.YAxis,
    val markerDilationCenterX: Float = 0f,
    val markerDilationCenterY: Float = 0f,
    val markerDilationScale: Float = 1.4f,
    val markerHorizontalStretch: Float = 1.4f,
    val markerVerticalStretch: Float = 0.7f,
    val markerShear: Float = 0.45f,
    val markerTrigAngleDegrees: Float = 30f,
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
    val labelsVisible: Boolean = false,
    val formulaCardsVisible: Boolean = false,
    val formulaCardsCollapsed: Boolean = false,
    val measurementsVisible: Boolean = false,
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
    val compiledArExpression: ArCompiledExpression = ArMathEngine().compile("y = sin(x)"),
    val comparisonEquation: String = "y = 0",
    val compiledComparisonExpression: ArCompiledExpression = ArMathEngine().compile("y = 0"),
    val arGraphAnalysisFocusX: Double = 0.0,
    val arGraphAnalysis: ArGraphAnalysisReport = ArMathEngine().analyze(ArMathEngine().compile("y = sin(x)")),
    val arGraphDomain: ArGraphDomain = ArGraphDomain(),
    val arGraphQualityPreset: GraphQualityPreset = GraphQualityPreset.Balanced,
    val arGraphSliders: List<GraphSlider> = emptyList(),
    val markerGraphFunctions: List<MarkerGraphFunctionState> = emptyList(),
    val selectedMarkerGraphFunctionId: String? = null,
    val markerGraphTraceMode: MarkerGraphTraceMode = MarkerGraphTraceMode.Off,
    val markerGraphTraceProgress: Float = 0.5f,
    val markerGraphShowGrid: Boolean = true,
    val markerGraphShowLabels: Boolean = true,
    val markerGraphShowIntercepts: Boolean = true,
    val markerGraphShowExtrema: Boolean = false,
    val markerGraphShowDiscontinuities: Boolean = true,
    val markerGraphShowDerivative: Boolean = false,
    val markerGraphShowIntegralArea: Boolean = false,
    val marker2dActiveTool: Marker2dShapeTool? = null,
    val marker2dDraftPoints: List<Marker2dPointState> = emptyList(),
    val marker2dSelectedObjectId: String? = null,
    val marker2dShowLabels: Boolean = true,
    val marker2dShowVertices: Boolean = true,
    val marker2dShowMeasurements: Boolean = true,
    val marker2dSnapToGrid: Boolean = true,
    val marker2dSnapToPoints: Boolean = false,
    val marker2dLockShape: Boolean = false,
    val marker2dShowConstructionLines: Boolean = true,
    val marker2dActiveConstraint: Marker2dConstraintTool? = null,
    val marker3dPreviewTool: Marker3dShapeTool? = null,
    val marker3dPreviewX: Float = 0f,
    val marker3dPreviewZ: Float = 0f,
    val marker3dPreviewLift: Float = 0.08f,
    val marker3dSolids: List<Marker3dSolidState> = emptyList(),
    val marker3dSelectedSolidId: String? = null,
    val marker3dOverlays: Set<Marker3dOverlayOption> = setOf(
        Marker3dOverlayOption.Edges,
        Marker3dOverlayOption.Vertices,
        Marker3dOverlayOption.Dimensions,
        Marker3dOverlayOption.Volume
    ),
    val markerCoordinateTool: MarkerCoordinateTool = MarkerCoordinateTool.PlotPoint,
    val markerCoordinatePoints: List<MarkerCoordinatePointState> = emptyList(),
    val markerCoordinateShapes: List<MarkerCoordinateShapeState> = emptyList(),
    val markerCoordinateSelectedPointIds: List<String> = emptyList(),
    val markerCoordinateSelectedShapeId: String? = null,
    val markerCoordinateSnapToInteger: Boolean = true,
    val markerCoordinateFractional: Boolean = false,
    val markerCoordinateShowSlopeTriangle: Boolean = true,
    val advancedCapabilityReport: ArAdvancedCapabilityReport = ArAdvancedMathTools.evaluate(
        equationKind = GraphExpressionKind.Explicit2D,
        hasAnalysis = false,
        constructionCount = 0,
        pickedPointCount = 0,
        exported = false
    ),
    val selectedAdvancedToolId: String? = null,
    val pickedPoints: List<ArPointLabel> = emptyList(),
    val pickedGraphPoints: List<ArPickedMathPoint> = emptyList(),
    val rulerAnchors: List<ArPointLabel> = emptyList(),
    val rulerMeasurement: ArRulerMeasurement? = null,
    val constructionGeometry: ConstructionGeometryState = ConstructionGeometryState(),
    val resolvedConstructions: List<ResolvedConstructionObject> = emptyList(),
    val snappingEnabled: Boolean = false,
    val lastSnapResult: ArSnapResult? = null,
    val selectedGestureHandle: ArGestureHandle = ArGestureHandle.UniformScale,
    val transformHandleStep: Float = 0.05f,
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
    val exportedScenePackage: ArSceneSharePackage? = null,
    val exportedActivityPackage: ArActivitySharePackage? = null,
    val selectedTemplateId: String? = null,
    val activeWorkflowId: String = "surface-masterclass",
    val workflowProgress: ArWorkflowProgress = ArWorkflowProgress(),
    val workflowEvaluation: ArWorkflowEvaluation = ArWorkflowEvaluation(),
    val depthOcclusionMode: ArDepthOcclusionMode = ArDepthOcclusionMode.Off,
    val performanceProfile: ArPerformanceProfile = ArPerformanceProfile.Balanced,
    val meshDensity: Float = 0.65f,
    val maxSceneObjects: Int = 32,
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
