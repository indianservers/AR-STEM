package com.indianservers.ai_stem.feature.games.multiplayer.network

import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArenaTcpTransport(
    private val externalScope: CoroutineScope? = null
) {
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventsMutable = MutableSharedFlow<ArenaTransportEvent>(
        replay = 16,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ArenaTransportEvent> = eventsMutable

    fun host(port: Int = 0): ArenaNetworkHandle {
        val server = ServerSocket(port)
        val connections = ConcurrentHashMap<String, Socket>()
        val acceptJob = scope.launch {
            while (isActive && !server.isClosed) {
                runCatching {
                    val socket = server.accept()
                    val id = UUID.randomUUID().toString()
                    connections[id] = socket
                    eventsMutable.emit(ArenaTransportEvent.ClientConnected(id))
                    readLoop(id, socket, connections)
                }.onFailure {
                    if (!server.isClosed) eventsMutable.emit(ArenaTransportEvent.TransportError(it.message ?: "Accept failed"))
                }
            }
        }
        return object : ArenaNetworkHandle {
            override val port: Int = server.localPort
            override suspend fun send(connectionId: String, message: GameMessage) {
                connections[connectionId]?.writeMessage(message)
            }
            override suspend fun broadcast(message: GameMessage) {
                connections.values.forEach { it.writeMessage(message) }
            }
            override fun close() {
                acceptJob.cancel()
                runCatching { server.close() }
                connections.values.forEach { runCatching { it.close() } }
                connections.clear()
            }
        }
    }

    fun connect(host: String, port: Int): ArenaNetworkHandle {
        val socket = Socket(host, port)
        val id = "host"
        val connections = ConcurrentHashMap<String, Socket>().apply { put(id, socket) }
        val job = scope.launch { readLoop(id, socket, connections) }
        return object : ArenaNetworkHandle {
            override val port: Int = port
            override suspend fun send(connectionId: String, message: GameMessage) {
                socket.writeMessage(message)
            }
            override suspend fun broadcast(message: GameMessage) {
                socket.writeMessage(message)
            }
            override fun close() {
                job.cancel()
                runCatching { socket.close() }
                connections.clear()
            }
        }
    }

    private fun readLoop(connectionId: String, socket: Socket, connections: ConcurrentHashMap<String, Socket>) {
        scope.launch {
            runCatching {
                BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)).useLines { lines ->
                    lines.forEach { raw ->
                        ArenaProtocolCodec.decode(raw)
                            .onSuccess { eventsMutable.tryEmit(ArenaTransportEvent.MessageReceived(ArenaInboundMessage(connectionId, it))) }
                            .onFailure { eventsMutable.tryEmit(ArenaTransportEvent.TransportError(it.message ?: "Decode failed")) }
                    }
                }
            }.onFailure {
                eventsMutable.tryEmit(ArenaTransportEvent.ClientDisconnected(connectionId, it.message))
            }
            connections.remove(connectionId)
            runCatching { socket.close() }
        }
    }

    private suspend fun Socket.writeMessage(message: GameMessage): Unit = withContext(Dispatchers.IO) {
        BufferedWriter(OutputStreamWriter(getOutputStream(), Charsets.UTF_8)).apply {
            write(ArenaProtocolCodec.encode(message))
            newLine()
            flush()
        }
    }
}
