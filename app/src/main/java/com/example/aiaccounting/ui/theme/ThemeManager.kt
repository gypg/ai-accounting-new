package com.example.aiaccounting.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题管理器
 * 用于管理应用的主题设置（浅色/深色/跟随系统）
 */
class ThemeManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * 主题模式枚举
     */
    enum class ThemeMode {
        LIGHT,      // 浅色模式
        DARK,       // 深色模式
        SYSTEM      // 跟随系统
    }

    /**
     * 获取保存的主题模式
     */
    private fun getSavedThemeMode(): ThemeMode {
        val mode = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.values().getOrElse(mode) { ThemeMode.SYSTEM }
    }

    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        _themeMode.value = mode
    }

    /**
     * 获取当前主题模式
     */
    fun getCurrentThemeMode(): ThemeMode {
        return _themeMode.value
    }

    /**
     * 切换主题（浅色 <-> 深色）
     */
    fun toggleTheme() {
        val currentMode = _themeMode.value
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> {
                // 如果当前是跟随系统，根据当前系统主题切换到相反的主题
                ThemeMode.DARK // 简化处理，实际应该检测当前系统主题
            }
        }
        setThemeMode(newMode)
    }

    /**
     * 判断当前是否为深色模式
     */
    @Composable
    fun isDarkTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        }
    }
}

/**
 * 全局主题状态
 */
object ThemeState {
    private var themeManager: ThemeManager? = null

    fun init(context: Context) {
        themeManager = ThemeManager.getInstance(context)
    }

    fun getThemeManager(): ThemeManager {
        return themeManager ?: throw IllegalStateException("ThemeManager not initialized")
    }
}
