package com.indianservers.ai_stem.feature.games.recovery

import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchStage
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode

data class ArenaRecoverySnapshot(
    val roomId: String,
    val roomCode: String,
    val hostPlayerId: String,
    val matchId: String,
    val protocolVersion: Int,
    val roomVersion: Long,
    val originVersion: Long,
    val originMode: SharedOriginMode,
    val matchStage: BaseDefenseMatchStage,
    val hostSequence: Long,
    val savedAtEpochMs: Long
)

object ArenaSnapshotCodec {
    fun encode(snapshot: ArenaRecoverySnapshot): String = listOf(
        snapshot.roomId,
        snapshot.roomCode,
        snapshot.hostPlayerId,
        snapshot.matchId,
        snapshot.protocolVersion,
        snapshot.roomVersion,
        snapshot.originVersion,
        snapshot.originMode.name,
        snapshot.matchStage.name,
        snapshot.hostSequence,
        snapshot.savedAtEpochMs
    ).joinToString("|") { it.toString().escape() }

    fun decode(raw: String): Result<ArenaRecoverySnapshot> = runCatching {
        val p = raw.split("|").map { it.unescape() }
        require(p.size == 11) { "Invalid recovery snapshot." }
        ArenaRecoverySnapshot(
            roomId = p[0],
            roomCode = p[1],
            hostPlayerId = p[2],
            matchId = p[3],
            protocolVersion = p[4].toInt(),
            roomVersion = p[5].toLong(),
            originVersion = p[6].toLong(),
            originMode = SharedOriginMode.valueOf(p[7]),
            matchStage = BaseDefenseMatchStage.valueOf(p[8]),
            hostSequence = p[9].toLong(),
            savedAtEpochMs = p[10].toLong()
        )
    }

    private fun String.escape(): String = replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n")
    private fun String.unescape(): String = replace("\\n", "\n").replace("\\p", "|").replace("\\\\", "\\")
}
