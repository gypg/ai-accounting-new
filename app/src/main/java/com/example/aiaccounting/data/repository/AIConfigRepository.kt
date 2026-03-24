package com.example.aiaccounting.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.service.PreferredNetworkRoute
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI配置仓库
 */
@Singleton
class AIConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {
    private val dataStore = context.aiConfigDataStore

    private val legacyApiKeyKey = stringPreferencesKey(AIConfig.KEY_API_KEY)

    suspend fun migrateLegacyApiKeyIfNeeded() {
        try {
            val preferences = dataStore.data.first()
            val legacyKey = preferences[legacyApiKeyKey].orEmpty()
            if (legacyKey.isBlank()) return

            val encryptedExisting = try {
                securityManager.getEncryptedString(AIConfig.KEY_API_KEY).orEmpty()
            } catch (_: Exception) {
                ""
            }

            if (encryptedExisting.isBlank()) {
                securityManager.storeEncryptedString(AIConfig.KEY_API_KEY, legacyKey)
            }

            // Clear legacy plaintext key
            dataStore.edit { prefs ->
                prefs.remove(legacyApiKeyKey)
            }
        } catch (_: Exception) {
            // Best-effort migration. Never crash app startup due to migration.
        }
    }

    suspend fun migrateInviteGatewayConfigIfNeeded(
        defaultGatewayBaseUrl: String,
        legacyGatewayHostKeywords: List<String>
    ) {
        try {
            val preferences = dataStore.data.first()
            val currentGatewayBaseUrl = preferences[stringPreferencesKey(AIConfig.KEY_GATEWAY_BASE_URL)].orEmpty()
            val currentInviteApiUrl = preferences[stringPreferencesKey(AIConfig.KEY_INVITE_API_URL)].orEmpty()

            val shouldResetGatewayBaseUrl = shouldResetLegacyUrl(
                url = currentGatewayBaseUrl,
                legacyGatewayHostKeywords = legacyGatewayHostKeywords
            )
            val shouldClearInviteBinding = shouldResetLegacyUrl(
                url = currentInviteApiUrl,
                legacyGatewayHostKeywords = legacyGatewayHostKeywords
            )

            if (!shouldResetGatewayBaseUrl && !shouldClearInviteBinding) return

            dataStore.edit { prefs ->
                if (shouldResetGatewayBaseUrl) {
                    prefs[stringPreferencesKey(AIConfig.KEY_GATEWAY_BASE_URL)] = defaultGatewayBaseUrl
                }

                if (shouldClearInviteBinding) {
                    prefs[booleanPreferencesKey(AIConfig.KEY_INVITE_BOUND)] = false
                    prefs.remove(stringPreferencesKey(AIConfig.KEY_INVITE_API_URL))
                    prefs.remove(stringPreferencesKey(AIConfig.KEY_INVITE_MODEL))
                    prefs.remove(stringPreferencesKey(AIConfig.KEY_INVITE_MODEL_MODE))
                    prefs.remove(intPreferencesKey(AIConfig.KEY_INVITE_RPM))
                    prefs[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = false
                }
            }

            if (shouldClearInviteBinding) {
                securityManager.removeEncryptedString(AIConfig.KEY_INVITE_TOKEN)
                securityManager.removeEncryptedString(AIConfig.KEY_INVITE_CODE)
            }
        } catch (_: Exception) {
            // Best-effort migration. Never crash app startup due to migration.
        }
    }

    private fun shouldResetLegacyUrl(
        url: String,
        legacyGatewayHostKeywords: List<String>
    ): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return legacyGatewayHostKeywords.any { keyword ->
            trimmed.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 获取AI配置流
     */
    fun getAIConfig(): Flow<AIConfig> {
        val useBuiltinFlow = getUseBuiltin()
        val inviteBoundFlow = getInviteBound()
        val modelModeFlow = getModelSelectionMode()
        val inviteModelModeFlow = getInviteModelSelectionMode()

        return combine(
            dataStore.data,
            useBuiltinFlow,
            inviteBoundFlow,
            modelModeFlow,
            inviteModelModeFlow
        ) { preferences, useBuiltin, inviteBound, modelMode, inviteModelMode ->
            val userApiKey = try {
                securityManager.getEncryptedString(AIConfig.KEY_API_KEY).orEmpty()
            } catch (_: Exception) {
                ""
            }

            val storedUserModel = preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] ?: ""

            val userConfig = AIConfig(
                provider = AIProvider.fromString(
                    preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] ?: AIProvider.QWEN.name
                ),
                apiKey = userApiKey,
                apiUrl = preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] ?: "",
                // 如果用户开启 AUTO，则运行时将 model 置空，交给 AIService 做自动选择/切换
                model = if (modelMode == ModelSelectionMode.AUTO) "" else storedUserModel,
                isEnabled = preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] ?: false
            )

            when {
                useBuiltin -> {
                    // 默认模型覆盖邀请码：invite 信息保留但不生效
                    userConfig.copy(
                        provider = AIConfig.BUILTIN_CONFIG.provider,
                        apiUrl = AIConfig.BUILTIN_CONFIG.apiUrl,
                        model = AIConfig.BUILTIN_CONFIG.model,
                        // apiKey 仍使用用户的 key（由用户导入/保存），避免被 BUILTIN_CONFIG 的空 key 覆盖
                        isEnabled = true
                    )
                }

                inviteBound -> {
                    val inviteToken = try {
                        securityManager.getEncryptedString(AIConfig.KEY_INVITE_TOKEN).orEmpty()
                    } catch (_: Exception) {
                        ""
                    }

                    val inviteApiUrl = preferences[stringPreferencesKey(AIConfig.KEY_INVITE_API_URL)] ?: ""
                    val storedInviteModel = preferences[stringPreferencesKey(AIConfig.KEY_INVITE_MODEL)] ?: ""

                    userConfig.copy(
                        provider = AIProvider.CUSTOM,
                        apiKey = inviteToken,
                        apiUrl = inviteApiUrl,
                        // invite AUTO 时 model 置空，交给 AIService 自动选择
                        model = if (inviteModelMode == ModelSelectionMode.AUTO) "" else storedInviteModel,
                        isEnabled = true
                    )
                }

                else -> userConfig
            }
        }
    }

    enum class ModelSelectionMode {
        AUTO,
        FIXED;
    }

    private fun parseModelSelectionMode(value: String?, defaultValue: ModelSelectionMode): ModelSelectionMode {
        return when (value?.uppercase()) {
            "AUTO" -> ModelSelectionMode.AUTO
            "FIXED" -> ModelSelectionMode.FIXED
            else -> defaultValue
        }
    }

    fun getModelSelectionMode(): Flow<ModelSelectionMode> {
        // Backward compatible default: historical behavior was always FIXED
        return dataStore.data.map { preferences ->
            parseModelSelectionMode(
                value = preferences[stringPreferencesKey(AIConfig.KEY_MODEL_MODE)],
                defaultValue = ModelSelectionMode.FIXED
            )
        }
    }

    suspend fun setModelSelectionMode(mode: ModelSelectionMode) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_MODEL_MODE)] = mode.name
        }
    }

    fun getInviteModelSelectionMode(): Flow<ModelSelectionMode> {
        // Requirement default: invite-bound users default to AUTO unless they manually change
        return dataStore.data.map { preferences ->
            parseModelSelectionMode(
                value = preferences[stringPreferencesKey(AIConfig.KEY_INVITE_MODEL_MODE)],
                defaultValue = ModelSelectionMode.AUTO
            )
        }
    }

    suspend fun setInviteModelSelectionMode(mode: ModelSelectionMode) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_INVITE_MODEL_MODE)] = mode.name
        }
    }

    suspend fun updateInviteModel(modelId: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_INVITE_MODEL)] = modelId
        }
    }

    /**
     * 获取是否使用内置配置
     */
    fun getUseBuiltin(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_USE_BUILTIN)] ?: false
        }
    }


    fun getGatewayBaseUrl(defaultValue: String): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_GATEWAY_BASE_URL)] ?: defaultValue
        }
    }

    suspend fun setGatewayBaseUrl(value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_GATEWAY_BASE_URL)] = value
        }
    }

    fun getInviteBound(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_INVITE_BOUND)] ?: false
        }
    }

    suspend fun setInviteBound(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_INVITE_BOUND)] = value
        }
    }

    /**
     * 设置是否使用内置配置
     */
    suspend fun setUseBuiltin(useBuiltin: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_USE_BUILTIN)] = useBuiltin
            if (useBuiltin) {
                // 如果使用内置配置，自动启用AI
                preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = true
            }
        }
    }

    /**
     * 保存AI配置
     */
    suspend fun saveAIConfig(config: AIConfig) {
        securityManager.storeEncryptedString(AIConfig.KEY_API_KEY, config.apiKey)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] = config.provider.name
            preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] = config.apiUrl
            preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] = config.model
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = config.isEnabled
            preferences.remove(legacyApiKeyKey)
        }
    }

    /**
     * 更新API密钥
     */
    suspend fun updateApiKey(apiKey: String) {
        securityManager.storeEncryptedString(AIConfig.KEY_API_KEY, apiKey)
        dataStore.edit { preferences ->
            preferences.remove(legacyApiKeyKey)
        }
    }

    /**
     * 更新API提供商
     */
    suspend fun updateProvider(provider: AIProvider) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] = provider.name
            // 更新为默认配置
            val defaultConfig = AIConfig.defaultFor(provider)
            preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] = defaultConfig.apiUrl
            preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] = defaultConfig.model
        }
    }

    /**
     * 更新启用状态
     */
    suspend fun updateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = enabled
        }
    }

    /**
     * 清除配置
     */
    suspend fun clearConfig() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        securityManager.removeEncryptedString(AIConfig.KEY_API_KEY)
    }

    suspend fun saveInviteBinding(
        inviteCode: String,
        token: String,
        apiBaseUrl: String,
        model: String,
        rpm: Int
    ) {
        securityManager.storeEncryptedString(AIConfig.KEY_INVITE_TOKEN, token)
        securityManager.storeEncryptedString(AIConfig.KEY_INVITE_CODE, inviteCode)

        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_INVITE_BOUND)] = true
            preferences[stringPreferencesKey(AIConfig.KEY_INVITE_API_URL)] = apiBaseUrl
            preferences[stringPreferencesKey(AIConfig.KEY_INVITE_MODEL)] = model
            preferences[intPreferencesKey(AIConfig.KEY_INVITE_RPM)] = rpm
            // 邀请码绑定默认启用 AI
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = true
        }
    }

    fun getInviteApiBaseUrl(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_INVITE_API_URL)] ?: ""
        }
    }

    fun getInviteRpm(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey(AIConfig.KEY_INVITE_RPM)] ?: 0
        }
    }

    fun getInviteCodeMasked(): Flow<String> {
        return dataStore.data.map {
            val raw = try {
                securityManager.getEncryptedString(AIConfig.KEY_INVITE_CODE).orEmpty()
            } catch (_: Exception) {
                ""
            }

            if (raw.isBlank()) return@map ""

            val suffix = raw.takeLast(4)
            "inv_****$suffix"
        }
    }

    fun getPreferredNetworkRoute(): Flow<PreferredNetworkRoute?> {
        return dataStore.data.map { preferences ->
            val targetId = preferences[stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_TARGET)].orEmpty().trim()
            val label = preferences[stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LABEL)].orEmpty().trim()
            val latencyMs = preferences[longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LATENCY_MS)]
            val updatedAtMillis = preferences[longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_UPDATED_AT)]

            if (targetId.isBlank() || label.isBlank() || latencyMs == null || updatedAtMillis == null) {
                null
            } else {
                PreferredNetworkRoute(
                    targetId = targetId,
                    label = label,
                    latencyMs = latencyMs,
                    updatedAtMillis = updatedAtMillis
                )
            }
        }
    }

    suspend fun savePreferredNetworkRoute(route: PreferredNetworkRoute) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_TARGET)] = route.targetId
            preferences[stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LABEL)] = route.label
            preferences[longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LATENCY_MS)] = route.latencyMs
            preferences[longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_UPDATED_AT)] = route.updatedAtMillis
        }
    }

    suspend fun clearPreferredNetworkRoute() {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_TARGET))
            preferences.remove(stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LABEL))
            preferences.remove(longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LATENCY_MS))
            preferences.remove(longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_UPDATED_AT))
        }
    }

    suspend fun clearInviteBinding() {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_INVITE_BOUND)] = false
            preferences.remove(stringPreferencesKey(AIConfig.KEY_INVITE_API_URL))
            preferences.remove(stringPreferencesKey(AIConfig.KEY_INVITE_MODEL))
            preferences.remove(stringPreferencesKey(AIConfig.KEY_INVITE_MODEL_MODE))
            preferences.remove(intPreferencesKey(AIConfig.KEY_INVITE_RPM))
            preferences.remove(stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_TARGET))
            preferences.remove(stringPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LABEL))
            preferences.remove(longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_LATENCY_MS))
            preferences.remove(longPreferencesKey(AIConfig.KEY_PREFERRED_ROUTE_UPDATED_AT))
            // 解绑后回到安全默认：AI 关闭，避免遗留开启状态造成行为不一致
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = false
        }
        securityManager.removeEncryptedString(AIConfig.KEY_INVITE_TOKEN)
        securityManager.removeEncryptedString(AIConfig.KEY_INVITE_CODE)
    }
}

