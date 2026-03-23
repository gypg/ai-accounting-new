package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.AILocalProcessor
import com.example.aiaccounting.ai.AIMessageParser
import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.ButlerRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.ChatSessionRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Assistant
 * Enhanced with AI service integration, network detection, and usage statistics
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val conversationRepository: AIConversationRepository,
    private val aiOperationExecutor: AIOperationExecutor,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val aiService: AIService,
    private val aiConfigRepository: AIConfigRepository,
    private val aiUsageRepository: AIUsageRepository,
    private val networkUtils: NetworkUtils,
    private val imageProcessingService: ImageProcessingService,
    private val chatSessionRepository: ChatSessionRepository,
    private val butlerRepository: ButlerRepository,
    private val aiReasoningEngine: AIReasoningEngine,
    private val transactionModificationHandler: TransactionModificationHandler,
    private val aiLocalProcessor: AILocalProcessor,
    private val actionExecutor: AIAssistantActionExecutor
) : ViewModel() {

    private val butlerCoordinator = AIAssistantButlerCoordinator(butlerRepository)
    private val configNetworkCoordinator = AIAssistantConfigNetworkCoordinator(
        aiConfigRepository = aiConfigRepository,
        networkUtils = networkUtils
    )
    private val sessionCoordinator = AIAssistantSessionCoordinator(chatSessionRepository)
    private val messageOrchestrator = AIAssistantMessageOrchestrator()
    private val messageExecutionCoordinator = AIAssistantMessageExecutionCoordinator(
        aiReasoningEngine = aiReasoningEngine,
        messageOrchestrator = messageOrchestrator
    )
    private val modificationCoordinator = AIAssistantModificationCoordinator(transactionModificationHandler)
    private val pendingModificationLifecycle = AIAssistantPendingModificationLifecycle(modificationCoordinator)
    private val pendingClarificationLifecycle = AIAssistantPendingClarificationLifecycle(AIMessageParser())
    private val imageMessageHandler = AIAssistantImageMessageHandler(
        aiService = aiService,
        imageProcessingService = imageProcessingService,
        aiUsageRepository = aiUsageRepository
    )
    private val remoteResponseInterpreter = AIAssistantRemoteResponseInterpreter()
    private val remoteResponseIntegrityChecker = AIAssistantRemoteResponseIntegrityChecker()
    private val remoteStreamCollector = AIAssistantRemoteStreamCollector(
        sendChatStream = aiService::sendChatStream,
        recordUsageFailure = { aiUsageRepository.recordCall(success = false) }
    )
    private val remoteExecutionHandler = AIAssistantRemoteExecutionHandler(
        streamCollector = remoteStreamCollector,
        integrityChecker = remoteResponseIntegrityChecker,
        interpreter = remoteResponseInterpreter,
        recordUsageSuccess = { aiUsageRepository.recordCall(success = true) },
        recordUsageFailure = { aiUsageRepository.recordCall(success = false) }
    )
    private val promptBuilder = AIAssistantPromptBuilder()
    private val commandHandler = AIAssistantCommandHandler(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        aiOperationExecutor = aiOperationExecutor
    )

    private var switchSessionJob: Job? = null

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    val conversations = conversationRepository.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 会话列表
    val sessions = chatSessionRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当前AI配置
    private var currentAIConfig: AIConfig = AIConfig()
    private var currentUseBuiltinConfig: Boolean = false

    init {
        viewModelScope.launch {
            aiConfigRepository.migrateLegacyApiKeyIfNeeded()
        }
        loadAIConfig()
        checkNetworkStatus()
        startObservingCurrentButler()
    }

    /**
     * 真正“观察”当前管家：监听选中 id 变化，并用 collectLatest 避免并发乱序。
     */
    private fun startObservingCurrentButler() {
        viewModelScope.launch {
            butlerCoordinator.observeCurrentButler()
                .collectLatest { butler ->
                    try {
                        _uiState.update { it.copy(currentButler = butler) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                        // 兜底：不阻塞启动流程
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
        }
    }

    /**
     * 切换管家
     */
    fun switchButler(butlerId: String) {
        butlerCoordinator.switchButler(butlerId)
    }

    /**
     * 获取所有可用管家
     */
    fun getAllButlers() = butlerCoordinator.getAllButlers()

    private fun loadAIConfig() {
        viewModelScope.launch {
            configNetworkCoordinator.observeConfig().collect { (config, useBuiltin) ->
                currentAIConfig = config
                currentUseBuiltinConfig = useBuiltin
                _uiState.update {
                    it.copy(
                        isAIConfigured = config.isEnabled && config.apiKey.isNotBlank(),
                        aiConfig = config
                    )
                }
            }
        }
    }

    /**
     * 检查网络状态
     */
    private fun checkNetworkStatus() {
        viewModelScope.launch {
            val isAvailable = configNetworkCoordinator.isNetworkAvailable()
            _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
        }
    }

    /**
     * 刷新网络状态
     */
    fun refreshNetworkStatus() {
        checkNetworkStatus()
    }

    private suspend fun refreshNetworkAndSyncUi(): Boolean {
        val isAvailable = configNetworkCoordinator.isNetworkAvailable()
        _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
        return isAvailable
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            var sessionId: String? = null
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 检查网络状态
                val isNetworkAvailable = refreshNetworkAndSyncUi()

                // 确保有当前会话
                sessionId = getOrCreateCurrentSession()

                // Save user message to both repositories
                conversationRepository.addUserMessage(message)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.USER, message)

                // 使用AI推理引擎进行智能决策
                val aiResponse = processWithAIReasoning(message, isNetworkAvailable)

                // Save AI response to both repositories
                conversationRepository.addAssistantMessage(aiResponse)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, aiResponse)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                val errorMessage = "抱歉，处理您的请求时出现了错误: ${e.message}"
                conversationRepository.addAssistantMessage(errorMessage)
                sessionId?.let {
                    chatSessionRepository.addMessage(it, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, errorMessage)
                }
            }
        }
    }

    /**
     * 使用AI推理引擎处理消息
     * AI自主决策调用哪些功能
     *
     * 【优先级处理】
     * 1. 身份确认询问（最高优先级）
     * 2. 交易修改/删除请求
     * 3. 信息查询和数据分析
     * 4. 记账操作
     * 5. 普通对话
     */
    private suspend fun processWithAIReasoning(message: String, isNetworkAvailable: Boolean): String {
        val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)
        val pendingClarificationResult = handlePendingClarificationIfNeeded(message, currentButler.id)
        if (pendingClarificationResult != null) {
            return pendingClarificationResult
        }
        return when (
            val result = messageExecutionCoordinator.execute(
                message = message,
                currentButler = currentButler,
                conversationHistory = getRecentConversationHistory(),
                isNetworkAvailable = isNetworkAvailable,
                currentUseBuiltinConfig = currentUseBuiltinConfig,
                currentAIConfig = currentAIConfig,
                pendingState = pendingModificationLifecycle.currentState(),
                handleModificationConfirmation = ::handleModificationConfirmation,
                handleTransactionModification = ::handleTransactionModification,
                processWithRemoteAI = ::processWithRemoteAI
            )
        ) {
            is AIAssistantMessageExecutionResult.Reply -> result.message
            is AIAssistantMessageExecutionResult.ConfirmationRequired -> result.message
            is AIAssistantMessageExecutionResult.ClarificationRequired -> {
                pendingClarificationLifecycle.begin(message, result.message).reply
            }
        }
    }
    
    /**
     * 处理交易修改请求
     */
    private suspend fun handleTransactionModification(
        message: String,
        butlerId: String
    ): ModificationFlowResult {
        return pendingModificationLifecycle.begin(message, butlerId)
    }
    
    /**
     * 处理交易修改确认
     */
    private suspend fun handleModificationConfirmation(
        message: String,
        butlerId: String
    ): ModificationFlowResult {
        return pendingModificationLifecycle.continuePending(message, butlerId)
    }
    
    private suspend fun handlePendingClarificationIfNeeded(
        message: String,
        butlerId: String
    ): String? {
        return when (val result = pendingClarificationLifecycle.currentState()?.let {
            pendingClarificationLifecycle.continuePending(message, butlerId)
        }) {
            null -> null
            is ClarificationFlowResult.RequestClarification -> result.reply
            is ClarificationFlowResult.Finish -> result.reply
            is ClarificationFlowResult.ContinueWithMessage -> {
                when (
                    val continuationResult = messageExecutionCoordinator.execute(
                        message = result.message,
                        currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler),
                        conversationHistory = getRecentConversationHistory(),
                        isNetworkAvailable = _uiState.value.isNetworkAvailable,
                        currentUseBuiltinConfig = currentUseBuiltinConfig,
                        currentAIConfig = currentAIConfig,
                        pendingState = pendingModificationLifecycle.currentState(),
                        handleModificationConfirmation = ::handleModificationConfirmation,
                        handleTransactionModification = ::handleTransactionModification,
                        processWithRemoteAI = ::processWithRemoteAI
                    )
                ) {
                    is AIAssistantMessageExecutionResult.Reply -> continuationResult.message
                    is AIAssistantMessageExecutionResult.ConfirmationRequired -> continuationResult.message
                    is AIAssistantMessageExecutionResult.ClarificationRequired -> {
                        pendingClarificationLifecycle.begin(result.message, continuationResult.message).reply
                    }
                }
            }
        }
    }

    /**
     * 获取最近的对话历史
     */
    private suspend fun getRecentConversationHistory(): List<String> {
        return conversationRepository.getRecentConversations(5).map { it.content }
    }

    /**
     * 获取或创建当前会话 - 优先使用当前会话，没有则使用最后一个，都没有才创建
     */
    private suspend fun getOrCreateCurrentSession(): String {
        val sessionId = sessionCoordinator.getOrCreateCurrentSession(_uiState.value.currentSessionId)
        if (_uiState.value.currentSessionId != sessionId) {
            _uiState.update { it.copy(currentSessionId = sessionId) }
        }
        return sessionId
    }

    /**
     * 使用远程大模型处理消息
     */
    private suspend fun processWithRemoteAI(message: String): String {
        val accounts = accountRepository.getAllAccountsList()
        val categories = categoryRepository.getAllCategoriesList()
        val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)
        val messages = promptBuilder.buildMessages(message, currentButler, accounts, categories)

        return when (val result = remoteExecutionHandler.execute(
            userMessage = message,
            messages = messages,
            config = currentAIConfig
        )) {
            is AIAssistantRemoteExecutionResult.Timeout -> "请求超时，请稍后重试。"
            is AIAssistantRemoteExecutionResult.TransportFailure -> "处理请求时出错: ${result.message}"
            is AIAssistantRemoteExecutionResult.IncompleteResponse -> "响应不完整，请稍后重试。"
            is AIAssistantRemoteExecutionResult.ActionExecutionRequested -> {
                actionExecutor.executeAIActions(result.rawResponse)
            }
            is AIAssistantRemoteExecutionResult.LocalFallbackRequested -> {
                val localResult = processWithLocalAI(message)
                remoteResponseInterpreter.combineRemoteAndLocalReply(
                    remoteReply = result.remoteReply,
                    localResult = localResult
                )
            }
            is AIAssistantRemoteExecutionResult.RemoteReply -> result.reply
        }
    }

    /**
     * 使用本地AI处理消息
     */
    private suspend fun processWithLocalAI(message: String): String {
        return aiLocalProcessor.processMessage(
            message = message,
            isNetworkAvailable = _uiState.value.isNetworkAvailable,
            isAIConfigured = currentAIConfig.isEnabled && currentAIConfig.apiKey.isNotBlank()
        )
    }

    private fun clearPendingInteractionStates() {
        pendingModificationLifecycle.clear()
        pendingClarificationLifecycle.clear()
    }

    /**
     * 创建新对话会话
     */
    fun createNewSession() {
        viewModelScope.launch {
            try {
                val session = sessionCoordinator.createNewSession("新对话 ${System.currentTimeMillis() / 1000}")
                _uiState.update { it.copy(currentSessionId = session.id) }
                clearPendingInteractionStates()
                conversationRepository.clearAllConversations()
                val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)
                val welcomeMessage = when (currentButler.id) {
                    "xiaocainiang" -> "主人~你好呀！🌸 小财娘在这里等着为您服务呢~\n有什么记账或理财的需求，随时告诉我哦~💕✨"
                    "taotao" -> "主人～你好呀！✨ 桃桃在这里等着为你服务呢～\n有什么需要帮忙的，随时告诉桃桃哦～🌸💕"
                    "guchen" -> "（懒洋洋地）啊...你来了...\n有什么事快说，说完我好继续睡觉...\n不过既然来了，你的财务就交给我吧。"
                    "suqian" -> "（平静地看着你）...\n有事就说。\n你的财务，我会处理好的。"
                    "yishuihan" -> "（温柔地微笑）你好呀～\n别紧张，有我在呢。\n有什么财务上的需要，随时告诉我。"
                    else -> "你好！我是你的AI记账助手。\n有什么记账或理财的需求，随时告诉我。"
                }
                conversationRepository.addAssistantMessage(welcomeMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 切换会话
     */
    fun switchSession(sessionId: String) {
        switchSessionJob?.cancel()
        switchSessionJob = viewModelScope.launch {
            try {
                val messages = sessionCoordinator.switchSession(sessionId)
                _uiState.update { it.copy(currentSessionId = sessionId) }
                clearPendingInteractionStates()
                conversationRepository.clearAllConversations()
                messages.forEach { msg ->
                    when (msg.role) {
                        com.example.aiaccounting.data.local.entity.MessageRole.USER -> conversationRepository.addUserMessage(msg.content)
                        com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT -> conversationRepository.addAssistantMessage(msg.content)
                        else -> {}
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 删除会话 - 如果只剩一个则清空而不是删除
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                when (val result = sessionCoordinator.deleteSession(sessionId, _uiState.value.currentSessionId)) {
                    DeleteSessionResult.NoChange,
                    DeleteSessionResult.DeletedNonCurrentSession -> Unit

                    DeleteSessionResult.ResetCurrentSingleSession -> {
                        clearPendingInteractionStates()
                        conversationRepository.clearAllConversations()
                        val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)
                        val welcomeMessage = when (currentButler.id) {
                            "xiaocainiang" -> "主人~你好呀！🌸 小财娘在这里等着为您服务呢~\n有什么记账或理财的需求，随时告诉我哦~💕✨"
                            "taotao" -> "主人～你好呀！✨ 桃桃在这里等着为你服务呢～\n有什么需要帮忙的，随时告诉桃桃哦～🌸💕"
                            "guchen" -> "（懒洋洋地）啊...你来了...\n有什么事快说，说完我好继续睡觉...\n不过既然来了，你的财务就交给我吧。"
                            "suqian" -> "（平静地看着你）...\n有事就说。\n你的财务，我会处理好的。"
                            "yishuihan" -> "（温柔地微笑）你好呀～\n别紧张，有我在呢。\n有什么财务上的需要，随时告诉我。"
                            else -> "你好！我是你的AI记账助手。\n有什么记账或理财的需求，随时告诉我。"
                        }
                        conversationRepository.addAssistantMessage(welcomeMessage)
                    }

                    DeleteSessionResult.ClearedCurrentSession -> {
                        _uiState.update { it.copy(currentSessionId = null) }
                        clearPendingInteractionStates()
                        conversationRepository.clearAllConversations()
                    }

                    is DeleteSessionResult.SwitchedToSession -> {
                        _uiState.update { it.copy(currentSessionId = result.sessionId) }
                        clearPendingInteractionStates()
                        conversationRepository.clearAllConversations()
                        result.messages.forEach { msg ->
                            when (msg.role) {
                                com.example.aiaccounting.data.local.entity.MessageRole.USER -> conversationRepository.addUserMessage(msg.content)
                                com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT -> conversationRepository.addAssistantMessage(msg.content)
                                else -> {}
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 重命名会话
     */
    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                sessionCoordinator.renameSession(sessionId, newTitle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 发送消息和图片
     */
    fun sendMessageWithImages(message: String, imageUris: List<Uri>, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val sessionId = getOrCreateCurrentSession()
                val displayMessage = message.ifBlank { "发送了${imageUris.size}张图片" }
                val imageUriStrings = imageUris.map { it.toString() }
                conversationRepository.addUserMessageWithImages(displayMessage, imageUriStrings)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.USER, displayMessage, imageUriStrings)

                val isNetworkAvailable = refreshNetworkAndSyncUi()
                val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)
                val processingResult = imageMessageHandler.processImageMessage(
                    message = message,
                    imageUris = imageUris,
                    context = context,
                    currentButler = currentButler,
                    config = currentAIConfig,
                    isNetworkAvailable = isNetworkAvailable
                )

                val assistantMessage = when (processingResult) {
                    is ImageMessageProcessingResult.Error -> processingResult.message
                    is ImageMessageProcessingResult.Success -> processingResult.message
                }
                conversationRepository.addAssistantMessage(assistantMessage)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, assistantMessage)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                aiUsageRepository.recordCall(success = false)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                conversationRepository.addAssistantMessage(
                    "处理图片时出错: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI State for AI Assistant screen
 */
data class AIAssistantUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAIConfigured: Boolean = false,
    val aiConfig: AIConfig = AIConfig(),
    val isNetworkAvailable: Boolean = true,
    val currentSessionId: String? = null,
    val currentButler: Butler? = null
)
