package com.indianservers.ai_stem.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.indianservers.ai_stem.core.designsystem.AiStemTheme
import com.indianservers.ai_stem.feature.arviewer.ArViewerScreen
import com.indianservers.ai_stem.feature.games.GamesLibraryScreen
import com.indianservers.ai_stem.feature.games.GameDetailsScreen
import com.indianservers.ai_stem.feature.games.GameComingSoonScreen
import com.indianservers.ai_stem.feature.games.GameHowToPlayScreen
import com.indianservers.ai_stem.feature.games.authoring.TeacherAuthoringStudioScreen
import com.indianservers.ai_stem.feature.games.coordinateconquest.CoordinateConquestScreen
import com.indianservers.ai_stem.feature.games.equationescape.EquationEscapeScreen
import com.indianservers.ai_stem.feature.games.fractionfactory.FractionFactoryScreen
import com.indianservers.ai_stem.feature.games.geometryarchitect.GeometryArchitectScreen
import com.indianservers.ai_stem.feature.games.matharena.ArMathArenaScreen
import com.indianservers.ai_stem.feature.games.mathexpedition.MathExpeditionScreen
import com.indianservers.ai_stem.feature.games.navigation.GamesRoutes
import com.indianservers.ai_stem.feature.games.tournament.TournamentHubScreen
import com.indianservers.ai_stem.feature.graphing.GraphingStudioScreen
import com.indianservers.ai_stem.feature.geometry2d.Geometry2dWorkspaceScreen
import com.indianservers.ai_stem.feature.geometry3d.Geometry3dWorkspaceScreen
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
                    },
                    onGames = {
                        navController.navigate(AppDestination.Games.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.Games.route) {
                GamesLibraryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTournamentHub = {
                        navController.navigate(AppDestination.TournamentHub.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenTeacherAuthoringStudio = {
                        navController.navigate(AppDestination.TeacherAuthoringStudio.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGame = { game ->
                        navController.navigate(game.destination.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenDetails = { game ->
                        navController.navigate(GamesRoutes.details(game.id)) {
                            launchSingleTop = true
                        }
                    },
                    onHowToPlay = { game ->
                        navController.navigate(GamesRoutes.howToPlay(game.id)) {
                            launchSingleTop = true
                        }
                    },
                    onComingSoon = { game ->
                        navController.navigate(GamesRoutes.comingSoon(game.id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(GamesRoutes.Details) { backStackEntry ->
                GameDetailsScreen(
                    gameId = backStackEntry.arguments?.getString("gameId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onPlay = { game ->
                        navController.navigate(game.destination.route) {
                            launchSingleTop = true
                        }
                    },
                    onHowToPlay = { game ->
                        navController.navigate(GamesRoutes.howToPlay(game.id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(GamesRoutes.HowToPlay) { backStackEntry ->
                GameHowToPlayScreen(
                    gameId = backStackEntry.arguments?.getString("gameId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onDetails = { game ->
                        navController.navigate(GamesRoutes.details(game.id)) {
                            launchSingleTop = true
                        }
                    },
                    onPlay = { game ->
                        navController.navigate(game.destination.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(GamesRoutes.ComingSoon) { backStackEntry ->
                GameComingSoonScreen(
                    gameId = backStackEntry.arguments?.getString("gameId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onHowToPlay = { game ->
                        navController.navigate(GamesRoutes.howToPlay(game.id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.ArMathArena.route) {
                ArMathArenaScreen(
                    onBack = { navController.popBackStack() },
                    onHowToPlay = {
                        navController.navigate(GamesRoutes.howToPlay("ar-math-arena")) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.TournamentHub.route) {
                TournamentHubScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.TeacherAuthoringStudio.route) {
                TeacherAuthoringStudioScreen(onBack = { navController.popBackStack() })
            }
            composable(GamesRoutes.EquationEscape) {
                EquationEscapeScreen(onBack = { navController.popBackStack() })
            }
            composable(GamesRoutes.GeometryArchitect) {
                GeometryArchitectScreen(onBack = { navController.popBackStack() })
            }
            composable(GamesRoutes.FractionFactory) {
                FractionFactoryScreen(onBack = { navController.popBackStack() })
            }
            composable(GamesRoutes.CoordinateConquest) {
                CoordinateConquestScreen(onBack = { navController.popBackStack() })
            }
            composable(GamesRoutes.MathExpedition) {
                MathExpeditionScreen(onBack = { navController.popBackStack() })
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
                    onOpenGeometry2D = {
                        navController.navigate(AppDestination.Geometry2DWorkspace.route) { launchSingleTop = true }
                    },
                    onOpenGeometry3D = {
                        navController.navigate(AppDestination.Geometry3DWorkspace.route) { launchSingleTop = true }
                    },
                    onOpenAlgebra = {
                        navController.navigate(AppDestination.AlgebraLab.route) { launchSingleTop = true }
                    },
                    onOpenCalculus = {
                        navController.navigate(AppDestination.CalculusLab.route) { launchSingleTop = true }
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
            composable(AppDestination.Geometry2DWorkspace.route) {
                Geometry2dWorkspaceScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Geometry3DWorkspace.route) {
                Geometry3dWorkspaceScreen(onBack = { navController.popBackStack() })
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
