package com.example.aiaccounting.ai

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
}
