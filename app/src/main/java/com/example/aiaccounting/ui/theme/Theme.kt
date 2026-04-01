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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.example.aiaccounting.ui.theme.FreshSciColorScheme

/**
 * Current app theme id (string) for theme-aware UI.
 */
val LocalAppThemeId = staticCompositionLocalOf { AppThemeIds.SYSTEM }

/**
 * Whether the current theme uses dreamy glass effect (Fresh Sci-Fi / New Year Horse).
 * Extracted to CompositionLocal to avoid repeated pattern matching across screens.
 */
val LocalIsDreamyTheme = staticCompositionLocalOf { false }

// ==================== 亮色主题 - 极简高级 (Exaggerated Minimalism) ====================
private val LightColorScheme = lightColorScheme(
	primary = Color(0xFF2563EB), // 信任蓝
	onPrimary = Color.White,
	primaryContainer = Color(0xFFDBEAFE),
	onPrimaryContainer = Color(0xFF1E3A8A),

	// 次要色调 - 辅助蓝
	secondary = Color(0xFF3B82F6),
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFEFF6FF),
	onSecondaryContainer = Color(0xFF1D4ED8),

	// 第三色调/CTA - 醒目橙
	tertiary = Color(0xFFF97316),
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFFFEDD5),
	onTertiaryContainer = Color(0xFF7C2D12),

	// 背景 - 极简冷白
	background = Color(0xFFF8FAFC),
	onBackground = Color(0xFF1E293B),

	// 表面色 - 纯白，形成深度
	surface = Color(0xFFFFFFFF),
	onSurface = Color(0xFF0F172A),
	surfaceVariant = Color(0xFFF1F5F9),
	onSurfaceVariant = Color(0xFF475569),

	// 错误色
	error = Color(0xFFEF4444),
	onError = Color.White,
	errorContainer = Color(0xFFFEE2E2),
	onErrorContainer = Color(0xFF991B1B),

	// 边框色
	outline = Color(0xFFCBD5E1),
	outlineVariant = Color(0xFFE2E8F0),

	// 反色
	inverseSurface = Color(0xFF0F172A),
	inverseOnSurface = Color(0xFFF8FAFC),
	inversePrimary = Color(0xFF60A5FA),

	surfaceTint = Color(0xFF2563EB),
	scrim = Color(0xFF000000)
)

// ==================== 暗黑主题 - 护眼深岩 (Premium Dark) ====================
private val DarkColorScheme = darkColorScheme(
	// 主色调 - 亮信任蓝
	primary = Color(0xFF60A5FA),
	onPrimary = Color(0xFF0D2847),
	primaryContainer = Color(0xFF1E3A8A),
	onPrimaryContainer = Color(0xFFDBEAFE),

	// 次要色调
	secondary = Color(0xFF93C5FD),
	onSecondary = Color(0xFF082F49),
	secondaryContainer = Color(0xFF1D4ED8),
	onSecondaryContainer = Color(0xFFEFF6FF),

	// 第三色调/CTA - 亮橙
	tertiary = Color(0xFFFB923C),
	onTertiary = Color(0xFF431407),
	tertiaryContainer = Color(0xFF7C2D12),
	onTertiaryContainer = Color(0xFFFFEDD5),

	// 背景 - 深岩灰
	background = Color(0xFF0F172A),
	onBackground = Color(0xFFF8FAFC),

	// 表面色
	surface = Color(0xFF1E293B),
	onSurface = Color(0xFFF1F5F9),
	surfaceVariant = Color(0xFF334155),
	onSurfaceVariant = Color(0xFFCBD5E1),

	// 错误色
	error = Color(0xFFF87171),
	onError = Color(0xFF450A0A),
	errorContainer = Color(0xFF991B1B),
	onErrorContainer = Color(0xFFFEE2E2),

	// 边框色
	outline = Color(0xFF475569),
	outlineVariant = Color(0xFF334155),

	// 反色
	inverseSurface = Color(0xFFF8FAFC),
	inverseOnSurface = Color(0xFF0F172A),
	inversePrimary = Color(0xFF2563EB),

	surfaceTint = Color(0xFF60A5FA),
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
	uiScalePreferences: com.example.aiaccounting.data.local.prefs.UiScalePreferences = com.example.aiaccounting.data.local.prefs.UiScalePreferences(),
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit
) {
	val isSystemInDarkTheme = isSystemInDarkTheme()

	val (darkTheme, amoledMode, dynamicMode, horse2026Mode) = when (themeSetting) {
		"light" -> Quadruple(false, false, false, false)
		"dark" -> Quadruple(true, false, false, false)
		"amoled" -> Quadruple(true, true, false, false)
		"dynamic" -> Quadruple(isSystemInDarkTheme, false, true, false)
		"horse_year" -> Quadruple(false, false, false, true) // legacy alias -> horse_2026
		"horse_2026" -> Quadruple(false, false, false, true)
		else -> Quadruple(isSystemInDarkTheme, false, false, false) // system default
	}

	val freshSciMode = themeSetting == "fresh_sci"

	val colorScheme = when {
		// 2026马年主题（新设计）
		horse2026Mode -> HorseTheme2026ColorScheme
		// 浅色科幻清新主题
		freshSciMode -> FreshSciColorScheme
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

	// Dreamy themes use glass cards with transparent backgrounds
	val isDreamyTheme = themeSetting == "fresh_sci"

	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography
	) {
		CompositionLocalProvider(
			LocalAppThemeId provides themeSetting,
			LocalIsDreamyTheme provides isDreamyTheme,
			LocalUiScale provides uiScalePreferences
		) {
			content()
		}
	}
}

// ==================== 主题工具函数 ====================

/**
 * 四元组数据类
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * 五元组数据类
 */
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

/**
 * 六元组数据类
 */
data class Sextuple<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)

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
