package com.indianservers.ai_stem.feature.games.diagnostics

import com.indianservers.ai_stem.feature.games.arcore.ArAvailabilityResult
import com.indianservers.ai_stem.feature.games.arcore.ArGameCapabilityMatrix

enum class CameraPermissionDiagnostic { Granted, Denied }
enum class ArSessionDiagnosticState { NotStarted, Ready, Closed, Error }

data class ArDiagnosticsReport(
    val deviceModel: String,
    val androidVersion: String,
    val arCoreAvailability: ArAvailabilityResult,
    val playServicesForArStatus: String,
    val cameraPermission: CameraPermissionDiagnostic,
    val capabilityMatrix: ArGameCapabilityMatrix,
    val flashSupported: Boolean,
    val rendererReady: Boolean,
    val sessionState: ArSessionDiagnosticState,
    val lastError: String? = null
)
