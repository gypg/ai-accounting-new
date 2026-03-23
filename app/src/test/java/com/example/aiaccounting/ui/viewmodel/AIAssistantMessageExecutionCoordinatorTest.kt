package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerPersonality
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AIAssistantMessageExecutionCoordinatorTest {

    private val aiReasoningEngine = mockk<AIReasoningEngine>()
    private val messageOrchestrator = mockk<AIAssistantMessageOrchestrator>()
    private val coordinator = AIAssistantMessageExecutionCoordinator(aiReasoningEngine, messageOrchestrator)

    private val butler = Butler(
        id = "xiaocainiang",
        name = "小财娘",
        title = "可爱管家",
        avatarResId = 0,
        description = "",
        systemPrompt = "",
        personality = ButlerPersonality.CUTE,
        specialties = emptyList()
    )

    @Test
    fun execute_returnsConfirmationResult_whenPendingStateExists() = runTest {
        val result = coordinator.execute(
            message = "确认",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = pendingState(),
            handleModificationConfirmation = { _, _ -> "已确认" },
            handleTransactionModification = { _, _ -> "不会走到这里" },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals("已确认", result)
    }

    @Test
    fun execute_returnsLocalActionResult_whenRouteIsLocalActions() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.QUERY_INFORMATION)
        coEvery { aiReasoningEngine.reason(any(), any()) } returns reasoningResult
        coEvery { aiReasoningEngine.executeActions(any()) } returns "本地查询结果"
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.LocalActions(emptyList())

        val result = coordinator.execute(
            message = "查看余额",
            currentButler = butler,
            conversationHistory = listOf("你好"),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = null,
            handleModificationConfirmation = { _, _ -> "不会走到这里" },
            handleTransactionModification = { _, _ -> "不会走到这里" },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals("本地查询结果", result)
    }

    @Test
    fun execute_returnsRemoteResult_whenRouteIsRemoteFallback() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION)
        coEvery { aiReasoningEngine.reason(any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.RemoteOrLocalFallback("帮我记一笔午饭")

        val result = coordinator.execute(
            message = "帮我记一笔午饭",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = null,
            handleModificationConfirmation = { _, _ -> "不会走到这里" },
            handleTransactionModification = { _, _ -> "不会走到这里" },
            processWithRemoteAI = { "远程结果" }
        )

        assertEquals("远程结果", result)
    }

    @Test
    fun execute_returnsModificationResult_whenRouteIsModificationFlow() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.ModificationFlow(
                message = "把上一笔改成20",
                butlerId = butler.id,
                pendingState = null
            )

        val result = coordinator.execute(
            message = "把上一笔改成20",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = null,
            handleModificationConfirmation = { _, _ -> "不会走到这里" },
            handleTransactionModification = { _, _ -> "修改结果" },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals("修改结果", result)
    }

    private fun reasoningResult(intent: AIReasoningEngine.UserIntent) = AIReasoningEngine.ReasoningResult(
        intent = intent,
        confidence = 0.9f,
        actions = emptyList(),
        reasoningExplanation = "test"
    )

    private fun pendingState() = PendingModificationState(
        intent = com.example.aiaccounting.ai.TransactionModificationHandler.ModificationIntent.MODIFY_LAST_TRANSACTION,
        confirmation = com.example.aiaccounting.ai.TransactionModificationHandler.ModificationConfirmation(
            transaction = com.example.aiaccounting.data.local.entity.Transaction(
                id = 1,
                accountId = 1,
                categoryId = 1,
                type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
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
}
