package com.indianservers.ai_stem.feature.games.catalog

import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.api.GameDestination
import com.indianservers.ai_stem.feature.games.api.GameIcon
import com.indianservers.ai_stem.feature.games.api.GameNavigationRegistry
import com.indianservers.ai_stem.feature.games.api.GamePlugin
import com.indianservers.ai_stem.feature.games.api.GameRuntimeContract

object GamesCatalog {
    val registeredPlugins: GameNavigationRegistry = GameNavigationRegistry(
        listOf(
            ArMathArenaPlugin,
            FutureGamePlugin("ar-geometry-hunt", "AR Geometry Hunt", "Find shapes in real space.", GameIcon.Hunt),
            FutureGamePlugin("fraction-factory", "Fraction Factory", "Build fractions under time pressure.", GameIcon.Factory),
            FutureGamePlugin("coordinate-capture", "Coordinate Capture", "Claim points on a shared grid.", GameIcon.Coordinates),
            FutureGamePlugin("equation-escape", "Equation Escape", "Solve equations to unlock rooms.", GameIcon.Escape),
            FutureGamePlugin("math-treasure-hunt", "Math Treasure Hunt", "Follow clues through number puzzles.", GameIcon.Treasure)
        )
    )

    val games: List<GameDefinition> = registeredPlugins.games

    val firstPlayableGame: GameDefinition get() = registeredPlugins.playableGames().first()

    private data object ArMathArenaPlugin : GamePlugin {
        override val definition: GameDefinition = GameDefinition(
            id = "ar-math-arena",
            title = "AR Math Arena",
            subtitle = "Move. Solve. Build. Win Together.",
            description = "A local Wi-Fi team mathematics game where students explore, solve, build and defend a shared augmented-reality base.",
            icon = GameIcon.Arena,
            capabilities = setOf(
                GameCapability.AugmentedReality,
                GameCapability.LocalWifi,
                GameCapability.TeamGame,
                GameCapability.Mathematics,
                GameCapability.OfflineMatch,
                GameCapability.Diagnostics,
                GameCapability.SharedSpatialOrigin,
                GameCapability.HostAuthoritative
            ),
            availability = GameAvailability.Available,
            destination = GameDestination.ArMathArena,
            recommendedPlayers = "2-6 local players"
        )

        override val runtimeContract: GameRuntimeContract = GameRuntimeContract(
            gameId = definition.id,
            protocolNamespace = "arena.v1.math",
            minPlayers = 2,
            maxPlayers = 6,
            requiresSharedOrigin = true,
            hostAuthoritative = true,
            supportsNonArParticipant = true,
            privacyNotes = listOf(
                "Uses local Wi-Fi sockets only.",
                "Camera frames stay on device.",
                "Room token is temporary and scoped to the LAN match."
            )
        )
    }

    private data class FutureGamePlugin(
        val id: String,
        val title: String,
        val description: String,
        val icon: GameIcon
    ) : GamePlugin {
        override val definition: GameDefinition = GameDefinition(
            id = id,
            title = title,
            subtitle = "Coming in a future game phase.",
            description = description,
            icon = icon,
            capabilities = setOf(GameCapability.Mathematics),
            availability = GameAvailability.ComingSoon,
            destination = GameDestination.ArMathArena,
            recommendedPlayers = "Solo or team"
        )

        override val runtimeContract: GameRuntimeContract = GameRuntimeContract(
            gameId = id,
            protocolNamespace = "future.${id.replace("-", ".")}",
            minPlayers = 1,
            maxPlayers = 6,
            requiresSharedOrigin = false,
            hostAuthoritative = false,
            supportsNonArParticipant = true,
            privacyNotes = listOf("Future plugin slot only; no runtime transport is active.")
        )
    }
}
