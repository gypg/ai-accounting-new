package com.example.aiaccounting.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aiaccounting.ui.screens.*

/**
 * App Navigation
 */
sealed class Screen(val route: String) {
    object SetupPin : Screen("setup_pin")
    object Login : Screen("login")
    object InitialSetup : Screen("initial_setup")
    object Home : Screen("home")
    object AddTransaction : Screen("add_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object AIAssistant : Screen("ai_assistant")
    object Statistics : Screen("statistics")
    object Accounts : Screen("accounts")
    object Settings : Screen("settings")
    object Categories : Screen("categories")
    object Budgets : Screen("budgets")
    object Export : Screen("export")
}

/**
 * App Navigation Host
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    onSetupComplete: (String) -> Unit = {},
    onLoginSuccess: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // PIN Setup
        composable(Screen.SetupPin.route) {
            SetupPinScreen(
                onSetupComplete = { pin ->
                    onSetupComplete(pin)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetupPin.route) { inclusive = true }
                    }
                }
            )
        }

        // Login
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { pin ->
                    onLoginSuccess(pin)
                    navController.navigate(Screen.InitialSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Initial Setup
        composable(Screen.InitialSetup.route) {
            InitialSetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.InitialSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onNavigateToAI = {
                    navController.navigate(Screen.AIAssistant.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Add Transaction
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSave = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Transaction
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
            EditTransactionScreen(
                transactionId = transactionId,
                onBack = {
                    navController.popBackStack()
                },
                onSave = {
                    navController.popBackStack()
                },
                onDelete = {
                    navController.popBackStack()
                }
            )
        }

        // AI Assistant
        composable(Screen.AIAssistant.route) {
            AIAssistantScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Statistics
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                }
            )
        }

        // Accounts
        composable(Screen.Accounts.route) {
            AccountsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToBudgets = {
                    navController.navigate(Screen.Budgets.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                }
            )
        }

        // Categories
        composable(Screen.Categories.route) {
            CategoriesScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Budgets
        composable(Screen.Budgets.route) {
            BudgetsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Export
        composable(Screen.Export.route) {
            ExportScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}