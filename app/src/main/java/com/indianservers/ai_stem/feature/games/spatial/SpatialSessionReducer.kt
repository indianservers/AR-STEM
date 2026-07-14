package com.indianservers.ai_stem.feature.games.spatial

sealed interface SpatialCommand {
    data class StartCalibration(val hostPlayerId: String, val mode: SharedOriginMode) : SpatialCommand
    data class UpdatePlayerCalibration(val status: PlayerCalibrationStatus) : SpatialCommand
    data class AcceptCalibration(val hostPlayerId: String, val playerId: String) : SpatialCommand
    data class RejectCalibration(val hostPlayerId: String, val playerId: String, val reason: String) : SpatialCommand
    data class DefineOrigin(val hostPlayerId: String, val definition: SharedOriginDefinition) : SpatialCommand
    data class UpsertAnchor(val hostPlayerId: String, val anchor: SharedAnchorRecord) : SpatialCommand
    data class RemoveAnchor(val hostPlayerId: String, val anchorId: String) : SpatialCommand
    data class RequestRecalibration(val hostPlayerId: String, val playerId: String, val reason: String) : SpatialCommand
    data object ClearForArSessionClosed : SpatialCommand
}

data class SpatialSessionState(
    val hostPlayerId: String,
    val originState: SharedOriginState = SharedOriginState.Undefined,
    val originVersion: Long = 0,
    val originDefinition: SharedOriginDefinition? = null,
    val calibrationMode: SharedOriginMode = SharedOriginMode.PrintedMarkerOrigin,
    val playerStatuses: Map<String, PlayerCalibrationStatus> = emptyMap(),
    val anchors: List<SharedAnchorRecord> = emptyList(),
    val rejectedPlayers: Map<String, String> = emptyMap(),
    val recalibrationRequests: Map<String, String> = emptyMap(),
    val lastSequence: Long = 0,
    val lastError: String? = null
) {
    fun requiredPlayersReady(requiredPlayerIds: Set<String>, allowUnsupportedAsAnalyst: Boolean): Boolean =
        requiredPlayerIds.all { playerId ->
            val status = playerStatuses[playerId] ?: return@all false
            status.calibrationState == CalibrationState.Ready ||
                (allowUnsupportedAsAnalyst && status.nonArAnalyst)
        }
}

data class SpatialReducerResult(
    val state: SpatialSessionState,
    val accepted: Boolean,
    val reason: String? = null
)

object SpatialSessionReducer {
    fun reduce(state: SpatialSessionState, command: SpatialCommand): SpatialReducerResult =
        runCatching {
            when (command) {
                is SpatialCommand.StartCalibration -> {
                    requireHost(state, command.hostPlayerId)
                    state.copy(
                        originState = SharedOriginState.Searching,
                        calibrationMode = command.mode,
                        originVersion = state.originVersion + 1,
                        originDefinition = null,
                        anchors = emptyList(),
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.UpdatePlayerCalibration -> {
                    state.copy(
                        playerStatuses = state.playerStatuses + (command.status.playerId to command.status),
                        originState = command.status.calibrationState.toOriginState(state.originState),
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.AcceptCalibration -> {
                    requireHost(state, command.hostPlayerId)
                    val current = state.playerStatuses[command.playerId] ?: error("Player has not reported calibration.")
                    require(current.calibrationState == CalibrationState.Ready || current.nonArAnalyst) { "Player is not calibration-ready." }
                    state.copy(lastSequence = state.lastSequence + 1, lastError = null).ok()
                }
                is SpatialCommand.RejectCalibration -> {
                    requireHost(state, command.hostPlayerId)
                    state.copy(
                        rejectedPlayers = state.rejectedPlayers + (command.playerId to command.reason),
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.DefineOrigin -> {
                    requireHost(state, command.hostPlayerId)
                    require(command.definition.originVersion >= state.originVersion) { "Origin version is stale." }
                    SharedTransformMath.validate(command.definition.hostTransform).getOrThrow()
                    state.copy(
                        originState = SharedOriginState.Ready,
                        originVersion = command.definition.originVersion,
                        originDefinition = command.definition,
                        anchors = state.anchors.filter { it.originVersion == command.definition.originVersion },
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.UpsertAnchor -> {
                    requireHost(state, command.hostPlayerId)
                    require(command.anchor.originVersion == state.originVersion) { "Anchor origin version mismatch." }
                    SharedTransformMath.validate(command.anchor.sharedTransform).getOrThrow()
                    state.copy(
                        anchors = state.anchors.filterNot { it.anchorId == command.anchor.anchorId } + command.anchor,
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.RemoveAnchor -> {
                    requireHost(state, command.hostPlayerId)
                    state.copy(
                        anchors = state.anchors.map {
                            if (it.anchorId == command.anchorId) it.copy(active = false, lastSynchronizationEpochMs = System.currentTimeMillis()) else it
                        },
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                is SpatialCommand.RequestRecalibration -> {
                    requireHost(state, command.hostPlayerId)
                    state.copy(
                        recalibrationRequests = state.recalibrationRequests + (command.playerId to command.reason),
                        playerStatuses = state.playerStatuses + (
                            command.playerId to (state.playerStatuses[command.playerId]?.copy(calibrationState = CalibrationState.RecalibrationRequired)
                                ?: PlayerCalibrationStatus(command.playerId, command.playerId, CalibrationState.RecalibrationRequired, 0, command.reason, state.originVersion))
                            ),
                        originState = SharedOriginState.RecalibrationRequired,
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
                SpatialCommand.ClearForArSessionClosed -> {
                    state.copy(
                        originState = SharedOriginState.Undefined,
                        originDefinition = null,
                        anchors = emptyList(),
                        lastSequence = state.lastSequence + 1,
                        lastError = null
                    ).ok()
                }
            }
        }.getOrElse { state.copy(lastError = it.message).let { SpatialReducerResult(it, accepted = false, reason = it.lastError) } }

    private fun requireHost(state: SpatialSessionState, playerId: String) {
        require(playerId == state.hostPlayerId) { "Only the host can modify shared spatial state." }
    }

    private fun SpatialSessionState.ok(): SpatialReducerResult = SpatialReducerResult(this, accepted = true)

    private fun CalibrationState.toOriginState(previous: SharedOriginState): SharedOriginState =
        when (this) {
            CalibrationState.Searching -> SharedOriginState.Searching
            CalibrationState.MarkerDetected,
            CalibrationState.Stabilizing -> SharedOriginState.Stabilizing
            CalibrationState.Ready -> SharedOriginState.Ready
            CalibrationState.TrackingWeak -> SharedOriginState.Weak
            CalibrationState.MarkerLost -> SharedOriginState.Lost
            CalibrationState.RecalibrationRequired -> SharedOriginState.RecalibrationRequired
        }.let { next ->
            if (previous == SharedOriginState.Ready && next == SharedOriginState.Searching) SharedOriginState.Weak else next
        }
}
