package com.example.aiaccounting.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityConfirmationDetectorTest {

    private val detector = IdentityConfirmationDetector()

    @Test
    fun detectIdentityQuery_whenAskingWhoAreYou_returnsTrue() {
        val result = detector.detectIdentityQuery("你是谁")
        assertTrue(result.isIdentityQuery)
    }

    @Test
    fun detectIdentityQuery_whenAskingModel_returnsFalse() {
        val result = detector.detectIdentityQuery("你是什么模型")
        assertFalse(result.isIdentityQuery)
    }

    @Test
    fun detectIdentityQuery_whenSpecificButlerName_returnsTrue() {
        val result = detector.detectIdentityQuery("你是小财娘吗")
        assertTrue(result.isIdentityQuery)
    }

    @Test
    fun generateIdentityResponse_whenButlerMatches_returnsPersonaReply() {
        val result = detector.generateIdentityResponse(
            currentButlerId = "taotao",
            queryResult = IdentityConfirmationDetector.IdentityQueryResult(
                isIdentityQuery = true,
                queryType = IdentityConfirmationDetector.IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
                mentionedName = "桃桃",
                confidence = 0.95f
            )
        )

        assertTrue(result.contains("我就是桃桃"))
    }

    @Test
    fun detectIdentityQuery_whenActiveCustomButlerNameIsAsked_returnsTrue() {
        val result = detector.detectIdentityQuery("你是小王吗", activeButlerName = "小王")
        assertTrue(result.isIdentityQuery)
        assertEquals(IdentityConfirmationDetector.IdentityQueryType.SPECIFIC_IDENTITY_CHECK, result.queryType)
        assertEquals("小王", result.mentionedName)
    }

    @Test
    fun detectIdentityQuery_whenActiveCustomLatinNameCaseDiff_stillMatchesSpecificIdentity() {
        val result = detector.detectIdentityQuery(
            message = "你是alice吗",
            activeButlerName = "Alice"
        )

        assertTrue(result.isIdentityQuery)
        assertEquals(
            IdentityConfirmationDetector.IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
            result.queryType
        )
        assertEquals("Alice", result.mentionedName)
    }

    @Test
    fun generateIdentityResponse_whenCustomButlerNameProvided_usesCustomName() {
        val result = detector.generateIdentityResponse(
            currentButlerId = "custom_butler",
            queryResult = IdentityConfirmationDetector.IdentityQueryResult(
                isIdentityQuery = true,
                queryType = IdentityConfirmationDetector.IdentityQueryType.DIRECT_IDENTITY_ASK,
                confidence = 0.95f
            ),
            activeButlerName = "小王"
        )

        assertEquals("是的，我是小王。有什么可以帮你的吗？", result)
    }
}
