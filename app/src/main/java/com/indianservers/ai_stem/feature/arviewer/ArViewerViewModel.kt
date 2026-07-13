package com.indianservers.ai_stem.feature.arviewer

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.ar.core.ArCoreApk
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ArViewerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArViewerUiState())
    val uiState: StateFlow<ArViewerUiState> = _uiState

    fun checkAvailability(context: Context) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val availability = runCatching {
            val arCore = ArCoreApk.getInstance().checkAvailability(context)
            val glOk = readGlMajorVersion() >= 3
            when {
                !glOk -> ArAvailabilityState.UnsupportedDevice
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
        Log.d("AiStemAR", "AR availability result: $availability")
        _uiState.update {
            it.copy(
                availability = availability,
                permission = if (permissionGranted) CameraPermissionState.Granted else it.permission,
                sessionStatus = if (availability == ArAvailabilityState.Supported) ArSessionStatus.Scanning else it.sessionStatus
            )
        }
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        Log.d("AiStemAR", "Permission state transition granted=$granted permanentlyDenied=$permanentlyDenied")
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

    fun selectObject(type: MathObjectType) {
        Log.d("AiStemAR", "Object selected: $type")
        _uiState.update {
            it.copy(
                selectedObjectType = type,
                userMessage = if (it.placedObject != null) UiMessage("Use Replace Object to switch the placed model.") else null
            )
        }
    }

    fun onPlaneStatus(hasHit: Boolean) {
        _uiState.update {
            it.copy(
                hasValidPlacementHit = hasHit,
                sessionStatus = when {
                    it.placedObject != null -> ArSessionStatus.ObjectPlaced
                    hasHit -> ArSessionStatus.ReadyToPlace
                    else -> ArSessionStatus.Scanning
                }
            )
        }
    }

    fun placeSelected() {
        val type = _uiState.value.selectedObjectType
        Log.d("AiStemAR", "Placement succeeded: $type")
        _uiState.update {
            it.copy(
                placedObject = PlacedMathObject(id = "phase1-active-object", type = type),
                sessionStatus = ArSessionStatus.ObjectSelected,
                interactionMode = InteractionMode.ObjectSelected,
                userMessage = UiMessage("${type.displayName} placed.")
            )
        }
    }

    fun resetTransform() {
        _uiState.update { reduceInteraction(it, ObjectInteractionCommand.ResetTransform) }
    }

    fun applyTransform(scaleFactor: Float, rotationDeltaDegrees: Float) {
        _uiState.update {
            reduceInteraction(
                reduceInteraction(it, ObjectInteractionCommand.Scale(scaleFactor)),
                ObjectInteractionCommand.Rotate(rotationDeltaDegrees)
            )
        }
    }

    fun deleteObject() {
        Log.d("AiStemAR", "Anchor detached")
        _uiState.update { reduceInteraction(it, ObjectInteractionCommand.DeleteSelected) }
    }

    fun togglePlanes() {
        _uiState.update { it.copy(planesVisible = !it.planesVisible) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun readGlMajorVersion(): Int {
        val version = GLES20.glGetString(GLES20.GL_VERSION) ?: return 3
        return version.firstOrNull { it.isDigit() }?.digitToIntOrNull() ?: 3
    }
}
