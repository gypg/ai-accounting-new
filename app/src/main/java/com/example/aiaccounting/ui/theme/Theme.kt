package com.example.aiaccounting.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== 亮色主题 ====================
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF1565C0),
    
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF2E7D32),
    
    tertiary = Color(0xFFFF9800),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFFE65100),
    
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    error = Color(0xFFF44336),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFF6B9FFF),
    
    surfaceTint = Color(0xFF2196F3),
    scrim = Color(0xFF000000)
)

// ==================== 暗黑主题 - 护眼深灰 ====================
// 参考BeeCount的深色模式，使用深灰色而非纯黑，对眼睛更友好
private val DarkColorScheme = darkColorScheme(
    // 主色调 - 更亮的蓝色用于暗黑模式
    primary = Color(0xFF6B9FFF),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    // 次要色调 - 绿色
    secondary = Color(0xFF6BFFB8),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF005236),
    onSecondaryContainer = Color(0xFF89F8C5),
    
    // 第三色调 - 橙色
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF4D2700),
    tertiaryContainer = Color(0xFF6D3A00),
    onTertiaryContainer = Color(0xFFFFDCC2),
    
    // 背景 - 深灰色，对眼睛更友好
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE6E1E5),
    
    // 表面色 - 比背景稍亮的深灰色
    surface = Color(0xFF252542),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2D2D4A),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    // 错误色
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // 边框色
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    // 反色
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF2196F3),
    
    surfaceTint = Color(0xFF6B9FFF),
    scrim = Color(0xFF000000)
)

// ==================== 纯黑AMOLED主题 - 极致省电 ====================
private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B9FFF),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF1A237E),
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    secondary = Color(0xFF6BFFB8),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFF89F8C5),
    
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF4D2700),
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFDCC2),
    
    // 纯黑背景 - OLED省电
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    
    // 纯黑表面 - 与背景一致
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF1A1A1A),
    
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF2196F3),
    
    surfaceTint = Color(0xFF6B9FFF),
    scrim = Color(0xFF000000)
)

@Composable
fun AIAccountingTheme(
    themeSetting: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    val (darkTheme, amoledMode, dynamicMode) = when (themeSetting) {
        "light" -> Triple(false, false, false)
        "dark" -> Triple(true, false, false)
        "amoled" -> Triple(true, true, false)
        "dynamic" -> Triple(isSystemInDarkTheme, false, true)
        else -> Triple(isSystemInDarkTheme, false, false) // system default
    }
    
    val colorScheme = when {
        // Material You动态主题（Android 12+）
        dynamicMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // AMOLED纯黑模式
        amoledMode -> AmoledDarkColorScheme
        // 暗黑模式
        darkTheme -> DarkColorScheme
        // 亮色模式
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色为透明，使用系统栏适配
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // 设置导航栏颜色
            window.navigationBarColor = if (amoledMode) {
                Color.Black.toArgb()
            } else {
                colorScheme.surface.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ==================== 主题工具函数 ====================

/**
 * 获取当前是否为暗黑模式
 */
@Composable
fun isDarkTheme(): Boolean = isSystemInDarkTheme()

/**
 * 获取主题颜色
 */
object ThemeColors {
    val IncomeGreen = Color(0xFF4CAF50)
    val ExpenseRed = Color(0xFFF44336)
    val TransferBlue = Color(0xFF2196F3)
    val WarningOrange = Color(0xFFFF9800)
    
    // 暗黑模式适配的颜色
    @Composable
    fun incomeColor(): Color = if (isDarkTheme()) Color(0xFF6BFFB8) else IncomeGreen
    
    @Composable
    fun expenseColor(): Color = if (isDarkTheme()) Color(0xFFFFB4AB) else ExpenseRed
    
    @Composable
    fun transferColor(): Color = if (isDarkTheme()) Color(0xFF6B9FFF) else TransferBlue
}
