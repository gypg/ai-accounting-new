package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.ui.theme.AppThemeOptions
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseSettingsScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToButlerSettings: () -> Unit = {},
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
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentTheme = appStateManager.getTheme()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HorseTheme2026Colors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = HorseTheme2026Colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        HorseBackground {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // 功能按钮网格 - 3列
                    val settingsItems = listOf(
                        Triple("账户管理", Icons.Default.AccountBalance, onNavigateToAccounts),
                        Triple("分类管理", Icons.Default.Label, onNavigateToCategories),
                        Triple("数据备份", Icons.Default.Backup, onNavigateToExport),
                        Triple("个人中心", Icons.Default.Person, onNavigateToProfile),
                        Triple("记账模板", Icons.Default.Bookmark, onNavigateToTemplates),
                        Triple("账单导入", Icons.Default.FileUpload, onNavigateToImport),
                        Triple("AI助手设置", Icons.Default.SmartToy, onNavigateToAISettings),
                        Triple("预算管理", Icons.Default.AccountBalanceWallet, onNavigateToBudgets),
                        Triple("生物识别解锁", Icons.Default.Fingerprint) {
                            // Toggle biometric
                        },
                        Triple("主题", Icons.Default.Palette) {
                            showThemeDialog = true
                        },
                        Triple("AI管家", Icons.Default.Settings, onNavigateToButlerSettings)
                    )

                    // 第一行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        settingsItems.take(3).forEach { (title, icon, onClick) ->
                            SettingsGridButton(
                                icon = icon,
                                title = title,
                                onClick = onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 第二行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        settingsItems.drop(3).take(3).forEach { (title, icon, onClick) ->
                            SettingsGridButton(
                                icon = icon,
                                title = title,
                                onClick = onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 第三行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        settingsItems.drop(6).take(3).forEach { (title, icon, onClick) ->
                            SettingsGridButton(
                                icon = icon,
                                title = title,
                                onClick = onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 第四行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        settingsItems.drop(9).take(3).forEach { (title, icon, onClick) ->
                            SettingsGridButton(
                                icon = icon,
                                title = title,
                                onClick = onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }

                // 底部装饰
                BottomHorseDecoration(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 主题对话框
                if (showThemeDialog) {
                    HorseThemeSelectionDialog(
                        currentTheme = currentTheme,
                        onDismiss = { showThemeDialog = false },
                        onThemeSelected = { theme ->
                            appStateManager.setTheme(theme)
                            showThemeDialog = false
                            onThemeChanged()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGridButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标背景
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HorseTheme2026Colors.Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = HorseTheme2026Colors.Gold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = HorseTheme2026Colors.TextPrimary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HorseThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    val themes = AppThemeOptions.all().map { option ->
        Triple(option.id, option.title, option.description)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HorseTheme2026Colors.CardBackground,
        title = {
            Text(
                "选择主题",
                color = HorseTheme2026Colors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                themes.forEach { (theme, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HorseTheme2026Colors.Gold,
                                unselectedColor = HorseTheme2026Colors.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = HorseTheme2026Colors.TextPrimary
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = HorseTheme2026Colors.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = HorseTheme2026Colors.Gold)
            }
        }
    )
}
