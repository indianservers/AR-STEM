package com.indianservers.ai_stem.core.ar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

enum class PaperGraphTrackingMethod { FullTracking, LastKnownPose, NotTracking }
enum class GraphColorMap { Height, Slope, Curvature, XValue, YValue }

data class PaperGraphTargetObservation(
    val index: Int,
    val name: String,
    val extentX: Float,
    val extentZ: Float,
    val trackingState: TrackingState,
    val trackingMethod: PaperGraphTrackingMethod
)

data class PaperGraphFrameState(
    val enabled: Boolean,
    val configuredReferenceCount: Int,
    val trackedTargets: List<PaperGraphTargetObservation>
) {
    val hasLockedTarget: Boolean
        get() = trackedTargets.any { it.trackingState == TrackingState.TRACKING && it.trackingMethod == PaperGraphTrackingMethod.FullTracking }
}

object AugmentedImageArEngine {
    fun configureSession(session: Session, config: Config, enabled: Boolean): PaperGraphSessionConfigurationResult {
        if (!enabled) {
            return PaperGraphSessionConfigurationResult(enabled = false, referenceImageCount = 0)
        }
        val database = AugmentedImageDatabase(session)
        database.addImage(DEFAULT_TARGET_NAME, createDefaultWorksheetTarget(), DEFAULT_TARGET_WIDTH_METERS)
        config.augmentedImageDatabase = database
        return PaperGraphSessionConfigurationResult(enabled = true, referenceImageCount = database.numImages)
    }

    fun observeFrame(frame: Frame): PaperGraphFrameState {
        val images = frame.getUpdatedTrackables(AugmentedImage::class.java)
            .filter { it.trackingState == TrackingState.TRACKING || it.trackingState == TrackingState.PAUSED }
            .map { image ->
                PaperGraphTargetObservation(
                    index = image.index,
                    name = image.name.orEmpty().ifBlank { "Paper graph ${image.index + 1}" },
                    extentX = image.extentX,
                    extentZ = image.extentZ,
                    trackingState = image.trackingState,
                    trackingMethod = image.trackingMethod.toPaperGraphTrackingMethod()
                )
            }
            .take(MAX_TRACKED_IMAGES)
        return PaperGraphFrameState(
            enabled = true,
            configuredReferenceCount = 1,
            trackedTargets = images
        )
    }

    private fun AugmentedImage.TrackingMethod.toPaperGraphTrackingMethod(): PaperGraphTrackingMethod =
        when (this) {
            AugmentedImage.TrackingMethod.FULL_TRACKING -> PaperGraphTrackingMethod.FullTracking
            AugmentedImage.TrackingMethod.LAST_KNOWN_POSE -> PaperGraphTrackingMethod.LastKnownPose
            AugmentedImage.TrackingMethod.NOT_TRACKING -> PaperGraphTrackingMethod.NotTracking
        }

    private const val MAX_TRACKED_IMAGES = 20
    private const val DEFAULT_TARGET_NAME = "AI STEM Paper Graph"
    private const val DEFAULT_TARGET_WIDTH_METERS = 0.21f

    private fun createDefaultWorksheetTarget(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 18f
        }
        canvas.drawRect(18f, 18f, size - 18f, size - 18f, border)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 219, 238)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        for (i in 1 until 10) {
            val p = 18f + i * ((size - 36f) / 10f)
            canvas.drawLine(18f, p, size - 18f, p, grid)
            canvas.drawLine(p, 18f, p, size - 18f, grid)
        }

        val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 25, 25)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val center = size / 2f
        canvas.drawLine(54f, center, size - 54f, center, axis)
        canvas.drawLine(center, 54f, center, size - 54f, axis)

        val curve = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 40, 40)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        var lastX = 54f
        var lastY = center
        for (i in 1..160) {
            val t = i / 160f
            val x = 54f + t * (size - 108f)
            val graphX = (t - 0.5f) * 4f
            val y = center - (graphX * graphX - 1.2f) * 38f
            canvas.drawLine(lastX, lastY, x, y, curve)
            lastX = x
            lastY = y
        }

        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawCircle(74f, 74f, 24f, markerPaint)
        canvas.drawCircle(size - 82f, 98f, 16f, markerPaint)
        canvas.drawRect(size - 116f, size - 116f, size - 62f, size - 62f, markerPaint)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 55, 110)
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("AI STEM GRAPH", 58f, size - 54f, text)
        return bitmap
    }
}

data class PaperGraphSessionConfigurationResult(
    val enabled: Boolean,
    val referenceImageCount: Int
)
