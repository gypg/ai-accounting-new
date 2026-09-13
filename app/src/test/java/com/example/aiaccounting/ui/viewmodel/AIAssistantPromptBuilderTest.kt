package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerPersonality
import com.example.aiaccounting.data.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantPromptBuilderTest {

    private val promptBuilder = AIAssistantPromptBuilder()

    private val butler = Butler(
        id = "cat",
        name = "小财娘",
        title = "可爱管家",
        avatarResId = 0,
        description = "",
        systemPrompt = "你是小财娘，一位温柔可靠的记账管家。",
        personality = ButlerPersonality.CUTE,
        specialties = emptyList()
    )

    private val accounts = listOf(
        Account(name = "微信", type = AccountType.WECHAT, balance = 100.0, isDefault = true),
        Account(name = "支付宝", type = AccountType.ALIPAY, balance = 88.0)
    )

    private val categories = listOf(
        Category(name = "餐饮", type = TransactionType.EXPENSE),
        Category(name = "工资", type = TransactionType.INCOME)
    )

    @Test
    fun buildMessages_chatScenario_keepsPersonaWithoutBookkeepingBaggage() {
        val messages = promptBuilder.buildMessages(
            userMessage = "你好呀",
            currentButler = butler,
            accounts = accounts,
            categories = categories,
            scenario = AIAssistantRemotePromptScenario.Chat
        )

        assertEquals(2, messages.size)
        assertEquals(MessageRole.SYSTEM, messages[0].role)
        assertTrue(messages[0].content.contains("你是小财娘"))
        assertTrue(messages[0].content.contains("普通聊天场景"))
        assertFalse(messages[0].content.contains("query_accounts"))
        assertFalse(messages[0].content.contains("add_transaction"))
        assertFalse(messages[0].content.contains("当前账本状况"))
    }

    @Test
    fun buildMessages_bookkeepingScenario_keepsPersonaAndActionContract() {
        val messages = promptBuilder.buildMessages(
            userMessage = "帮我记一笔午饭 25 元",
            currentButler = butler,
            accounts = accounts,
            categories = categories,
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        assertTrue(systemPrompt.contains("你是小财娘"))
        assertTrue(systemPrompt.contains("query_accounts"))
        assertTrue(systemPrompt.contains("add_transaction"))
        assertTrue(systemPrompt.contains("当前账本状况"))
        assertTrue(systemPrompt.contains("微信"))
        assertTrue(systemPrompt.contains("餐饮"))
    }

    @Test
    fun buildMessages_longBookkeepingScenario_enforcesStrictActionEnvelopeAndNoNarrativeOutput() {
        val longMessage = "午饭 25 元 微信支付 餐饮分类 ".repeat(800)

        val messages = promptBuilder.buildMessages(
            userMessage = longMessage,
            currentButler = butler,
            accounts = accounts,
            categories = categories,
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        assertTrue(systemPrompt.contains("记账规则 - 重要"))
        assertTrue(systemPrompt.contains("当前账本状况"))
        assertTrue(systemPrompt.contains("仅输出单个 JSON 对象"))
        assertTrue(systemPrompt.contains("解释性正文"))
        assertTrue(systemPrompt.contains("必须覆盖每一笔候选交易"))
    }

    @Test
    fun buildMessages_bookkeepingScenario_keepsFullContext_whenMoreThanTwelveAccounts() {
        val manyAccounts = (1..15).map { index ->
            Account(
                name = "账户$index",
                type = AccountType.CASH,
                balance = index.toDouble()
            )
        }
        val userMessage = manyAccounts.joinToString(" ") { it.name }

        val messages = promptBuilder.buildMessages(
            userMessage = userMessage,
            currentButler = butler,
            accounts = manyAccounts,
            categories = categories,
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        // Verify all 15 accounts appear in the prompt
        manyAccounts.forEach { acc ->
            assertTrue("账户 ${acc.name} 应该出现在 prompt 中", systemPrompt.contains(acc.name))
        }
        // Prompt must not truncate context for many accounts
        assertFalse(systemPrompt.contains("最小账本上下文"))
    }

    @Test
    fun buildMessages_bookkeepingScenario_whenLedgerContextIsEmpty_allowsNameBasedResolutionInsteadOfForcingIds() {
        val messages = promptBuilder.buildMessages(
            userMessage = "📅 2026年3月31日\n-12.00 餐饮（咖啡）——微信\n+5000.00 工资——银行代发",
            currentButler = butler,
            accounts = emptyList(),
            categories = emptyList(),
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        assertTrue(systemPrompt.contains("暂无账户"))
        assertTrue(systemPrompt.contains("暂无分类"))
        assertTrue(systemPrompt.contains("优先使用 categoryRef.name"))
        assertTrue(systemPrompt.contains("执行阶段自动解析或补全"))
        assertFalse(systemPrompt.contains("必须包含 id 字段"))
        assertFalse(systemPrompt.contains("严禁调用 create_account"))
    }

    @Test
    fun buildMessages_bookkeepingScenario_withExistingContext_stillPrefersIdsAndRejectsMerchantAsAccount() {
        val accountsWithIds = listOf(
            Account(id = 1, name = "微信", type = AccountType.WECHAT, balance = 100.0),
            Account(id = 2, name = "支付宝", type = AccountType.ALIPAY, balance = 88.0)
        )
        val categoriesWithIds = listOf(
            Category(id = 10, name = "餐饮", type = TransactionType.EXPENSE),
            Category(id = 11, name = "工资", type = TransactionType.INCOME)
        )

        val messages = promptBuilder.buildMessages(
            userMessage = "瑞幸微信支付的咖啡",
            currentButler = butler,
            accounts = accountsWithIds,
            categories = categoriesWithIds,
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        assertTrue(systemPrompt.contains("accountRef.id"))
        assertTrue(systemPrompt.contains("categoryRef.id"))
        assertTrue(systemPrompt.contains("商家名称"))
        assertTrue(systemPrompt.contains("瑞幸"))
        assertTrue(systemPrompt.contains("支付渠道才是账户"))
    }
}
