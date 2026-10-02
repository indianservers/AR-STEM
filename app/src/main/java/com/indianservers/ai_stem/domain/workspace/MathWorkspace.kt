package com.indianservers.ai_stem.domain.workspace

enum class WorkspaceEnvironmentMode(val displayName: String) {
    White("White Workspace"),
    Black("Black Workspace"),
    AR("AR Workspace")
}

enum class MathWorkspaceModule {
    Graph,
    Geometry2D,
    Geometry3D
}

data class WorkspaceViewportState(
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val zoom: Double = 1.0,
    val rotationX: Double = 0.0,
    val rotationY: Double = 0.0,
    val rotationZ: Double = 0.0
)

interface WorkspaceEnvironment {
    val mode: WorkspaceEnvironmentMode
    val usesCamera: Boolean
}

object LightWorkspaceEnvironment : WorkspaceEnvironment {
    override val mode = WorkspaceEnvironmentMode.White
    override val usesCamera = false
}

object DarkWorkspaceEnvironment : WorkspaceEnvironment {
    override val mode = WorkspaceEnvironmentMode.Black
    override val usesCamera = false
}

object ArCameraEnvironment : WorkspaceEnvironment {
    override val mode = WorkspaceEnvironmentMode.AR
    override val usesCamera = true
}

interface MathWorkspaceRenderer<Model> {
    val module: MathWorkspaceModule
    fun supports(environment: WorkspaceEnvironment): Boolean
    fun submit(model: Model, viewport: WorkspaceViewportState)
}

interface GraphRenderer<Model> : MathWorkspaceRenderer<Model>
interface Geometry2DRenderer<Model> : MathWorkspaceRenderer<Model>
interface Geometry3DRenderer<Model> : MathWorkspaceRenderer<Model>

fun WorkspaceEnvironmentMode.environment(): WorkspaceEnvironment = when (this) {
    WorkspaceEnvironmentMode.White -> LightWorkspaceEnvironment
    WorkspaceEnvironmentMode.Black -> DarkWorkspaceEnvironment
    WorkspaceEnvironmentMode.AR -> ArCameraEnvironment
}
