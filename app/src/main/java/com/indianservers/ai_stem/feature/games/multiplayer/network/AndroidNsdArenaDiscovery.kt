package com.indianservers.ai_stem.feature.games.multiplayer.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidNsdArenaDiscovery(private val context: Context) {
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    fun registerRoom(settings: ArenaRoomSettings, port: Int, onRegistered: (String) -> Unit, onError: (String) -> Unit): NsdManager.RegistrationListener? {
        val manager = nsdManager ?: return null.also { onError("NSD is unavailable on this device.") }
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "AI STEM ${settings.roomCode}"
            serviceType = ARENA_NSD_SERVICE_TYPE
            this.port = port
            setAttribute("roomCode", settings.roomCode)
            setAttribute("roomName", settings.roomName.take(36))
            setAttribute("protocol", "1")
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = onRegistered(info.serviceName)
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = onError("NSD registration failed: $errorCode")
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = onError("NSD unregister failed: $errorCode")
        }
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        return listener
    }

    fun unregister(listener: NsdManager.RegistrationListener?) {
        if (listener != null) runCatching { nsdManager?.unregisterService(listener) }
    }

    fun discoverRooms(): Flow<DiscoveredArenaRoom> = callbackFlow {
        val manager = nsdManager
        if (manager == null) {
            close(IllegalStateException("NSD is unavailable on this device."))
            return@callbackFlow
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(IllegalStateException("NSD discovery failed: $errorCode"))
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != ARENA_NSD_SERVICE_TYPE) return
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val roomCode = info.attributes["roomCode"]?.decodeToString()
                            ?: info.serviceName.takeLast(6)
                        val roomName = info.attributes["roomName"]?.decodeToString() ?: "AR Math Arena"
                        trySend(
                            DiscoveredArenaRoom(
                                serviceName = info.serviceName,
                                roomCode = roomCode,
                                roomName = roomName,
                                hostAddress = info.host?.hostAddress,
                                port = info.port
                            )
                        )
                    }
                })
            }
        }
        manager.discoverServices(ARENA_NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { manager.stopServiceDiscovery(listener) } }
    }
}
