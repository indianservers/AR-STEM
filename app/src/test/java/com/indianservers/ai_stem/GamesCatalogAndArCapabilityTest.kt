package com.indianservers.ai_stem

import com.google.ar.core.ArCoreApk
import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.api.GameDestination
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
        assertEquals(
            listOf(
                "ar-math-arena",
                "equation_escape_ar",
                "geometry_architect_ar",
                "fraction_factory_ar",
                "coordinate_conquest_ar",
                "math_expedition_ar"
            ),
            GamesCatalog.games.map { it.id }
        )
        assertEquals("Math Fortress AR", GamesCatalog.games.first().title)
        assertEquals("Solve. Build. Defend Together.", GamesCatalog.games.first().tagline)
        assertEquals(GameAvailability.Available, GamesCatalog.games.first().availability)
        assertEquals(GameAvailability.Available, GamesCatalog.requireGame("equation_escape_ar").availability)
        assertEquals(GameAvailability.Available, GamesCatalog.requireGame("geometry_architect_ar").availability)
        assertEquals(GameAvailability.Available, GamesCatalog.requireGame("fraction_factory_ar").availability)
        assertEquals(GameAvailability.Available, GamesCatalog.requireGame("coordinate_conquest_ar").availability)
        assertEquals(GameAvailability.Available, GamesCatalog.requireGame("math_expedition_ar").availability)
        assertTrue(GamesCatalog.games.first().playable)
        assertTrue(GamesCatalog.games.all { it.playable })
        assertEquals(GameDestination.ArMathArena.route, GamesCatalog.games.first().destination.route)
        assertEquals(GameDestination.EquationEscape.route, GamesCatalog.requireGame("equation_escape_ar").destination.route)
        assertEquals(GameDestination.GeometryArchitect.route, GamesCatalog.requireGame("geometry_architect_ar").destination.route)
        assertEquals(GameDestination.FractionFactory.route, GamesCatalog.requireGame("fraction_factory_ar").destination.route)
        assertEquals(GameDestination.CoordinateConquest.route, GamesCatalog.requireGame("coordinate_conquest_ar").destination.route)
        assertEquals(GameDestination.MathExpedition.route, GamesCatalog.requireGame("math_expedition_ar").destination.route)
    }

    @Test
    fun newGamesExposeBadgesTopicsAndSafeComingSoonDestinations() {
        val expedition = GamesCatalog.requireGame("math_expedition_ar")

        assertTrue(expedition.outdoorRequired)
        assertTrue(expedition.openMapRequired)
        assertTrue(expedition.capabilities.contains(GameCapability.Outdoor))
        assertTrue(expedition.capabilities.contains(GameCapability.OpenMap))
        assertEquals(GameDestination.MathExpedition.route, expedition.destination.route)
        assertTrue(expedition.availabilityMessage.contains("Available", ignoreCase = true))
        assertTrue(expedition.howToPlay.tutorialAvailability.contains("Playable", ignoreCase = true))
        assertTrue(GamesCatalog.games.all { it.supportedTopics.isNotEmpty() })
    }

    @Test
    fun allHowToPlayPagesHaveCompleteOfflineGameSpecificContent() {
        GamesCatalog.games.forEach { game ->
            val howTo = game.howToPlay
            assertTrue(howTo.overview.isNotBlank())
            assertTrue(howTo.estimatedReadingMinutes in 2..6)
            assertEquals(5, howTo.quickStartSteps.size)
            assertTrue(howTo.learningObjectives.isNotEmpty())
            assertTrue(howTo.playerModes.isNotEmpty())
            assertTrue(howTo.setupSteps.isNotEmpty())
            assertTrue(howTo.playSteps.isNotEmpty())
            assertTrue(howTo.controls.isNotEmpty())
            assertTrue(howTo.roles.isNotEmpty())
            assertTrue(howTo.scoring.isNotEmpty())
            assertTrue(howTo.winCondition.isNotBlank())
            assertTrue(howTo.safetyNotes.isNotEmpty())
            assertTrue(howTo.deviceRequirements.isNotEmpty())
            assertTrue(howTo.accessibilityNotes.isNotEmpty())
            assertTrue(howTo.tutorialAvailability.isNotBlank())
            assertFalse(howTo.overview.contains("http", ignoreCase = true))
        }
        assertTrue(GamesCatalog.requireGame("ar-math-arena").howToPlay.playSteps.any { it.contains("boss", ignoreCase = true) })
        assertTrue(GamesCatalog.requireGame("coordinate_conquest_ar").howToPlay.safetyNotes.any { it.contains("safe boundary", ignoreCase = true) })
        assertTrue(GamesCatalog.requireGame("math_expedition_ar").howToPlay.deviceRequirements.any { it.contains("OpenStreetMap", ignoreCase = true) })
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
