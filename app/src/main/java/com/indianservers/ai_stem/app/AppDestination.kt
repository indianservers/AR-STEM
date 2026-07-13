package com.indianservers.ai_stem.app

sealed class AppDestination(val route: String) {
    data object Welcome : AppDestination("welcome")
    data object Subjects : AppDestination("subjects")
    data object Mathematics : AppDestination("mathematics")
    data object ArViewer : AppDestination("ar_viewer")
}
