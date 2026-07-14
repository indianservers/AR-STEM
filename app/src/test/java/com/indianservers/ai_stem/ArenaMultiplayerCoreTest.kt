package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.multiplayer.ARENA_PROTOCOL_VERSION
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaCodeGenerator
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaConnectionState
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaLobbyCommand
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaLobbyReducer
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaPlayer
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaQrJoinCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaQrJoinPayload
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomSettings
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomState
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import com.indianservers.ai_stem.feature.games.multiplayer.LocalPlayerProfile
import com.indianservers.ai_stem.feature.games.multiplayer.defaultArenaTeams
import com.indianservers.ai_stem.feature.games.multiplayer.network.ArenaTcpTransport
import com.indianservers.ai_stem.feature.games.multiplayer.network.ArenaTransportEvent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaMultiplayerCoreTest {
    @Test
    fun protocolRoundTripsExplicitJoinRequest() {
        val profile = LocalPlayerProfile(playerId = "p2", displayName = "Riya", preferredRole = ArenaRole.Analyst)
        val message = GameMessage(
            roomId = "room",
            senderPlayerId = "p2",
            sequence = 7,
            payload = GamePayload.JoinRequest(profile, "ABC234", "token-123456789012", null)
        )

        val decoded = ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(message)).getOrThrow()

        assertEquals(ARENA_PROTOCOL_VERSION, decoded.protocolVersion)
        assertEquals("p2", decoded.senderPlayerId)
        assertEquals(GamePayload.JoinRequest::class, decoded.payload::class)
        assertEquals("ABC234", (decoded.payload as GamePayload.JoinRequest).roomCode)
    }

    @Test
    fun reducerRejectsInvalidTokenDuplicateMessageAndUnauthorizedHostCommand() {
        val state = hostState()
        val guest = LocalPlayerProfile(playerId = "p2", displayName = "Guest")

        val badJoin = ArenaLobbyReducer.reduce(state, ArenaLobbyCommand.Join(guest, "wrong"))
        assertFalse(badJoin.accepted)

        val unauthorized = ArenaLobbyReducer.reduce(state, ArenaLobbyCommand.LockRoom("p2", true))
        assertFalse(unauthorized.accepted)

        val message = GameMessage(
            messageId = "m1",
            roomId = "temporary-code-ok-for-first-join",
            senderPlayerId = "p2",
            sequence = 1,
            payload = GamePayload.JoinRequest(guest, state.roomCode, state.roomToken, null)
        )
        val accepted = ArenaLobbyReducer.reduceMessage(state, message)
        assertTrue(accepted.accepted)
        val duplicate = ArenaLobbyReducer.reduceMessage(accepted.state, message)
        assertFalse(duplicate.accepted)
    }

    @Test
    fun reducerTracksTeamsRolesReadinessAndReconnects() {
        val state = hostState()
        val joined = ArenaLobbyReducer.reduce(
            state,
            ArenaLobbyCommand.Join(LocalPlayerProfile(playerId = "p2", displayName = "Dev"), state.roomToken)
        ).state
        val readyHost = ArenaLobbyReducer.reduce(joined, ArenaLobbyCommand.SetReady("host", true)).state
        val readyGuest = ArenaLobbyReducer.reduce(readyHost, ArenaLobbyCommand.SetReady("p2", true)).state
        val guest = readyGuest.players.first { it.playerId == "p2" }

        assertTrue(readyGuest.canStartMatch)

        val disconnected = ArenaLobbyReducer.reduce(readyGuest, ArenaLobbyCommand.Disconnect("p2", 1000)).state
        assertEquals(ArenaConnectionState.Reconnecting, disconnected.players.first { it.playerId == "p2" }.connectionState)
        val reconnected = ArenaLobbyReducer.reduce(disconnected, ArenaLobbyCommand.Reconnect("p2", guest.reconnectToken)).state
        assertEquals(ArenaConnectionState.Connected, reconnected.players.first { it.playerId == "p2" }.connectionState)
    }

    @Test
    fun qrJoinPayloadIsStrictAndTemporary() {
        val now = System.currentTimeMillis()
        val encoded = ArenaQrJoinCodec.encode(
            ArenaQrJoinPayload(
                roomCode = "ABC234",
                hostServiceName = "AI STEM ABC234",
                hostAddress = "192.168.1.9",
                port = 42311,
                roomToken = "token-123456789012",
                expiresAtEpochMs = now + 60_000L
            )
        )

        val decoded = ArenaQrJoinCodec.decode(encoded, now = now).getOrThrow()
        assertEquals("ABC234", decoded.roomCode)
        assertEquals(42311, decoded.port)
        assertTrue(ArenaQrJoinCodec.decode(encoded.replace("v=1", "v=99")).isFailure)
    }

    @Test
    fun tcpTransportMovesProtocolMessagesOnLocalhost() = runBlocking {
        val hostTransport = ArenaTcpTransport()
        val host = hostTransport.host()
        val clientTransport = ArenaTcpTransport()
        val client = clientTransport.connect("127.0.0.1", host.port)
        val message = GameMessage(roomId = "room", senderPlayerId = "p2", sequence = 1, payload = GamePayload.Heartbeat(0))
        val inboundJob = async {
            withTimeout(2_000) {
                hostTransport.events.filterIsInstance<ArenaTransportEvent.MessageReceived>().first()
            }
        }

        client.broadcast(message)

        val inbound = inboundJob.await()
        assertEquals(GamePayload.Heartbeat::class, inbound.inbound.message.payload::class)
        host.close()
        client.close()
    }

    private fun hostState(): ArenaRoomState {
        val settings = ArenaRoomSettings(roomCode = "ABC234", maxPlayers = 4)
        val teams = defaultArenaTeams(settings.teamCount)
        return ArenaRoomState(
            roomCode = settings.roomCode,
            roomToken = ArenaCodeGenerator.token(),
            hostPlayerId = "host",
            settings = settings,
            players = listOf(
                ArenaPlayer(
                    playerId = "host",
                    displayName = "Host",
                    avatarSeed = 1,
                    teamId = teams.first().id,
                    role = ArenaRole.Commander,
                    reconnectToken = ArenaCodeGenerator.token(),
                    lastSeenEpochMs = 1
                )
            )
        )
    }
}
