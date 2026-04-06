package com.example.aiaccounting.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI 缩放偏好数据类
 */
data class UiScalePreferences(
    val cardScale: Float = 1.0f,
    val fontScale: Float = 1.0f
)

data class LogAutoClearPreferences(
    val enabled: Boolean = AppStateManager.DEFAULT_LOG_AUTO_CLEAR_ENABLED,
    val intervalHours: Int = AppStateManager.DEFAULT_LOG_AUTO_CLEAR_INTERVAL_HOURS,
    val lastRunTimestamp: Long = 0L
)

/**
 * 应用状态管理器 - 管理应用的初始化状态和设置
 */
@Singleton
class AppStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {
    companion object {
        private const val PREFS_NAME = "app_state_prefs"
        private const val KEY_DATABASE_INITIALIZED = "database_initialized"
        private const val KEY_INITIAL_SETUP_COMPLETED = "initial_setup_completed"
        private const val KEY_AI_MODEL_TYPE = "ai_model_type"
        private const val KEY_CUSTOM_MODEL_URL = "custom_model_url"
        private const val KEY_CUSTOM_MODEL_API_KEY = "custom_model_api_key" // legacy plaintext key (will be migrated)
        private const val KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED = "custom_model_api_key_encrypted"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        // UI 缩放偏好
        private const val KEY_UI_SCALE_CARD = "ui_scale_card"
        private const val KEY_UI_SCALE_FONT = "ui_scale_font"
        private const val KEY_UI_SCALE_OVERVIEW = "ui_scale_overview"
        private const val KEY_UI_SCALE_STATISTICS = "ui_scale_statistics"
        private const val KEY_UI_SCALE_TRANSACTION = "ui_scale_transaction"
        private const val KEY_UI_SCALE_SETTINGS = "ui_scale_settings"

        // 日志自动清理
        private const val KEY_LOG_AUTO_CLEAR_ENABLED = "log_auto_clear_enabled"
        private const val KEY_LOG_AUTO_CLEAR_INTERVAL_HOURS = "log_auto_clear_interval_hours"
        private const val KEY_LOG_AUTO_CLEAR_LAST_RUN = "log_auto_clear_last_run"

        // AI 模型类型
        const val AI_MODEL_DEFAULT = "default"
        const val AI_MODEL_CUSTOM = "custom"

        // UI 缩放默认值
        const val DEFAULT_UI_SCALE = 1.0f
        const val MIN_UI_SCALE = 0.7f
        const val MAX_UI_SCALE = 1.4f

        // 日志自动清理默认值
        const val DEFAULT_LOG_AUTO_CLEAR_ENABLED = true
        const val DEFAULT_LOG_AUTO_CLEAR_INTERVAL_HOURS = 1
        val VALID_LOG_AUTO_CLEAR_INTERVAL_HOURS = setOf(1, 6, 24, 168)
    }
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun migrateLegacyCustomModelApiKeyIfNeeded() {
        val legacy = prefs.getString(KEY_CUSTOM_MODEL_API_KEY, "").orEmpty()
        if (legacy.isBlank()) return

        try {
            val existing = securityManager.getEncryptedString(KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED).orEmpty()
            if (existing.isBlank()) {
                securityManager.storeEncryptedString(KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED, legacy)
            }

            // only remove legacy after successful check/store
            prefs.edit().remove(KEY_CUSTOM_MODEL_API_KEY).apply()
        } catch (_: Exception) {
            // Keep legacy plaintext so user doesn't lose access; avoid logging secrets
        }
    }

    // ==================== 数据库初始化状态 ====================

    fun isDatabaseInitialized(): Boolean {
        return prefs.getBoolean(KEY_DATABASE_INITIALIZED, false)
    }

    fun setDatabaseInitialized(initialized: Boolean) {
        prefs.edit().putBoolean(KEY_DATABASE_INITIALIZED, initialized).apply()
    }

    // ==================== 初始设置状态 ====================

    fun isInitialSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_INITIAL_SETUP_COMPLETED, false)
    }

    fun setInitialSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_INITIAL_SETUP_COMPLETED, completed).apply()
    }

    // ==================== AI 模型设置 ====================

    fun getAIModelType(): String {
        return prefs.getString(KEY_AI_MODEL_TYPE, AI_MODEL_DEFAULT) ?: AI_MODEL_DEFAULT
    }

    fun setAIModelType(type: String) {
        prefs.edit().putString(KEY_AI_MODEL_TYPE, type).apply()
    }

    fun getCustomModelUrl(): String {
        return prefs.getString(KEY_CUSTOM_MODEL_URL, "") ?: ""
    }

    fun setCustomModelUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_MODEL_URL, url).apply()
    }

    fun getCustomModelApiKey(): String {
        migrateLegacyCustomModelApiKeyIfNeeded()
        return try {
            securityManager.getEncryptedString(KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED).orEmpty()
        } catch (_: Exception) {
            // fail-closed: do not fall back to plaintext secret
            ""
        }
    }

    fun setCustomModelApiKey(apiKey: String) {
        migrateLegacyCustomModelApiKeyIfNeeded()
        try {
            securityManager.storeEncryptedString(KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED, apiKey)
            // Ensure plaintext is not kept
            prefs.edit().remove(KEY_CUSTOM_MODEL_API_KEY).apply()
        } catch (_: Exception) {
            // fail-closed: do not store plaintext secret
        }
    }

    // ==================== 用户信息 ====================

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "用户") ?: "用户"
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserAvatar(): String? {
        return prefs.getString(KEY_USER_AVATAR, null)
    }

    fun setUserAvatar(avatarPath: String?) {
        prefs.edit().putString(KEY_USER_AVATAR, avatarPath).apply()
    }

    // ==================== 通用设置 ====================

    fun getCurrency(): String {
        return prefs.getString(KEY_CURRENCY, "CNY") ?: "CNY"
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "zh") ?: "zh"
    }

    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    // ==================== 登录状态 ====================

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    // ==================== UI 缩放偏好 ====================

    /**
     * 获取 UI 缩放偏好数据类
     */
    fun getUiScalePreferences(): UiScalePreferences {
        val legacyOverview = prefs.getFloat(KEY_UI_SCALE_OVERVIEW, DEFAULT_UI_SCALE)
        val legacyStatistics = prefs.getFloat(KEY_UI_SCALE_STATISTICS, DEFAULT_UI_SCALE)
        val legacyTransaction = prefs.getFloat(KEY_UI_SCALE_TRANSACTION, DEFAULT_UI_SCALE)
        val legacySettings = prefs.getFloat(KEY_UI_SCALE_SETTINGS, DEFAULT_UI_SCALE)
        val fallbackCardScale = maxOf(legacyOverview, legacyStatistics, legacyTransaction, legacySettings)

        return UiScalePreferences(
            cardScale = prefs.getFloat(KEY_UI_SCALE_CARD, fallbackCardScale),
            fontScale = prefs.getFloat(KEY_UI_SCALE_FONT, DEFAULT_UI_SCALE)
        )
    }

    fun setCardScale(scale: Float) {
        prefs.edit().putFloat(KEY_UI_SCALE_CARD, scale.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)).apply()
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_UI_SCALE_FONT, scale.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)).apply()
    }

    fun getLogAutoClearPreferences(): LogAutoClearPreferences {
        val storedInterval = prefs.getInt(KEY_LOG_AUTO_CLEAR_INTERVAL_HOURS, DEFAULT_LOG_AUTO_CLEAR_INTERVAL_HOURS)
        val intervalHours = storedInterval.takeIf { it in VALID_LOG_AUTO_CLEAR_INTERVAL_HOURS }
            ?: DEFAULT_LOG_AUTO_CLEAR_INTERVAL_HOURS
        return LogAutoClearPreferences(
            enabled = prefs.getBoolean(KEY_LOG_AUTO_CLEAR_ENABLED, DEFAULT_LOG_AUTO_CLEAR_ENABLED),
            intervalHours = intervalHours,
            lastRunTimestamp = prefs.getLong(KEY_LOG_AUTO_CLEAR_LAST_RUN, 0L)
        )
    }

    fun setLogAutoClearEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_AUTO_CLEAR_ENABLED, enabled).apply()
    }

    fun setLogAutoClearIntervalHours(hours: Int) {
        val safeHours = hours.takeIf { it in VALID_LOG_AUTO_CLEAR_INTERVAL_HOURS }
            ?: DEFAULT_LOG_AUTO_CLEAR_INTERVAL_HOURS
        prefs.edit().putInt(KEY_LOG_AUTO_CLEAR_INTERVAL_HOURS, safeHours).apply()
    }

    fun setLogAutoClearLastRun(timestamp: Long) {
        prefs.edit().putLong(KEY_LOG_AUTO_CLEAR_LAST_RUN, timestamp).apply()
    }

    fun resetUiScalesToDefault() {
        prefs.edit()
            .putFloat(KEY_UI_SCALE_OVERVIEW, DEFAULT_UI_SCALE)
            .putFloat(KEY_UI_SCALE_STATISTICS, DEFAULT_UI_SCALE)
            .putFloat(KEY_UI_SCALE_TRANSACTION, DEFAULT_UI_SCALE)
            .putFloat(KEY_UI_SCALE_SETTINGS, DEFAULT_UI_SCALE)
            .putFloat(KEY_UI_SCALE_FONT, DEFAULT_UI_SCALE)
            .apply()
    }

    // ==================== 清除所有状态 ====================

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * 清除所有数据（用于忘记密码重置）
     */
    fun clearAllData() {
        // 清除所有SharedPreferences
        prefs.edit().clear().apply()

        // 清除安全区（PIN + 敏感 key）
        securityManager.clearPin()
        securityManager.removeEncryptedString(KEY_CUSTOM_MODEL_API_KEY_ENCRYPTED)

        // 清除数据库
        context.deleteDatabase("ai_accounting_db")
    }
}
