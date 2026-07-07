package com.ktacrack.sudokueink

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Menu : Screen("menu/{mode}") {
        fun createRoute(mode: String = "none") = "menu/$mode"
    }
    object Game : Screen("game/{mode}/{difficulty}/{isZenMode}") {
        fun createRoute(mode: GameMode, difficulty: Difficulty, isZenMode: Boolean = false) =
            "game/${mode.name}/${difficulty.name}/$isZenMode"
    }
    object DailyMenu : Screen("daily_menu")  // Submenu dificultats
    object DailyGame : Screen("daily_game/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "daily_game/${difficulty.name}"
    }
    object Statistics : Screen("statistics")
    object Calibration : Screen("calibration")

}


@Composable
fun AppNavigation(onThemeChange: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Menu.createRoute("none")
    ) {
        // Pantalla de menú amb paràmetre de mode
        composable(
            route = Screen.Menu.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "none"
                }
            )
        ) { backStackEntry ->
            val currentMode = backStackEntry.arguments?.getString("mode") ?: "none"

            MainScreen(
                currentMode = if (currentMode != "none") currentMode else null,
                onModeSelected = { mode ->
                    navController.navigate(Screen.Menu.createRoute(mode))
                },
                onGameModeSelected = { mode, difficulty, isZenMode ->
                    navController.navigate(Screen.Game.createRoute(mode, difficulty, isZenMode))
                },
                onDailySudokuClick = {  // ← AFEGIT AQUÍ DINS
                    navController.navigate(Screen.DailyGame.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onCalibrationClick = { navController.navigate(Screen.Calibration.route) },
                onAchievementsClick = { navController.navigate("achievements") },
                onBackToMainMenu = {
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                },
                onThemeChange = onThemeChange
            )
        }
        // Daily Sudoku
        composable(Screen.DailyGame.route) {
            DailyGameScreen(
                onBack = {
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de joc
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("isZenMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val modeName = backStackEntry.arguments?.getString("mode")
            val mode = try {
                GameMode.valueOf(modeName ?: "NORMAL")
            } catch (e: Exception) {
                GameMode.NORMAL
            }

            val difficultyName = backStackEntry.arguments?.getString("difficulty")
            val difficulty = Difficulty.valueOf(difficultyName ?: "EASY")

            val isZenMode = backStackEntry.arguments?.getBoolean("isZenMode") ?: false

            GameScreen(
                difficulty = difficulty,
                mode = mode,
                isZenMode = isZenMode,
                onBack = {
                    navController.navigate(Screen.Menu.createRoute(modeName ?: "NORMAL")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla d'estadístiques
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = {
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla Achievements
        composable("achievements") {
            AchievementsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Pantalla de calibració d'escriptura
        composable(Screen.Calibration.route) {
            CalibrationScreen(
                onBack = {
                    navController.navigate(Screen.Menu.createRoute("none")) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
