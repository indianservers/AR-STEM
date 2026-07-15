package com.indianservers.ai_stem.core.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import org.json.JSONArray

data class MarkerArTarget(
    val id: String,
    val name: String,
    val assetPath: String,
    val widthMeters: Float,
    val category: String
)

enum class PaperGraphTrackingMethod { FullTracking, LastKnownPose, NotTracking }
enum class GraphColorMap { Height, Slope, Curvature, XValue, YValue }

data class PaperGraphTargetObservation(
    val index: Int,
    val name: String,
    val extentX: Float,
    val extentZ: Float,
    val trackingState: TrackingState,
    val trackingMethod: PaperGraphTrackingMethod,
    val updatedThisFrame: Boolean = false
) {
    val isFullyTracked: Boolean
        get() = trackingState == TrackingState.TRACKING && trackingMethod == PaperGraphTrackingMethod.FullTracking

    val estimatedAreaSquareMeters: Float
        get() = extentX.coerceAtLeast(0f) * extentZ.coerceAtLeast(0f)

    val markerQualityScore: Int
        get() {
            val trackingScore = when (trackingMethod) {
                PaperGraphTrackingMethod.FullTracking -> 70
                PaperGraphTrackingMethod.LastKnownPose -> 35
                PaperGraphTrackingMethod.NotTracking -> 0
            }
            val sizeScore = when {
                estimatedAreaSquareMeters >= 0.035f -> 30
                estimatedAreaSquareMeters >= 0.015f -> 20
                estimatedAreaSquareMeters > 0f -> 10
                else -> 0
            }
            return trackingScore + sizeScore
        }
}

data class PaperGraphFrameState(
    val enabled: Boolean,
    val configuredReferenceCount: Int,
    val trackedTargets: List<PaperGraphTargetObservation>,
    val configuredTargetNames: List<String> = emptyList()
) {
    val hasLockedTarget: Boolean
        get() = trackedTargets.any { it.trackingState == TrackingState.TRACKING && it.trackingMethod == PaperGraphTrackingMethod.FullTracking }

    val bestLockedTarget: PaperGraphTargetObservation?
        get() = trackedTargets
            .filter { it.isFullyTracked }
            .maxWithOrNull(
                compareBy<PaperGraphTargetObservation> { it.updatedThisFrame }
                    .thenBy { it.markerQualityScore }
                    .thenBy { it.estimatedAreaSquareMeters }
            )

    val guidance: String
        get() = when {
            bestLockedTarget != null -> "Marker locked. Place shapes on the paper grid."
            trackedTargets.any { it.trackingMethod == PaperGraphTrackingMethod.LastKnownPose } -> "Hold the marker fully in view."
            trackedTargets.isNotEmpty() -> "Keep scanning until image tracking locks."
            else -> "Point at the AI STEM marker or worksheet."
        }
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

    fun configureSession(context: Context, session: Session, config: Config, enabled: Boolean): PaperGraphSessionConfigurationResult {
        if (!enabled) {
            return PaperGraphSessionConfigurationResult(enabled = false, referenceImageCount = 0)
        }
        val database = AugmentedImageDatabase(session)
        val loadedTargets = loadGeometryMarkerTargets(context).mapNotNull { target ->
            val bitmap = decodeReferenceBitmap(context, target.assetPath) ?: return@mapNotNull null
            try {
                val imageIndex = database.addImage(target.name, bitmap, target.widthMeters)
                target.copy(id = "${target.id}:$imageIndex")
            } finally {
                bitmap.recycle()
            }
        }
        if (loadedTargets.isEmpty()) {
            database.addImage(DEFAULT_TARGET_NAME, createDefaultWorksheetTarget(), DEFAULT_TARGET_WIDTH_METERS)
        }
        config.augmentedImageDatabase = database
        return PaperGraphSessionConfigurationResult(
            enabled = true,
            referenceImageCount = database.numImages,
            targetNames = loadedTargets.map { it.name }.ifEmpty { listOf(DEFAULT_TARGET_NAME) }
        )
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
                    trackingMethod = image.trackingMethod.toPaperGraphTrackingMethod(),
                    updatedThisFrame = true
                )
            }
            .sortedWith(
                compareByDescending<PaperGraphTargetObservation> { it.updatedThisFrame }
                    .thenByDescending { it.markerQualityScore }
                    .thenByDescending { it.estimatedAreaSquareMeters }
                    .thenBy { it.index }
            )
            .take(MAX_TRACKED_IMAGES)
        return PaperGraphFrameState(
            enabled = true,
            configuredReferenceCount = 1,
            trackedTargets = images
        )
    }

    fun observeFrame(session: Session, frame: Frame): PaperGraphFrameState {
        val updatedIndexes = frame.getUpdatedTrackables(AugmentedImage::class.java)
            .map { it.index }
            .toSet()
        val images = session.getAllTrackables(AugmentedImage::class.java)
            .filter { it.trackingState == TrackingState.TRACKING || it.trackingState == TrackingState.PAUSED }
            .map { image ->
                PaperGraphTargetObservation(
                    index = image.index,
                    name = image.name.orEmpty().ifBlank { "Paper graph ${image.index + 1}" },
                    extentX = image.extentX,
                    extentZ = image.extentZ,
                    trackingState = image.trackingState,
                    trackingMethod = image.trackingMethod.toPaperGraphTrackingMethod(),
                    updatedThisFrame = image.index in updatedIndexes
                )
            }
            .sortedWith(
                compareByDescending<PaperGraphTargetObservation> { it.updatedThisFrame }
                    .thenByDescending { it.markerQualityScore }
                    .thenByDescending { it.estimatedAreaSquareMeters }
                    .thenBy { it.index }
            )
            .take(MAX_TRACKED_IMAGES)
        return PaperGraphFrameState(
            enabled = true,
            configuredReferenceCount = 1,
            trackedTargets = images
        )
    }

    fun loadGeometryMarkerTargets(context: Context): List<MarkerArTarget> =
        runCatching {
            val json = context.assets.open(GEOMETRY_MARKER_CATALOG).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val width = item.optDouble("widthMeters", DEFAULT_GEOMETRY_MARKER_WIDTH_METERS.toDouble()).toFloat()
                MarkerArTarget(
                    id = item.optString("id").ifBlank { "marker-$index" },
                    name = item.optString("name").ifBlank { "Geometry Marker ${index + 1}" },
                    assetPath = item.optString("asset"),
                    widthMeters = width.coerceAtLeast(0.05f),
                    category = item.optString("category").ifBlank { "geometry" }
                )
            }.filter { it.assetPath.isNotBlank() }
        }.getOrDefault(defaultGeometryMarkerTargets())

    private fun decodeReferenceBitmap(context: Context, assetPath: String): Bitmap? =
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
            val sampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxDimension = MAX_REFERENCE_IMAGE_DIMENSION_PX
            )
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth / 2 >= maxDimension || sampledHeight / 2 >= maxDimension) {
            sample *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun AugmentedImage.TrackingMethod.toPaperGraphTrackingMethod(): PaperGraphTrackingMethod =
        when (this) {
            AugmentedImage.TrackingMethod.FULL_TRACKING -> PaperGraphTrackingMethod.FullTracking
            AugmentedImage.TrackingMethod.LAST_KNOWN_POSE -> PaperGraphTrackingMethod.LastKnownPose
            AugmentedImage.TrackingMethod.NOT_TRACKING -> PaperGraphTrackingMethod.NotTracking
        }

    private const val MAX_TRACKED_IMAGES = 20
    private const val GEOMETRY_MARKER_CATALOG = "ar_markers/geometry/marker_catalog.json"
    private const val DEFAULT_TARGET_NAME = "AI STEM Paper Graph"
    private const val DEFAULT_TARGET_WIDTH_METERS = 0.21f
    private const val DEFAULT_GEOMETRY_MARKER_WIDTH_METERS = 0.16f
    private const val MAX_REFERENCE_IMAGE_DIMENSION_PX = 512

    private fun defaultGeometryMarkerTargets(): List<MarkerArTarget> =
        listOf(
            MarkerArTarget(
                id = "G01",
                name = "G01 Geometry Foundations",
                assetPath = "ar_markers/geometry/G01.png",
                widthMeters = DEFAULT_GEOMETRY_MARKER_WIDTH_METERS,
                category = "geometry"
            )
        )

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
    val referenceImageCount: Int,
    val targetNames: List<String> = emptyList()
)
