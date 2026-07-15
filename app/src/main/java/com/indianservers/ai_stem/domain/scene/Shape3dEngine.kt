package com.indianservers.ai_stem.domain.scene

import com.indianservers.ai_stem.domain.mathematics.CalculatedMathProperty
import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.mathematics.parameterNumber
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

enum class Shape3dAnchorMode { MarkerImage, SurfacePlane, Air, OutdoorMesh }
enum class Shape3dRenderQuality(val curveSegments: Int, val surfaceSegments: Int) {
    Draft(curveSegments = 16, surfaceSegments = 12),
    Balanced(curveSegments = 32, surfaceSegments = 24),
    High(curveSegments = 64, surfaceSegments = 48),
    Ultra(curveSegments = 96, surfaceSegments = 72)
}

enum class Shape3dCapability {
    Solid,
    Flat,
    Wireframe,
    TransparentMaterial,
    MeasurementLabels,
    FormulaCards,
    VertexHandles,
    EdgeHandles,
    FaceHandles,
    AxisSnapping,
    GridSnapping,
    CrossSection,
    VolumeLayers,
    FunctionAnimation
}

data class Marker3dWorkspace(
    val markerName: String = "AI STEM Marker",
    val widthMeters: Double = 0.21,
    val heightMeters: Double = 0.21,
    val gridStepMeters: Double = 0.025,
    val safeInsetMeters: Double = 0.018
) {
    init {
        require(widthMeters > 0.05) { "Marker width must be usable for AR tracking." }
        require(heightMeters > 0.05) { "Marker height must be usable for AR tracking." }
        require(gridStepMeters > 0.0) { "Grid step must be positive." }
        require(safeInsetMeters >= 0.0) { "Safe inset cannot be negative." }
    }

    val usableHalfWidth: Double get() = (widthMeters / 2.0 - safeInsetMeters).coerceAtLeast(widthMeters * 0.2)
    val usableHalfHeight: Double get() = (heightMeters / 2.0 - safeInsetMeters).coerceAtLeast(heightMeters * 0.2)
}

data class Shape3dBounds(
    val widthMeters: Double,
    val heightMeters: Double,
    val depthMeters: Double
) {
    val footprintAreaSquareMeters: Double get() = widthMeters * depthMeters
    val maxDimensionMeters: Double get() = max(widthMeters, max(heightMeters, depthMeters))
}

data class Shape3dMeshBudget(
    val vertexCount: Int,
    val triangleCount: Int,
    val edgeSegmentCount: Int
)

data class Shape3dRenderProfile(
    val type: MathObjectType,
    val quality: Shape3dRenderQuality,
    val bounds: Shape3dBounds,
    val meshBudget: Shape3dMeshBudget,
    val capabilities: Set<Shape3dCapability>,
    val recommendedOpacity: Float,
    val showBackFaces: Boolean,
    val depthSorted: Boolean
)

data class Shape3dMetricSummary(
    val definitionId: String,
    val bounds: Shape3dBounds,
    val formulas: List<CalculatedMathProperty>,
    val primaryLabel: String,
    val volumeCubicMeters: Double?,
    val surfaceAreaSquareMeters: Double?
)

data class Shape3dPlacementRequest(
    val definitionId: String,
    val anchorMode: Shape3dAnchorMode,
    val markerWorkspace: Marker3dWorkspace = Marker3dWorkspace(),
    val normalizedX: Double = 0.0,
    val normalizedZ: Double = 0.0,
    val liftMeters: Double = 0.0,
    val snapToGrid: Boolean = true,
    val quality: Shape3dRenderQuality = Shape3dRenderQuality.Balanced
)

data class Shape3dPlacementPlan(
    val definitionId: String,
    val transform: ObjectTransform,
    val anchorStrategy: AnchorStrategy,
    val renderProfile: Shape3dRenderProfile,
    val metricSummary: Shape3dMetricSummary,
    val snapped: Boolean,
    val warnings: List<String>
)

object Shape3dEngine {
    fun placementPlan(request: Shape3dPlacementRequest): Shape3dPlacementPlan {
        val definition = requireNotNull(DefaultMathObjectRegistry.getDefinition(request.definitionId)) {
            "Unknown shape definition: ${request.definitionId}"
        }
        val metrics = metricSummary(request.definitionId, definition.defaultParameters)
        val localPosition = markerLocalPosition(
            workspace = request.markerWorkspace,
            normalizedX = request.normalizedX,
            normalizedZ = request.normalizedZ,
            liftMeters = request.liftMeters + defaultLift(definition.type, metrics.bounds),
            snapToGrid = request.snapToGrid
        )
        val scale = uniformScaleForWorkspace(metrics.bounds, request.markerWorkspace)
        val transform = ObjectTransform(
            position = localPosition,
            scale = Vector3Value(scale, scale, scale)
        )
        return Shape3dPlacementPlan(
            definitionId = request.definitionId,
            transform = transform,
            anchorStrategy = request.anchorMode.toAnchorStrategy(),
            renderProfile = renderProfile(definition.type, definition.defaultParameters, request.quality),
            metricSummary = metrics,
            snapped = request.snapToGrid,
            warnings = placementWarnings(metrics.bounds, request.markerWorkspace, request.anchorMode)
        )
    }

    fun metricSummary(
        definitionId: String,
        parameters: Map<String, MathParameterValue> = DefaultMathObjectRegistry.getDefinition(definitionId)?.defaultParameters.orEmpty(),
        transform: ObjectTransform = ObjectTransform()
    ): Shape3dMetricSummary {
        val definition = requireNotNull(DefaultMathObjectRegistry.getDefinition(definitionId)) {
            "Unknown shape definition: $definitionId"
        }
        val scale = transform.scale
        val bounds = baseBounds(definition.type, parameters).scaled(scale)
        val formulas = definition.measurementProvider(parameters)
        return Shape3dMetricSummary(
            definitionId = definitionId,
            bounds = bounds,
            formulas = formulas,
            primaryLabel = primaryFormulaLabel(definition.type, formulas),
            volumeCubicMeters = formulas.firstOrNull { it.id == "volume" }?.value?.times(scale.x * scale.y * scale.z),
            surfaceAreaSquareMeters = formulas.firstOrNull { it.id == "surfaceArea" || it.id == "area" }?.value
        )
    }

    fun renderProfile(
        type: MathObjectType,
        parameters: Map<String, MathParameterValue> = emptyMap(),
        quality: Shape3dRenderQuality = Shape3dRenderQuality.Balanced
    ): Shape3dRenderProfile {
        val bounds = baseBounds(type, parameters)
        return Shape3dRenderProfile(
            type = type,
            quality = quality,
            bounds = bounds,
            meshBudget = meshBudget(type, quality),
            capabilities = capabilities(type),
            recommendedOpacity = recommendedOpacity(type),
            showBackFaces = type in setOf(MathObjectType.Circle, MathObjectType.Triangle, MathObjectType.CoordinatePlane),
            depthSorted = type in setOf(MathObjectType.Sphere, MathObjectType.Cylinder, MathObjectType.Cone)
        )
    }

    fun markerLocalPosition(
        workspace: Marker3dWorkspace,
        normalizedX: Double,
        normalizedZ: Double,
        liftMeters: Double = 0.0,
        snapToGrid: Boolean = true
    ): Vector3Value {
        val clampedX = normalizedX.coerceIn(-1.0, 1.0) * workspace.usableHalfWidth
        val clampedZ = normalizedZ.coerceIn(-1.0, 1.0) * workspace.usableHalfHeight
        val raw = Vector3Value(clampedX, liftMeters.coerceAtLeast(0.0), clampedZ)
        return if (snapToGrid) snapToMarkerGrid(raw, workspace) else raw
    }

    fun snapToMarkerGrid(position: Vector3Value, workspace: Marker3dWorkspace): Vector3Value =
        Vector3Value(
            x = snap(position.x, workspace.gridStepMeters).coerceIn(-workspace.usableHalfWidth, workspace.usableHalfWidth),
            y = position.y.coerceAtLeast(0.0),
            z = snap(position.z, workspace.gridStepMeters).coerceIn(-workspace.usableHalfHeight, workspace.usableHalfHeight)
        )

    fun validateTransform(transform: ObjectTransform, workspace: Marker3dWorkspace? = null): List<String> {
        val warnings = mutableListOf<String>()
        if (!transform.isValid()) warnings += "Transform contains invalid position, rotation, or scale."
        if (workspace != null) {
            if (abs(transform.position.x) > workspace.usableHalfWidth) warnings += "Object is outside marker width."
            if (abs(transform.position.z) > workspace.usableHalfHeight) warnings += "Object is outside marker height."
        }
        if (transform.scale.x > 8.0 || transform.scale.y > 8.0 || transform.scale.z > 8.0) {
            warnings += "Object scale is too large for stable AR interaction."
        }
        return warnings
    }

    private fun baseBounds(type: MathObjectType, parameters: Map<String, MathParameterValue>): Shape3dBounds =
        when (type) {
            MathObjectType.Cube -> {
                val side = parameterNumber(parameters, "sideLength", 0.24)
                Shape3dBounds(side, side, side)
            }
            MathObjectType.RectangularPrism -> Shape3dBounds(
                widthMeters = parameterNumber(parameters, "length", 0.32),
                heightMeters = parameterNumber(parameters, "height", 0.22),
                depthMeters = parameterNumber(parameters, "width", 0.18)
            )
            MathObjectType.Sphere -> {
                val diameter = parameterNumber(parameters, "radius", 0.16) * 2.0
                Shape3dBounds(diameter, diameter, diameter)
            }
            MathObjectType.Cylinder -> {
                val diameter = parameterNumber(parameters, "radius", 0.12) * 2.0
                Shape3dBounds(diameter, parameterNumber(parameters, "height", 0.32), diameter)
            }
            MathObjectType.Cone -> {
                val diameter = parameterNumber(parameters, "radius", 0.12) * 2.0
                Shape3dBounds(diameter, parameterNumber(parameters, "height", 0.32), diameter)
            }
            MathObjectType.Triangle -> {
                val width = max(parameterNumber(parameters, "sideA", 0.3), max(parameterNumber(parameters, "sideB", 0.25), parameterNumber(parameters, "sideC", 0.22)))
                Shape3dBounds(width, 0.01, width * 0.82)
            }
            MathObjectType.Circle -> {
                val diameter = parameterNumber(parameters, "radius", 0.18) * 2.0
                Shape3dBounds(diameter, 0.01, diameter)
            }
            MathObjectType.NumberLine -> {
                val range = abs(parameterNumber(parameters, "maximum", 5.0) - parameterNumber(parameters, "minimum", -5.0)).coerceAtLeast(1.0)
                Shape3dBounds((range * 0.04).coerceIn(0.2, 1.2), 0.01, 0.04)
            }
            MathObjectType.VectorArrow -> {
                val x = parameterNumber(parameters, "x", 1.0)
                val y = parameterNumber(parameters, "y", 1.0)
                val z = parameterNumber(parameters, "z", 0.0)
                val magnitude = sqrt(x * x + y * y + z * z).coerceAtLeast(0.1)
                Shape3dBounds(magnitude * 0.18, magnitude * 0.18, magnitude * 0.18)
            }
            MathObjectType.SineCurve -> Shape3dBounds(0.56, 0.22, 0.04)
            MathObjectType.CoordinatePlane -> {
                val width = abs(parameterNumber(parameters, "xMax", 5.0) - parameterNumber(parameters, "xMin", -5.0)).coerceAtLeast(1.0)
                val height = abs(parameterNumber(parameters, "yMax", 5.0) - parameterNumber(parameters, "yMin", -5.0)).coerceAtLeast(1.0)
                Shape3dBounds((width * 0.04).coerceIn(0.2, 1.2), 0.01, (height * 0.04).coerceIn(0.2, 1.2))
            }
        }

    private fun Shape3dBounds.scaled(scale: Vector3Value): Shape3dBounds =
        Shape3dBounds(widthMeters * scale.x, heightMeters * scale.y, depthMeters * scale.z)

    private fun meshBudget(type: MathObjectType, quality: Shape3dRenderQuality): Shape3dMeshBudget =
        when (type) {
            MathObjectType.Cube, MathObjectType.RectangularPrism -> Shape3dMeshBudget(8, 12, 12)
            MathObjectType.Sphere -> {
                val lat = quality.surfaceSegments
                val lon = quality.surfaceSegments * 2
                Shape3dMeshBudget((lat + 1) * (lon + 1), lat * lon * 2, lat * lon)
            }
            MathObjectType.Cylinder -> {
                val s = quality.curveSegments
                Shape3dMeshBudget((s + 1) * 2 + 2, s * 4, s * 3)
            }
            MathObjectType.Cone -> {
                val s = quality.curveSegments
                Shape3dMeshBudget(s + 2, s * 2, s * 2)
            }
            MathObjectType.Circle -> Shape3dMeshBudget(quality.curveSegments + 1, quality.curveSegments, quality.curveSegments)
            MathObjectType.Triangle -> Shape3dMeshBudget(3, 1, 3)
            MathObjectType.SineCurve -> Shape3dMeshBudget(quality.curveSegments + 1, 0, quality.curveSegments)
            MathObjectType.CoordinatePlane -> {
                val lines = quality.surfaceSegments + 1
                Shape3dMeshBudget(lines * 4, 0, lines * 2)
            }
            MathObjectType.NumberLine -> Shape3dMeshBudget(quality.curveSegments * 2, 0, quality.curveSegments)
            MathObjectType.VectorArrow -> Shape3dMeshBudget(24, 20, 12)
        }

    private fun capabilities(type: MathObjectType): Set<Shape3dCapability> {
        val common = setOf(
            Shape3dCapability.Wireframe,
            Shape3dCapability.TransparentMaterial,
            Shape3dCapability.MeasurementLabels,
            Shape3dCapability.FormulaCards,
            Shape3dCapability.AxisSnapping,
            Shape3dCapability.GridSnapping
        )
        return common + when (type) {
            MathObjectType.Cube, MathObjectType.RectangularPrism -> setOf(
                Shape3dCapability.Solid,
                Shape3dCapability.VertexHandles,
                Shape3dCapability.EdgeHandles,
                Shape3dCapability.FaceHandles,
                Shape3dCapability.CrossSection,
                Shape3dCapability.VolumeLayers
            )
            MathObjectType.Sphere, MathObjectType.Cylinder, MathObjectType.Cone -> setOf(
                Shape3dCapability.Solid,
                Shape3dCapability.CrossSection,
                Shape3dCapability.VolumeLayers
            )
            MathObjectType.SineCurve -> setOf(Shape3dCapability.FunctionAnimation, Shape3dCapability.VertexHandles)
            MathObjectType.CoordinatePlane, MathObjectType.NumberLine, MathObjectType.VectorArrow -> setOf(Shape3dCapability.VertexHandles)
            MathObjectType.Triangle, MathObjectType.Circle -> setOf(Shape3dCapability.Flat, Shape3dCapability.VertexHandles, Shape3dCapability.EdgeHandles)
        }
    }

    private fun defaultLift(type: MathObjectType, bounds: Shape3dBounds): Double =
        when (type) {
            MathObjectType.Circle,
            MathObjectType.Triangle,
            MathObjectType.CoordinatePlane,
            MathObjectType.NumberLine,
            MathObjectType.SineCurve -> 0.004
            else -> bounds.heightMeters / 2.0
        }

    private fun uniformScaleForWorkspace(bounds: Shape3dBounds, workspace: Marker3dWorkspace): Double {
        val available = minOf(workspace.usableHalfWidth * 1.5, workspace.usableHalfHeight * 1.5)
        if (bounds.maxDimensionMeters <= available) return 1.0
        return (available / bounds.maxDimensionMeters).coerceIn(0.15, 1.0)
    }

    private fun placementWarnings(bounds: Shape3dBounds, workspace: Marker3dWorkspace, anchorMode: Shape3dAnchorMode): List<String> {
        val warnings = mutableListOf<String>()
        if (anchorMode == Shape3dAnchorMode.MarkerImage && bounds.maxDimensionMeters > workspace.widthMeters) {
            warnings += "Shape is larger than the marker; auto-scaling is recommended."
        }
        if (bounds.heightMeters > workspace.widthMeters * 1.5) {
            warnings += "Tall shape may drift visually if the marker is viewed at a steep angle."
        }
        return warnings
    }

    private fun primaryFormulaLabel(type: MathObjectType, formulas: List<CalculatedMathProperty>): String {
        val preferred = when (type) {
            MathObjectType.Cube,
            MathObjectType.RectangularPrism,
            MathObjectType.Sphere,
            MathObjectType.Cylinder,
            MathObjectType.Cone -> "volume"
            MathObjectType.Circle,
            MathObjectType.Triangle -> "area"
            MathObjectType.VectorArrow -> "magnitude"
            MathObjectType.SineCurve -> "amplitude"
            MathObjectType.CoordinatePlane -> "distance"
            MathObjectType.NumberLine -> "range"
        }
        val property = formulas.firstOrNull { it.id == preferred } ?: formulas.firstOrNull()
        return property?.let { "${it.label}: ${formatNumber(it.value)}${it.unitLabel?.let { unit -> " $unit" }.orEmpty()}" }.orEmpty()
    }

    private fun recommendedOpacity(type: MathObjectType): Float =
        when (type) {
            MathObjectType.Cube,
            MathObjectType.RectangularPrism,
            MathObjectType.Sphere,
            MathObjectType.Cylinder,
            MathObjectType.Cone -> 0.68f
            MathObjectType.SineCurve,
            MathObjectType.VectorArrow -> 0.92f
            else -> 0.78f
        }

    private fun Shape3dAnchorMode.toAnchorStrategy(): AnchorStrategy =
        when (this) {
            Shape3dAnchorMode.MarkerImage,
            Shape3dAnchorMode.SurfacePlane,
            Shape3dAnchorMode.OutdoorMesh -> AnchorStrategy.SceneOrigin
            Shape3dAnchorMode.Air -> AnchorStrategy.Independent
        }

    private fun snap(value: Double, step: Double): Double =
        round(value / step) * step

    private fun formatNumber(value: Double): String =
        when {
            value.isNaN() || value.isInfinite() -> "n/a"
            abs(value) >= 100 -> "%.0f".format(value)
            abs(value) >= 10 -> "%.1f".format(value)
            else -> "%.2f".format(value)
        }.trimEnd('0').trimEnd('.')
}

fun markerWorkspaceForA4Worksheet(widthMeters: Double = 0.21): Marker3dWorkspace =
    Marker3dWorkspace(
        markerName = "A4 worksheet marker",
        widthMeters = widthMeters,
        heightMeters = widthMeters * sqrt(2.0),
        gridStepMeters = widthMeters / ceil(widthMeters / 0.025),
        safeInsetMeters = widthMeters * 0.08
    )
