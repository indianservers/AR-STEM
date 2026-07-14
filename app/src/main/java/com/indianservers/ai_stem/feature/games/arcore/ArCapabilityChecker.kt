package com.indianservers.ai_stem.feature.games.arcore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk

interface ArCapabilityChecker {
    fun checkAvailability(context: Context): ArAvailabilityResult
}

class AndroidArCapabilityChecker : ArCapabilityChecker {
    override fun checkAvailability(context: Context): ArAvailabilityResult {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val arAvailability = runCatching { ArCoreApk.getInstance().checkAvailability(context) }
            .getOrElse { return ArAvailabilityResult.Failure }
        return mapArCoreAvailability(arAvailability, cameraGranted)
    }
}

fun mapArCoreAvailability(
    availability: ArCoreApk.Availability,
    cameraGranted: Boolean
): ArAvailabilityResult =
    when {
        availability.isTransient -> ArAvailabilityResult.TemporarilyUnavailable
        availability == ArCoreApk.Availability.SUPPORTED_INSTALLED && !cameraGranted -> ArAvailabilityResult.CameraPermissionRequired
        availability == ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArAvailabilityResult.SupportedReady
        availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArAvailabilityResult.SupportedInstallRequired
        availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArAvailabilityResult.SupportedUpdateRequired
        availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArAvailabilityResult.Unsupported
        else -> ArAvailabilityResult.Failure
    }
