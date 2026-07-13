package com.indianservers.ai_stem.domain.scene

import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MathObjectType
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue
import com.indianservers.ai_stem.domain.mathematics.MeasurementUnit
import com.indianservers.ai_stem.domain.mathematics.normalizeRotationDegrees
import kotlin.math.max

const val CURRENT_SCENE_SCHEMA_VERSION = 2

data class Vector3Value(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0) {
    fun plus(other: Vector3Value) = Vector3Value(x + other.x, y + other.y, z + other.z)
    fun isFinite() = x.isFinite() && y.isFinite() && z.isFinite()
}

data class QuaternionValue(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0, val w: Double = 1.0) {
    fun isFinite() = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()
}

data class ObjectTransform(
    val position: Vector3Value = Vector3Value(),
    val rotation: QuaternionValue = QuaternionValue(),
    val scale: Vector3Value = Vector3Value(1.0, 1.0, 1.0)
) {
    fun isValid(): Boolean = position.isFinite() && rotation.isFinite() && scale.isFinite() &&
        scale.x >= 0.05 && scale.y >= 0.05 && scale.z >= 0.05
}

data class ObjectAppearance(
    val fillColor: Long = 0xAA4FD1C5,
    val edgeColor: Long = 0xFF4EE7FF,
    val opacity: Float = 0.84f,
    val edgesVisible: Boolean = true,
    val labelsVisible: Boolean = true,
    val measurementsVisible: Boolean = true
)

data class ObjectInteractionState(
    val selected: Boolean = false,
    val locked: Boolean = false
)

data class ObjectVisibility(val visible: Boolean = true)

data class AnchorReference(
    val strategy: AnchorStrategy = AnchorStrategy.SceneOrigin,
    val recoveryNote: String = "Place Scene"
)

enum class AnchorStrategy { SceneOrigin, Independent, NeedsPlacement }

data class SceneAnnotation(
    val id: String,
    val objectId: String?,
    val text: String,
    val colorTag: Long = 0xFFFFC857,
    val hidden: Boolean = false
)

data class SceneGroup(
    val id: String,
    val displayName: String,
    val memberObjectIds: List<String>,
    val pivot: Vector3Value
)

data class MathSceneObject(
    val id: String,
    val definitionId: String,
    val displayName: String,
    val objectType: MathObjectType,
    val transform: ObjectTransform,
    val parameters: Map<String, MathParameterValue>,
    val appearance: ObjectAppearance = ObjectAppearance(),
    val interactionState: ObjectInteractionState = ObjectInteractionState(),
    val visibility: ObjectVisibility = ObjectVisibility(),
    val groupId: String? = null,
    val anchorReference: AnchorReference? = AnchorReference(),
    val createdAt: Long,
    val updatedAt: Long
)

data class MeasurementSettings(
    val displayUnit: MeasurementUnit = MeasurementUnit.Centimetres,
    val labelsEnabled: Boolean = true
)

data class MathScene(
    val id: String = IdFactory.nextSceneId(),
    val name: String = "Untitled Mathematics Scene",
    val schemaVersion: Int = CURRENT_SCENE_SCHEMA_VERSION,
    val objects: List<MathSceneObject> = emptyList(),
    val groups: List<SceneGroup> = emptyList(),
    val annotations: List<SceneAnnotation> = emptyList(),
    val measurementSettings: MeasurementSettings = MeasurementSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val originStrategy: AnchorStrategy = AnchorStrategy.SceneOrigin
) {
    val selectedObjectIds: List<String> get() = objects.filter { it.interactionState.selected }.map { it.id }
    val primarySelectedObject: MathSceneObject? get() = objects.firstOrNull { it.interactionState.selected }
}

enum class ExperienceMode { Beginner, Advanced }

sealed interface SceneInteractionMode {
    data object Select : SceneInteractionMode
    data object Place : SceneInteractionMode
    data object Move : SceneInteractionMode
    data object Rotate : SceneInteractionMode
    data object Scale : SceneInteractionMode
    data object Measure : SceneInteractionMode
    data object MultiSelect : SceneInteractionMode
}

object IdFactory {
    private var counter = 0L
    fun nextObjectId(): String = "object-${System.currentTimeMillis()}-${counter++}"
    fun nextGroupId(): String = "group-${System.currentTimeMillis()}-${counter++}"
    fun nextSceneId(): String = "scene-${System.currentTimeMillis()}-${counter++}"
}

object SceneObjectFactory {
    fun create(definitionId: String, existingNames: List<String> = emptyList(), position: Vector3Value = Vector3Value()): MathSceneObject {
        val definition = requireNotNull(DefaultMathObjectRegistry.getDefinition(definitionId)) {
            "Unknown object definition: $definitionId"
        }
        val now = System.currentTimeMillis()
        return MathSceneObject(
            id = IdFactory.nextObjectId(),
            definitionId = definition.definitionId,
            displayName = uniqueName(definition.displayName, existingNames),
            objectType = definition.type,
            transform = ObjectTransform(position = position),
            parameters = definition.defaultParameters,
            createdAt = now,
            updatedAt = now
        )
    }

    fun uniqueName(baseName: String, existingNames: List<String>): String {
        if (baseName !in existingNames) return baseName
        var index = 2
        while ("$baseName $index" in existingNames) index++
        return "$baseName $index"
    }
}

data class TransformMovement(val delta: Vector3Value)
data class TransformRotation(val xDegrees: Double = 0.0, val yDegrees: Double = 0.0, val zDegrees: Double = 0.0)
data class TransformScaling(val x: Double, val y: Double = x, val z: Double = x)

sealed interface SceneMutationResult {
    data class Success(val scene: MathScene, val message: String) : SceneMutationResult
    data class Failure(val message: String) : SceneMutationResult
}

interface ObjectTransformService {
    fun move(scene: MathScene, objectId: String, movement: TransformMovement): SceneMutationResult
    fun rotate(scene: MathScene, objectId: String, rotation: TransformRotation): SceneMutationResult
    fun scale(scene: MathScene, objectId: String, scaling: TransformScaling): SceneMutationResult
    fun reset(scene: MathScene, objectId: String): SceneMutationResult
}

object DefaultObjectTransformService : ObjectTransformService {
    override fun move(scene: MathScene, objectId: String, movement: TransformMovement): SceneMutationResult =
        mutateUnlocked(scene, objectId, "Moved") {
            val transform = it.transform.copy(position = it.transform.position.plus(movement.delta))
            it.copy(transform = transform, updatedAt = System.currentTimeMillis())
        }

    override fun rotate(scene: MathScene, objectId: String, rotation: TransformRotation): SceneMutationResult =
        mutateUnlocked(scene, objectId, "Turned") {
            val transform = it.transform.copy(
                rotation = QuaternionValue(
                    x = normalizeRotationDegrees(rotation.xDegrees.toFloat()).toDouble(),
                    y = normalizeRotationDegrees(rotation.yDegrees.toFloat()).toDouble(),
                    z = normalizeRotationDegrees(rotation.zDegrees.toFloat()).toDouble(),
                    w = 1.0
                )
            )
            it.copy(transform = transform, updatedAt = System.currentTimeMillis())
        }

    override fun scale(scene: MathScene, objectId: String, scaling: TransformScaling): SceneMutationResult =
        mutateUnlocked(scene, objectId, "Resized") {
            val next = ObjectTransform(
                position = it.transform.position,
                rotation = it.transform.rotation,
                scale = Vector3Value(
                    max(0.05, it.transform.scale.x * scaling.x),
                    max(0.05, it.transform.scale.y * scaling.y),
                    max(0.05, it.transform.scale.z * scaling.z)
                )
            )
            if (!next.isValid()) it else it.copy(transform = next, updatedAt = System.currentTimeMillis())
        }

    override fun reset(scene: MathScene, objectId: String): SceneMutationResult =
        mutateUnlocked(scene, objectId, "Transform reset") {
            it.copy(transform = ObjectTransform(position = it.transform.position), updatedAt = System.currentTimeMillis())
        }

    private fun mutateUnlocked(
        scene: MathScene,
        objectId: String,
        message: String,
        transform: (MathSceneObject) -> MathSceneObject
    ): SceneMutationResult {
        val target = scene.objects.firstOrNull { it.id == objectId } ?: return SceneMutationResult.Failure("This object could not be found.")
        if (target.interactionState.locked) return SceneMutationResult.Failure("Unlock the object before changing it.")
        return SceneMutationResult.Success(
            scene.copy(objects = scene.objects.map { if (it.id == objectId) transform(it) else it }, updatedAt = System.currentTimeMillis()),
            message
        )
    }
}
