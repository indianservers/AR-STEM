package com.indianservers.ai_stem.feature.games.multiplayer

sealed interface ArenaLobbyCommand {
    data class Join(val profile: LocalPlayerProfile, val token: String) : ArenaLobbyCommand
    data class SetReady(val playerId: String, val ready: Boolean) : ArenaLobbyCommand
    data class AssignTeam(val hostPlayerId: String, val playerId: String, val teamId: String) : ArenaLobbyCommand
    data class AssignRole(val hostPlayerId: String, val playerId: String, val role: ArenaRole) : ArenaLobbyCommand
    data class AutoAssign(val hostPlayerId: String, val mode: ArenaTeamAssignmentMode) : ArenaLobbyCommand
    data class LockRoom(val hostPlayerId: String, val locked: Boolean) : ArenaLobbyCommand
    data class Disconnect(val playerId: String, val graceUntilEpochMs: Long) : ArenaLobbyCommand
    data class Reconnect(val playerId: String, val reconnectToken: String) : ArenaLobbyCommand
    data class RemovePlayer(val hostPlayerId: String, val playerId: String) : ArenaLobbyCommand
    data class StartMatch(val hostPlayerId: String) : ArenaLobbyCommand
    data object HostShutdown : ArenaLobbyCommand
}

data class ArenaLobbyResult(
    val state: ArenaRoomState,
    val accepted: Boolean,
    val reason: String? = null
)

object ArenaLobbyReducer {
    fun reduce(state: ArenaRoomState, command: ArenaLobbyCommand, now: Long = System.currentTimeMillis()): ArenaLobbyResult =
        runCatching {
            when (command) {
                is ArenaLobbyCommand.Join -> join(state, command.profile, command.token, now)
                is ArenaLobbyCommand.SetReady -> mutatePlayer(state, command.playerId) { it.copy(ready = command.ready) }
                is ArenaLobbyCommand.AssignTeam -> {
                    requireHost(state, command.hostPlayerId)
                    require(state.teams.any { it.id == command.teamId }) { "Unknown team." }
                    mutatePlayer(state, command.playerId) { it.copy(teamId = command.teamId) }
                }
                is ArenaLobbyCommand.AssignRole -> {
                    requireHost(state, command.hostPlayerId)
                    assignRole(state, command.playerId, command.role)
                }
                is ArenaLobbyCommand.AutoAssign -> {
                    requireHost(state, command.hostPlayerId)
                    autoAssign(state, command.mode)
                }
                is ArenaLobbyCommand.LockRoom -> {
                    requireHost(state, command.hostPlayerId)
                    state.bump().copy(locked = command.locked).ok()
                }
                is ArenaLobbyCommand.Disconnect -> mutatePlayer(state, command.playerId) {
                    it.copy(connectionState = ArenaConnectionState.Reconnecting, lastSeenEpochMs = now)
                }
                is ArenaLobbyCommand.Reconnect -> reconnect(state, command.playerId, command.reconnectToken, now)
                is ArenaLobbyCommand.RemovePlayer -> {
                    requireHost(state, command.hostPlayerId)
                    mutatePlayer(state, command.playerId) { it.copy(connectionState = ArenaConnectionState.Removed, ready = false) }
                }
                is ArenaLobbyCommand.StartMatch -> {
                    requireHost(state, command.hostPlayerId)
                    require(state.canStartMatch) { "Minimum lobby requirements are not satisfied." }
                    state.bump().copy(matchStarting = true, locked = true).ok()
                }
                ArenaLobbyCommand.HostShutdown -> state.bump().copy(hostShutdown = true, locked = true).ok()
            }
        }.getOrElse { state.copy(lastError = it.message).let { ArenaLobbyResult(it, accepted = false, reason = it.lastError) } }

    fun reduceMessage(state: ArenaRoomState, message: GameMessage): ArenaLobbyResult {
        state.rejectInvalidMessage(message)?.let { return ArenaLobbyResult(state.copy(lastError = it), false, it) }
        val next = state.copy(seenMessageIds = (state.seenMessageIds + message.messageId).takeLastSet(256))
        return when (val payload = message.payload) {
            is GamePayload.JoinRequest -> reduce(next, ArenaLobbyCommand.Join(payload.profile, payload.roomToken))
            is GamePayload.ReadyChanged -> reduce(next, ArenaLobbyCommand.SetReady(payload.playerId, payload.ready))
            is GamePayload.TeamAssignmentRequested -> {
                val teamId = payload.teamId ?: next.teams.minBy { team -> next.players.count { it.teamId == team.id } }.id
                reduce(next, ArenaLobbyCommand.AssignTeam(next.hostPlayerId, payload.playerId, teamId))
            }
            is GamePayload.RoleSelected -> reduce(next, ArenaLobbyCommand.AssignRole(next.hostPlayerId, payload.playerId, payload.role))
            is GamePayload.ReconnectRequest -> reduce(next, ArenaLobbyCommand.Reconnect(payload.playerId, payload.reconnectToken))
            else -> ArenaLobbyResult(next, true)
        }
    }

    private fun join(state: ArenaRoomState, profile: LocalPlayerProfile, token: String, now: Long): ArenaLobbyResult {
        state.validateJoin(profile, token, now).getOrThrow()
        val existing = state.players.firstOrNull { it.playerId == profile.playerId }
        val updatedPlayers = if (existing != null) {
            state.players.map {
                if (it.playerId == profile.playerId) {
                    it.copy(
                        displayName = ArenaTextValidator.cleanDisplayName(profile.displayName),
                        connectionState = ArenaConnectionState.Connected,
                        lastSeenEpochMs = now
                    )
                } else {
                    it
                }
            }
        } else {
            val team = state.teams.minBy { team -> state.players.count { it.teamId == team.id } }
            state.players + ArenaPlayer(
                playerId = profile.playerId,
                displayName = ArenaTextValidator.cleanDisplayName(profile.displayName),
                avatarSeed = profile.avatarSeed,
                teamId = team.id,
                role = firstAvailableRole(state.players, team.id) ?: profile.preferredRole,
                reconnectToken = ArenaCodeGenerator.token(),
                lastSeenEpochMs = now
            )
        }
        return state.bump().copy(players = updatedPlayers).ok()
    }

    private fun reconnect(state: ArenaRoomState, playerId: String, reconnectToken: String, now: Long): ArenaLobbyResult =
        mutatePlayer(state, playerId) {
            require(it.reconnectToken == reconnectToken) { "Invalid reconnection token." }
            it.copy(connectionState = ArenaConnectionState.Connected, lastSeenEpochMs = now)
        }

    private fun assignRole(state: ArenaRoomState, playerId: String, role: ArenaRole): ArenaLobbyResult {
        val player = state.players.firstOrNull { it.playerId == playerId } ?: error("Unknown player.")
        val sameTeam = state.players.filter { it.teamId == player.teamId && it.playerId != playerId && it.connectionState != ArenaConnectionState.Removed }
        val roleTaken = sameTeam.any { it.role == role }
        require(!roleTaken || sameTeam.size + 1 > ArenaRole.entries.size) { "Role is already occupied on this team." }
        return mutatePlayer(state, playerId) { it.copy(role = role) }
    }

    private fun autoAssign(state: ArenaRoomState, mode: ArenaTeamAssignmentMode): ArenaLobbyResult {
        val teams = state.teams
        val players = when (mode) {
            ArenaTeamAssignmentMode.Manual -> state.players
            ArenaTeamAssignmentMode.Balanced -> state.players.mapIndexed { index, player ->
                player.copy(teamId = teams[index % teams.size].id, role = ArenaRole.entries[index % ArenaRole.entries.size])
            }
            ArenaTeamAssignmentMode.Random -> state.players.shuffled().mapIndexed { index, player ->
                player.copy(teamId = teams[index % teams.size].id, role = ArenaRole.entries[index % ArenaRole.entries.size])
            }
        }
        return state.bump().copy(players = players).ok()
    }

    private fun mutatePlayer(state: ArenaRoomState, playerId: String, block: (ArenaPlayer) -> ArenaPlayer): ArenaLobbyResult {
        require(state.players.any { it.playerId == playerId }) { "Unknown player." }
        return state.bump().copy(players = state.players.map { if (it.playerId == playerId) block(it) else it }).ok()
    }

    private fun firstAvailableRole(players: List<ArenaPlayer>, teamId: String): ArenaRole? {
        val used = players.filter { it.teamId == teamId }.mapNotNull { it.role }.toSet()
        return ArenaRole.entries.firstOrNull { it !in used }
    }

    private fun requireHost(state: ArenaRoomState, playerId: String) {
        require(playerId == state.hostPlayerId) { "Only the host can do this." }
    }

    private fun ArenaRoomState.bump(): ArenaRoomState = copy(hostSequence = hostSequence + 1, roomVersion = roomVersion + 1)
    private fun ArenaRoomState.ok(): ArenaLobbyResult = ArenaLobbyResult(this, accepted = true)
    private fun Set<String>.takeLastSet(max: Int): Set<String> = if (size <= max) this else drop(size - max).toSet()
}
