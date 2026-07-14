package com.indianservers.ai_stem.app

sealed class AppDestination(val route: String) {
    data object Welcome : AppDestination("welcome")
    data object Subjects : AppDestination("subjects")
    data object Games : AppDestination("games")
    data object TournamentHub : AppDestination("games/tournament_hub")
    data object TeacherAuthoringStudio : AppDestination("games/teacher_authoring_studio")
    data object ArMathArena : AppDestination("games/ar_math_arena")
    data object Mathematics : AppDestination("mathematics")
    data object Projects : AppDestination("projects")
    data object ArViewer : AppDestination("ar_viewer")
    data object GraphingStudio : AppDestination("graphing_studio")
    data object SolarSystem : AppDestination("solar_system")
    data object AlgebraLab : AppDestination("algebra_lab")
    data object CalculusLab : AppDestination("calculus_lab")
    data object DataProbabilityLab : AppDestination("data_probability_lab")
}
