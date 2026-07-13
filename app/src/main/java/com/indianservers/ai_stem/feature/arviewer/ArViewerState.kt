package com.indianservers.ai_stem.feature.arviewer

import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.clampScale
import com.indianservers.ai_stem.domain.mathematics.normalizeRotationDegrees
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
enum class ArSessionStatus { Initializing, Scanning, PlaneDetected, ReadyToPlace, ObjectPlaced, ObjectSelected, MovingObject, TrackingLost, Paused, FatalError }
enum class TrackingStatus { Unknown, Tracking, Limited, Paused }
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
    val sessionStatus: ArSessionStatus = ArSessionStatus.Initializing,
    val trackingStatus: TrackingStatus = TrackingStatus.Unknown,
    val selectedObjectType: MathObjectType = MathObjectType.Cube,
    val placedObject: PlacedMathObject? = null,
    val hasValidPlacementHit: Boolean = false,
    val planesVisible: Boolean = true,
    val interactionMode: InteractionMode = InteractionMode.Placement,
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
                rotationDegrees = normalizeRotationDegrees(objectState.rotationDegrees + command.deltaDegrees)
            )
        )
        is ObjectInteractionCommand.Scale -> state.copy(
            placedObject = objectState.copy(scaleFactor = clampScale(objectState.scaleFactor * command.scaleFactor))
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
