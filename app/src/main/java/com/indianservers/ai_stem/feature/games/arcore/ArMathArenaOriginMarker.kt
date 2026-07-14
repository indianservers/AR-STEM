package com.indianservers.ai_stem.feature.games.arcore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.indianservers.ai_stem.feature.games.spatial.AR_MATH_ARENA_MARKER_NAME
import com.indianservers.ai_stem.feature.games.spatial.AR_MATH_ARENA_MARKER_WIDTH_METRES
import com.indianservers.ai_stem.feature.games.spatial.ArCameraTrackingQuality
import com.indianservers.ai_stem.feature.games.spatial.CalibrationSample
import com.indianservers.ai_stem.feature.games.spatial.LocalPoseDto
import com.indianservers.ai_stem.feature.games.spatial.MarkerTrackingQuality
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.abs
import kotlin.math.acos

data class ArenaMarkerSessionConfiguration(
    val augmentedImageCount: Int,
    val depthEnabled: Boolean,
    val environmentalHdrEnabled: Boolean,
    val instantPlacementPreviewEnabled: Boolean
)

data class ArenaMarkerObservation(
    val markerName: String,
    val markerTracking: MarkerTrackingQuality,
    val extentXMetres: Float,
    val extentZMetres: Float,
    val pose: LocalPoseDto?,
    val sample: CalibrationSample
)

object ArMathArenaOriginMarker {
    fun configureMarkerSession(session: Session, config: Config, enableInstantPreview: Boolean): ArenaMarkerSessionConfiguration {
        val database = AugmentedImageDatabase(session)
        database.addImage(AR_MATH_ARENA_MARKER_NAME, createMarkerBitmap(), AR_MATH_ARENA_MARKER_WIDTH_METRES)
        config.augmentedImageDatabase = database
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.geospatialMode = Config.GeospatialMode.DISABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        val depthEnabled = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        config.depthMode = if (depthEnabled) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
        config.instantPlacementMode = if (enableInstantPreview) Config.InstantPlacementMode.LOCAL_Y_UP else Config.InstantPlacementMode.DISABLED
        return ArenaMarkerSessionConfiguration(
            augmentedImageCount = database.numImages,
            depthEnabled = depthEnabled,
            environmentalHdrEnabled = true,
            instantPlacementPreviewEnabled = enableInstantPreview
        )
    }

    fun observe(frame: Frame, nowMs: Long = System.currentTimeMillis(), previousPose: LocalPoseDto? = null): ArenaMarkerObservation? {
        val cameraQuality = when (frame.camera.trackingState) {
            TrackingState.TRACKING -> ArCameraTrackingQuality.Tracking
            TrackingState.PAUSED -> ArCameraTrackingQuality.Paused
            TrackingState.STOPPED -> ArCameraTrackingQuality.Stopped
        }
        val image = frame.getUpdatedTrackables(AugmentedImage::class.java)
            .filter { it.name == AR_MATH_ARENA_MARKER_NAME }
            .maxByOrNull { if (it.trackingState == TrackingState.TRACKING) 1 else 0 }
            ?: return null
        val pose = if (image.trackingState == TrackingState.TRACKING) image.centerPose.toLocalPoseDto() else null
        val distance = pose?.translationMetres?.magnitude() ?: Float.MAX_VALUE
        val motion = if (pose != null && previousPose != null) (pose.translationMetres - previousPose.translationMetres).magnitude() else 0f
        val light = runCatching { frame.lightEstimate.pixelIntensity }.getOrNull()
        val sample = CalibrationSample(
            cameraTracking = cameraQuality,
            markerTracking = image.toMarkerTrackingQuality(),
            markerExtentXMetres = image.extentX,
            markerExtentZMetres = image.extentZ,
            markerPose = pose,
            distanceFromMarkerMetres = distance,
            viewingAngleDegrees = pose?.viewingAngleDegrees() ?: 90f,
            lightEstimate = light,
            deviceMotionMetresPerSecond = motion,
            timestampMs = nowMs
        )
        return ArenaMarkerObservation(
            markerName = image.name.orEmpty(),
            markerTracking = image.toMarkerTrackingQuality(),
            extentXMetres = image.extentX,
            extentZMetres = image.extentZ,
            pose = pose,
            sample = sample
        )
    }

    fun createMarkerBitmap(size: Int = 1200): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
        val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 74, 156); style = Paint.Style.FILL }
        val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(205, 38, 38); style = Paint.Style.FILL }
        val yellow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(248, 197, 40); style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = size * 0.028f }
        canvas.drawRect(size * 0.04f, size * 0.04f, size * 0.96f, size * 0.96f, stroke)
        canvas.drawRect(size * 0.09f, size * 0.09f, size * 0.29f, size * 0.29f, black)
        canvas.drawRect(size * 0.71f, size * 0.09f, size * 0.91f, size * 0.29f, blue)
        canvas.drawCircle(size * 0.2f, size * 0.8f, size * 0.1f, red)
        canvas.drawCircle(size * 0.8f, size * 0.8f, size * 0.07f, black)
        for (i in 0 until 9) {
            val left = size * (0.36f + i * 0.032f)
            val top = size * (0.18f + (i % 3) * 0.08f)
            canvas.drawRect(left, top, left + size * 0.018f, top + size * (0.09f + i * 0.006f), if (i % 2 == 0) black else blue)
        }
        val arrow = Path().apply {
            moveTo(size * 0.5f, size * 0.13f)
            lineTo(size * 0.58f, size * 0.33f)
            lineTo(size * 0.52f, size * 0.31f)
            lineTo(size * 0.52f, size * 0.58f)
            lineTo(size * 0.48f, size * 0.58f)
            lineTo(size * 0.48f, size * 0.31f)
            lineTo(size * 0.42f, size * 0.33f)
            close()
        }
        canvas.drawPath(arrow, yellow)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = size * 0.043f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AR Math Arena Origin", size * 0.5f, size * 0.67f, text)
        text.textSize = size * 0.027f
        text.isFakeBoldText = false
        canvas.drawText("Print width: 18 cm  |  Keep flat and well lit", size * 0.5f, size * 0.72f, text)
        canvas.drawText("+X right   +Y outward   +Z arrow forward", size * 0.5f, size * 0.76f, text)
        val qr = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 238, 242); style = Paint.Style.FILL }
        canvas.drawRect(size * 0.36f, size * 0.8f, size * 0.64f, size * 0.92f, qr)
        text.textSize = size * 0.022f
        canvas.drawText("QR JOIN AREA", size * 0.5f, size * 0.875f, text)
        return bitmap
    }

    private fun AugmentedImage.toMarkerTrackingQuality(): MarkerTrackingQuality =
        when {
            trackingState == TrackingState.PAUSED -> MarkerTrackingQuality.Detected
            trackingState != TrackingState.TRACKING -> MarkerTrackingQuality.NotDetected
            trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING -> MarkerTrackingQuality.FullTracking
            trackingMethod == AugmentedImage.TrackingMethod.LAST_KNOWN_POSE -> MarkerTrackingQuality.LastKnownPose
            else -> MarkerTrackingQuality.NotDetected
        }

    private fun Pose.toLocalPoseDto(): LocalPoseDto =
        LocalPoseDto(
            translationMetres = Vector3Dto(tx(), ty(), tz()),
            rotation = QuaternionDto(qx(), qy(), qz(), qw())
        )

    private fun LocalPoseDto.viewingAngleDegrees(): Float {
        val forwardDot = abs(rotation.w).coerceIn(0f, 1f)
        return Math.toDegrees((2f * acos(forwardDot)).toDouble()).toFloat().coerceIn(0f, 90f)
    }
}
