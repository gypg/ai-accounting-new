package com.example.aiaccounting.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantRemoteResponseInterpreterTest {

    private val interpreter = AIAssistantRemoteResponseInterpreter()

    @Test
    fun interpret_returnsExecuteActions_whenResponseContainsActionField() {
        val decision = interpreter.interpret(
            userMessage = "帮我记一笔午饭 25 元",
            remoteResponse = "{\"action\":\"add_transaction\",\"amount\":25}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
    }

    @Test
    fun interpret_returnsExecuteActions_whenJsonActionHasTextPreamble() {
        val decision = interpreter.interpret(
            userMessage = "帮我记一笔午饭 25 元",
            remoteResponse = "好的，下面是执行结果：{\"action\":\"add_transaction\",\"amount\":25}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
    }

    @Test
    fun interpret_returnsFallbackToLocalTransaction_whenPlainTextReplyForTransactionRequest() {
        val decision = interpreter.interpret(
            userMessage = "昨天买咖啡花了18元",
            remoteResponse = "好的，我来帮你处理这笔消费。"
        )

        assertTrue(decision is RemoteResponseDecision.FallbackToLocalTransaction)
    }

    @Test
    fun interpret_returnsRemoteReply_whenPlainTextMentionsActionWord() {
        val decision = interpreter.interpret(
            userMessage = "你是谁",
            remoteResponse = "我可以根据你的需求决定下一步 action。"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("我可以根据你的需求决定下一步 action。", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_returnsFallbackToLocalTransaction_whenMessageContainsLedgerPhrase() {
        val decision = interpreter.interpret(
            userMessage = "帮我记一笔午饭 25 元",
            remoteResponse = "好的，我来帮你处理这笔记录。"
        )

        assertTrue(decision is RemoteResponseDecision.FallbackToLocalTransaction)
    }

    @Test
    fun combineRemoteAndLocalReply_returnsLocalError_whenLocalFallbackFails() {
        val combined = interpreter.combineRemoteAndLocalReply(
            remoteReply = "好的，我来帮你处理。",
            localResult = "❌ 记账失败：金额必须大于0"
        )

        assertEquals("❌ 记账失败：金额必须大于0", combined)
    }
}
