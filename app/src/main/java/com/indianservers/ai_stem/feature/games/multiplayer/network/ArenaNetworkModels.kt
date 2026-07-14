package com.indianservers.ai_stem.feature.games.multiplayer.network

import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage

const val ARENA_NSD_SERVICE_TYPE = "_aistem-arena._tcp."

data class DiscoveredArenaRoom(
    val serviceName: String,
    val roomCode: String,
    val roomName: String,
    val hostAddress: String?,
    val port: Int,
    val lastSeenEpochMs: Long = System.currentTimeMillis()
)

data class LocalNetworkDiagnostics(
    val wifiOrLanActive: Boolean,
    val metered: Boolean,
    val internetPermissionUseful: Boolean,
    val multicastLockRecommended: Boolean,
    val activeAddresses: List<String>,
    val message: String
)

data class ArenaInboundMessage(
    val connectionId: String,
    val message: GameMessage
)

sealed interface ArenaTransportEvent {
    data class ClientConnected(val connectionId: String) : ArenaTransportEvent
    data class ClientDisconnected(val connectionId: String, val reason: String?) : ArenaTransportEvent
    data class MessageReceived(val inbound: ArenaInboundMessage) : ArenaTransportEvent
    data class TransportError(val reason: String) : ArenaTransportEvent
}

interface ArenaNetworkHandle {
    val port: Int
    suspend fun send(connectionId: String, message: GameMessage)
    suspend fun broadcast(message: GameMessage)
    fun close()
}
