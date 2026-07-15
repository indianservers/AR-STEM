package com.indianservers.ai_stem.feature.arviewer

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.ar.core.ArCoreApk
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.core.ar.GraphColorMap
import com.indianservers.ai_stem.core.ar.ArGuidanceEngine
import com.indianservers.ai_stem.core.ar.ArGuidanceInput
import com.indianservers.ai_stem.core.ar.ArSensorState
import com.indianservers.ai_stem.core.ar.ArSensorFusionResult
import com.indianservers.ai_stem.core.ar.ArStabilityEngine
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialFrameState
import com.indianservers.ai_stem.core.ar.PaperGraphFrameState
import com.indianservers.ai_stem.data.scene.LocalSceneRepository
import com.indianservers.ai_stem.data.scene.SceneRepository
import com.indianservers.ai_stem.data.scene.SceneStorageResult
import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.graph.ArAdvancedMathTools
import com.indianservers.ai_stem.domain.graph.ArAdvancedToolKind
import com.indianservers.ai_stem.domain.graph.ArGraphDomain
import com.indianservers.ai_stem.domain.graph.ArMathEngine
import com.indianservers.ai_stem.domain.graph.GraphQualityPreset
import com.indianservers.ai_stem.domain.graph.GraphSlider
import com.indianservers.ai_stem.domain.geometry.ConstructionConstraintKind
import com.indianservers.ai_stem.domain.geometry.ConstructionDependencyKind
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryEngine
import com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState
import com.indianservers.ai_stem.domain.geometry.ConstructionObject
import com.indianservers.ai_stem.domain.geometry.ConstructionObjectKind
import com.indianservers.ai_stem.domain.geometry.ConstructionPoint
import com.indianservers.ai_stem.domain.interaction.ArGestureHandle
import com.indianservers.ai_stem.domain.interaction.ArInteractionEngine
import com.indianservers.ai_stem.domain.interaction.ArSnappingProfile
import com.indianservers.ai_stem.domain.interaction.SnapKind
import com.indianservers.ai_stem.domain.scene.DefaultObjectTransformService
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.ArDepthOcclusionMode
import com.indianservers.ai_stem.domain.scene.ArWorkflowEngine
import com.indianservers.ai_stem.domain.scene.ArWorkflowProgress
import com.indianservers.ai_stem.domain.scene.ArWorkflowSignal
import com.indianservers.ai_stem.domain.scene.ArWorkflowTemplates
import com.indianservers.ai_stem.domain.scene.ArPerformanceProfile
import com.indianservers.ai_stem.domain.scene.ArSceneProductionSettings
import com.indianservers.ai_stem.domain.scene.ArSceneShareExporter
import com.indianservers.ai_stem.domain.scene.ArSceneTemplates
import com.indianservers.ai_stem.domain.scene.ObjectTransform
import com.indianservers.ai_stem.domain.scene.PersistentAnchorKind
import com.indianservers.ai_stem.domain.scene.PersistentAnchorRecord
import com.indianservers.ai_stem.domain.scene.SceneInteractionMode
import com.indianservers.ai_stem.domain.scene.SceneMutations
import com.indianservers.ai_stem.domain.scene.Shape3dAnchorMode
import com.indianservers.ai_stem.domain.scene.Shape3dEngine
import com.indianservers.ai_stem.domain.scene.Shape3dPlacementRequest
import com.indianservers.ai_stem.domain.scene.SnapshotSceneCommand
import com.indianservers.ai_stem.domain.scene.TransformMovement
import com.indianservers.ai_stem.domain.scene.TransformRotation
import com.indianservers.ai_stem.domain.scene.TransformScaling
import com.indianservers.ai_stem.domain.scene.Vector3Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ArViewerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArViewerUiState())
    val uiState: StateFlow<ArViewerUiState> = _uiState
    private var sceneRepository: SceneRepository? = null
    private val stabilityEngine = ArStabilityEngine()
    private val guidanceEngine = ArGuidanceEngine()
    private val arMathEngine = ArMathEngine()
    private val arInteractionEngine = ArInteractionEngine(arMathEngine)
    private val constructionEngine = ConstructionGeometryEngine()
    private val arSceneShareExporter = ArSceneShareExporter()
    private val arWorkflowEngine = ArWorkflowEngine()

    fun checkAvailability(context: Context) {
        sceneRepository = sceneRepository ?: LocalSceneRepository(context.applicationContext)
        refreshSavedScenes()
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val availability = runCatching {
            val arCore = ArCoreApk.getInstance().checkAvailability(context)
            val sensorReadiness = readSensorReadiness(context)
            Log.d(
                "AiStemAR",
                "Availability permission=$permissionGranted arCore=$arCore sensors=$sensorReadiness"
            )
            when {
                !sensorReadiness.ready -> ArAvailabilityState.Error(sensorReadiness.userMessage)
                arCore.isTransient -> ArAvailabilityState.Checking
                arCore == ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    if (permissionGranted) ArAvailabilityState.Supported else ArAvailabilityState.PermissionRequired
                }
                arCore == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArAvailabilityState.ArServicesInstallationRequired
                arCore == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArAvailabilityState.ArServicesUpdateRequired
                arCore == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArAvailabilityState.UnsupportedDevice
                else -> ArAvailabilityState.Error("Unable to confirm AR support. Please try again.")
            }
        }.getOrElse {
            Log.e("AiStemAR", "AR availability check failed", it)
            ArAvailabilityState.Error("AR compatibility check failed.")
        }
        _uiState.update {
            it.copy(
                availability = availability,
                permission = if (permissionGranted) CameraPermissionState.Granted else it.permission,
                locationPermission = if (hasFineLocation(context)) LocationPermissionState.Granted else it.locationPermission,
                sessionStatus = if (availability == ArAvailabilityState.Supported) ArSessionStatus.Scanning else it.sessionStatus
            )
        }
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _uiState.update {
            it.copy(
                permission = when {
                    granted -> CameraPermissionState.Granted
                    permanentlyDenied -> CameraPermissionState.PermanentlyDenied
                    else -> CameraPermissionState.Denied
                },
                availability = if (granted) ArAvailabilityState.Supported else ArAvailabilityState.PermissionRequired,
                userMessage = if (granted) UiMessage("Camera ready. Move slowly to find a flat surface.") else null
            )
        }
    }

    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _uiState.update {
            it.copy(
                locationPermission = when {
                    granted -> LocationPermissionState.Granted
                    permanentlyDenied -> LocationPermissionState.PermanentlyDenied
                    else -> LocationPermissionState.Denied
                },
                userMessage = if (granted) {
                    UiMessage("Outdoor geospatial mode ready. Scan nearby buildings or terrain.")
                } else {
                    UiMessage("Outdoor Geospatial Math needs precise location for VPS and Streetscape Geometry.")
                }
            )
        }
    }

    fun selectArEngineMode(mode: ArEngineMode) {
        _uiState.update {
            val experience = when (mode) {
                ArEngineMode.PaperGraph -> MathArExperience.MarkerBasedGraph
                ArEngineMode.OutdoorGeospatialMath -> MathArExperience.OutdoorGeometry
                ArEngineMode.Indoor,
                ArEngineMode.SurfacePlacement,
                ArEngineMode.AirPlacement -> MathArExperience.MarkerlessObjects
            }
            it.copy(
                arEngineMode = mode,
                mathArExperience = experience,
                outdoorGeospatial = if (mode == ArEngineMode.OutdoorGeospatialMath) it.outdoorGeospatial else null,
                paperGraph = if (mode == ArEngineMode.PaperGraph) it.paperGraph else null,
                placementHitKind = PlacementHitKind.None,
                hasValidPlacementHit = false,
                sessionStatus = ArSessionStatus.Scanning,
                userMessage = UiMessage(
                    when (mode) {
                        ArEngineMode.Indoor -> "Indoor AR mode. Scan a table or floor for math placement."
                        ArEngineMode.PaperGraph -> "Marker-Based AR. Point at the G01 geometry marker."
                        ArEngineMode.AirPlacement -> "Air Placement mode. Tap open space, then move slowly to refine."
                        ArEngineMode.SurfacePlacement -> "Surface Placement mode. Aim at a stable real surface."
                        ArEngineMode.OutdoorGeospatialMath -> "Outdoor Geospatial Math. Scan buildings or terrain outside."
                    }
                )
            )
        }
    }

    fun selectMathArExperience(experience: MathArExperience) {
        val mode = when (experience) {
            MathArExperience.MarkerlessObjects,
            MathArExperience.Drawing2dTo3d,
            MathArExperience.SolarSystem,
            MathArExperience.SceneTools -> ArEngineMode.Indoor
            MathArExperience.MarkerBasedGraph -> ArEngineMode.PaperGraph
            MathArExperience.OutdoorGeometry -> ArEngineMode.OutdoorGeospatialMath
        }
        _uiState.update {
            it.copy(
                mathArExperience = experience,
                arEngineMode = mode,
                outdoorGeospatial = if (mode == ArEngineMode.OutdoorGeospatialMath) it.outdoorGeospatial else null,
                paperGraph = if (mode == ArEngineMode.PaperGraph) it.paperGraph else null,
                placementHitKind = PlacementHitKind.None,
                hasValidPlacementHit = false,
                sessionStatus = ArSessionStatus.Scanning,
                userMessage = UiMessage(experience.userMessage())
            )
        }
    }

    fun selectObject(type: MathObjectType) {
        val definition = DefaultMathObjectRegistry.getAllDefinitions().first { it.type == type }
        _uiState.update {
            it.copy(
                selectedObjectType = type,
                selectedDefinitionId = definition.definitionId,
                sceneInteractionMode = SceneInteractionMode.Place,
                userMessage = UiMessage("Tap a surface or open space to place the ${definition.displayName.lowercase()}.")
            )
        }
    }

    fun selectMarkerMathActivity(activity: MarkerMathActivity) {
        _uiState.update {
            val nextEquation = if (activity == MarkerMathActivity.FunctionGraph && it.liveEquation == "y = sin(x)" && it.markerGraphFunctions.isEmpty()) "" else it.liveEquation
            it.copy(
                markerMathActivity = activity,
                liveEquation = nextEquation,
                compiledArExpression = arMathEngine.compile(nextEquation),
                selectedObjectType = when (activity) {
                    MarkerMathActivity.Geometry2D,
                    MarkerMathActivity.CoordinateLab,
                    MarkerMathActivity.Transformations,
                    MarkerMathActivity.MeasurementLab,
                    MarkerMathActivity.Trigonometry -> MathObjectType.Triangle
                    MarkerMathActivity.Geometry3D -> MathObjectType.Cube
                    MarkerMathActivity.FunctionGraph -> MathObjectType.SineCurve
                },
                selectedDefinitionId = when (activity) {
                    MarkerMathActivity.Geometry2D,
                    MarkerMathActivity.CoordinateLab,
                    MarkerMathActivity.Transformations,
                    MarkerMathActivity.MeasurementLab,
                    MarkerMathActivity.Trigonometry -> "triangle"
                    MarkerMathActivity.Geometry3D -> "cube"
                    MarkerMathActivity.FunctionGraph -> "sine-curve"
                },
                sceneInteractionMode = SceneInteractionMode.Place,
                userMessage = UiMessage(if (activity == MarkerMathActivity.FunctionGraph) "Graph workspace ready. Enter a function and tap Add." else "${activity.label()} selected.")
            )
        }
    }

    fun selectMarkerTransformationTool(tool: MarkerTransformationTool) {
        _uiState.update {
            it.copy(
                markerMathActivity = MarkerMathActivity.Transformations,
                markerTransformTool = tool,
                markerTransformProgress = 1f,
                userMessage = UiMessage("${tool.label()} ready.")
            )
        }
    }

    fun setMarkerTransformProgress(progress: Float) {
        _uiState.update {
            it.copy(markerMathActivity = MarkerMathActivity.Transformations, markerTransformProgress = progress.coerceIn(0f, 1f))
        }
    }

    fun setMarkerTranslationX(value: Float) {
        _uiState.update { it.copy(markerTranslationX = value.coerceIn(-0.28f, 0.28f)) }
    }

    fun setMarkerTranslationY(value: Float) {
        _uiState.update { it.copy(markerTranslationY = value.coerceIn(-0.28f, 0.28f)) }
    }

    fun setMarkerRotationDegrees(value: Float) {
        _uiState.update { it.copy(markerRotationDegrees = value.coerceIn(-180f, 180f)) }
    }

    fun toggleMarkerRotationDirection() {
        _uiState.update { it.copy(markerRotationClockwise = !it.markerRotationClockwise) }
    }

    fun setMarkerReflectionLine(line: MarkerReflectionLine) {
        _uiState.update { it.copy(markerReflectionLine = line, markerTransformTool = MarkerTransformationTool.Reflection, markerMathActivity = MarkerMathActivity.Transformations) }
    }

    fun setMarkerDilationScale(value: Float) {
        _uiState.update { it.copy(markerDilationScale = value.coerceIn(0.2f, 2.6f)) }
    }

    fun setMarkerHorizontalStretch(value: Float) {
        _uiState.update { it.copy(markerHorizontalStretch = value.coerceIn(0.2f, 2.6f)) }
    }

    fun setMarkerVerticalStretch(value: Float) {
        _uiState.update { it.copy(markerVerticalStretch = value.coerceIn(0.2f, 2.6f)) }
    }

    fun setMarkerShear(value: Float) {
        _uiState.update { it.copy(markerShear = value.coerceIn(-1.2f, 1.2f)) }
    }

    fun undoMarkerTransformation() {
        _uiState.update {
            it.copy(
                markerTransformProgress = 0f,
                markerTranslationX = 0.16f,
                markerTranslationY = 0.1f,
                markerRotationDegrees = 45f,
                markerRotationClockwise = false,
                markerReflectionLine = MarkerReflectionLine.YAxis,
                markerDilationScale = 1.4f,
                markerHorizontalStretch = 1.4f,
                markerVerticalStretch = 0.7f,
                markerShear = 0.45f,
                userMessage = UiMessage("Transformation reset.")
            )
        }
    }

    fun setMarkerTrigAngle(value: Float) {
        _uiState.update { it.copy(markerMathActivity = MarkerMathActivity.Trigonometry, markerTrigAngleDegrees = value.coerceIn(0f, 360f)) }
    }

    fun addMarker2dShape(shape: Marker2dShapeTool) {
        _uiState.update { state ->
            val construction = when (shape) {
                Marker2dShapeTool.Point -> state.constructionGeometry.addMarkerPoint()
                Marker2dShapeTool.Line -> state.constructionGeometry.addMarkerLine()
                Marker2dShapeTool.Segment -> state.constructionGeometry.addMarkerLine()
                Marker2dShapeTool.Ray -> state.constructionGeometry.addMarkerLine()
                Marker2dShapeTool.Triangle -> state.constructionGeometry.addMarkerTriangle()
                Marker2dShapeTool.Square -> state.constructionGeometry.addMarkerSquare()
                Marker2dShapeTool.Rectangle -> state.constructionGeometry.addMarkerSquare()
                Marker2dShapeTool.Circle -> state.constructionGeometry.addMarkerCircle()
                Marker2dShapeTool.Ellipse -> state.constructionGeometry.addMarkerCircle()
                Marker2dShapeTool.Polygon -> state.constructionGeometry.addMarkerSquare()
                Marker2dShapeTool.RegularPolygon -> state.constructionGeometry.addMarkerSquare()
                Marker2dShapeTool.Angle -> state.constructionGeometry.addMarkerTriangle()
                Marker2dShapeTool.Arc -> state.constructionGeometry.addMarkerCircle()
                Marker2dShapeTool.Perpendicular -> state.constructionGeometry.addMarkerLine()
                Marker2dShapeTool.Parallel -> state.constructionGeometry.addMarkerLine()
                Marker2dShapeTool.Clear -> ConstructionGeometryState()
            }
            state.copy(
                markerMathActivity = MarkerMathActivity.Geometry2D,
                constructionGeometry = construction,
                resolvedConstructions = constructionEngine.resolve(construction),
                enabledMathArFeatures = state.enabledMathArFeatures + MathArFeature.MultiObjectConstraints,
                userMessage = UiMessage(if (shape == Marker2dShapeTool.Clear) "2D geometry cleared." else "${shape.label} added on G01.")
            )
        }
    }

    fun addMarker3dObject(definitionId: String) {
        val definition = DefaultMathObjectRegistry.getDefinition(definitionId) ?: return
        val current = _uiState.value
        val transform = Shape3dEngine.placementPlan(
            Shape3dPlacementRequest(
                definitionId = definitionId,
                anchorMode = Shape3dAnchorMode.MarkerImage,
                normalizedX = ((current.mathScene.objects.size % 3) - 1) * 0.42,
                normalizedZ = (current.mathScene.objects.size / 3) * 0.32 - 0.12,
                snapToGrid = true
            )
        ).transform
        mutate("Add ${definition.displayName}", "${definition.displayName} added on G01.") {
            SceneMutations.addObject(it, definitionId, transform)
        }
        _uiState.update {
            it.copy(
                markerMathActivity = MarkerMathActivity.Geometry3D,
                selectedObjectType = definition.type,
                selectedDefinitionId = definition.definitionId,
                sceneInteractionMode = SceneInteractionMode.Select
            )
        }
    }

    fun clearMarkerWorkspace() {
        val before = _uiState.value.mathScene
        val after = before.copy(objects = emptyList(), groups = emptyList(), annotations = emptyList(), updatedAt = System.currentTimeMillis())
        setSceneWithHistory(before, after, "Clear marker workspace", "Marker workspace cleared.")
        _uiState.update {
            it.copy(
                constructionGeometry = ConstructionGeometryState(),
                resolvedConstructions = emptyList(),
                pickedPoints = emptyList(),
                rulerAnchors = emptyList()
            )
        }
    }

    fun selectDefinition(definitionId: String) {
        val definition = DefaultMathObjectRegistry.getDefinition(definitionId) ?: return
        selectObject(definition.type)
    }

    fun onPlaneStatus(hasHit: Boolean, hitKind: PlacementHitKind = if (hasHit) PlacementHitKind.Plane else PlacementHitKind.None) {
        _uiState.update {
            val stability = stabilityEngine.evaluate(
                ArSensorState(
                    cameraTracking = it.trackingStatus,
                    anchorTracking = it.anchorTrackingStatus,
                    placementHitKind = hitKind,
                    hasPlacedObject = it.mathScene.objects.isNotEmpty(),
                    trackingMessage = it.trackingMessage
                )
            )
            it.copy(
                hasValidPlacementHit = hasHit,
                placementHitKind = hitKind,
                placementQuality = stability.placementQuality,
                recoveryMode = stability.recoveryMode,
                canRePlaceObject = stability.canRePlace,
                sessionStatus = when {
                    stability.recoveryMode == ArRecoveryMode.RePlacementRequired -> ArSessionStatus.TrackingLost
                    it.mathScene.objects.isNotEmpty() -> ArSessionStatus.ObjectPlaced
                    hasHit -> ArSessionStatus.ReadyToPlace
                    else -> ArSessionStatus.Scanning
                }
            )
        }
    }

    fun onTrackingStatus(camera: TrackingStatus, anchor: TrackingStatus, message: String) {
        _uiState.update {
            val stability = stabilityEngine.evaluate(
                ArSensorState(
                    cameraTracking = camera,
                    anchorTracking = anchor,
                    placementHitKind = it.placementHitKind,
                    hasPlacedObject = it.mathScene.objects.isNotEmpty(),
                    trackingMessage = message
                )
            )
            it.copy(
                trackingStatus = camera,
                anchorTrackingStatus = anchor,
                trackingMessage = stability.guidance,
                placementQuality = stability.placementQuality,
                recoveryMode = stability.recoveryMode,
                canRePlaceObject = stability.canRePlace,
                sessionStatus = when {
                    stability.recoveryMode == ArRecoveryMode.RePlacementRequired -> ArSessionStatus.TrackingLost
                    stability.recoveryMode == ArRecoveryMode.AnchorRecovering -> ArSessionStatus.TrackingLost
                    stability.recoveryMode == ArRecoveryMode.TrackingLimited -> ArSessionStatus.Scanning
                    it.mathScene.objects.isNotEmpty() -> ArSessionStatus.ObjectPlaced
                    it.hasValidPlacementHit -> ArSessionStatus.ReadyToPlace
                    else -> it.sessionStatus
                }
            )
        }
    }

    fun onSensorFusion(result: ArSensorFusionResult) {
        _uiState.update {
            val guidance = guidanceEngine.guide(
                ArGuidanceInput(
                    cameraTracking = it.trackingStatus,
                    anchorTracking = it.anchorTrackingStatus,
                    recoveryMode = it.recoveryMode,
                    placementQuality = result.quality,
                    placementHitKind = it.placementHitKind,
                    placementScore = result.score,
                    motionStable = result.motionStable,
                    lightStable = result.lightStable,
                    shouldPreferPlane = result.shouldPreferPlane,
                    hasPlacedObject = it.mathScene.objects.isNotEmpty(),
                    hasValidPlacementHit = it.hasValidPlacementHit
                )
            )
            it.copy(
                placementScore = result.score,
                placementQuality = result.quality,
                motionStable = result.motionStable,
                lightStable = result.lightStable,
                shouldDelayPlacement = result.shouldDelayPlacement,
                shouldPreferPlane = result.shouldPreferPlane,
                smartPlacementGuidance = result.guidance,
                guidanceHeadline = guidance.headline,
                guidanceInstruction = guidance.instruction,
                guidanceSeverity = guidance.severity,
                recommendedPlacementMode = guidance.recommendedMode,
                arDiagnostics = (guidance.diagnostics + result.diagnostics).distinct().take(12)
            )
        }
    }

    fun requestRePlacement() {
        _uiState.update {
            it.copy(
                recoveryMode = ArRecoveryMode.RePlacementRequired,
                canRePlaceObject = true,
                interactionMode = InteractionMode.Placement,
                sceneInteractionMode = SceneInteractionMode.Place,
                userMessage = UiMessage("Tap a stable surface to re-place the object.")
            )
        }
    }

    fun onPlacementMissed() {
        Log.d("AiStemAR", "Placement missed")
        _uiState.update {
            it.copy(
                userMessage = UiMessage(
                    if (it.arEngineMode == ArEngineMode.OutdoorGeospatialMath) {
                        "No building or terrain mesh under the tap yet. Move outside, face a Street View-covered area, and scan slowly."
                    } else {
                        "No ARCore plane under the tap yet. Aim at a textured floor/table, move the phone in a slow circle, then tap only when the surface highlight appears."
                    }
                )
            )
        }
    }

    fun onOutdoorGeospatialFrame(frameState: OutdoorGeospatialFrameState) {
        _uiState.update {
            it.copy(
                outdoorGeospatial = frameState,
                arDiagnostics = buildList {
                    addAll(it.arDiagnostics.filterNot { diagnostic -> diagnostic.startsWith("Outdoor mesh") || diagnostic.startsWith("Streetscape") })
                    add("Streetscape: buildings=${frameState.buildingCount} terrain=${frameState.terrainCount}")
                    add("Outdoor mesh quality: LOD1=${frameState.lod1Count} LOD2=${frameState.lod2Count}")
                    frameState.selectedMetrics?.let { metrics ->
                        add("Outdoor mesh height=${"%.1f".format(metrics.estimatedHeightMeters)}m angle=${"%.1f".format(metrics.angleOfElevationDegrees)}deg slope=${"%.2f".format(metrics.slopeToRoofline)}")
                    }
                }.take(12)
            )
        }
    }

    fun onPaperGraphFrame(frameState: PaperGraphFrameState) {
        val markerLessonId = frameState.bestLockedTarget?.name?.extractMarkerLessonId()
        _uiState.update {
            val lessonState = if (markerLessonId != null && markerLessonId != it.activeMarkerLessonId) {
                markerLessonState(markerLessonId, it)
            } else {
                it
            }
            it.copy(
                paperGraph = frameState,
                activeMarkerLessonId = lessonState.activeMarkerLessonId,
                markerLessonTitle = lessonState.markerLessonTitle,
                markerLessonSubtitle = lessonState.markerLessonSubtitle,
                markerLessonInteraction = lessonState.markerLessonInteraction,
                mathScene = lessonState.mathScene,
                constructionGeometry = lessonState.constructionGeometry,
                resolvedConstructions = lessonState.resolvedConstructions,
                selectedObjectType = lessonState.selectedObjectType,
                selectedDefinitionId = lessonState.selectedDefinitionId,
                userMessage = lessonState.userMessage,
                arDiagnostics = buildList {
                    addAll(it.arDiagnostics.filterNot { diagnostic -> diagnostic.startsWith("Marker") || diagnostic.startsWith("Augmented image") || diagnostic.startsWith("Paper graph") })
                    add("Marker targets: ${frameState.trackedTargets.size}")
                    add("Marker locked: ${frameState.hasLockedTarget}")
                    frameState.bestLockedTarget?.let { target ->
                        add("Marker best: ${target.name} score=${target.markerQualityScore}")
                    }
                    frameState.trackedTargets.firstOrNull()?.let { target ->
                        add("Augmented image: ${target.name} ${"%.2f".format(target.extentX)}m x ${"%.2f".format(target.extentZ)}m ${target.trackingMethod}")
                    }
                }.take(12)
            )
        }
    }

    fun zoomMarkerLesson(delta: Float) {
        _uiState.update {
            val next = (it.markerLessonInteraction.zoom * delta).coerceIn(0.55f, 2.8f)
            it.copy(markerLessonInteraction = it.markerLessonInteraction.copy(zoom = next))
        }
    }

    fun rotateMarkerLesson(deltaDegrees: Float) {
        _uiState.update {
            it.copy(
                markerLessonInteraction = it.markerLessonInteraction.copy(
                    rotationDegrees = com.indianservers.ai_stem.domain.mathematics.normalizeRotationDegrees(
                        it.markerLessonInteraction.rotationDegrees + deltaDegrees
                    )
                )
            )
        }
    }

    fun toggleMarkerLessonExpanded() {
        _uiState.update {
            it.copy(markerLessonInteraction = it.markerLessonInteraction.copy(expanded = !it.markerLessonInteraction.expanded))
        }
    }

    fun focusNextMarkerLessonElement() {
        _uiState.update {
            val count = it.markerLessonInteraction.elements.size.coerceAtLeast(1)
            it.copy(markerLessonInteraction = it.markerLessonInteraction.copy(focusIndex = (it.markerLessonInteraction.focusIndex + 1) % count))
        }
    }

    fun resetMarkerLessonInteraction() {
        _uiState.update {
            it.copy(markerLessonInteraction = it.markerLessonInteraction.copy(zoom = 1f, rotationDegrees = 0f, expanded = false, focusIndex = 0, step = 0))
        }
    }

    fun onPaperGraphCalibrationTap(point: PaperGraphCalibrationPoint) {
        _uiState.update {
            if (it.arEngineMode != ArEngineMode.PaperGraph || it.paperGraph?.hasLockedTarget != true) {
                return@update it.copy(userMessage = UiMessage("Scan and lock a geometry marker before calibration."))
            }
            val next = when (it.paperGraphCalibration.step) {
                PaperGraphCalibrationStep.Origin -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.XAxisPoint,
                    originLocked = true,
                    origin = point
                )
                PaperGraphCalibrationStep.XAxisPoint -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.YAxisPoint,
                    xAxisLocked = true,
                    xAxisPoint = point
                )
                PaperGraphCalibrationStep.YAxisPoint -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.Complete,
                    yAxisLocked = true,
                    yAxisPoint = point
                )
                PaperGraphCalibrationStep.Complete -> PaperGraphCalibrationState(origin = point, originLocked = true, step = PaperGraphCalibrationStep.XAxisPoint)
            }
            it.copy(
                paperGraphCalibration = next,
                selectedObjectType = MathObjectType.SineCurve,
                selectedDefinitionId = "sine-curve",
                userMessage = UiMessage(
                    if (next.isComplete) {
                        "Graph calibrated. Axes, scale, 3D surface and cross-section are locked to the paper."
                    } else {
                        "Locked ${it.paperGraphCalibration.step.prompt}. Tap ${next.step.prompt}."
                    }
                )
            )
        }
    }

    fun togglePaperGraphLayer(layer: PaperGraphLayer) {
        _uiState.update {
            val layers = if (layer in it.paperGraphLayers) it.paperGraphLayers - layer else it.paperGraphLayers + layer
            it.copy(paperGraphLayers = layers, userMessage = UiMessage("${layer.label()} ${if (layer in layers) "shown" else "hidden"}."))
        }
    }

    fun selectFeaturePhase(phase: ArFeaturePhase) {
        _uiState.update { it.copy(selectedFeaturePhase = phase, userMessage = UiMessage("${phase.label()} tools shown.")) }
    }

    fun toggleMathArFeature(feature: MathArFeature) {
        _uiState.update {
            val enabled = feature !in it.enabledMathArFeatures
            val features = if (enabled) it.enabledMathArFeatures + feature else it.enabledMathArFeatures - feature
            it.copy(
                enabledMathArFeatures = features,
                snappingEnabled = if (feature == MathArFeature.ObjectSnapping) enabled else it.snappingEnabled,
                coordinateGridLocked = if (feature == MathArFeature.CoordinateGridLocking) enabled else it.coordinateGridLocked,
                compareModeEnabled = if (feature == MathArFeature.CompareMode) enabled else it.compareModeEnabled,
                depthOcclusionPolishEnabled = if (feature == MathArFeature.DepthOcclusion) enabled else it.depthOcclusionPolishEnabled,
                graphAnimationEnabled = if (feature in setOf(MathArFeature.AreaUnderCurve, MathArFeature.VolumeBuilder)) true else it.graphAnimationEnabled,
                measurementsVisible = if (feature == MathArFeature.MeasurementRulerAnchors) true else it.measurementsVisible,
                formulaCardsVisible = if (feature == MathArFeature.LiveEquationEditing) true else it.formulaCardsVisible,
                userMessage = UiMessage("${feature.label()} ${if (enabled) "enabled" else "disabled"}.")
            )
        }
    }

    fun updateLiveEquation(equation: String) {
        _uiState.update {
            val trimmed = equation.take(160)
            val compiled = arMathEngine.compile(
                source = trimmed,
                existingSliders = it.arGraphSliders,
                domain = it.arGraphDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            val comparison = arMathEngine.compile(
                source = it.comparisonEquation,
                existingSliders = it.arGraphSliders,
                domain = it.arGraphDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            it.copy(
                markerMathActivity = if (it.arEngineMode == ArEngineMode.PaperGraph) MarkerMathActivity.FunctionGraph else it.markerMathActivity,
                liveEquation = trimmed,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, it.arGraphAnalysisFocusX, comparison),
                arGraphSliders = compiled.parameters,
                selectedObjectType = MathObjectType.SineCurve,
                selectedDefinitionId = "sine-curve",
                userMessage = UiMessage(compiled.message)
            )
        }
    }

    fun addMarkerGraphFunction() {
        _uiState.update { state ->
            val source = state.liveEquation.trim()
            if (source.isBlank()) {
                return@update state.copy(userMessage = UiMessage("Enter a function first."))
            }
            val compiled = arMathEngine.compile(
                source = source,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = state.arGraphQualityPreset
            )
            if (!compiled.isValid) {
                return@update state.copy(userMessage = UiMessage(compiled.message))
            }
            val id = "marker-graph-${System.currentTimeMillis()}-${state.markerGraphFunctions.size}"
            val graph = MarkerGraphFunctionState(
                id = id,
                source = source,
                compiled = compiled,
                sliders = compiled.parameters,
                selected = true,
                colorIndex = state.markerGraphFunctions.size
            )
            state.copy(
                markerMathActivity = MarkerMathActivity.FunctionGraph,
                markerGraphFunctions = state.markerGraphFunctions.map { it.copy(selected = false) } + graph,
                selectedMarkerGraphFunctionId = id,
                compiledArExpression = compiled,
                arGraphSliders = compiled.parameters,
                arGraphAnalysis = arMathEngine.analyze(compiled, state.arGraphAnalysisFocusX, state.compiledComparisonExpression),
                userMessage = UiMessage("${source} added to G01.")
            )
        }
    }

    fun selectMarkerGraphFunction(id: String) {
        _uiState.update { state ->
            val selected = state.markerGraphFunctions.firstOrNull { it.id == id } ?: return@update state
            state.copy(
                markerMathActivity = MarkerMathActivity.FunctionGraph,
                markerGraphFunctions = state.markerGraphFunctions.map { it.copy(selected = it.id == id) },
                selectedMarkerGraphFunctionId = id,
                liveEquation = selected.source,
                compiledArExpression = selected.compiled,
                arGraphSliders = selected.sliders,
                arGraphAnalysis = arMathEngine.analyze(selected.compiled, state.arGraphAnalysisFocusX, state.compiledComparisonExpression),
                userMessage = UiMessage("${selected.source} selected.")
            )
        }
    }

    fun toggleSelectedMarkerGraphVisibility() {
        _uiState.update { state ->
            val id = state.selectedMarkerGraphFunctionId ?: return@update state.copy(userMessage = UiMessage("Select a function first."))
            state.copy(
                markerGraphFunctions = state.markerGraphFunctions.map { graph ->
                    if (graph.id == id) graph.copy(visible = !graph.visible) else graph
                },
                userMessage = UiMessage("Function visibility changed.")
            )
        }
    }

    fun deleteSelectedMarkerGraphFunction() {
        _uiState.update { state ->
            val id = state.selectedMarkerGraphFunctionId ?: return@update state.copy(userMessage = UiMessage("Select a function first."))
            val remaining = state.markerGraphFunctions.filterNot { it.id == id }
            val nextSelected = remaining.lastOrNull()
            state.copy(
                markerGraphFunctions = remaining.map { it.copy(selected = it.id == nextSelected?.id) },
                selectedMarkerGraphFunctionId = nextSelected?.id,
                liveEquation = nextSelected?.source ?: state.liveEquation,
                compiledArExpression = nextSelected?.compiled ?: state.compiledArExpression,
                arGraphSliders = nextSelected?.sliders ?: emptyList(),
                userMessage = UiMessage("Function deleted.")
            )
        }
    }

    fun duplicateSelectedMarkerGraphFunction() {
        _uiState.update { state ->
            val selected = state.markerGraphFunctions.firstOrNull { it.id == state.selectedMarkerGraphFunctionId }
                ?: return@update state.copy(userMessage = UiMessage("Select a function first."))
            val copy = selected.copy(
                id = "marker-graph-${System.currentTimeMillis()}-${state.markerGraphFunctions.size}",
                source = selected.source,
                selected = true,
                colorIndex = state.markerGraphFunctions.size
            )
            state.copy(
                markerGraphFunctions = state.markerGraphFunctions.map { it.copy(selected = false) } + copy,
                selectedMarkerGraphFunctionId = copy.id,
                userMessage = UiMessage("Function duplicated.")
            )
        }
    }

    fun updateSelectedMarkerGraphParameter(symbol: String, value: Float) {
        _uiState.update { state ->
            val id = state.selectedMarkerGraphFunctionId ?: return@update state
            var selectedCompiled = state.compiledArExpression
            var selectedSliders = state.arGraphSliders
            val graphs = state.markerGraphFunctions.map { graph ->
                if (graph.id != id) return@map graph
                val sliders = graph.sliders.map { slider ->
                    if (slider.symbol == symbol) slider.copy(value = value.toDouble().coerceIn(slider.minimum, slider.maximum)) else slider
                }
                val compiled = arMathEngine.compile(
                    source = graph.source,
                    existingSliders = sliders,
                    domain = state.arGraphDomain,
                    qualityPreset = state.arGraphQualityPreset
                )
                selectedCompiled = compiled
                selectedSliders = compiled.parameters
                graph.copy(compiled = compiled, sliders = compiled.parameters)
            }
            state.copy(
                markerGraphFunctions = graphs,
                compiledArExpression = selectedCompiled,
                arGraphSliders = selectedSliders,
                arGraphAnalysis = arMathEngine.analyze(selectedCompiled, state.arGraphAnalysisFocusX, state.compiledComparisonExpression),
                userMessage = UiMessage("$symbol = ${"%.2f".format(value)}")
            )
        }
    }

    fun toggleMarkerGraphTrace(mode: MarkerGraphTraceMode) {
        _uiState.update { state ->
            val next = if (state.markerGraphTraceMode == mode) MarkerGraphTraceMode.Off else mode
            state.copy(
                markerGraphTraceMode = next,
                markerGraphShowDerivative = next == MarkerGraphTraceMode.Tangent,
                markerGraphShowIntegralArea = next == MarkerGraphTraceMode.Integral,
                userMessage = UiMessage("${next.name.lowercase().replaceFirstChar { it.uppercase() }} mode.")
            )
        }
    }

    fun setMarkerGraphTraceProgress(progress: Float) {
        _uiState.update { state ->
            val x = state.arGraphDomain.xMin + (state.arGraphDomain.xMax - state.arGraphDomain.xMin) * progress.coerceIn(0f, 1f)
            val selected = state.markerGraphFunctions.firstOrNull { it.id == state.selectedMarkerGraphFunctionId }
            state.copy(
                markerGraphTraceProgress = progress.coerceIn(0f, 1f),
                arGraphAnalysisFocusX = x,
                arGraphAnalysis = selected?.let { arMathEngine.analyze(it.compiled, x, state.compiledComparisonExpression) } ?: state.arGraphAnalysis
            )
        }
    }

    fun fitMarkerGraphView() {
        _uiState.update { state ->
            val domain = ArGraphDomain(xMin = -6.0, xMax = 6.0, yMin = -6.0, yMax = 6.0, valueClamp = 8.0)
            state.recompileMarkerGraphs(domain).copy(userMessage = UiMessage("Graph view fitted."))
        }
    }

    fun zoomMarkerGraph(factor: Double) {
        _uiState.update { state ->
            val domain = state.arGraphDomain.zoomed(factor)
            state.recompileMarkerGraphs(domain).copy(userMessage = UiMessage("Graph zoom updated."))
        }
    }

    fun panMarkerGraph(dx: Double, dy: Double) {
        _uiState.update { state ->
            val domain = state.arGraphDomain.copy(
                xMin = state.arGraphDomain.xMin + dx,
                xMax = state.arGraphDomain.xMax + dx,
                yMin = state.arGraphDomain.yMin + dy,
                yMax = state.arGraphDomain.yMax + dy
            )
            state.recompileMarkerGraphs(domain).copy(userMessage = UiMessage("Graph panned."))
        }
    }

    fun toggleMarkerGraphOption(option: MarkerGraphTraceMode) {
        toggleMarkerGraphTrace(option)
    }

    fun updateArGraphSlider(symbol: String, value: Float) {
        _uiState.update {
            val sliders = it.arGraphSliders.map { slider ->
                if (slider.symbol == symbol) slider.copy(value = value.toDouble().coerceIn(slider.minimum, slider.maximum)) else slider
            }
            val compiled = arMathEngine.compile(
                source = it.liveEquation,
                existingSliders = sliders,
                domain = it.arGraphDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            val comparison = arMathEngine.compile(
                source = it.comparisonEquation,
                existingSliders = sliders,
                domain = it.arGraphDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            it.copy(
                arGraphSliders = sliders,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, it.arGraphAnalysisFocusX, comparison),
                selectedObjectType = MathObjectType.SineCurve,
                selectedDefinitionId = "sine-curve",
                userMessage = UiMessage("$symbol = ${"%.2f".format(value)}")
            )
        }
    }

    fun setArGraphQualityPreset(qualityPreset: GraphQualityPreset) {
        _uiState.update {
            val compiled = arMathEngine.compile(
                source = it.liveEquation,
                existingSliders = it.arGraphSliders,
                domain = it.arGraphDomain,
                qualityPreset = qualityPreset
            )
            val comparison = arMathEngine.compile(
                source = it.comparisonEquation,
                existingSliders = it.arGraphSliders,
                domain = it.arGraphDomain,
                qualityPreset = qualityPreset
            )
            it.copy(
                arGraphQualityPreset = qualityPreset,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, it.arGraphAnalysisFocusX, comparison),
                userMessage = UiMessage("${qualityPreset.label()} graph quality selected.")
            )
        }
    }

    fun updateArGraphDomain(domain: ArGraphDomain) {
        _uiState.update {
            val safeDomain = domain.copy(
                xMin = minOf(domain.xMin, domain.xMax - 0.25),
                xMax = maxOf(domain.xMax, domain.xMin + 0.25),
                yMin = minOf(domain.yMin, domain.yMax - 0.25),
                yMax = maxOf(domain.yMax, domain.yMin + 0.25)
            )
            val compiled = arMathEngine.compile(
                source = it.liveEquation,
                existingSliders = it.arGraphSliders,
                domain = safeDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            val comparison = arMathEngine.compile(
                source = it.comparisonEquation,
                existingSliders = it.arGraphSliders,
                domain = safeDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            it.copy(
                arGraphDomain = safeDomain,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysisFocusX = it.arGraphAnalysisFocusX.coerceIn(safeDomain.xMin, safeDomain.xMax),
                arGraphAnalysis = arMathEngine.analyze(compiled, it.arGraphAnalysisFocusX.coerceIn(safeDomain.xMin, safeDomain.xMax), comparison),
                userMessage = UiMessage("AR graph domain updated.")
            )
        }
    }

    fun updateComparisonEquation(equation: String) {
        _uiState.update {
            val trimmed = equation.take(160)
            val comparison = arMathEngine.compile(
                source = trimmed,
                existingSliders = it.arGraphSliders,
                domain = it.arGraphDomain,
                qualityPreset = it.arGraphQualityPreset
            )
            it.copy(
                comparisonEquation = trimmed,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(it.compiledArExpression, it.arGraphAnalysisFocusX, comparison),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.RootVisualizer,
                userMessage = UiMessage("Comparison graph updated.")
            )
        }
    }

    fun setArGraphAnalysisFocus(progress: Float) {
        _uiState.update {
            val x = it.arGraphDomain.xMin + (it.arGraphDomain.xMax - it.arGraphDomain.xMin) * progress.coerceIn(0f, 1f)
            it.copy(
                arGraphAnalysisFocusX = x,
                arGraphAnalysis = arMathEngine.analyze(it.compiledArExpression, x, it.compiledComparisonExpression),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.TangentNormalTool,
                userMessage = UiMessage("Tangent focus x=${"%.2f".format(x)}")
            )
        }
    }

    fun applyAdvancedMathTool(toolId: String) {
        val tool = ArAdvancedMathTools.tools.firstOrNull { it.id == toolId } ?: return
        _uiState.update { state ->
            val compiled = arMathEngine.compile(
                source = tool.equationInsert,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = state.arGraphQualityPreset
            )
            val comparison = arMathEngine.compile(
                source = state.comparisonEquation,
                existingSliders = compiled.parameters,
                domain = state.arGraphDomain,
                qualityPreset = state.arGraphQualityPreset
            )
            val analysis = arMathEngine.analyze(compiled, state.arGraphAnalysisFocusX, comparison)
            val nextState = state.copy(
                selectedAdvancedToolId = tool.id,
                liveEquation = tool.equationInsert,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = analysis,
                arGraphSliders = compiled.parameters,
                selectedFeaturePhase = when (tool.kind) {
                    ArAdvancedToolKind.MacroConstruction,
                    ArAdvancedToolKind.ProofCheck -> ArFeaturePhase.EngineStrengthening
                    else -> ArFeaturePhase.GraphAnalysis
                },
                enabledMathArFeatures = state.enabledMathArFeatures + tool.kind.features(),
                graphAnimationEnabled = tool.kind in setOf(ArAdvancedToolKind.ParametricCurve, ArAdvancedToolKind.LocusTrace),
                labelsVisible = true,
                userMessage = UiMessage("${tool.title}: ${tool.arHint}")
            )
            nextState.copy(advancedCapabilityReport = nextState.advancedCapability())
        }
    }

    fun addGeneratedPointLabel() {
        _uiState.update {
            val index = it.pickedPoints.size + 1
            val x = -1f + index * 0.5f
            val y = arMathEngine.evaluate2d(it.compiledArExpression, x.toDouble()).toFloatOrZero()
            val point = ArPointLabel("P$index (${ "%.1f".format(x) }, ${ "%.2f".format(y) })", x, y, 0f)
            it.copy(
                pickedPoints = (it.pickedPoints + point).takeLast(6),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.PointPicker,
                labelsVisible = true,
                userMessage = UiMessage("Point ${point.label} added.")
            )
        }
    }

    fun addPointLabelAt(worldX: Float, worldY: Float, worldZ: Float) {
        _uiState.update {
            val index = it.pickedPoints.size + 1
            val raw = Vector3Value(worldX.toDouble(), worldY.toDouble(), worldZ.toDouble())
            val snap = arInteractionEngine.snapWorldPoint(raw, it.snapProfile(), it.graphSnapCandidates())
            val graphPoint = arInteractionEngine.pickGraphPoint(it.compiledArExpression, snap.position)
            val point = ArPointLabel(
                label = "P$index ${graphPoint.label}",
                x = snap.position.x.toFloat(),
                y = snap.position.y.toFloat(),
                z = snap.position.z.toFloat()
            )
            it.copy(
                pickedPoints = (it.pickedPoints + point).takeLast(6),
                pickedGraphPoints = (it.pickedGraphPoints + graphPoint).takeLast(6),
                lastSnapResult = snap,
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.PointPicker,
                labelsVisible = true,
                userMessage = UiMessage("Picked ${point.label}${if (snap.snapped) " snapped to ${snap.label}" else ""}.")
            )
        }
    }

    fun addRulerAnchor() {
        _uiState.update {
            val index = it.rulerAnchors.size + 1
            val anchor = ArPointLabel("R$index", index * 0.18f, 0f, index * 0.08f)
            it.copy(
                rulerAnchors = (it.rulerAnchors + anchor).takeLast(2),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MeasurementRulerAnchors,
                measurementsVisible = true,
                userMessage = UiMessage("Ruler anchor ${anchor.label} added.")
            )
        }
    }

    fun addRulerAnchorAt(worldX: Float, worldY: Float, worldZ: Float) {
        _uiState.update {
            val index = (it.rulerAnchors.size % 2) + 1
            val raw = Vector3Value(worldX.toDouble(), worldY.toDouble(), worldZ.toDouble())
            val snap = arInteractionEngine.snapWorldPoint(raw, it.snapProfile(), it.graphSnapCandidates())
            val anchor = ArPointLabel("R$index", snap.position.x.toFloat(), snap.position.y.toFloat(), snap.position.z.toFloat())
            val anchors = (it.rulerAnchors + anchor).takeLast(2)
            val measurement = if (anchors.size == 2) {
                arInteractionEngine.rulerMeasurement(
                    Vector3Value(anchors[0].x.toDouble(), anchors[0].y.toDouble(), anchors[0].z.toDouble()),
                    Vector3Value(anchors[1].x.toDouble(), anchors[1].y.toDouble(), anchors[1].z.toDouble())
                )
            } else {
                null
            }
            it.copy(
                rulerAnchors = anchors,
                rulerMeasurement = measurement,
                lastSnapResult = snap,
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MeasurementRulerAnchors,
                measurementsVisible = true,
                userMessage = UiMessage("Ruler anchor ${anchor.label} placed${if (snap.snapped) " on ${snap.label}" else ""}.")
            )
        }
    }

    fun recordPlacementPoint(worldX: Float, worldY: Float, worldZ: Float) {
        _uiState.update {
            val raw = Vector3Value(worldX.toDouble(), worldY.toDouble(), worldZ.toDouble())
            val snap = arInteractionEngine.snapWorldPoint(raw, it.snapProfile(), it.graphSnapCandidates())
            it.copy(
                lastPlacementPoint = ArPointLabel("anchor", snap.position.x.toFloat(), snap.position.y.toFloat(), snap.position.z.toFloat()),
                lastSnapResult = snap
            )
        }
    }

    fun setCompareOffset(offsetMeters: Float) {
        _uiState.update {
            it.copy(
                compareOffsetMeters = offsetMeters.coerceIn(0.18f, 0.8f),
                compareModeEnabled = true,
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.CompareMode
            )
        }
    }

    fun selectGestureHandle(handle: ArGestureHandle) {
        _uiState.update {
            it.copy(
                selectedGestureHandle = handle,
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.GestureHandles,
                sceneInteractionMode = when (handle) {
                    ArGestureHandle.RotateY -> SceneInteractionMode.Rotate
                    ArGestureHandle.UniformScale,
                    ArGestureHandle.StretchX,
                    ArGestureHandle.StretchZ -> SceneInteractionMode.Scale
                    else -> SceneInteractionMode.Move
                },
                userMessage = UiMessage("${handle.label} handle selected.")
            )
        }
    }

    fun setTransformHandleStep(step: Float) {
        _uiState.update { it.copy(transformHandleStep = step.coerceIn(0.01f, 0.3f)) }
    }

    fun applySelectedGestureHandle(direction: Float = 1f) {
        val current = _uiState.value
        val objectId = current.mathScene.primarySelectedObject?.id ?: return
        val step = current.transformHandleStep.toDouble() * direction.toDouble()
        val before = current.mathScene
        val result = when (current.selectedGestureHandle) {
            ArGestureHandle.MoveX -> DefaultObjectTransformService.move(before, objectId, TransformMovement(Vector3Value(step, 0.0, 0.0)))
            ArGestureHandle.MoveY,
            ArGestureHandle.Lift -> DefaultObjectTransformService.move(before, objectId, TransformMovement(Vector3Value(0.0, step, 0.0)))
            ArGestureHandle.MoveZ -> DefaultObjectTransformService.move(before, objectId, TransformMovement(Vector3Value(0.0, 0.0, step)))
            ArGestureHandle.RotateY -> DefaultObjectTransformService.rotate(before, objectId, TransformRotation(yDegrees = step * 240.0))
            ArGestureHandle.UniformScale -> DefaultObjectTransformService.scale(before, objectId, TransformScaling(1.0 + step))
            ArGestureHandle.StretchX -> DefaultObjectTransformService.scale(before, objectId, TransformScaling(1.0 + step, 1.0, 1.0))
            ArGestureHandle.StretchZ -> DefaultObjectTransformService.scale(before, objectId, TransformScaling(1.0, 1.0, 1.0 + step))
        }
        val success = result as? com.indianservers.ai_stem.domain.scene.SceneMutationResult.Success ?: return
        setSceneWithHistory(before, success.scene, current.selectedGestureHandle.label, success.message)
        _uiState.update {
            it.copy(
                placedObject = it.mathScene.primarySelectedObject?.let { obj ->
                    PlacedMathObject(
                        id = obj.id,
                        type = obj.objectType,
                        rotationDegrees = obj.transform.rotation.y.toFloat(),
                        scaleFactor = obj.transform.scale.x.toFloat(),
                        selected = obj.interactionState.selected,
                        transformRevision = (it.placedObject?.transformRevision ?: 0) + 1
                    )
                },
                userMessage = UiMessage("${current.selectedGestureHandle.label} applied.")
            )
        }
    }

    fun addConstructionPointFromSelection() {
        _uiState.update {
            val position = when {
                it.pickedGraphPoints.isNotEmpty() -> it.pickedGraphPoints.last().worldPosition
                it.rulerAnchors.isNotEmpty() -> it.rulerAnchors.last().let { anchor -> Vector3Value(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble()) }
                it.lastPlacementPoint != null -> Vector3Value(it.lastPlacementPoint.x.toDouble(), it.lastPlacementPoint.y.toDouble(), it.lastPlacementPoint.z.toDouble())
                else -> Vector3Value((it.constructionGeometry.points.size % 3) * 0.12, 0.02, (it.constructionGeometry.points.size / 3) * 0.12)
            }
            val construction = constructionEngine.addPoint(it.constructionGeometry, position)
            it.copy(
                constructionGeometry = construction,
                resolvedConstructions = constructionEngine.resolve(construction),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MultiObjectConstraints,
                labelsVisible = true,
                userMessage = UiMessage("Construction point added.")
            )
        }
    }

    fun createConstructionLine() = updateConstruction("Line through points") { constructionEngine.createLineThroughSelected(it) }
    fun createConstructionSegment() = updateConstruction("Segment through points") { constructionEngine.createSegmentThroughSelected(it) }
    fun createConstructionVector() = updateConstruction("Vector AB") { constructionEngine.createVectorBetweenSelected(it) }
    fun createConstructionPlane() = updateConstruction("Plane through 3 points") { constructionEngine.createPlaneThroughSelected(it) }
    fun createConstructionCircle() = updateConstruction("Circle from center + point") { constructionEngine.createCircleFromSelected(it) }
    fun createConstructionPolygon() = updateConstruction("Polygon through points") { constructionEngine.createPolygonFromSelected(it) }
    fun createConstructionMidpoint() = updateConstruction("Midpoint") { constructionEngine.createMidpoint(it) }
    fun createConstructionParallel() = updateConstruction("Parallel through point") { constructionEngine.createParallelThroughSelected(it) }
    fun createConstructionPerpendicular() = updateConstruction("Perpendicular through point") { constructionEngine.createPerpendicularThroughSelected(it) }

    fun addConstructionConstraint(kind: ConstructionConstraintKind) {
        _uiState.update {
            val objectIds = it.constructionGeometry.objects.takeLast(2).map { obj -> obj.id }
            if (objectIds.isEmpty()) {
                it.copy(userMessage = UiMessage("Create construction objects before adding constraints."))
            } else {
                val construction = constructionEngine.addConstraint(it.constructionGeometry, kind, objectIds)
                it.copy(
                    constructionGeometry = construction,
                    resolvedConstructions = constructionEngine.resolve(construction),
                    enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MultiObjectConstraints,
                    userMessage = UiMessage("${kind.name.lowercase().replaceFirstChar { ch -> ch.uppercase() }} constraint added.")
                )
            }
        }
    }

    private fun updateConstruction(message: String, block: (com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState) -> com.indianservers.ai_stem.domain.geometry.ConstructionGeometryState) {
        _uiState.update {
            runCatching { block(it.constructionGeometry) }
                .map { construction ->
                    it.copy(
                        constructionGeometry = construction,
                        resolvedConstructions = constructionEngine.resolve(construction),
                        enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MultiObjectConstraints,
                        userMessage = UiMessage(message)
                    )
                }
                .getOrElse { error -> it.copy(userMessage = UiMessage(error.message ?: "Add more construction points.")) }
        }
    }

    fun selectFloatingTool(tool: FloatingMathTool) {
        _uiState.update {
            it.copy(
                activeFloatingTool = tool,
                axesVisible = if (tool == FloatingMathTool.Axes) !it.axesVisible else it.axesVisible,
                gridVisible = if (tool == FloatingMathTool.Grid) !it.gridVisible else it.gridVisible,
                labelsVisible = if (tool == FloatingMathTool.Labels) !it.labelsVisible else it.labelsVisible,
                formulaCardsCollapsed = if (tool == FloatingMathTool.Formula && it.formulaCardsVisible) !it.formulaCardsCollapsed else it.formulaCardsCollapsed,
                formulaCardsVisible = if (tool == FloatingMathTool.Formula && !it.formulaCardsVisible) true else it.formulaCardsVisible,
                measurementsVisible = if (tool == FloatingMathTool.Measure) !it.measurementsVisible else it.measurementsVisible,
                slicePlaneVisible = if (tool == FloatingMathTool.Slice) !it.slicePlaneVisible else it.slicePlaneVisible,
                graphAnimationEnabled = if (tool == FloatingMathTool.Animate) !it.graphAnimationEnabled else it.graphAnimationEnabled,
                captureRequested = tool == FloatingMathTool.Capture,
                userMessage = UiMessage(tool.userMessage())
            )
        }
    }

    fun setGraphColorMap(colorMap: GraphColorMap) {
        _uiState.update { it.copy(graphColorMap = colorMap, userMessage = UiMessage("Graph colors show ${colorMap.label.lowercase()}.")) }
    }

    fun setFunction3dTransformMode(mode: Function3dTransformMode) {
        _uiState.update {
            it.copy(
                function3dTransformMode = mode,
                selectedObjectType = MathObjectType.SineCurve,
                selectedDefinitionId = "sine-curve",
                graphAnimationEnabled = when (mode) {
                    Function3dTransformMode.Surface -> it.graphAnimationEnabled
                    Function3dTransformMode.Extrusion -> true
                    Function3dTransformMode.SolidOfRevolution -> true
                    Function3dTransformMode.TangentPlane -> true
                    Function3dTransformMode.CrossSectionSlices -> true
                },
                slicePlaneVisible = mode == Function3dTransformMode.CrossSectionSlices || it.slicePlaneVisible,
                userMessage = UiMessage("${mode.label()} selected for the 2D function.")
            )
        }
    }

    fun setGraphAnimationProgress(progress: Float) {
        _uiState.update { it.copy(graphAnimationProgress = progress.coerceIn(0f, 1f)) }
    }

    fun setGraphAnimationMode(mode: GraphAnimationMode) {
        _uiState.update {
            it.copy(
                graphAnimationMode = mode,
                graphAnimationEnabled = true,
                userMessage = UiMessage("${mode.label()} animation selected.")
            )
        }
    }

    fun setGraphSlicePosition(position: Float) {
        _uiState.update {
            it.copy(
                graphSlicePosition = position.coerceIn(0f, 1f),
                slicePlaneVisible = true
            )
        }
    }

    fun advancePaperGraphCalibration() {
        _uiState.update {
            val next = when (it.paperGraphCalibration.step) {
                PaperGraphCalibrationStep.Origin -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.XAxisPoint,
                    originLocked = true
                )
                PaperGraphCalibrationStep.XAxisPoint -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.YAxisPoint,
                    xAxisLocked = true
                )
                PaperGraphCalibrationStep.YAxisPoint -> it.paperGraphCalibration.copy(
                    step = PaperGraphCalibrationStep.Complete,
                    yAxisLocked = true
                )
                PaperGraphCalibrationStep.Complete -> PaperGraphCalibrationState()
            }
            it.copy(
                paperGraphCalibration = next,
                userMessage = UiMessage(
                    if (next.isComplete) {
                        "Paper graph calibrated. Place a 3D surface or slice plane."
                    } else {
                        "Tap ${next.step.prompt.lowercase()} on the paper graph."
                    }
                )
            )
        }
    }

    fun acknowledgeCapture() {
        _uiState.update { it.copy(captureRequested = false, userMessage = UiMessage("AR capture overlay prepared.")) }
    }

    fun capturePersistentAnchor() {
        _uiState.update { state ->
            val anchor = state.productionAnchorRecord()
            state.copy(
                mathScene = state.mathScene.copy(persistentAnchor = anchor),
                loadedSceneNeedsPlacement = false,
                enabledMathArFeatures = state.enabledMathArFeatures + MathArFeature.ScenePersistence,
                userMessage = UiMessage("${anchor.label} saved. ${anchor.restoreHint}")
            )
        }
    }

    fun setDepthOcclusionMode(mode: ArDepthOcclusionMode) {
        _uiState.update { state ->
            val settings = state.productionSettings().copy(depthMode = mode)
            state.copy(
                depthOcclusionMode = mode,
                depthOcclusionPolishEnabled = mode != ArDepthOcclusionMode.Off,
                enabledMathArFeatures = if (mode == ArDepthOcclusionMode.Off) {
                    state.enabledMathArFeatures - MathArFeature.DepthOcclusion
                } else {
                    state.enabledMathArFeatures + MathArFeature.DepthOcclusion
                },
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                userMessage = UiMessage("${mode.label()} selected for AR depth.")
            )
        }
    }

    fun setPerformanceProfile(profile: ArPerformanceProfile) {
        _uiState.update { state ->
            val quality = profile.toGraphQualityPreset()
            val compiled = arMathEngine.compile(
                source = state.liveEquation,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = quality
            )
            val comparison = arMathEngine.compile(
                source = state.comparisonEquation,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = quality
            )
            val settings = state.productionSettings().copy(performanceProfile = profile)
            state.copy(
                performanceProfile = profile,
                arGraphQualityPreset = quality,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, state.arGraphAnalysisFocusX, comparison),
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                userMessage = UiMessage("${profile.label()} production profile applied.")
            )
        }
    }

    fun setMeshDensity(value: Float) {
        _uiState.update { state ->
            val density = value.coerceIn(0.1f, 1f)
            val settings = state.productionSettings().copy(meshDensity = density)
            state.copy(
                meshDensity = density,
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                userMessage = UiMessage("Mesh density ${(density * 100f).toInt()}%.")
            )
        }
    }

    fun setMaxSceneObjects(value: Float) {
        _uiState.update { state ->
            val maxObjects = value.toInt().coerceIn(4, 128)
            val settings = state.productionSettings().copy(maxSceneObjects = maxObjects)
            state.copy(
                maxSceneObjects = maxObjects,
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                userMessage = UiMessage("Scene object budget set to $maxObjects.")
            )
        }
    }

    fun markScreenshotReady() {
        _uiState.update { state ->
            val settings = state.productionSettings().copy(screenshotReady = true)
            state.copy(
                captureRequested = true,
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                userMessage = UiMessage("Scene staged for screenshot/export.")
            )
        }
    }

    fun exportArScenePackage() {
        _uiState.update { state ->
            val scene = state.sceneWithProductionState()
            val sharePackage = arSceneShareExporter.export(
                scene = scene,
                equation = state.liveEquation,
                comparisonEquation = state.comparisonEquation,
                constructionCount = state.constructionGeometry.objects.size,
                graphQuality = state.arGraphQualityPreset.label(),
                colorMap = state.graphColorMap.label
            )
            state.copy(
                mathScene = scene,
                exportedScenePackage = sharePackage,
                enabledMathArFeatures = state.enabledMathArFeatures + MathArFeature.ScenePersistence,
                userMessage = UiMessage("Export package ready: ${sharePackage.fileName}")
            )
        }
    }

    fun clearExportPackage() {
        _uiState.update { it.copy(exportedScenePackage = null) }
    }

    fun applyArSceneTemplate(templateId: String) {
        val template = ArSceneTemplates.templates.firstOrNull { it.id == templateId } ?: return
        _uiState.update { state ->
            val mode = template.engineMode.toArEngineMode()
            val quality = template.performanceProfile.toGraphQualityPreset()
            val compiled = arMathEngine.compile(
                source = template.equation,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = quality
            )
            val comparison = arMathEngine.compile(
                source = template.comparisonEquation,
                existingSliders = compiled.parameters,
                domain = state.arGraphDomain,
                qualityPreset = quality
            )
            val settings = state.productionSettings().copy(
                depthMode = template.depthMode,
                performanceProfile = template.performanceProfile
            )
            state.copy(
                selectedTemplateId = template.id,
                arEngineMode = mode,
                mathArExperience = mode.toMathArExperience(),
                liveEquation = template.equation,
                comparisonEquation = template.comparisonEquation,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, state.arGraphAnalysisFocusX, comparison),
                arGraphSliders = compiled.parameters,
                arGraphQualityPreset = quality,
                depthOcclusionMode = template.depthMode,
                depthOcclusionPolishEnabled = template.depthMode != ArDepthOcclusionMode.Off,
                performanceProfile = template.performanceProfile,
                graphAnimationEnabled = template.id in setOf("volume-builder", "vector-field-lab"),
                slicePlaneVisible = template.id == "paper-graph-lab",
                mathScene = state.mathScene.copy(arProductionSettings = settings),
                enabledMathArFeatures = state.enabledMathArFeatures + setOf(
                    MathArFeature.ScenePersistence,
                    MathArFeature.PrecisionConfidenceHud
                ) + if (template.depthMode != ArDepthOcclusionMode.Off) setOf(MathArFeature.DepthOcclusion) else emptySet(),
                userMessage = UiMessage("${template.title} template loaded.")
            )
        }
    }

    fun selectArWorkflow(workflowId: String) {
        val template = ArWorkflowTemplates.templates.firstOrNull { it.id == workflowId } ?: return
        _uiState.update { state ->
            val mode = template.recommendedMode.toArEngineMode()
            val compiled = arMathEngine.compile(
                source = template.equation,
                existingSliders = state.arGraphSliders,
                domain = state.arGraphDomain,
                qualityPreset = state.arGraphQualityPreset
            )
            val comparison = arMathEngine.compile(
                source = template.comparisonEquation,
                existingSliders = compiled.parameters,
                domain = state.arGraphDomain,
                qualityPreset = state.arGraphQualityPreset
            )
            val progress = ArWorkflowProgress(templateId = workflowId)
            val nextState = state.copy(
                activeWorkflowId = workflowId,
                workflowProgress = progress,
                arEngineMode = mode,
                mathArExperience = mode.toMathArExperience(),
                liveEquation = template.equation,
                comparisonEquation = template.comparisonEquation,
                compiledArExpression = compiled,
                compiledComparisonExpression = comparison,
                arGraphAnalysis = arMathEngine.analyze(compiled, state.arGraphAnalysisFocusX, comparison),
                arGraphSliders = compiled.parameters,
                selectedFeaturePhase = ArFeaturePhase.WorkflowStudio,
                enabledMathArFeatures = state.enabledMathArFeatures + setOf(
                    MathArFeature.GuidedWorkflow,
                    MathArFeature.PrecisionConfidenceHud
                ),
                userMessage = UiMessage("${template.title} workflow started.")
            )
            nextState.copy(workflowEvaluation = arWorkflowEngine.evaluate(template, progress, nextState.workflowSignal()))
        }
    }

    fun completeWorkflowStep() {
        _uiState.update { state ->
            val template = state.activeWorkflowTemplate()
            val progress = arWorkflowEngine.completeCurrent(state.workflowProgress, template)
            state.copy(
                workflowProgress = progress,
                workflowEvaluation = arWorkflowEngine.evaluate(template, progress, state.workflowSignal()),
                enabledMathArFeatures = state.enabledMathArFeatures + MathArFeature.GuidedWorkflow,
                userMessage = UiMessage(progress.currentStep(template)?.title ?: "Workflow complete.")
            )
        }
    }

    fun advanceWorkflowStep() {
        _uiState.update { state ->
            val template = state.activeWorkflowTemplate()
            val progress = arWorkflowEngine.advance(state.workflowProgress, template)
            state.copy(
                workflowProgress = progress,
                workflowEvaluation = arWorkflowEngine.evaluate(template, progress, state.workflowSignal()),
                userMessage = UiMessage(progress.currentStep(template)?.actionHint ?: "Workflow complete.")
            )
        }
    }

    fun refreshWorkflowEvaluation() {
        _uiState.update { state ->
            val template = state.activeWorkflowTemplate()
            state.copy(workflowEvaluation = arWorkflowEngine.evaluate(template, state.workflowProgress, state.workflowSignal()))
        }
    }

    fun exportArActivityPackage() {
        _uiState.update { state ->
            val template = state.activeWorkflowTemplate()
            val evaluation = arWorkflowEngine.evaluate(template, state.workflowProgress, state.workflowSignal())
            val activity = arWorkflowEngine.exportActivity(
                template = template,
                progress = state.workflowProgress,
                evaluation = evaluation,
                scenePackage = state.exportedScenePackage
            )
            state.copy(
                workflowEvaluation = evaluation,
                exportedActivityPackage = activity,
                enabledMathArFeatures = state.enabledMathArFeatures + setOf(MathArFeature.ActivityExport, MathArFeature.EvidenceRecorder),
                userMessage = UiMessage("Activity package ready: ${activity.fileName}")
            )
        }
    }

    fun onRuntimeError(stage: String, throwable: Throwable) {
        Log.e("AiStemAR", stage, throwable)
        _uiState.update {
            it.copy(
                diagnosticsVisible = true,
                arDiagnostics = listOf(
                    "Runtime error: $stage",
                    "Type: ${throwable::class.java.simpleName}",
                    "Message: ${throwable.message ?: "No message"}",
                    "Use: adb logcat -s AiStemAR AndroidRuntime"
                ) + it.arDiagnostics.take(8),
                userMessage = UiMessage("$stage. Logs enabled with tag AiStemAR.")
            )
        }
    }

    fun onAnchorEstablished() {
        if (_uiState.value.arEngineMode == ArEngineMode.PaperGraph && _uiState.value.activeMarkerLessonId != null) {
            _uiState.update {
                it.copy(
                    sessionStatus = ArSessionStatus.ObjectSelected,
                    recoveryMode = ArRecoveryMode.Normal,
                    canRePlaceObject = true,
                    placementQuality = PlacementQuality.Excellent,
                    interactionMode = InteractionMode.ObjectSelected,
                    sceneInteractionMode = SceneInteractionMode.Select,
                    userMessage = UiMessage("${it.activeMarkerLessonId} lesson anchored.")
                )
            }
            return
        }
        if (_uiState.value.mathScene.objects.isEmpty()) {
            val current = _uiState.value
            val transform = if (current.arEngineMode == ArEngineMode.PaperGraph) {
                Shape3dEngine.placementPlan(
                    Shape3dPlacementRequest(
                        definitionId = current.selectedDefinitionId,
                        anchorMode = current.arEngineMode.toShape3dAnchorMode(),
                        normalizedX = 0.0,
                        normalizedZ = 0.0,
                        snapToGrid = true
                    )
                ).transform
            } else {
                ObjectTransform()
            }
            mutate("Place object") { SceneMutations.addObject(it, current.selectedDefinitionId, transform) }
        }
        val placed = _uiState.value.mathScene.primarySelectedObject ?: _uiState.value.mathScene.objects.firstOrNull()
        _uiState.update {
            it.copy(
                placedObject = placed?.let { obj -> PlacedMathObject(obj.id, obj.objectType) },
                sessionStatus = ArSessionStatus.ObjectSelected,
                recoveryMode = ArRecoveryMode.Normal,
                canRePlaceObject = true,
                placementQuality = if (it.placementHitKind == PlacementHitKind.Plane) PlacementQuality.Excellent else PlacementQuality.Good,
                interactionMode = InteractionMode.ObjectSelected,
                sceneInteractionMode = SceneInteractionMode.Select,
                userMessage = UiMessage(if (it.mathScene.objects.size <= 1) "${placed?.displayName ?: "Object"} anchored." else "Scene re-anchored.")
            )
        }
    }

    fun selectSceneObject(objectId: String, multi: Boolean = false) {
        _uiState.update {
            val scene = if (multi && it.experienceMode == ExperienceMode.Advanced) {
                SceneMutations.toggleSelection(it.mathScene, objectId)
            } else {
                SceneMutations.selectOnly(it.mathScene, objectId)
            }
            it.copy(mathScene = scene, inspectorVisible = true, userMessage = UiMessage(scene.primarySelectedObject?.displayName ?: "Selection cleared"))
        }
    }

    fun setExperienceMode(mode: ExperienceMode) {
        _uiState.update {
            it.copy(
                experienceMode = mode,
                sceneInteractionMode = if (mode == ExperienceMode.Beginner) SceneInteractionMode.Select else it.sceneInteractionMode,
                userMessage = UiMessage(if (mode == ExperienceMode.Beginner) "Beginner mode" else "Advanced mode")
            )
        }
    }

    fun setSceneInteractionMode(mode: SceneInteractionMode) {
        _uiState.update { it.copy(sceneInteractionMode = mode) }
    }

    fun resetTransform() {
        val objectId = _uiState.value.mathScene.primarySelectedObject?.id ?: return
        val before = _uiState.value.mathScene
        val result = DefaultObjectTransformService.reset(before, objectId)
        if (result is com.indianservers.ai_stem.domain.scene.SceneMutationResult.Success) {
            setSceneWithHistory(before, result.scene, "Reset transform", result.message)
        }
    }

    fun applyTransform(scaleFactor: Float, rotationDeltaDegrees: Float) {
        val objectId = _uiState.value.mathScene.primarySelectedObject?.id ?: return
        val before = _uiState.value.mathScene
        val rotated = DefaultObjectTransformService.rotate(before, objectId, TransformRotation(yDegrees = rotationDeltaDegrees.toDouble()))
        val sceneAfterRotation = (rotated as? com.indianservers.ai_stem.domain.scene.SceneMutationResult.Success)?.scene ?: before
        val scaled = DefaultObjectTransformService.scale(sceneAfterRotation, objectId, TransformScaling(scaleFactor.toDouble()))
        val after = (scaled as? com.indianservers.ai_stem.domain.scene.SceneMutationResult.Success)?.scene ?: sceneAfterRotation
        setSceneWithHistory(before, after, "Transform object", "Object updated")
    }

    fun deleteObject() {
        mutate("Delete object", "Object deleted") { SceneMutations.deleteSelected(it) }
    }

    fun duplicateSelected() {
        mutate("Duplicate object", "Copied") { SceneMutations.duplicateSelected(it) }
    }

    fun toggleLockSelected() {
        val selected = _uiState.value.mathScene.primarySelectedObject ?: return
        mutate(if (selected.interactionState.locked) "Unlock object" else "Lock object") {
            SceneMutations.setLocked(it, selected.id, !selected.interactionState.locked)
        }
        _uiState.update { it.copy(userMessage = UiMessage(if (selected.interactionState.locked) "Object unlocked" else "Object locked")) }
    }

    fun toggleHideSelected() {
        val selected = _uiState.value.mathScene.primarySelectedObject ?: return
        mutate(if (selected.visibility.visible) "Hide object" else "Show object") {
            SceneMutations.setVisible(it, selected.id, !selected.visibility.visible)
        }
        _uiState.update { it.copy(userMessage = UiMessage(if (selected.visibility.visible) "Object hidden" else "Object shown")) }
    }

    fun groupSelected() {
        runCatching { mutate("Group objects") { SceneMutations.groupSelected(it) } }
            .onFailure { _uiState.update { state -> state.copy(userMessage = UiMessage("Select at least two objects to group.")) } }
    }

    fun ungroupSelected() {
        mutate("Ungroup objects") { SceneMutations.ungroupSelected(it) }
    }

    fun renameSelected(name: String) {
        val selected = _uiState.value.mathScene.primarySelectedObject ?: return
        runCatching { mutate("Rename object") { SceneMutations.rename(it, selected.id, name) } }
            .onFailure { _uiState.update { state -> state.copy(userMessage = UiMessage("Enter a name for this object.")) } }
    }

    fun updateSelectedParameter(parameterId: String, value: MathParameterValue) {
        val selected = _uiState.value.mathScene.primarySelectedObject ?: return
        runCatching { mutate("Edit parameter") { SceneMutations.updateParameter(it, selected.id, parameterId, value) } }
            .onFailure { _uiState.update { state -> state.copy(userMessage = UiMessage("That value is outside the allowed range.")) } }
    }

    fun clearScene() {
        mutate("Clear scene", "Scene cleared") { SceneMutations.clear(it) }
    }

    fun undo() {
        _uiState.update {
            val (scene, history) = it.history.undo(it.mathScene)
            it.copy(mathScene = scene, history = history, userMessage = history.lastMessage?.let(::UiMessage))
        }
    }

    fun redo() {
        _uiState.update {
            val (scene, history) = it.history.redo(it.mathScene)
            it.copy(mathScene = scene, history = history, userMessage = history.lastMessage?.let(::UiMessage))
        }
    }

    fun saveScene(name: String = _uiState.value.mathScene.name) {
        val repository = sceneRepository ?: return
        val sceneToSave = _uiState.value.sceneWithProductionState()
        when (val result = repository.save(sceneToSave, name)) {
            is SceneStorageResult.Success -> _uiState.update {
                it.copy(
                    mathScene = result.scene,
                    savedScenes = repository.listScenes(),
                    enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.ScenePersistence,
                    userMessage = UiMessage("Scene saved with AR production settings")
                )
            }
            is SceneStorageResult.Failure -> _uiState.update { it.copy(userMessage = UiMessage(result.userMessage)) }
        }
    }

    fun loadScene(sceneId: String) {
        val repository = sceneRepository ?: return
        when (val result = repository.load(sceneId)) {
            is SceneStorageResult.Success -> _uiState.update {
                val production = result.scene.arProductionSettings
                it.copy(
                    mathScene = result.scene,
                    loadedSceneNeedsPlacement = true,
                    savedScenesVisible = false,
                    depthOcclusionMode = production.depthMode,
                    depthOcclusionPolishEnabled = production.depthMode != ArDepthOcclusionMode.Off,
                    performanceProfile = production.performanceProfile,
                    meshDensity = production.meshDensity,
                    maxSceneObjects = production.maxSceneObjects,
                    enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.ScenePersistence +
                        if (production.depthMode != ArDepthOcclusionMode.Off) setOf(MathArFeature.DepthOcclusion) else emptySet(),
                    userMessage = UiMessage("${result.scene.persistentAnchor.restoreHint} Tap a surface to restore this scene.")
                )
            }
            is SceneStorageResult.Failure -> _uiState.update { it.copy(userMessage = UiMessage(result.userMessage)) }
        }
    }

    fun deleteSavedScene(sceneId: String) {
        val repository = sceneRepository ?: return
        repository.delete(sceneId)
        _uiState.update { it.copy(savedScenes = repository.listScenes(), userMessage = UiMessage("Saved scene deleted")) }
    }

    fun togglePlanes() {
        _uiState.update { it.copy(planesVisible = !it.planesVisible) }
    }

    fun toggleInspector() {
        _uiState.update { it.copy(inspectorVisible = !it.inspectorVisible) }
    }

    fun toggleLayers() {
        _uiState.update { it.copy(layersVisible = !it.layersVisible) }
    }

    fun toggleSavedScenes() {
        refreshSavedScenes()
        _uiState.update { it.copy(savedScenesVisible = !it.savedScenesVisible) }
    }

    fun toggleDiagnostics() {
        _uiState.update { it.copy(diagnosticsVisible = !it.diagnosticsVisible) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun mutate(description: String, message: String = description, block: (com.indianservers.ai_stem.domain.scene.MathScene) -> com.indianservers.ai_stem.domain.scene.MathScene) {
        val before = _uiState.value.mathScene
        val after = block(before)
        setSceneWithHistory(before, after, description, message)
    }

    private fun setSceneWithHistory(before: com.indianservers.ai_stem.domain.scene.MathScene, after: com.indianservers.ai_stem.domain.scene.MathScene, description: String, message: String) {
        if (before == after) return
        _uiState.update {
            it.copy(
                mathScene = after,
                history = it.history.record(SnapshotSceneCommand(description = description, before = before, after = after)),
                userMessage = UiMessage(message)
            )
        }
    }

    private fun refreshSavedScenes() {
        sceneRepository?.let { repository -> _uiState.update { it.copy(savedScenes = repository.listScenes()) } }
    }

    private fun readSensorReadiness(context: Context): ArSensorReadiness {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return ArSensorReadiness(false, "Android sensor service is unavailable.")
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val uncalibratedGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
        return ArSensorReadiness(
            ready = accelerometer != null && (gyroscope != null || uncalibratedGyro != null),
            userMessage = when {
                accelerometer == null -> "This device is missing the accelerometer required by AR."
                gyroscope == null && uncalibratedGyro == null -> "This device is missing the gyroscope required by AR."
                else -> "AR sensors ready."
            },
            accelerometer = accelerometer?.name,
            gyroscope = gyroscope?.name,
            uncalibratedGyroscope = uncalibratedGyro?.name,
            rotationVector = rotationVector?.name
        )
    }

    private fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private val GraphColorMap.label: String
    get() = when (this) {
        GraphColorMap.Height -> "Height"
        GraphColorMap.Slope -> "Slope"
        GraphColorMap.Curvature -> "Curvature"
        GraphColorMap.XValue -> "X value"
        GraphColorMap.YValue -> "Y value"
    }

private val PaperGraphCalibrationStep.prompt: String
    get() = when (this) {
        PaperGraphCalibrationStep.Origin -> "origin"
        PaperGraphCalibrationStep.XAxisPoint -> "X-axis point"
        PaperGraphCalibrationStep.YAxisPoint -> "Y-axis point"
        PaperGraphCalibrationStep.Complete -> "reset calibration"
    }

private fun FloatingMathTool.userMessage(): String = when (this) {
    FloatingMathTool.Axes -> "Axes visibility toggled."
    FloatingMathTool.Grid -> "Grid visibility toggled."
    FloatingMathTool.Labels -> "Tap labels toggled."
    FloatingMathTool.Formula -> "Formula cards toggled."
    FloatingMathTool.Measure -> "Measurement overlay toggled."
    FloatingMathTool.Slice -> "Interactive slice plane toggled."
    FloatingMathTool.Animate -> "Graph animation scrubber toggled."
    FloatingMathTool.Capture -> "Quick capture overlay ready."
}

private fun GraphAnimationMode.label(): String = when (this) {
    GraphAnimationMode.RotateGraph -> "Rotate graph"
    GraphAnimationMode.SweepArea -> "Sweep area"
    GraphAnimationMode.BuildVolume -> "Build volume"
    GraphAnimationMode.MoveTangentPoint -> "Move tangent point"
    GraphAnimationMode.AnimateSineWave -> "Animate sine wave"
}

private fun Function3dTransformMode.label(): String = when (this) {
    Function3dTransformMode.Surface -> "Surface"
    Function3dTransformMode.Extrusion -> "Extrusion"
    Function3dTransformMode.SolidOfRevolution -> "Solid of revolution"
    Function3dTransformMode.TangentPlane -> "Tangent plane"
    Function3dTransformMode.CrossSectionSlices -> "Cross-section slices"
}

private fun GraphQualityPreset.label(): String = when (this) {
    GraphQualityPreset.BatterySaver -> "Battery saver"
    GraphQualityPreset.Balanced -> "Balanced"
    GraphQualityPreset.HighQuality -> "High quality"
    GraphQualityPreset.Presentation -> "Presentation"
}

private fun Double.toFloatOrZero(): Float = if (isFinite()) toFloat() else 0f

private fun ArViewerUiState.snapProfile(): ArSnappingProfile =
    ArSnappingProfile(
        enabled = snappingEnabled || coordinateGridLocked || MathArFeature.ObjectSnapping in enabledMathArFeatures,
        gridStepMeters = if (coordinateGridLocked) 0.025 else 0.05,
        axisToleranceMeters = if (coordinateGridLocked) 0.04 else 0.025,
        graphToleranceMeters = 0.055
    )

private fun ArViewerUiState.graphSnapCandidates() =
    arInteractionEngineForHelpers.graphSnapCandidates(compiledArExpression, arGraphAnalysis.highlightedPoints) +
        listOf(
            com.indianservers.ai_stem.domain.interaction.ArSnapCandidate(Vector3Value(), SnapKind.Origin, "Origin", priority = 8)
        )

private val arInteractionEngineForHelpers = ArInteractionEngine(ArMathEngine())

private fun ArViewerUiState.activeWorkflowTemplate() =
    ArWorkflowTemplates.templates.firstOrNull { it.id == activeWorkflowId }
        ?: ArWorkflowTemplates.templates.first()

private fun ArViewerUiState.workflowSignal(): ArWorkflowSignal =
    ArWorkflowSignal(
        mode = arEngineMode.name,
        hasPlacedObject = mathScene.objects.isNotEmpty(),
        placementScore = placementScore,
        paperCalibrated = paperGraphCalibration.isComplete,
        equationValid = compiledArExpression.isValid,
        hasAnalysis = arGraphAnalysis.highlightedPoints.isNotEmpty() ||
            arGraphAnalysis.tangent != null ||
            arGraphAnalysis.integral != null,
        pickedPointCount = pickedPoints.size + pickedGraphPoints.size,
        rulerAnchorCount = rulerAnchors.size,
        constructionObjectCount = constructionGeometry.objects.size,
        anchorSaved = mathScene.persistentAnchor.kind != PersistentAnchorKind.SceneOrigin,
        exportReady = exportedScenePackage != null || exportedActivityPackage != null,
        depthEnabled = depthOcclusionMode != ArDepthOcclusionMode.Off || depthOcclusionPolishEnabled
    )

private fun ArViewerUiState.advancedCapability() =
    ArAdvancedMathTools.evaluate(
        equationKind = compiledArExpression.kind,
        hasAnalysis = arGraphAnalysis.highlightedPoints.isNotEmpty() ||
            arGraphAnalysis.tangent != null ||
            arGraphAnalysis.integral != null,
        constructionCount = constructionGeometry.objects.size,
        pickedPointCount = pickedPoints.size + pickedGraphPoints.size,
        exported = exportedScenePackage != null || exportedActivityPackage != null
    )

private fun ArAdvancedToolKind.features(): Set<MathArFeature> = when (this) {
    ArAdvancedToolKind.ParametricCurve -> setOf(MathArFeature.ParametricGraphing, MathArFeature.LocusTrace, MathArFeature.PointPicker)
    ArAdvancedToolKind.ImplicitRelation -> setOf(MathArFeature.ImplicitRelations, MathArFeature.RootVisualizer)
    ArAdvancedToolKind.InequalityRegion -> setOf(MathArFeature.ImplicitRelations, MathArFeature.AreaUnderCurve)
    ArAdvancedToolKind.LocusTrace -> setOf(MathArFeature.LocusTrace, MathArFeature.PointPicker)
    ArAdvancedToolKind.MacroConstruction -> setOf(MathArFeature.MacroTools, MathArFeature.MultiObjectConstraints)
    ArAdvancedToolKind.ProofCheck -> setOf(MathArFeature.ProofChecker, MathArFeature.EvidenceRecorder, MathArFeature.MultiObjectConstraints)
    ArAdvancedToolKind.CasCommand -> setOf(MathArFeature.CasCommands, MathArFeature.TangentNormalTool, MathArFeature.AreaUnderCurve)
    ArAdvancedToolKind.MultiGraphIntersection -> setOf(MathArFeature.RootVisualizer, MathArFeature.CompareMode)
}

private fun ArViewerUiState.productionSettings(): ArSceneProductionSettings =
    mathScene.arProductionSettings.copy(
        depthMode = depthOcclusionMode,
        performanceProfile = performanceProfile,
        meshDensity = meshDensity.coerceIn(0.1f, 1f),
        maxSceneObjects = maxSceneObjects.coerceIn(4, 128)
    )

private fun ArViewerUiState.sceneWithProductionState(): com.indianservers.ai_stem.domain.scene.MathScene =
    mathScene.copy(
        persistentAnchor = if (mathScene.persistentAnchor.kind == PersistentAnchorKind.SceneOrigin && lastPlacementPoint != null) {
            productionAnchorRecord()
        } else {
            mathScene.persistentAnchor
        },
        arProductionSettings = productionSettings()
    )

private fun ArViewerUiState.productionAnchorRecord(): PersistentAnchorRecord {
    val position = lastPlacementPoint?.let { Vector3Value(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
        ?: mathScene.primarySelectedObject?.transform?.position
        ?: Vector3Value()
    val lockedImage = paperGraph?.trackedTargets?.firstOrNull()?.name
    val kind = when {
        arEngineMode == ArEngineMode.PaperGraph -> PersistentAnchorKind.PaperImage
        arEngineMode == ArEngineMode.OutdoorGeospatialMath && placementHitKind == PlacementHitKind.StreetscapeGeometry -> PersistentAnchorKind.StreetscapeGeometry
        arEngineMode == ArEngineMode.OutdoorGeospatialMath -> PersistentAnchorKind.Geospatial
        placementHitKind in setOf(PlacementHitKind.Plane, PlacementHitKind.DepthPoint) -> PersistentAnchorKind.Plane
        else -> PersistentAnchorKind.SceneOrigin
    }
    return PersistentAnchorRecord(
        kind = kind,
        engineMode = arEngineMode.name,
        label = when (kind) {
            PersistentAnchorKind.SceneOrigin -> "Scene origin anchor"
            PersistentAnchorKind.Plane -> "Surface plane anchor"
            PersistentAnchorKind.PaperImage -> "Marker image anchor"
            PersistentAnchorKind.Geospatial -> "Outdoor geospatial anchor"
            PersistentAnchorKind.StreetscapeGeometry -> "Building mesh anchor"
        },
        worldPosition = position,
        imageTargetName = lockedImage ?: if (kind == PersistentAnchorKind.PaperImage) "AI STEM Geometry Marker" else null,
        accuracyMeters = when (placementQuality) {
            PlacementQuality.Excellent -> 0.02
            PlacementQuality.Good -> 0.05
            PlacementQuality.Weak -> 0.15
            PlacementQuality.Recovering -> 0.35
            else -> null
        }
    )
}

private fun String.extractMarkerLessonId(): String? =
    Regex("""[Gg]01""")
        .find(this)
        ?.value
        ?.uppercase()

enum class Marker2dShapeTool(val label: String) {
    Point("Point"),
    Line("Line"),
    Segment("Segment"),
    Ray("Ray"),
    Triangle("Triangle"),
    Square("Square"),
    Rectangle("Rectangle"),
    Circle("Circle"),
    Ellipse("Ellipse"),
    Polygon("Polygon"),
    RegularPolygon("Regular polygon"),
    Angle("Angle"),
    Arc("Arc"),
    Perpendicular("Perpendicular"),
    Parallel("Parallel"),
    Clear("Clear")
}

private fun MarkerMathActivity.label(): String = when (this) {
    MarkerMathActivity.Geometry2D -> "2D Geometry"
    MarkerMathActivity.Geometry3D -> "3D Shapes"
    MarkerMathActivity.FunctionGraph -> "Graph"
    MarkerMathActivity.CoordinateLab -> "Coordinates"
    MarkerMathActivity.Transformations -> "Transforms"
    MarkerMathActivity.MeasurementLab -> "Measure"
    MarkerMathActivity.Trigonometry -> "Trigonometry"
}

private fun MarkerTransformationTool.label(): String = when (this) {
    MarkerTransformationTool.Translation -> "Translation"
    MarkerTransformationTool.Rotation -> "Rotation"
    MarkerTransformationTool.Reflection -> "Reflection"
    MarkerTransformationTool.Dilation -> "Dilation"
    MarkerTransformationTool.HorizontalStretch -> "Horizontal stretch"
    MarkerTransformationTool.VerticalStretch -> "Vertical stretch"
    MarkerTransformationTool.Shear -> "Shear"
    MarkerTransformationTool.Composite -> "Composite"
}

private fun ArGraphDomain.zoomed(factor: Double): ArGraphDomain {
    val safe = factor.coerceIn(0.35, 2.5)
    val cx = (xMin + xMax) / 2.0
    val cy = (yMin + yMax) / 2.0
    val halfX = (xMax - xMin) * safe / 2.0
    val halfY = (yMax - yMin) * safe / 2.0
    return copy(xMin = cx - halfX, xMax = cx + halfX, yMin = cy - halfY, yMax = cy + halfY)
}

private fun ArViewerUiState.recompileMarkerGraphs(domain: ArGraphDomain): ArViewerUiState {
    val engine = ArMathEngine()
    val graphs = markerGraphFunctions.map { graph ->
        val compiled = engine.compile(
            source = graph.source,
            existingSliders = graph.sliders,
            domain = domain,
            qualityPreset = arGraphQualityPreset
        )
        graph.copy(compiled = compiled, sliders = compiled.parameters)
    }
    val selected = graphs.firstOrNull { it.id == selectedMarkerGraphFunctionId }
    return copy(
        arGraphDomain = domain,
        markerGraphFunctions = graphs,
        compiledArExpression = selected?.compiled ?: compiledArExpression,
        arGraphSliders = selected?.sliders ?: arGraphSliders
    )
}

private fun ConstructionGeometryState.addMarkerPoint(): ConstructionGeometryState {
    val index = points.size
    val point = ConstructionPoint(
        id = "marker-point-$index",
        label = "P${index + 1}",
        position = Vector3Value(-0.16 + (index % 5) * 0.08, 0.0, -0.12 + (index / 5) * 0.08)
    )
    val obj = ConstructionObject(
        id = "marker-point-object-$index",
        label = point.label,
        kind = ConstructionObjectKind.Point,
        pointIds = listOf(point.id),
        dependencyKind = ConstructionDependencyKind.Free
    )
    return copy(points = points + point, objects = objects + obj, selectedPointIds = listOf(point.id), revision = revision + 1)
}

private fun ConstructionGeometryState.addMarkerLine(): ConstructionGeometryState =
    addMarkerPolygonObject(
        label = "Line",
        kind = ConstructionObjectKind.Line,
        dependencyKind = ConstructionDependencyKind.ThroughTwoPoints,
        points = listOf(Vector3Value(-0.2, 0.0, 0.0), Vector3Value(0.2, 0.0, 0.0))
    )

private fun ConstructionGeometryState.addMarkerTriangle(): ConstructionGeometryState =
    addMarkerPolygonObject(
        label = "Triangle",
        kind = ConstructionObjectKind.Polygon,
        dependencyKind = ConstructionDependencyKind.PolygonThroughPoints,
        points = listOf(Vector3Value(-0.16, 0.0, 0.12), Vector3Value(0.16, 0.0, 0.12), Vector3Value(0.0, 0.0, -0.14))
    )

private fun ConstructionGeometryState.addMarkerSquare(): ConstructionGeometryState =
    addMarkerPolygonObject(
        label = "Square",
        kind = ConstructionObjectKind.Polygon,
        dependencyKind = ConstructionDependencyKind.PolygonThroughPoints,
        points = listOf(Vector3Value(-0.14, 0.0, -0.14), Vector3Value(0.14, 0.0, -0.14), Vector3Value(0.14, 0.0, 0.14), Vector3Value(-0.14, 0.0, 0.14))
    )

private fun ConstructionGeometryState.addMarkerCircle(): ConstructionGeometryState =
    addMarkerPolygonObject(
        label = "Circle",
        kind = ConstructionObjectKind.Circle,
        dependencyKind = ConstructionDependencyKind.CircleCenterPoint,
        points = listOf(Vector3Value(0.0, 0.0, 0.0), Vector3Value(0.13, 0.0, 0.0))
    )

private fun ConstructionGeometryState.addMarkerPolygonObject(
    label: String,
    kind: ConstructionObjectKind,
    dependencyKind: ConstructionDependencyKind,
    points: List<Vector3Value>
): ConstructionGeometryState {
    val offset = objects.size * 0.025
    val base = revision + objects.size + this.points.size
    val newPoints = points.mapIndexed { index, position ->
        ConstructionPoint(
            id = "marker-${label.lowercase()}-$base-$index",
            label = "${label.take(1)}${index + 1}",
            position = position.plus(Vector3Value(offset, 0.0, offset))
        )
    }
    val obj = ConstructionObject(
        id = "marker-${label.lowercase()}-$base",
        label = label,
        kind = kind,
        pointIds = newPoints.map { it.id },
        dependencyKind = dependencyKind
    )
    return copy(points = this.points + newPoints, objects = objects + obj, selectedPointIds = newPoints.map { it.id }.takeLast(3), revision = revision + 1)
}

private fun markerObjectTransform(definitionId: String, index: Int): ObjectTransform =
    Shape3dEngine.placementPlan(
        Shape3dPlacementRequest(
            definitionId = definitionId,
            anchorMode = Shape3dAnchorMode.MarkerImage,
            normalizedX = ((index % 3) - 1) * 0.42,
            normalizedZ = (index / 3) * 0.32 - 0.12,
            snapToGrid = true
        )
    ).transform

private fun markerLessonState(markerId: String, state: ArViewerUiState): ArViewerUiState {
    val definition = markerLessonDefinition(markerId)
    val selectedType = definition.selectedType
    val definitionId = DefaultMathObjectRegistry.getAllDefinitions().first { it.type == selectedType }.definitionId
    return state.copy(
        activeMarkerLessonId = markerId,
        markerLessonTitle = definition.title,
        markerLessonSubtitle = definition.subtitle,
        markerLessonInteraction = MarkerLessonInteractionState(elements = definition.elements),
        selectedObjectType = selectedType,
        selectedDefinitionId = definitionId,
        userMessage = UiMessage("G01 locked. Choose 2D, 3D, Graph, or Coord.")
    )
}

private data class MarkerLessonDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val selectedType: MathObjectType,
    val construction: ConstructionGeometryState,
    val elements: List<MarkerLessonElement>
)

private fun markerLessonDefinition(markerId: String): MarkerLessonDefinition =
    when (markerId) {
        "G01" -> MarkerLessonDefinition(markerId, "G01 Geometry Foundations", "Points, lines, segments, rays and planes", MathObjectType.CoordinatePlane, geometryFoundationsConstruction(), elements(
            "Point A" to "A has position only",
            "Line PQ" to "extends forever",
            "Segment AB" to "finite length",
            "Ray CD" to "one endpoint",
            "Plane pi" to "flat 2D surface in 3D"
        ))
        "G02" -> MarkerLessonDefinition(markerId, "G02 Angles", "Types, measures and relationships", MathObjectType.VectorArrow, anglesConstruction(), elements(
            "Acute angle" to "0 < theta < 90",
            "Right angle" to "theta = 90",
            "Obtuse angle" to "90 < theta < 180",
            "Supplementary" to "alpha + beta = 180"
        ))
        "G03" -> MarkerLessonDefinition(markerId, "G03 2D Shapes", "Triangles, quadrilaterals, circles and polygons", MathObjectType.Triangle, shapeGalleryConstruction(), elements(
            "Triangle" to "P = a + b + c",
            "Square" to "A = s^2",
            "Rectangle" to "A = l x w",
            "Circle" to "A = pi r^2",
            "Polygon" to "(n - 2) x 180"
        ))
        "G04" -> MarkerLessonDefinition(markerId, "G04 Circle Concepts", "Radius, diameter, chords, tangent, secant and sector", MathObjectType.Circle, circleConceptConstruction(), elements(
            "Radius OC" to "center to circle",
            "Diameter BD" to "2r",
            "Chord EC" to "joins two circle points",
            "Tangent PT" to "touches once",
            "Sector BOC" to "arc plus two radii"
        ))
        "G05" -> MarkerLessonDefinition(markerId, "G05 Triangle Geometry", "Types, angle sum, congruence and similarity", MathObjectType.Triangle, triangleGeometryConstruction(), elements(
            "Equilateral" to "all angles 60",
            "Isosceles" to "base angles equal",
            "Scalene" to "all sides different",
            "Angle sum" to "A + B + C = 180",
            "Similarity" to "same shape, scaled"
        ))
        "G06" -> MarkerLessonDefinition(markerId, "G06 Transformations", "Translation, rotation, reflection and enlargement", MathObjectType.VectorArrow, transformationsConstruction(), elements(
            "Translation" to "move by vector v",
            "Rotation" to "turn about a center",
            "Reflection" to "mirror across line l",
            "Enlargement" to "scale factor k"
        ))
        "A01" -> MarkerLessonDefinition(markerId, "A01 Algebra Foundations", "Variables, coefficients, constants and like terms", MathObjectType.NumberLine, algebraFoundationsConstruction(), elements(
            "Coefficient" to "3 in 3x",
            "Variable" to "x, y",
            "Constant" to "-5",
            "Like terms" to "5x + 3x - 2x = 6x"
        ))
        "A02" -> MarkerLessonDefinition(markerId, "A02 Equations & Inequalities", "Linear equations, simultaneous equations and number-line solutions", MathObjectType.NumberLine, equationsConstruction(), elements(
            "Linear equation" to "2x + 5 = 17",
            "Solution" to "x = 6",
            "Simultaneous" to "x = 2, y = 3",
            "Inequality" to "x > 2"
        ))
        "A03" -> MarkerLessonDefinition(markerId, "A03 Polynomials", "Operations, factorisation, roots and graphs", MathObjectType.SineCurve, polynomialConstruction(), elements(
            "Polynomial" to "x^2 + 5x + 6",
            "Factorisation" to "(x + 2)(x + 3)",
            "Roots" to "x = -2, -3",
            "Vertex" to "axis of symmetry"
        ))
        "A04" -> MarkerLessonDefinition(markerId, "A04 Sequences & Series", "AP, GP and nth term", MathObjectType.NumberLine, sequencesConstruction(), elements(
            "Pattern" to "2, 5, 8, 11...",
            "AP" to "a_n = a_1 + (n - 1)d",
            "GP" to "a_n = a_1 r^(n - 1)",
            "nth term" to "jump directly to term n"
        ))
        "C01" -> MarkerLessonDefinition(markerId, "C01 Coordinate Basics", "Cartesian plane, quadrants and point plotting", MathObjectType.CoordinatePlane, coordinateBasicsConstruction(), elements(
            "Quadrant I" to "x > 0, y > 0",
            "Quadrant II" to "x < 0, y > 0",
            "Point A" to "(2, 3)",
            "Point D" to "(3, -1)"
        ))
        "C02" -> MarkerLessonDefinition(markerId, "C02 Lines & Distance", "Distance, midpoint, slope and line equation", MathObjectType.CoordinatePlane, linesDistanceConstruction(), elements(
            "Distance" to "sqrt((x2-x1)^2+(y2-y1)^2)",
            "Midpoint" to "((x1+x2)/2, (y1+y2)/2)",
            "Slope" to "(y2-y1)/(x2-x1)",
            "Line" to "y = mx + c"
        ))
        "C03" -> MarkerLessonDefinition(markerId, "C03 Advanced Coordinate Geometry", "Circles, intersections, tangents and loci", MathObjectType.Circle, advancedCoordinateConstruction(), elements(
            "Circle equation" to "(x-h)^2 + (y-k)^2 = r^2",
            "Line-circle" to "two intersections",
            "Tangent" to "radius perpendicular",
            "Locus" to "set of points"
        ))
        "F01" -> MarkerLessonDefinition(markerId, "F01 Functions & Graphs", "Linear, quadratic, cubic, exponential and logarithmic", MathObjectType.SineCurve, functionsConstruction(), elements(
            "Linear" to "f(x)=mx+c",
            "Quadratic" to "f(x)=ax^2+bx+c",
            "Cubic" to "f(x)=ax^3+...",
            "Exponential" to "f(x)=a^x",
            "Logarithmic" to "f(x)=log_a x"
        ))
        "F02" -> MarkerLessonDefinition(markerId, "F02 Graph Transformations", "Translation, reflection, stretching and compression", MathObjectType.SineCurve, graphTransformConstruction(), elements(
            "Parent" to "y=x^2",
            "Horizontal shift" to "y=(x-h)^2",
            "Vertical shift" to "y=x^2+k",
            "Reflection" to "y=-x^2",
            "Stretch" to "y=ax^2"
        ))
        "M01" -> MarkerLessonDefinition(markerId, "M01 Plane Mensuration", "Perimeter and area of plane shapes", MathObjectType.RectangularPrism, planeMensurationConstruction(), elements(
            "Rectangle" to "A = l x b",
            "Triangle" to "A = 1/2 x b x h",
            "Circle" to "A = pi r^2",
            "Circumference" to "C = 2 pi r"
        ))
        "M02" -> MarkerLessonDefinition(markerId, "M02 Solid Mensuration", "Surface area and volume of solids", MathObjectType.Cube, solidMensurationConstruction(), elements(
            "Cube" to "SA = 6a^2, V = a^3",
            "Cylinder" to "SA = 2pi r(r+h), V = pi r^2h",
            "Cone" to "SA = pi r(l+r), V = 1/3 pi r^2h",
            "Sphere" to "SA = 4pi r^2, V = 4/3 pi r^3"
        ))
        else -> MarkerLessonDefinition(markerId, "$markerId Marker", "Interactive AR Maths lesson", MathObjectType.Cube, emptyMarkerConstruction(markerId), elements(markerId to "Marker locked"))
    }

private fun elements(vararg entries: Pair<String, String>): List<MarkerLessonElement> =
    entries.mapIndexed { index, entry ->
        MarkerLessonElement(
            id = "element-$index",
            label = entry.first,
            kind = MarkerLessonElementKind.Point,
            formula = entry.second
        )
    }

private fun geometryFoundationsConstruction(): ConstructionGeometryState {
    val points = listOf(
        ConstructionPoint("g01-a", "A", Vector3Value(-0.18, 0.0, -0.12)),
        ConstructionPoint("g01-p", "P", Vector3Value(-0.2, 0.0, 0.0)),
        ConstructionPoint("g01-q", "Q", Vector3Value(-0.04, 0.0, 0.0)),
        ConstructionPoint("g01-b1", "B", Vector3Value(-0.2, 0.0, 0.11)),
        ConstructionPoint("g01-b2", "C", Vector3Value(-0.04, 0.0, 0.11)),
        ConstructionPoint("g01-r1", "D", Vector3Value(-0.2, 0.0, 0.2)),
        ConstructionPoint("g01-r2", "E", Vector3Value(-0.04, 0.0, 0.2)),
        ConstructionPoint("g01-x", "X", Vector3Value(0.08, 0.0, -0.05)),
        ConstructionPoint("g01-y", "Y", Vector3Value(0.2, 0.0, 0.02)),
        ConstructionPoint("g01-z", "Z", Vector3Value(0.1, 0.0, 0.15))
    )
    val objects = listOf(
        ConstructionObject("g01-point-a", "Point A", ConstructionObjectKind.Point, listOf("g01-a"), ConstructionDependencyKind.Free),
        ConstructionObject("g01-line-pq", "Line PQ", ConstructionObjectKind.Line, listOf("g01-p", "g01-q"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g01-segment-bc", "Segment BC", ConstructionObjectKind.Segment, listOf("g01-b1", "g01-b2"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g01-ray-de", "Ray DE", ConstructionObjectKind.Ray, listOf("g01-r1", "g01-r2"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g01-plane-xyz", "Plane pi", ConstructionObjectKind.Plane, listOf("g01-x", "g01-y", "g01-z"), ConstructionDependencyKind.PlaneThroughThreePoints)
    )
    return ConstructionGeometryState(points = points, objects = objects, revision = 1)
}

private fun anglesConstruction(): ConstructionGeometryState {
    val points = listOf(
        ConstructionPoint("g02-o", "O", Vector3Value(0.0, 0.0, 0.0)),
        ConstructionPoint("g02-a", "A", Vector3Value(-0.18, 0.0, 0.12)),
        ConstructionPoint("g02-b", "B", Vector3Value(0.2, 0.0, 0.1)),
        ConstructionPoint("g02-c", "C", Vector3Value(0.02, 0.0, -0.2)),
        ConstructionPoint("g02-d", "D", Vector3Value(0.22, 0.0, -0.12)),
        ConstructionPoint("g02-e", "E", Vector3Value(-0.22, 0.0, -0.08))
    )
    val objects = listOf(
        ConstructionObject("g02-ray-oa", "Ray OA", ConstructionObjectKind.Ray, listOf("g02-o", "g02-a"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g02-ray-ob", "Ray OB", ConstructionObjectKind.Ray, listOf("g02-o", "g02-b"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g02-segment-cd", "Angle side CD", ConstructionObjectKind.Segment, listOf("g02-c", "g02-d"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g02-segment-ce", "Angle side CE", ConstructionObjectKind.Segment, listOf("g02-c", "g02-e"), ConstructionDependencyKind.ThroughTwoPoints),
        ConstructionObject("g02-line-ed", "Straight angle ED", ConstructionObjectKind.Line, listOf("g02-e", "g02-d"), ConstructionDependencyKind.ThroughTwoPoints)
    )
    return ConstructionGeometryState(points = points, objects = objects, revision = 1)
}

private fun shapeGalleryConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("tri-a", -0.24, -0.12), p("tri-b", -0.14, -0.12), p("tri-c", -0.19, -0.01),
            p("sq-a", -0.08, -0.12), p("sq-b", 0.02, -0.12), p("sq-c", 0.02, -0.02), p("sq-d", -0.08, -0.02),
            p("rect-a", 0.08, -0.12), p("rect-b", 0.24, -0.12), p("rect-c", 0.24, -0.04), p("rect-d", 0.08, -0.04),
            p("circle-o", -0.18, 0.14), p("circle-r", -0.1, 0.14),
            p("poly-a", 0.1, 0.08), p("poly-b", 0.18, 0.04), p("poly-c", 0.24, 0.1), p("poly-d", 0.2, 0.18), p("poly-e", 0.1, 0.18)
        ),
        objects = listOf(
            o("tri", "Triangle", ConstructionObjectKind.Polygon, "tri-a", "tri-b", "tri-c"),
            o("sq", "Square", ConstructionObjectKind.Polygon, "sq-a", "sq-b", "sq-c", "sq-d"),
            o("rect", "Rectangle", ConstructionObjectKind.Polygon, "rect-a", "rect-b", "rect-c", "rect-d"),
            o("circle", "Circle", ConstructionObjectKind.Circle, "circle-o", "circle-r"),
            o("poly", "Pentagon", ConstructionObjectKind.Polygon, "poly-a", "poly-b", "poly-c", "poly-d", "poly-e")
        ),
        revision = 1
    )

private fun circleConceptConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("o", 0.0, 0.0), p("a", -0.16, 0.0), p("c", 0.16, 0.0), p("b", 0.0, -0.16), p("d", 0.0, 0.16),
            p("e", -0.12, -0.1), p("f", 0.12, 0.1), p("p", 0.12, -0.1), p("t", 0.22, -0.18), p("s", -0.16, 0.12), p("r", 0.22, 0.18)
        ),
        objects = listOf(
            o("circle", "Circle", ConstructionObjectKind.Circle, "o", "c"),
            o("radius", "Radius OC", ConstructionObjectKind.Segment, "o", "c"),
            o("diameter", "Diameter BD", ConstructionObjectKind.Segment, "b", "d"),
            o("chord", "Chord EF", ConstructionObjectKind.Segment, "e", "f"),
            o("tangent", "Tangent PT", ConstructionObjectKind.Line, "p", "t"),
            o("secant", "Secant SR", ConstructionObjectKind.Line, "s", "r"),
            o("sector", "Sector BOC", ConstructionObjectKind.Polygon, "o", "b", "c")
        ),
        revision = 1
    )

private fun triangleGeometryConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("eq-a", -0.24, -0.1), p("eq-b", -0.12, -0.1), p("eq-c", -0.18, -0.0),
            p("iso-a", -0.04, -0.1), p("iso-b", 0.08, -0.1), p("iso-c", 0.02, 0.04),
            p("sca-a", 0.14, -0.1), p("sca-b", 0.28, -0.08), p("sca-c", 0.2, 0.07),
            p("sum-a", -0.12, 0.12), p("sum-b", 0.08, 0.12), p("sum-c", -0.02, 0.24)
        ),
        objects = listOf(
            o("eq", "Equilateral", ConstructionObjectKind.Polygon, "eq-a", "eq-b", "eq-c"),
            o("iso", "Isosceles", ConstructionObjectKind.Polygon, "iso-a", "iso-b", "iso-c"),
            o("sca", "Scalene", ConstructionObjectKind.Polygon, "sca-a", "sca-b", "sca-c"),
            o("sum", "Angle Sum", ConstructionObjectKind.Polygon, "sum-a", "sum-b", "sum-c")
        ),
        revision = 1
    )

private fun transformationsConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("ta", -0.24, -0.1), p("tb", -0.16, -0.1), p("tc", -0.16, -0.02), p("td", -0.24, -0.02),
            p("ta2", -0.06, 0.0), p("tb2", 0.02, 0.0), p("tc2", 0.02, 0.08), p("td2", -0.06, 0.08),
            p("ra", 0.12, -0.1), p("rb", 0.22, -0.1), p("rc", 0.17, 0.02),
            p("ma", -0.2, 0.12), p("mb", -0.1, 0.12), p("mc", -0.2, 0.22), p("ma2", 0.1, 0.12), p("mb2", 0.2, 0.12), p("mc2", 0.2, 0.22),
            p("mirror-a", 0.0, 0.08), p("mirror-b", 0.0, 0.26)
        ),
        objects = listOf(
            o("trans-a", "A", ConstructionObjectKind.Polygon, "ta", "tb", "tc", "td"),
            o("trans-b", "A prime", ConstructionObjectKind.Polygon, "ta2", "tb2", "tc2", "td2"),
            o("vector", "Vector v", ConstructionObjectKind.Vector, "tc", "ta2"),
            o("rotation", "Rotation", ConstructionObjectKind.Polygon, "ra", "rb", "rc"),
            o("reflect-a", "Reflect A", ConstructionObjectKind.Polygon, "ma", "mb", "mc"),
            o("reflect-b", "Reflect A prime", ConstructionObjectKind.Polygon, "ma2", "mb2", "mc2"),
            o("mirror", "Mirror line", ConstructionObjectKind.Line, "mirror-a", "mirror-b")
        ),
        revision = 1
    )

private fun algebraFoundationsConstruction(): ConstructionGeometryState =
    expressionBlocks("A01", listOf("3x" to -0.16, "2y" to 0.02, "-5" to 0.18))

private fun equationsConstruction(): ConstructionGeometryState =
    expressionBlocks("A02", listOf("2x+5" to -0.18, "17" to -0.02, "x>2" to 0.16))

private fun polynomialConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = parabolaPoints("poly", -0.22, 0.22, 9) + listOf(p("r1", -0.12, 0.0), p("r2", 0.12, 0.0)),
        objects = parabolaObjects("poly", 9) + listOf(
            o("root1", "Root -2", ConstructionObjectKind.Point, "r1"),
            o("root2", "Root -3", ConstructionObjectKind.Point, "r2")
        ),
        revision = 1
    )

private fun sequencesConstruction(): ConstructionGeometryState {
    val points = (0..5).map { index -> p("seq-$index", -0.24 + index * 0.095, -0.02) }
    return ConstructionGeometryState(
        points = points,
        objects = (0 until 5).map { index -> o("step-$index", "+3", ConstructionObjectKind.Vector, "seq-$index", "seq-${index + 1}") },
        revision = 1
    )
}

private fun coordinateBasicsConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("origin", 0.0, 0.0), p("x1", 0.24, 0.0), p("x2", -0.24, 0.0), p("y1", 0.0, 0.24), p("y2", 0.0, -0.24),
            p("a", 0.1, -0.15), p("b", -0.15, -0.1), p("c", -0.1, 0.1), p("d", 0.16, 0.08)
        ),
        objects = listOf(
            o("xaxis", "x-axis", ConstructionObjectKind.Line, "x1", "x2"),
            o("yaxis", "y-axis", ConstructionObjectKind.Line, "y1", "y2"),
            o("a", "A(2,3)", ConstructionObjectKind.Point, "a"),
            o("b", "B(-3,2)", ConstructionObjectKind.Point, "b"),
            o("c", "C(-2,-2)", ConstructionObjectKind.Point, "c"),
            o("d", "D(3,-1)", ConstructionObjectKind.Point, "d")
        ),
        revision = 1
    )

private fun linesDistanceConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(p("a", -0.12, 0.08), p("b", 0.18, -0.12), p("m", 0.03, -0.02), p("x1", -0.24, 0.0), p("x2", 0.24, 0.0), p("y1", 0.0, -0.2), p("y2", 0.0, 0.2)),
        objects = listOf(
            o("xaxis", "x-axis", ConstructionObjectKind.Line, "x1", "x2"),
            o("yaxis", "y-axis", ConstructionObjectKind.Line, "y1", "y2"),
            o("ab", "Line AB", ConstructionObjectKind.Segment, "a", "b"),
            o("m", "Midpoint M", ConstructionObjectKind.Point, "m")
        ),
        revision = 1
    )

private fun advancedCoordinateConstruction(): ConstructionGeometryState =
    circleConceptConstruction().copy(revision = 1)

private fun functionsConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = parabolaPoints("f", -0.24, 0.24, 13),
        objects = parabolaObjects("f", 13),
        revision = 1
    )

private fun graphTransformConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = parabolaPoints("parent", -0.24, 0.02, 8) + parabolaPoints("shift", -0.02, 0.24, 8, zOffset = -0.04),
        objects = parabolaObjects("parent", 8) + parabolaObjects("shift", 8),
        revision = 1
    )

private fun planeMensurationConstruction(): ConstructionGeometryState =
    shapeGalleryConstruction().copy(revision = 1)

private fun solidMensurationConstruction(): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(
            p("cube-a", -0.24, -0.1), p("cube-b", -0.14, -0.1), p("cube-c", -0.14, 0.0), p("cube-d", -0.24, 0.0),
            p("cyl-o", -0.02, -0.04), p("cyl-r", 0.06, -0.04),
            p("cone-a", 0.1, -0.1), p("cone-b", 0.22, -0.1), p("cone-c", 0.16, 0.05),
            p("sphere-o", 0.12, 0.16), p("sphere-r", 0.22, 0.16)
        ),
        objects = listOf(
            o("cube", "Cube", ConstructionObjectKind.Polygon, "cube-a", "cube-b", "cube-c", "cube-d"),
            o("cylinder", "Cylinder base", ConstructionObjectKind.Circle, "cyl-o", "cyl-r"),
            o("cone", "Cone", ConstructionObjectKind.Polygon, "cone-a", "cone-b", "cone-c"),
            o("sphere", "Sphere", ConstructionObjectKind.Circle, "sphere-o", "sphere-r")
        ),
        revision = 1
    )

private fun expressionBlocks(prefix: String, terms: List<Pair<String, Double>>): ConstructionGeometryState {
    val points = terms.flatMapIndexed { index, (_, x) ->
        listOf(p("$prefix-$index-a", x - 0.04, 0.0), p("$prefix-$index-b", x + 0.04, 0.0), p("$prefix-$index-c", x + 0.04, 0.08), p("$prefix-$index-d", x - 0.04, 0.08))
    }
    val objects = terms.mapIndexed { index, (label, _) ->
        o("$prefix-term-$index", label, ConstructionObjectKind.Polygon, "$prefix-$index-a", "$prefix-$index-b", "$prefix-$index-c", "$prefix-$index-d")
    }
    return ConstructionGeometryState(points = points, objects = objects, revision = 1)
}

private fun parabolaPoints(prefix: String, start: Double, end: Double, count: Int, zOffset: Double = 0.0): List<ConstructionPoint> =
    (0 until count).map { index ->
        val t = index / (count - 1).toDouble()
        val x = start + (end - start) * t
        val z = ((t - 0.5) * (t - 0.5) * 0.55 - 0.05) + zOffset
        p("$prefix-$index", x, z)
    }

private fun parabolaObjects(prefix: String, count: Int): List<ConstructionObject> =
    (0 until count - 1).map { index ->
        o("$prefix-seg-$index", "Graph", ConstructionObjectKind.Segment, "$prefix-$index", "$prefix-${index + 1}")
    }

private fun p(id: String, x: Double, z: Double, y: Double = 0.0): ConstructionPoint =
    ConstructionPoint(id, id.uppercase(), Vector3Value(x, y, z))

private fun o(id: String, label: String, kind: ConstructionObjectKind, vararg pointIds: String): ConstructionObject =
    ConstructionObject(
        id = id,
        label = label,
        kind = kind,
        pointIds = pointIds.toList(),
        dependencyKind = when (kind) {
            ConstructionObjectKind.Circle -> ConstructionDependencyKind.CircleCenterPoint
            ConstructionObjectKind.Plane -> ConstructionDependencyKind.PlaneThroughThreePoints
            ConstructionObjectKind.Polygon -> ConstructionDependencyKind.PolygonThroughPoints
            ConstructionObjectKind.Vector -> ConstructionDependencyKind.VectorBetweenPoints
            else -> ConstructionDependencyKind.ThroughTwoPoints
        }
    )

private fun emptyMarkerConstruction(markerId: String): ConstructionGeometryState =
    ConstructionGeometryState(
        points = listOf(ConstructionPoint("$markerId-origin", markerId, Vector3Value())),
        objects = listOf(ConstructionObject("$markerId-point", markerId, ConstructionObjectKind.Point, listOf("$markerId-origin"), ConstructionDependencyKind.Free)),
        revision = 1
    )

private fun ArPerformanceProfile.toGraphQualityPreset(): GraphQualityPreset = when (this) {
    ArPerformanceProfile.BatterySaver -> GraphQualityPreset.BatterySaver
    ArPerformanceProfile.Balanced -> GraphQualityPreset.Balanced
    ArPerformanceProfile.HighQuality -> GraphQualityPreset.HighQuality
    ArPerformanceProfile.Presentation -> GraphQualityPreset.Presentation
}

private fun String.toArEngineMode(): ArEngineMode = when (this) {
    "PaperGraph" -> ArEngineMode.PaperGraph
    "OutdoorGeospatialMath" -> ArEngineMode.OutdoorGeospatialMath
    "SurfacePlacement" -> ArEngineMode.SurfacePlacement
    "AirPlacement" -> ArEngineMode.AirPlacement
    else -> ArEngineMode.Indoor
}

private fun ArEngineMode.toMathArExperience(): MathArExperience = when (this) {
    ArEngineMode.PaperGraph -> MathArExperience.MarkerBasedGraph
    ArEngineMode.OutdoorGeospatialMath -> MathArExperience.OutdoorGeometry
    else -> MathArExperience.MarkerlessObjects
}

private fun ArEngineMode.toShape3dAnchorMode(): Shape3dAnchorMode = when (this) {
    ArEngineMode.PaperGraph -> Shape3dAnchorMode.MarkerImage
    ArEngineMode.OutdoorGeospatialMath -> Shape3dAnchorMode.OutdoorMesh
    ArEngineMode.AirPlacement -> Shape3dAnchorMode.Air
    ArEngineMode.Indoor,
    ArEngineMode.SurfacePlacement -> Shape3dAnchorMode.SurfacePlane
}

private fun ArDepthOcclusionMode.label(): String = when (this) {
    ArDepthOcclusionMode.Off -> "Depth off"
    ArDepthOcclusionMode.SoftDepth -> "Soft depth"
    ArDepthOcclusionMode.DepthTest -> "Depth test"
    ArDepthOcclusionMode.GeospatialDepth -> "Geospatial depth"
}

private fun ArPerformanceProfile.label(): String = when (this) {
    ArPerformanceProfile.BatterySaver -> "Battery saver"
    ArPerformanceProfile.Balanced -> "Balanced"
    ArPerformanceProfile.HighQuality -> "High quality"
    ArPerformanceProfile.Presentation -> "Presentation"
}

private fun PaperGraphLayer.label(): String = when (this) {
    PaperGraphLayer.Axes -> "Axes"
    PaperGraphLayer.Scale -> "Scale"
    PaperGraphLayer.Graph -> "Graph"
    PaperGraphLayer.Surface3d -> "3D Surface"
    PaperGraphLayer.CrossSection -> "Cross Section"
}

private fun ArFeaturePhase.label(): String = when (this) {
    ArFeaturePhase.DirectInteraction -> "Phase 1"
    ArFeaturePhase.GraphAnalysis -> "Phase 2"
    ArFeaturePhase.EngineStrengthening -> "Phase 3"
    ArFeaturePhase.WorkflowStudio -> "Phase 6"
}

private fun MathArFeature.label(): String = when (this) {
    MathArFeature.ObjectSnapping -> "Object snapping"
    MathArFeature.GestureHandles -> "Gesture handles"
    MathArFeature.LiveEquationEditing -> "Live equation editing"
    MathArFeature.PointPicker -> "Point picker"
    MathArFeature.RootVisualizer -> "Root visualizer"
    MathArFeature.TangentNormalTool -> "Tangent / normal"
    MathArFeature.AreaUnderCurve -> "Area under curve"
    MathArFeature.VolumeBuilder -> "Volume builder"
    MathArFeature.MeasurementRulerAnchors -> "Ruler anchors"
    MathArFeature.CoordinateGridLocking -> "Coordinate grid locking"
    MathArFeature.MultiObjectConstraints -> "Multi-object constraints"
    MathArFeature.DepthOcclusion -> "Depth occlusion"
    MathArFeature.ScenePersistence -> "Scene persistence"
    MathArFeature.PrecisionConfidenceHud -> "Precision HUD"
    MathArFeature.CompareMode -> "Compare mode"
    MathArFeature.GuidedWorkflow -> "Guided workflow"
    MathArFeature.ActivityExport -> "Activity export"
    MathArFeature.EvidenceRecorder -> "Evidence recorder"
    MathArFeature.ParametricGraphing -> "Parametric graphing"
    MathArFeature.ImplicitRelations -> "Implicit relations"
    MathArFeature.LocusTrace -> "Locus trace"
    MathArFeature.ProofChecker -> "Proof checker"
    MathArFeature.MacroTools -> "Macro tools"
    MathArFeature.CasCommands -> "CAS commands"
}

private fun equationValue(equation: String, x: Float): Float {
    val clean = equation.lowercase().replace(" ", "")
    return when {
        "cos" in clean -> kotlin.math.cos(x)
        "x^2" in clean || "x2" in clean || "parabola" in clean -> x * x
        "2x" in clean || "2*x" in clean -> 2f * x
        "0.5x" in clean || "0.5*x" in clean -> 0.5f * x
        clean.contains("x") && !clean.contains("sin") -> x
        else -> kotlin.math.sin(x)
    }
}

private fun snappedPosition(point: ArPointLabel, state: ArViewerUiState): Vector3Value {
    fun snap(value: Float, interval: Float): Double =
        if (interval <= 0f) value.toDouble() else (kotlin.math.round(value / interval) * interval).toDouble()
    val interval = when {
        state.coordinateGridLocked -> 0.1f
        state.snappingEnabled -> 0.05f
        else -> 0f
    }
    return Vector3Value(
        x = snap(point.x, interval),
        y = if (state.coordinateGridLocked || state.snappingEnabled) 0.0 else point.y.toDouble(),
        z = snap(point.z, interval)
    )
}

private fun MathArExperience.userMessage(): String = when (this) {
    MathArExperience.MarkerlessObjects -> "Markerless math objects. Place graphs, solids and vectors on surfaces."
    MathArExperience.Drawing2dTo3d -> "Drawing 2D to 3D. Use calibration, color maps and slicing tools."
    MathArExperience.MarkerBasedGraph -> "Marker-based graph mode. Scan a worksheet or printed target."
    MathArExperience.OutdoorGeometry -> "Outdoor geometry mode. Scan buildings and terrain."
    MathArExperience.SolarSystem -> "Solar System mode. Place an orbit model markerlessly."
    MathArExperience.SceneTools -> "Scene tools. Manage layers, captures, labels and saved scenes."
}

private data class ArSensorReadiness(
    val ready: Boolean,
    val userMessage: String,
    val accelerometer: String? = null,
    val gyroscope: String? = null,
    val uncalibratedGyroscope: String? = null,
    val rotationVector: String? = null
)
