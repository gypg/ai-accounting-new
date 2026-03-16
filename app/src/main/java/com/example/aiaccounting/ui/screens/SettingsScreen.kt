package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToButlerSettings: () -> Unit,
    onNavigateToButlerMarket: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    onNavigateToAISettings: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToSetupPin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onThemeChanged: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentTheme = appStateManager.getTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "设置",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 数据管理区域
            SettingsSectionTitle("数据管理")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGridItem(
                    icon = Icons.Default.AccountBalance,
                    title = "账户管理",
                    subtitle = "管理银行卡、现金等账户",
                    onClick = onNavigateToAccounts,
                    modifier = Modifier.weight(1f)
                )
                SettingsGridItem(
                    icon = Icons.Default.Label,
                    title = "分类管理",
                    subtitle = "管理收支分类",
                    onClick = onNavigateToCategories,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGridItem(
                    icon = Icons.Default.Backup,
                    title = "数据备份",
                    subtitle = "备份与恢复数据库",
                    onClick = onNavigateToExport,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFFFF3E0)
                )
                SettingsGridItem(
                    icon = Icons.Default.Person,
                    title = "个人中心",
                    subtitle = "个人资料与头像",
                    onClick = onNavigateToProfile,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFE8F5E9)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 新增功能入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGridItem(
                    icon = Icons.Default.Bookmark,
                    title = "记账模板",
                    subtitle = "快速记账模板管理",
                    onClick = onNavigateToTemplates,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFF3E5F5)
                )
                SettingsGridItem(
                    icon = Icons.Default.FileUpload,
                    title = "账单导入",
                    subtitle = "导入支付宝/微信账单",
                    onClick = onNavigateToImport,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFE0F2F1)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGridItem(
                    icon = Icons.Default.SmartToy,
                    title = "AI助手设置",
                    subtitle = "配置AI大模型API",
                    onClick = onNavigateToAISettings,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFFFF8E1)
                )
                SettingsGridItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "预算管理",
                    subtitle = "设置月度预算",
                    onClick = onNavigateToBudgets,
                    modifier = Modifier.weight(1f),
                    iconBackgroundColor = Color(0xFFE3F2FD)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 安全设置区域
            SettingsSectionTitle("安全")

            val uiState by viewModel.uiState.collectAsState()
            var showBiometricDialog by remember { mutableStateOf(false) }
            var showPinOptionsDialog by remember { mutableStateOf(false) }

            // PIN码设置
            SettingsListItem(
                icon = Icons.Default.Lock,
                title = "PIN码保护",
                subtitle = if (uiState.isPinSet) "已设置" else "未设置",
                onClick = {
                    showPinOptionsDialog = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 生物识别开关
            SettingsListItem(
                icon = Icons.Default.Fingerprint,
                title = "生物识别解锁",
                subtitle = if (uiState.isBiometricEnabled) "已开启" else "已关闭",
                onClick = {
                    showBiometricDialog = true
                }
            )

            // PIN码选项对话框
            if (showPinOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showPinOptionsDialog = false },
                    title = { Text("PIN码保护") },
                    text = {
                        Column {
                            if (uiState.isPinSet) {
                                Text("您已设置PIN码保护")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        showPinOptionsDialog = false
                                        onNavigateToSetupPin()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("修改PIN码")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearPin()
                                        showPinOptionsDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("清除PIN码")
                                }
                            } else {
                                Text("设置PIN码可以保护您的记账数据安全")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        showPinOptionsDialog = false
                                        onNavigateToSetupPin()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("设置PIN码")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPinOptionsDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }

            // 生物识别设置对话框
            if (showBiometricDialog) {
                AlertDialog(
                    onDismissRequest = { showBiometricDialog = false },
                    title = { Text("生物识别解锁") },
                    text = {
                        Column {
                            Text("使用指纹或面部识别快速解锁应用")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("启用生物识别")
                                Switch(
                                    checked = uiState.isBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.toggleBiometric(enabled)
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBiometricDialog = false }) {
                            Text("确定")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 外观设置区域
            SettingsSectionTitle("外观")

            // 主题设置
            val themeText = when (currentTheme) {
                "light" -> "浅色"
                "dark" -> "深色"
                "amoled" -> "AMOLED纯黑"
                "dynamic" -> "Material You动态"
                "horse_2026" -> "2026马年主题"
                else -> "跟随系统"
            }

            SettingsListItem(
                icon = Icons.Default.Palette,
                title = "主题",
                subtitle = themeText,
                onClick = { showThemeDialog = true }
            )

            // Material You动态主题开关（仅Android 12+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                var dynamicThemeEnabled by remember { mutableStateOf(currentTheme == "dynamic") }
                
                SettingsListItem(
                    icon = Icons.Default.ColorLens,
                    title = "Material You动态主题",
                    subtitle = "跟随系统主题色自动调整",
                    onClick = {
                        dynamicThemeEnabled = !dynamicThemeEnabled
                        val newTheme = if (dynamicThemeEnabled) "dynamic" else "system"
                        appStateManager.setTheme(newTheme)
                        onThemeChanged()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 管家设置区域
            SettingsSectionTitle("管家")

            // 管家角色管理
            SettingsListItem(
                icon = Icons.Default.SmartToy,
                title = "AI管家",
                subtitle = "选择您喜欢的管家角色",
                onClick = onNavigateToButlerSettings
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 独立市场入口（更显眼，便于直达）
            SettingsListItem(
                icon = Icons.Default.Storefront,
                title = "管家市场",
                subtitle = "发现、创建和管理自定义管家",
                onClick = onNavigateToButlerMarket
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 其他区域
            SettingsSectionTitle("其他")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 关于按钮
                OutlinedButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("关于")
                }
                
                // 公告按钮
                OutlinedButton(
                    onClick = { showNoticeDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("公告")
                    // 红点提示
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, RoundedCornerShape(4.dp))
                    )
                }
                
                // 退出登录按钮
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("退出登录")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 关于对话框
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 AI 记账") },
            text = {
                Column {
                    Text("AI记账是一款智能的个人财务管理应用。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("版本：${BuildConfig.VERSION_NAME}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("功能特点：")
                    Text("• 智能语音识别记账")
                    Text("• 数据加密安全存储")
                    Text("• 多维度统计分析")
                    Text("• Excel导出功能")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 公告对话框
    if (showNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = { Text("公告") },
            text = {
                Column {
                    Text("欢迎使用 AI 记账！")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 新增AI智能记账功能")
                    Text("• 优化界面设计")
                    Text("• 修复已知问题")
                }
            },
            confirmButton = {
                TextButton(onClick = { showNoticeDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 退出登录确认对话框
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    }
                ) {
                    Text("确定", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 主题选择对话框
    if (showThemeDialog) {
        val themes = listOf(
            Triple("system", "跟随系统", "自动切换浅色/深色模式"),
            Triple("light", "浅色", "明亮的浅色主题"),
            Triple("dark", "深色", "深色主题，护眼模式"),
            Triple("amoled", "AMOLED纯黑", "纯黑背景，OLED省电"),
            Triple("horse_2026", "2026马年主题", "新春马年主题")
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    themes.forEach { (theme, title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appStateManager.setTheme(theme)
                                    showThemeDialog = false
                                    // 通知主题变化
                                    onThemeChanged()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == theme,
                                onClick = {
                                    appStateManager.setTheme(theme)
                                    showThemeDialog = false
                                    // 通知主题变化
                                    onThemeChanged()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        color = Color(0xFF888888),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun SettingsGridItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = Color(0xFFE3F2FD)
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 文字内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改PIN码") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 6) currentPin = it },
                    label = { Text("当前PIN码") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("新PIN码") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("确认新PIN码") },
                    singleLine = true
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        currentPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty() ->
                            errorMessage = "请填写所有字段"
                        newPin != confirmPin ->
                            errorMessage = "两次输入的新PIN码不一致"
                        newPin.length < 4 ->
                            errorMessage = "PIN码至少需要4位"
                        else -> {
                            errorMessage = null
                            onConfirm(currentPin, newPin)
                        }
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
