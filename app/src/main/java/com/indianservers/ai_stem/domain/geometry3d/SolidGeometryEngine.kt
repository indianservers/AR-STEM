package com.indianservers.ai_stem.domain.geometry3d

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SolidVector(val x: Double, val y: Double, val z: Double)
data class SolidEdge(val from: Int, val to: Int)
data class SolidFace(val indices: List<Int>)

enum class SolidType(val displayName: String) {
    Cube("Cube"), Cuboid("Cuboid"), Sphere("Sphere"), Cylinder("Cylinder"), Cone("Cone"),
    Pyramid("Pyramid"), TriangularPrism("Triangular prism"), Tetrahedron("Tetrahedron"),
    Torus("Torus"), Frustum("Frustum")
}

data class SolidParameters(
    val length: Double = 2.0,
    val width: Double = 2.0,
    val height: Double = 2.0,
    val radius: Double = 1.0,
    val topRadius: Double = 0.55
)

data class SolidTransform(
    val position: SolidVector = SolidVector(0.0, 0.0, 0.0),
    val rotation: SolidVector = SolidVector(-18.0, 28.0, 0.0),
    val scale: Double = 1.0
)

data class SolidObject(
    val id: String,
    val type: SolidType,
    val parameters: SolidParameters = SolidParameters(),
    val transform: SolidTransform = SolidTransform(),
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class SolidMesh(val vertices: List<SolidVector>, val edges: List<SolidEdge>, val faces: List<SolidFace>)
data class SolidMeasurement(val label: String, val value: Double, val unit: String, val formula: String)

object SolidGeometryEngine {
    fun mesh(objectValue: SolidObject, resolution: Int = 18): SolidMesh = when (objectValue.type) {
        SolidType.Cube -> box(objectValue.parameters.length, objectValue.parameters.length, objectValue.parameters.length)
        SolidType.Cuboid -> box(objectValue.parameters.length, objectValue.parameters.height, objectValue.parameters.width)
        SolidType.Sphere -> sphere(objectValue.parameters.radius, resolution)
        SolidType.Cylinder -> ringSolid(objectValue.parameters.radius, objectValue.parameters.radius, objectValue.parameters.height, resolution)
        SolidType.Cone -> ringSolid(objectValue.parameters.radius, 0.0, objectValue.parameters.height, resolution)
        SolidType.Pyramid -> pyramid(objectValue.parameters.length, objectValue.parameters.width, objectValue.parameters.height)
        SolidType.TriangularPrism -> triangularPrism(objectValue.parameters.length, objectValue.parameters.height, objectValue.parameters.width)
        SolidType.Tetrahedron -> tetrahedron(objectValue.parameters.length)
        SolidType.Torus -> torus(objectValue.parameters.radius, objectValue.parameters.width.coerceAtMost(objectValue.parameters.radius * 0.7), resolution)
        SolidType.Frustum -> ringSolid(objectValue.parameters.radius, objectValue.parameters.topRadius, objectValue.parameters.height, resolution)
    }

    fun measurements(objectValue: SolidObject): List<SolidMeasurement> {
        val p = objectValue.parameters
        return when (objectValue.type) {
            SolidType.Cube -> listOf(
                SolidMeasurement("Surface area", 6 * p.length * p.length, "u²", "6a²"),
                SolidMeasurement("Volume", p.length.pow(3), "u³", "a³"),
                SolidMeasurement("Space diagonal", p.length * sqrt(3.0), "u", "a√3")
            )
            SolidType.Cuboid -> listOf(
                SolidMeasurement("Surface area", 2 * (p.length * p.width + p.length * p.height + p.width * p.height), "u²", "2(lw+lh+wh)"),
                SolidMeasurement("Volume", p.length * p.width * p.height, "u³", "lwh")
            )
            SolidType.Sphere -> listOf(
                SolidMeasurement("Surface area", 4 * PI * p.radius * p.radius, "u²", "4πr²"),
                SolidMeasurement("Volume", 4.0 / 3.0 * PI * p.radius.pow(3), "u³", "4πr³/3")
            )
            SolidType.Cylinder -> listOf(
                SolidMeasurement("Surface area", 2 * PI * p.radius * (p.radius + p.height), "u²", "2πr(r+h)"),
                SolidMeasurement("Volume", PI * p.radius * p.radius * p.height, "u³", "πr²h")
            )
            SolidType.Cone -> {
                val slant = sqrt(p.radius * p.radius + p.height * p.height)
                listOf(
                    SolidMeasurement("Slant height", slant, "u", "√(r²+h²)"),
                    SolidMeasurement("Surface area", PI * p.radius * (p.radius + slant), "u²", "πr(r+l)"),
                    SolidMeasurement("Volume", PI * p.radius * p.radius * p.height / 3.0, "u³", "πr²h/3")
                )
            }
            SolidType.Pyramid -> listOf(SolidMeasurement("Volume", p.length * p.width * p.height / 3.0, "u³", "lwh/3"))
            SolidType.TriangularPrism -> listOf(SolidMeasurement("Volume", p.length * p.height * p.width / 2.0, "u³", "base area × length"))
            SolidType.Tetrahedron -> listOf(
                SolidMeasurement("Surface area", sqrt(3.0) * p.length * p.length, "u²", "√3a²"),
                SolidMeasurement("Volume", p.length.pow(3) / (6 * sqrt(2.0)), "u³", "a³/(6√2)")
            )
            SolidType.Torus -> {
                val tube = p.width.coerceAtMost(p.radius * 0.7)
                listOf(
                    SolidMeasurement("Surface area", 4 * PI * PI * p.radius * tube, "u²", "4π²Rr"),
                    SolidMeasurement("Volume", 2 * PI * PI * p.radius * tube * tube, "u³", "2π²Rr²")
                )
            }
            SolidType.Frustum -> listOf(
                SolidMeasurement("Volume", PI * p.height * (p.radius * p.radius + p.radius * p.topRadius + p.topRadius * p.topRadius) / 3.0, "u³", "πh(R²+Rr+r²)/3")
            )
        }
    }

    private fun box(length: Double, height: Double, width: Double): SolidMesh {
        val x = length / 2; val y = height / 2; val z = width / 2
        val vertices = listOf(
            SolidVector(-x, -y, -z), SolidVector(x, -y, -z), SolidVector(x, y, -z), SolidVector(-x, y, -z),
            SolidVector(-x, -y, z), SolidVector(x, -y, z), SolidVector(x, y, z), SolidVector(-x, y, z)
        )
        val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).map { SolidEdge(it.first, it.second) }
        val faces = listOf(listOf(0,1,2,3), listOf(4,5,6,7), listOf(0,1,5,4), listOf(2,3,7,6), listOf(1,2,6,5), listOf(0,3,7,4)).map(::SolidFace)
        return SolidMesh(vertices, edges, faces)
    }

    private fun ringSolid(bottomRadius: Double, topRadius: Double, height: Double, resolution: Int): SolidMesh {
        val segments = resolution.coerceIn(8, 40)
        val vertices = mutableListOf<SolidVector>()
        repeat(segments) { index ->
            val angle = 2 * PI * index / segments
            vertices += SolidVector(bottomRadius * cos(angle), -height / 2, bottomRadius * sin(angle))
            vertices += SolidVector(topRadius * cos(angle), height / 2, topRadius * sin(angle))
        }
        val edges = mutableListOf<SolidEdge>()
        repeat(segments) { index ->
            val next = (index + 1) % segments
            edges += SolidEdge(index * 2, next * 2)
            if (topRadius > 0.0) edges += SolidEdge(index * 2 + 1, next * 2 + 1)
            edges += SolidEdge(index * 2, index * 2 + 1)
        }
        val faces = (0 until segments).map { i -> SolidFace(listOf(i * 2, ((i + 1) % segments) * 2, ((i + 1) % segments) * 2 + 1, i * 2 + 1)) }
        return SolidMesh(vertices, edges, faces)
    }

    private fun sphere(radius: Double, resolution: Int): SolidMesh {
        val lon = resolution.coerceIn(10, 30); val lat = (lon / 2).coerceAtLeast(6)
        val vertices = mutableListOf<SolidVector>()
        for (j in 0..lat) {
            val phi = PI * j / lat
            for (i in 0 until lon) {
                val theta = 2 * PI * i / lon
                vertices += SolidVector(radius * sin(phi) * cos(theta), radius * cos(phi), radius * sin(phi) * sin(theta))
            }
        }
        val edges = mutableListOf<SolidEdge>()
        for (j in 0..lat) for (i in 0 until lon) {
            edges += SolidEdge(j * lon + i, j * lon + (i + 1) % lon)
            if (j < lat) edges += SolidEdge(j * lon + i, (j + 1) * lon + i)
        }
        return SolidMesh(vertices, edges, emptyList())
    }

    private fun pyramid(length: Double, width: Double, height: Double): SolidMesh {
        val vertices = listOf(SolidVector(-length/2, -height/2, -width/2), SolidVector(length/2, -height/2, -width/2), SolidVector(length/2, -height/2, width/2), SolidVector(-length/2, -height/2, width/2), SolidVector(0.0, height/2, 0.0))
        val edges = listOf(0 to 1,1 to 2,2 to 3,3 to 0,0 to 4,1 to 4,2 to 4,3 to 4).map { SolidEdge(it.first,it.second) }
        return SolidMesh(vertices, edges, listOf(SolidFace(listOf(0,1,2,3)), SolidFace(listOf(0,1,4)), SolidFace(listOf(1,2,4)), SolidFace(listOf(2,3,4)), SolidFace(listOf(3,0,4))))
    }

    private fun triangularPrism(length: Double, height: Double, depth: Double): SolidMesh {
        val vertices = listOf(SolidVector(-length/2,-height/2,-depth/2), SolidVector(length/2,-height/2,-depth/2), SolidVector(0.0,height/2,-depth/2), SolidVector(-length/2,-height/2,depth/2), SolidVector(length/2,-height/2,depth/2), SolidVector(0.0,height/2,depth/2))
        val edges = listOf(0 to 1,1 to 2,2 to 0,3 to 4,4 to 5,5 to 3,0 to 3,1 to 4,2 to 5).map { SolidEdge(it.first,it.second) }
        return SolidMesh(vertices, edges, emptyList())
    }

    private fun tetrahedron(side: Double): SolidMesh {
        val h = sqrt(2.0 / 3.0) * side
        val vertices = listOf(SolidVector(-side/2,-h/3,-side/(2*sqrt(3.0))), SolidVector(side/2,-h/3,-side/(2*sqrt(3.0))), SolidVector(0.0,-h/3,side/sqrt(3.0)), SolidVector(0.0,2*h/3,0.0))
        val edges = listOf(0 to 1,1 to 2,2 to 0,0 to 3,1 to 3,2 to 3).map { SolidEdge(it.first,it.second) }
        return SolidMesh(vertices, edges, listOf(SolidFace(listOf(0,1,2)),SolidFace(listOf(0,1,3)),SolidFace(listOf(1,2,3)),SolidFace(listOf(2,0,3))))
    }

    private fun torus(major: Double, minorRaw: Double, resolution: Int): SolidMesh {
        val minor = minorRaw.coerceAtLeast(0.12); val majorSegments = resolution.coerceIn(12, 32); val tubeSegments = (majorSegments / 2).coerceAtLeast(8)
        val vertices = mutableListOf<SolidVector>()
        repeat(majorSegments) { i -> repeat(tubeSegments) { j ->
            val u = 2*PI*i/majorSegments; val v = 2*PI*j/tubeSegments
            vertices += SolidVector((major + minor*cos(v))*cos(u), minor*sin(v), (major + minor*cos(v))*sin(u))
        } }
        val edges = mutableListOf<SolidEdge>()
        repeat(majorSegments) { i -> repeat(tubeSegments) { j ->
            val index = i*tubeSegments+j
            edges += SolidEdge(index, i*tubeSegments+(j+1)%tubeSegments)
            edges += SolidEdge(index, ((i+1)%majorSegments)*tubeSegments+j)
        } }
        return SolidMesh(vertices, edges, emptyList())
    }
}
