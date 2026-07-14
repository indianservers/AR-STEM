package com.indianservers.ai_stem.feature.games.multiplayer

class ArenaRateLimiter(
    private val windowMs: Long,
    private val maxEvents: Int
) {
    private val eventsByKey = mutableMapOf<String, ArrayDeque<Long>>()

    fun allow(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val queue = eventsByKey.getOrPut(key) { ArrayDeque() }
        while (queue.isNotEmpty() && now - queue.first() > windowMs) queue.removeFirst()
        if (queue.size >= maxEvents) return false
        queue.addLast(now)
        return true
    }
}

object ArenaTextValidator {
    private val displayNameRegex = Regex("[A-Za-z0-9 _.-]{1,24}")
    private val roomNameRegex = Regex("[A-Za-z0-9 _.-]{1,36}")

    fun cleanDisplayName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ").take(24)
        require(clean.isNotBlank()) { "Display name is required." }
        require(displayNameRegex.matches(clean)) { "Use letters, numbers, spaces, dots, dashes or underscores." }
        return clean
    }

    fun cleanRoomName(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ").take(36)
        return if (clean.isNotBlank() && roomNameRegex.matches(clean)) clean else "AR Math Arena"
    }

    fun validateRoomCode(raw: String): String {
        val code = raw.trim().uppercase()
        require(Regex("[A-Z2-9]{6}").matches(code)) { "Room code must be six safe characters." }
        return code
    }
}

fun ArenaRoomState.validateJoin(profile: LocalPlayerProfile, token: String, now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
    require(protocolVersion == ARENA_PROTOCOL_VERSION) { "Unsupported protocol version." }
    require(token == roomToken) { "Invalid room token." }
    require(!locked) { "Room is locked." }
    require(players.size < settings.maxPlayers || players.any { it.playerId == profile.playerId }) { "Room is full." }
    val cleanName = ArenaTextValidator.cleanDisplayName(profile.displayName)
    val duplicateName = players.any { it.playerId != profile.playerId && it.displayName.equals(cleanName, ignoreCase = true) }
    require(!duplicateName) { "That display name is already used in this room." }
    require(now > 0L) { "Invalid join time." }
}

fun ArenaRoomState.rejectInvalidMessage(message: GameMessage): String? = when {
    message.protocolVersion != ARENA_PROTOCOL_VERSION -> "Unsupported protocol version."
    message.payload is GamePayload.JoinRequest && message.payload.roomCode != roomCode -> "Invalid room code."
    message.payload !is GamePayload.JoinRequest && message.roomId != roomId -> "Invalid room ID."
    message.messageId in seenMessageIds -> "Duplicate message."
    message.sequence < 0 -> "Invalid sequence."
    message.payload is GamePayload.JoinRequest && message.payload.roomToken != roomToken -> "Invalid room token."
    else -> null
}
