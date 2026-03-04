package com.example.aiaccounting.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.ui.components.BottomNavBar
import com.example.aiaccounting.ui.components.BottomNavItems
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
    object Profile : Screen("profile")
    object AIModelSettings : Screen("ai_model_settings")
    object Templates : Screen("templates")
    object Import : Screen("import")
    object AISettings : Screen("ai_settings")
}

/**
 * App Navigation Host
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    appStateManager: AppStateManager,
    onSetupComplete: (String) -> Unit = {},
    onLoginSuccess: (String) -> Unit = {},
    onInitialSetupComplete: () -> Unit = {},
    onThemeChanged: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 判断是否需要显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        "overview", "transactions", "statistics", "settings"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: "overview",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // 保存当前页面状态
                            popUpTo("overview") {
                                saveState = true
                            }
                            // 避免重复创建页面
                            launchSingleTop = true
                            // 恢复之前保存的状态
                            restoreState = true
                        }
                    },
                    items = BottomNavItems.items
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // PIN Setup
            composable(Screen.SetupPin.route) {
                SetupPinScreen(
                    onSetupComplete = { pin ->
                        onSetupComplete(pin)
                        navController.navigate(Screen.InitialSetup.route) {
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
                        navController.navigate("overview") {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onForgotPassword = {
                        // 清除所有数据并重新初始化
                        appStateManager.clearAllData()
                        navController.navigate(Screen.SetupPin.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Initial Setup
            composable(Screen.InitialSetup.route) {
                InitialSetupScreen(
                    onSetupComplete = {
                        onInitialSetupComplete()
                        navController.navigate("overview") {
                            popUpTo(Screen.InitialSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            // 总览 (使用底部导航)
            composable("overview") {
                OverviewScreen(
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onNavigateToAI = {
                        navController.navigate(Screen.AIAssistant.route)
                    }
                )
            }

            // 明细 (使用底部导航)
            composable("transactions") {
                TransactionListScreen(
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    }
                )
            }

            // 统计 (使用底部导航)
            composable("statistics") {
                StatisticsScreen()
            }

            // 设置 (使用底部导航)
            composable("settings") {
                SettingsScreen(
                    appStateManager = appStateManager,
                    onNavigateBack = {
                        navController.navigate("overview") {
                            popUpTo("overview") { inclusive = true }
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToAIModelSettings = {
                        navController.navigate(Screen.AIModelSettings.route)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route)
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    },
                    onNavigateToTemplates = {
                        navController.navigate(Screen.Templates.route)
                    },
                    onNavigateToImport = {
                        navController.navigate(Screen.Import.route)
                    },
                    onNavigateToAISettings = {
                        navController.navigate(Screen.AISettings.route)
                    },
                    onNavigateToBudgets = {
                        navController.navigate(Screen.Budgets.route)
                    },
                    onLogout = {
                        // 清除登录状态并跳转到登录页面
                        appStateManager.setLoggedIn(false)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onThemeChanged = onThemeChanged
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
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
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
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Accounts
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Categories
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onNavigateBack = {
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

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    appStateManager = appStateManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // AI Model Settings
            composable(Screen.AIModelSettings.route) {
                AIModelSettingsScreen(
                    appStateManager = appStateManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Templates
            composable(Screen.Templates.route) {
                TemplateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route)
                    }
                )
            }

            // Import
            composable(Screen.Import.route) {
                ImportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // AI Settings
            composable(Screen.AISettings.route) {
                AISettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
