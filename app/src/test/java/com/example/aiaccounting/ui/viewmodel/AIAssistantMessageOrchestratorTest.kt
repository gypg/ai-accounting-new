package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantMessageOrchestratorTest {

    private val orchestrator = AIAssistantMessageOrchestrator()

    @Test
    fun route_returnsModificationFlow_whenPendingStateExists() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.QUERY_INFORMATION),
            userMessage = "确认",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingState = PendingModificationState(
                intent = TransactionModificationHandler.ModificationIntent.MODIFY_LAST_TRANSACTION,
                confirmation = TransactionModificationHandler.ModificationConfirmation(
                    transaction = Transaction(
                        id = 1,
                        accountId = 1,
                        categoryId = 1,
                        type = TransactionType.EXPENSE,
                        amount = 10.0,
                        date = 0L,
                        note = "午饭"
                    ),
                    originalValues = emptyMap(),
                    newValues = emptyMap(),
                    confirmationMessage = "确认修改",
                    requiresConfirmation = true
                )
            )
        )

        assertTrue(route is AIAssistantMessageRoute.ModificationFlow)
    }

    @Test
    fun route_returnsRemoteFallback_whenIntentNeedsRemoteAndRemoteAvailable() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingState = null
        )

        assertTrue(route is AIAssistantMessageRoute.RemoteOrLocalFallback)
    }

    @Test
    fun route_returnsLocalActions_whenIntentNeedsRemoteButRemoteUnavailable() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.UNKNOWN),
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = false,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingState = null
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
    }

    @Test
    fun route_returnsModificationFlow_whenIntentIsModifyTransaction() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION),
            userMessage = "把上一笔改成 20",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingState = null
        )

        assertTrue(route is AIAssistantMessageRoute.ModificationFlow)
    }

    @Test
    fun route_returnsLocalActions_whenReasoningResultContainsRequestClarification_evenIfRemoteIsAvailable() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(
                intent = AIReasoningEngine.UserIntent.RECORD_TRANSACTION,
                actions = listOf(AIReasoningEngine.AIAction.RequestClarification("请问这笔交易的金额是多少呢？"))
            ),
            userMessage = "帮我记一笔午饭",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingState = null
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
    }

    private fun reasoningResult(
        intent: AIReasoningEngine.UserIntent,
        actions: List<AIReasoningEngine.AIAction> = emptyList()
    ) = AIReasoningEngine.ReasoningResult(
        intent = intent,
        confidence = 0.9f,
        actions = actions,
        reasoningExplanation = "test"
    )
}
