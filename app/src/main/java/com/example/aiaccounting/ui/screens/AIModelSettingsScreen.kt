package com.example.aiaccounting.ui.screens

import androidx.compose.runtime.Composable
import com.example.aiaccounting.data.local.prefs.AppStateManager

/**
 * 旧版 AI 模型设置界面（已迁移到 [AISettingsScreen]）。
 *
 * 说明：历史上该页面通过 [AppStateManager]（SharedPreferences）保存模型配置。
 * 目前项目已统一到 DataStore + AISettingsViewModel。
 */
@Composable
fun AIModelSettingsScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit
) {
    // 保留参数仅用于兼容旧路由，避免大范围改动导航代码
    AISettingsScreen(onNavigateBack = onNavigateBack)
}
