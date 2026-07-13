package com.indianservers.ai_stem.core.ar

import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus
import kotlin.math.abs
import kotlin.math.sqrt

data class ArPoseSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNanos: Long
)

data class ArSensorFusionInput(
    val cameraTracking: TrackingStatus,
    val anchorTracking: TrackingStatus,
    val placementHitKind: PlacementHitKind,
    val lightIntensity: Float?,
    val poseSamples: List<ArPoseSample>,
    val nativeSensor: NativeArSensorSample? = null,
    val sceneUnderstanding: ArSceneUnderstandingSample? = null,
    val hasDepthSupport: Boolean,
    val hasPlacedObject: Boolean
)

data class NativeArSensorSample(
    val linearAccelerationMagnitude: Float,
    val gyroscopeMagnitude: Float,
    val pitchDegrees: Float?,
    val rollDegrees: Float?,
    val magneticMagnitude: Float?,
    val ambientLightLux: Float?,
    val sensorAgeMillis: Long
)

data class ArSceneUnderstandingSample(
    val semanticsSupported: Boolean,
    val semanticsEnabled: Boolean,
    val semanticsAvailable: Boolean,
    val depthSupported: Boolean,
    val depthEnabled: Boolean,
    val depthImageAvailable: Boolean = false,
    val centerDepthMeters: Float? = null,
    val skyFraction: Float = 0f,
    val buildingFraction: Float = 0f,
    val treeFraction: Float = 0f,
    val roadFraction: Float = 0f,
    val sidewalkFraction: Float = 0f,
    val terrainFraction: Float = 0f,
    val structureFraction: Float = 0f,
    val waterFraction: Float = 0f,
    val vehicleFraction: Float = 0f,
    val personFraction: Float = 0f,
    val objectFraction: Float = 0f
) {
    val placementSurfaceFraction: Float
        get() = roadFraction + sidewalkFraction + terrainFraction + structureFraction + objectFraction

    val dynamicObstacleFraction: Float
        get() = vehicleFraction + personFraction

    val outdoorContextFraction: Float
        get() = skyFraction + buildingFraction + treeFraction + roadFraction + sidewalkFraction + terrainFraction
}

data class ArSensorFusionResult(
    val score: Int,
    val quality: PlacementQuality,
    val motionStable: Boolean,
    val lightStable: Boolean,
    val shouldDelayPlacement: Boolean,
    val shouldPreferPlane: Boolean,
    val guidance: String,
    val diagnostics: List<String> = emptyList()
)

class ArSensorFusionEngine {
    fun evaluate(input: ArSensorFusionInput): ArSensorFusionResult {
        val motionMetersPerSecond = input.poseSamples.motionMetersPerSecond()
        val native = input.nativeSensor
        val scene = input.sceneUnderstanding
        val nativeFresh = native == null || native.sensorAgeMillis <= MAX_NATIVE_SENSOR_AGE_MS
        val accelerationStable = native == null || native.linearAccelerationMagnitude <= STABLE_ACCELERATION_MPS2
        val rotationStable = native == null || native.gyroscopeMagnitude <= STABLE_GYRO_RAD_PER_SECOND
        val attitudeStable = native?.let { sample ->
            val pitch = sample.pitchDegrees?.let(::abs) ?: 0f
            val roll = sample.rollDegrees?.let(::abs) ?: 0f
            pitch <= MAX_TILTED_SCAN_DEGREES && roll <= MAX_TILTED_SCAN_DEGREES
        } ?: true
        val semanticObstacleClear = scene == null ||
            !scene.semanticsAvailable ||
            scene.dynamicObstacleFraction <= MAX_DYNAMIC_OBSTACLE_FRACTION
        val semanticSurfaceUseful = scene?.let {
            it.semanticsAvailable && it.placementSurfaceFraction >= MIN_USEFUL_SEMANTIC_SURFACE_FRACTION
        } ?: false
        val semanticOutdoorBoost = scene?.let {
            it.semanticsAvailable && it.outdoorContextFraction >= MIN_OUTDOOR_CONTEXT_FRACTION
        } ?: false
        val motionStable = motionMetersPerSecond <= STABLE_MOTION_MPS &&
            nativeFresh &&
            accelerationStable &&
            rotationStable &&
            attitudeStable
        val effectiveLight = native?.ambientLightLux?.let { luxToArCoreLight(it) } ?: input.lightIntensity
        val lightStable = effectiveLight == null || effectiveLight >= MIN_LIGHT_INTENSITY
        val base = when (input.placementHitKind) {
            PlacementHitKind.StreetscapeGeometry -> 68
            PlacementHitKind.Plane -> 62
            PlacementHitKind.DepthPoint -> 58
            PlacementHitKind.FeaturePoint -> 45
            PlacementHitKind.Instant -> 30
            PlacementHitKind.None -> 0
        }
        val trackingBonus = when (input.cameraTracking) {
            TrackingStatus.Tracking -> 18
            TrackingStatus.Limited -> 5
            TrackingStatus.Paused -> -40
            TrackingStatus.Unknown -> -10
        }
        val anchorBonus = when {
            !input.hasPlacedObject -> 0
            input.anchorTracking == TrackingStatus.Tracking -> 8
            input.anchorTracking == TrackingStatus.Limited -> -8
            input.anchorTracking == TrackingStatus.Paused -> -25
            else -> -4
        }
        val nativeSensorBonus = when {
            native == null -> 0
            !nativeFresh -> -10
            accelerationStable && rotationStable && attitudeStable -> 10
            accelerationStable && rotationStable -> 3
            else -> -18
        }
        val sceneBonus = when {
            scene == null -> 0
            scene.semanticsEnabled && !scene.semanticsAvailable -> 1
            !semanticObstacleClear -> -24
            semanticSurfaceUseful -> 10
            semanticOutdoorBoost -> 5
            scene.depthImageAvailable -> 7
            scene.depthEnabled -> 3
            else -> 0
        }
        val motionBonus = if (motionStable) 12 else -20
        val lightBonus = if (lightStable) 8 else -18
        val depthBonus = if (input.hasDepthSupport || scene?.depthEnabled == true) 5 else 0
        val score = (base + trackingBonus + anchorBonus + motionBonus + nativeSensorBonus + sceneBonus + lightBonus + depthBonus).coerceIn(0, 100)
        val quality = when {
            score >= 82 -> PlacementQuality.Excellent
            score >= 64 -> PlacementQuality.Good
            score >= 36 -> PlacementQuality.Weak
            input.hasPlacedObject && input.anchorTracking == TrackingStatus.Limited -> PlacementQuality.Recovering
            input.hasPlacedObject && input.anchorTracking == TrackingStatus.Paused -> PlacementQuality.Lost
            else -> PlacementQuality.Unknown
        }
        val shouldDelay = input.cameraTracking != TrackingStatus.Tracking ||
            input.placementHitKind == PlacementHitKind.None ||
            !motionStable ||
            !semanticObstacleClear ||
            !lightStable
        val guidance = when {
            input.cameraTracking == TrackingStatus.Paused -> "Tracking paused. Point back at the surface and move slowly."
            input.placementHitKind == PlacementHitKind.None -> "Aim at a surface or open space and move slowly."
            !nativeFresh -> "Sensor stream is stale. Pause and reopen AR if tracking does not recover."
            !rotationStable -> "Rotate slower. AR needs gentle motion to lock onto the surface."
            !accelerationStable -> "Move the phone slowly sideways, not in fast swings."
            !attitudeStable -> "Keep the phone less tilted while scanning the surface."
            !semanticObstacleClear -> "Avoid placing through moving people or vehicles."
            motionMetersPerSecond > STABLE_MOTION_MPS -> "Hold steady for a moment before placing."
            !lightStable -> "Add more light for stronger tracking."
            semanticSurfaceUseful -> "AI scene understanding sees a usable surface. Tap to place."
            input.placementHitKind == PlacementHitKind.StreetscapeGeometry -> "Building or terrain mesh locked. Tap to anchor outdoor math."
            input.placementHitKind == PlacementHitKind.DepthPoint -> "Depth surface locked. Tap to place and walk around slowly."
            input.placementHitKind == PlacementHitKind.Instant -> "Air placement ready. Tap to place, then move slowly to refine."
            input.placementHitKind == PlacementHitKind.FeaturePoint -> "Air placement ready from visual features. Tap to place."
            quality == PlacementQuality.Excellent -> "Excellent surface. Tap to place."
            quality == PlacementQuality.Good -> "Good surface. Tap to place."
            else -> "Weak placement. Move slowly or aim at a textured area."
        }
        return ArSensorFusionResult(
            score = score,
            quality = quality,
            motionStable = motionStable,
            lightStable = lightStable,
            shouldDelayPlacement = shouldDelay,
            shouldPreferPlane = input.placementHitKind == PlacementHitKind.Instant || input.placementHitKind == PlacementHitKind.FeaturePoint,
            guidance = guidance,
            diagnostics = buildDiagnostics(input, motionMetersPerSecond, semanticSurfaceUseful, semanticObstacleClear)
        )
    }

    private fun buildDiagnostics(
        input: ArSensorFusionInput,
        motionMetersPerSecond: Float,
        semanticSurfaceUseful: Boolean,
        semanticObstacleClear: Boolean
    ): List<String> {
        val scene = input.sceneUnderstanding
        return buildList {
            add("Native motion: ${"%.2f".format(motionMetersPerSecond)} m/s")
            input.nativeSensor?.let {
                add("Sensors: acc=${"%.2f".format(it.linearAccelerationMagnitude)} gyro=${"%.2f".format(it.gyroscopeMagnitude)} age=${it.sensorAgeMillis}ms")
            }
            if (scene != null) {
                add("Scene AI: semantics=${scene.semanticsEnabled}/${scene.semanticsAvailable} depth=${scene.depthEnabled}/${scene.depthImageAvailable}")
                scene.centerDepthMeters?.let { add("Depth center: ${"%.2f".format(it)} m") }
                add("Scene AI surfaces: ${(scene.placementSurfaceFraction * 100f).toInt()}% obstacles=${(scene.dynamicObstacleFraction * 100f).toInt()}%")
                add("Scene AI decision: surface=$semanticSurfaceUseful obstacleClear=$semanticObstacleClear")
            }
        }
    }

    private fun luxToArCoreLight(lux: Float): Float =
        (lux / GOOD_LIGHT_LUX).coerceIn(0f, 1f)

    private fun List<ArPoseSample>.motionMetersPerSecond(): Float {
        if (size < 2) return 0f
        val first = first()
        val last = last()
        val seconds = ((last.timestampNanos - first.timestampNanos).coerceAtLeast(1L)) / 1_000_000_000f
        val distance = sqrt(
            (last.x - first.x) * (last.x - first.x) +
                (last.y - first.y) * (last.y - first.y) +
                (last.z - first.z) * (last.z - first.z)
        )
        val jitter = zipWithNext().sumOf { (a, b) ->
            abs((b.x - a.x).toDouble()) + abs((b.y - a.y).toDouble()) + abs((b.z - a.z).toDouble())
        }.toFloat()
        return (distance + jitter * JITTER_WEIGHT) / seconds
    }

    private companion object {
        const val STABLE_MOTION_MPS = 0.55f
        const val STABLE_ACCELERATION_MPS2 = 1.85f
        const val STABLE_GYRO_RAD_PER_SECOND = 1.15f
        const val MAX_TILTED_SCAN_DEGREES = 68f
        const val MIN_USEFUL_SEMANTIC_SURFACE_FRACTION = 0.08f
        const val MIN_OUTDOOR_CONTEXT_FRACTION = 0.18f
        const val MAX_DYNAMIC_OBSTACLE_FRACTION = 0.12f
        const val MIN_LIGHT_INTENSITY = 0.18f
        const val GOOD_LIGHT_LUX = 500f
        const val JITTER_WEIGHT = 0.08f
        const val MAX_NATIVE_SENSOR_AGE_MS = 750L
    }
}
