package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.ui.components.SciFiBottomDecoration
import com.example.aiaccounting.ui.components.FreshSciBackground
import com.example.aiaccounting.ui.theme.AppThemeOptions
import com.example.aiaccounting.ui.theme.FreshSciThemeColors
import com.example.aiaccounting.ui.theme.LocalUiScale
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshSettingsScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToButlerSettings: () -> Unit,
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
    uiScaleKey: Int = 0,
    onUiScaleChanged: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUiScaleDialog by remember { mutableStateOf(false) }
    val currentTheme by remember(uiScaleKey) { mutableStateOf(appStateManager.getTheme()) }
    val uiScale by remember(uiScaleKey) { mutableStateOf(appStateManager.getUiScalePreferences()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2E)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        FreshSciBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 账号管理区域
                SettingsSectionTitle("账号管理", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.AccountCircle,
                    title = "个人中心",
                    subtitle = "管理个人信息",
                    onClick = onNavigateToProfile,
                    primaryColor = FreshSciThemeColors.primary
                )

                SettingsRow(
                    icon = Icons.Default.SmartToy,
                    title = "AI管家",
                    subtitle = "管理您的AI管家",
                    onClick = onNavigateToButlerSettings,
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 账户分类区域
                SettingsSectionTitle("账户分类", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.AccountBalance,
                    title = "账户管理",
                    subtitle = "管理银行卡、现金等账户",
                    onClick = onNavigateToAccounts,
                    primaryColor = FreshSciThemeColors.primary
                )
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Label,
                    title = "分类管理",
                    subtitle = "管理收支分类",
                    onClick = onNavigateToCategories,
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 数据管理区域
                SettingsSectionTitle("数据管理", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "导入数据",
                    subtitle = "从文件导入交易数据",
                    onClick = onNavigateToImport,
                    primaryColor = FreshSciThemeColors.primary
                )
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "导出数据",
                    subtitle = "导出交易数据到文件",
                    onClick = onNavigateToExport,
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // AI设置区域
                SettingsSectionTitle("AI设置", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.SettingsEthernet,
                    title = "AI设置",
                    subtitle = "配置AI模型和API",
                    onClick = onNavigateToAISettings,
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 模板和预算区域
                SettingsSectionTitle("模板与预算", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.Description,
                    title = "账单模板",
                    subtitle = "管理常用账单模板",
                    onClick = onNavigateToTemplates,
                    primaryColor = FreshSciThemeColors.primary
                )
                SettingsRow(
                    icon = Icons.Default.PieChart,
                    title = "预算管理",
                    subtitle = "设置月度预算",
                    onClick = onNavigateToBudgets,
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 隐私安全区域
                SettingsSectionTitle("隐私与安全", primaryColor = FreshSciThemeColors.primary)

                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "设置锁屏PIN",
                    subtitle = "${if (viewModel.uiState.value.isPinSet) "已设置" else "未设置"}",
                    onClick = onNavigateToSetupPin,
                    primaryColor = FreshSciThemeColors.primary
                )
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "主题选择",
                    subtitle = "当前主题：${getCurrentThemeName(currentTheme)}",
                    onClick = { showThemeDialog = true },
                    primaryColor = FreshSciThemeColors.primary
                )
                SettingsRow(
                    icon = Icons.Default.ZoomIn,
                    title = "显示大小",
                    subtitle = "调整界面和字体大小",
                    onClick = { showUiScaleDialog = true },
                    primaryColor = FreshSciThemeColors.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                // 退出登录
                TextButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "退出登录",
                        color = Color(0xFFFF5252),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showLogoutConfirmDialog) {
            ConfirmLogoutDialog(
                onDismiss = { showLogoutConfirmDialog = false },
                onLogout = {
                    showLogoutConfirmDialog = false
                    onLogout()
                }
            )
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentTheme,
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { newTheme ->
                    appStateManager.setTheme(newTheme)
                    showThemeDialog = false
                    onThemeChanged()
                },
                primaryColor = FreshSciThemeColors.primary
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                primaryColor = FreshSciThemeColors.primary
            )
        }

        if (showUiScaleDialog) {
            FreshUiScaleDialog(
                uiScale = uiScale,
                onDismiss = { showUiScaleDialog = false },
                onConfirm = { overviewScale, statisticsScale, transactionScale, settingsScale, fontScale ->
                    appStateManager.setOverviewScale(overviewScale)
                    appStateManager.setStatisticsScale(statisticsScale)
                    appStateManager.setTransactionScale(transactionScale)
                    appStateManager.setSettingsScale(settingsScale)
                    appStateManager.setFontScale(fontScale)
                    showUiScaleDialog = false
                    onUiScaleChanged()
                }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(
    title: String,
    primaryColor: Color = FreshSciThemeColors.primary
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = primaryColor,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primaryColor: Color = FreshSciThemeColors.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFFFF).copy(alpha = 0.88f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = primaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0D1B2E)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF656D78)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFB0B8C4),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit,
    primaryColor: Color
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择主题",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1B2E)
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                AppThemeOptions.all().forEach { option ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (currentTheme == option.id) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                                )
                                .padding(12.dp)
                                .clickable { onThemeSelected(option.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(option.previewColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentTheme == option.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = option.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0D1B2E)
                                    )
                                    Text(
                                        text = option.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF656D78)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = primaryColor,
                    fontSize = 14.sp
                )
            }
        }
    )
}

@Composable
fun ConfirmLogoutDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "退出登录", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B2E))
        },
        text = {
            Text(text = "确定要退出登录吗？", fontSize = 15.sp, color = Color(0xFF656D78))
        },
        confirmButton = {
            TextButton(onClick = onLogout) {
                Text(text = "退出", color = Color(0xFFFF5252), fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = Color(0xFF656D78), fontSize = 14.sp)
            }
        }
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "关于", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B2E))
        },
        text = {
            Column {
                Text(text = "记账APP v${BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = Color(0xFF0D1B2E))
                Text(text = "AI智能记账，让生活更简单", fontSize = 12.sp, color = Color(0xFF656D78))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "确定", color = primaryColor, fontSize = 14.sp)
            }
        }
    )
}

@Composable
fun getCurrentThemeName(themeId: String): String {
    return AppThemeOptions.all().find { it.id == themeId }?.title ?: "未知"
}

/**
 * 浅色科幻清新主题的显示大小对话框
 */
@Composable
fun FreshUiScaleDialog(
    uiScale: com.example.aiaccounting.data.local.prefs.UiScalePreferences,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float, Float, Float) -> Unit
) {
    var localOverviewScale by remember { mutableFloatStateOf(uiScale.overviewScale) }
    var localStatisticsScale by remember { mutableFloatStateOf(uiScale.statisticsScale) }
    var localTransactionScale by remember { mutableFloatStateOf(uiScale.transactionScale) }
    var localSettingsScale by remember { mutableFloatStateOf(uiScale.settingsScale) }
    var localFontScale by remember { mutableFloatStateOf(uiScale.fontScale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FreshSciThemeColors.surface,
        title = {
            Text(
                "显示大小",
                color = FreshSciThemeColors.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "调整各项界面的显示大小（${(localOverviewScale * 100).toInt()}%）",
                    fontSize = 12.sp,
                    color = FreshSciThemeColors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                FreshScaleSliderItem("总览界面", localOverviewScale) { localOverviewScale = it }
                FreshScaleSliderItem("统计界面", localStatisticsScale) { localStatisticsScale = it }
                FreshScaleSliderItem("交易明细", localTransactionScale) { localTransactionScale = it }
                FreshScaleSliderItem("设置界面", localSettingsScale) { localSettingsScale = it }
                FreshScaleSliderItem("全局字体", localFontScale) { localFontScale = it }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    localOverviewScale = 1.0f
                    localStatisticsScale = 1.0f
                    localTransactionScale = 1.0f
                    localSettingsScale = 1.0f
                    localFontScale = 1.0f
                }) {
                    Text("重置", color = FreshSciThemeColors.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    onConfirm(localOverviewScale, localStatisticsScale, localTransactionScale, localSettingsScale, localFontScale)
                }) {
                    Text("确定", color = FreshSciThemeColors.primary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = FreshSciThemeColors.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun FreshScaleSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp, color = FreshSciThemeColors.onSurface)
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                color = FreshSciThemeColors.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.7f..1.4f,
            steps = 6,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
