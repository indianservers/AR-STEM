package com.indianservers.ai_stem.feature.games.spatial

enum class ArCameraTrackingQuality { Tracking, Paused, Stopped }
enum class MarkerTrackingQuality { NotDetected, Detected, LastKnownPose, FullTracking }

enum class CalibrationState {
    Searching,
    MarkerDetected,
    Stabilizing,
    Ready,
    TrackingWeak,
    MarkerLost,
    RecalibrationRequired
}

data class CalibrationSample(
    val cameraTracking: ArCameraTrackingQuality,
    val markerTracking: MarkerTrackingQuality,
    val markerExtentXMetres: Float,
    val markerExtentZMetres: Float,
    val markerPose: LocalPoseDto?,
    val distanceFromMarkerMetres: Float,
    val viewingAngleDegrees: Float,
    val lightEstimate: Float?,
    val deviceMotionMetresPerSecond: Float,
    val timestampMs: Long
)

data class CalibrationQuality(
    val state: CalibrationState,
    val score: Int,
    val instruction: String,
    val canAccept: Boolean,
    val poseVarianceMetres: Float,
    val extentStabilityMetres: Float,
    val sampleCount: Int
)

class CalibrationReadinessEvaluator(
    private val markerWidthMetres: Float = AR_MATH_ARENA_MARKER_WIDTH_METRES,
    private val requiredSamples: Int = 12
) {
    fun evaluate(samples: List<CalibrationSample>): CalibrationQuality {
        val window = samples.takeLast(requiredSamples)
        if (window.isEmpty()) return quality(CalibrationState.Searching, 0, "Point the full camera at the AR Math Arena Origin marker.")
        val latest = window.last()
        if (latest.cameraTracking != ArCameraTrackingQuality.Tracking) {
            return quality(CalibrationState.TrackingWeak, 18, "Move the phone slowly and keep it steady.", window)
        }
        if (latest.markerTracking == MarkerTrackingQuality.NotDetected) {
            return quality(CalibrationState.Searching, 20, "Point the full camera at the marker and avoid covering it.", window)
        }
        if (latest.markerTracking == MarkerTrackingQuality.LastKnownPose) {
            return quality(CalibrationState.MarkerLost, 28, "Marker lost. Show the printed marker again.", window)
        }
        if (latest.markerTracking == MarkerTrackingQuality.Detected) {
            return quality(CalibrationState.MarkerDetected, 42, "Hold the device steady while tracking locks.", window)
        }
        val extentStability = extentStability(window)
        val poseVariance = poseVariance(window)
        val enoughSamples = window.size >= requiredSamples
        val closeEnough = latest.distanceFromMarkerMetres in 0.25f..2.2f
        val angleGood = latest.viewingAngleDegrees <= 55f
        val lightGood = latest.lightEstimate == null || latest.lightEstimate >= 0.35f
        val motionGood = latest.deviceMotionMetresPerSecond <= 0.18f
        val extentGood = extentStability <= markerWidthMetres * 0.035f
        val poseGood = poseVariance <= 0.018f
        val score = listOf(enoughSamples, closeEnough, angleGood, lightGood, motionGood, extentGood, poseGood).count { it } * 14
        val instruction = when {
            !closeEnough && latest.distanceFromMarkerMetres > 2.2f -> "Move closer to the marker."
            !angleGood -> "Face the marker more directly and reduce glare."
            !lightGood -> "Improve room lighting or use Improve Lighting if supported."
            !motionGood -> "Hold the device steady."
            !extentGood -> "Keep the marker flat and fully visible."
            !poseGood -> "Wait for the pose to stabilize."
            !enoughSamples -> "Stabilizing marker pose."
            else -> "Ready. Shared origin can be accepted."
        }
        val ready = enoughSamples && closeEnough && angleGood && lightGood && motionGood && extentGood && poseGood
        return CalibrationQuality(
            state = if (ready) CalibrationState.Ready else CalibrationState.Stabilizing,
            score = score.coerceIn(0, 100),
            instruction = instruction,
            canAccept = ready,
            poseVarianceMetres = poseVariance,
            extentStabilityMetres = extentStability,
            sampleCount = window.size
        )
    }

    private fun quality(state: CalibrationState, score: Int, instruction: String, samples: List<CalibrationSample> = emptyList()): CalibrationQuality =
        CalibrationQuality(state, score, instruction, canAccept = false, poseVarianceMetres = poseVariance(samples), extentStabilityMetres = extentStability(samples), sampleCount = samples.size)

    private fun poseVariance(samples: List<CalibrationSample>): Float {
        val poses = samples.mapNotNull { it.markerPose?.translationMetres }
        if (poses.size < 2) return Float.MAX_VALUE
        val average = poses.reduce { a, b -> a + b } * (1f / poses.size)
        return poses.maxOf { (it - average).magnitude() }
    }

    private fun extentStability(samples: List<CalibrationSample>): Float {
        if (samples.size < 2) return Float.MAX_VALUE
        val values = samples.map { (it.markerExtentXMetres + it.markerExtentZMetres) * 0.5f }
        return (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f)
    }
}

data class PlayerCalibrationStatus(
    val playerId: String,
    val displayName: String,
    val calibrationState: CalibrationState,
    val qualityScore: Int,
    val instruction: String,
    val originVersion: Long?,
    val nonArAnalyst: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
