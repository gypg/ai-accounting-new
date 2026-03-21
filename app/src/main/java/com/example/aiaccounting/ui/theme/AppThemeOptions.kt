package com.example.aiaccounting.ui.theme

import android.os.Build

/**
 * Single source of truth for theme options.
 */
object AppThemeIds {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    const val AMOLED = "amoled"
    const val DYNAMIC = "dynamic"

    const val HORSE_2026 = "horse_2026"
    const val NEW_YEAR_HORSE = "new_year_horse"

    const val FRESH_SCI = "fresh_sci"
}

data class ThemeOption(
    val id: String,
    val title: String,
    val description: String,
    val requiresAndroidS: Boolean = false
)

object AppThemeOptions {
    private fun allIncludingUnsupported(): List<ThemeOption> {
        return listOf(
            ThemeOption(AppThemeIds.SYSTEM, "跟随系统", "自动切换浅色/深色模式"),
            ThemeOption(AppThemeIds.LIGHT, "浅色", "明亮的浅色主题"),
            ThemeOption(AppThemeIds.FRESH_SCI, "浅色科幻清新", "浅蓝渐变 + 磨砂玻璃风格"),
            ThemeOption(AppThemeIds.DARK, "深色", "深色主题，护眼模式"),
            ThemeOption(AppThemeIds.AMOLED, "AMOLED纯黑", "纯黑背景，OLED省电"),
            ThemeOption(AppThemeIds.DYNAMIC, "Material You动态", "跟随系统主题色自动调整", requiresAndroidS = true),
            ThemeOption(AppThemeIds.NEW_YEAR_HORSE, "新马年科幻", "粉紫科幻节日主题"),
            ThemeOption(AppThemeIds.HORSE_2026, "2026马年主题", "新春马年主题")
        )
    }

    fun all(): List<ThemeOption> {
        val base = allIncludingUnsupported()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            base
        } else {
            base.filterNot { it.requiresAndroidS }
        }
    }

    fun labelOf(themeId: String): String {
        return allIncludingUnsupported().firstOrNull { it.id == themeId }?.title ?: "跟随系统"
    }
}
