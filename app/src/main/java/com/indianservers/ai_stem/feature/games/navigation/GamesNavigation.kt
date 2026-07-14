package com.indianservers.ai_stem.feature.games.navigation

object GamesRoutes {
    const val Library = "games"
    const val ArMathArena = "games/ar_math_arena"
    const val EquationEscape = "games/equation_escape_ar"
    const val GeometryArchitect = "games/geometry_architect_ar"
    const val FractionFactory = "games/fraction_factory_ar"
    const val CoordinateConquest = "games/coordinate_conquest_ar"
    const val MathExpedition = "games/math_expedition_ar"
    const val Details = "games/details/{gameId}"
    const val HowToPlay = "games/how_to_play/{gameId}"
    const val ComingSoon = "games/coming_soon/{gameId}"

    fun details(gameId: String): String = "games/details/$gameId"
    fun howToPlay(gameId: String): String = "games/how_to_play/$gameId"
    fun comingSoon(gameId: String): String = "games/coming_soon/$gameId"
}
