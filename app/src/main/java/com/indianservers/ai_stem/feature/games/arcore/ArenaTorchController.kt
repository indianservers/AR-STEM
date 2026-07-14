package com.indianservers.ai_stem.feature.games.arcore

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

data class ArenaTorchState(
    val supported: Boolean,
    val enabled: Boolean,
    val message: String
)

class ArenaTorchController(private val context: Context) {
    private val cameraManager: CameraManager? = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var activeCameraId: String? = null

    fun supportState(): ArenaTorchState {
        val hasFlash = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        val cameraId = findTorchCameraId()
        return ArenaTorchState(
            supported = hasFlash && cameraId != null,
            enabled = false,
            message = if (hasFlash && cameraId != null) "Improve Lighting available. Use briefly to reduce marker glare/shadows." else "Torch is not supported on this device."
        )
    }

    fun setEnabled(enabled: Boolean): ArenaTorchState {
        val cameraId = findTorchCameraId() ?: return ArenaTorchState(false, false, "Torch is not supported on this device.")
        return runCatching {
            cameraManager?.setTorchMode(cameraId, enabled)
            activeCameraId = if (enabled) cameraId else null
            ArenaTorchState(true, enabled, if (enabled) "Lighting improved. Turn off if the phone gets warm." else "Lighting assistance off.")
        }.getOrElse {
            ArenaTorchState(false, false, "Torch could not be changed: ${it.message ?: it::class.java.simpleName}")
        }
    }

    fun turnOff() {
        activeCameraId?.let { runCatching { cameraManager?.setTorchMode(it, false) } }
        activeCameraId = null
    }

    private fun findTorchCameraId(): String? {
        val manager = cameraManager ?: return null
        return manager.cameraIdList.firstOrNull { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
}
