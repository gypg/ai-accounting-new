package com.example.aiaccounting.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

        // AI 模型类型
        const val AI_MODEL_DEFAULT = "default"
        const val AI_MODEL_CUSTOM = "custom"
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
