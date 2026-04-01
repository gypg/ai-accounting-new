package com.example.aiaccounting.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color

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

    const val FRESH_SCI = "fresh_sci"
}

data class ThemeOption(
    val id: String,
    val title: String,
    val description: String,
    val requiresAndroidS: Boolean = false,
    val previewColor: Color = Color.Gray
)

object AppThemeOptions {
    private fun allIncludingUnsupported(): List<ThemeOption> {
        return listOf(
            ThemeOption(AppThemeIds.SYSTEM, "跟随系统", "自动切换浅色/深色模式", previewColor = Color(0xFF94A3B8)),
            ThemeOption(AppThemeIds.LIGHT, "浅色", "明亮的浅色主题", previewColor = Color(0xFF5B8DEF)),
            ThemeOption(AppThemeIds.FRESH_SCI, "浅色科幻清新", "浅蓝渐变 + 磨砂玻璃风格", previewColor = Color(0xFF4DB5C8)),
            ThemeOption(AppThemeIds.DARK, "深色", "深色主题，护眼模式", previewColor = Color(0xFF2563EB)),
            ThemeOption(AppThemeIds.AMOLED, "AMOLED纯黑", "纯黑背景，OLED省电", previewColor = Color(0xFF000000)),
            ThemeOption(AppThemeIds.DYNAMIC, "Material You动态", "跟随系统主题色自动调整", requiresAndroidS = true, previewColor = Color(0xFF9333EA)),
            ThemeOption(AppThemeIds.HORSE_2026, "2026马年主题", "新春马年主题", previewColor = Color(0xFFE53935))
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
