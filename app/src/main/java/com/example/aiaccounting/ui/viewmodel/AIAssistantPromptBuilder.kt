package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI Assistant 提示词构建器
 *
 * 负责构建远程 AI 服务的系统提示词和消息列表
 */
internal class AIAssistantPromptBuilder {

    /**
     * 构建聊天消息列表
     *
     * @param userMessage 用户消息
     * @param currentButler 当前管家
     * @param accounts 账户列表
     * @param categories 分类列表
     * @return 聊天消息列表
     */
    fun buildMessages(
        userMessage: String,
        currentButler: Butler,
        accounts: List<Account>,
        categories: List<Category>,
        scenario: AIAssistantRemotePromptScenario
    ): List<ChatMessage> {
        val systemPrompt = when (scenario) {
            AIAssistantRemotePromptScenario.Chat -> buildChatSystemPrompt(currentButler)
            AIAssistantRemotePromptScenario.Bookkeeping -> buildBookkeepingSystemPrompt(
                currentButler = currentButler,
                accounts = accounts,
                categories = categories,
                userMessage = userMessage
            )
        }

        return listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, userMessage)
        )
    }

    private fun buildChatSystemPrompt(currentButler: Butler): String {
        return """
${currentButler.systemPrompt}

【当前任务】
- 这是普通聊天场景。
- 直接用你当前角色的语气自然回复。
- 不要主动输出 JSON、代码块、actions 或记账工具格式。
- 只有当用户明确提出记账、查账、创建账户/分类等账务需求时，才在后续轮次切到对应账务链路。
        """.trimIndent()
    }

    /**
     * 构建记账系统提示词
     */
    private fun buildBookkeepingSystemPrompt(
        currentButler: Butler,
        accounts: List<Account>,
        categories: List<Category>,
        userMessage: String
    ): String {
        val baseSystemPrompt = currentButler.systemPrompt
        val accountsInfo = buildAccountsContext(accounts, userMessage)
        val categoriesInfo = buildCategoriesContext(categories, userMessage)
        val shouldUseCompactVariant = userMessage.length > 4000

        return if (shouldUseCompactVariant) {
            buildCompactBookkeepingPrompt(baseSystemPrompt, accountsInfo, categoriesInfo)
        } else {
            buildFullBookkeepingPrompt(baseSystemPrompt, accountsInfo, categoriesInfo)
        }
    }

    private fun buildFullBookkeepingPrompt(
        baseSystemPrompt: String,
        accountsInfo: String,
        categoriesInfo: String
    ): String {
        return """
$baseSystemPrompt

【当前账本状况】
🏦 已有账户：
$accountsInfo

📁 已有分类：
$categoriesInfo

【记账规则 - 重要！】
1. 🔍 多笔消费要全部识别并分别输出。
2. 🎯 根据消费内容智能匹配最合适分类。
3. 💳 根据用户描述识别账户；未指定时优先默认账户。
4. 📅 日期必须结合当前时间 ${getCurrentDateTime()} 解析，并在 JSON 中输出毫秒时间戳 date。
5. ⚡ 识别到明确账务需求后直接执行，不要询问确认。
6. 🏷️ note 要具体，不要只写泛化短词。

【工具语义合同】
你只能使用以下动作名：
- query_accounts
- query_categories
- query_transactions
- create_account
- create_category
- add_transaction

【输出要求】
- 只在账务/查询/账户分类管理场景输出 JSON。
- 不要输出 markdown，不要代码块。
- 返回对象格式：{"actions":[...],"reply":"..."}
- 查询动作使用 action + target。
- 交易动作统一使用 add_transaction，并带 transactionType/accountRef/categoryRef/date。
- reply 必须使用当前角色语气，总结执行结果。
- 分类和账户不存在时可按语义创建；不要因为缺失实体放弃返回动作。
        """.trimIndent()
    }

    private fun buildCompactBookkeepingPrompt(
        baseSystemPrompt: String,
        accountsInfo: String,
        categoriesInfo: String
    ): String {
        return """
$baseSystemPrompt

【当前任务】
- 这是超长账务文本场景，请优先保证动作提取稳定，不要闲聊铺陈。
- 只返回 JSON 对象：{"actions":[...],"reply":"..."}
- 不要 markdown，不要代码块，不要额外解释。

【账务合同】
- 支持动作：query_accounts, query_categories, query_transactions, create_account, create_category, add_transaction
- 明确记账时必须输出 actions。
- 多笔记录要拆成多条 actions。
- date 必须输出毫秒时间戳。
- transactionType 只能是 income / expense / transfer。
- reply 保留当前角色语气，但要简洁。

【最小账本上下文】
账户：
$accountsInfo
分类：
$categoriesInfo
        """.trimIndent()
    }

    private fun buildAccountsContext(accounts: List<Account>, userMessage: String): String {
        val prioritizedAccounts = prioritizeAccounts(accounts.map {
            "- ${it.name}: ¥${it.balance} (${it.type})"
        }, userMessage)
        return prioritizedAccounts.ifEmpty { listOf("暂无账户") }.joinToString("\n")
    }

    private fun buildCategoriesContext(categories: List<Category>, userMessage: String): String {
        val prioritizedCategories = prioritizeAccounts(categories.map {
            "- ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else if (it.type == TransactionType.EXPENSE) "支出" else "转账"})"
        }, userMessage)
        return prioritizedCategories.ifEmpty { listOf("暂无分类") }.joinToString("\n")
    }

    private fun prioritizeAccounts(items: List<String>, userMessage: String): List<String> {
        if (items.size <= 12) {
            return items
        }
        val normalizedUserMessage = userMessage.lowercase(Locale.getDefault())
        val matched = items.filter { item ->
            val normalized = item.lowercase(Locale.getDefault())
            normalizedUserMessage.contains(normalized.substringAfter("- ").substringBefore(":")) ||
                normalizedUserMessage.contains(normalized.substringAfter("- ").substringBefore(" ("))
        }
        val remaining = items.filterNot { it in matched }
        val remainingSlots = (12 - matched.size).coerceAtLeast(0)
        return (matched + remaining.take(remainingSlots)).take(12)
    }

    /**
     * 获取当前日期时间字符串
     */
    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
    }
}
