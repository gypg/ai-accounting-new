package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Calendar
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
        val hasExistingAccounts = accounts.isNotEmpty()
        val hasExistingCategories = categories.isNotEmpty()
        val shouldAllowSemanticEntityOutput = shouldAllowSemanticEntityOutput(
            userMessage = userMessage,
            accounts = accounts,
            categories = categories
        )

        return buildFullBookkeepingPrompt(
            baseSystemPrompt = baseSystemPrompt,
            accountsInfo = accountsInfo,
            categoriesInfo = categoriesInfo,
            hasExistingAccounts = hasExistingAccounts,
            hasExistingCategories = hasExistingCategories,
            shouldAllowSemanticEntityOutput = shouldAllowSemanticEntityOutput
        )
    }

    private fun buildFullBookkeepingPrompt(
        baseSystemPrompt: String,
        accountsInfo: String,
        categoriesInfo: String,
        hasExistingAccounts: Boolean,
        hasExistingCategories: Boolean,
        shouldAllowSemanticEntityOutput: Boolean
    ): String {
        val categoryRule = if (!shouldAllowSemanticEntityOutput && hasExistingCategories) {
            "2. 🎯 分类必须从上面已有的分类列表中选择最合适的一个，用 categoryRef.id 引用。"
        } else {
            "2. 🎯 若现有分类不足以覆盖用户输入，优先使用 categoryRef.name 按语义给出分类，执行阶段自动解析或补全。"
        }
        val accountRule = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts) {
            "3. 💳 账户必须从上面已有的账户列表中选择，用 accountRef.id 引用。"
        } else {
            "3. 💳 若现有账户不足以覆盖支付渠道，优先使用 accountRef.name 给出微信、支付宝、现金、银行卡、银行代发等语义账户名，执行阶段自动解析或补全。"
        }
        val referenceRule = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts && hasExistingCategories) {
            "7. ⚠️ 禁止凭空捏造账户名或分类名，必须使用上面提供的列表中的名称或 ID。"
        } else {
            "7. ⚠️ 禁止把商家名当账户名；支付渠道才是账户。若账本不完整，可输出语义账户名/分类名，由执行阶段自动解析或补全。"
        }
        val currentTimestamp = getCurrentTimestamp()
        val exampleEnvelope = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts && hasExistingCategories) {
            "{\"actions\":[{\"action\":\"add_transaction\",\"amount\":25.00,\"transactionType\":\"expense\",\"accountRef\":{\"id\":1,\"name\":\"微信\"},\"categoryRef\":{\"id\":10,\"name\":\"餐饮\"},\"note\":\"午餐\",\"date\":$currentTimestamp}],\"reply\":\"已记账\"}"
        } else {
            "{\"actions\":[{\"action\":\"add_transaction\",\"amount\":25.00,\"transactionType\":\"expense\",\"accountRef\":{\"name\":\"微信\",\"kind\":\"account\"},\"categoryRef\":{\"name\":\"餐饮\",\"kind\":\"category\"},\"note\":\"午餐\",\"date\":$currentTimestamp}],\"reply\":\"已记账\"}"
        }
        val transactionOutputRule = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts && hasExistingCategories) {
            "- 交易动作统一使用 add_transaction，accountRef 和 categoryRef 必须包含 id 字段并引用上面列表中的 ID。"
        } else {
            "- 交易动作统一使用 add_transaction；若现有账本覆盖不足，优先输出 accountRef.name / categoryRef.name，由执行阶段自动解析或补全。"
        }
        val uncertaintyRule = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts && hasExistingCategories) {
            "- 对不确定字段，优先使用 query_accounts / query_categories 获取上下文，不得臆造。"
        } else {
            "- 对不确定字段不要臆造；但不要因为账本不完整而只返回 query。优先输出可执行的 add_transaction 动作。"
        }
        val createAccountRule = if (!shouldAllowSemanticEntityOutput && hasExistingAccounts) {
            "- ⚠️ 严禁调用 create_account！现有账户足以覆盖所有支付场景，绝不要创建同名或近似名账户。"
        } else {
            "- ⚠️ 不要把瑞幸、美团、医院等商家名当成账户；优先输出微信、支付宝、现金、信用卡、银行卡、银行代发等支付渠道。"
        }

        return """
$baseSystemPrompt

【当前账本状况】
🏦 已有账户：
$accountsInfo

📁 已有分类：
$categoriesInfo

【记账规则 - 重要！】
1. 🔍 多笔消费要全部识别并分别输出，每笔都要有账户和分类。
$categoryRule
$accountRule
4. 📅 日期必须结合当前时间 ${getCurrentDateTime()} 解析，并在 JSON 中输出毫秒时间戳 date。
5. ⚡ 识别到明确账务需求后直接执行，不要询问确认。
6. 🏷️ note 要具体，不要只写泛化短词。
$referenceRule

【账户映射示例】（严格遵守！）
- "微信/微信支付/微信红包/瑞幸微信" → 找列表中 微信支付 类型账户
- "支付宝/美团/淘宝/医院/打车" → 找列表中 支付宝 类型账户
- "现金/人民币/纸币" → 找列表中 现金 类型账户
- "信用卡/信用/卡号" → 找列表中 信用卡 类型账户
- "银行卡/银行转账/工资卡" → 找列表中 银行账户 类型账户
- ⚠️ 商家名称（如"瑞幸"、"美团"、"医院"）不是账户名！支付渠道才是账户

【工具语义合同】
你只能使用以下动作名：
- query_accounts
- query_categories
- query_transactions
- add_transaction

【输出要求】
- 仅输出单个 JSON 对象：{"actions":[...],"reply":"..." }。
- 禁止输出 markdown、代码块、表格或任何解释性正文。
- 示例格式（严格遵守）：
$exampleEnvelope
- 查询动作使用 action + target。
$transactionOutputRule
- 必须覆盖每一笔候选交易，不得只输出汇总结论。
- reply 必须使用当前角色语气，总结执行结果。
$uncertaintyRule
$createAccountRule
        """.trimIndent()
    }

    private fun buildAccountsContext(accounts: List<Account>, userMessage: String): String {
        val prioritizedAccounts = prioritizeAccounts(accounts, userMessage)
        return prioritizedAccounts.ifEmpty { listOf("暂无账户") }.joinToString("\n")
    }

    private fun buildCategoriesContext(categories: List<Category>, userMessage: String): String {
        val prioritizedCategories = prioritizeCategories(categories, userMessage)
        return prioritizedCategories.ifEmpty { listOf("暂无分类") }.joinToString("\n")
    }

    private fun prioritizeAccounts(accounts: List<Account>, userMessage: String): List<String> {
        val lowerMsg = userMessage.lowercase()
        return accounts
            .sortedByDescending { account ->
                if (lowerMsg.isBlank()) {
                    0
                } else {
                    val accountName = account.name.lowercase()
                    if (accountName in lowerMsg || accountName.replace(" ", "") in lowerMsg) 1 else 0
                }
            }
            .map { acc ->
                val typeLabel = when (acc.type) {
                    AccountType.WECHAT -> "微信支付"
                    AccountType.ALIPAY -> "支付宝"
                    AccountType.CASH -> "现金"
                    AccountType.CREDIT_CARD -> "信用卡"
                    AccountType.DEBIT_CARD -> "借记卡"
                    AccountType.BANK -> "银行账户"
                    AccountType.OTHER -> "其他"
                }
                "- (ID=${acc.id}) ${acc.name} $typeLabel 余额¥${String.format(Locale.getDefault(), "%.2f", acc.balance)}${if (acc.isDefault) " [默认]" else ""}"
            }
    }

    private fun prioritizeCategories(categories: List<Category>, userMessage: String): List<String> {
        val lowerMsg = userMessage.lowercase()
        return categories
            .sortedByDescending { category ->
                if (lowerMsg.isBlank()) {
                    0
                } else {
                    val categoryName = category.name.lowercase()
                    if (categoryName in lowerMsg || categoryName.replace(" ", "") in lowerMsg) 1 else 0
                }
            }
            .map { cat ->
                val typeLabel = when (cat.type) {
                    TransactionType.INCOME -> "收入"
                    TransactionType.EXPENSE -> "支出"
                    TransactionType.TRANSFER -> "转账"
                }
                "- (ID=${cat.id}) ${cat.name} ($typeLabel)"
            }
    }

    /**
     * 获取当前日期时间字符串
     */
    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
    }

    private fun getCurrentTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun shouldAllowSemanticEntityOutput(
        userMessage: String,
        accounts: List<Account>,
        categories: List<Category>
    ): Boolean {
        if (accounts.isEmpty() || categories.isEmpty()) return true

        val normalizedMessage = userMessage.lowercase()
        val accountNames = accounts.map { it.name.lowercase() }
        val categoryNames = categories.map { it.name.lowercase() }
        val hasUnknownPaymentChannel = listOf("现金", "支付宝", "银行", "银行代发", "银行卡", "微信转账", "信用卡")
            .any { keyword -> keyword.lowercase() in normalizedMessage && accountNames.none { it in normalizedMessage } }
        val hasUnknownCategory = listOf("购物", "娱乐", "交通", "红包", "工资", "收入")
            .any { keyword -> keyword.lowercase() in normalizedMessage && categoryNames.none { it == keyword.lowercase() } }

        return hasUnknownPaymentChannel || hasUnknownCategory
    }
}
