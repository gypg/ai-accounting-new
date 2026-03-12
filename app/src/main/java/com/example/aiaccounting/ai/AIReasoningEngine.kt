package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI推理引擎
 * 负责理解用户意图、分析上下文、自主决策调用哪些功能
 * 
 * 【重要】处理优先级：
 * 1. 身份确认询问（最高优先级）
 * 2. 信息查询和数据分析
 * 3. 记账操作
 * 4. 普通对话
 */
@Singleton
class AIReasoningEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val aiInformationSystem: AIInformationSystem,
    private val identityConfirmationDetector: IdentityConfirmationDetector,
    private val transactionModificationHandler: TransactionModificationHandler,
    private val aiOperationExecutor: AIOperationExecutor
) {

    /**
     * 用户意图类型
     */
    enum class UserIntent {
        IDENTITY_CONFIRMATION,      // 身份确认（最高优先级）
        MODIFY_TRANSACTION,         // 修改交易记录
        DELETE_TRANSACTION,         // 删除交易记录
        RECORD_TRANSACTION,         // 记账
        QUERY_INFORMATION,          // 查询信息
        ANALYZE_DATA,               // 数据分析
        MANAGE_ACCOUNT,             // 管理账户
        MANAGE_CATEGORY,            // 管理分类
        GENERAL_CONVERSATION,       // 普通对话
        UNKNOWN                     // 未知意图
    }

    /**
     * 推理上下文
     */
    data class ReasoningContext(
        val userMessage: String,
        val conversationHistory: List<String> = emptyList(),
        val currentAccounts: List<com.example.aiaccounting.data.local.entity.Account> = emptyList(),
        val currentCategories: List<com.example.aiaccounting.data.local.entity.Category> = emptyList(),
        val lastQueryTime: Long? = null
    )

    /**
     * 推理结果
     */
    data class ReasoningResult(
        val intent: UserIntent,
        val confidence: Float,
        val actions: List<AIAction>,
        val reasoningExplanation: String,
        val requiresUserConfirmation: Boolean = false
    )

    /**
     * AI动作
     */
    sealed class AIAction {
        data class RecordTransaction(
            val amount: Double,
            val type: TransactionType,
            val categoryHint: String?,
            val accountHint: String?,
            val note: String,
            val date: Long
        ) : AIAction()

        data class QueryInformation(
            val queryType: AIInformationSystem.QueryType,
            val startDate: Long? = null,
            val endDate: Long? = null,
            val parameters: Map<String, String> = emptyMap()
        ) : AIAction()

        data class AnalyzeData(
            val analysisType: AnalysisType,
            val scope: AnalysisScope
        ) : AIAction()

        data class GenerateResponse(
            val responseContent: String
        ) : AIAction()

        data class RequestClarification(
            val question: String
        ) : AIAction()
    }

    /**
     * 分析类型
     */
    enum class AnalysisType {
        EXPENSE_STRUCTURE,      // 支出结构分析
        INCOME_STRUCTURE,       // 收入结构分析
        TREND_ANALYSIS,         // 趋势分析
        COMPARISON_ANALYSIS,    // 对比分析
        BUDGET_ANALYSIS,        // 预算分析
        COMPREHENSIVE_REPORT    // 综合报告
    }

    /**
     * 分析范围
     */
    enum class AnalysisScope {
        TODAY,          // 今天
        YESTERDAY,      // 昨天
        THIS_WEEK,      // 本周
        THIS_MONTH,     // 本月
        LAST_MONTH,     // 上月
        THIS_YEAR,      // 今年
        LAST_7_DAYS,    // 最近7天
        LAST_30_DAYS,   // 最近30天
        CUSTOM          // 自定义
    }

    /**
     * 执行推理
     * 
     * 【处理优先级】
     * 1. 身份确认询问（最高优先级）- 必须先确认身份
     * 2. 交易修改/删除请求
     * 3. 信息查询和数据分析
     * 4. 记账操作
     * 5. 普通对话
     */
    suspend fun reason(context: ReasoningContext, currentButlerId: String): ReasoningResult {
        // 【第一步】检测身份确认询问（最高优先级）
        val identityQueryResult = identityConfirmationDetector.detectIdentityQuery(context.userMessage)
        
        if (identityQueryResult.isIdentityQuery) {
            // 生成身份确认回复
            val identityResponse = identityConfirmationDetector.generateIdentityResponse(
                currentButlerId, 
                identityQueryResult
            )
            
            // 检查是否同时包含功能请求
            val hasFunctionRequest = identityConfirmationDetector.hasMixedIntent(context.userMessage)
            
            return if (hasFunctionRequest) {
                // 混合意图：先确认身份，然后处理功能请求
                val functionPart = identityConfirmationDetector.extractFunctionPart(context.userMessage)
                val functionActions = processFunctionRequest(functionPart, context)
                
                ReasoningResult(
                    intent = UserIntent.IDENTITY_CONFIRMATION,
                    confidence = identityQueryResult.confidence,
                    actions = listOf(
                        AIAction.GenerateResponse(identityResponse),
                        *functionActions.toTypedArray()
                    ),
                    reasoningExplanation = "检测到身份确认询问+功能请求，先确认身份，再执行功能",
                    requiresUserConfirmation = false
                )
            } else {
                // 纯身份询问
                ReasoningResult(
                    intent = UserIntent.IDENTITY_CONFIRMATION,
                    confidence = identityQueryResult.confidence,
                    actions = listOf(AIAction.GenerateResponse(identityResponse)),
                    reasoningExplanation = "检测到身份确认询问，优先处理",
                    requiresUserConfirmation = false
                )
            }
        }
        
        // 【第二步】检测交易修改/删除意图
        val modificationRequest = transactionModificationHandler.detectModificationIntent(context.userMessage)
        
        if (modificationRequest.intent != TransactionModificationHandler.ModificationIntent.UNKNOWN &&
            modificationRequest.targetTransaction != null) {
            
            // 生成修改确认
            val confirmation = transactionModificationHandler.generateModificationConfirmation(modificationRequest)
            
            if (confirmation != null) {
                val intent = when (modificationRequest.intent) {
                    TransactionModificationHandler.ModificationIntent.MODIFY_LAST_TRANSACTION,
                    TransactionModificationHandler.ModificationIntent.MODIFY_SPECIFIC_TRANSACTION -> UserIntent.MODIFY_TRANSACTION
                    TransactionModificationHandler.ModificationIntent.DELETE_LAST_TRANSACTION,
                    TransactionModificationHandler.ModificationIntent.DELETE_SPECIFIC_TRANSACTION -> UserIntent.DELETE_TRANSACTION
                    else -> UserIntent.UNKNOWN
                }
                
                val personalityMessage = transactionModificationHandler.generatePersonalityConfirmationMessage(
                    currentButlerId, confirmation
                )
                
                return ReasoningResult(
                    intent = intent,
                    confidence = modificationRequest.confidence,
                    actions = listOf(AIAction.GenerateResponse(personalityMessage)),
                    reasoningExplanation = "检测到交易修改/删除请求，生成确认信息",
                    requiresUserConfirmation = true
                )
            }
        }
        
        // 【第三步】分析其他意图
        val intentAnalysis = analyzeIntent(context.userMessage)
        
        // 【第四步】根据意图生成动作序列
        // 【权限统一】所有人格拥有完全相同的操作权限，统一使用 "unified" ID
        val actions = when (intentAnalysis.intent) {
            UserIntent.QUERY_INFORMATION -> generateQueryActions(context, intentAnalysis)
            UserIntent.ANALYZE_DATA -> generateAnalysisActions(context, intentAnalysis)
            UserIntent.RECORD_TRANSACTION -> generateTransactionActions(context, intentAnalysis, "unified")
            UserIntent.MANAGE_ACCOUNT -> generateAccountManagementActions(context, intentAnalysis)
            UserIntent.MANAGE_CATEGORY -> generateCategoryManagementActions(context, intentAnalysis)
            UserIntent.GENERAL_CONVERSATION -> generateConversationActions(context, intentAnalysis)
            UserIntent.UNKNOWN -> listOf(AIAction.RequestClarification("抱歉，我不太理解您的意思。您可以尝试说：\n• 查看账户余额\n• 分析本月支出\n• 记一笔100元的餐饮消费"))
            else -> listOf(AIAction.RequestClarification("抱歉，我不太理解您的意思。"))
        }

        return ReasoningResult(
            intent = intentAnalysis.intent,
            confidence = intentAnalysis.confidence,
            actions = actions,
            reasoningExplanation = generateExplanation(intentAnalysis, actions),
            requiresUserConfirmation = shouldRequireConfirmation(actions)
        )
    }
    
    /**
     * 处理功能请求
     * 
     * 【权限统一】所有人格拥有完全相同的操作权限
     */
    private suspend fun processFunctionRequest(
        message: String, 
        context: ReasoningContext
    ): List<AIAction> {
        val intentAnalysis = analyzeIntent(message)
        
        // 所有人格统一使用 "unified" ID，确保权限完全一致
        return when (intentAnalysis.intent) {
            UserIntent.QUERY_INFORMATION -> generateQueryActions(context, intentAnalysis)
            UserIntent.ANALYZE_DATA -> generateAnalysisActions(context, intentAnalysis)
            UserIntent.RECORD_TRANSACTION -> generateTransactionActions(context, intentAnalysis, "unified")
            UserIntent.MANAGE_ACCOUNT -> generateAccountManagementActions(context, intentAnalysis)
            UserIntent.MANAGE_CATEGORY -> generateCategoryManagementActions(context, intentAnalysis)
            else -> emptyList()
        }
    }

    /**
     * 分析用户意图
     */
    private fun analyzeIntent(message: String): IntentAnalysis {
        val lowerMessage = message.lowercase()
        
        // 记账意图特征
        val transactionPatterns = listOf(
            "花了", "消费", "支出", "收入", "转账", "买", "卖", "付", "赚",
            "记账", "记录", "花了", "用了", "收到", "工资", "奖金"
        )
        
        // 查询意图特征
        val queryPatterns = listOf(
            "查", "看", "显示", "告诉", "多少", "余额", "资产",
            "账户", "分类", "记录", "明细", "账单", "有哪些"
        )
        
        // 分析意图特征
        val transactionScore = transactionPatterns.count { lowerMessage.contains(it) }
        val queryScore = queryPatterns.count { lowerMessage.contains(it) }
        
        // 判断意图
        return when {
            // 高置信度记账
            transactionScore >= 2 || (transactionScore >= 1 && containsAmount(message)) -> {
                IntentAnalysis(UserIntent.RECORD_TRANSACTION, 0.85f)
            }
            
            // 高置信度查询
            queryScore >= 2 && !containsActionKeywords(lowerMessage) -> {
                IntentAnalysis(UserIntent.QUERY_INFORMATION, 0.80f)
            }
            
            // 分析意图
            containsAnalysisKeywords(lowerMessage) -> {
                IntentAnalysis(UserIntent.ANALYZE_DATA, 0.75f)
            }
            
            // 账户管理
            containsAccountManagementKeywords(lowerMessage) -> {
                IntentAnalysis(UserIntent.MANAGE_ACCOUNT, 0.70f)
            }
            
            // 分类管理
            containsCategoryManagementKeywords(lowerMessage) -> {
                IntentAnalysis(UserIntent.MANAGE_CATEGORY, 0.70f)
            }
            
            // 问候或简单对话
            isGreetingOrSimpleConversation(lowerMessage) -> {
                IntentAnalysis(UserIntent.GENERAL_CONVERSATION, 0.90f)
            }
            
            else -> IntentAnalysis(UserIntent.UNKNOWN, 0.30f)
        }
    }

    /**
     * 生成查询动作
     */
    private suspend fun generateQueryActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis
    ): List<AIAction> {
        val message = context.userMessage.lowercase()
        val actions = mutableListOf<AIAction>()
        
        // 智能推断查询类型
        val queryType = inferQueryType(message)
        val dateRange = extractDateRange(message)
        
        // 根据查询类型生成相应动作
        when (queryType) {
            AIInformationSystem.QueryType.ACCOUNT_INFO -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                ))
            }
            
            AIInformationSystem.QueryType.TRANSACTION_LIST -> {
                val limit = extractLimit(message) ?: 10
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.TRANSACTION_LIST,
                    startDate = dateRange.first,
                    endDate = dateRange.second,
                    parameters = mapOf("limit" to limit.toString())
                ))
            }
            
            AIInformationSystem.QueryType.TRANSACTION_SUMMARY -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.TRANSACTION_SUMMARY,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                ))
            }
            
            AIInformationSystem.QueryType.EXPENSE_ANALYSIS -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.EXPENSE_ANALYSIS,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                ))
            }
            
            AIInformationSystem.QueryType.INCOME_ANALYSIS -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.INCOME_ANALYSIS,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                ))
            }
            
            AIInformationSystem.QueryType.TREND_ANALYSIS -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.TREND_ANALYSIS,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                ))
            }
            
            AIInformationSystem.QueryType.COMPARISON_ANALYSIS -> {
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.COMPARISON_ANALYSIS
                ))
            }
            
            else -> {
                // 默认查询账户信息
                actions.add(AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                ))
            }
        }
        
        return actions
    }

    /**
     * 生成分析动作
     */
    private suspend fun generateAnalysisActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis
    ): List<AIAction> {
        val message = context.userMessage.lowercase()
        val actions = mutableListOf<AIAction>()
        
        // 推断分析类型
        val analysisType = when {
            message.contains("支出") || message.contains("消费") || message.contains("钱花") -> 
                AnalysisType.EXPENSE_STRUCTURE
            message.contains("收入") -> AnalysisType.INCOME_STRUCTURE
            message.contains("趋势") || message.contains("走势") -> AnalysisType.TREND_ANALYSIS
            message.contains("对比") || message.contains("比较") -> AnalysisType.COMPARISON_ANALYSIS
            message.contains("预算") -> AnalysisType.BUDGET_ANALYSIS
            else -> AnalysisType.COMPREHENSIVE_REPORT
        }
        
        // 推断分析范围
        val scope = extractAnalysisScope(message)
        
        actions.add(AIAction.AnalyzeData(analysisType, scope))
        
        return actions
    }

    /**
     * 生成交易记录动作
     * 
     * 【权限声明】所有五个人格（小财娘、桃桃、顾沉、苏浅、易水寒）拥有完全相同的操作权限：
     * - 记账：创建收入/支出/转账记录
     * - 查询：查询账户、交易、统计数据
     * - 管理：创建/修改账户、分类
     * - 分析：数据分析、趋势预测
     * 
     * 所有操作都会实际执行数据库写入，不受人格限制
     */
    private suspend fun generateTransactionActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis,
        currentButlerId: String
    ): List<AIAction> {
        val message = context.userMessage
        val actions = mutableListOf<AIAction>()
        
        // 提取金额
        val amount = extractAmount(message) ?: return listOf(
            AIAction.RequestClarification("请问这笔交易的金额是多少呢？")
        )
        
        // 判断交易类型
        val type = when {
            message.contains("收入") || message.contains("收到") || message.contains("工资") || 
            message.contains("奖金") || message.contains("赚") -> TransactionType.INCOME
            message.contains("转账") -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        
        // 智能推断分类
        val categoryHint = inferCategory(message, type)
        
        // 智能推断账户
        val accountHint = inferAccount(message)
        
        // 提取备注
        val note = extractNote(message) ?: "AI记账"
        
        // 提取日期
        val date = extractTransactionDate(message)
        
        // 添加不情愿回复（如果是顾沉或苏浅）
        val reluctantResponse = generateReluctantResponse(currentButlerId, "记账")
        if (reluctantResponse != null) {
            actions.add(AIAction.GenerateResponse(reluctantResponse))
        }
        
        actions.add(AIAction.RecordTransaction(
            amount = amount,
            type = type,
            categoryHint = categoryHint,
            accountHint = accountHint,
            note = note,
            date = date
        ))
        
        return actions
    }
    
    /**
     * 生成不情愿回复
     * 当人格执行非擅长功能时，表现出不情愿态度
     * 
     * 【重要】所有人格都有完全相同的操作权限，此函数仅影响回复语气，不影响功能执行
     */
    private fun generateReluctantResponse(butlerId: String, action: String): String? {
        // 所有人格都有相同权限，这里只返回null，不限制任何操作
        // 所有记账、查询、管理操作都能正常执行
        return null
    }

    /**
     * 生成账户管理动作
     */
    private suspend fun generateAccountManagementActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis
    ): List<AIAction> {
        // 暂时返回查询账户信息
        return listOf(AIAction.QueryInformation(
            queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
        ))
    }

    /**
     * 生成分类管理动作 - 支持创建顶级分类和子分类
     */
    private suspend fun generateCategoryManagementActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis
    ): List<AIAction> {
        val message = context.userMessage

        // 解析子分类语义：在X下创建Y / 给X添加子分类Y / X下新建Y
        val subCategoryInfo = parseSubCategoryIntent(message)

        if (subCategoryInfo != null) {
            val (parentName, childName) = subCategoryInfo
            // 查找父分类
            val allCategories = categoryRepository.getAllCategoriesList()
            val parentCategory = allCategories.firstOrNull {
                it.name.contains(parentName) || parentName.contains(it.name)
            }

            if (parentCategory != null) {
                val op = AIOperation.AddCategory(
                    name = childName,
                    type = parentCategory.type,
                    parentId = parentCategory.id
                )
                val result = aiOperationExecutor.executeOperation(op)
                val responseMsg = when (result) {
                    is AIOperationExecutor.AIOperationResult.Success -> result.message
                    is AIOperationExecutor.AIOperationResult.Error -> "创建子分类失败: ${result.error}"
                }
                return listOf(AIAction.GenerateResponse(responseMsg))
            } else {
                return listOf(AIAction.GenerateResponse("未找到名为「$parentName」的分类，无法创建子分类。请先确认父分类名称。"))
            }
        }

        // 解析普通分类创建：创建XX分类
        val categoryName = parseCategoryName(message)
        if (categoryName != null && (message.contains("创建") || message.contains("添加") || message.contains("新建"))) {
            val type = if (message.contains("收入")) TransactionType.INCOME else TransactionType.EXPENSE
            val op = AIOperation.AddCategory(name = categoryName, type = type)
            val result = aiOperationExecutor.executeOperation(op)
            val responseMsg = when (result) {
                is AIOperationExecutor.AIOperationResult.Success -> result.message
                is AIOperationExecutor.AIOperationResult.Error -> "创建分类失败: ${result.error}"
            }
            return listOf(AIAction.GenerateResponse(responseMsg))
        }

        // 默认返回分类信息查询
        return listOf(AIAction.QueryInformation(
            queryType = AIInformationSystem.QueryType.CATEGORY_INFO
        ))
    }

    /**
     * 解析子分类创建意图
     * 支持语义：在餐饮下创建火锅 / 给交通添加子分类打车 / 餐饮下新建奶茶
     * 返回 Pair(父分类名, 子分类名) 或 null
     */
    private fun parseSubCategoryIntent(message: String): Pair<String, String>? {
        // 模式1: 在X下创建/添加/新建Y
        val pattern1 = Regex("在[「「]?(.+?)[」」]?下[面]?(?:创建|添加|新建)[「「]?(.+?)[」」]?(?:分类|子分类)?$")
        pattern1.find(message)?.let {
            return it.groupValues[1].trim() to it.groupValues[2].trim()
        }

        // 模式2: 给X添加子分类Y
        val pattern2 = Regex("给[「「]?(.+?)[」」]?(?:添加|创建|新建)子分类[「「]?(.+?)[」」]?$")
        pattern2.find(message)?.let {
            return it.groupValues[1].trim() to it.groupValues[2].trim()
        }

        // 模式3: X下新建/创建/添加Y
        val pattern3 = Regex("[「「]?(.+?)[」」]?下(?:创建|添加|新建)[「「]?(.+?)[」」]?(?:分类|子分类)?$")
        pattern3.find(message)?.let {
            return it.groupValues[1].trim() to it.groupValues[2].trim()
        }

        // 模式4: 创建/添加子分类Y到X / 把Y归到X下
        val pattern4 = Regex("把[「「]?(.+?)[」」]?归[到入][「「]?(.+?)[」」]?下")
        pattern4.find(message)?.let {
            return it.groupValues[2].trim() to it.groupValues[1].trim()
        }

        return null
    }

    /**
     * 从消息中提取分类名称
     */
    private fun parseCategoryName(message: String): String? {
        val patterns = listOf(
            Regex("(?:创建|添加|新建)[「「]?(.+?)[」」]?分类"),
            Regex("分类[「「]?(.+?)[」」]?$"),
            Regex("(?:创建|添加|新建)(?:一个)?[「「]?(.+?)[」」]?(?:的)?(?:支出|收入)?分类")
        )
        for (pattern in patterns) {
            pattern.find(message)?.let {
                val name = it.groupValues[1].trim()
                if (name.isNotEmpty() && name.length <= 10) return name
            }
        }
        return null
    }

    /**
     * 生成对话动作
     */
    private suspend fun generateConversationActions(
        context: ReasoningContext,
        intentAnalysis: IntentAnalysis
    ): List<AIAction> {
        val message = context.userMessage.lowercase()
        
        val response = when {
            message.contains("你好") || message.contains("您好") || message.contains("hi") || message.contains("hello") -> {
                "您好！我是您的AI记账助手。我可以帮您：\n" +
                "• 记账：直接说花了50元买咖啡\n" +
                "• 查询：查看账户余额、交易记录\n" +
                "• 分析：分析支出结构、收支趋势\n" +
                "有什么可以帮您的吗？"
            }
            message.contains("谢谢") || message.contains("感谢") -> {
                "不客气！随时为您服务。"
            }
            message.contains("再见") || message.contains("拜拜") -> {
                "再见！记得坚持记账哦！"
            }
            else -> {
                "您好！我可以帮您记账、查询财务信息或分析数据。请告诉我您需要什么帮助？"
            }
        }
        
        return listOf(AIAction.GenerateResponse(response))
    }

    /**
     * 执行动作序列
     */
    suspend fun executeActions(actions: List<AIAction>): String {
        val results = mutableListOf<String>()
        
        for (action in actions) {
            val result = when (action) {
                is AIAction.RecordTransaction -> executeRecordTransaction(action)
                is AIAction.QueryInformation -> executeQueryInformation(action)
                is AIAction.AnalyzeData -> executeAnalyzeData(action)
                is AIAction.GenerateResponse -> action.responseContent
                is AIAction.RequestClarification -> action.question
            }
            results.add(result)
        }
        
        return results.joinToString("\n\n")
    }

    /**
     * 执行记账动作
     * 
     * 真正调用AIOperationExecutor执行数据库写入
     */
    private suspend fun executeRecordTransaction(action: AIAction.RecordTransaction): String {
        // 1. 查找或创建账户
        var accounts = accountRepository.getAllAccountsList()
        var account = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        
        // 如果没有账户，创建默认账户
        if (account == null) {
            val createAccountOp = AIOperation.AddAccount(
                name = "默认账户",
                type = com.example.aiaccounting.data.local.entity.AccountType.CASH,
                balance = 0.0
            )
            when (val result = aiOperationExecutor.executeOperation(createAccountOp)) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    accounts = accountRepository.getAllAccountsList()
                    account = accounts.firstOrNull()
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    return "记账失败：创建账户失败 - ${result.error ?: "未知错误"}"
                }
            }
        }
        
        if (account == null) {
            return "记账失败：无法创建账户"
        }
        
        // 2. 查找或创建分类
        var categories = categoryRepository.getAllCategoriesList()
        var category = categories.firstOrNull { it.type == action.type }
        
        // 如果没有分类，创建默认分类
        if (category == null) {
            val categoryName = when (action.type) {
                TransactionType.INCOME -> "其他收入"
                TransactionType.EXPENSE -> "其他支出"
                TransactionType.TRANSFER -> "转账"
            }
            val createCategoryOp = AIOperation.AddCategory(
                name = categoryName,
                type = action.type
            )
            when (val result = aiOperationExecutor.executeOperation(createCategoryOp)) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    categories = categoryRepository.getAllCategoriesList()
                    category = categories.firstOrNull { it.type == action.type }
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    // 尝试使用任何可用分类
                    category = categories.firstOrNull()
                }
            }
        }
        
        if (category == null) {
            return "记账失败：无法创建分类"
        }
        
        // 3. 执行记账操作
        val operation = AIOperation.AddTransaction(
            amount = action.amount,
            type = action.type,
            accountId = account.id,
            categoryId = category.id,
            note = action.note,
            date = action.date
        )
        
        return when (val result = aiOperationExecutor.executeOperation(operation)) {
            is AIOperationExecutor.AIOperationResult.Success -> {
                val typeStr = when (action.type) {
                    TransactionType.INCOME -> "收入"
                    TransactionType.EXPENSE -> "支出"
                    TransactionType.TRANSFER -> "转账"
                }
                "✅ 已记录${typeStr}：¥${String.format("%.2f", action.amount)} - ${action.note}"
            }
            is AIOperationExecutor.AIOperationResult.Error -> {
                "❌ 记账失败：${result.error ?: "未知错误"}"
            }
        }
    }

    /**
     * 执行信息查询
     */
    private suspend fun executeQueryInformation(action: AIAction.QueryInformation): String {
        val request = AIInformationSystem.QueryRequest(
            queryType = action.queryType,
            startDate = action.startDate,
            endDate = action.endDate,
            customParams = action.parameters
        )
        
        val result = aiInformationSystem.executeQuery(request)
        return if (result.success) result.details else "查询失败：${result.errorMessage}"
    }

    /**
     * 执行数据分析
     */
    private suspend fun executeAnalyzeData(action: AIAction.AnalyzeData): String {
        // 根据分析类型调用相应的查询
        val queryType = when (action.analysisType) {
            AnalysisType.EXPENSE_STRUCTURE -> AIInformationSystem.QueryType.EXPENSE_ANALYSIS
            AnalysisType.INCOME_STRUCTURE -> AIInformationSystem.QueryType.INCOME_ANALYSIS
            AnalysisType.TREND_ANALYSIS -> AIInformationSystem.QueryType.TREND_ANALYSIS
            AnalysisType.COMPARISON_ANALYSIS -> AIInformationSystem.QueryType.COMPARISON_ANALYSIS
            else -> AIInformationSystem.QueryType.TRANSACTION_SUMMARY
        }
        
        val (startDate, endDate) = getScopeDateRange(action.scope)
        
        val request = AIInformationSystem.QueryRequest(
            queryType = queryType,
            startDate = startDate,
            endDate = endDate
        )
        
        val result = aiInformationSystem.executeQuery(request)
        return if (result.success) result.details else "分析失败：${result.errorMessage}"
    }

    // ============ 辅助方法 ============

    private data class IntentAnalysis(val intent: UserIntent, val confidence: Float)

    private fun containsAmount(message: String): Boolean {
        return Regex("""\d+\.?\d*\s*[元块]?""").containsMatchIn(message)
    }

    private fun containsActionKeywords(message: String): Boolean {
        val actionKeywords = listOf("记", "添加", "创建", "新建", "删除", "修改", "更新")
        return actionKeywords.any { message.contains(it) }
    }

    private fun containsAnalysisKeywords(message: String): Boolean {
        val analysisKeywords = listOf("分析", "统计", "趋势", "对比", "比较", "结构", "报告")
        return analysisKeywords.any { message.contains(it) }
    }

    private fun containsAccountManagementKeywords(message: String): Boolean {
        val keywords = listOf("添加账户", "新建账户", "创建账户", "删除账户")
        return keywords.any { message.contains(it) }
    }

    private fun containsCategoryManagementKeywords(message: String): Boolean {
        val keywords = listOf(
            "添加分类", "新建分类", "创建分类", "删除分类",
            "添加子分类", "新建子分类", "创建子分类",
            "子分类", "下面添加", "下面创建", "下新建",
            "下添加", "下创建", "归到", "归入"
        )
        return keywords.any { message.contains(it) }
    }

    private fun isGreetingOrSimpleConversation(message: String): Boolean {
        val greetings = listOf("你好", "您好", "hi", "hello", "在吗", "在不在", "有人吗")
        return greetings.any { message.contains(it) }
    }

    private fun inferQueryType(message: String): AIInformationSystem.QueryType {
        return when {
            message.contains("账户") || message.contains("余额") || message.contains("资产") -> 
                AIInformationSystem.QueryType.ACCOUNT_INFO
            message.contains("分类") || message.contains("类别") -> 
                AIInformationSystem.QueryType.CATEGORY_INFO
            message.contains("明细") || message.contains("记录") || message.contains("账单") -> 
                AIInformationSystem.QueryType.TRANSACTION_LIST
            message.contains("支出分析") || message.contains("消费分析") || message.contains("钱花") -> 
                AIInformationSystem.QueryType.EXPENSE_ANALYSIS
            message.contains("收入分析") -> 
                AIInformationSystem.QueryType.INCOME_ANALYSIS
            message.contains("趋势") || message.contains("走势") -> 
                AIInformationSystem.QueryType.TREND_ANALYSIS
            message.contains("对比") || message.contains("比较") || message.contains("环比") -> 
                AIInformationSystem.QueryType.COMPARISON_ANALYSIS
            message.contains("预算") -> 
                AIInformationSystem.QueryType.BUDGET_STATUS
            message.contains("收支") || message.contains("汇总") || message.contains("总结") -> 
                AIInformationSystem.QueryType.TRANSACTION_SUMMARY
            else -> AIInformationSystem.QueryType.ACCOUNT_INFO
        }
    }

    private fun extractDateRange(message: String): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        return when {
            message.contains("今天") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("昨天") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            message.contains("本周") || message.contains("这周") -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("本月") || message.contains("这个月") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("上月") || message.contains("上个月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            message.contains("最近") || message.contains("近") -> {
                val days = Regex("""(\d+)""").find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 7
                calendar.add(Calendar.DAY_OF_MONTH, -days)
                Pair(calendar.timeInMillis, now)
            }
            else -> Pair(null, null)
        }
    }

    private fun extractLimit(message: String): Int? {
        return Regex("""(\d+)""").find(message)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractAmount(message: String): Double? {
        val match = Regex("""(\d+\.?\d*)\s*[元块]?""").find(message)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun inferCategory(message: String, type: TransactionType): String? {
        val categoryKeywords = mapOf(
            "餐饮" to listOf("吃", "饭", "餐厅", "外卖", "奶茶", "咖啡", "菜", "肉"),
            "交通" to listOf("车", "地铁", "公交", "打车", "滴滴", "油费", "交通"),
            "购物" to listOf("买", "淘宝", "京东", "衣服", "化妆品", "购物"),
            "娱乐" to listOf("电影", "游戏", "旅游", "KTV", "玩"),
            "居住" to listOf("房租", "水电", "物业", "房租"),
            "医疗" to listOf("药", "医院", "看病", "体检"),
            "工资" to listOf("工资", "薪水", "薪资"),
            "奖金" to listOf("奖金", "奖励", "红包")
        )
        
        val lowerMessage = message.lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerMessage.contains(it) }) {
                return category
            }
        }
        return null
    }

    private fun inferAccount(message: String): String? {
        return when {
            message.contains("微信") || message.contains("WeChat") -> "微信"
            message.contains("支付宝") || message.contains("Alipay") -> "支付宝"
            message.contains("现金") -> "现金"
            message.contains("银行卡") || message.contains("银行") -> "银行卡"
            message.contains("信用卡") -> "信用卡"
            else -> null
        }
    }

    private fun extractNote(message: String): String? {
        // 尝试提取有意义的描述
        val patterns = listOf(
            Regex("""买了(.+?)[，。]?"""),
            Regex("""花了.+?买(.+?)[，。]?"""),
            Regex("""消费(.+?)[，。]?"""),
            Regex("""用于(.+?)[，。]?""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }

    /**
     * 提取交易日期
     * 支持：今天、昨天、前天、具体日期（3月7日、3.7、3-7）
     */
    private fun extractTransactionDate(message: String): Long {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        // 先尝试提取具体日期（如：3月7日、3.7、3-7）
        val specificDate = extractSpecificDateFromMessage(message, currentYear)
        if (specificDate != null) {
            return specificDate
        }
        
        // 处理相对日期
        when {
            message.contains("昨天") -> calendar.add(Calendar.DAY_OF_MONTH, -1)
            message.contains("前天") -> calendar.add(Calendar.DAY_OF_MONTH, -2)
            message.contains("大前天") -> calendar.add(Calendar.DAY_OF_MONTH, -3)
            message.contains("上周") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            message.contains("上月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        // 设置为当天的开始时间（00:00:00）
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }
    
    /**
     * 从消息中提取具体日期
     * 支持格式：3月7日、3.7、3-7、3/7、2026年3月7日
     */
    private fun extractSpecificDateFromMessage(message: String, defaultYear: Int): Long? {
        val patterns = listOf(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号]?" to listOf(1, 2, 3),  // 2026年3月7日
            "(\\d{1,2})月(\\d{1,2})[日号]?" to listOf(-1, 1, 2),           // 3月7日（使用默认年）
            "(\\d{1,2})\\.(\\d{1,2})" to listOf(-1, 1, 2),                  // 3.7
            "(\\d{1,2})-(\\d{1,2})" to listOf(-1, 1, 2),                    // 3-7
            "(\\d{1,2})/(\\d{1,2})" to listOf(-1, 1, 2)                     // 3/7
        )
        
        for ((pattern, groups) in patterns) {
            val matcher = java.util.regex.Pattern.compile(pattern).matcher(message)
            if (matcher.find()) {
                val year = if (groups[0] == -1) defaultYear else matcher.group(groups[0])?.toIntOrNull() ?: defaultYear
                val month = matcher.group(groups[1])?.toIntOrNull()
                val day = matcher.group(groups[2])?.toIntOrNull()
                
                if (month != null && day != null && month in 1..12 && day in 1..31) {
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month - 1, day, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis
                }
            }
        }
        
        return null
    }

    private fun extractAnalysisScope(message: String): AnalysisScope {
        return when {
            message.contains("今天") -> AnalysisScope.TODAY
            message.contains("昨天") -> AnalysisScope.YESTERDAY
            message.contains("本周") || message.contains("这周") -> AnalysisScope.THIS_WEEK
            message.contains("本月") || message.contains("这个月") -> AnalysisScope.THIS_MONTH
            message.contains("上月") || message.contains("上个月") -> AnalysisScope.LAST_MONTH
            message.contains("今年") || message.contains("本年") -> AnalysisScope.THIS_YEAR
            message.contains("最近7天") || message.contains("近7天") -> AnalysisScope.LAST_7_DAYS
            message.contains("最近30天") || message.contains("近30天") -> AnalysisScope.LAST_30_DAYS
            else -> AnalysisScope.THIS_MONTH
        }
    }

    private fun getScopeDateRange(scope: AnalysisScope): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        val startDate = when (scope) {
            AnalysisScope.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            AnalysisScope.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_MONTH, -7)
                calendar.timeInMillis
            }
            AnalysisScope.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                calendar.timeInMillis
            }
            AnalysisScope.CUSTOM -> calendar.timeInMillis
        }
        
        return Pair(startDate, endDate)
    }

    private fun generateExplanation(intentAnalysis: IntentAnalysis, actions: List<AIAction>): String {
        return "识别到意图：${intentAnalysis.intent}（置信度：${String.format("%.0f", intentAnalysis.confidence * 100)}%），" +
               "计划执行 ${actions.size} 个动作"
    }

    private fun shouldRequireConfirmation(actions: List<AIAction>): Boolean {
        // 对于记账操作，可能需要用户确认
        return actions.any { it is AIAction.RecordTransaction && it.amount > 1000 }
    }
}
