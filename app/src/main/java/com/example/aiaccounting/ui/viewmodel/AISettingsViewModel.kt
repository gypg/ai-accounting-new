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
import com.example.aiaccounting.data.service.NetworkSpeedTestResult
import com.example.aiaccounting.data.service.NetworkSpeedTestService
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.DeviceIdProvider
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEFAULT_GATEWAY_BASE_URL = "https://api.gdmon.dpdns.org"
private val LEGACY_GATEWAY_HOST_KEYWORDS = listOf(
    "workers.dev"
)

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val aiUsageRepository: AIUsageRepository,
    private val networkUtils: NetworkUtils,
    private val inviteGatewayService: InviteGatewayService,
    private val deviceIdProvider: DeviceIdProvider,
    private val networkSpeedTestService: NetworkSpeedTestService? = null
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
            aiConfigRepository.migrateInviteGatewayConfigIfNeeded(
                defaultGatewayBaseUrl = DEFAULT_GATEWAY_BASE_URL,
                legacyGatewayHostKeywords = LEGACY_GATEWAY_HOST_KEYWORDS
            )
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

                    _uiState.update { current ->
                        val effectiveInviteBound = result.inviteBound || current.inviteBound
                        current.copy(
                            useBuiltinConfig = false,
                            config = result.config,
                            inviteBound = effectiveInviteBound,
                            userModelMode = result.userModelMode,
                            inviteModelMode = result.inviteModelMode,
                            isLoading = false,
                            testResult = TestResult.Error("默认配置不可用，请手动配置 API Key")
                        )
                    }
                } else {
                    _uiState.update { current ->
                        val effectiveInviteBound = result.inviteBound || current.inviteBound
                        current.copy(
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
        }

        // 进入设置页时仅在为空或命中旧部署域名时切到新默认网关，避免覆盖用户自定义地址
        viewModelScope.launch {
            val current = aiConfigRepository.getGatewayBaseUrl(defaultValue = DEFAULT_GATEWAY_BASE_URL).first().trim()
            if (shouldAutoFixGatewayBaseUrl(current)) {
                aiConfigRepository.setGatewayBaseUrl(DEFAULT_GATEWAY_BASE_URL)
            }
        }
    }

    private fun shouldAutoFixGatewayBaseUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return true
        return LEGACY_GATEWAY_HOST_KEYWORDS.any { keyword ->
            trimmed.contains(keyword, ignoreCase = true)
        }
    }

    private fun loadUsageStats() {
        viewModelScope.launch {
            aiUsageRepository.getUsageStats().collect { stats ->
                _uiState.update { it.copy(usageStats = stats) }
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
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    provider = provider,
                    apiUrl = defaultConfig.apiUrl,
                    model = defaultConfig.model
                )
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        // Invite-bound config is independent from user config. Updating apiKey should not clear binding.
        // 清理API密钥中的换行符和空格
        val cleanedKey = apiKey.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        _uiState.update {
            it.copy(config = it.config.copy(apiKey = cleanedKey))
        }
    }

    fun updateApiUrl(url: String) {
        // Invite-bound config is independent from user config. Updating apiUrl should not clear binding.
        _uiState.update {
            it.copy(config = it.config.copy(apiUrl = url))
        }
    }

    fun updateModel(model: String) {
        // 邀请码绑定状态下允许选择模型，不应清除绑定
        val cleaned = model.trim()

        // 普通用户：在 AUTO 模式下手动选择模型，切换回 FIXED（但不影响邀请码模式）
        val state = _uiState.value
        if (!state.inviteBound && state.userModelMode == AIConfigRepository.ModelSelectionMode.AUTO && cleaned.isNotBlank()) {
            updateUserModelMode(AIConfigRepository.ModelSelectionMode.FIXED)
        }

        _uiState.update {
            it.copy(config = it.config.copy(model = cleaned))
        }
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
        _uiState.update {
            it.copy(config = it.config.copy(isEnabled = enabled))
        }
    }

    fun saveConfig() {
        // Invite-bound config is persisted separately (token/apiUrl/model mode). Saving user config here must not
        // accidentally persist invite token into the user's API key storage.
        // If builtin is active, invite binding is kept but not effective; allow saving user config.
        if (_uiState.value.inviteBound && !_uiState.value.useBuiltinConfig) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val config = _uiState.value.config
            aiConfigRepository.saveAIConfig(config)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
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
                _uiState.update {
                    it.copy(
                        useBuiltinConfig = true,
                        config = it.config.copy(
                            provider = AIConfig.BUILTIN_CONFIG.provider,
                            apiUrl = AIConfig.BUILTIN_CONFIG.apiUrl,
                            model = AIConfig.BUILTIN_CONFIG.model,
                            isEnabled = true
                        )
                    )
                }
            } else {
                // 关闭默认模型：恢复为用户可编辑态（不清空邀请码绑定/不覆盖持久化 token），清理内置的 key/url
                _uiState.update { current ->
                    val isBuiltinUrl = current.config.apiUrl == AIConfig.BUILTIN_CONFIG.apiUrl
                    current.copy(
                        useBuiltinConfig = false,
                        config = current.config.copy(
                            apiKey = if (isBuiltinUrl) "" else current.config.apiKey,
                            apiUrl = if (isBuiltinUrl) "" else current.config.apiUrl
                        )
                    )
                }
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

            _uiState.update {
                it.copy(
                    useBuiltinConfig = true,
                    config = nextConfig
                )
            }

            aiConfigRepository.saveAIConfig(nextConfig)
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    fun applyBuiltinPreset() {
        toggleBuiltinConfig(true)
    }

    fun runNetworkSpeedTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingNetworkSpeed = true, networkSpeedTestResult = null) }

            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isTestingNetworkSpeed = false,
                        networkSpeedTestResult = NetworkSpeedTestUiResult.Error("网络不可用，请检查网络连接")
                    )
                }
                return@launch
            }

            val service = networkSpeedTestService
            if (service == null) {
                _uiState.update {
                    it.copy(
                        isTestingNetworkSpeed = false,
                        networkSpeedTestResult = NetworkSpeedTestUiResult.Error("网络测速服务不可用")
                    )
                }
                return@launch
            }

            val summary = service.test(
                config = _uiState.value.config,
                gatewayBaseUrl = gatewayBaseUrl.value
            )

            val nextResult = summary.fastest?.let { fastest ->
                NetworkSpeedTestUiResult.Success(
                    fastestLabel = fastest.target.label,
                    latencyMs = fastest.latencyMs,
                    measuredTargets = summary.targets.map { targetResult ->
                        when (targetResult) {
                            is NetworkSpeedTestResult.Success -> NetworkSpeedTestMeasuredTarget(
                                label = targetResult.target.label,
                                latencyMs = targetResult.latencyMs,
                                message = "HTTP ${targetResult.statusCode}"
                            )
                            is NetworkSpeedTestResult.Error -> NetworkSpeedTestMeasuredTarget(
                                label = targetResult.target.label,
                                latencyMs = null,
                                message = targetResult.message
                            )
                        }
                    }
                )
            } ?: NetworkSpeedTestUiResult.Error(summary.errorMessage ?: "网络测速失败")

            _uiState.update {
                it.copy(
                    isTestingNetworkSpeed = false,
                    networkSpeedTestResult = nextResult
                )
            }
        }
    }

    fun clearNetworkSpeedTestResult() {
        _uiState.update { it.copy(networkSpeedTestResult = null) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = TestResult.Error("网络不可用，请检查网络连接")
                    )
                }
                return@launch
            }

            // 使用AI服务进行真正的连接测试
            val config = _uiState.value.config
            val errorMessage = aiService.testConnection(config)

            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = if (errorMessage == null) {
                        TestResult.Success
                    } else {
                        TestResult.Error(errorMessage)
                    }
                )
            }
        }
    }

    /**
     * 测试指定模型的连接
     */
    fun testModelConnection(modelId: String, onResult: (com.example.aiaccounting.ui.screens.ModelTestResult) -> Unit) {
        viewModelScope.launch {
            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                onResult(com.example.aiaccounting.ui.screens.ModelTestResult.Error("网络不可用，请检查网络连接"))
                return@launch
            }

            val config = _uiState.value.config.copy(model = modelId)
            val startTime = System.currentTimeMillis()
            val errorMessage = aiService.testConnection(config)
            val latency = System.currentTimeMillis() - startTime

            if (errorMessage == null) {
                onResult(com.example.aiaccounting.ui.screens.ModelTestResult.Success(latency))
            } else {
                onResult(com.example.aiaccounting.ui.screens.ModelTestResult.Error(errorMessage))
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }

    /**
     * 获取远程模型列表
     */
    fun fetchRemoteModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true) }

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        testResult = TestResult.Error("网络不可用，无法获取模型列表")
                    )
                }
                return@launch
            }

            // 获取模型列表
            val config = _uiState.value.config
            val models = aiService.fetchModels(config)

            _uiState.update {
                it.copy(
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
                            model = "", // 空字符串表示启用自动优选模型
                            rpm = result.rpm
                        )

                        val nextConfig = _uiState.value.config.copy(
                            provider = AIProvider.CUSTOM,
                            apiKey = result.token,
                            apiUrl = result.apiBaseUrl,
                            model = "", // 空字符串表示启用自动优选模型
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
    val isTestingNetworkSpeed: Boolean = false,
    val networkSpeedTestResult: NetworkSpeedTestUiResult? = null,
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

sealed class NetworkSpeedTestUiResult {
    data class Success(
        val fastestLabel: String,
        val latencyMs: Long,
        val measuredTargets: List<NetworkSpeedTestMeasuredTarget>
    ) : NetworkSpeedTestUiResult()

    data class Error(val message: String) : NetworkSpeedTestUiResult()
}

data class NetworkSpeedTestMeasuredTarget(
    val label: String,
    val latencyMs: Long?,
    val message: String
)

sealed class InviteBindResult {
    data class Success(
        val apiBaseUrl: String,
        val rpm: Int,
        val hint: String = "已自动启用，可返回 AI 助手页面直接使用"
    ) : InviteBindResult()

    data class Error(val message: String) : InviteBindResult()
}
