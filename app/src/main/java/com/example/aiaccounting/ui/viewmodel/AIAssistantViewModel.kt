package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.aiaccounting.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.AILocalProcessor
import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private val modificationCoordinator = AIAssistantModificationCoordinator(transactionModificationHandler)
    private val imageMessageHandler = AIAssistantImageMessageHandler(
        aiService = aiService,
        imageProcessingService = imageProcessingService,
        aiUsageRepository = aiUsageRepository
    )
    private val promptBuilder = AIAssistantPromptBuilder()

    // 待确认的交易修改状态
    private var pendingModificationState: PendingModificationState? = null
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

        pendingModificationState?.let {
            return handleModificationConfirmation(message, currentButler.id)
        }

        val context = AIReasoningEngine.ReasoningContext(
            userMessage = message,
            conversationHistory = getRecentConversationHistory()
        )

        val reasoningResult = aiReasoningEngine.reason(context, currentButler.id)
        return when (
            val route = messageOrchestrator.route(
                reasoningResult = reasoningResult,
                userMessage = message,
                butlerId = currentButler.id,
                isNetworkAvailable = isNetworkAvailable,
                isBuiltinConfigEnabled = currentUseBuiltinConfig,
                isAIEnabled = currentAIConfig.isEnabled,
                hasApiKey = currentAIConfig.apiKey.isNotBlank(),
                pendingState = pendingModificationState
            )
        ) {
            is AIAssistantMessageRoute.LocalActions -> {
                aiReasoningEngine.executeActions(route.actions)
            }
            is AIAssistantMessageRoute.RemoteOrLocalFallback -> {
                processWithRemoteAI(route.userMessage)
            }
            is AIAssistantMessageRoute.ModificationFlow -> {
                if (route.pendingState != null) {
                    handleModificationConfirmation(route.message, route.butlerId)
                } else {
                    handleTransactionModification(route.message, route.butlerId)
                }
            }
        }
    }
    
    /**
     * 处理交易修改请求
     */
    private suspend fun handleTransactionModification(
        message: String,
        butlerId: String
    ): String {
        return when (val result = modificationCoordinator.beginModification(message, butlerId)) {
            is ModificationFlowResult.StartConfirmation -> {
                pendingModificationState = result.pendingState
                result.reply
            }
            is ModificationFlowResult.Finish -> result.reply
        }
    }
    
    /**
     * 处理交易修改确认
     */
    private suspend fun handleModificationConfirmation(
        message: String,
        butlerId: String
    ): String {
        val pendingState = pendingModificationState ?: return "抱歉，没有待确认的操作。"
        val result = modificationCoordinator.continueModification(message, butlerId, pendingState)
        if (result is ModificationFlowResult.Finish &&
            (transactionModificationHandler.isConfirmation(message) || transactionModificationHandler.isCancellation(message))
        ) {
            pendingModificationState = null
        }
        return when (result) {
            is ModificationFlowResult.Finish -> result.reply
            is ModificationFlowResult.StartConfirmation -> result.reply
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
     * 判断是否使用远程AI
     */
    private fun shouldUseRemoteAI(isNetworkAvailable: Boolean): Boolean {
        // 规则联动：默认模型开启时，强制走本地 AI；关闭时才允许邀请码云端模型生效
        if (currentUseBuiltinConfig) return false

        return isNetworkAvailable &&
               currentAIConfig.isEnabled &&
               currentAIConfig.apiKey.isNotBlank()
    }

    /**
     * 使用远程大模型处理消息
     */
    private suspend fun processWithRemoteAI(message: String): String {
        // 获取当前账户和分类信息，提供给AI上下文
        val accounts = accountRepository.getAllAccountsList()
        val categories = categoryRepository.getAllCategoriesList()

        // 获取当前管家
        val currentButler = butlerCoordinator.resolveCurrentButler(_uiState.value.currentButler)

        // 使用 PromptBuilder 构建消息
        val messages = promptBuilder.buildMessages(message, currentButler, accounts, categories)

        return try {
            // 增加超时时间到60秒
            val result = withTimeoutOrNull(60000L) {
                var fullResponse = ""
                aiService.sendChatStream(messages, currentAIConfig).collect { chunk ->
                    fullResponse += chunk
                }
                fullResponse
            }

            if (result == null) {
                aiUsageRepository.recordCall(success = false)
                // 超时回复
                return "请求超时，请稍后重试。"
            }

            // 记录成功
            aiUsageRepository.recordCall(success = true)

            // 检查是否是JSON操作指令（兼容多种格式）
            val actionTypeRegex = Regex("\"type\"\\s*:\\s*\"(add_transaction|create_account|query|query_accounts|query_categories|query_transactions|create_category)\"")
            val isActionCommand = result.contains("\"action\"") ||
                                  result.contains("\"actions\"") ||
                                  actionTypeRegex.containsMatchIn(result)
            if (isActionCommand) {
                actionExecutor.executeAIActions(result)
            } else {
                // 如果远程AI只返回纯文本，尝试使用本地AI推理引擎处理记账请求
                val lowerMessage = message.lowercase()
                val isTransactionRequest = containsAny(lowerMessage, listOf("记", "花了", "收入", "支出", "消费", "买", "卖", "转账", "付", "赚", "工资", "奖金", "红包", "退款", "报销"))
                
                if (isTransactionRequest) {
                    if (BuildConfig.DEBUG) {
                        Log.d("AIAssistantViewModel", "【记账请求】远程AI只返回文本，强制使用本地AI处理")
                    }

                    // 【关键修复】直接使用本地AI处理记账，不依赖远程AI的JSON
                    // 这会真正执行数据库操作
                    val localResult = processWithLocalAI(message)
                    if (BuildConfig.DEBUG) {
                        Log.d("AIAssistantViewModel", "本地AI执行结果: $localResult")
                    }
                    
                    // 如果本地执行成功，返回本地结果；如果失败，返回错误信息
                    if (localResult.contains("✅")) {
                        // 本地记账成功，返回成功信息（可以包含远程AI的友好回复）
                        "${result}\n\n${localResult}"
                    } else if (localResult.contains("❌")) {
                        // 本地记账失败，返回错误信息
                        localResult
                    } else {
                        // 其他情况，返回合并结果
                        "${result}\n\n${localResult}"
                    }
                } else {
                    // 普通对话回复
                    result
                }
            }
        } catch (e: Exception) {
            aiUsageRepository.recordCall(success = false)
            // 错误回复
            "处理请求时出错: ${e.message}"
        }
    }

    /**
     * 确保基础分类存在
     */
    private suspend fun ensureBasicCategoriesExist() {
        aiLocalProcessor.ensureBasicCategoriesExist()
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

    private suspend fun handleTransactionCommand(message: String, lowerMessage: String): String {
        val amount = extractAmount(lowerMessage)
        val type = if (containsAny(lowerMessage, listOf("收入", "赚", "收到", "工资", "奖金"))) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        if (amount != null) {
            // 确保基础分类存在
            ensureBasicCategoriesExist()
            
            var accounts = accountRepository.getAllAccountsList()
            var categories = categoryRepository.getAllCategoriesList()

            // 如果没有账户，自动创建一个默认账户
            var defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
            
            if (defaultAccount == null) {
                // 自动创建默认账户
                val createAccountOp = AIOperation.AddAccount(
                    name = "默认账户",
                    type = com.example.aiaccounting.data.local.entity.AccountType.CASH,
                    balance = 0.0
                )
                when (val result = aiOperationExecutor.executeOperation(createAccountOp)) {
                    is AIOperationExecutor.AIOperationResult.Success -> {
                        // 重新获取账户列表
                        accounts = accountRepository.getAllAccountsList()
                        defaultAccount = accounts.firstOrNull()
                    }
                    is AIOperationExecutor.AIOperationResult.Error -> {
                        return "创建默认账户失败: ${result.error}"
                    }
                }
            }
            
            if (defaultAccount == null) {
                return "无法创建账户，请手动创建账户后再试。"
            }

            // 获取或创建分类
            var defaultCategory = categories.firstOrNull { it.type == type }
            
            if (defaultCategory == null) {
                // 自动创建默认分类
                val categoryName = if (type == TransactionType.INCOME) "其他收入" else "其他支出"
                val createCategoryOp = AIOperation.AddCategory(
                    name = categoryName,
                    type = type
                )
                when (val result = aiOperationExecutor.executeOperation(createCategoryOp)) {
                    is AIOperationExecutor.AIOperationResult.Success -> {
                        // 重新获取分类列表
                        categories = categoryRepository.getAllCategoriesList()
                        defaultCategory = categories.firstOrNull { it.type == type }
                    }
                    is AIOperationExecutor.AIOperationResult.Error -> {
                        // 创建失败，记录日志但继续尝试使用默认分类
                        Log.w("AIAssistantViewModel", "创建分类失败: ${result.error}")
                        defaultCategory = categories.firstOrNull { it.type == type }
                    }
                }
            }
            
            // 如果仍然没有分类，尝试使用任何可用分类
            if (defaultCategory == null) {
                defaultCategory = categories.firstOrNull { it.type == type } 
                    ?: categories.firstOrNull()
            }
            
            // 如果还是没有分类，创建紧急默认分类
            if (defaultCategory == null) {
                val emergencyCategoryOp = AIOperation.AddCategory(
                    name = if (type == TransactionType.INCOME) "收入" else "支出",
                    type = type
                )
                when (aiOperationExecutor.executeOperation(emergencyCategoryOp)) {
                    is AIOperationExecutor.AIOperationResult.Success -> {
                        categories = categoryRepository.getAllCategoriesList()
                        defaultCategory = categories.firstOrNull { it.type == type }
                    }
                    is AIOperationExecutor.AIOperationResult.Error -> {
                        return "❌ 记账失败：无法创建分类，请检查数据库权限"
                    }
                }
            }
            
            // 最终检查
            if (defaultCategory == null) {
                return "❌ 记账失败：系统无法创建分类，请手动创建分类后再试"
            }

            val operation = AIOperation.AddTransaction(
                amount = amount,
                type = type,
                accountId = defaultAccount.id,
                categoryId = defaultCategory.id,
                note = message
            )

            return when (val result = aiOperationExecutor.executeOperation(operation)) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    "✅ ${result.message}\n" +
                    "账户: ${defaultAccount.name}\n" +
                    "分类: ${defaultCategory.name}\n" +
                    "您可以说「查看最近交易」来确认记录。"
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    "❌ 记账失败: ${result.error}"
                }
            }
        }

        return "我没有识别到金额。请告诉我具体的金额，比如：\n" +
               "- 花了50元买咖啡\n" +
               "- 今天收入5000元工资\n" +
               "- 支出200元超市购物"
    }

    private suspend fun handleLocalQueryCommand(lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("余额", "资产", "总共", "多少")) -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) {
                    "暂无账户信息"
                } else {
                    val total = accounts.sumOf { it.balance }
                    "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" } +
                    "\n\n💰 总资产: ¥${String.format("%.2f", total)}"
                }
            }
            containsAny(lowerMessage, listOf("交易", "记录", "明细", "最近")) -> {
                val operation = AIOperation.QueryData("transactions", extractNumber(lowerMessage) ?: 10)
                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationExecutor.AIOperationResult.Success -> result.message
                    is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
                }
            }
            containsAny(lowerMessage, listOf("账户", "银行卡")) -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) {
                    "暂无账户"
                } else {
                    "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" }
                }
            }
            containsAny(lowerMessage, listOf("分类", "类别")) -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) {
                    "暂无分类"
                } else {
                    "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
                }
            }
            else -> {
                "您可以这样查询：\n" +
                "- 查看总资产\n" +
                "- 最近10笔交易\n" +
                "- 账户列表\n" +
                "- 分类统计"
            }
        }
    }

    private suspend fun handleAccountCommand(message: String, lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("添加", "新建", "创建")) -> {
                val name = extractAccountName(message)
                if (name != null) {
                    val type = extractAccountType(lowerMessage)
                    val operation = AIOperation.AddAccount(
                        name = name,
                        type = type,
                        balance = extractAmount(lowerMessage) ?: 0.0
                    )
                    when (val result = aiOperationExecutor.executeOperation(operation)) {
                        is AIOperationExecutor.AIOperationResult.Success -> result.message
                        is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
                    }
                } else {
                    "请告诉我账户名称，比如：\n" +
                    "- 添加一个现金账户\n" +
                    "- 新建银行卡账户余额10000元"
                }
            }
            containsAny(lowerMessage, listOf("删除", "移除")) -> {
                "删除账户功能需要在账户管理界面操作，以确保数据安全。"
            }
            else -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) {
                    "暂无账户"
                } else {
                    val total = accounts.sumOf { it.balance }
                    "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" } +
                    "\n\n💰 总资产: ¥${String.format("%.2f", total)}"
                }
            }
        }
    }

    private suspend fun handleCategoryCommand(message: String, lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("添加", "新建", "创建")) -> {
                val name = extractCategoryName(message)
                val type = if (containsAny(lowerMessage, listOf("收入"))) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                if (name != null) {
                    val operation = AIOperation.AddCategory(
                        name = name,
                        type = type
                    )
                    when (val result = aiOperationExecutor.executeOperation(operation)) {
                        is AIOperationExecutor.AIOperationResult.Success -> result.message
                        is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
                    }
                } else {
                    "请告诉我分类名称，比如：\n" +
                    "- 添加餐饮分类\n" +
                    "- 新建交通支出分类"
                }
            }
            else -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) {
                    "暂无分类"
                } else {
                    "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
                }
            }
        }
    }

    private fun handleBudgetCommand(lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("设置", "添加", "创建")) -> {
                val amount = extractAmount(lowerMessage)
                if (amount != null) {
                    "预算设置功能已记录。您可以在预算管理界面查看和修改。"
                } else {
                    "请告诉我预算金额，比如：\n" +
                    "- 设置餐饮预算2000元\n" +
                    "- 每月交通预算500元"
                }
            }
            else -> {
                "预算管理功能：\n" +
                "- 设置XX分类预算XXX元\n" +
                "- 查看预算执行情况请前往预算管理界面"
            }
        }
    }

    private suspend fun handleExportCommand(): String {
        val operation = AIOperation.ExportData(format = "excel")
        return when (val result = aiOperationExecutor.executeOperation(operation)) {
            is AIOperationExecutor.AIOperationResult.Success -> {
                result.message + "\n请前往设置 > 数据备份页面完成导出操作。"
            }
            is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
        }
    }

    private fun handleGeneralConversation(message: String): String {
        return when {
            containsAny(message.lowercase(), listOf("你好", "您好", "hi", "hello")) -> {
                val mode = if (currentAIConfig.isEnabled) {
                    if (_uiState.value.isNetworkAvailable) "🤖 智能模式（联网）" 
                    else "📱 本地模式（离线）"
                } else "📱 本地模式"
                
                "您好！我是您的AI记账助手。\n" +
                "当前模式: $mode\n\n" +
                "我可以帮您：\n" +
                "记账 - 直接说花了50元买咖啡\n" +
                "查询 - 查看总资产或最近交易\n" +
                "管理账户 - 添加现金账户\n" +
                "管理分类 - 添加餐饮分类\n" +
                "导出数据 - 导出Excel\n\n" +
                "有什么可以帮您的吗？"
            }
            containsAny(message.lowercase(), listOf("谢谢", "感谢")) -> {
                "不客气！随时为您服务。有其他问题随时告诉我！"
            }
            containsAny(message.lowercase(), listOf("再见", "拜拜")) -> {
                "再见！记得坚持记账哦，祝您理财顺利！"
            }
            else -> {
                "抱歉，我不太理解您的意思。您可以尝试：\n\n" +
                "记账：\n" +
                "  - 花了30元吃午饭\n" +
                "  - 今天收入5000元工资\n\n" +
                "查询：\n" +
                "  - 查看总资产\n" +
                "  - 最近10笔交易\n\n" +
                "或者输入帮助查看更多功能。"
            }
        }
    }

    // Helper functions
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d+\.?\d*)\s*[元块]?""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractNumber(text: String): Int? {
        val regex = Regex("""(\d+)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractAccountName(text: String): String? {
        val patterns = listOf(
            Regex("""添加[了一个]*(.+?)[账户卡]?"""),
            Regex("""新建[了一个]*(.+?)[账户卡]?"""),
            Regex("""创建[了一个]*(.+?)[账户卡]?""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.groupValues?.get(1)?.let { return it.trim() }
        }
        return null
    }

    private fun extractAccountType(text: String): com.example.aiaccounting.data.local.entity.AccountType {
        return when {
            text.contains("现金") -> com.example.aiaccounting.data.local.entity.AccountType.CASH
            text.contains("支付宝") -> com.example.aiaccounting.data.local.entity.AccountType.ALIPAY
            text.contains("微信") -> com.example.aiaccounting.data.local.entity.AccountType.WECHAT
            text.contains("信用卡") -> com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD
            text.contains("借记卡") -> com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD
            text.contains("银行") -> com.example.aiaccounting.data.local.entity.AccountType.BANK
            else -> com.example.aiaccounting.data.local.entity.AccountType.OTHER
        }
    }

    private fun extractCategoryName(text: String): String? {
        val patterns = listOf(
            Regex("""添加[了一个]*(.+?)[分类]?"""),
            Regex("""新建[了一个]*(.+?)[分类]?"""),
            Regex("""创建[了一个]*(.+?)[分类]?""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.groupValues?.get(1)?.let { return it.trim() }
        }
        return null
    }

    /**
     * 创建新对话会话
     */
    fun createNewSession() {
        viewModelScope.launch {
            try {
                val session = sessionCoordinator.createNewSession("新对话 ${System.currentTimeMillis() / 1000}")
                _uiState.update { it.copy(currentSessionId = session.id) }
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
                        conversationRepository.clearAllConversations()
                    }

                    is DeleteSessionResult.SwitchedToSession -> {
                        _uiState.update { it.copy(currentSessionId = result.sessionId) }
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
