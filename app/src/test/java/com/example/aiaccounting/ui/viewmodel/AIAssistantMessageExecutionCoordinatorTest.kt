package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerPersonality
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun execute_returnsReplyResult_whenPendingStateExists() = runTest {
        val result = coordinator.execute(
            message = "确认",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = pendingState(),
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("已确认", shouldClearPending = true) },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(AIAssistantMessageExecutionResult.Reply("已确认"), result)
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
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(AIAssistantMessageExecutionResult.Reply("本地查询结果"), result)
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
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "远程结果" }
        )

        assertEquals(AIAssistantMessageExecutionResult.Reply("远程结果"), result)
    }

    @Test
    fun execute_returnsConfirmationRequired_whenModificationFlowStartsConfirmation() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.ModificationFlow(
                message = "把上一笔改成20",
                butlerId = butler.id,
                pendingState = null
            )

        var remoteCalled = false
        val result = coordinator.execute(
            message = "把上一笔改成20",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = null,
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ ->
                ModificationFlowResult.StartConfirmation(
                    pendingState = pendingState(),
                    reply = "请确认修改"
                )
            },
            processWithRemoteAI = {
                remoteCalled = true
                "不会走到这里"
            }
        )

        assertEquals(AIAssistantMessageExecutionResult.ConfirmationRequired("请确认修改"), result)
        assertTrue(!remoteCalled)
    }

    @Test
    fun execute_returnsReplyResult_whenRouteIsModificationFlowAndConfirmationContinues() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.ModificationFlow(
                message = "确认",
                butlerId = butler.id,
                pendingState = pendingState()
            )

        val result = coordinator.execute(
            message = "确认",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingState = pendingState(),
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("修改完成", shouldClearPending = true) },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(AIAssistantMessageExecutionResult.Reply("修改完成"), result)
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
