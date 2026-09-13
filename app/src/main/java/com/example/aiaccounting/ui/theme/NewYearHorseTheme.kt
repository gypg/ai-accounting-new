package com.example.aiaccounting.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 新马年科幻节日主题（New Year Horse Sci-Fi Festival, Light）
 * 特点：粉紫 + 金色点缀，浪漫科幻感
 */

// 新马年主题颜色定义
val NewYearHorseSciColorScheme = lightColorScheme(
    primary = Color(0xFFD64040),       // 粉红主色
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB86BFF),     // 紫色辅助
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFE6C278),      // 金色点缀
    onTertiary = Color(0xFF1E293B),
    surface = Color(0xFFFFFAF5),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF5E6F8),
    onSurfaceVariant = Color(0xFF475569),
    background = Color(0xFFFFFAF5),
    onBackground = Color(0xFF1E293B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFE7C6D9)
)

// 兼容别名
val NewYearHorseThemeColors = NewYearHorseSciColorScheme

// 豆包设计颜色别名（用于迁移复用）
val HorseYearPrimary = NewYearHorseSciColorScheme.primary          // 粉红主色 0xFFD64040
val HorseYearGold = Color(0xFFE6C278)                              // 金色点缀
val HorseYearSuccess = Color(0xFF4CAF50)                           // 绿色（收入）
val HorseYearWarning = Color(0xFFFF6B35)                           // 橙色（支出）
val HorseYearTextPrimary = Color(0xFF1E293B)                       // 深色主文字
val HorseYearTextSecondary = Color(0xFF656D78)                     // 灰色次要文字
val HorseYearCardBackground = Color(0xFFFFFAF5)                    // 卡片暖白背景

@Composable
fun NewYearHorseTheme(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = NewYearHorseSciColorScheme,
        typography = Typography,
        content = content
    )
}
