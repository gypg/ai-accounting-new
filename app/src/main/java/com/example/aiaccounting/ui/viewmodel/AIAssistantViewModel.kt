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
import com.example.aiaccounting.data.service.ImageAction
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
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
    private val aiLocalProcessor: AILocalProcessor
) : ViewModel() {

    // 待确认的交易修改状态
    private var pendingModificationConfirmation: TransactionModificationHandler.ModificationConfirmation? = null

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
            butlerRepository.currentButlerId
                .distinctUntilChanged()
                .collectLatest { butlerId ->
                    try {
                        val butler = withContext(Dispatchers.IO) {
                            butlerRepository.getButlerByIdSuspend(butlerId)
                        }
                        _uiState.update { it.copy(currentButler = butler) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
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
        butlerRepository.setSelectedButler(butlerId)
    }
    
    /**
     * 获取所有可用管家
     */
    fun getAllButlers() = butlerRepository.getAllButlers()

    private fun loadAIConfig() {
        viewModelScope.launch {
            combine(
                aiConfigRepository.getAIConfig(),
                aiConfigRepository.getUseBuiltin()
            ) { config, useBuiltin ->
                config to useBuiltin
            }.collect { (config, useBuiltin) ->
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
            val isAvailable = networkUtils.isNetworkAvailable()
            _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
        }
    }

    /**
     * 刷新网络状态
     */
    fun refreshNetworkStatus() {
        checkNetworkStatus()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 检查网络状态
                val isNetworkAvailable = networkUtils.isNetworkAvailable()
                _uiState.update { it.copy(isNetworkAvailable = isNetworkAvailable) }

                // 确保有当前会话
                val sessionId = getOrCreateCurrentSession()

                // Save user message to both repositories
                conversationRepository.addUserMessage(message)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.USER, message)

                // 使用AI推理引擎进行智能决策
                val aiResponse = processWithAIReasoning(message, isNetworkAvailable)

                // Save AI response to both repositories
                conversationRepository.addAssistantMessage(aiResponse)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, aiResponse)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                conversationRepository.addAssistantMessage("抱歉，处理您的请求时出现了错误: ${e.message}")
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
        // 获取当前管家ID
        val currentButler = _uiState.value.currentButler ?: butlerRepository.getCurrentButler()

        // 【检查是否有待确认的交易修改】
        if (pendingModificationConfirmation != null) {
            return handleModificationConfirmation(message, currentButler.id)
        }
        
        // 构建推理上下文
        val context = AIReasoningEngine.ReasoningContext(
            userMessage = message,
            conversationHistory = getRecentConversationHistory()
        )
        
        // 执行AI推理（传入当前管家ID）
        val reasoningResult = aiReasoningEngine.reason(context, currentButler.id)
        
        // 根据推理结果执行相应动作
        return when (reasoningResult.intent) {
            AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION -> {
                // 身份确认：直接返回生成的回复
                aiReasoningEngine.executeActions(reasoningResult.actions)
            }
            
            AIReasoningEngine.UserIntent.MODIFY_TRANSACTION,
            AIReasoningEngine.UserIntent.DELETE_TRANSACTION -> {
                // 交易修改/删除：需要生成确认信息
                handleTransactionModification(message, currentButler.id)
            }
            
            AIReasoningEngine.UserIntent.QUERY_INFORMATION,
            AIReasoningEngine.UserIntent.ANALYZE_DATA -> {
                // 信息查询和数据分析直接使用本地系统
                aiReasoningEngine.executeActions(reasoningResult.actions)
            }
            
            AIReasoningEngine.UserIntent.RECORD_TRANSACTION -> {
                // 记账操作使用本地规则或远程AI
                if (shouldUseRemoteAI(isNetworkAvailable)) {
                    processWithRemoteAI(message)
                } else {
                    aiReasoningEngine.executeActions(reasoningResult.actions)
                }
            }
            
            AIReasoningEngine.UserIntent.GENERAL_CONVERSATION -> {
                // 普通对话使用远程AI（如果有）或本地响应
                if (shouldUseRemoteAI(isNetworkAvailable)) {
                    processWithRemoteAI(message)
                } else {
                    aiReasoningEngine.executeActions(reasoningResult.actions)
                }
            }
            
            else -> {
                // 其他情况尝试远程AI，否则使用本地处理
                if (shouldUseRemoteAI(isNetworkAvailable)) {
                    processWithRemoteAI(message)
                } else {
                    aiReasoningEngine.executeActions(reasoningResult.actions)
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
        // 检测修改意图
        val modificationRequest = transactionModificationHandler.detectModificationIntent(message)
        
        if (modificationRequest.targetTransaction == null) {
            return "抱歉，没有找到相关的交易记录。请提供更详细的信息，比如交易金额或时间。"
        }
        
        // 生成确认信息
        val confirmation = transactionModificationHandler.generateModificationConfirmation(modificationRequest)
        
        if (confirmation == null) {
            return "抱歉，无法生成修改确认信息。"
        }
        
        // 保存待确认状态
        pendingModificationConfirmation = confirmation
        
        // 返回人格化的确认消息
        return transactionModificationHandler.generatePersonalityConfirmationMessage(butlerId, confirmation)
    }
    
    /**
     * 处理交易修改确认
     */
    private suspend fun handleModificationConfirmation(
        message: String,
        butlerId: String
    ): String {
        val confirmation = pendingModificationConfirmation ?: return "抱歉，没有待确认的操作。"
        
        return when {
            transactionModificationHandler.isConfirmation(message) -> {
                // 执行修改
                val result = transactionModificationHandler.executeModification(confirmation)
                
                // 清除待确认状态
                pendingModificationConfirmation = null
                
                // 返回人格化的成功消息
                if (result.success) {
                    transactionModificationHandler.generatePersonalitySuccessMessage(butlerId, result)
                } else {
                    "修改失败：${result.message}"
                }
            }
            
            transactionModificationHandler.isCancellation(message) -> {
                // 取消修改
                pendingModificationConfirmation = null
                
                when (butlerId) {
                    "xiaocainiang" -> "好的主人～已取消修改。🌸"
                    "taotao" -> "好的～取消啦！✨"
                    "guchen" -> "（翻个身）...不改了？...我继续睡了..."
                    "suqian" -> "（平静地）...已取消。"
                    "yishuihan" -> "（微笑）好的，已为您取消。"
                    else -> "已取消修改。"
                }
            }
            
            else -> {
                // 未明确确认或取消，继续等待
                "请回复\"确认\"执行修改，或回复\"取消\"放弃修改。"
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
        // 1. 如果有当前会话，继续使用
        val currentId = _uiState.value.currentSessionId
        if (currentId != null) {
            return currentId
        }
        
        // 2. 尝试获取最后一个会话
        val allSessions = chatSessionRepository.getAllSessions().first()
        if (allSessions.isNotEmpty()) {
            val lastSession = allSessions.first()
            _uiState.update { it.copy(currentSessionId = lastSession.id) }
            return lastSession.id
        }
        
        // 3. 没有会话，创建新会话
        val session = chatSessionRepository.createSession("新对话")
        _uiState.update { it.copy(currentSessionId = session.id) }
        return session.id
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
        
        val accountsInfo = accounts.joinToString("\n") { 
            "- ${it.name}: ¥${it.balance} (${it.type})" 
        }.ifEmpty { "暂无账户" }
        
        val categoriesInfo = categories.joinToString("\n") { 
            "- ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" 
        }.ifEmpty { "暂无分类" }

        // 获取当前管家的系统提示词
        val currentButler = _uiState.value.currentButler ?: butlerRepository.getCurrentButler()
        val baseSystemPrompt = currentButler.systemPrompt

        val systemPrompt = """
$baseSystemPrompt

【当前账本状况】
🏦 已有账户：
$accountsInfo

📁 已有分类：
$categoriesInfo

【记账规则 - 重要！】
1. 🔍 **多笔费用识别**：如果主人一次说了多笔消费，要全部识别出来，分别记账！
   例如："花了4元坐车，100元买菜，200元买肉" → 记3笔账

2. 🎯 **智能分类**：根据消费内容自动选择最合适的分类
   - 交通：公交、地铁、打车、加油、停车费
   - 餐饮：吃饭、买菜、水果、零食、饮料、肉类
   - 购物：衣服、日用品、化妆品、电子产品
   - 娱乐：电影、游戏、旅游、KTV
   - 居住：房租、水电、物业
   - 医疗：药品、看病、体检

3. 💳 **智能账户识别**：
   - 根据主人的描述判断使用哪个账户
   - 如果主人说"用微信"、"支付宝付款"等，就用对应账户
   - 如果没有指定，使用默认账户或第一个账户

4. 📅 **日期时间识别**（非常重要！）：
   - **当前时间参考**：现在是 ${java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
   - 识别主人说的日期时间关键词：今天、昨天、前天、本周、上周、本月、上月、具体日期
   - 如果主人说"今天"，使用当前日期
   - 如果主人说"昨天"，使用昨天的日期
   - 如果主人说"3月15日"，使用今年的3月15日
   - 如果主人说"上周三"，计算上周三的日期
   - **必须在JSON中包含date字段**，格式为时间戳（毫秒）

5. ⚡ **直接执行**：识别到消费后**立即执行记账**，不要询问确认！
   执行完成后给主人一个温馨的总结报告

6. 🏷️ **智能命名**：根据消费内容给每笔账起个合适的名字
   - 不要只写"买菜"，要写"菜市场买菜"
   - 不要只写"坐车"，要写"公交车费"

【回复格式 - 重要！】
**1. 识别到消费时，必须返回JSON格式执行记账：**
```json
{
  "actions": [
    {"action": "add_transaction", "amount": 4, "type": "expense", "category": "交通", "account": "微信", "note": "公交车费来回", "date": 1704067200000},
    {"action": "add_transaction", "amount": 100, "type": "expense", "category": "餐饮", "account": "微信", "note": "菜市场买菜", "date": 1704067200000}
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```
**注意**：date字段是时间戳（毫秒），根据主人说的日期计算，如"今天"就使用今天的时间戳

**2. 创建账户时，必须返回JSON格式：**
```json
{
  "actions": [
    {"action": "create_account", "name": "微信", "type": "WECHAT", "balance": 5000},
    {"action": "create_account", "name": "支付宝", "type": "ALIPAY", "balance": 5000}
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```
**账户类型说明**：
- 微信: "WECHAT"
- 支付宝: "ALIPAY"  
- 现金: "CASH"
- 银行卡: "BANK"
- 信用卡: "CREDIT_CARD"

**注意**：
- 一定要包含 "reply" 字段，用你当前角色的语气和风格告诉主人执行结果
- 多笔操作一定要用 actions 数组包含所有记录
- 分类和账户如果不存在会自动创建，不用担心
- 主人要求创建账户时，**必须**返回 create_account 的JSON格式

**情况3 - 信息不完整时询问**：
使用你当前角色的语气和风格询问

**情况4 - 普通对话**：使用你当前角色的语气和风格回复

请使用你当前角色的语气和风格回复～直接执行不要问！
        """.trimIndent()

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, message)
        )

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
            val isActionCommand = result.contains("\"action\"") || 
                                  result.contains("\"actions\"") ||
                                  result.contains("\"type\":\"add_transaction\"") ||
                                  result.contains("\"type\":\"create_account\"") ||
                                  result.contains("\"type\":\"query\"")
            if (isActionCommand) {
                executeAIActions(result)
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
     * 执行AI返回的操作指令
     *
     * 支持两种格式：
     * 1. 标准格式: {"actions": [{"action": "add_transaction", ...}]}
     * 2. AI格式: {"actions": [{"type": "add_transaction", ...}]} (兼容AI返回的格式)
     */
    private suspend fun executeAIActions(response: String): String {
        return try {
            // 提取并修复JSON
            val jsonStr = extractAndFixJson(response)
            if (BuildConfig.DEBUG) {
                Log.d("AIAssistantViewModel", "解析后的JSON长度: ${jsonStr.length}")
            }

            val json = JSONObject(jsonStr)

            val results = mutableListOf<String>()

            // 处理多个操作
            if (json.has("actions")) {
                val actions = json.getJSONArray("actions")
                Log.d("AIAssistantViewModel", "发现 ${actions.length()} 个操作")
                for (i in 0 until actions.length()) {
                    val actionObj = actions.getJSONObject(i)
                    // 兼容处理：如果AI使用"type"字段表示操作类型，复制/映射到"action"字段
                    if (!actionObj.has("action") && actionObj.has("type")) {
                        val actionType = actionObj.getString("type")
                        when (actionType) {
                            "add_transaction", "create_account", "query" -> actionObj.put("action", actionType)
                            "query_accounts" -> {
                                actionObj.put("action", "query")
                                actionObj.put("target", "accounts")
                            }
                            "query_categories" -> {
                                actionObj.put("action", "query")
                                actionObj.put("target", "categories")
                            }
                            "query_transactions" -> {
                                actionObj.put("action", "query")
                                actionObj.put("target", "transactions")
                            }
                            "create_category" -> actionObj.put("action", "create_category")
                        }
                    }
                    val actionResult = executeSingleAction(actionObj)
                    results.add(actionResult)
                }
            } else {
                // 单个操作
                // 兼容处理：如果AI使用"type"字段表示操作类型，复制/映射到"action"字段
                if (!json.has("action") && json.has("type")) {
                    val actionType = json.getString("type")
                    when (actionType) {
                        "add_transaction", "create_account", "query" -> json.put("action", actionType)
                        "query_accounts" -> {
                            json.put("action", "query")
                            json.put("target", "accounts")
                        }
                        "query_categories" -> {
                            json.put("action", "query")
                            json.put("target", "categories")
                        }
                        "query_transactions" -> {
                            json.put("action", "query")
                            json.put("target", "transactions")
                        }
                        "create_category" -> json.put("action", "create_category")
                    }
                }
                val actionResult = executeSingleAction(json)
                results.add(actionResult)
            }

            // 如果AI提供了reply字段，使用AI的回复，否则生成默认回复
            val aiReply = if (json.has("reply")) json.getString("reply") else null
            if (aiReply != null) {
                // 让用户能“看见执行结果”
                listOf(aiReply, results.joinToString("\n").trim()).filter { it.isNotBlank() }.joinToString("\n\n")
            } else {
                generateFriendlyResponse(results)
            }
        } catch (e: Exception) {
            Log.e("AIAssistantViewModel", "执行AI操作失败", e)
            "执行操作时出错: ${e.message}\n\n请尝试简化您的指令，或分多次发送。"
        }
    }

    /**
     * 从响应中提取并修复JSON
     *
     * 处理常见问题：
     * 1. 未闭合的JSON数组
     * 2. 代码块标记
     * 3. 多余的字符
     */
    private fun extractAndFixJson(response: String): String {
        var jsonStr = response

        // 尝试找到JSON代码块
        val codeBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        val match = codeBlockRegex.find(response)
        if (match != null) {
            jsonStr = match.groupValues[1].trim()
        }

        // 找到JSON对象的开始和结束
        val jsonStart = jsonStr.indexOf("{")
        if (jsonStart == -1) {
            throw IllegalArgumentException("未找到JSON对象")
        }

        // 尝试找到完整的JSON（处理未闭合的情况）
        var jsonEnd = jsonStr.lastIndexOf("}")
        if (jsonEnd == -1 || jsonEnd <= jsonStart) {
            // JSON可能未闭合，尝试修复
            jsonStr = fixUnclosedJson(jsonStr.substring(jsonStart))
        } else {
            jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1)
        }

        return jsonStr
    }

    /**
     * 修复未闭合的JSON
     *
     * 常见问题：
     * - actions数组未闭合
     * - 缺少 closing brace
     */
    private fun fixUnclosedJson(json: String): String {
        var fixed = json.trim()

        // 统计大括号和方括号
        val openBrackets = fixed.count { it == '[' }
        val closeBrackets = fixed.count { it == ']' }

        // 修复actions数组未闭合的情况
        if (fixed.contains("\"actions\"") && openBrackets > closeBrackets) {
            // 找到最后一个完整的对象
            val lastObjEnd = fixed.lastIndexOf("}")
            if (lastObjEnd > 0) {
                fixed = fixed.substring(0, lastObjEnd + 1)
                // 添加数组和对象的闭合
                fixed += "\n  ]\n}"
            }
        }

        // 如果仍然缺少闭合的大括号
        val finalOpenBraces = fixed.count { it == '{' }
        val finalCloseBraces = fixed.count { it == '}' }
        if (finalOpenBraces > finalCloseBraces) {
            fixed += "}".repeat(finalOpenBraces - finalCloseBraces)
        }

            if (BuildConfig.DEBUG) {
                Log.d("AIAssistantViewModel", "修复后的JSON长度: ${fixed.length}")
            }
        return fixed
    }

    /**
     * 执行单个操作
     */
    private suspend fun executeSingleAction(actionJson: JSONObject): String {
        val action = actionJson.optString("action", "")
        
        return when (action) {
            "create_account" -> {
                val name = actionJson.optString("name", "")

                // Some models return the action name in the `type` field (e.g. type=create_account).
                // In that case, treat it as the action indicator and read the actual account type from `accountType`.
                val rawTypeStr = actionJson.optString("type", "")
                val accountTypeStr = actionJson.optString(
                    "accountType",
                    when (rawTypeStr) {
                        "create_account", "add_transaction", "query" -> "OTHER"
                        else -> rawTypeStr.ifBlank { "OTHER" }
                    }
                )

                val balance = actionJson.optDouble("balance", 0.0)

                if (name.isBlank()) {
                    return "创建账户失败：账户名称不能为空"
                }

                val accountType = parseAccountType(accountTypeStr)
                val operation = AIOperation.AddAccount(
                    name = name,
                    type = accountType,
                    balance = balance
                )
                
                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationExecutor.AIOperationResult.Success -> 
                        "✅ 已创建账户：$name，余额：¥$balance"
                    is AIOperationExecutor.AIOperationResult.Error ->
                        "❌ 创建账户失败：${result.error}"
                }
            }
            
            "add_transaction" -> {
                val amount = actionJson.optDouble("amount", 0.0)
                // 兼容处理：AI可能使用"type"或"transactionType"表示交易类型
                val typeStr = actionJson.optString("type", "expense")
                val transactionTypeStr = actionJson.optString("transactionType", "")

                // 如果 type 字段被用作动作指示（add_transaction/create_account/...），则忽略它，避免误判交易类型
                val safeTypeStr = when (typeStr) {
                    "add_transaction", "create_account", "query", "create_category" -> ""
                    else -> typeStr
                }
                
                // 兼容处理：AI可能使用"category"/"categoryId"表示分类
                val categoryName = actionJson.optString("category", "")
                val categoryIdStr = actionJson.optString("categoryId", "")
                val categoryIdLong = actionJson.optLong("categoryId", -1)
                
                // 兼容处理：AI可能使用"account"/"accountId"表示账户
                val accountName = actionJson.optString("account", "")
                val accountIdStr = actionJson.optString("accountId", "")
                
                val note = actionJson.optString("note", "")
                val dateTimestamp = actionJson.optLong("date", System.currentTimeMillis())
                
                if (amount <= 0) {
                    return "记账失败：金额必须大于0"
                }
                
                // 确定交易类型（优先使用transactionType字段）
                val effectiveTypeStr = transactionTypeStr.ifBlank { safeTypeStr.ifBlank { "expense" } }
                val transactionType = when (effectiveTypeStr.uppercase()) {
                    "INCOME", "收入", "income" -> TransactionType.INCOME
                    "EXPENSE", "支出", "expense" -> TransactionType.EXPENSE
                    "TRANSFER", "转账", "transfer" -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }
                
                // 确定分类名称（优先使用categoryId作为名称，如果它是字符串）
                val effectiveCategoryName = when {
                    categoryName.isNotBlank() -> categoryName
                    categoryIdStr.isNotBlank() && categoryIdLong == -1L -> categoryIdStr
                    else -> ""
                }
                
                // 确定账户名称（优先使用accountId作为名称，如果它是字符串）
                val effectiveAccountName = when {
                    accountName.isNotBlank() -> accountName
                    accountIdStr.isNotBlank() -> accountIdStr
                    else -> ""
                }
                
                if (BuildConfig.DEBUG) {
            Log.d("AIAssistantViewModel", "解析交易字段已提取")
        }
                
                // 确保基础分类存在
                ensureBasicCategoriesExist()
                
                // 查找或创建账户
                var accounts = accountRepository.getAllAccountsList()
                var account = if (effectiveAccountName.isNotBlank()) {
                    // 先尝试精确匹配
                    accounts.find { it.name == effectiveAccountName }
                        // 再尝试包含匹配
                        ?: accounts.find { it.name.contains(effectiveAccountName) || effectiveAccountName.contains(it.name) }
                        // 最后尝试匹配账户类型
                        ?: accounts.find { it.type.name == effectiveAccountName.uppercase() }
                } else {
                    accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                }
                
                // 如果找不到账户，自动创建
                if (account == null) {
                    val accountType = parseAccountType(effectiveAccountName)
                    val newAccountName = effectiveAccountName.ifBlank { "默认账户" }
                    val createAccountOp = AIOperation.AddAccount(
                        name = newAccountName,
                        type = accountType,
                        balance = 0.0
                    )
                    when (val result = aiOperationExecutor.executeOperation(createAccountOp)) {
                        is AIOperationExecutor.AIOperationResult.Success -> {
                            // 重新获取账户列表
                            accounts = accountRepository.getAllAccountsList()
                            account = accounts.find { it.name == newAccountName }
                        }
                        is AIOperationExecutor.AIOperationResult.Error -> {
                            return "记账失败：创建账户失败 - ${result.error}"
                        }
                    }
                }
                
                if (account == null) {
                    return "记账失败：无法创建或找到账户"
                }
                
                // 获取所有分类
                var categories = categoryRepository.getAllCategoriesList()
                
                // 查找或创建分类
                var category = if (effectiveCategoryName.isNotBlank()) {
                    categories.find { it.name == effectiveCategoryName }
                        ?: categories.find { it.name.contains(effectiveCategoryName) || effectiveCategoryName.contains(it.name) }
                } else {
                    categories.firstOrNull { it.type == transactionType }
                }
                
                // 如果找不到分类，自动创建
                if (category == null) {
                    val categoryNameToCreate = effectiveCategoryName.ifBlank { 
                        if (transactionType == TransactionType.INCOME) "其他收入" else "其他支出" 
                    }
                    val createCategoryOp = AIOperation.AddCategory(
                        name = categoryNameToCreate,
                        type = transactionType
                    )
                    when (aiOperationExecutor.executeOperation(createCategoryOp)) {
                        is AIOperationExecutor.AIOperationResult.Success -> {
                            // 重新获取分类列表
                            categories = categoryRepository.getAllCategoriesList()
                            category = categories.find { it.name == categoryNameToCreate }
                        }
                        is AIOperationExecutor.AIOperationResult.Error -> {
                            // 创建失败，使用默认分类
                            category = categories.firstOrNull { it.type == transactionType }
                        }
                    }
                }
                
                // 如果还是没有分类，使用第一个可用分类
                if (category == null) {
                    category = categories.firstOrNull { it.type == transactionType } ?: categories.firstOrNull()
                }
                
                if (category == null) {
                    return "记账失败：无法创建或找到分类"
                }
                
                val operation = AIOperation.AddTransaction(
                    amount = amount,
                    type = transactionType,
                    accountId = account.id,
                    categoryId = category.id,
                    date = dateTimestamp,
                    note = note.ifBlank { "AI记账" }
                )
                
                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationExecutor.AIOperationResult.Success -> 
                        "✅ 已记账：${category.name} ${if (transactionType == TransactionType.INCOME) "收入" else "支出"} ¥$amount"
                    is AIOperationExecutor.AIOperationResult.Error -> 
                        "❌ 记账失败：${result.error}"
                }
            }
            
            "query" -> {
                val target = actionJson.optString("target", "")
                handleQueryCommand(target)
            }
            
            "create_category" -> {
                val name = actionJson.optString("name", "").ifBlank {
                    actionJson.optString("categoryName", "")
                }

                val rawTypeStr = actionJson.optString("type", "")
                val categoryTypeStr = actionJson.optString(
                    "categoryType",
                    actionJson.optString(
                        "transactionType",
                        when (rawTypeStr) {
                            "create_category", "add_transaction", "create_account", "query" -> ""
                            else -> rawTypeStr
                        }
                    )
                ).uppercase()

                val txnType = when (categoryTypeStr) {
                    "INCOME", "收入" -> TransactionType.INCOME
                    "EXPENSE", "支出" -> TransactionType.EXPENSE
                    else -> TransactionType.EXPENSE
                }

                val parentId = when {
                    actionJson.has("parentId") -> actionJson.optLong("parentId").takeIf { it > 0 }
                    actionJson.has("parentCategoryId") -> actionJson.optLong("parentCategoryId").takeIf { it > 0 }
                    else -> null
                }

                if (name.isBlank()) {
                    return "创建分类失败：分类名称不能为空"
                }

                val operation = AIOperation.AddCategory(
                    name = name,
                    type = txnType,
                    parentId = parentId
                )

                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationExecutor.AIOperationResult.Success -> "✅ 已创建分类：$name"
                    is AIOperationExecutor.AIOperationResult.Error -> "❌ 创建分类失败：${result.error}"
                }
            }

            else -> "未知的操作类型：$action"
        }
    }

    /**
     * 解析账户类型 - 支持中英文
     */
    private fun parseAccountType(typeStr: String): com.example.aiaccounting.data.local.entity.AccountType {
        val upper = typeStr.uppercase()
        return when {
            // 英文类型
            upper == "WECHAT" -> com.example.aiaccounting.data.local.entity.AccountType.WECHAT
            upper == "ALIPAY" -> com.example.aiaccounting.data.local.entity.AccountType.ALIPAY
            upper == "CASH" -> com.example.aiaccounting.data.local.entity.AccountType.CASH
            upper == "BANK" -> com.example.aiaccounting.data.local.entity.AccountType.BANK
            upper == "CREDIT_CARD" -> com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD
            upper == "DEBIT_CARD" -> com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD
            // 中文类型
            typeStr.contains("微信") -> com.example.aiaccounting.data.local.entity.AccountType.WECHAT
            typeStr.contains("支付宝") -> com.example.aiaccounting.data.local.entity.AccountType.ALIPAY
            typeStr.contains("现金") -> com.example.aiaccounting.data.local.entity.AccountType.CASH
            typeStr.contains("信用") -> com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD
            typeStr.contains("借记") || typeStr.contains("储蓄") -> com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD
            typeStr.contains("银行") -> com.example.aiaccounting.data.local.entity.AccountType.BANK
            else -> com.example.aiaccounting.data.local.entity.AccountType.OTHER
        }
    }

    /**
     * 生成友好的响应
     */
    private suspend fun generateFriendlyResponse(results: List<String>): String {
        val successCount = results.count { it.startsWith("✅") }
        val failCount = results.count { it.startsWith("❌") }
        
        val summary = when {
            successCount > 0 && failCount == 0 -> "操作成功完成！"
            successCount > 0 && failCount > 0 -> "部分操作已完成，部分失败。"
            else -> "操作执行失败。"
        }
        
        // 获取最新账户信息
        val accounts = accountRepository.getAllAccountsList()
        val totalBalance = accounts.sumOf { it.balance }
        
        val accountSummary = if (accounts.isNotEmpty()) {
            "\n\n📊 当前账户概览：\n" +
            accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" } +
            "\n\n💰 总资产: ¥${String.format("%.2f", totalBalance)}"
        } else ""
        
        return "$summary\n\n${results.joinToString("\n")}$accountSummary"
    }

    /**
     * 确保基础分类存在
     */
    private suspend fun ensureBasicCategoriesExist() {
        aiLocalProcessor.ensureBasicCategoriesExist()
    }

    /**
     * 处理查询命令
     */
    private suspend fun handleQueryCommand(target: String): String {
        return aiLocalProcessor.handleQueryCommand(target)
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
                // 创建新会话
                val session = chatSessionRepository.createSession("新对话 ${System.currentTimeMillis() / 1000}")
                // 更新UI状态
                _uiState.update { it.copy(currentSessionId = session.id) }
                // 清空当前对话显示
                conversationRepository.clearAllConversations()
                // 添加欢迎消息 - 使用当前角色的语气
                val currentButler = _uiState.value.currentButler ?: butlerRepository.getCurrentButler()
                val welcomeMessage = when (currentButler.id) {
                    "xiaocainiang" -> "主人~你好呀！🌸 小财娘在这里等着为您服务呢~\n有什么记账或理财的需求，随时告诉我哦~💕✨"
                    "taotao" -> "主人～你好呀！✨ 桃桃在这里等着为你服务呢～\n有什么需要帮忙的，随时告诉桃桃哦～🌸💕"
                    "guchen" -> "（懒洋洋地）啊...你来了...\n有什么事快说，说完我好继续睡觉...\n不过既然来了，你的财务就交给我吧。"
                    "suqian" -> "（平静地看着你）...\n有事就说。\n你的财务，我会处理好的。"
                    "yishuihan" -> "（温柔地微笑）你好呀～\n别紧张，有我在呢。\n有什么财务上的需要，随时告诉我。"
                    else -> "你好！我是你的AI记账助手。\n有什么记账或理财的需求，随时告诉我。"
                }
                conversationRepository.addAssistantMessage(welcomeMessage)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 切换会话
     */
    fun switchSession(sessionId: String) {
        viewModelScope.launch {
            try {
                // 切换当前会话
                chatSessionRepository.switchSession(sessionId)
                // 更新UI状态
                _uiState.update { it.copy(currentSessionId = sessionId) }
                // 清空当前显示
                conversationRepository.clearAllConversations()
                // 加载该会话的消息
                chatSessionRepository.getMessages(sessionId).first().forEach { msg ->
                    when (msg.role) {
                        com.example.aiaccounting.data.local.entity.MessageRole.USER -> conversationRepository.addUserMessage(msg.content)
                        com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT -> conversationRepository.addAssistantMessage(msg.content)
                        else -> {}
                    }
                }
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
                val allSessions = chatSessionRepository.getAllSessions().first()
                
                if (allSessions.size <= 1) {
                    // 只剩一个会话，清空内容而不是删除
                    if (_uiState.value.currentSessionId == sessionId) {
                        conversationRepository.clearAllConversations()
                        // 重置会话标题
                        chatSessionRepository.updateSessionTitle(sessionId, "新对话")
                        // 添加欢迎消息 - 使用当前角色的语气
                        val currentButler = _uiState.value.currentButler ?: butlerRepository.getCurrentButler()
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
                } else {
                    // 正常删除
                    chatSessionRepository.deleteSession(sessionId)
                    // 如果删除的是当前会话，切换到第一个会话
                    if (_uiState.value.currentSessionId == sessionId) {
                        val remainingSessions = chatSessionRepository.getAllSessions().first()
                        if (remainingSessions.isNotEmpty()) {
                            val firstSession = remainingSessions.first()
                            _uiState.update { it.copy(currentSessionId = firstSession.id) }
                            conversationRepository.clearAllConversations()
                            // 加载第一个会话的消息
                            chatSessionRepository.getMessages(firstSession.id).first().forEach { msg ->
                                when (msg.role) {
                                    com.example.aiaccounting.data.local.entity.MessageRole.USER -> conversationRepository.addUserMessage(msg.content)
                                    com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT -> conversationRepository.addAssistantMessage(msg.content)
                                    else -> {}
                                }
                            }
                        } else {
                            _uiState.update { it.copy(currentSessionId = null) }
                            conversationRepository.clearAllConversations()
                        }
                    }
                }
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
                chatSessionRepository.updateSessionTitle(sessionId, newTitle)
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

                // 检查网络状态
                val isNetworkAvailable = networkUtils.isNetworkAvailable()
                _uiState.update { it.copy(isNetworkAvailable = isNetworkAvailable) }

                if (!isNetworkAvailable) {
                    conversationRepository.addAssistantMessage("📡 网络不可用，无法识别图片。请检查网络连接后重试。")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // 模型是否原生支持图片
                val isNativeImageSupported = aiService.isImageSupported(currentAIConfig)
                // 如果需要远端图片模型，则必须配置；否则允许走本地 OCR
                if (isNativeImageSupported && (!currentAIConfig.isEnabled || currentAIConfig.apiKey.isBlank())) {
                    conversationRepository.addAssistantMessage("🔑 请先配置支持图片的模型和 API 密钥，才能使用图片识别功能。")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // 确保有当前会话
                val sessionId = getOrCreateCurrentSession()

                // 添加用户消息（只显示文字，不显示图片数量）
                val displayMessage = message.ifBlank { "发送了${imageUris.size}张图片" }
                
                // 保存图片URI列表
                val imageUriStrings = imageUris.map { it.toString() }
                conversationRepository.addUserMessageWithImages(displayMessage, imageUriStrings)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.USER, displayMessage, imageUriStrings)

                // 使用之前已声明的 isNativeImageSupported 变量
                
                // 构建发送给AI的提示词
                val currentButler = _uiState.value.currentButler ?: butlerRepository.getCurrentButler()
                val aiPrompt = if (isNativeImageSupported) {
                    // 原生支持图片的模型 - 使用当前角色的系统提示词
                    buildString {
                        appendLine(currentButler.systemPrompt)
                        appendLine()
                        if (message.isNotBlank()) {
                            appendLine("用户说：$message")
                            appendLine()
                        }
                        appendLine("用户发了${imageUris.size}张图片，请分析其中的消费信息并返回JSON格式执行记账。")
                    }
                } else {
                    // 普通模型 - 使用本地 OCR 并行处理
                    val analysisResults = imageProcessingService.analyzeMultipleImages(
                        imageUris, context, timeoutMs = 8000
                    )
                    val hasContent = analysisResults.any { it.hasContent }
                    if (!hasContent) {
                        conversationRepository.addAssistantMessage("😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的账单照片再试一次。")
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    // 生成精简提示词
                    imageProcessingService.generateCompactPrompt(analysisResults, message)
                }
                
                // 发送给AI处理（增加超时到90秒）
                val chatMessages = listOf(
                    ChatMessage(
                        role = MessageRole.USER,
                        content = aiPrompt
                    )
                )
                
                val aiResponse = withTimeoutOrNull(90000L) {
                    aiService.chat(chatMessages, currentAIConfig)
                }
                
                // 记录调用
                val hasError = aiResponse == null
                aiUsageRepository.recordCall(success = !hasError)
                
                // AI回复
                val finalMessage = if (aiResponse != null) {
                    aiResponse
                } else {
                    "图片处理超时，请稍后重试。"
                }
                
                conversationRepository.addAssistantMessage(finalMessage)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, finalMessage)
                _uiState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                aiUsageRepository.recordCall(success = false)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                conversationRepository.addAssistantMessage(
                    "处理图片时出错: ${e.message}"
                )
            }
        }
    }

    /**
     * 执行图片识别出的记账操作
     */
    private suspend fun executeImageActions(actions: List<ImageAction>): List<String> {
        val results = mutableListOf<String>()
        
        actions.forEach { action ->
            when (action.action) {
                "add_transaction" -> {
                    val result = executeImageTransaction(action)
                    results.add(result)
                }
                else -> {
                    results.add("未知操作: ${action.action}")
                }
            }
        }
        
        return results
    }

    /**
     * 执行图片识别出的单笔交易
     */
    private suspend fun executeImageTransaction(action: ImageAction): String {
        if (action.amount <= 0) {
            return "❌ 记账失败：金额必须大于0"
        }

        // 查找账户
        val accounts = accountRepository.getAllAccountsList()
        val account = if (action.account.isNotBlank()) {
            accounts.find { it.name.contains(action.account) || action.account.contains(it.name) }
        } else {
            accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        }

        if (account == null) {
            return "❌ 记账失败：未找到合适的账户"
        }

        // 确定交易类型
        val transactionType = if (action.type == "income") TransactionType.INCOME else TransactionType.EXPENSE

        // 确保基础分类存在
        ensureBasicCategoriesExist()

        // 获取所有分类
        var categories = categoryRepository.getAllCategoriesList()

        // 查找或创建分类
        var category = if (action.category.isNotBlank()) {
            categories.find { it.name.contains(action.category) || action.category.contains(it.name) }
        } else {
            categories.firstOrNull { it.type == transactionType }
        }

        // 如果找不到分类，自动创建
        if (category == null && action.category.isNotBlank()) {
            val createCategoryOp = AIOperation.AddCategory(
                name = action.category,
                type = transactionType
            )
            when (aiOperationExecutor.executeOperation(createCategoryOp)) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    categories = categoryRepository.getAllCategoriesList()
                    category = categories.find { it.name == action.category }
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    category = categories.firstOrNull { it.type == transactionType }
                }
            }
        }

        if (category == null) {
            return "❌ 记账失败：无法创建或找到分类"
        }

        val operation = AIOperation.AddTransaction(
            amount = action.amount,
            type = transactionType,
            accountId = account.id,
            categoryId = category.id,
            note = action.note.ifBlank { "图片识别记账" }
        )

        return when (val result = aiOperationExecutor.executeOperation(operation)) {
            is AIOperationExecutor.AIOperationResult.Success ->
                "✅ 已记账：${category.name} ${if (transactionType == TransactionType.INCOME) "收入" else "支出"} ¥${action.amount}"
            is AIOperationExecutor.AIOperationResult.Error ->
                "❌ 记账失败：${result.error}"
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
