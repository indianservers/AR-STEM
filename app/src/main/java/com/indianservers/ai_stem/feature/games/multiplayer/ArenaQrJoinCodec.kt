package com.indianservers.ai_stem.feature.games.multiplayer

import java.net.URLDecoder
import java.net.URLEncoder

private const val QR_SCHEME = "aistem-arena://join"
private const val QR_MAX_AGE_MS = 10 * 60 * 1000L

data class ArenaQrJoinPayload(
    val protocolVersion: Int = ARENA_PROTOCOL_VERSION,
    val roomCode: String,
    val hostServiceName: String,
    val hostAddress: String?,
    val port: Int,
    val roomToken: String,
    val expiresAtEpochMs: Long
)

object ArenaQrJoinCodec {
    fun encode(payload: ArenaQrJoinPayload): String {
        require(payload.protocolVersion == ARENA_PROTOCOL_VERSION) { "Unsupported protocol version." }
        val code = ArenaTextValidator.validateRoomCode(payload.roomCode)
        require(payload.roomToken.length in 12..64) { "Invalid room token." }
        require(payload.port in 1..65535) { "Invalid host port." }
        require(payload.expiresAtEpochMs > System.currentTimeMillis()) { "Join code is expired." }
        return QR_SCHEME + "?" + listOf(
            "v" to payload.protocolVersion.toString(),
            "code" to code,
            "svc" to payload.hostServiceName,
            "host" to payload.hostAddress.orEmpty(),
            "port" to payload.port.toString(),
            "token" to payload.roomToken,
            "exp" to payload.expiresAtEpochMs.toString()
        ).joinToString("&") { (key, value) -> "${key}=${value.urlEncode()}" }
    }

    fun decode(raw: String, now: Long = System.currentTimeMillis()): Result<ArenaQrJoinPayload> = runCatching {
        require(raw.startsWith("$QR_SCHEME?")) { "Not an AR Math Arena join code." }
        val params = raw.substringAfter("?").split("&")
            .filter { it.contains("=") }
            .associate {
                val key = it.substringBefore("=")
                val value = it.substringAfter("=").urlDecode()
                key to value
            }
        val version = params.requireValue("v").toInt()
        require(version == ARENA_PROTOCOL_VERSION) { "Unsupported protocol version." }
        val expiresAt = params.requireValue("exp").toLong()
        require(expiresAt >= now) { "Join code is expired." }
        require(expiresAt - now <= QR_MAX_AGE_MS) { "Join code lifetime is too long." }
        ArenaQrJoinPayload(
            protocolVersion = version,
            roomCode = ArenaTextValidator.validateRoomCode(params.requireValue("code")),
            hostServiceName = params.requireValue("svc").take(64),
            hostAddress = params["host"]?.ifBlank { null },
            port = params.requireValue("port").toInt().also { require(it in 1..65535) { "Invalid host port." } },
            roomToken = params.requireValue("token").also { require(it.length in 12..64) { "Invalid room token." } },
            expiresAtEpochMs = expiresAt
        )
    }

    private fun Map<String, String>.requireValue(key: String): String =
        requireNotNull(this[key]) { "Missing QR field: $key" }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
    private fun String.urlDecode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())
}
