package com.indianservers.ai_stem.domain.geometry3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class SolidGeometryEngineTest {
    @Test
    fun everySolidProducesItsOwnUsableMesh() {
        val signatures = SolidType.entries.map { type ->
            val mesh = SolidGeometryEngine.mesh(SolidObject(type.name, type))
            assertTrue("$type has vertices", mesh.vertices.isNotEmpty())
            assertTrue("$type has edges", mesh.edges.isNotEmpty())
            type to (mesh.vertices.size to mesh.edges.size)
        }.toMap()

        assertNotEquals(signatures[SolidType.Cube], signatures[SolidType.Sphere])
        assertNotEquals(signatures[SolidType.Cylinder], signatures[SolidType.Torus])
        assertNotEquals(signatures[SolidType.Pyramid], signatures[SolidType.Tetrahedron])
    }

    @Test
    fun cylinderVolumeUsesCurrentParameters() {
        val solid = SolidObject("cylinder", SolidType.Cylinder, SolidParameters(radius = 2.0, height = 3.0))
        val volume = SolidGeometryEngine.measurements(solid).first { it.label == "Volume" }
        assertEquals(12.0 * PI, volume.value, 1e-9)
    }
}
