package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.mathexpedition.CheckpointVerificationStatus
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionPackage
import com.indianservers.ai_stem.feature.games.mathexpedition.ExpeditionPrivacyState
import com.indianservers.ai_stem.feature.games.mathexpedition.LocationPermissionState
import com.indianservers.ai_stem.feature.games.mathexpedition.LocationSample
import com.indianservers.ai_stem.feature.games.mathexpedition.MapCoordinate
import com.indianservers.ai_stem.feature.games.mathexpedition.MapProviderCapability
import com.indianservers.ai_stem.feature.games.mathexpedition.MathExpeditionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpeditionEngineTest {
    private val engine = MathExpeditionEngine()

    @Test
    fun exposesEightModesFiveTemplatesAndAttribution() {
        assertEquals(8, engine.modes.size)
        assertEquals(5, engine.sampleTemplates.size)
        assertTrue(engine.mapProviders.all { it.attributionText.isNotBlank() })
        assertTrue(engine.mapProviders.any { MapProviderCapability.ConfigurableTileSource in it.capabilities })
    }

    @Test
    fun geodesicDistanceBearingAndRouteLengthUseGeographicMath() {
        val a = MapCoordinate(12.9716, 77.5946)
        val b = MapCoordinate(12.9726, 77.5946)
        val distance = engine.geodesicDistanceMetres(a, b)
        val route = engine.sampleTemplates.first().route

        assertTrue(distance in 100.0..115.0)
        assertEquals(0.0, engine.bearingDegrees(a, b), 1.0)
        assertTrue(engine.routeLength(route) > engine.directDistance(route))
    }

    @Test
    fun routeValidationDetectsSafeTemplates() {
        val result = engine.validateRoute(engine.sampleTemplates.first().route)

        assertTrue(result.issues.joinToString { it.message }, result.valid)
    }

    @Test
    fun checkpointVerificationRequiresLocationAccuracyDwellBoundaryAndHostAuthorityWhenConfigured() {
        val route = engine.sampleTemplates.first().route
        val checkpoint = route.checkpoints.first()
        val good = LocationSample(checkpoint.coordinate, accuracyMetres = 10.0, speedMetresPerSecond = 0.6, elapsedDwellSeconds = 3, providerAvailable = true, approximateOnly = false, mockFlaggedByAndroid = false)
        val poor = good.copy(accuracyMetres = 100.0)
        val fast = good.copy(speedMetresPerSecond = 8.0)

        assertEquals(CheckpointVerificationStatus.Verified, engine.verifyCheckpoint(route, checkpoint, good, hostAuthoritative = true).status)
        assertEquals(CheckpointVerificationStatus.PoorAccuracy, engine.verifyCheckpoint(route, checkpoint, poor, hostAuthoritative = true).status)
        assertEquals(CheckpointVerificationStatus.MovingTooFast, engine.verifyCheckpoint(route, checkpoint, fast, hostAuthoritative = true).status)
    }

    @Test
    fun locationPermissionMessagesCoverDeniedGpsAndApproximateStates() {
        assertTrue(engine.permissionMessage(LocationPermissionState.Denied).contains("denied", ignoreCase = true))
        assertTrue(engine.permissionMessage(LocationPermissionState.GpsDisabled).contains("disabled", ignoreCase = true))
        assertTrue(engine.permissionMessage(LocationPermissionState.GrantedApproximate).contains("coarse", ignoreCase = true))
    }

    @Test
    fun routeImportExportRejectsExecutableOversizedAndUnsupportedPackages() {
        val exported = engine.exportRoute(engine.sampleTemplates.first())
        val invalid = ExpeditionPackage(version = 99, metadata = "bad", checkpointCount = 2, payload = "<script>alert(1)</script>")
        val oversized = ExpeditionPackage(version = 1, metadata = "big", checkpointCount = 101, payload = "x".repeat(250_001))

        assertTrue(engine.validateImport(exported).valid)
        assertFalse(engine.validateImport(invalid).valid)
        assertFalse(engine.validateImport(oversized).valid)
    }

    @Test
    fun analyticsAndPrivacyDeletionAreLocalAndSummarized() {
        val route = engine.sampleTemplates.first().route
        val analytics = engine.analytics(route, completed = 2, actualMinutes = 25, accuracySamples = listOf(10.0, 20.0), hints = 1, arCount = 1, safetyInterruptions = 2)
        val privacy = ExpeditionPrivacyState(setOf("route"), setOf("map"), setOf("history"), locationCacheEntries = 4)

        assertEquals(2, analytics.checkpointsCompleted)
        assertEquals(15.0, analytics.accuracyAverageMetres, 0.0001)
        assertTrue(engine.deleteHistory(privacy).historyEntryIds.isEmpty())
        assertTrue(engine.deleteDownloadedMaps(privacy).downloadedMapIds.isEmpty())
        assertTrue(engine.deleteSavedRoutes(privacy).savedRouteIds.isEmpty())
    }
}
