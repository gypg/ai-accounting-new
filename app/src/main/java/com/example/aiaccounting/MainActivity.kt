package com.example.aiaccounting

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.security.SecurityManager
import com.example.aiaccounting.ui.navigation.AppNavigation
import com.example.aiaccounting.ui.navigation.Screen
import com.example.aiaccounting.ui.theme.AIAccountingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var appStateManager: AppStateManager

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "需要存储权限才能导出文件", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏标题栏
        actionBar?.hide()

        // 获取小组件传来的action
        val widgetAction = intent.getStringExtra("action")

        setContent {
            // 使用remember来监听主题变化
            var themeKey by remember { mutableStateOf(0) }
            
            // 读取当前主题设置
            val themeSetting = remember(themeKey) { appStateManager.getTheme() }
            
            AIAccountingTheme(
                themeSetting = themeSetting
            ) {
                MainApp(
                    securityManager = securityManager,
                    appStateManager = appStateManager,
                    themeKey = themeKey,
                    onThemeChanged = { themeKey++ },
                    onRequestStoragePermission = { requestStoragePermission() },
                    widgetAction = widgetAction
                )
            }
        }
    }

    private fun requestStoragePermission() {
        when {
            // Android 13+ (API 33+) - Use granular media permissions
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13+, we need to request READ_MEDIA_IMAGES permission
                // or use Storage Access Framework for saving to Downloads
                // For now, we use app-specific directory which doesn't need permission
                // If you need to save to Downloads, implement Storage Access Framework
            }
            // Android 10+ (API 29+) - Scoped storage
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                // Use app-specific directory or Storage Access Framework
                // No permission needed for app-specific directories
            }
            // Android 9 and below
            else -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun MainApp(
    securityManager: SecurityManager,
    appStateManager: AppStateManager,
    themeKey: Int,
    onThemeChanged: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    widgetAction: String? = null
) {
    // 使用remember来强制重新读取状态
    var refreshKey by remember { mutableStateOf(0) }
    
    // 内存中的登录状态 - 每次启动都是false，需要重新登录
    var isLoggedIn by remember { mutableStateOf(false) }
    
    // 每次refreshKey变化时重新读取持久化状态
    val isPinSet = remember(refreshKey) { securityManager.isPinSet() }
    val hasInitialSetup = remember(refreshKey) { appStateManager.isInitialSetupCompleted() }

    // 如果有小组件action且已设置PIN，自动标记为已登录（简化流程）
    val autoLoginFromWidget = widgetAction != null && isPinSet && hasInitialSetup
    
    // 实际登录状态
    val effectiveIsLoggedIn = isLoggedIn || autoLoginFromWidget

    // Determine start destination
    val startDestination = when {
        !hasInitialSetup -> Screen.InitialSetup.route
        !isPinSet -> Screen.SetupPin.route
        !effectiveIsLoggedIn -> Screen.Login.route
        else -> "overview"
    }

    val navController = rememberNavController()

    // Global PIN holder for database initialization
    var globalPin by remember { mutableStateOf<String?>(null) }
    
    // 小组件跳转目标
    var widgetTargetRoute by remember { mutableStateOf<String?>(null) }
    
    // 处理小组件action
    LaunchedEffect(widgetAction, effectiveIsLoggedIn) {
        if (effectiveIsLoggedIn && widgetAction != null) {
            widgetTargetRoute = when (widgetAction) {
                "add_expense" -> "add_transaction/expense"
                "add_income" -> "add_transaction/income"
                "ai_chat" -> "ai_assistant"
                else -> null
            }
        }
    }
    
    // 当登录成功后，如果有小组件目标，自动跳转
    LaunchedEffect(effectiveIsLoggedIn, widgetTargetRoute) {
        if (effectiveIsLoggedIn && widgetTargetRoute != null) {
            // 延迟一点确保导航已准备好
            kotlinx.coroutines.delay(100)
            navController.navigate(widgetTargetRoute!!) {
                popUpTo("overview") { inclusive = false }
            }
            widgetTargetRoute = null
        }
    }

    AppNavigation(
        navController = navController,
        startDestination = startDestination,
        appStateManager = appStateManager,
        onSetupComplete = { pin ->
            globalPin = pin
            isLoggedIn = true
            refreshKey++
        },
        onLoginSuccess = { pin ->
            globalPin = pin
            isLoggedIn = true
            refreshKey++
        },
        onInitialSetupComplete = {
            appStateManager.setInitialSetupCompleted(true)
            refreshKey++
        },
        onThemeChanged = onThemeChanged
    )
}
