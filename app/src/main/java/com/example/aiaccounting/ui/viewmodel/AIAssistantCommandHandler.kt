package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository

/**
 * AI Assistant 命令处理器
 *
 * 负责处理各类用户命令：交易、查询、账户、分类、预算等
 */
internal class AIAssistantCommandHandler(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val aiOperationExecutor: AIOperationExecutor
) {

    /**
     * 处理交易命令
     */
    suspend fun handleTransactionCommand(
        message: String,
        lowerMessage: String,
        ensureBasicCategoriesExist: suspend () -> Unit
    ): String {
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
                    type = AccountType.CASH,
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

    /**
     * 处理本地查询命令
     */
    suspend fun handleLocalQueryCommand(lowerMessage: String): String {
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

    /**
     * 处理账户命令
     */
    suspend fun handleAccountCommand(message: String, lowerMessage: String): String {
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

    /**
     * 处理分类命令
     */
    suspend fun handleCategoryCommand(message: String, lowerMessage: String): String {
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

    /**
     * 处理预算命令
     */
    fun handleBudgetCommand(lowerMessage: String): String {
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

    /**
     * 处理导出命令
     */
    suspend fun handleExportCommand(): String {
        val operation = AIOperation.ExportData(format = "excel")
        return when (val result = aiOperationExecutor.executeOperation(operation)) {
            is AIOperationExecutor.AIOperationResult.Success -> {
                result.message + "\n请前往设置 > 数据备份页面完成导出操作。"
            }
            is AIOperationExecutor.AIOperationResult.Error -> "错误: ${result.error}"
        }
    }

    /**
     * 处理普通对话
     */
    fun handleGeneralConversation(message: String, aiConfig: AIConfig, isNetworkAvailable: Boolean): String {
        return when {
            containsAny(message.lowercase(), listOf("你好", "您好", "hi", "hello")) -> {
                val mode = if (aiConfig.isEnabled) {
                    if (isNetworkAvailable) "🤖 智能模式（联网）"
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

    // ========== 辅助方法 ==========

    fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d+\.?\d*)\s*[元块]?""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun extractNumber(text: String): Int? {
        val regex = Regex("""(\d+)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
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
