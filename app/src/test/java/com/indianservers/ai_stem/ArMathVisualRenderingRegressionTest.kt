package com.indianservers.ai_stem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArMathVisualRenderingRegressionTest {
    private val screenSource: String by lazy {
        File("src/main/java/com/indianservers/ai_stem/feature/arviewer/ArViewerScreen.kt").readText()
    }

    @Test
    fun markerMathUsesSharedRadiantPointAndThickLineRenderers() {
        assertTrue(screenSource.contains("data class RadiantPointStyle"))
        assertTrue(screenSource.contains("data class ThickLineStyle"))
        assertTrue(screenSource.contains("private fun NodeScope.RadiantPointRenderer"))
        assertTrue(screenSource.contains("private fun NodeScope.ThickLineRenderer"))
    }

    @Test
    fun coordinateLabPointsAndShapesDoNotUseCubeMarkers() {
        val coordinateSection = screenSource.substringAfter("private fun NodeScope.MarkerCoordinateWorkspaceNode")
            .substringBefore("private const val MARKER_COORDINATE_UNIT_UI")

        assertTrue(coordinateSection.contains("RadiantPointRenderer"))
        assertTrue(coordinateSection.contains("ThickLineRenderer"))
        assertFalse(coordinateSection.contains("CubeNode("))
        assertFalse(coordinateSection.contains("LineNode("))
    }

    @Test
    fun twoDimensionalConstructionPointsUseRadiantDots() {
        val constructionSection = screenSource.substringAfter("private fun NodeScope.ConstructionGeometryNodes")
            .substringBefore("private fun NodeScope.VectorHead")

        assertTrue(constructionSection.contains("RadiantPointRenderer"))
        assertTrue(constructionSection.contains("ThickLineRenderer"))
        assertFalse(constructionSection.contains("CubeNode("))
    }

    @Test
    fun graphCurvesAndTracePointsUseRibbonAndRadiantRendering() {
        val graphSection = screenSource.substringAfter("private fun NodeScope.MarkerGraphWorkspaceNode")
            .substringBefore("private fun markerSurfacePoint")

        assertTrue(graphSection.contains("ThickLineRenderer("))
        assertTrue(graphSection.contains("RadiantPointRenderer("))
        assertFalse(graphSection.contains("CubeNode("))
    }

    @Test
    fun threeDimensionalVertexOverlayUsesRadiantSpheresNotCubes() {
        val verticesSection = screenSource.substringAfter("private fun NodeScope.Marker3dVertexNodes")
            .substringBefore("private fun NodeScope.Marker3dNetNode")

        assertTrue(verticesSection.contains("RadiantPointRenderer"))
        assertFalse(verticesSection.contains("CubeNode("))
    }
}
