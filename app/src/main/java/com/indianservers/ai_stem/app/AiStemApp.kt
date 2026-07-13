package com.indianservers.ai_stem.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.indianservers.ai_stem.core.designsystem.AiStemTheme
import com.indianservers.ai_stem.feature.arviewer.ArViewerScreen
import com.indianservers.ai_stem.feature.mathematics.MathematicsHomeScreen
import com.indianservers.ai_stem.feature.onboarding.WelcomeScreen
import com.indianservers.ai_stem.feature.subjects.SubjectSelectionScreen

@Composable
fun AiStemApp() {
    AiStemTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = AppDestination.Welcome.route
        ) {
            composable(AppDestination.Welcome.route) {
                WelcomeScreen(onStart = {
                    navController.navigate(AppDestination.Subjects.route) {
                        launchSingleTop = true
                    }
                })
            }
            composable(AppDestination.Subjects.route) {
                SubjectSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onMathematics = {
                        navController.navigate(AppDestination.Mathematics.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.Mathematics.route) {
                MathematicsHomeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAr = {
                        navController.navigate(AppDestination.ArViewer.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.ArViewer.route) {
                ArViewerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
