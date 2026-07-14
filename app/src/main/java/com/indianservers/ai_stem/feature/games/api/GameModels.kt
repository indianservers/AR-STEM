package com.indianservers.ai_stem.feature.games.api

enum class GameIcon { Fortress, Escape, Architect, Factory, Coordinates, Expedition }
enum class GameCapability {
    AugmentedReality,
    LocalWifi,
    TeamGame,
    SinglePlayer,
    Mathematics,
    OfflineMatch,
    Outdoor,
    OpenMap,
    Diagnostics,
    SharedSpatialOrigin,
    HostAuthoritative,
    SingleDeviceHost,
    OptionalDepth,
    OptionalEnvironmentalHdr,
    MarkerInteractions,
    PuzzleSequencing,
    PlaneDetection,
    Measurement,
    ObjectManipulation,
    FloorPlane,
    SafeMovementArea,
    FutureLocationPermission,
    OfflineMaps,
    TeacherApprovedRoute
}
enum class GameAvailability { Available, Beta, ComingSoon, DeviceUnsupported, UpdateRequired, TemporarilyDisabled, RequiresSupportedDevice }
enum class GamePlayerMode { SinglePlayer, Team, OutdoorMap }
enum class GameArRequirement { Required, Optional, NotRequired }
enum class GameEnvironment { Indoor, Outdoor, IndoorOrOutdoor }
enum class GameHeroArtwork { Fortress, EscapeRoom, GeometryStudio, FactoryLine, CoordinateField, ExpeditionMap }

sealed class GameDestination(val route: String) {
    data object ArMathArena : GameDestination("games/ar_math_arena")
    data object EquationEscape : GameDestination("games/equation_escape_ar")
    data object GeometryArchitect : GameDestination("games/geometry_architect_ar")
    data object FractionFactory : GameDestination("games/fraction_factory_ar")
    data object CoordinateConquest : GameDestination("games/coordinate_conquest_ar")
    data object MathExpedition : GameDestination("games/math_expedition_ar")
    data object ComingSoon : GameDestination("games/coming_soon")
}

data class GameHowToPlay(
    val overview: String,
    val estimatedReadingMinutes: Int,
    val quickStartSteps: List<String>,
    val learningObjectives: List<String>,
    val playerModes: List<String>,
    val setupSteps: List<String>,
    val playSteps: List<String>,
    val controls: List<String>,
    val roles: List<String>,
    val scoring: List<String>,
    val winCondition: String,
    val safetyNotes: List<String>,
    val deviceRequirements: List<String>,
    val accessibilityNotes: List<String>,
    val tutorialAvailability: String
) {
    val sectionTitles: List<String>
        get() = listOf(
            "What Is This Game?",
            "Quick Start",
            "What You Will Learn",
            "Player Modes",
            "Before You Start",
            "How a Match Works",
            "Controls",
            "Team Roles",
            "Scoring",
            "How to Win",
            "Safety",
            "Device Requirements",
            "Accessibility",
            "Practice and Tutorial"
        )
}

data class GameDefinition(
    val id: String,
    val title: String,
    val tagline: String,
    val description: String,
    val icon: GameIcon,
    val heroArtwork: GameHeroArtwork,
    val playerModes: Set<GamePlayerMode>,
    val arRequirement: GameArRequirement,
    val localWifiSupported: Boolean,
    val outdoorRequired: Boolean,
    val openMapRequired: Boolean,
    val supportedTopics: List<String>,
    val recommendedGrade: String,
    val capabilities: Set<GameCapability>,
    val availability: GameAvailability,
    val destination: GameDestination,
    val recommendedPlayers: String,
    val deviceRequirements: List<String>,
    val accessibilitySupport: List<String>,
    val learningOutcomes: List<String>,
    val mainGameLoop: List<String>,
    val howToPlay: GameHowToPlay,
    val availabilityMessage: String
) {
    val playable: Boolean get() = availability == GameAvailability.Available || availability == GameAvailability.Beta
}

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
