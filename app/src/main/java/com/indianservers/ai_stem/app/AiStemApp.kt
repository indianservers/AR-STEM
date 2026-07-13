package com.indianservers.ai_stem.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.indianservers.ai_stem.core.designsystem.AiStemTheme
import com.indianservers.ai_stem.feature.arviewer.ArViewerScreen
import com.indianservers.ai_stem.feature.graphing.GraphingStudioScreen
import com.indianservers.ai_stem.feature.labs.AlgebraLaboratoryScreen
import com.indianservers.ai_stem.feature.labs.CalculusLaboratoryScreen
import com.indianservers.ai_stem.feature.labs.DataProbabilityLaboratoryScreen
import com.indianservers.ai_stem.feature.mathematics.MathematicsHomeScreen
import com.indianservers.ai_stem.feature.onboarding.WelcomeScreen
import com.indianservers.ai_stem.feature.projects.ProjectManagerScreen
import com.indianservers.ai_stem.feature.solarsystem.SolarSystemScreen
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
                    },
                    onSolarSystem = {
                        navController.navigate(AppDestination.SolarSystem.route) {
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
                    },
                    onOpenGraphingStudio = {
                        navController.navigate(AppDestination.GraphingStudio.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenProjects = {
                        navController.navigate(AppDestination.Projects.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.Projects.route) {
                ProjectManagerScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.ArViewer.route) {
                ArViewerScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.GraphingStudio.route) {
                GraphingStudioScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.SolarSystem.route) {
                SolarSystemScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAr = {
                        navController.navigate(AppDestination.ArViewer.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.AlgebraLab.route) {
                AlgebraLaboratoryScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.CalculusLab.route) {
                CalculusLaboratoryScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.DataProbabilityLab.route) {
                DataProbabilityLaboratoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
