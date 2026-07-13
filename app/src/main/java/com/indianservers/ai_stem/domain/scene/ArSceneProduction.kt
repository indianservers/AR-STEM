package com.indianservers.ai_stem.domain.scene

enum class PersistentAnchorKind { SceneOrigin, Plane, PaperImage, Geospatial, StreetscapeGeometry }
enum class ArDepthOcclusionMode { Off, SoftDepth, DepthTest, GeospatialDepth }
enum class ArPerformanceProfile { BatterySaver, Balanced, HighQuality, Presentation }

data class PersistentAnchorRecord(
    val kind: PersistentAnchorKind = PersistentAnchorKind.SceneOrigin,
    val engineMode: String = "Indoor",
    val label: String = "Scene origin",
    val worldPosition: Vector3Value = Vector3Value(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val imageTargetName: String? = null,
    val accuracyMeters: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val restoreHint: String
        get() = when (kind) {
            PersistentAnchorKind.SceneOrigin -> "Place near the original surface origin."
            PersistentAnchorKind.Plane -> "Scan the same table, floor, or wall surface."
            PersistentAnchorKind.PaperImage -> "Scan the same worksheet/image target."
            PersistentAnchorKind.Geospatial -> "Return near the saved outdoor geospatial location."
            PersistentAnchorKind.StreetscapeGeometry -> "Scan the same building or terrain mesh."
        }
}

data class ArSceneProductionSettings(
    val depthMode: ArDepthOcclusionMode = ArDepthOcclusionMode.Off,
    val performanceProfile: ArPerformanceProfile = ArPerformanceProfile.Balanced,
    val meshDensity: Float = 0.65f,
    val maxSceneObjects: Int = 32,
    val exportVersion: Int = 1,
    val screenshotReady: Boolean = false
)

data class ArSceneTemplate(
    val id: String,
    val title: String,
    val description: String,
    val engineMode: String,
    val equation: String,
    val comparisonEquation: String = "y = 0",
    val depthMode: ArDepthOcclusionMode = ArDepthOcclusionMode.SoftDepth,
    val performanceProfile: ArPerformanceProfile = ArPerformanceProfile.Balanced
)

data class ArSceneSharePackage(
    val fileName: String,
    val mimeType: String,
    val payload: String,
    val summary: String
)

object ArSceneTemplates {
    val templates: List<ArSceneTemplate> = listOf(
        ArSceneTemplate(
            id = "paper-graph-lab",
            title = "Paper Graph Lab",
            description = "Worksheet-locked axes, calibrated graph, surface and cross-section.",
            engineMode = "PaperGraph",
            equation = "z = sin(x) * cos(y)",
            depthMode = ArDepthOcclusionMode.SoftDepth,
            performanceProfile = ArPerformanceProfile.Balanced
        ),
        ArSceneTemplate(
            id = "surface-explorer",
            title = "Surface Explorer",
            description = "Markerless 3D surface with color map and tangent tools.",
            engineMode = "Indoor",
            equation = "z = x^2 + y^2",
            comparisonEquation = "y = 1",
            depthMode = ArDepthOcclusionMode.DepthTest,
            performanceProfile = ArPerformanceProfile.HighQuality
        ),
        ArSceneTemplate(
            id = "volume-builder",
            title = "Volume Builder",
            description = "Area sweep and solid-of-revolution volume model.",
            engineMode = "Indoor",
            equation = "y = sqrt(abs(x))",
            depthMode = ArDepthOcclusionMode.SoftDepth,
            performanceProfile = ArPerformanceProfile.Balanced
        ),
        ArSceneTemplate(
            id = "coordinate-grid-lab",
            title = "Coordinate Grid Lab",
            description = "Snap-ready grid with ruler anchors and vector constraints.",
            engineMode = "SurfacePlacement",
            equation = "y = x",
            depthMode = ArDepthOcclusionMode.SoftDepth,
            performanceProfile = ArPerformanceProfile.BatterySaver
        ),
        ArSceneTemplate(
            id = "outdoor-building-geometry",
            title = "Outdoor Building Geometry",
            description = "Geospatial building mesh, height, slope and volume approximation.",
            engineMode = "OutdoorGeospatialMath",
            equation = "y = x",
            depthMode = ArDepthOcclusionMode.GeospatialDepth,
            performanceProfile = ArPerformanceProfile.Balanced
        ),
        ArSceneTemplate(
            id = "vector-field-lab",
            title = "Vector Field Lab",
            description = "Outdoor/indoor vector field and displacement comparison.",
            engineMode = "Indoor",
            equation = "z = sin(x) + cos(y)",
            comparisonEquation = "y = x",
            depthMode = ArDepthOcclusionMode.DepthTest,
            performanceProfile = ArPerformanceProfile.HighQuality
        )
    )
}

class ArSceneShareExporter {
    fun export(
        scene: MathScene,
        equation: String,
        comparisonEquation: String,
        constructionCount: Int,
        graphQuality: String,
        colorMap: String
    ): ArSceneSharePackage {
        val payload = buildString {
            append("{")
            append("\"schema\":${scene.arProductionSettings.exportVersion},")
            append("\"sceneId\":\"${scene.id.escapeJson()}\",")
            append("\"name\":\"${scene.name.escapeJson()}\",")
            append("\"objects\":${scene.objects.size},")
            append("\"constructions\":$constructionCount,")
            append("\"equation\":\"${equation.escapeJson()}\",")
            append("\"comparison\":\"${comparisonEquation.escapeJson()}\",")
            append("\"graphQuality\":\"${graphQuality.escapeJson()}\",")
            append("\"colorMap\":\"${colorMap.escapeJson()}\",")
            append("\"anchor\":${scene.persistentAnchor.toJson()},")
            append("\"production\":${scene.arProductionSettings.toJson()}")
            append("}")
        }
        return ArSceneSharePackage(
            fileName = "${scene.name.sanitizedFileName()}.aistem-ar.json",
            mimeType = "application/vnd.aistem.ar+json",
            payload = payload,
            summary = "${scene.name}: ${scene.objects.size} objects, $constructionCount construction objects"
        )
    }

    private fun PersistentAnchorRecord.toJson(): String =
        """{"kind":"$kind","engine":"${engineMode.escapeJson()}","label":"${label.escapeJson()}","x":${worldPosition.x},"y":${worldPosition.y},"z":${worldPosition.z},"restore":"${restoreHint.escapeJson()}"}"""

    private fun ArSceneProductionSettings.toJson(): String =
        """{"depth":"$depthMode","performance":"$performanceProfile","meshDensity":$meshDensity,"maxObjects":$maxSceneObjects,"screenshotReady":$screenshotReady}"""
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
private fun String.sanitizedFileName(): String =
    replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "ar-scene" }.take(48)
