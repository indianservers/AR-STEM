package com.indianservers.ai_stem.domain.scene

import com.indianservers.ai_stem.domain.mathematics.DefaultMathObjectRegistry
import com.indianservers.ai_stem.domain.mathematics.MathParameterValue

interface SceneCommand {
    val id: String
    val description: String
    fun apply(scene: MathScene): MathScene
    fun revert(scene: MathScene): MathScene
}

data class SnapshotSceneCommand(
    override val id: String = IdFactory.nextSceneId(),
    override val description: String,
    private val before: MathScene,
    private val after: MathScene
) : SceneCommand {
    override fun apply(scene: MathScene): MathScene = after
    override fun revert(scene: MathScene): MathScene = before
}

data class SceneHistory(
    val undoStack: List<SceneCommand> = emptyList(),
    val redoStack: List<SceneCommand> = emptyList(),
    val maxEntries: Int = 100,
    val lastMessage: String? = null
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(command: SceneCommand): SceneHistory =
        copy(
            undoStack = (undoStack + command).takeLast(maxEntries),
            redoStack = emptyList(),
            lastMessage = command.description
        )

    fun undo(scene: MathScene): Pair<MathScene, SceneHistory> {
        val command = undoStack.lastOrNull() ?: return scene to this
        return command.revert(scene) to copy(
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + command,
            lastMessage = "Undid ${command.description.lowercase()}"
        )
    }

    fun redo(scene: MathScene): Pair<MathScene, SceneHistory> {
        val command = redoStack.lastOrNull() ?: return scene to this
        return command.apply(scene) to copy(
            undoStack = undoStack + command,
            redoStack = redoStack.dropLast(1),
            lastMessage = "Redid ${command.description.lowercase()}"
        )
    }
}

object SceneMutations {
    fun addObject(scene: MathScene, definitionId: String): MathScene {
        val offset = scene.objects.size * 0.12
        return addObject(scene, definitionId, Vector3Value(offset, 0.0, offset))
    }

    fun addObject(scene: MathScene, definitionId: String, position: Vector3Value): MathScene {
        val objectState = SceneObjectFactory.create(
            definitionId = definitionId,
            existingNames = scene.objects.map { it.displayName },
            position = position
        )
        return selectOnly(scene.copy(objects = scene.objects + objectState, updatedAt = System.currentTimeMillis()), objectState.id)
    }

    fun addObject(scene: MathScene, definitionId: String, transform: ObjectTransform): MathScene {
        val objectState = SceneObjectFactory.create(
            definitionId = definitionId,
            existingNames = scene.objects.map { it.displayName },
            transform = transform
        )
        return selectOnly(scene.copy(objects = scene.objects + objectState, updatedAt = System.currentTimeMillis()), objectState.id)
    }

    fun selectOnly(scene: MathScene, objectId: String?): MathScene =
        scene.copy(objects = scene.objects.map { it.copy(interactionState = it.interactionState.copy(selected = it.id == objectId)) })

    fun toggleSelection(scene: MathScene, objectId: String): MathScene =
        scene.copy(objects = scene.objects.map {
            if (it.id == objectId) it.copy(interactionState = it.interactionState.copy(selected = !it.interactionState.selected)) else it
        })

    fun deleteSelected(scene: MathScene): MathScene {
        val selected = scene.selectedObjectIds.toSet()
        return scene.copy(
            objects = scene.objects.filterNot { it.id in selected },
            groups = scene.groups.map { it.copy(memberObjectIds = it.memberObjectIds.filterNot(selected::contains)) }
                .filter { it.memberObjectIds.size >= 2 },
            annotations = scene.annotations.filterNot { it.objectId in selected },
            updatedAt = System.currentTimeMillis()
        )
    }

    fun duplicateSelected(scene: MathScene): MathScene {
        val existingNames = scene.objects.map { it.displayName }
        val copies = scene.objects.filter { it.interactionState.selected }.mapIndexed { index, original ->
            original.copy(
                id = IdFactory.nextObjectId(),
                displayName = SceneObjectFactory.uniqueName("${original.displayName} Copy", existingNames + scene.objects.map { it.displayName }),
                transform = original.transform.copy(position = original.transform.position.plus(Vector3Value(0.12 + index * 0.04, 0.0, 0.12))),
                groupId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                interactionState = original.interactionState.copy(selected = true)
            )
        }
        return scene.copy(
            objects = scene.objects.map { it.copy(interactionState = it.interactionState.copy(selected = false)) } + copies,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun rename(scene: MathScene, objectId: String, rawName: String): MathScene {
        val name = rawName.trim().replace(Regex("\\s+"), " ").take(40)
        require(name.isNotBlank()) { "Name cannot be empty." }
        val existing = scene.objects.filterNot { it.id == objectId }.map { it.displayName }
        val finalName = SceneObjectFactory.uniqueName(name, existing)
        return scene.copy(objects = scene.objects.map {
            if (it.id == objectId) it.copy(displayName = finalName, updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun setLocked(scene: MathScene, objectId: String, locked: Boolean): MathScene =
        scene.copy(objects = scene.objects.map {
            if (it.id == objectId) it.copy(interactionState = it.interactionState.copy(locked = locked), updatedAt = System.currentTimeMillis()) else it
        })

    fun setVisible(scene: MathScene, objectId: String, visible: Boolean): MathScene =
        scene.copy(objects = scene.objects.map {
            if (it.id == objectId) it.copy(visibility = ObjectVisibility(visible), updatedAt = System.currentTimeMillis()) else it
        })

    fun updateParameter(scene: MathScene, objectId: String, parameterId: String, value: MathParameterValue): MathScene {
        val target = scene.objects.firstOrNull { it.id == objectId } ?: return scene
        if (target.interactionState.locked) return scene
        val definition = DefaultMathObjectRegistry.getDefinition(target.definitionId) ?: return scene
        val nextParameters = target.parameters + (parameterId to value)
        require(definition.validate(nextParameters).isEmpty()) { "Invalid parameter." }
        return scene.copy(objects = scene.objects.map {
            if (it.id == objectId) it.copy(parameters = nextParameters, updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun groupSelected(scene: MathScene): MathScene {
        val selected = scene.objects.filter { it.interactionState.selected }
        require(selected.size >= 2) { "Select at least two objects to group." }
        val pivot = Vector3Value(
            selected.map { it.transform.position.x }.average(),
            selected.map { it.transform.position.y }.average(),
            selected.map { it.transform.position.z }.average()
        )
        val group = SceneGroup(
            id = IdFactory.nextGroupId(),
            displayName = "Group ${scene.groups.size + 1}",
            memberObjectIds = selected.map { it.id },
            pivot = pivot
        )
        return scene.copy(
            groups = scene.groups + group,
            objects = scene.objects.map { if (it.id in group.memberObjectIds) it.copy(groupId = group.id) else it },
            updatedAt = System.currentTimeMillis()
        )
    }

    fun ungroupSelected(scene: MathScene): MathScene {
        val selectedGroups = scene.objects.filter { it.interactionState.selected }.mapNotNull { it.groupId }.toSet()
        return scene.copy(
            groups = scene.groups.filterNot { it.id in selectedGroups },
            objects = scene.objects.map { if (it.groupId in selectedGroups) it.copy(groupId = null) else it },
            updatedAt = System.currentTimeMillis()
        )
    }

    fun clear(scene: MathScene): MathScene =
        scene.copy(objects = emptyList(), groups = emptyList(), annotations = emptyList(), updatedAt = System.currentTimeMillis())
}
