package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIModelPerformanceRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.InviteGatewayService
import com.example.aiaccounting.data.service.NetworkSpeedTestResult
import com.example.aiaccounting.data.service.NetworkSpeedTestService
import com.example.aiaccounting.data.service.NetworkSpeedTestSummary
import com.example.aiaccounting.data.service.PreferredNetworkRoute
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.OpenAiUrlUtils
import com.example.aiaccounting.utils.DeviceIdProvider
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEFAULT_GATEWAY_BASE_URL = "https://api.gdmon.dpdns.org"
private const val CONNECTION_TEST_REQUIRES_SAVE_MESSAGE = "当前配置尚未保存，请先保存后再测试聊天链路"
private const val NETWORK_WARNING_LATENCY_THRESHOLD_MS = 800L
private val LEGACY_GATEWAY_HOST_KEYWORDS = listOf(
    "workers.dev"
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val aiUsageRepository: AIUsageRepository,
    private val modelPerformanceRepository: AIModelPerformanceRepository,
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

    val preferredNetworkRoute: StateFlow<PreferredNetworkRoute?> = aiConfigRepository
        .getPreferredNetworkRoute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val availableRemoteModels = MutableStateFlow<List<RemoteModel>>(emptyList())

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
        val inviteModelMode: AIConfigRepository.ModelSelectionMode,
        val preferredRoute: PreferredNetworkRoute?
    )

    private fun matchesCurrentPreferredRoute(route: PreferredNetworkRoute, config: AIConfig, gatewayBaseUrlValue: String): Boolean {
        val expectedEndpoint = when (route.targetId) {
            "api" -> config.apiUrl.trim().takeIf { it.isNotBlank() }?.let { OpenAiUrlUtils.models(it) }
            "gateway" -> gatewayBaseUrlValue.trim().takeIf { it.isNotBlank() }?.let { com.example.aiaccounting.utils.UrlUtils.join(it, "bootstrap") }
            else -> null
        }
        return expectedEndpoint != null && expectedEndpoint == route.endpointUrl
    }

    private fun loadAIConfig() {
        viewModelScope.launch {
            val baseConfigFlow = combine(
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
                    inviteModelMode = inviteModelMode,
                    preferredRoute = null
                )
            }

            combine(
                baseConfigFlow,
                aiConfigRepository.getPreferredNetworkRoute(),
                availableRemoteModels,
                gatewayBaseUrl
            ) { baseResult, preferredRoute, remoteModels, currentGatewayBaseUrl ->
                val validatedPreferredRoute = preferredRoute?.takeIf {
                    matchesCurrentPreferredRoute(
                        route = it,
                        config = baseResult.config,
                        gatewayBaseUrlValue = currentGatewayBaseUrl
                    )
                }
                if (preferredRoute != null && validatedPreferredRoute == null) {
                    viewModelScope.launch {
                        aiConfigRepository.clearPreferredNetworkRoute()
                    }
                }
                Quadruple(baseResult.copy(preferredRoute = validatedPreferredRoute), remoteModels, currentGatewayBaseUrl, validatedPreferredRoute)
            }.collect { (result, remoteModels, _, _) ->
                val builtinAvailable = true
                val recommendation = if (remoteModels.isEmpty()) {
                    null
                } else {
                    modelPerformanceRepository.getRecommendation(
                        config = result.config,
                        remoteModelIds = remoteModels.map { it.id }
                    ).first()
                }

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
                            preferredRouteSummary = result.preferredRoute?.toSummary(),
                            recommendedModelSummary = recommendation?.toUiSummary(),
                            isLoading = false,
                            testResult = TestResult.Error("默认配置不可用，请手动配置 API Key")
                        )
                    }
                    refreshNetworkHealthWarning()
                } else {
                    _uiState.update { current ->
                        val effectiveInviteBound = result.inviteBound || current.inviteBound
                        current.copy(
                            useBuiltinConfig = result.useBuiltin,
                            config = result.config,
                            inviteBound = effectiveInviteBound,
                            userModelMode = result.userModelMode,
                            inviteModelMode = result.inviteModelMode,
                            preferredRouteSummary = result.preferredRoute?.toSummary(),
                            recommendedModelSummary = recommendation?.toUiSummary(),
                            isLoading = false
                        )
                    }
                    refreshNetworkHealthWarning()
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

    private fun PreferredNetworkRoute.toSummary(): PreferredRouteSummary {
        return PreferredRouteSummary(
            label = label,
            latencyMs = latencyMs,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun com.example.aiaccounting.data.service.RecommendedModelSummary.toUiSummary(): RecommendedModelUiSummary {
        return RecommendedModelUiSummary(
            modelId = modelId,
            reason = reason,
            latencyMs = latencyMs,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun NetworkSpeedTestSummary.toWarning(): NetworkHealthWarning? {
        val fastestLatency = fastest?.latencyMs
        if (fastestLatency != null && fastestLatency >= NETWORK_WARNING_LATENCY_THRESHOLD_MS) {
            return NetworkHealthWarning(
                level = NetworkHealthWarningLevel.Warning,
                title = "网络延迟较高",
                message = "当前最快节点延迟 ${fastestLatency}ms，云端 AI 响应可能变慢。",
                observedAtMillis = System.currentTimeMillis()
            )
        }

        if (fastest == null) {
            val errorResult = targets.filterIsInstance<NetworkSpeedTestResult.Error>().firstOrNull()
            val message = errorResult?.message ?: errorMessage ?: "网络测速失败，请稍后重试"
            return NetworkHealthWarning(
                level = NetworkHealthWarningLevel.Error,
                title = "网络状态异常",
                message = message,
                observedAtMillis = System.currentTimeMillis()
            )
        }

        return null
    }

    private fun buildNetworkHealthWarning(
        isNetworkAvailable: Boolean,
        preferredRouteSummary: PreferredRouteSummary?,
        latestSpeedTestWarning: NetworkHealthWarning?
    ): NetworkHealthWarning? {
        if (!isNetworkAvailable) {
            return NetworkHealthWarning(
                level = NetworkHealthWarningLevel.Error,
                title = "网络不可用",
                message = "当前处于离线模式，AI 助手将优先使用本地解析。",
                observedAtMillis = System.currentTimeMillis()
            )
        }

        if (latestSpeedTestWarning != null) {
            return latestSpeedTestWarning
        }

        val preferredLatency = preferredRouteSummary?.latencyMs ?: return null
        if (preferredLatency < NETWORK_WARNING_LATENCY_THRESHOLD_MS) {
            return null
        }

        return NetworkHealthWarning(
            level = NetworkHealthWarningLevel.Warning,
            title = "推荐线路延迟偏高",
            message = "最近一次测速推荐线路延迟 ${preferredLatency}ms，建议重新测速或稍后再试。",
            observedAtMillis = preferredRouteSummary.updatedAtMillis
        )
    }

    private fun refreshNetworkHealthWarning() {
        _uiState.update { current ->
            current.copy(
                networkHealthWarning = buildNetworkHealthWarning(
                    isNetworkAvailable = current.isNetworkAvailable,
                    preferredRouteSummary = current.preferredRouteSummary,
                    latestSpeedTestWarning = current.latestSpeedTestWarning
                )
            )
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
            refreshNetworkHealthWarning()
        }
    }

    fun refreshNetworkStatus() {
        checkNetworkStatus()
    }

    fun updateProvider(provider: AIProvider) {
        // Invite-bound config is independent from user config. Updating provider should not clear binding.
        val defaultConfig = AIConfig.defaultFor(provider)
        viewModelScope.launch {
            aiConfigRepository.clearPreferredNetworkRoute()
            modelPerformanceRepository.clearForConfig(_uiState.value.config)
        }
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    provider = provider,
                    apiUrl = defaultConfig.apiUrl,
                    model = defaultConfig.model
                ),
                preferredRouteSummary = null,
                recommendedModelSummary = null,
                networkSpeedTestResult = null,
                latestSpeedTestWarning = null,
                networkHealthWarning = null
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
        val currentConfig = _uiState.value.config
        viewModelScope.launch {
            aiConfigRepository.clearPreferredNetworkRoute()
            modelPerformanceRepository.clearForConfig(currentConfig)
        }
        _uiState.update {
            it.copy(
                config = it.config.copy(apiUrl = url),
                preferredRouteSummary = null,
                recommendedModelSummary = null,
                networkSpeedTestResult = null,
                latestSpeedTestWarning = null,
                networkHealthWarning = null
            )
        }
    }

    fun updateModel(model: String) {
        // 邀请码绑定状态下允许选择模型，不应清除绑定
        val cleaned = model.trim()
        val state = _uiState.value

        if (cleaned.isNotBlank()) {
            viewModelScope.launch {
                val inviteBound = state.inviteBound || aiConfigRepository.getInviteBound().first()
                if (!inviteBound &&
                    (state.userModelMode == AIConfigRepository.ModelSelectionMode.AUTO || state.config.model.isBlank())) {
                    aiConfigRepository.setModelSelectionMode(AIConfigRepository.ModelSelectionMode.FIXED)
                    _uiState.update { current ->
                        current.copy(userModelMode = AIConfigRepository.ModelSelectionMode.FIXED)
                    }
                }
            }
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
        viewModelScope.launch {
            val inviteBound = _uiState.value.inviteBound || aiConfigRepository.getInviteBound().first()
            val useBuiltin = _uiState.value.useBuiltinConfig || aiConfigRepository.getUseBuiltin().first()

            // Invite-bound config is persisted separately (token/apiUrl/model mode). Saving user config here must not
            // accidentally persist invite token into the user's API key storage.
            // If builtin is active, invite binding is kept but not effective; allow saving user config.
            if (inviteBound && !useBuiltin) return@launch

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
            val inviteBound = _uiState.value.inviteBound || aiConfigRepository.getInviteBound().first()

            if (useBuiltin) {
                _uiState.update {
                    it.copy(
                        useBuiltinConfig = true,
                        inviteBound = inviteBound,
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
                        inviteBound = inviteBound,
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
                        networkSpeedTestResult = NetworkSpeedTestUiResult.Error("网络不可用，请检查网络连接"),
                        latestSpeedTestWarning = NetworkHealthWarning(
                            level = NetworkHealthWarningLevel.Error,
                            title = "网络不可用",
                            message = "当前处于离线模式，AI 助手将优先使用本地解析。",
                            observedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                refreshNetworkHealthWarning()
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

            val latestWarning = summary.toWarning()
            val nextResult = summary.fastest?.let { fastest ->
                val preferredRoute = PreferredNetworkRoute(
                    targetId = fastest.target.id,
                    label = fastest.target.label,
                    latencyMs = fastest.latencyMs,
                    updatedAtMillis = System.currentTimeMillis(),
                    endpointUrl = fastest.target.url
                )
                aiConfigRepository.savePreferredNetworkRoute(preferredRoute)
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
                    networkSpeedTestResult = nextResult,
                    latestSpeedTestWarning = latestWarning
                )
            }
            refreshNetworkHealthWarning()
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

            val draftConfig = _uiState.value.config
            val effectiveConfig = aiConfigRepository.getAIConfig().first()
            if (draftConfig != effectiveConfig) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = TestResult.Error(CONNECTION_TEST_REQUIRES_SAVE_MESSAGE)
                    )
                }
                return@launch
            }

            // 使用与聊天主链一致的生效配置进行真实聊天链路测试
            val errorMessage = aiService.testChatPath(effectiveConfig)
            val testResult = if (errorMessage == null) {
                TestResult.Success
            } else {
                TestResult.Error(buildActionableChatPathError(errorMessage, effectiveConfig))
            }

            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = testResult
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

    private fun buildActionableChatPathError(errorMessage: String, config: AIConfig): String {
        val routeLabel = if (config.model.isBlank()) "AUTO" else "FIXED"
        val modelLabel = config.model.ifBlank { "AUTO" }
        val diagnostics = "（provider=${config.provider.name}, model=$modelLabel, route=$routeLabel）"
        val safeMessage = errorMessage.trim().ifBlank { "聊天链路测试失败" }
        val nextStep = when {
            safeMessage.contains("API密钥") -> "建议：保存配置后重试聊天链路，或检查 API Key。"
            safeMessage.contains("模型") -> "建议：重试聊天链路，或切换模型后再测。"
            safeMessage.contains("超时") || safeMessage.contains("网络") -> "建议：重试聊天链路，或稍后再试。"
            else -> "建议：重试聊天链路，必要时保存配置并切换模型。"
        }
        return "$safeMessage $diagnostics\n$nextStep"
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
            availableRemoteModels.value = models

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
            aiConfigRepository.clearPreferredNetworkRoute()
        }
        _uiState.update {
            it.copy(
                preferredRouteSummary = null,
                networkSpeedTestResult = null,
                latestSpeedTestWarning = null,
                networkHealthWarning = null
            )
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
    val preferredRouteSummary: PreferredRouteSummary? = null,
    val recommendedModelSummary: RecommendedModelUiSummary? = null,
    val latestSpeedTestWarning: NetworkHealthWarning? = null,
    val networkHealthWarning: NetworkHealthWarning? = null,
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

data class PreferredRouteSummary(
    val label: String,
    val latencyMs: Long,
    val updatedAtMillis: Long
)

data class RecommendedModelUiSummary(
    val modelId: String,
    val reason: String,
    val latencyMs: Long?,
    val updatedAtMillis: Long
)

enum class NetworkHealthWarningLevel {
    Warning,
    Error
}

data class NetworkHealthWarning(
    val level: NetworkHealthWarningLevel,
    val title: String,
    val message: String,
    val observedAtMillis: Long
)

sealed class InviteBindResult {
    data class Success(
        val apiBaseUrl: String,
        val rpm: Int,
        val hint: String = "已自动启用，可返回 AI 助手页面直接使用"
    ) : InviteBindResult()

    data class Error(val message: String) : InviteBindResult()
}
