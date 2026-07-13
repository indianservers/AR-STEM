package com.indianservers.ai_stem

import com.indianservers.ai_stem.core.ar.ArPoseSample
import com.indianservers.ai_stem.core.ar.ArSceneUnderstandingSample
import com.indianservers.ai_stem.core.ar.ArSensorFusionEngine
import com.indianservers.ai_stem.core.ar.ArSensorFusionInput
import com.indianservers.ai_stem.core.ar.NativeArSensorSample
import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArSensorFusionEngineTest {
    private val engine = ArSensorFusionEngine()

    @Test
    fun stablePlaneWithLightScoresExcellent() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                lightIntensity = 0.8f,
                poseSamples = stableSamples(),
                hasDepthSupport = true,
                hasPlacedObject = false
            )
        )

        assertEquals(PlacementQuality.Excellent, result.quality)
        assertFalse(result.shouldDelayPlacement)
        assertTrue(result.motionStable)
        assertTrue(result.lightStable)
    }

    @Test
    fun fastMotionDelaysInstantPlacement() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Instant,
                lightIntensity = 0.9f,
                poseSamples = movingSamples(),
                hasDepthSupport = false,
                hasPlacedObject = false
            )
        )

        assertTrue(result.shouldDelayPlacement)
        assertFalse(result.motionStable)
        assertTrue(result.shouldPreferPlane)
    }

    @Test
    fun lowLightWeakensFeaturePointPlacement() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.FeaturePoint,
                lightIntensity = 0.05f,
                poseSamples = stableSamples(),
                hasDepthSupport = false,
                hasPlacedObject = false
            )
        )

        assertFalse(result.lightStable)
        assertTrue(result.score < 64)
    }

    @Test
    fun fastGyroscopeDelaysEvenWhenCameraPoseLooksStable() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                lightIntensity = 0.9f,
                poseSamples = stableSamples(),
                nativeSensor = NativeArSensorSample(
                    linearAccelerationMagnitude = 0.2f,
                    gyroscopeMagnitude = 2.4f,
                    pitchDegrees = 12f,
                    rollDegrees = 4f,
                    magneticMagnitude = 40f,
                    ambientLightLux = 600f,
                    sensorAgeMillis = 0L
                ),
                hasDepthSupport = false,
                hasPlacedObject = false
            )
        )

        assertFalse(result.motionStable)
        assertTrue(result.shouldDelayPlacement)
    }

    @Test
    fun nativeLuxCanRescueMissingArcoreLightEstimate() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                lightIntensity = null,
                poseSamples = stableSamples(),
                nativeSensor = NativeArSensorSample(
                    linearAccelerationMagnitude = 0.1f,
                    gyroscopeMagnitude = 0.1f,
                    pitchDegrees = 8f,
                    rollDegrees = 6f,
                    magneticMagnitude = 38f,
                    ambientLightLux = 700f,
                    sensorAgeMillis = 0L
                ),
                hasDepthSupport = false,
                hasPlacedObject = false
            )
        )

        assertTrue(result.lightStable)
        assertTrue(result.score >= 82)
    }

    @Test
    fun semanticSurfaceBoostsPlacementConfidence() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                lightIntensity = 0.7f,
                poseSamples = stableSamples(),
                sceneUnderstanding = ArSceneUnderstandingSample(
                    semanticsSupported = true,
                    semanticsEnabled = true,
                    semanticsAvailable = true,
                    depthSupported = true,
                    depthEnabled = true,
                    sidewalkFraction = 0.14f,
                    terrainFraction = 0.05f,
                    buildingFraction = 0.2f
                ),
                hasDepthSupport = true,
                hasPlacedObject = false
            )
        )

        assertEquals(PlacementQuality.Excellent, result.quality)
        assertTrue(result.diagnostics.any { it.contains("Scene AI") })
        assertFalse(result.shouldDelayPlacement)
    }

    @Test
    fun semanticDynamicObstaclesLowerPlacementConfidence() {
        val result = engine.evaluate(
            ArSensorFusionInput(
                cameraTracking = TrackingStatus.Tracking,
                anchorTracking = TrackingStatus.Unknown,
                placementHitKind = PlacementHitKind.Plane,
                lightIntensity = 0.8f,
                poseSamples = stableSamples(),
                sceneUnderstanding = ArSceneUnderstandingSample(
                    semanticsSupported = true,
                    semanticsEnabled = true,
                    semanticsAvailable = true,
                    depthSupported = true,
                    depthEnabled = true,
                    sidewalkFraction = 0.2f,
                    personFraction = 0.16f
                ),
                hasDepthSupport = true,
                hasPlacedObject = false
            )
        )

        assertTrue(result.score < 90)
        assertTrue(result.guidance.contains("moving people"))
    }

    private fun stableSamples(): List<ArPoseSample> = listOf(
        ArPoseSample(0f, 0f, 0f, 0L),
        ArPoseSample(0.01f, 0f, 0.01f, 1_000_000_000L)
    )

    private fun movingSamples(): List<ArPoseSample> = listOf(
        ArPoseSample(0f, 0f, 0f, 0L),
        ArPoseSample(1.2f, 0f, 0.3f, 1_000_000_000L)
    )
}
