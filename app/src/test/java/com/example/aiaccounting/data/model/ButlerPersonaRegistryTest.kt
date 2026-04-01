package com.example.aiaccounting.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ButlerPersonaRegistryTest {

    @Test
    fun buildGeneralConversationReply_returnsGreeting() {
        val reply = ButlerPersonaRegistry.buildGeneralConversationReply(
            butlerId = "custom_sebastian",
            lowerMessage = "你好",
            isNetworkAvailable = true,
            isAIConfigured = true,
            containsAny = { text, keywords -> keywords.any { text.contains(it) } }
        )

        assertTrue(reply.isNotBlank())
    }

    @Test
    fun buildGeneralConversationReply_returnsCapabilityReply() {
        val reply = ButlerPersonaRegistry.buildGeneralConversationReply(
            butlerId = "custom_sebastian",
            lowerMessage = "你能做什么",
            isNetworkAvailable = true,
            isAIConfigured = true,
            containsAny = { text, keywords -> keywords.any { text.contains(it) } }
        )

        assertTrue(reply.contains("记账"))
    }

    @Test
    fun buildIdentityReply_returnsCustomName_forDirectIdentityQuery() {
        val reply = ButlerPersonaRegistry.buildIdentityReply(
            butlerId = "custom_sebastian",
            queryType = com.example.aiaccounting.ai.IdentityConfirmationDetector.IdentityQueryType.DIRECT_IDENTITY_ASK,
            mentionedName = null,
            activeButlerName = "塞巴斯蒂安"
        )

        assertEquals("是的，我是塞巴斯蒂安。", reply)
    }

    @Test
    fun buildIdentityReply_rejectsOtherName_forCustomButler() {
        val reply = ButlerPersonaRegistry.buildIdentityReply(
            butlerId = "custom_sebastian",
            queryType = com.example.aiaccounting.ai.IdentityConfirmationDetector.IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
            mentionedName = "小财娘",
            activeButlerName = "塞巴斯蒂安"
        )

        assertEquals("不是，我是塞巴斯蒂安，不是小财娘。", reply)
    }

    @Test
    fun buildWelcomeReply_returnsCustomName_forCustomButler() {
        val reply = ButlerPersonaRegistry.buildWelcomeReply(
            butlerId = "custom_sebastian"
        )

        assertTrue(reply.isNotBlank())
    }
}
