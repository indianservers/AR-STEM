package com.indianservers.ai_stem

import com.indianservers.ai_stem.data.scene.migrateSceneSchema
import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.mathematics.MeasurementUnit
import com.indianservers.ai_stem.domain.scene.CURRENT_SCENE_SCHEMA_VERSION
import com.indianservers.ai_stem.domain.scene.DefaultObjectTransformService
import com.indianservers.ai_stem.domain.scene.MathScene
import com.indianservers.ai_stem.domain.scene.SceneHistory
import com.indianservers.ai_stem.domain.scene.SceneMutations
import com.indianservers.ai_stem.domain.scene.SnapshotSceneCommand
import com.indianservers.ai_stem.domain.scene.TransformScaling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PhaseTwoDomainTest {
    @Test
    fun registryContainsAllPhaseTwoObjects() {
        val types = DefaultMathObjectRegistry.getAllDefinitions().map { it.type }.toSet()
        assertEquals(
            setOf(
                MathObjectType.CoordinatePlane,
                MathObjectType.Cube,
                MathObjectType.SineCurve,
                MathObjectType.Sphere,
                MathObjectType.Cylinder,
                MathObjectType.Cone,
                MathObjectType.RectangularPrism,
                MathObjectType.Triangle,
                MathObjectType.Circle,
                MathObjectType.NumberLine,
                MathObjectType.VectorArrow
            ),
            types
        )
    }

    @Test
    fun formulasProduceExpectedValues() {
        val cube = requireNotNull(DefaultMathObjectRegistry.getDefinition("cube"))
        val cubeMeasurements = cube.measurementProvider(mapOf("sideLength" to MathParameterValue.NumberValue(2.0)))
        assertNearly(24.0, cubeMeasurements.first { it.id == "surfaceArea" }.value)
        assertNearly(8.0, cubeMeasurements.first { it.id == "volume" }.value)

        val sphere = requireNotNull(DefaultMathObjectRegistry.getDefinition("sphere"))
        val sphereMeasurements = sphere.measurementProvider(mapOf("radius" to MathParameterValue.NumberValue(1.0)))
        assertNearly(4.0 * PI, sphereMeasurements.first { it.id == "surfaceArea" }.value)

        val vector = requireNotNull(DefaultMathObjectRegistry.getDefinition("vector-arrow"))
        val vectorMeasurements = vector.measurementProvider(
            mapOf("x" to MathParameterValue.NumberValue(3.0), "y" to MathParameterValue.NumberValue(4.0), "z" to MathParameterValue.NumberValue(0.0))
        )
        assertNearly(5.0, vectorMeasurements.first { it.id == "magnitude" }.value)
    }

    @Test
    fun sceneSupportsDuplicateLockHideGroupAndUngroup() {
        val scene = MathScene()
        val withCube = SceneMutations.addObject(scene, "cube")
        val withSphere = SceneMutations.addObject(withCube, "sphere")
        assertEquals(2, withSphere.objects.size)

        val multiSelected = withSphere.copy(objects = withSphere.objects.map { it.copy(interactionState = it.interactionState.copy(selected = true)) })
        val grouped = SceneMutations.groupSelected(multiSelected)
        assertEquals(1, grouped.groups.size)
        assertTrue(grouped.objects.all { it.groupId == grouped.groups.first().id })

        val ungrouped = SceneMutations.ungroupSelected(grouped)
        assertTrue(ungrouped.groups.isEmpty())
        assertTrue(ungrouped.objects.all { it.groupId == null })

        val selectedId = ungrouped.primarySelectedObject?.id ?: ungrouped.objects.first().id
        val locked = SceneMutations.setLocked(ungrouped, selectedId, true)
        assertTrue(locked.objects.first { it.id == selectedId }.interactionState.locked)
        val hidden = SceneMutations.setVisible(locked, selectedId, false)
        assertFalse(hidden.objects.first { it.id == selectedId }.visibility.visible)

        val duplicated = SceneMutations.duplicateSelected(ungrouped)
        assertEquals(4, duplicated.objects.size)
        assertNotEquals(duplicated.objects[0].id, duplicated.objects[2].id)
    }

    @Test
    fun transformServiceRejectsLockedObjectScale() {
        val scene = SceneMutations.addObject(MathScene(), "cube")
        val objectId = requireNotNull(scene.primarySelectedObject).id
        val locked = SceneMutations.setLocked(scene, objectId, true)
        val result = DefaultObjectTransformService.scale(locked, objectId, TransformScaling(2.0))
        assertTrue(result is com.indianservers.ai_stem.domain.scene.SceneMutationResult.Failure)
    }

    @Test
    fun historyUndoRedoRestoresSnapshots() {
        val before = MathScene()
        val after = SceneMutations.addObject(before, "cube")
        val history = SceneHistory().record(SnapshotSceneCommand(description = "Place object", before = before, after = after))
        val (undone, undoHistory) = history.undo(after)
        assertTrue(undone.objects.isEmpty())
        val (redone, redoHistory) = undoHistory.redo(undone)
        assertEquals(1, redone.objects.size)
        assertNotNull(redoHistory.lastMessage)
    }

    @Test
    fun schemaMigrationRecognizesSupportedVersions() {
        assertTrue(migrateSceneSchema(1))
        assertTrue(migrateSceneSchema(CURRENT_SCENE_SCHEMA_VERSION))
        assertFalse(migrateSceneSchema(CURRENT_SCENE_SCHEMA_VERSION + 1))
    }

    @Test
    fun measurementUnitsConvertFromMeters() {
        assertNearly(0.01, MeasurementUnit.Centimetres.meters)
        assertNearly(0.0254, MeasurementUnit.Inches.meters)
    }

    private fun assertNearly(expected: Double, actual: Double) {
        assertEquals(expected, actual, 0.0001)
    }
}
