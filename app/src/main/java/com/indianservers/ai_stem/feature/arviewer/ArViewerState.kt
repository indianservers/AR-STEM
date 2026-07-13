package com.indianservers.ai_stem.feature.arviewer

import com.indianservers.ai_stem.data.scene.SavedSceneSummary
import com.indianservers.ai_stem.core.ar.ArGuidanceSeverity
import com.indianservers.ai_stem.core.ar.ArEngineMode
import com.indianservers.ai_stem.core.ar.ArPlacementMode
import com.indianservers.ai_stem.core.ar.OutdoorGeospatialFrameState
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

data class UiMessage(val text: String)

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
