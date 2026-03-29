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
    fun analyze_returnsDailyChatTopLevelIntent_whenReasoningIsGeneralConversation() {
        val analysis = orchestrator.analyze(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
            userMessage = "你好呀",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        assertEquals(AIAssistantTopLevelIntent.DAILY_CHAT, analysis.topLevelIntent)
        assertEquals(AIAssistantEngineMode.Remote, analysis.engineMode)
    }

    @Test
    fun analyze_returnsBookkeepingTopLevelIntent_whenReasoningIsRecordTransaction() {
        val analysis = orchestrator.analyze(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.RECORD_TRANSACTION),
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = false,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        assertEquals(AIAssistantTopLevelIntent.BOOKKEEPING, analysis.topLevelIntent)
        assertEquals(AIAssistantEngineMode.Local, analysis.engineMode)
    }

    @Test
    fun analyze_returnsOcrImageTopLevelIntent_whenMessageContainsImagePayload() {
        val analysis = orchestrator.analyze(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
            userMessage = "data:image/png;base64,abc",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        assertEquals(AIAssistantTopLevelIntent.OCR_IMAGE, analysis.topLevelIntent)
    }

    @Test
    fun analyze_returnsOcrImageTopLevelIntent_whenMessageContainsRemoteImageUrl() {
        val analysis = orchestrator.analyze(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
            userMessage = "https://example.com/bill.jpg",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        assertEquals(AIAssistantTopLevelIntent.OCR_IMAGE, analysis.topLevelIntent)
    }

    @Test
    fun route_usesAnalysisIntentNotMessageHeuristics_whenMessageLooksLikeBookkeeping_butAnalysisIsDailyChat() {
        val route = orchestrator.route(
            AIAssistantMessageAnalysis(
                reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION),
                topLevelIntent = AIAssistantTopLevelIntent.DAILY_CHAT,
                userMessage = "帮我记一笔午饭 25 元",
                butlerId = "xiaocainiang",
                pendingInteractionState = null,
                engineMode = AIAssistantEngineMode.Remote,
                hasClarificationAction = false
            )
        )

        assertTrue(route is AIAssistantMessageRoute.RemoteOrLocalFallback)
    }

    @Test
    fun route_usesAnalysisIntentNotMessageHeuristics_whenMessageLooksLikeChat_butAnalysisIsBookkeeping() {
        val route = orchestrator.route(
            AIAssistantMessageAnalysis(
                reasoningResult = reasoningResult(
                    intent = AIReasoningEngine.UserIntent.RECORD_TRANSACTION,
                    actions = listOf(AIReasoningEngine.AIAction.GenerateResponse("本地记账"))
                ),
                topLevelIntent = AIAssistantTopLevelIntent.BOOKKEEPING,
                userMessage = "你好呀",
                butlerId = "xiaocainiang",
                pendingInteractionState = null,
                engineMode = AIAssistantEngineMode.Local,
                hasClarificationAction = false
            )
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
    }

    @Test
    fun route_returnsLocalActions_whenTopLevelIntentIsDailyChat_andEngineModeIsLocal() {
        val route = orchestrator.route(
            AIAssistantMessageAnalysis(
                reasoningResult = reasoningResult(
                    AIReasoningEngine.UserIntent.GENERAL_CONVERSATION,
                    actions = listOf(AIReasoningEngine.AIAction.GenerateResponse("本地聊天"))
                ),
                topLevelIntent = AIAssistantTopLevelIntent.DAILY_CHAT,
                userMessage = "你好呀",
                butlerId = "xiaocainiang",
                pendingInteractionState = null,
                engineMode = AIAssistantEngineMode.Local,
                hasClarificationAction = false
            )
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
        route as AIAssistantMessageRoute.LocalActions
        assertEquals(AIAssistantInteractionStage.Execution, route.stage)
    }

    @Test
    fun route_returnsLocalActions_whenTopLevelIntentIsBookkeeping_andEngineModeIsLocal() {
        val route = orchestrator.route(
            AIAssistantMessageAnalysis(
                reasoningResult = reasoningResult(
                    AIReasoningEngine.UserIntent.RECORD_TRANSACTION,
                    actions = listOf(AIReasoningEngine.AIAction.GenerateResponse("本地记账"))
                ),
                topLevelIntent = AIAssistantTopLevelIntent.BOOKKEEPING,
                userMessage = "帮我记一笔午饭 25 元",
                butlerId = "xiaocainiang",
                pendingInteractionState = null,
                engineMode = AIAssistantEngineMode.Local,
                hasClarificationAction = false
            )
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
        route as AIAssistantMessageRoute.LocalActions
        assertEquals(AIAssistantInteractionStage.Execution, route.stage)
    }

    @Test
    fun route_returnsLocalActions_whenTopLevelIntentIsOcrImage_evenIfRemoteAvailable() {
        val route = orchestrator.route(
            AIAssistantMessageAnalysis(
                reasoningResult = reasoningResult(
                    AIReasoningEngine.UserIntent.GENERAL_CONVERSATION,
                    actions = listOf(AIReasoningEngine.AIAction.GenerateResponse("本地 OCR 处理提示"))
                ),
                topLevelIntent = AIAssistantTopLevelIntent.OCR_IMAGE,
                userMessage = "data:image/png;base64,abc",
                butlerId = "xiaocainiang",
                pendingInteractionState = null,
                engineMode = AIAssistantEngineMode.Remote,
                hasClarificationAction = false
            )
        )

        assertTrue(route is AIAssistantMessageRoute.LocalActions)
        route as AIAssistantMessageRoute.LocalActions
        assertEquals(AIAssistantInteractionStage.Execution, route.stage)
    }

    @Test
    fun route_legacyAdapter_matchesAnalyzeThenRoute() {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.UNKNOWN)

        val legacyRoute = orchestrator.route(
            reasoningResult = reasoningResult,
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        val analysis = orchestrator.analyze(
            reasoningResult = reasoningResult,
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        val twoPhaseRoute = orchestrator.route(analysis)

        assertEquals(legacyRoute, twoPhaseRoute)
    }

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
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.RECORD_TRANSACTION),
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = true,
            isAIEnabled = true,
            hasApiKey = true,
            pendingInteractionState = null
        )

        assertTrue(route is AIAssistantMessageRoute.RemoteOrLocalFallback)
        route as AIAssistantMessageRoute.RemoteOrLocalFallback
        assertEquals("帮我记一笔午饭 25 元", route.request.userMessage)
        assertEquals(AIAssistantInteractionStage.Execution, route.request.stage)
        assertEquals(AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired, route.request.responseRequirement)
    }

    @Test
    fun route_returnsLocalActions_whenIntentNeedsRemoteButRemoteUnavailable() {
        val route = orchestrator.route(
            reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.UNKNOWN),
            userMessage = "帮我记一笔午饭 25 元",
            butlerId = "xiaocainiang",
            isNetworkAvailable = false,
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

    @Test
    fun buildBookkeepingEnvelopeCorrectionMessage_returnsStrictJsonEnvelopeInstruction() {
        val message = orchestrator.buildBookkeepingEnvelopeCorrectionMessage()

        assertTrue(message.contains("仅返回"))
        assertTrue(message.contains("JSON"))
        assertTrue(message.contains("actions"))
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
