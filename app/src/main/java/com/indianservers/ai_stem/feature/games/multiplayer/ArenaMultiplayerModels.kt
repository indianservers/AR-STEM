package com.indianservers.ai_stem.feature.games.multiplayer

import java.util.UUID

const val ARENA_PROTOCOL_VERSION = 1
const val MAX_ARENA_MESSAGE_BYTES = 16_384

enum class ArenaGradeLevel { Grade6, Grade7, Grade8, Grade9, Grade10, Mixed }
enum class ArenaTopic { Arithmetic, Fractions, Algebra, Geometry, Graphing, Measurement }
enum class ArenaDifficulty { Easy, Normal, Hard, Adaptive }
enum class ArenaMatchMode { Cooperative, Competitive }
enum class ArenaTeamAssignmentMode { Manual, Balanced, Random }
enum class ArenaConnectionState { Connected, Reconnecting, Disconnected, Removed }
enum class ArenaRole { Navigator, Solver, Builder, Analyst, Commander }

data class LocalPlayerProfile(
    val playerId: String = UUID.randomUUID().toString(),
    val displayName: String = "Player",
    val avatarSeed: Int = displayName.hashCode(),
    val preferredRole: ArenaRole = ArenaRole.Solver,
    val lastTeamColor: String = "Red",
    val highContrast: Boolean = false,
    val largeText: Boolean = false
)

data class ArenaTeam(
    val id: String,
    val name: String,
    val colorName: String
)

data class ArenaPlayer(
    val playerId: String,
    val displayName: String,
    val avatarSeed: Int,
    val teamId: String? = null,
    val role: ArenaRole? = null,
    val ready: Boolean = false,
    val connectionState: ArenaConnectionState = ArenaConnectionState.Connected,
    val reconnectToken: String,
    val lastSeenEpochMs: Long,
    val lastAcknowledgedSequence: Long = 0
)

data class ArenaRoomSettings(
    val roomName: String = "AR Math Arena",
    val roomCode: String = ArenaCodeGenerator.roomCode(),
    val gradeLevel: ArenaGradeLevel = ArenaGradeLevel.Mixed,
    val topics: Set<ArenaTopic> = setOf(ArenaTopic.Algebra, ArenaTopic.Geometry, ArenaTopic.Measurement),
    val difficulty: ArenaDifficulty = ArenaDifficulty.Normal,
    val teamCount: Int = 2,
    val maxPlayers: Int = 6,
    val matchDurationMinutes: Int = 12,
    val matchMode: ArenaMatchMode = ArenaMatchMode.Cooperative,
    val roleRotation: Boolean = true,
    val hintsEnabled: Boolean = true,
    val teacherControlsEnabled: Boolean = true,
    val spectatorsEnabled: Boolean = false,
    val sharedMarkerMode: Boolean = true,
    val surfacePlacementFallback: Boolean = true,
    val questionTimeLimitSeconds: Int = 45,
    val adaptiveDifficulty: Boolean = true,
    val friendlyAttackEffects: Boolean = false
) {
    fun sanitized(): ArenaRoomSettings = copy(
        roomName = ArenaTextValidator.cleanRoomName(roomName),
        teamCount = teamCount.coerceIn(1, 4),
        maxPlayers = maxPlayers.coerceIn(2, 12),
        matchDurationMinutes = matchDurationMinutes.coerceIn(3, 45),
        questionTimeLimitSeconds = questionTimeLimitSeconds.coerceIn(10, 180)
    )
}

data class ArenaRoomState(
    val roomId: String = UUID.randomUUID().toString(),
    val protocolVersion: Int = ARENA_PROTOCOL_VERSION,
    val roomCode: String,
    val roomToken: String,
    val hostPlayerId: String,
    val hostSequence: Long = 0,
    val matchSeed: Long = System.currentTimeMillis(),
    val roomVersion: Long = 0,
    val settings: ArenaRoomSettings,
    val teams: List<ArenaTeam> = defaultArenaTeams(settings.teamCount),
    val players: List<ArenaPlayer> = emptyList(),
    val locked: Boolean = false,
    val matchStarting: Boolean = false,
    val hostShutdown: Boolean = false,
    val seenMessageIds: Set<String> = emptySet(),
    val lastError: String? = null
) {
    val canStartMatch: Boolean
        get() = !locked &&
            players.size >= 2 &&
            players.count { it.connectionState == ArenaConnectionState.Connected } >= 2 &&
            players.all { it.ready && it.teamId != null && it.role != null }
}

object ArenaCodeGenerator {
    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun roomCode(): String = (1..6).map { chars.random() }.joinToString("")
    fun token(): String = UUID.randomUUID().toString().replace("-", "").take(24)
}

fun defaultArenaTeams(count: Int): List<ArenaTeam> =
    listOf(
        ArenaTeam("red", "Red Dragons", "Red"),
        ArenaTeam("blue", "Blue Titans", "Blue"),
        ArenaTeam("green", "Green Wizards", "Green"),
        ArenaTeam("gold", "Gold Guardians", "Gold")
    ).take(count.coerceIn(1, 4))
