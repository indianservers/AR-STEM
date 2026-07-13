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
import com.indianservers.ai_stem.domain.scene.DefaultObjectTransformService
import com.indianservers.ai_stem.domain.scene.ExperienceMode
import com.indianservers.ai_stem.domain.scene.SceneInteractionMode
import com.indianservers.ai_stem.domain.scene.SceneMutations
import com.indianservers.ai_stem.domain.scene.SnapshotSceneCommand
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
                        ArEngineMode.PaperGraph -> "Paper Graph mode. Point at a worksheet or calibrated graph target."
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
                        "No stable flat surface under the tap yet. Move sideways slowly and aim at a textured floor or table."
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
        _uiState.update {
            it.copy(
                paperGraph = frameState,
                arDiagnostics = buildList {
                    addAll(it.arDiagnostics.filterNot { diagnostic -> diagnostic.startsWith("Paper graph") || diagnostic.startsWith("Augmented image") })
                    add("Paper graph targets: ${frameState.trackedTargets.size}")
                    add("Paper graph locked: ${frameState.hasLockedTarget}")
                    frameState.trackedTargets.firstOrNull()?.let { target ->
                        add("Augmented image: ${target.name} ${"%.2f".format(target.extentX)}m x ${"%.2f".format(target.extentZ)}m ${target.trackingMethod}")
                    }
                }.take(12)
            )
        }
    }

    fun onPaperGraphCalibrationTap(point: PaperGraphCalibrationPoint) {
        _uiState.update {
            if (it.arEngineMode != ArEngineMode.PaperGraph || it.paperGraph?.hasLockedTarget != true) {
                return@update it.copy(userMessage = UiMessage("Scan and lock the worksheet target before calibration."))
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
            it.copy(
                liveEquation = equation.take(80),
                selectedObjectType = MathObjectType.SineCurve,
                selectedDefinitionId = "sine-curve",
                userMessage = UiMessage("AR equation updated.")
            )
        }
    }

    fun addGeneratedPointLabel() {
        _uiState.update {
            val index = it.pickedPoints.size + 1
            val x = -1f + index * 0.5f
            val y = equationValue(it.liveEquation, x)
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
            val point = ArPointLabel(
                label = "P$index (${ "%.2f".format(worldX) }, ${ "%.2f".format(worldY) }, ${ "%.2f".format(worldZ) })",
                x = worldX,
                y = worldY,
                z = worldZ
            )
            it.copy(
                pickedPoints = (it.pickedPoints + point).takeLast(6),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.PointPicker,
                labelsVisible = true,
                userMessage = UiMessage("Picked ${point.label}.")
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
            val anchor = ArPointLabel("R$index", worldX, worldY, worldZ)
            it.copy(
                rulerAnchors = (it.rulerAnchors + anchor).takeLast(2),
                enabledMathArFeatures = it.enabledMathArFeatures + MathArFeature.MeasurementRulerAnchors,
                measurementsVisible = true,
                userMessage = UiMessage("Ruler anchor ${anchor.label} placed.")
            )
        }
    }

    fun recordPlacementPoint(worldX: Float, worldY: Float, worldZ: Float) {
        _uiState.update {
            it.copy(lastPlacementPoint = ArPointLabel("anchor", worldX, worldY, worldZ))
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
        Log.d("AiStemAR", "Anchor established objects=${_uiState.value.mathScene.objects.size}")
        if (_uiState.value.mathScene.objects.isEmpty()) {
            val current = _uiState.value
            val placement = current.lastPlacementPoint?.let { point ->
                snappedPosition(point, current)
            } ?: Vector3Value()
            mutate("Place object") { SceneMutations.addObject(it, current.selectedDefinitionId, placement) }
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
        when (val result = repository.save(_uiState.value.mathScene, name)) {
            is SceneStorageResult.Success -> _uiState.update {
                it.copy(mathScene = result.scene, savedScenes = repository.listScenes(), userMessage = UiMessage("Scene saved"))
            }
            is SceneStorageResult.Failure -> _uiState.update { it.copy(userMessage = UiMessage(result.userMessage)) }
        }
    }

    fun loadScene(sceneId: String) {
        val repository = sceneRepository ?: return
        when (val result = repository.load(sceneId)) {
            is SceneStorageResult.Success -> _uiState.update {
                it.copy(
                    mathScene = result.scene,
                    loadedSceneNeedsPlacement = true,
                    savedScenesVisible = false,
                    userMessage = UiMessage("Tap a surface to place this scene")
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
