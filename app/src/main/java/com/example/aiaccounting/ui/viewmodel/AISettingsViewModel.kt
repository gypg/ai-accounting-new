package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.InviteGatewayService
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.DeviceIdProvider
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEFAULT_GATEWAY_BASE_URL = "https://api.gdmon.dpdns.org"

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val aiUsageRepository: AIUsageRepository,
    private val networkUtils: NetworkUtils,
    private val inviteGatewayService: InviteGatewayService,
    private val deviceIdProvider: DeviceIdProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISettingsUiState())
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    val gatewayBaseUrl: StateFlow<String> = aiConfigRepository
        .getGatewayBaseUrl(defaultValue = DEFAULT_GATEWAY_BASE_URL)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DEFAULT_GATEWAY_BASE_URL
        )

    val inviteApiBaseUrl: StateFlow<String> = aiConfigRepository
        .getInviteApiBaseUrl()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val inviteRpm: StateFlow<Int> = aiConfigRepository
        .getInviteRpm()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val inviteCodeMasked: StateFlow<String> = aiConfigRepository
        .getInviteCodeMasked()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    init {
        viewModelScope.launch {
            aiConfigRepository.migrateLegacyApiKeyIfNeeded()
        }
        loadAIConfig()
        loadUsageStats()
        checkNetworkStatus()
    }

    private data class AIConfigLoadResult(
        val useBuiltin: Boolean,
        val inviteBound: Boolean,
        val config: AIConfig,
        val userModelMode: AIConfigRepository.ModelSelectionMode,
        val inviteModelMode: AIConfigRepository.ModelSelectionMode
    )

    private fun loadAIConfig() {
        viewModelScope.launch {
            combine(
                aiConfigRepository.getUseBuiltin(),
                aiConfigRepository.getInviteBound(),
                aiConfigRepository.getAIConfig(),
                aiConfigRepository.getModelSelectionMode(),
                aiConfigRepository.getInviteModelSelectionMode()
            ) { useBuiltin, bound, config, userModelMode, inviteModelMode ->
                AIConfigLoadResult(
                    useBuiltin = useBuiltin,
                    inviteBound = bound,
                    config = config,
                    userModelMode = userModelMode,
                    inviteModelMode = inviteModelMode
                )
            }.collect { result ->
                val builtinAvailable = true

                if (result.useBuiltin && !builtinAvailable) {
                    // 防止历史状态把用户困在“内置开启但不可用”
                    aiConfigRepository.setUseBuiltin(false)

                    val effectiveInviteBound = result.inviteBound || _uiState.value.inviteBound

                    _uiState.value = _uiState.value.copy(
                        useBuiltinConfig = false,
                        config = result.config,
                        inviteBound = effectiveInviteBound,
                        userModelMode = result.userModelMode,
                        inviteModelMode = result.inviteModelMode,
                        isLoading = false,
                        testResult = TestResult.Error("默认配置不可用，请手动配置 API Key")
                    )
                } else {
                    val effectiveInviteBound = result.inviteBound || _uiState.value.inviteBound

                    _uiState.value = _uiState.value.copy(
                        useBuiltinConfig = result.useBuiltin,
                        config = result.config,
                        inviteBound = effectiveInviteBound,
                        userModelMode = result.userModelMode,
                        inviteModelMode = result.inviteModelMode,
                        isLoading = false
                    )
                }
            }
        }

        // 进入设置页时自动修正网关地址（避免 workers.dev 在模拟器超时）
        viewModelScope.launch {
            val current = gatewayBaseUrl.value.trim()
            if (shouldAutoFixGatewayBaseUrl(current)) {
                aiConfigRepository.setGatewayBaseUrl(DEFAULT_GATEWAY_BASE_URL)
            }
        }
    }

    private fun shouldAutoFixGatewayBaseUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return true
        return trimmed.contains("workers.dev", ignoreCase = true)
    }

    private fun loadUsageStats() {
        viewModelScope.launch {
            aiUsageRepository.getUsageStats().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    usageStats = stats
                )
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            val isAvailable = networkUtils.isNetworkAvailable()
            _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
        }
    }

    fun refreshNetworkStatus() {
        checkNetworkStatus()
    }

    fun updateProvider(provider: AIProvider) {
        // Invite-bound config is independent from user config. Updating provider should not clear binding.
        val defaultConfig = AIConfig.defaultFor(provider)
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(
                provider = provider,
                apiUrl = defaultConfig.apiUrl,
                model = defaultConfig.model
            )
        )
    }

    fun updateApiKey(apiKey: String) {
        // Invite-bound config is independent from user config. Updating apiKey should not clear binding.
        // 清理API密钥中的换行符和空格
        val cleanedKey = apiKey.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(apiKey = cleanedKey)
        )
    }

    fun updateApiUrl(url: String) {
        // Invite-bound config is independent from user config. Updating apiUrl should not clear binding.
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(apiUrl = url)
        )
    }

    fun updateModel(model: String) {
        // 邀请码绑定状态下允许选择模型，不应清除绑定
        val cleaned = model.trim()

        // 普通用户：在 AUTO 模式下手动选择模型，切换回 FIXED（但不影响邀请码模式）
        if (!_uiState.value.inviteBound && _uiState.value.userModelMode == AIConfigRepository.ModelSelectionMode.AUTO && cleaned.isNotBlank()) {
            updateUserModelMode(AIConfigRepository.ModelSelectionMode.FIXED)
        }

        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(model = cleaned)
        )
    }

    fun updateInviteModel(modelId: String) {
        val cleaned = modelId.trim()
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            // 持久化到 invite 专用字段，避免覆盖普通用户 KEY_MODEL
            aiConfigRepository.updateInviteModel(cleaned)
        }

        // 立即更新 UI（config 会在 DataStore 流回传后最终一致）
        _uiState.update { it.copy(config = it.config.copy(model = cleaned)) }
    }

    fun updateInviteModelMode(mode: AIConfigRepository.ModelSelectionMode) {
        viewModelScope.launch {
            aiConfigRepository.setInviteModelSelectionMode(mode)
        }

        _uiState.update {
            val nextConfig = if (mode == AIConfigRepository.ModelSelectionMode.AUTO) {
                it.config.copy(model = "")
            } else {
                it.config
            }
            it.copy(inviteModelMode = mode, config = nextConfig)
        }
    }

    fun updateUserModelMode(mode: AIConfigRepository.ModelSelectionMode) {
        viewModelScope.launch {
            aiConfigRepository.setModelSelectionMode(mode)
        }

        _uiState.update {
            val nextConfig = if (mode == AIConfigRepository.ModelSelectionMode.AUTO) {
                it.config.copy(model = "")
            } else {
                it.config
            }
            it.copy(userModelMode = mode, config = nextConfig)
        }
    }

    fun updateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(isEnabled = enabled)
        )
    }

    fun saveConfig() {
        // Invite-bound config is persisted separately (token/apiUrl/model mode). Saving user config here must not
        // accidentally persist invite token into the user's API key storage.
        // If builtin is active, invite binding is kept but not effective; allow saving user config.
        if (_uiState.value.inviteBound && !_uiState.value.useBuiltinConfig) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            aiConfigRepository.saveAIConfig(_uiState.value.config)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    /**
     * 切换是否使用内置配置
     * 当启用默认模型时，使用内置配置
     * 当取消默认模型时，清空API配置，让用户手动输入
     */
    fun toggleBuiltinConfig(useBuiltin: Boolean) {
        viewModelScope.launch {
            aiConfigRepository.setUseBuiltin(useBuiltin)

            if (useBuiltin) {
                _uiState.value = _uiState.value.copy(
                    useBuiltinConfig = true,
                    config = _uiState.value.config.copy(
                        provider = AIConfig.BUILTIN_CONFIG.provider,
                        apiUrl = AIConfig.BUILTIN_CONFIG.apiUrl,
                        model = AIConfig.BUILTIN_CONFIG.model,
                        isEnabled = true
                    )
                )
            } else {
                // 关闭默认模型：恢复为用户可编辑态（不清空邀请码绑定/不覆盖持久化 token）
                _uiState.value = _uiState.value.copy(
                    useBuiltinConfig = false
                )
            }
        }
    }

    fun applyBuiltinPresetWithApiKey(apiKey: String) {
        val cleanedKey = apiKey.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        viewModelScope.launch {
            aiConfigRepository.updateApiKey(cleanedKey)
            aiConfigRepository.setUseBuiltin(true)

            val nextConfig = _uiState.value.config.copy(
                provider = AIConfig.BUILTIN_CONFIG.provider,
                apiKey = cleanedKey,
                apiUrl = AIConfig.BUILTIN_CONFIG.apiUrl,
                model = AIConfig.BUILTIN_CONFIG.model,
                isEnabled = true
            )

            _uiState.value = _uiState.value.copy(
                useBuiltinConfig = true,
                config = nextConfig
            )

            aiConfigRepository.saveAIConfig(nextConfig)
            _uiState.value = _uiState.value.copy(saveSuccess = true)
        }
    }

    fun applyBuiltinPreset() {
        toggleBuiltinConfig(true)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("网络不可用，请检查网络连接")
                )
                return@launch
            }

            // 使用AI服务进行真正的连接测试
            val errorMessage = aiService.testConnection(_uiState.value.config)

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = if (errorMessage == null) {
                    TestResult.Success
                } else {
                    TestResult.Error(errorMessage)
                }
            )
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }

    /**
     * 获取远程模型列表
     */
    fun fetchRemoteModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingModels = true)

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.value = _uiState.value.copy(
                    isFetchingModels = false,
                    testResult = TestResult.Error("网络不可用，无法获取模型列表")
                )
                return@launch
            }

            // 获取模型列表
            val models = aiService.fetchModels(_uiState.value.config)

            _uiState.value = _uiState.value.copy(
                isFetchingModels = false,
                remoteModels = models,
                testResult = if (models.isEmpty()) {
                    TestResult.Error("无法获取模型列表，请检查API配置")
                } else {
                    null
                }
            )
        }
    }

    /**
     * 重置使用统计
     */
    fun resetUsageStats() {
        viewModelScope.launch {
            aiUsageRepository.resetStats()
        }
    }

    fun bindInviteCode(inviteCode: String, gatewayBaseUrl: String) {
        val cleanedInvite = inviteCode.trim()
        if (cleanedInvite.isBlank()) {
            _uiState.update { it.copy(inviteBindResult = InviteBindResult.Error("请输入邀请码")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBindingInvite = true, inviteBindResult = null) }

            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isBindingInvite = false,
                        inviteBindResult = InviteBindResult.Error("网络不可用，请检查网络连接")
                    )
                }
                return@launch
            }

            val deviceId = deviceIdProvider.getStableDeviceId().trim()
            if (deviceId.isBlank()) {
                _uiState.update {
                    it.copy(
                        isBindingInvite = false,
                        inviteBindResult = InviteBindResult.Error("无法获取设备标识，请重试")
                    )
                }
                return@launch
            }

            val rawGateway = gatewayBaseUrl.trim()
            val gateway = if (shouldAutoFixGatewayBaseUrl(rawGateway)) DEFAULT_GATEWAY_BASE_URL else rawGateway

            when (val result = inviteGatewayService.bootstrap(
                inviteCode = cleanedInvite,
                deviceId = deviceId,
                gatewayBaseUrl = gateway
            )) {
                is InviteGatewayService.BootstrapResult.Success -> {
                    try {
                        // Remember the gateway base URL on successful bind
                        aiConfigRepository.setGatewayBaseUrl(gateway)

                        aiConfigRepository.setInviteModelSelectionMode(AIConfigRepository.ModelSelectionMode.AUTO)

                        aiConfigRepository.saveInviteBinding(
                            inviteCode = cleanedInvite,
                            token = result.token,
                            apiBaseUrl = result.apiBaseUrl,
                            model = "openai/gpt-oss-120b",
                            rpm = result.rpm
                        )

                        val nextConfig = _uiState.value.config.copy(
                            provider = AIProvider.CUSTOM,
                            apiKey = result.token,
                            apiUrl = result.apiBaseUrl,
                            model = "",
                            isEnabled = true
                        )

                        _uiState.update {
                            it.copy(
                                config = nextConfig,
                                inviteBound = true,
                                isBindingInvite = false,
                                inviteBindResult = InviteBindResult.Success(
                                    apiBaseUrl = result.apiBaseUrl,
                                    rpm = result.rpm
                                )
                            )
                        }
                    } catch (_: Exception) {
                        _uiState.update {
                            it.copy(
                                isBindingInvite = false,
                                inviteBindResult = InviteBindResult.Error("保存配置失败，请重试")
                            )
                        }
                    }
                }

                is InviteGatewayService.BootstrapResult.ApiError -> {
                    val message = when (result.code) {
                        "invalid_inviteCode" -> "邀请码无效"
                        "invite_revoked" -> "邀请码已被撤销"
                        "invite_already_used" -> "邀请码已被其他设备使用"
                        "missing_deviceId" -> "设备标识缺失"
                        "rate_limited" -> "请求过于频繁，请稍后再试"
                        else -> "绑定失败，请检查邀请码是否正确"
                    }

                    _uiState.update {
                        it.copy(
                            isBindingInvite = false,
                            inviteBindResult = InviteBindResult.Error(message)
                        )
                    }
                }

                is InviteGatewayService.BootstrapResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isBindingInvite = false,
                            inviteBindResult = InviteBindResult.Error(result.message)
                        )
                    }
                }
            }
        }
    }

    fun clearInviteBinding() {
        viewModelScope.launch {
            try {
                aiConfigRepository.clearInviteBinding()
                _uiState.update {
                    it.copy(
                        config = it.config.copy(
                            provider = AIProvider.CUSTOM,
                            apiKey = "",
                            apiUrl = "",
                            model = "",
                            isEnabled = false
                        ),
                        inviteBindResult = null,
                        isBindingInvite = false,
                        inviteBound = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        inviteBindResult = InviteBindResult.Error("解除绑定失败，请重试"),
                        isBindingInvite = false
                    )
                }
            }
        }
    }

    fun clearInviteBindResult() {
        _uiState.update { it.copy(inviteBindResult = null) }
    }

    fun setGatewayBaseUrl(value: String) {
        val cleaned = value.trim()
        viewModelScope.launch {
            aiConfigRepository.setGatewayBaseUrl(cleaned)
        }
    }
}

data class AISettingsUiState(
    val config: AIConfig = AIConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: TestResult? = null,
    val usageStats: AIUsageStats = AIUsageStats(),
    val isNetworkAvailable: Boolean = true,
    val isFetchingModels: Boolean = false,
    val remoteModels: List<RemoteModel> = emptyList(),
    val useBuiltinConfig: Boolean = false, // 是否使用内置配置
    val isBuiltinAvailable: Boolean = true,
    val isBindingInvite: Boolean = false,
    val inviteBindResult: InviteBindResult? = null,
    val inviteBound: Boolean = false,
    val userModelMode: AIConfigRepository.ModelSelectionMode = AIConfigRepository.ModelSelectionMode.FIXED,
    val inviteModelMode: AIConfigRepository.ModelSelectionMode = AIConfigRepository.ModelSelectionMode.AUTO
)

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}

sealed class InviteBindResult {
    data class Success(
        val apiBaseUrl: String,
        val rpm: Int,
        val hint: String = "已自动启用，可返回 AI 助手页面直接使用"
    ) : InviteBindResult()

    data class Error(val message: String) : InviteBindResult()
}
