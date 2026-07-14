package com.indianservers.ai_stem.feature.games.spatial

enum class SharedAnchorType { MarkerOrigin, SurfaceOrigin, GameObject, ReticlePreview, TemporaryGuide }

data class SharedAnchorRecord(
    val anchorId: String,
    val sharedTransform: SharedTransform,
    val anchorType: SharedAnchorType,
    val ownerPlayerId: String,
    val creationSequence: Long,
    val active: Boolean = true,
    val originVersion: Long,
    val lastSynchronizationEpochMs: Long = System.currentTimeMillis()
)

class SharedAnchorRegistry<TLocalAnchor> {
    private val anchors = linkedMapOf<String, SharedAnchorRecord>()
    private val localAnchors = mutableMapOf<String, TLocalAnchor>()

    fun upsert(record: SharedAnchorRecord, localAnchor: TLocalAnchor? = null) {
        SharedTransformMath.validate(record.sharedTransform).getOrThrow()
        anchors[record.anchorId] = record
        if (localAnchor != null) localAnchors[record.anchorId] = localAnchor
    }

    fun remove(anchorId: String): SharedAnchorRecord? {
        localAnchors.remove(anchorId)
        val existing = anchors[anchorId] ?: return null
        val removed = existing.copy(active = false, lastSynchronizationEpochMs = System.currentTimeMillis())
        anchors[anchorId] = removed
        return removed
    }

    fun cleanupForOriginVersion(newOriginVersion: Long): List<SharedAnchorRecord> {
        val removed = anchors.values.filter { it.originVersion != newOriginVersion && it.active }
        removed.forEach { remove(it.anchorId) }
        return removed
    }

    fun cleanupAll(): List<SharedAnchorRecord> {
        val removed = anchors.values.filter { it.active }
        removed.forEach { remove(it.anchorId) }
        localAnchors.clear()
        return removed
    }

    fun records(): List<SharedAnchorRecord> = anchors.values.toList()
    fun localAnchor(anchorId: String): TLocalAnchor? = localAnchors[anchorId]
}
