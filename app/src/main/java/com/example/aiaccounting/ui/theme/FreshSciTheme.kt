package com.example.aiaccounting.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 浅色科幻清新主题（Fresh Sci-Fi Light）
 * 特点：柔和天空蓝 + 青绿辅助 + 薰衣草点缀，清新科技感
 */

// 主题颜色定义
val FreshSciColorScheme = lightColorScheme(
    primary = Color(0xFF5B8DEF),       // 天蓝主色
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4DB5C8),     // 青绿辅助
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8E7CC3),      // 薰衣草点缀
    onTertiary = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF94A3B8)
)

// 兼容别名
val FreshSciThemeColors = FreshSciColorScheme

// 豆包设计颜色别名（用于迁移复用）
val LightSciFiPrimary = FreshSciColorScheme.primary              // 天蓝主色 0xFF5B8DEF
val LightSciFiSuccess = Color(0xFF4CAF50)                         // 绿色（收入）
val LightSciFiWarning = Color(0xFFFF6B35)                         // 橙色（支出）
val LightSciFiTextPrimary = Color(0xFF0D1B2E)                    // 深色主文字
val LightSciFiTextSecondary = Color(0xFF656D78)                  // 灰色次要文字
val LightSciFiCardBackground = Color(0xFFFFFFFF)                 // 卡片白色背景

@Composable
fun FreshSciTheme(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = FreshSciColorScheme,
        typography = Typography,
        content = content
    )
}
