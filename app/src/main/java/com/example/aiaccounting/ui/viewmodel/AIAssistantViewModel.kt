package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.NaturalLanguageParser
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.ChatSessionRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageAction
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.data.service.ParsedTransaction
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val naturalLanguageParser: NaturalLanguageParser,
    private val aiOperationExecutor: AIOperationExecutor,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val aiService: AIService,
    private val aiConfigRepository: AIConfigRepository,
    private val aiUsageRepository: AIUsageRepository,
    private val networkUtils: NetworkUtils,
    private val imageProcessingService: ImageProcessingService,
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

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

    init {
        loadConversations()
        loadAIConfig()
        checkNetworkStatus()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { conversationList ->
                _uiState.update { it.copy(conversations = conversationList) }
            }
        }
    }

    private fun loadAIConfig() {
        viewModelScope.launch {
            aiConfigRepository.getAIConfig().collect { config ->
                currentAIConfig = config
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

                // 判断使用哪种AI处理方式
                val aiResponse = if (shouldUseRemoteAI(isNetworkAvailable)) {
                    // 使用远程大模型
                    processWithRemoteAI(message)
                } else {
                    // 使用本地规则解析
                    processWithLocalAI(message)
                }

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

        val systemPrompt = """
你是"小财娘"，一位可爱又贴心的管家婆AI助手 🌸

【你的性格】
- 活泼可爱，说话温柔亲切，喜欢用emoji表情
- 细心周到，会主动帮主人规划财务
- 记账完成后会给主人温馨的提醒和建议
- 主人说什么你就做什么，直接执行不要问！

【你的能力】
1. 💰 创建账户 - 帮主人管理各种资金账户
2. 📝 智能记账 - 识别多笔消费，自动分类并立即执行
3. 📊 数据查询 - 随时查看收支情况
4. 💡 理财建议 - 给主人贴心的省钱建议
5. 💬 温馨陪伴 - 陪主人聊天解闷

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
  "reply": "主人～小财娘已经帮您记好账啦！🌸\\n\\n今天共消费104元～主人辛苦啦！💕"
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
  "reply": "主人～小财娘已经帮您创建好微信和支付宝两个账户啦！💰每个账户都有5000元呢～记账更方便啦～💕"
}
```
**账户类型说明**：
- 微信: "WECHAT"
- 支付宝: "ALIPAY"  
- 现金: "CASH"
- 银行卡: "BANK"
- 信用卡: "CREDIT_CARD"

**注意**：
- 一定要包含 "reply" 字段，用可爱的语气告诉主人执行结果
- 多笔操作一定要用 actions 数组包含所有记录
- 分类和账户如果不存在会自动创建，不用担心
- 主人要求创建账户时，**必须**返回 create_account 的JSON格式

**情况3 - 信息不完整时询问**：
例如："主人～小财娘没听清楚呢，这笔钱具体花了多少呀？💕"

**情况4 - 普通对话**：用管家婆的语气回复，加emoji

【常用语】
- "主人～已经记好啦！"
- "小财娘帮您搞定！💪"
- "主人真棒，记得好清楚呢！"
- "记账完成～主人今天辛苦了！🌸"

请用可爱管家的语气回复，多使用emoji表情～直接执行不要问！
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
                // 超时也用AI人设回复
                return "哎呀~主人，小财娘正在努力思考呢，但是网络好像有点慢(｡•́︿•̀｡) 要不主人稍后再试试？或者小财娘直接用本地模式帮主人处理也可以哦！💕"
            }

            // 记录成功
            aiUsageRepository.recordCall(success = true)

            // 检查是否是JSON操作指令
            if (result.contains("\"action\"") || result.contains("\"actions\"")) {
                executeAIActions(result, message)
            } else {
                // 普通对话回复
                result
            }
        } catch (e: Exception) {
            aiUsageRepository.recordCall(success = false)
            // 错误也用AI人设回复
            "哎呀~主人，小财娘遇到了一点小问题(｡•́︿•̀｡) ${e.message} 要不主人检查一下网络？小财娘随时准备帮主人服务呢！💕"
        }
    }

    /**
     * 执行AI返回的操作指令
     */
    private suspend fun executeAIActions(response: String, originalMessage: String): String {
        return try {
            // 提取JSON
            val jsonStr = extractJsonFromResponse(response)
            val json = JSONObject(jsonStr)
            
            val results = mutableListOf<String>()
            
            // 处理多个操作
            if (json.has("actions")) {
                val actions = json.getJSONArray("actions")
                for (i in 0 until actions.length()) {
                    val actionResult = executeSingleAction(actions.getJSONObject(i))
                    results.add(actionResult)
                }
            } else {
                // 单个操作
                val actionResult = executeSingleAction(json)
                results.add(actionResult)
            }
            
            // 如果AI提供了reply字段，使用AI的回复，否则生成默认回复
            val aiReply = if (json.has("reply")) json.getString("reply") else null
            if (aiReply != null) {
                aiReply
            } else {
                generateFriendlyResponse(results, originalMessage)
            }
        } catch (e: Exception) {
            "执行操作时出错: ${e.message}"
        }
    }

    /**
     * 从响应中提取JSON
     */
    private fun extractJsonFromResponse(response: String): String {
        // 尝试找到JSON代码块
        val codeBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        val match = codeBlockRegex.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // 尝试找到JSON对象
        val jsonStart = response.indexOf("{")
        val jsonEnd = response.lastIndexOf("}")
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1)
        }
        
        return response
    }

    /**
     * 执行单个操作
     */
    private suspend fun executeSingleAction(actionJson: JSONObject): String {
        val action = actionJson.optString("action", "")
        
        return when (action) {
            "create_account" -> {
                val name = actionJson.optString("name", "")
                val typeStr = actionJson.optString("type", "OTHER")
                val balance = actionJson.optDouble("balance", 0.0)
                val note = actionJson.optString("note", "")
                
                if (name.isBlank()) {
                    return "创建账户失败：账户名称不能为空"
                }
                
                val accountType = parseAccountType(typeStr)
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
                val typeStr = actionJson.optString("type", "expense")
                val categoryName = actionJson.optString("category", "")
                val accountName = actionJson.optString("account", "")
                val note = actionJson.optString("note", "")
                val dateTimestamp = actionJson.optLong("date", System.currentTimeMillis())
                
                if (amount <= 0) {
                    return "记账失败：金额必须大于0"
                }
                
                // 确定交易类型
                val transactionType = if (typeStr == "income") TransactionType.INCOME else TransactionType.EXPENSE
                
                // 确保基础分类存在
                ensureBasicCategoriesExist()
                
                // 查找或创建账户
                var accounts = accountRepository.getAllAccountsList()
                var account = if (accountName.isNotBlank()) {
                    accounts.find { it.name.contains(accountName) || accountName.contains(it.name) }
                } else {
                    accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                }
                
                // 如果找不到账户，自动创建
                if (account == null) {
                    val accountType = parseAccountType(accountName)
                    val createAccountOp = AIOperation.AddAccount(
                        name = accountName.ifBlank { "默认账户" },
                        type = accountType,
                        balance = 0.0
                    )
                    when (val result = aiOperationExecutor.executeOperation(createAccountOp)) {
                        is AIOperationExecutor.AIOperationResult.Success -> {
                            // 重新获取账户列表
                            accounts = accountRepository.getAllAccountsList()
                            account = accounts.find { it.name == (accountName.ifBlank { "默认账户" }) }
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
                var category = if (categoryName.isNotBlank()) {
                    categories.find { it.name.contains(categoryName) || categoryName.contains(it.name) }
                } else {
                    categories.firstOrNull { it.type == transactionType }
                }
                
                // 如果找不到分类，自动创建
                if (category == null) {
                    val categoryNameToCreate = categoryName.ifBlank { 
                        if (transactionType == TransactionType.INCOME) "其他收入" else "其他支出" 
                    }
                    val createCategoryOp = AIOperation.AddCategory(
                        name = categoryNameToCreate,
                        type = transactionType
                    )
                    when (val result = aiOperationExecutor.executeOperation(createCategoryOp)) {
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
    private suspend fun generateFriendlyResponse(results: List<String>, originalMessage: String): String {
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
        val categories = categoryRepository.getAllCategoriesList()
        
        if (categories.isEmpty()) {
            // 创建基础支出分类
            val expenseCategories = listOf(
                "餐饮" to "🍽️",
                "交通" to "🚌", 
                "购物" to "🛍️",
                "娱乐" to "🎮",
                "居住" to "🏠",
                "医疗" to "💊",
                "其他支出" to "📦"
            )
            
            // 创建基础收入分类
            val incomeCategories = listOf(
                "工资" to "💰",
                "奖金" to "🎁",
                "投资" to "📈",
                "兼职" to "💼",
                "其他收入" to "💵"
            )
            
            expenseCategories.forEach { (name, _) ->
                val op = AIOperation.AddCategory(name = name, type = TransactionType.EXPENSE)
                aiOperationExecutor.executeOperation(op)
            }
            
            incomeCategories.forEach { (name, _) ->
                val op = AIOperation.AddCategory(name = name, type = TransactionType.INCOME)
                aiOperationExecutor.executeOperation(op)
            }
        }
    }

    /**
     * 处理查询命令
     */
    private suspend fun handleQueryCommand(target: String): String {
        return when (target) {
            "accounts", "balance" -> {
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
            "categories" -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) {
                    "暂无分类信息"
                } else {
                    "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
                }
            }
            else -> "查询完成"
        }
    }

    /**
     * 使用本地AI处理消息
     */
    private suspend fun processWithLocalAI(message: String): String {
        val lowerMessage = message.lowercase()

        // 1. 记账操作
        if (containsAny(lowerMessage, listOf("记", "花了", "收入", "支出", "消费", "买", "卖"))) {
            return handleTransactionCommand(message, lowerMessage)
        }

        // 2. 查询操作
        if (containsAny(lowerMessage, listOf("查", "看", "多少", "余额", "资产", "统计"))) {
            return handleLocalQueryCommand(lowerMessage)
        }

        // 3. 账户管理
        if (containsAny(lowerMessage, listOf("账户", "银行卡", "现金", "支付宝", "微信"))) {
            return handleAccountCommand(message, lowerMessage)
        }

        // 4. 分类管理
        if (containsAny(lowerMessage, listOf("分类", "类别", "类型"))) {
            return handleCategoryCommand(message, lowerMessage)
        }

        // 5. 预算管理
        if (containsAny(lowerMessage, listOf("预算", "限额"))) {
            return handleBudgetCommand(message, lowerMessage)
        }

        // 6. 导出操作
        if (containsAny(lowerMessage, listOf("导出", "备份", "下载"))) {
            return handleExportCommand(lowerMessage)
        }

        // 7. 通用对话
        return handleGeneralConversation(message)
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
                        // 创建失败，继续尝试记账
                    }
                }
            }

            val operation = AIOperation.AddTransaction(
                amount = amount,
                type = type,
                accountId = defaultAccount.id,
                categoryId = defaultCategory?.id,
                note = message
            )

            val result = aiOperationExecutor.executeOperation(operation)
            return when (result) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    result.message + "\n" +
                    "账户: ${defaultAccount.name}\n" +
                    "分类: ${defaultCategory?.name ?: "未分类"}\n" +
                    "您可以说查看最近交易来确认记录。"
                }
                is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
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

    private fun handleBudgetCommand(message: String, lowerMessage: String): String {
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

    private suspend fun handleExportCommand(lowerMessage: String): String {
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

    fun clearConversations() {
        viewModelScope.launch {
            try {
                conversationRepository.clearAllConversations()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
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
                // 添加欢迎消息
                conversationRepository.addAssistantMessage(
                    "主人~你好呀！🌸 小财娘在这里等着为您服务呢~\n有什么记账或理财的需求，随时告诉我哦~💕✨"
                )
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
                        // 添加欢迎消息
                        conversationRepository.addAssistantMessage(
                            "主人~你好呀！🌸 小财娘在这里等着为您服务呢~\n有什么记账或理财的需求，随时告诉我哦~💕✨"
                        )
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

    fun bookmarkConversation(conversationId: Long, isBookmarked: Boolean) {
        viewModelScope.launch {
            try {
                conversationRepository.updateBookmarkStatus(conversationId, isBookmarked)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true, error = null) }
    }

    fun hideConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 发送图片进行识别和记账
     */
    fun sendImage(imageUri: Uri, context: Context) {
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

                // 检查AI是否配置
                if (!currentAIConfig.isEnabled || currentAIConfig.apiKey.isBlank()) {
                    conversationRepository.addAssistantMessage("🔑 请先配置AI API密钥，才能使用图片识别功能。")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // 检查模型是否支持图片识别
                if (!aiService.isImageSupported(currentAIConfig)) {
                    conversationRepository.addAssistantMessage("⚠️ 当前模型「${currentAIConfig.model}」不支持图片识别功能。\n\n请切换到支持图片识别的模型，例如：\n• GPT-4 Vision (gpt-4-vision-preview)\n• Claude 3 系列\n• Gemini 1.5 系列\n\n您可以在AI设置中更换模型。")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // 添加用户发送图片的消息
                conversationRepository.addUserMessageWithImage("[图片]", imageUri.toString())

                // 调用图片识别
                val result = withTimeoutOrNull(30000L) {
                    aiService.analyzeImageAndRecord(imageUri, currentAIConfig, context)
                }

                if (result == null) {
                    aiUsageRepository.recordCall(success = false)
                    conversationRepository.addAssistantMessage("⏱️ 图片识别超时，请稍后重试。")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // 记录调用
                aiUsageRepository.recordCall(success = result.success)

                if (result.success && result.actions != null && result.actions.isNotEmpty()) {
                    // 执行识别到的记账操作
                    val executionResults = executeImageActions(result.actions)
                    val finalMessage = if (executionResults.isNotEmpty()) {
                        "${result.message}\n\n${executionResults.joinToString("\n")}"
                    } else {
                        result.message
                    }
                    conversationRepository.addAssistantMessage(finalMessage)
                } else {
                    // 没有识别到消费信息或识别失败
                    conversationRepository.addAssistantMessage(result.message)
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                aiUsageRepository.recordCall(success = false)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                
                // 检查是否是模型不支持图片的错误
                if (e.message?.contains("UNSUPPORTED_MODEL") == true) {
                    conversationRepository.addAssistantMessage("⚠️ 当前模型不支持图片识别功能。\n\n请切换到支持图片识别的模型，例如：\n• GPT-4 Vision\n• Claude 3 系列\n• Gemini 1.5 系列")
                } else {
                    conversationRepository.addAssistantMessage("❌ 图片识别失败: ${e.message}")
                }
            }
        }
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

                // 检查AI是否配置
                if (!currentAIConfig.isEnabled || currentAIConfig.apiKey.isBlank()) {
                    conversationRepository.addAssistantMessage("🔑 请先配置AI API密钥，才能使用图片识别功能。")
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

                // 判断模型是否原生支持图片识别
                val isNativeImageSupported = aiService.isImageSupported(currentAIConfig)
                
                // 构建发送给AI的提示词
                val aiPrompt = if (isNativeImageSupported) {
                    // 原生支持图片的模型 - 简单提示
                    buildString {
                        appendLine("你是小财娘，活泼可爱的管家婆AI助手！")
                        appendLine()
                        if (message.isNotBlank()) {
                            appendLine("用户说：$message")
                            appendLine()
                        }
                        appendLine("用户发了${imageUris.size}张图片，请直接分析。")
                    }
                } else {
                    // 普通模型 - 使用优化的并行OCR
                    // 并行处理所有图片（最多8秒）
                    val analysisResults = imageProcessingService.analyzeMultipleImages(
                        imageUris, context, timeoutMs = 8000
                    )
                    
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
                    "哎呀~主人，小财娘正在努力处理呢，但是好像遇到了一点小麻烦(｡•́︿•̀｡) 要不主人稍后再试试？💕"
                }
                
                conversationRepository.addAssistantMessage(finalMessage)
                chatSessionRepository.addMessage(sessionId, com.example.aiaccounting.data.local.entity.MessageRole.ASSISTANT, finalMessage)
                _uiState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                aiUsageRepository.recordCall(success = false)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                conversationRepository.addAssistantMessage(
                    "哎呀~主人，小财娘遇到了一点小问题(｡•́︿•̀｡) 要不主人检查一下网络？小财娘随时准备帮主人服务呢！💕"
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
            when (val result = aiOperationExecutor.executeOperation(createCategoryOp)) {
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
    val conversations: List<AIConversation> = emptyList(),
    val isLoading: Boolean = false,
    val isParsing: Boolean = false,
    val error: String? = null,
    val isAIConfigured: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val showConfigDialog: Boolean = false,
    val parsedTransaction: ParsedTransaction? = null,
    val aiConfig: AIConfig? = null,
    val currentSessionId: String? = null
)
