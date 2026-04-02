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
    fun buildMessages_longBookkeepingScenario_usesCompactVariant() {
        val longMessage = "午饭 25 元 ".repeat(800)

        val messages = promptBuilder.buildMessages(
            userMessage = longMessage,
            currentButler = butler,
            accounts = accounts,
            categories = categories,
            scenario = AIAssistantRemotePromptScenario.Bookkeeping
        )

        val systemPrompt = messages[0].content
        assertTrue(systemPrompt.contains("超长账务文本场景"))
        assertTrue(systemPrompt.contains("只返回 JSON 对象"))
        assertTrue(systemPrompt.contains("最小账本上下文"))
        assertFalse(systemPrompt.contains("记账规则 - 重要"))
    }

    @Test
    fun buildMessages_bookkeepingScenario_limitsContext_whenMoreThanTwelveAccountsMatch() {
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
        val accountLines = systemPrompt.lines().filter { it.startsWith("- 账户") }
        assertEquals(12, accountLines.size)
    }
}
