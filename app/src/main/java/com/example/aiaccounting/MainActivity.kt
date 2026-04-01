package com.example.aiaccounting

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

 private var pendingWidgetAction by mutableStateOf<String?>(null)
 private var widgetActionNonce by mutableLongStateOf(0L)

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

 pendingWidgetAction = intent.getStringExtra("action")?.takeIf { it.isNotBlank() }
 widgetActionNonce = SystemClock.elapsedRealtime()

 setContent {
 // 使用remember来监听主题和UI缩放变化
 var themeKey by remember { mutableStateOf(0) }
 var uiScaleKey by remember { mutableStateOf(0) }

 // 读取当前主题设置
 val themeSetting = remember(themeKey) { appStateManager.getTheme() }

 // 读取当前UI缩放设置
 val uiScalePreferences = remember(uiScaleKey) { appStateManager.getUiScalePreferences() }

 AIAccountingTheme(
 themeSetting = themeSetting,
 uiScalePreferences = uiScalePreferences
 ) {
 MainApp(
 securityManager = securityManager,
 appStateManager = appStateManager,
 themeKey = themeKey,
 onThemeChanged = { themeKey++ },
 uiScaleKey = uiScaleKey,
 onUiScaleChanged = { uiScaleKey++ },
 onRequestStoragePermission = { requestStoragePermission() },
 widgetAction = pendingWidgetAction,
 widgetActionNonce = widgetActionNonce,
 onWidgetActionConsumed = {
 pendingWidgetAction = null
 }
 )
 }
 }
}

 override fun onNewIntent(intent: Intent) {
 super.onNewIntent(intent)
 setIntent(intent)

 pendingWidgetAction = intent.getStringExtra("action")?.takeIf { it.isNotBlank() }
 widgetActionNonce = SystemClock.elapsedRealtime()
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
 @Suppress("UNUSED_PARAMETER") themeKey: Int,
 onThemeChanged: () -> Unit,
 @Suppress("UNUSED_PARAMETER") uiScaleKey: Int,
 @Suppress("UNUSED_PARAMETER") onUiScaleChanged: () -> Unit,
 @Suppress("UNUSED_PARAMETER") onRequestStoragePermission: () -> Unit,
 widgetAction: String? = null,
 widgetActionNonce: Long = 0L,
 onWidgetActionConsumed: () -> Unit = {}
) {
 // 使用remember来强制重新读取状态
 var refreshKey by remember { mutableStateOf(0) }

 // 内存中的登录状态 - 每次启动都是false，需要重新登录
 var isLoggedIn by remember { mutableStateOf(false) }

 val pinState by remember(refreshKey) {
 mutableStateOf(securityManager.getPinState())
 }
 val isPinSet = pinState is SecurityManager.PinState.Set
 val hasInitialSetup by remember(refreshKey) {
 mutableStateOf(appStateManager.isInitialSetupCompleted())
 }

 // 添加加载状态
 var isLoading by remember { mutableStateOf(true) }

 // 模拟异步加载完成
 LaunchedEffect(Unit) {
 kotlinx.coroutines.delay(100)
 isLoading = false
 }

 // 如果还在加载中，显示加载界面
 if (isLoading) {
 Box(
 modifier = androidx.compose.ui.Modifier.fillMaxSize(),
 contentAlignment = androidx.compose.ui.Alignment.Center
 ) {
 CircularProgressIndicator()
 }
 return
 }

 // Security subsystem init failed: block access (fail-closed)
 if (pinState is SecurityManager.PinState.Error) {
 Box(
 modifier = androidx.compose.ui.Modifier
 .fillMaxSize()
 .padding(24.dp),
 contentAlignment = androidx.compose.ui.Alignment.Center
 ) {
 Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
 Text(
 text = "安全组件初始化失败，无法验证身份",
 style = MaterialTheme.typography.titleMedium
 )
 Spacer(modifier = Modifier.height(12.dp))
 Text(
 text = "请尝试重启应用；若仍失败，可在系统设置中清除应用数据后重试。",
 style = MaterialTheme.typography.bodyMedium
 )
 Spacer(modifier = Modifier.height(16.dp))
 Button(onClick = { refreshKey++ }) {
 Text("重试")
 }
 }
 }
 return
 }

 // 实际登录状态
 val effectiveIsLoggedIn = isLoggedIn

 // Determine start destination
 // PIN码现在可选，用户可以选择设置或跳过
 val startDestination = when {
 !hasInitialSetup -> Screen.InitialSetup.route
 !effectiveIsLoggedIn && isPinSet -> Screen.Login.route
 else -> "overview"
 }

 val navController = rememberNavController()

 // Global PIN holder for database initialization
 var globalPin by remember { mutableStateOf<String?>(null) }

 // 小组件跳转目标（登录后执行）
 var widgetTargetRoute by remember(widgetAction, widgetActionNonce) {
 mutableStateOf(
 when (widgetAction) {
 "add_expense" -> "add_transaction/expense"
 "add_income" -> "add_transaction/income"
 "ai_chat" -> "ai_assistant"
 else -> null
 }
 )
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
 onWidgetActionConsumed()
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
 globalPin = pin.takeIf { it.isNotBlank() }
 isLoggedIn = true
 refreshKey++
 },
 onInitialSetupComplete = {
 appStateManager.setInitialSetupCompleted(true)
 refreshKey++
 },
 onThemeChanged = onThemeChanged,
 uiScaleKey = uiScaleKey,
 onUiScaleChanged = onUiScaleChanged
 )
}
