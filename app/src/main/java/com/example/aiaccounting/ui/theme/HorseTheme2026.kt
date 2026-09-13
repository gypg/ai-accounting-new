package com.example.aiaccounting.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 2026马年新春主题 - 基于新设计图实现
 * 特点：深红背景、金马装饰、祥云图案、中国传统元素
 */

object HorseTheme2026Colors {
    // 主背景色 - 亮眼鲜红色系（如图2）
    val Background = Color(0xFFE53935)           // 主背景鲜红
    val BackgroundDark = Color(0xFFD32F2F)       // 更深红色
    val BackgroundLight = Color(0xFFEF5350)      // 稍亮红色

    // 卡片背景
    val CardBackground = Color(0xFFEF5350)       // 卡片红色
    val CardBackgroundElevated = Color(0xFFE57373) // 提升卡片

    // 蓝色卡片（明细界面顶部）
    val BlueCard = Color(0xFF2196F3)
    val BlueCardLight = Color(0xFF42A5F5)

    // 金色/黄色系
    val Gold = Color(0xFFFFD700)
    val GoldLight = Color(0xFFFFE55C)
    val GoldDark = Color(0xFFB8860B)

    // 文字颜色
    val TextPrimary = Color(0xFFFFFFFF)          // 纯白
    val TextSecondary = Color(0xFFFFE4B5)        // 米黄
    val TextTertiary = Color(0x80FFFFFF)         // 半透明白

    // 功能色 - 优化对比度
    val Income = Color(0xFF4CAF50)               // 收入绿
    val Expense = Color(0xFFFFEB3B)              // 支出亮黄色（高对比度）
    val ExpenseDark = Color(0xFFFF6B35)          // 支出橙色（备选）
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFF44336)
    val Warning = Color(0xFFFF9800)

    // 装饰色
    val CloudBlue = Color(0xFF64B5F6)            // 祥云蓝
    val BorderGold = Color(0xFFD4AF37)           // 边框金
    val LanternRed = Color(0xFFD32F2F)           // 灯笼红

    // 导航栏颜色 - 肉色/黄金色
    val NavBackground = Color(0xFFE8C9A0)        // 肉色/浅金色背景
    val NavSelectedBackground = Color(0xFFD32F2F) // 红色选中背景
}

// 渐变定义
object HorseTheme2026Gradients {
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            HorseTheme2026Colors.Background,
            HorseTheme2026Colors.BackgroundDark
        )
    )

    val BlueCardGradient = Brush.verticalGradient(
        colors = listOf(
            HorseTheme2026Colors.BlueCardLight,
            HorseTheme2026Colors.BlueCard
        )
    )

    val GoldGradient = Brush.linearGradient(
        colors = listOf(
            HorseTheme2026Colors.GoldLight,
            HorseTheme2026Colors.Gold
        )
    )
}

// Material3 颜色方案
val HorseTheme2026ColorScheme = darkColorScheme(
    primary = HorseTheme2026Colors.Gold,
    onPrimary = Color.Black,
    primaryContainer = HorseTheme2026Colors.GoldDark,
    onPrimaryContainer = Color.White,
    secondary = HorseTheme2026Colors.BlueCard,
    onSecondary = Color.White,
    secondaryContainer = HorseTheme2026Colors.BlueCardLight,
    onSecondaryContainer = Color.White,
    background = HorseTheme2026Colors.Background,
    onBackground = HorseTheme2026Colors.TextPrimary,
    surface = HorseTheme2026Colors.CardBackground,
    onSurface = HorseTheme2026Colors.TextPrimary,
    surfaceVariant = HorseTheme2026Colors.CardBackgroundElevated,
    onSurfaceVariant = HorseTheme2026Colors.TextSecondary,
    outline = HorseTheme2026Colors.BorderGold,
    error = HorseTheme2026Colors.Error,
    onError = Color.White
)

@Composable
fun HorseTheme2026(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = HorseTheme2026ColorScheme,
        typography = Typography,
        content = content
    )
}
