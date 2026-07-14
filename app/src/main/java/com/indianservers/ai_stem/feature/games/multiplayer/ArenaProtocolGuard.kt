package com.indianservers.ai_stem.feature.games.multiplayer

data class ArenaProtocolGuardConfig(
    val maxMessagesPerWindow: Int = 48,
    val windowMs: Long = 2_000L,
    val maxSeenMessageIds: Int = 512
)

data class ArenaProtocolGuardDecision(
    val accepted: Boolean,
    val reason: String? = null
)

class ArenaProtocolGuard(
    private val hostPlayerId: String,
    private val config: ArenaProtocolGuardConfig = ArenaProtocolGuardConfig(),
    private val rateLimiter: ArenaRateLimiter = ArenaRateLimiter(config.windowMs, config.maxMessagesPerWindow)
) {
    private val highestSequenceBySender = mutableMapOf<String, Long>()
    private val seenMessageIds = ArrayDeque<String>()
    private val seenMessageSet = mutableSetOf<String>()

    fun inspect(message: GameMessage, now: Long = System.currentTimeMillis()): ArenaProtocolGuardDecision {
        val senderKey = message.senderPlayerId ?: "anonymous"
        if (!rateLimiter.allow(senderKey, now)) return reject("Rate limit exceeded.")
        if (message.messageId in seenMessageSet) return reject("Duplicate message.")
        if (message.sequence < 0) return reject("Invalid sequence.")
        val previous = highestSequenceBySender[senderKey]
        if (previous != null && message.sequence <= previous) return reject("Out-of-order sequence.")
        if (message.payload.requiresHostAuthority() && message.senderPlayerId != hostPlayerId) {
            return reject("Host-authoritative payload from non-host sender.")
        }
        remember(message, senderKey)
        return ArenaProtocolGuardDecision(accepted = true)
    }

    fun decodeAndInspect(raw: String, now: Long = System.currentTimeMillis()): ArenaProtocolGuardDecision =
        ArenaProtocolCodec.decode(raw)
            .fold(
                onSuccess = { inspect(it, now) },
                onFailure = { reject("Malformed packet: ${it.message ?: "unknown"}") }
            )

    private fun remember(message: GameMessage, senderKey: String) {
        highestSequenceBySender[senderKey] = message.sequence
        seenMessageIds.addLast(message.messageId)
        seenMessageSet += message.messageId
        while (seenMessageIds.size > config.maxSeenMessageIds) {
            seenMessageSet -= seenMessageIds.removeFirst()
        }
    }

    private fun reject(reason: String) = ArenaProtocolGuardDecision(accepted = false, reason = reason)
}

fun GamePayload.requiresHostAuthority(): Boolean = when (this) {
    is GamePayload.JoinAccepted,
    is GamePayload.JoinRejected,
    is GamePayload.PlayerJoined,
    is GamePayload.PlayerDisconnected,
    is GamePayload.PlayerRemoved,
    is GamePayload.LobbySnapshot,
    is GamePayload.TeamAssigned,
    is GamePayload.RoomLocked,
    is GamePayload.MatchStarting,
    is GamePayload.ReconnectAccepted,
    is GamePayload.StateSnapshot,
    is GamePayload.ProtocolError,
    is GamePayload.CalibrationStarted,
    is GamePayload.CalibrationAccepted,
    is GamePayload.CalibrationRejected,
    is GamePayload.SharedOriginDefined,
    is GamePayload.SharedOriginVersionChanged,
    is GamePayload.SpatialSnapshotMessage,
    is GamePayload.AnchorDefinition,
    is GamePayload.AnchorRemoved,
    is GamePayload.RecalibrationRequested,
    is GamePayload.MissionCreated,
    is GamePayload.MissionAssigned,
    is GamePayload.RoleClueAssigned,
    is GamePayload.MissionStarted,
    is GamePayload.HintApproved,
    is GamePayload.AnswerValidated,
    is GamePayload.MissionStateChanged,
    is GamePayload.ScoreUpdated,
    is GamePayload.MissionCompleted,
    is GamePayload.BaseDefenseStageChanged,
    is GamePayload.TeamBaseDefined,
    is GamePayload.ResourceSpawned,
    is GamePayload.ResourceResolved,
    is GamePayload.DefenseBuilt,
    is GamePayload.EnemyWaveStarted,
    is GamePayload.EnemyStateUpdated,
    is GamePayload.BossPhaseChanged,
    is GamePayload.MatchPaused,
    is GamePayload.MatchRecoverySnapshot -> true
    is GamePayload.HostHello,
    is GamePayload.JoinRequest,
    is GamePayload.ReadyChanged,
    is GamePayload.TeamAssignmentRequested,
    is GamePayload.RoleSelected,
    is GamePayload.Heartbeat,
    is GamePayload.Acknowledgement,
    is GamePayload.ReconnectRequest,
    is GamePayload.MarkerDetected,
    is GamePayload.CalibrationQualityUpdated,
    is GamePayload.PlayerCalibrationState,
    is GamePayload.HintRequested,
    is GamePayload.AnswerSubmitted,
    is GamePayload.ParticipationUpdated -> false
}
