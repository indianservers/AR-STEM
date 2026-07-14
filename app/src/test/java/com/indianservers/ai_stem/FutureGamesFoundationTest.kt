package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinateGridDefinition
import com.indianservers.ai_stem.feature.games.coordinateconquest.GridScale
import com.indianservers.ai_stem.feature.games.equationescape.EscapeMode
import com.indianservers.ai_stem.feature.games.equationescape.EscapeRoomDefinition
import com.indianservers.ai_stem.feature.games.fractionfactory.IngredientQuantity
import com.indianservers.ai_stem.feature.games.geometryarchitect.DesignBrief
import com.indianservers.ai_stem.feature.games.geometryarchitect.MaterialBudget
import com.indianservers.ai_stem.feature.games.mathexpedition.MapCoordinate
import com.indianservers.ai_stem.feature.games.mathexpedition.OfflineMapPackageMetadata
import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.Rational
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FutureGamesFoundationTest {
    @Test
    fun gameIdsAreStableAndPhaseOneIndoorGamesArePlayable() {
        val ids = GamesCatalog.games.map { it.id }
        assertEquals(listOf("ar-math-arena", "equation_escape_ar", "geometry_architect_ar", "fraction_factory_ar", "coordinate_conquest_ar", "math_expedition_ar"), ids)
        assertTrue(GamesCatalog.games.all { it.playable })
    }

    @Test
    fun futureGamesDeclareSpecificCapabilitiesWithoutStartingRuntimeFeatures() {
        assertTrue(GamesCatalog.requireGame("equation_escape_ar").capabilities.contains(GameCapability.PuzzleSequencing))
        assertTrue(GamesCatalog.requireGame("geometry_architect_ar").capabilities.contains(GameCapability.Measurement))
        assertTrue(GamesCatalog.requireGame("fraction_factory_ar").capabilities.contains(GameCapability.ObjectManipulation))
        assertTrue(GamesCatalog.requireGame("coordinate_conquest_ar").capabilities.contains(GameCapability.SafeMovementArea))
        assertTrue(GamesCatalog.requireGame("math_expedition_ar").capabilities.contains(GameCapability.FutureLocationPermission))
    }

    @Test
    fun gameDomainBoundariesExposeReusableModels() {
        val room = EscapeRoomDefinition("room", "Room", EscapeMode.SinglePlayer, setOf(MathTopic.Algebra), emptyList(), emptyList())
        val brief = DesignBrief("brief", "Bridge", emptyList(), MaterialBudget(10.0, "blocks"))
        val quantity = IngredientQuantity("flour", Rational(1, 2), "cup")
        val grid = CoordinateGridDefinition("grid", Vector3Dto(0f, 0f, 0f), GridScale(0.5, 10))
        val map = OfflineMapPackageMetadata("pkg", "OpenStreetMap-compatible provider", "ODbL-compatible review required", "Campus", 0)

        assertEquals("room", room.roomId)
        assertEquals("Bridge", brief.title)
        assertEquals("cup", quantity.unit)
        assertEquals(10, grid.scale.visibleUnitRadius)
        assertTrue(map.providerName.contains("OpenStreetMap"))
    }

    @Test
    fun manifestDeclaresForegroundLocationForMathExpeditionOnly() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("ACCESS_FINE_LOCATION"))
        assertTrue(manifest.contains("ACCESS_COARSE_LOCATION"))
        assertFalse(manifest.contains("ACCESS_BACKGROUND_LOCATION"))
    }
}
