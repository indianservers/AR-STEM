package com.indianservers.ai_stem

import com.google.ar.core.ArCoreApk
import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.arcore.ArAvailabilityResult
import com.indianservers.ai_stem.feature.games.arcore.ArGameCapabilityRole
import com.indianservers.ai_stem.feature.games.arcore.ArGameFeature
import com.indianservers.ai_stem.feature.games.arcore.ArMathArenaCapabilityPolicy
import com.indianservers.ai_stem.feature.games.arcore.mapArCoreAvailability
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesCatalogAndArCapabilityTest {
    @Test
    fun arMathArenaIsFirstPlayableGameAndCatalogScales() {
        assertEquals("ar-math-arena", GamesCatalog.games.first().id)
        assertEquals(GameAvailability.Available, GamesCatalog.games.first().availability)
        assertTrue(GamesCatalog.games.size >= 6)
        assertTrue(GamesCatalog.games.drop(1).all { it.availability == GameAvailability.ComingSoon })
    }

    @Test
    fun mapsArCoreAvailabilityStates() {
        assertEquals(
            ArAvailabilityResult.SupportedReady,
            mapArCoreAvailability(ArCoreApk.Availability.SUPPORTED_INSTALLED, cameraGranted = true)
        )
        assertEquals(
            ArAvailabilityResult.CameraPermissionRequired,
            mapArCoreAvailability(ArCoreApk.Availability.SUPPORTED_INSTALLED, cameraGranted = false)
        )
        assertEquals(
            ArAvailabilityResult.SupportedInstallRequired,
            mapArCoreAvailability(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED, cameraGranted = true)
        )
        assertEquals(
            ArAvailabilityResult.SupportedUpdateRequired,
            mapArCoreAvailability(ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, cameraGranted = true)
        )
        assertEquals(
            ArAvailabilityResult.Unsupported,
            mapArCoreAvailability(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE, cameraGranted = true)
        )
    }

    @Test
    fun baseGameplayRequiresOnlyLocalArCapabilities() {
        val ready = ArMathArenaCapabilityPolicy.matrix(
            availability = ArAvailabilityResult.SupportedReady,
            depthSupported = false,
            rawDepthSupported = false,
            augmentedImagesSupported = true
        )

        assertTrue(ready.baseGameplayReady)
        assertFalse(ready.features.first { it.feature == ArGameFeature.CloudAnchors }.available)
        assertEquals(ArGameCapabilityRole.CloudDependentFeature, ready.features.first { it.feature == ArGameFeature.CloudAnchors }.role)
        assertEquals(ArGameCapabilityRole.FutureOutdoorFeature, ready.features.first { it.feature == ArGameFeature.Geospatial }.role)
    }

    @Test
    fun unsupportedDeviceCanStillRenderLibraryButNotBaseGameplay() {
        val unsupported = ArMathArenaCapabilityPolicy.matrix(ArAvailabilityResult.Unsupported)

        assertFalse(unsupported.baseGameplayReady)
        assertTrue(unsupported.features.filter { it.role == ArGameCapabilityRole.RequiredForBaseGameplay }.all { !it.available })
    }
}
