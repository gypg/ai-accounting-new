package com.example.aiaccounting.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel

/**
 * 浅色科幻清新主题统计页面
 * 直接复用 StatisticsScreen（已支持 isDreamyTheme/FreshSci 样式）
 * uiScale 参数为后续扩展预留
 */
@Composable
fun FreshStatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    uiScaleKey: Int = 0,
    onUiScaleChanged: () -> Unit = {}
) {
    // FreshSci 模式由 StatisticsScreen 内部的 isDreamyTheme 检测自动生效
    StatisticsScreen(viewModel = viewModel)
}
