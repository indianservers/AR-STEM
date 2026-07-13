package com.indianservers.ai_stem.domain.interaction

data class CoalescedInteraction<T>(
    val id: String,
    val description: String,
    val before: T,
    val after: T
)

class CoalescedInteractionHistory<T>(private val maxEntries: Int = 100) {
    private var active: ActiveInteraction<T>? = null
    private val undoStack = mutableListOf<CoalescedInteraction<T>>()
    private val redoStack = mutableListOf<CoalescedInteraction<T>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size

    fun begin(id: String, description: String, initial: T) {
        active = ActiveInteraction(id, description, initial, initial)
    }

    fun update(value: T): T {
        val current = active ?: return value
        active = current.copy(current = value)
        return value
    }

    fun commit(): CoalescedInteraction<T>? {
        val current = active ?: return null
        active = null
        if (current.initial == current.current) return null
        val entry = CoalescedInteraction(current.id, current.description, current.initial, current.current)
        undoStack += entry
        while (undoStack.size > maxEntries) undoStack.removeAt(0)
        redoStack.clear()
        return entry
    }

    fun cancel(): T? {
        val current = active ?: return null
        active = null
        return current.initial
    }

    fun undo(current: T): T {
        val entry = undoStack.removeLastOrNull() ?: return current
        redoStack += entry
        return entry.before
    }

    fun redo(current: T): T {
        val entry = redoStack.removeLastOrNull() ?: return current
        undoStack += entry
        return entry.after
    }

    private data class ActiveInteraction<T>(
        val id: String,
        val description: String,
        val initial: T,
        val current: T
    )
}
