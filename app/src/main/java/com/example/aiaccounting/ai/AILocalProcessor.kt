package com.example.aiaccounting.ai

import android.util.Log
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AIOperationExecutor.AIOperationResult
import com.example.aiaccounting.ai.AITraceContext
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.ButlerPersonaRegistry
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 AI 处理器
 * 处理离线模式下的记账、查询、账户/分类管理等命令
 */
@Singleton
class AILocalProcessor @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val aiOperationExecutor: AIOperationExecutor
) {

    suspend fun processMessage(
        message: String,
        isNetworkAvailable: Boolean,
        isAIConfigured: Boolean,
        currentButlerId: String = "xiaocainiang"
    ): String {
        val lowerMessage = message.lowercase()

        if (isReminderLikeConversation(lowerMessage)) {
            return handleGeneralConversation(message, isNetworkAvailable, isAIConfigured, currentButlerId)
        }

        if (isTransactionCommand(lowerMessage)) {
            return handleTransactionCommand(message, lowerMessage)
        }
        if (containsAny(lowerMessage, listOf("查", "看", "多少", "余额", "资产", "统计"))) {
            return handleLocalQueryCommand(lowerMessage)
        }
        if (containsAny(lowerMessage, listOf("账户", "银行卡", "现金", "支付宝", "微信"))) {
            return handleAccountCommand(message, lowerMessage)
        }
        if (containsAny(lowerMessage, listOf("分类", "类别", "类型"))) {
            return handleCategoryCommand(message, lowerMessage)
        }
        if (containsAny(lowerMessage, listOf("预算", "限额"))) {
            return handleBudgetCommand(lowerMessage)
        }
        if (containsAny(lowerMessage, listOf("导出", "备份", "下载"))) {
            return handleExportCommand()
        }
        return handleGeneralConversation(message, isNetworkAvailable, isAIConfigured, currentButlerId)
    }

    suspend fun ensureBasicCategoriesExist() {
        val categories = categoryRepository.getAllCategoriesList()
        if (categories.isEmpty()) {
            val expenseCategories = listOf("餐饮", "交通", "购物", "娱乐", "居住", "医疗", "其他支出")
            val incomeCategories = listOf("工资", "奖金", "投资", "兼职", "其他收入")
            expenseCategories.forEach { name ->
                aiOperationExecutor.executeOperation(AIOperation.AddCategory(name = name, type = TransactionType.EXPENSE))
            }
            incomeCategories.forEach { name ->
                aiOperationExecutor.executeOperation(AIOperation.AddCategory(name = name, type = TransactionType.INCOME))
            }
        }
    }

    suspend fun handleQueryCommand(target: String): String {
        return when (target) {
            "accounts", "balance" -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) "暂无账户信息"
                else {
                    val total = accounts.sumOf { it.balance }
                    "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" } +
                    "\n\n💰 总资产: ¥${String.format("%.2f", total)}"
                }
            }
            "categories" -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) "暂无分类信息"
                else "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
            }
            else -> "查询完成"
        }
    }

    private fun isTransactionCommand(lowerMessage: String): Boolean {
        val explicitTransactionPhrases = listOf(
            "记账", "记个账", "记一笔", "记一下", "记下", "记录一笔",
            "花了", "收入", "支出", "消费", "买了", "买菜", "买饭", "转账", "工资", "奖金", "报销"
        )
        if (explicitTransactionPhrases.any { lowerMessage.contains(it) }) {
            return true
        }

        val hasTransactionVerb = listOf(
            "花", "花了", "买", "卖", "付", "收", "收到", "赚", "报销", "转账", "消费"
        ).any { lowerMessage.contains(it) }
        val hasAmount = extractAmount(lowerMessage) != null
        val looksLikeReminderOnly = isReminderLikeConversation(lowerMessage) && !hasAmount
        val hasConciseJiAmountPattern = Regex("^(?:\\s*(?:帮我|请|麻烦|给我|帮忙)?\\s*)记(?!得)\\s*(?:¥|￥)?\\s*\\d").containsMatchIn(lowerMessage)

        return (hasTransactionVerb && hasAmount && !looksLikeReminderOnly) ||
            (hasConciseJiAmountPattern && hasAmount && !isReminderLikeConversation(lowerMessage))
    }

    private fun isReminderLikeConversation(lowerMessage: String): Boolean {
        return listOf(
            "记得", "提醒我", "提醒一下", "会议", "会议要点", "备忘", "笔记", "日记"
        ).any { lowerMessage.contains(it) }
    }

    private suspend fun handleTransactionCommand(message: String, lowerMessage: String): String {
        val amount = extractAmount(lowerMessage)
        val type = if (containsAny(lowerMessage, listOf("收入", "赚", "收到", "工资", "奖金")))
            TransactionType.INCOME else TransactionType.EXPENSE

        if (amount != null) {
            ensureBasicCategoriesExist()

            val accountResolution = AITransactionEntityResolver.resolveAccount(
                accountRepository = accountRepository,
                aiOperationExecutor = aiOperationExecutor,
                fallbackAccountName = "默认账户",
                fallbackAccountType = AccountType.CASH,
                allowExplicitFallbackCreation = true,
                traceContext = AITraceContext(sourceType = "AI_LOCAL")
            )
            val defaultAccount = accountResolution.account
                ?: accountResolution.creationError?.let { return "创建默认账户失败: $it" }
                ?: return "无法创建账户，请手动创建账户后再试。"

            val categoryResolution = AITransactionEntityResolver.resolveCategory(
                categoryRepository = categoryRepository,
                aiOperationExecutor = aiOperationExecutor,
                transactionType = type,
                fallbackCategoryName = if (type == TransactionType.INCOME) "其他收入" else "其他支出",
                emergencyCategoryName = if (type == TransactionType.INCOME) "收入" else "支出",
                traceContext = AITraceContext(sourceType = "AI_LOCAL")
            )
            val defaultCategory = categoryResolution.category
                ?: categoryResolution.terminalCreationError?.let {
                    return "❌ 记账失败：无法创建分类，请检查数据库权限"
                }
                ?: return "❌ 记账失败：系统无法创建分类，请手动创建分类后再试"

            val operation = AIOperation.AddTransaction(
                amount = amount, type = type,
                accountId = defaultAccount.id, categoryId = defaultCategory.id, note = message,
                traceContext = AITraceContext(sourceType = "AI_LOCAL")
            )
            return when (val result = aiOperationExecutor.executeOperation(operation)) {
                is AIOperationResult.Success ->
                    "✅ ${result.message}\n账户: ${defaultAccount.name}\n分类: ${defaultCategory.name}\n您可以说「查看最近交易」来确认记录。"
                is AIOperationResult.Error ->
                    "❌ 记账失败: ${result.error}"
            }
        }
        return "我没有识别到金额。请告诉我具体的金额，比如：\n- 花了50元买咖啡\n- 今天收入5000元工资\n- 支出200元超市购物"
    }


    private suspend fun handleLocalQueryCommand(lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("余额", "资产", "总共", "多少")) -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) "暂无账户信息"
                else {
                    val total = accounts.sumOf { it.balance }
                    "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" } +
                    "\n\n💰 总资产: ¥${String.format("%.2f", total)}"
                }
            }
            containsAny(lowerMessage, listOf("交易", "记录", "明细", "最近")) -> {
                val operation = AIOperation.QueryData("transactions", extractNumber(lowerMessage) ?: 10)
                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationResult.Success -> result.message
                    is AIOperationResult.Error -> "错误: ${result.error}"
                }
            }
            containsAny(lowerMessage, listOf("账户", "银行卡")) -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) "暂无账户"
                else "📊 账户列表：\n" +
                    accounts.joinToString("\n") { "• ${it.name}: ¥${String.format("%.2f", it.balance)}" }
            }
            containsAny(lowerMessage, listOf("分类", "类别")) -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) "暂无分类"
                else "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
            }
            else -> "您可以这样查询：\n- 查看总资产\n- 最近10笔交易\n- 账户列表\n- 分类统计"
        }
    }

    private suspend fun handleAccountCommand(message: String, lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("添加", "新建", "创建")) -> {
                val name = extractAccountName(message)
                if (name != null) {
                    val type = extractAccountType(lowerMessage)
                    val operation = AIOperation.AddAccount(name = name, type = type, balance = extractAmount(lowerMessage) ?: 0.0, traceContext = AITraceContext(sourceType = "AI_LOCAL"))
                    when (val result = aiOperationExecutor.executeOperation(operation)) {
                        is AIOperationResult.Success -> result.message
                        is AIOperationResult.Error -> "错误: ${result.error}"
                    }
                } else "请告诉我账户名称，比如：\n- 添加一个现金账户\n- 新建银行卡账户余额10000元"
            }
            containsAny(lowerMessage, listOf("删除", "移除")) ->
                "删除账户功能需要在账户管理界面操作，以确保数据安全。"
            else -> {
                val accounts = accountRepository.getAllAccountsList()
                if (accounts.isEmpty()) "暂无账户"
                else {
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
                val type = if (containsAny(lowerMessage, listOf("收入"))) TransactionType.INCOME else TransactionType.EXPENSE
                if (name != null) {
                    val operation = AIOperation.AddCategory(name = name, type = type, traceContext = AITraceContext(sourceType = "AI_LOCAL"))
                    when (val result = aiOperationExecutor.executeOperation(operation)) {
                        is AIOperationResult.Success -> result.message
                        is AIOperationResult.Error -> "错误: ${result.error}"
                    }
                } else "请告诉我分类名称，比如：\n- 添加餐饮分类\n- 新建交通支出分类"
            }
            else -> {
                val categories = categoryRepository.getAllCategoriesList()
                if (categories.isEmpty()) "暂无分类"
                else "📁 分类列表：\n" +
                    categories.joinToString("\n") { "• ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})" }
            }
        }
    }

    private fun handleBudgetCommand(lowerMessage: String): String {
        return when {
            containsAny(lowerMessage, listOf("设置", "添加", "创建")) -> {
                val amount = extractAmount(lowerMessage)
                if (amount != null) "预算设置功能已记录。您可以在预算管理界面查看和修改。"
                else "请告诉我预算金额，比如：\n- 设置餐饮预算2000元\n- 每月交通预算500元"
            }
            else -> "预算管理功能：\n- 设置XX分类预算XXX元\n- 查看预算执行情况请前往预算管理界面"
        }
    }

    private suspend fun handleExportCommand(): String {
        val operation = AIOperation.ExportData(format = "excel")
        return when (val result = aiOperationExecutor.executeOperation(operation)) {
            is AIOperationResult.Success -> result.message + "\n请前往设置 > 数据备份页面完成导出操作。"
            is AIOperationResult.Error -> "错误: ${result.error}"
        }
    }

    private fun handleGeneralConversation(
        message: String,
        isNetworkAvailable: Boolean,
        isAIConfigured: Boolean,
        currentButlerId: String
    ): String {
        val lowerMessage = message.lowercase()
        return ButlerPersonaRegistry.buildGeneralConversationReply(
            butlerId = currentButlerId,
            lowerMessage = lowerMessage,
            isNetworkAvailable = isNetworkAvailable,
            isAIConfigured = isAIConfigured,
            containsAny = ::containsAny
        )
    }

    // ============ Helper Functions ============

    fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun extractAmount(text: String): Double? {
        // Prefer explicit currency markers to avoid capturing dates like "3月15日花了50元".
        val normalized = text.replace(",", "")

        // 1) explicit currency symbol / unit (prefer the last match)
        val explicitMatches = Regex("""(\d+(?:\.\d+)?)\s*(?:元|块|rmb|cny)""")
            .findAll(normalized)
            .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .toList()
        val yuanMatches = Regex("""(?:¥|￥)\s*(\d+(?:\.\d+)?)""")
            .findAll(normalized)
            .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .toList()

        val currencyMatches = (yuanMatches + explicitMatches)
        if (currencyMatches.isNotEmpty()) return currencyMatches.last()

        // 2) fallback: if message contains common transaction verbs, prefer the first number (often the amount)
        val lower = normalized.lowercase()
        val numbers = Regex("""\d+(?:\.\d+)?""")
            .findAll(normalized)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

        if (numbers.isEmpty()) return null

        val hasTxnVerb = listOf("花", "花了", "支出", "收入", "买", "付", "转账", "充值", "收", "收到").any { lower.contains(it) }
        return if (hasTxnVerb) numbers.first() else numbers.last()
    }

    fun extractNumber(text: String): Int? {
        return Regex("""(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractAccountName(text: String): String? {
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

    fun extractAccountType(text: String): AccountType {
        return when {
            text.contains("现金") -> AccountType.CASH
            text.contains("支付宝") -> AccountType.ALIPAY
            text.contains("微信") -> AccountType.WECHAT
            text.contains("信用卡") -> AccountType.CREDIT_CARD
            text.contains("借记卡") -> AccountType.DEBIT_CARD
            text.contains("银行") -> AccountType.BANK
            else -> AccountType.OTHER
        }
    }

    fun extractCategoryName(text: String): String? {
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
}
