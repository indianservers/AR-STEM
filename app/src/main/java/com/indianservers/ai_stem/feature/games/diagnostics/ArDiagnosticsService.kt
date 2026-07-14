package com.indianservers.ai_stem.feature.games.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.indianservers.ai_stem.feature.games.arcore.AndroidArCapabilityChecker
import com.indianservers.ai_stem.feature.games.arcore.ArAvailabilityResult
import com.indianservers.ai_stem.feature.games.arcore.ArMathArenaCapabilityPolicy
import com.indianservers.ai_stem.feature.games.arcore.ArSessionConfigurationService

class ArDiagnosticsService(
    private val checker: AndroidArCapabilityChecker = AndroidArCapabilityChecker(),
    private val configurationService: ArSessionConfigurationService = ArSessionConfigurationService()
) {
    fun inspect(context: Context): ArDiagnosticsReport {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val availability = checker.checkAvailability(context)
        val flashSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        var sessionState = ArSessionDiagnosticState.NotStarted
        var lastError: String? = null
        val matrix = if (availability == ArAvailabilityResult.SupportedReady && cameraGranted) {
            val session = runCatching { Session(context) }
                .onFailure {
                    sessionState = ArSessionDiagnosticState.Error
                    lastError = it.message ?: it::class.java.simpleName
                }
                .getOrNull()
            if (session != null) {
                sessionState = ArSessionDiagnosticState.Ready
                val depthSupported = runCatching { session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) }.getOrDefault(false)
                val rawDepthSupported = runCatching { session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY) }.getOrDefault(false)
                val semanticSupported = runCatching { session.isSemanticModeSupported(Config.SemanticMode.ENABLED) }.getOrDefault(false)
                val capabilityMatrix = ArMathArenaCapabilityPolicy.matrix(
                    availability = availability,
                    depthSupported = depthSupported,
                    rawDepthSupported = rawDepthSupported,
                    flashSupported = flashSupported,
                    sceneSemanticsSupported = semanticSupported
                )
                runCatching {
                    val config = configurationService.configureForMathArena(session, Config(session), capabilityMatrix)
                    session.configure(config)
                }.onFailure {
                    lastError = it.message ?: it::class.java.simpleName
                }
                session.close()
                sessionState = ArSessionDiagnosticState.Closed
                capabilityMatrix
            } else {
                ArMathArenaCapabilityPolicy.matrix(availability, flashSupported = flashSupported)
            }
        } else {
            ArMathArenaCapabilityPolicy.matrix(availability, flashSupported = flashSupported)
        }
        return ArDiagnosticsReport(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            arCoreAvailability = availability,
            playServicesForArStatus = availability.playServicesStatus(),
            cameraPermission = if (cameraGranted) CameraPermissionDiagnostic.Granted else CameraPermissionDiagnostic.Denied,
            capabilityMatrix = matrix,
            flashSupported = flashSupported,
            rendererReady = availability == ArAvailabilityResult.SupportedReady && cameraGranted,
            sessionState = sessionState,
            lastError = lastError
        )
    }
}

private fun ArAvailabilityResult.playServicesStatus(): String = when (this) {
    ArAvailabilityResult.Checking -> "Checking"
    ArAvailabilityResult.SupportedReady -> "Installed and ready"
    ArAvailabilityResult.SupportedInstallRequired -> "Installation required"
    ArAvailabilityResult.SupportedUpdateRequired -> "Update required"
    ArAvailabilityResult.CameraPermissionRequired -> "Installed; camera permission required"
    ArAvailabilityResult.Unsupported -> "Unsupported device"
    ArAvailabilityResult.TemporarilyUnavailable -> "Temporarily unavailable"
    ArAvailabilityResult.Failure -> "Availability check failed"
}
