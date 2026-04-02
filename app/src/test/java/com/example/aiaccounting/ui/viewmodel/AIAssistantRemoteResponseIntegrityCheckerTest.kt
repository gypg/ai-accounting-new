package com.example.aiaccounting.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantRemoteResponseIntegrityCheckerTest {

    private val checker = AIAssistantRemoteResponseIntegrityChecker()

    @Test
    fun isComplete_returnsFalse_whenResponseIsBlank() {
        assertFalse(checker.isComplete("   "))
    }

    @Test
    fun isComplete_returnsFalse_whenFencedJsonIsNotClosed() {
        assertFalse(checker.isComplete("```json\n{\"action\":\"add_transaction\""))
    }

    @Test
    fun isComplete_returnsFalse_whenJsonBracesAreUnbalanced() {
        assertFalse(checker.isComplete("{\"action\":\"add_transaction\""))
    }

    @Test
    fun isComplete_returnsFalse_whenJsonHasTrailingComma() {
        assertFalse(checker.isComplete("{\"action\":\"add_transaction\",}"))
    }

    @Test
    fun isComplete_returnsFalse_whenFencedJsonHasTrailingComma() {
        assertFalse(checker.isComplete("```json\n{\"action\":\"add_transaction\",}\n```"))
    }

    @Test
    fun isComplete_returnsTrue_whenPlainTextReplyIsComplete() {
        assertTrue(checker.isComplete("好的，我来帮你处理。"))
    }

    @Test
    fun isComplete_returnsTrue_whenPlainTextContainsStrayBrace() {
        assertTrue(checker.isComplete("我会按 {预算} 来帮你整理。"))
    }

    @Test
    fun isComplete_returnsTrue_whenFencedJsonIsClosed() {
        assertTrue(checker.isComplete("```json\n{\"action\":\"add_transaction\"}\n```"))
    }

    @Test
    fun isComplete_returnsTrue_whenBareJsonIsValid() {
        assertTrue(checker.isComplete("{\"action\":\"add_transaction\"}"))
    }

    @Test
    fun isComplete_returnsTrue_whenBareJsonArrayIsValid() {
        assertTrue(checker.isComplete("[{\"action\":\"add_transaction\",\"amount\":25}]"))
    }

    @Test
    fun isComplete_returnsFalse_whenBareJsonArrayIsIncomplete() {
        assertFalse(checker.isComplete("[{\"action\":\"add_transaction\",\"amount\":25}"))
    }

    @Test
    fun isComplete_returnsTrue_whenNonJsonFenceContainsPlainText() {
        assertTrue(checker.isComplete("```text\nhello\n```"))
    }

    @Test
    fun isComplete_returnsFalse_whenOpenAiChoicesContentContainsHalfJson() {
        assertFalse(checker.isComplete("{\"choices\":[{\"message\":{\"content\":\"{\\\"action\\\":\\\"add_transaction\\\"\"}}]}"))
    }

    @Test
    fun isComplete_returnsTrue_whenOpenAiChoicesContentContainsCompleteJson() {
        assertTrue(checker.isComplete("{\"choices\":[{\"message\":{\"content\":\"{\\\"action\\\":\\\"add_transaction\\\"}\"}}]}"))
    }

    @Test
    fun isComplete_returnsTrue_whenOpenAiChoicesContentIsPlainText() {
        assertTrue(checker.isComplete("{\"choices\":[{\"message\":{\"content\":\"您好，我在呢\"}}]}"))
    }
}
