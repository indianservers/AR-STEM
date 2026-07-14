package com.indianservers.ai_stem.feature.games.multiplayer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class LocalNetworkInspector {
    fun inspect(context: Context): LocalNetworkDiagnostics {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = manager?.activeNetwork
        val capabilities = network?.let(manager::getNetworkCapabilities)
        val addresses = network?.let(manager::getLinkProperties)?.linkAddresses
            ?.mapNotNull { it.address?.hostAddress }
            ?.filterNot { it.startsWith("127.") || it == "::1" }
            .orEmpty()
        val localTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val metered = manager?.isActiveNetworkMetered ?: true
        val message = when {
            localTransport && addresses.isNotEmpty() -> "Local Wi-Fi/LAN ready. Devices must stay on the same network."
            localTransport -> "Local network detected, waiting for an address."
            else -> "Connect to the same Wi-Fi/LAN before hosting or joining."
        }
        return LocalNetworkDiagnostics(
            wifiOrLanActive = localTransport,
            metered = metered,
            internetPermissionUseful = true,
            multicastLockRecommended = true,
            activeAddresses = addresses,
            message = message
        )
    }
}
