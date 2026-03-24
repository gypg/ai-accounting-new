package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantMessageOrchestratorTest {

    private val orchestrator = AIAssistantMessageOrchestrator()

    @Test
    fun route_returnsModificationFlow_whenPendingModificationExists() {
        val pendingState = PendingModificationState(
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

        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.QUERY_INFORMATION),
            userMessage = "确认",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = PendingInteractionState.Modification(pendingState)
        )

        assertTrue(route is AIAssistantMessageRoute.ModificationFlow)
        route as AIAssistantMessageRoute.ModificationFlow
        assertEquals(pendingState, route.request.pendingState)
        assertEquals(AIAssistantInteractionStage.Confirmation, route.request.stage)
    }

    @Test
    fun route_doesNotReturnModificationFlow_whenPendingClarificationExists() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
            userMessage = "补充一下是午饭",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isBuiltinConfigEnabled = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = PendingInteractionState.Clarification(
                PendingClarificationState(
                    originalMessage = "帮我记一笔",
                    question = "是什么分类？",
                    trigger = ClarificationTrigger.TRANSACTION_CATEGORY
                )
            )
        )

        assertTrue(route is AIAssistantMessageRoute.RemoteOrLocalFallback)
        route as AIAssistantMessageRoute.RemoteOrLocalFallback
        assertEquals("补充一下是午饭", route.request.userMessage)
        assertEquals(AIAssistantInteractionStage.Execution, route.request.stage)
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
            pendingInteractionState = null
        )

        assertTrue(route is AIAssistantMessageRoute.RemoteOrLocalFallback)
        route as AIAssistantMessageRoute.RemoteOrLocalFallback
        assertEquals("帮我记一笔午饭 25 元", route.request.userMessage)
        assertEquals(AIAssistantInteractionStage.Execution, route.request.stage)
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
            pendingInteractionState = null
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
        route as AIAssistantMessageRoute.LocalActions
        assertEquals(AIAssistantInteractionStage.Execution, route.stage)
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
            pendingInteractionState = null
        )

        assertTrue(route is AIAssistantMessageRoute.ModificationFlow)
        route as AIAssistantMessageRoute.ModificationFlow
        assertEquals(AIAssistantInteractionStage.Confirmation, route.request.stage)
        assertEquals("把上一笔改成 20", route.request.message)
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
            pendingInteractionState = null
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
        route as AIAssistantMessageRoute.LocalActions
        assertEquals(AIAssistantInteractionStage.Clarification, route.stage)
    }

    @Test
    fun decideContinuation_requestsSecondRemote_whenRouteNeedsRemoteFallback() {
        val route = AIAssistantMessageRoute.RemoteOrLocalFallback(
            request = RemoteExecutionRequest(userMessage = "帮我记一笔午饭 25 元")
        )
        val payload = AIAssistantContinuationPayload(
            originalMessage = "帮我记一笔午饭",
            resumedMessage = "帮我记一笔午饭 25 元",
            trigger = ClarificationTrigger.TRANSACTION_AMOUNT,
            nextStep = AIAssistantContinuationStep.RequestSecondRemote
        )
        val decision = orchestrator.decideContinuation(route, payload)

        assertEquals(
            AIAssistantContinuationDecision.RequestSecondRemote(
                RemoteExecutionRequest(
                    userMessage = "帮我记一笔午饭 25 元",
                    continuationPayload = payload
                )
            ),
            decision
        )
    }

    @Test
    fun decideContinuation_executesLocally_whenRouteIsLocalActions() {
        val route = AIAssistantMessageRoute.LocalActions(emptyList())
        val payload = AIAssistantContinuationPayload(
            originalMessage = "帮我记一笔午饭",
            resumedMessage = "帮我记一笔午饭 25 元",
            trigger = ClarificationTrigger.TRANSACTION_AMOUNT,
            nextStep = AIAssistantContinuationStep.ExecuteLocally
        )
        val decision = orchestrator.decideContinuation(route, payload)

        assertEquals(
            AIAssistantContinuationDecision.ExecuteLocally(
                route.copy(stage = AIAssistantInteractionStage.Execution)
            ),
            decision
        )
    }

    @Test
    fun decideContinuation_executesLocally_whenRouteIsModificationFlow() {
        val route = AIAssistantMessageRoute.ModificationFlow(
            request = ModificationExecutionRequest(
                message = "把上一笔改成 20",
                butlerId = "xiaocainiang",
                pendingState = null,
                stage = AIAssistantInteractionStage.Execution
            )
        )
        val payload = AIAssistantContinuationPayload(
            originalMessage = "把上一笔改成 10",
            resumedMessage = "把上一笔改成 20",
            trigger = ClarificationTrigger.GENERIC,
            nextStep = AIAssistantContinuationStep.ExecuteModification
        )
        val decision = orchestrator.decideContinuation(route, payload)

        assertEquals(
            AIAssistantContinuationDecision.ExecuteLocally(
                AIAssistantMessageRoute.ModificationFlow(
                    route.request.copy(stage = AIAssistantInteractionStage.Confirmation)
                )
            ),
            decision
        )
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
