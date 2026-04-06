package com.example.aiaccounting.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.ui.components.BottomNavBar
import com.example.aiaccounting.ui.components.BottomNavItems
import com.example.aiaccounting.ui.components.HorseBottomNavBar
import com.example.aiaccounting.ui.components.FreshSciBottomNavBar
import com.example.aiaccounting.ui.screens.*

/**
 * App Navigation
 */
sealed class Screen(val route: String) {
    object SetupPin : Screen("setup_pin")
    object Login : Screen("login")
    object InitialSetup : Screen("initial_setup")
    object Home : Screen("home")
    object AddTransaction : Screen("add_transaction?entrySource={entrySource}") {
        fun createRoute(entrySource: String = "direct_add") = "add_transaction?entrySource=$entrySource"
    }
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
    object LogBrowser : Screen("log_browser")
    object MonthlyCalendar : Screen("monthly_calendar/{year}/{month}") {
        fun createRoute(year: Int, month: Int) = "monthly_calendar/$year/$month"
    }
    object MonthlyTransactions : Screen("transactions_month/{year}/{month}?day={day}") {
        fun createRoute(year: Int, month: Int, day: Int? = null): String {
            require(month in 1..12) { "month must be between 1 and 12" }
            require(day == null || day in 1..31) { "day must be between 1 and 31" }
            return buildString {
                append("transactions_month/")
                append(year)
                append("/")
                append(month)
                if (day != null) {
                    append("?day=")
                    append(day)
                }
            }
        }
    }
    object YearlyWealth : Screen("yearly_wealth/{year}") {
        fun createRoute(year: Int) = "yearly_wealth/$year"
    }
    object ButlerSettings : Screen("butler_settings")
    object Tags : Screen("tags")
    object ButlerMarket : Screen("butler_market")
    object ButlerEditor : Screen("butler_editor/{butlerId}") {
        fun createRoute(butlerId: String?) = "butler_editor/${butlerId ?: "new"}"
    }
}

private fun materializeRoute(entry: NavBackStackEntry): String? {
    val template = entry.destination.route ?: return null
    val arguments = entry.arguments ?: return template
    val replacements = listOf("transactionId", "butlerId", "year", "month", "day", "entrySource")
    var actualRoute = template
    replacements.forEach { key ->
        if (actualRoute.contains("{$key}")) {
            val value = arguments.get(key)?.toString() ?: return@forEach
            actualRoute = actualRoute.replace("{$key}", value)
        }
    }
    val day = arguments.getInt("day", -1).takeIf { it > 0 }
    if (day != null && !actualRoute.contains("day=") && template.contains("?day={day}")) {
        actualRoute = actualRoute.replace("?day={day}", "?day=$day")
    }
    return actualRoute
}

/**
 * App Navigation Host
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    appStateManager: AppStateManager,
    appLogLogger: AppLogLogger,
    onSetupComplete: (String) -> Unit = {},
    onLoginSuccess: (String) -> Unit = {},
    onInitialSetupComplete: () -> Unit = {},
    onThemeChanged: () -> Unit = {},
    uiScaleKey: Int = 0,
    onUiScaleChanged: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentActualRoute = navBackStackEntry?.let(::materializeRoute)
    val currentEntrySource = navBackStackEntry?.arguments?.getString("entrySource")

    // 判断是否需要显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        "overview", "transactions", "statistics", "settings"
    )

    // 获取当前主题
    val currentTheme = appStateManager.getTheme()
    val isHorseTheme = currentTheme == "horse_2026"
    val isFreshSciTheme = currentTheme == "fresh_sci"

    LaunchedEffect(currentRoute, currentActualRoute, currentTheme, currentEntrySource) {
        currentRoute?.let { route ->
            val routeDetails = buildString {
                append("route_template=")
                append(route)
                append(",route_actual=")
                append(currentActualRoute ?: route)
                if (currentEntrySource != null) {
                    append(",entrySource=")
                    append(currentEntrySource)
                }
                append(",theme=")
                append(currentTheme)
                append(",showBottomBar=")
                append(showBottomBar)
                append(",startDestination=")
                append(startDestination)
            }
            appLogLogger.info(
                source = "UI",
                category = "route_change",
                message = "导航路由切换",
                details = routeDetails
            )
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                if (isHorseTheme) {
                    // 马年主题底部导航栏
                    HorseBottomNavBar(
                        currentRoute = currentRoute ?: "overview",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("overview") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                } else if (isFreshSciTheme) {
                    // 浅色科幻清新主题底部导航栏
                    FreshSciBottomNavBar(
                        currentRoute = currentRoute ?: "overview",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("overview") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                } else {
                    // 默认底部导航栏
                    BottomNavBar(
                        currentRoute = currentRoute ?: "overview",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("overview") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        items = BottomNavItems.items
                    )
                }
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
                    },
                    onNavigateToSetupPin = {
                        navController.navigate(Screen.SetupPin.route) {
                            // 设置PIN码后返回初始设置
                        }
                    }
                )
            }

            // 总览 (使用底部导航)
            composable("overview") {
                if (isHorseTheme) {
                    HorseOverviewScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("overview_menu"))
                        },
                        onNavigateToAI = {
                            navController.navigate(Screen.AIAssistant.route)
                        },
                        onNavigateToButlerMarket = {
                            navController.navigate(Screen.ButlerSettings.route)
                        },
                        onNavigateToTransactions = { route ->
                            navController.navigate(route)
                        },
                        onNavigateToStatistics = {
                            navController.navigate("statistics")
                        },
                        onNavigateToAccounts = {
                            navController.navigate(Screen.Accounts.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToCalendar = { year, month ->
                            navController.navigate(Screen.MonthlyCalendar.createRoute(year, month))
                        },
                        onNavigateToYearlyWealth = { year ->
                            navController.navigate(Screen.YearlyWealth.createRoute(year))
                        }
                    )
                } else if (isFreshSciTheme) {
                    FreshOverviewScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("overview_menu"))
                        },
                        onNavigateToAI = {
                            navController.navigate(Screen.AIAssistant.route)
                        },
                        onNavigateToButlerMarket = {
                            navController.navigate(Screen.ButlerSettings.route)
                        },
                        onNavigateToTransactions = { route ->
                            navController.navigate(route)
                        },
                        onNavigateToStatistics = {
                            navController.navigate("statistics")
                        },
                        onNavigateToAccounts = {
                            navController.navigate(Screen.Accounts.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToCalendar = { year, month ->
                            navController.navigate(Screen.MonthlyCalendar.createRoute(year, month))
                        },
                        onNavigateToYearlyWealth = { year ->
                            navController.navigate(Screen.YearlyWealth.createRoute(year))
                        }
                    )
                } else {
                    OverviewScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("overview_menu"))
                        },
                        onNavigateToAI = {
                            navController.navigate(Screen.AIAssistant.route)
                        },
                        onNavigateToButlerMarket = {
                            navController.navigate(Screen.ButlerSettings.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToTransactions = { year, month, day ->
                            navController.navigate(Screen.MonthlyTransactions.createRoute(year, month, day))
                        },
                        onNavigateToAccounts = {
                            navController.navigate(Screen.Accounts.route)
                        },
                        onNavigateToCalendar = { year, month ->
                            navController.navigate(Screen.MonthlyCalendar.createRoute(year, month))
                        },
                        onNavigateToYearlyWealth = { year ->
                            navController.navigate(Screen.YearlyWealth.createRoute(year))
                        }
                    )
                }
            }

            // 明细 (使用底部导航)
            composable("transactions") {
                if (isHorseTheme) {
                    HorseTransactionScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("direct_add"))
                        }
                    )
                } else if (isFreshSciTheme) {
                    FreshTransactionScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("direct_add"))
                        }
                    )
                } else {
                    TransactionListScreen(
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.AddTransaction.createRoute("direct_add"))
                        },
                        onNavigateToExport = {
                            navController.navigate(Screen.Export.route)
                        }
                    )
                }
            }

            composable(
                route = Screen.MonthlyCalendar.route,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year")
                val month = backStackEntry.arguments?.getInt("month")
                if (year == null || month == null) {
                    navController.popBackStack()
                    return@composable
                }
                MonthlyCalendarScreen(
                    year = year,
                    month = month,
                    onBack = { navController.popBackStack() },
                    onNavigateToDayTransactions = { targetYear, targetMonth, targetDay ->
                        navController.navigate(Screen.MonthlyTransactions.createRoute(targetYear, targetMonth, targetDay))
                    }
                )
            }

            composable(
                route = Screen.YearlyWealth.route,
                arguments = listOf(navArgument("year") { type = NavType.IntType })
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year")
                if (year == null) {
                    navController.popBackStack()
                    return@composable
                }
                YearlyWealthScreen(
                    year = year,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MonthlyTransactions.route,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType },
                    navArgument("day") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year")
                val month = backStackEntry.arguments?.getInt("month")
                val day = backStackEntry.arguments?.getInt("day")?.takeIf { it > 0 }
                TransactionListScreen(
                    onNavigateToAddTransaction = {
                        navController.navigate(Screen.AddTransaction.createRoute("direct_add"))
                    },
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    },
                    initialYear = year,
                    initialMonth = month,
                    initialDay = day
                )
            }

            // 统计 (使用底部导航)
            composable("statistics") {
                if (isHorseTheme) {
                    HorseStatisticsScreen(
                        uiScaleKey = uiScaleKey,
                        onUiScaleChanged = onUiScaleChanged
                    )
                } else if (isFreshSciTheme) {
                    FreshStatisticsScreen(
                        uiScaleKey = uiScaleKey,
                        onUiScaleChanged = onUiScaleChanged
                    )
                } else {
                    StatisticsScreen()
                }
            }

            // 设置 (使用底部导航)
            composable("settings") {
                if (isHorseTheme) {
                    HorseSettingsScreen(
                        appStateManager = appStateManager,
                        onNavigateBack = {
                            navController.navigate("overview") {
                                popUpTo("overview") { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route)
                        },
                        onNavigateToButlerSettings = {
                            navController.navigate(Screen.ButlerSettings.route)
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
                        onNavigateToLogBrowser = {
                            navController.navigate(Screen.LogBrowser.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToSetupPin = {
                            navController.navigate(Screen.SetupPin.route)
                        },
                        onLogout = {
                            appStateManager.setLoggedIn(false)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onThemeChanged = onThemeChanged,
                        uiScaleKey = uiScaleKey,
                        onUiScaleChanged = onUiScaleChanged
                    )
                } else if (isFreshSciTheme) {
                    FreshSettingsScreen(
                        appStateManager = appStateManager,
                        onNavigateBack = {
                            navController.navigate("overview") {
                                popUpTo("overview") { inclusive = true }
                            }
                        },
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route)
                        },
                        onNavigateToButlerSettings = {
                            navController.navigate(Screen.ButlerSettings.route)
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
                        onNavigateToLogBrowser = {
                            navController.navigate(Screen.LogBrowser.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToSetupPin = {
                            navController.navigate(Screen.SetupPin.route)
                        },
                        onLogout = {
                            appStateManager.setLoggedIn(false)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onThemeChanged = onThemeChanged,
                        uiScaleKey = uiScaleKey,
                        onUiScaleChanged = onUiScaleChanged
                    )
                } else {
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
                        onNavigateToButlerSettings = {
                            navController.navigate(Screen.ButlerSettings.route)
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
                        onNavigateToLogBrowser = {
                            navController.navigate(Screen.LogBrowser.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToSetupPin = {
                            navController.navigate(Screen.SetupPin.route)
                        },
                        onLogout = {
                            appStateManager.setLoggedIn(false)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onThemeChanged = onThemeChanged,
                        uiScaleKey = uiScaleKey,
                        onUiScaleChanged = onUiScaleChanged
                    )
                }
            }

            // Add Transaction
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("entrySource") {
                        type = NavType.StringType
                        defaultValue = "direct_add"
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val entrySource = backStackEntry.arguments?.getString("entrySource") ?: "direct_add"
                AddTransactionScreen(
                    entrySource = entrySource,
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
                    },
                    onNavigateToButlerMarket = {
                        navController.navigate(Screen.ButlerSettings.route)
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

            // Profile / Personal Center
            composable(Screen.Profile.route) {
                PersonalCenterScreen(
                    appStateManager = appStateManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSetupPin = {
                        navController.navigate(Screen.SetupPin.route)
                    },
                    onThemeChanged = onThemeChanged
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
                        navController.navigate(Screen.AddTransaction.createRoute("template_flow"))
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
            
            // Log Browser
            composable(Screen.LogBrowser.route) {
                LogBrowserScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Butler Settings (alias to Market for a closed-loop entry experience)
            composable(Screen.ButlerSettings.route) {
                ButlerMarketScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditor = { butlerId ->
                        navController.navigate(Screen.ButlerEditor.createRoute(butlerId))
                    }
                )
            }

            // Tags
            composable(Screen.Tags.route) {
                TagsScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Butler Market
            composable(Screen.ButlerMarket.route) {
                ButlerMarketScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditor = { butlerId ->
                        navController.navigate(Screen.ButlerEditor.createRoute(butlerId))
                    }
                )
            }

            // Butler Editor
            composable(
                route = Screen.ButlerEditor.route,
                arguments = listOf(navArgument("butlerId") {
                    type = NavType.StringType
                    defaultValue = "new"
                })
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getString("butlerId")
                val butlerId = if (rawId == "new") null else rawId
                ButlerEditorScreen(
                    butlerId = butlerId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
