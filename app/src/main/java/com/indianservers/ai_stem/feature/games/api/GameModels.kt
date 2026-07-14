package com.indianservers.ai_stem.feature.games.api

enum class GameIcon { Arena, Hunt, Factory, Coordinates, Escape, Treasure }
enum class GameCapability { AugmentedReality, LocalWifi, TeamGame, Mathematics, OfflineMatch, Diagnostics, SharedSpatialOrigin, HostAuthoritative }
enum class GameAvailability { Available, ComingSoon, RequiresSupportedDevice }

sealed class GameDestination(val route: String) {
    data object ArMathArena : GameDestination("games/ar_math_arena")
}

data class GameDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: GameIcon,
    val capabilities: Set<GameCapability>,
    val availability: GameAvailability,
    val destination: GameDestination,
    val recommendedPlayers: String
)

data class GameRuntimeContract(
    val gameId: String,
    val protocolNamespace: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val requiresSharedOrigin: Boolean,
    val hostAuthoritative: Boolean,
    val supportsNonArParticipant: Boolean,
    val privacyNotes: List<String>
)

interface GamePlugin {
    val definition: GameDefinition
    val runtimeContract: GameRuntimeContract
}

class GameNavigationRegistry(plugins: List<GamePlugin>) {
    val plugins: List<GamePlugin> = plugins
    val games: List<GameDefinition> = this.plugins.map { it.definition }

    init {
        require(this.plugins.map { it.definition.id }.distinct().size == this.plugins.size) {
            "Game plugin IDs must be unique."
        }
        require(this.plugins.all { it.runtimeContract.gameId == it.definition.id }) {
            "Every runtime contract must match its game definition."
        }
        require(this.plugins.all { it.runtimeContract.minPlayers in 1..it.runtimeContract.maxPlayers }) {
            "Game player ranges must be valid."
        }
    }

    fun playableGames(): List<GameDefinition> = games.filter { it.availability == GameAvailability.Available }
    fun byId(gameId: String): GamePlugin? = plugins.firstOrNull { it.definition.id == gameId }
}
