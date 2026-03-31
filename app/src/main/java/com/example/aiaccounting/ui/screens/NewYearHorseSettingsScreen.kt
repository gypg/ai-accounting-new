package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.components.NewYearHorseBackground
import com.example.aiaccounting.ui.theme.NewYearHorseThemeColors
import com.example.aiaccounting.ui.theme.ThemeOption
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel

private typealias ThemeOptionGridItem = Triple<String, ImageVector, () -> Unit>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewYearHorseSettingsScreen(
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
    themeKey: Int = 0,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    // Re-read theme when themeKey changes so dialog shows correct selection
    val currentTheme by remember(themeKey) { mutableStateOf(appStateManager.getTheme()) }
    val uiState by viewModel.uiState.collectAsState()

    // Defer showing dialog until after first composition to avoid theme color access during transition
    var dialogReady by remember { mutableStateOf(false) }
    LaunchedEffect(showThemeDialog) {
        if (showThemeDialog) {
            dialogReady = true
        }
    }

    // Safe theme colors with fallback defaults (fallback to primary pink-red NewYearHorse default)
    // Use safe default colors to prevent crash on first theme switch
    val safePrimary = if (NewYearHorseThemeColors.primary.value != 0UL) NewYearHorseThemeColors.primary else Color(0xFFD64040)
    val safeOnSurface = if (NewYearHorseThemeColors.onSurface.value != 0UL) NewYearHorseThemeColors.onSurface else Color(0xFF1E293B)
    val safeOnSurfaceVariant = if (NewYearHorseThemeColors.onSurfaceVariant.value != 0UL) NewYearHorseThemeColors.onSurfaceVariant else Color(0xFF475569)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = safeOnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = safeOnSurface
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
        NewYearHorseBackground {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        Triple("分类管理", Icons.AutoMirrored.Filled.Label, onNavigateToCategories),
                        Triple("数据备份", Icons.Default.Backup, onNavigateToExport),
                        Triple("个人中心", Icons.Default.Person, onNavigateToProfile),
                        Triple("记账模板", Icons.Default.Bookmark, onNavigateToTemplates),
                        Triple("账单导入", Icons.Default.FileUpload, onNavigateToImport),
                        Triple("AI助手设置", Icons.Default.SmartToy, onNavigateToAISettings),
                        Triple("预算管理", Icons.Default.PieChart, onNavigateToBudgets),
                        Triple("设置密码", Icons.Default.Lock, onNavigateToSetupPin),
                        Triple("切换主题", Icons.Default.ColorLens, { showThemeDialog = true }),
                        Triple("退出登录", Icons.AutoMirrored.Filled.ExitToApp, onLogout)
                    )

                    // 设置项网格
                    Grid(
                        gridItems = settingsItems.chunked(3),
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 账号信息卡片
                    AccountInfoCard(
                        userName = appStateManager.getUserName(),
                        userEmail = "",
                        onNavigateToProfile = onNavigateToProfile,
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 版本信息
                    VersionCard(
                        version = BuildConfig.VERSION_NAME,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )
                }
            }
        }
    }

    // 主题选择对话框 - only show after first composition frame to avoid theme transition crash
    if (showThemeDialog && dialogReady) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false; dialogReady = false },
            onThemeSelected = { newTheme ->
                appStateManager.setTheme(newTheme)
                onThemeChanged()
                showThemeDialog = false
                dialogReady = false
            }
        )
    }
}

@Composable
private fun Grid(
    gridItems: List<List<ThemeOptionGridItem>>,
    primaryColor: Color,
    onSurfaceColor: Color
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(gridItems.size) { rowIndex ->
            val rowItems = gridItems[rowIndex]
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (label, icon, onClick) ->
                    SettingButton(
                        label = label,
                        icon = icon,
                        onClick = onClick,
                        primaryColor = primaryColor,
                        onSurfaceColor = onSurfaceColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, color = onSurfaceColor, maxLines = 1)
        }
    }
}

@Composable
private fun AccountInfoCard(
    userName: String,
    userEmail: String,
    onNavigateToProfile: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).clickable(onClick = onNavigateToProfile)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color = primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                    Text(text = userEmail, fontSize = 13.sp, color = onSurfaceVariantColor)
                }
            }
        }
    }
}

@Composable
private fun VersionCard(version: String, onSurfaceVariantColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "版本: $version", fontSize = 13.sp, color = onSurfaceVariantColor)
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    // Safe fallback colors in case theme is not fully established
    val safeSurface = NewYearHorseThemeColors.surface
    val safeOnSurface = NewYearHorseThemeColors.onSurface
    val safePrimary = NewYearHorseThemeColors.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp).width(320.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = safeSurface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "选择主题", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = safeOnSurface)
                Spacer(modifier = Modifier.height(16.dp))

                com.example.aiaccounting.ui.theme.AppThemeOptions.all().forEach { theme ->
                    NewYearHorseThemeOptionItem(
                        theme = theme,
                        selected = currentTheme == theme.id,
                        onClick = { onThemeSelected(theme.id) },
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "取消", color = safePrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewYearHorseThemeOptionItem(
    theme: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) primaryColor.copy(alpha = 0.1f) else Color.Transparent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(theme.color),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = theme.title, fontSize = 14.sp, color = onSurfaceColor)
        }
        if (selected) {
            Text(text = "当前", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
        }
    }
}
