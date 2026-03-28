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
    fun execute_returnsReplyResult_whenPendingModificationStateExists() = runTest {
        val result = coordinator.execute(
            message = "确认",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Modification(pendingModificationState()),
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("不会走到这里") },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("已确认", shouldClearPending = true) },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "已确认",
                stage = AIAssistantInteractionStage.Reply
            ),
            result
        )
    }

    @Test
    fun execute_returnsClarificationRequired_whenPendingClarificationStillNeedsInput() = runTest {
        val pendingState = pendingClarificationState()

        val result = coordinator.execute(
            message = "不知道",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ ->
                ClarificationFlowResult.RequestClarification(
                    pendingState = pendingState,
                    reply = "请再补充金额"
                )
            },
            clearPendingClarificationAfterSuccessfulContinuation = {
                throw AssertionError("should not clear pending clarification")
            },
            restorePendingClarification = {
                throw AssertionError("should not restore pending clarification")
            },
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.ClarificationRequired(
                originalMessage = pendingState.originalMessage,
                question = "请再补充金额",
                stage = AIAssistantInteractionStage.Clarification
            ),
            result
        )
    }

    @Test
    fun execute_clearsPendingClarification_beforeContinuingMessage() = runTest {
        val pendingState = pendingClarificationState()
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.QUERY_INFORMATION)
        var cleared = false

        coEvery { aiReasoningEngine.reason(any(), any(), any()) } answers {
            assertTrue(cleared)
            reasoningResult
        }
        coEvery { aiReasoningEngine.executeActions(any()) } returns "本地查询结果"
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.LocalActions(
                actions = emptyList(),
                stage = AIAssistantInteractionStage.Execution
            )
        coEvery { messageOrchestrator.decideContinuation(any(), any()) } returns
            AIAssistantContinuationDecision.ExecuteLocally(
                AIAssistantMessageRoute.LocalActions(
                    actions = emptyList(),
                    stage = AIAssistantInteractionStage.Execution
                )
            )

        val result = coordinator.execute(
            message = "二十五元",
            currentButler = butler,
            conversationHistory = listOf("帮我记一笔午饭"),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ ->
                ClarificationFlowResult.ContinueWithPayload(
                    ClarificationContinuationRequest(
                        originalMessage = "帮我记一笔午饭",
                        resumedMessage = "帮我记一笔午饭 二十五元",
                        trigger = ClarificationTrigger.TRANSACTION_AMOUNT
                    )
                )
            },
            clearPendingClarificationAfterSuccessfulContinuation = { cleared = true },
            restorePendingClarification = {
                throw AssertionError("should not restore pending clarification")
            },
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "本地查询结果",
                stage = AIAssistantInteractionStage.Execution
            ),
            result
        )
        assertTrue(cleared)
    }

    @Test
    fun execute_requestsSecondRemoteOnce_whenClarificationContinuationIsRemote() = runTest {
        val pendingState = pendingClarificationState()
        val resumedMessage = "帮我记一笔午饭 二十五元"
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION)
        var remoteCallCount = 0

        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.RemoteOrLocalFallback(
                request = RemoteExecutionRequest(userMessage = resumedMessage)
            )
        coEvery { messageOrchestrator.decideContinuation(any(), any()) } returns
            AIAssistantContinuationDecision.RequestSecondRemote(
                request = RemoteExecutionRequest(
                    userMessage = resumedMessage,
                    continuationPayload = AIAssistantContinuationPayload(
                        originalMessage = pendingState.originalMessage,
                        resumedMessage = resumedMessage,
                        trigger = pendingState.trigger,
                        nextStep = AIAssistantContinuationStep.RequestSecondRemote
                    )
                )
            )

        val result = coordinator.execute(
            message = "二十五元",
            currentButler = butler,
            conversationHistory = listOf("帮我记一笔午饭"),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ ->
                ClarificationFlowResult.ContinueWithPayload(
                    ClarificationContinuationRequest(
                        originalMessage = pendingState.originalMessage,
                        resumedMessage = resumedMessage,
                        trigger = pendingState.trigger
                    )
                )
            },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {
                throw AssertionError("should not restore pending clarification")
            },
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = {
                remoteCallCount += 1
                "远程续执行结果"
            }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "远程续执行结果",
                stage = AIAssistantInteractionStage.Execution
            ),
            result
        )
        assertEquals(1, remoteCallCount)
    }

    @Test
    fun execute_restoresPendingClarification_whenContinuationExecutionFails() = runTest {
        val pendingState = pendingClarificationState()
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION)
        var restoredState: PendingClarificationState? = null

        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.RemoteOrLocalFallback(
                request = RemoteExecutionRequest(userMessage = "帮我记一笔午饭 二十五元")
            )
        coEvery { messageOrchestrator.decideContinuation(any(), any()) } returns
            AIAssistantContinuationDecision.RequestSecondRemote(
                request = RemoteExecutionRequest(userMessage = "帮我记一笔午饭 二十五元")
            )

        val error = runCatching {
            coordinator.execute(
                message = "二十五元",
                currentButler = butler,
                conversationHistory = emptyList(),
                isNetworkAvailable = true,
                currentUseBuiltinConfig = false,
                currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
                pendingInteractionState = PendingInteractionState.Clarification(pendingState),
                continuePendingClarification = { _, _ ->
                    ClarificationFlowResult.ContinueWithPayload(
                        ClarificationContinuationRequest(
                            originalMessage = pendingState.originalMessage,
                            resumedMessage = "帮我记一笔午饭 二十五元",
                            trigger = pendingState.trigger
                        )
                    )
                },
                clearPendingClarificationAfterSuccessfulContinuation = {},
                restorePendingClarification = { restoredState = it },
                handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
                handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
                processWithRemoteAI = { throw IllegalStateException("network failed") }
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("network failed", error?.message)
        assertEquals(pendingState, restoredState)
    }

    @Test
    fun execute_doesNotRestorePendingClarification_whenContinuationFinishesWithoutReexecution() = runTest {
        val pendingState = pendingClarificationState()
        var restored = false
        var cleared = false

        val result = coordinator.execute(
            message = "取消",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("已取消", shouldClearPending = true) },
            clearPendingClarificationAfterSuccessfulContinuation = { cleared = true },
            restorePendingClarification = { restored = true },
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "已取消",
                stage = AIAssistantInteractionStage.Reply
            ),
            result
        )
        assertTrue(!cleared)
        assertTrue(!restored)
    }

    @Test
    fun execute_keepsReplyStage_whenRouteReturnsClarificationStageFinish() = runTest {
        val pendingState = pendingClarificationState()

        val result = coordinator.execute(
            message = "先取消",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("已取消", shouldClearPending = true) },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "已取消",
                stage = AIAssistantInteractionStage.Reply
            ),
            result
        )
    }

    @Test
    fun execute_returnsClarificationRequired_whenLocalActionsContainRequestClarification() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.RECORD_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { aiReasoningEngine.executeActions(any()) } returns "请问这笔交易的金额是多少呢？"
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.LocalActions(
                actions = listOf(AIReasoningEngine.AIAction.RequestClarification("请问这笔交易的金额是多少呢？")),
                stage = AIAssistantInteractionStage.Clarification
            )

        var remoteCalled = false
        val result = coordinator.execute(
            message = "帮我记一笔午饭",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = null,
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("不会走到这里") },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = {
                remoteCalled = true
                "不会走到这里"
            }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.ClarificationRequired(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                stage = AIAssistantInteractionStage.Clarification
            ),
            result
        )
        assertTrue(!remoteCalled)
    }

    @Test
    fun execute_returnsRemoteResult_whenRouteIsRemoteFallback() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION)
        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.RemoteOrLocalFallback(
                request = RemoteExecutionRequest(userMessage = "帮我记一笔午饭")
            )

        val result = coordinator.execute(
            message = "帮我记一笔午饭",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = null,
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("不会走到这里") },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "远程结果" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "远程结果",
                stage = AIAssistantInteractionStage.Execution
            ),
            result
        )
    }

    @Test
    fun execute_returnsConfirmationRequired_whenModificationFlowStartsConfirmation() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.ModificationFlow(
                request = ModificationExecutionRequest(
                    message = "把上一笔改成20",
                    butlerId = butler.id,
                    pendingState = null,
                    stage = AIAssistantInteractionStage.Confirmation
                )
            )

        var remoteCalled = false
        val result = coordinator.execute(
            message = "把上一笔改成20",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = null,
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("不会走到这里") },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ ->
                ModificationFlowResult.StartConfirmation(
                    pendingState = pendingModificationState(),
                    reply = "请确认修改"
                )
            },
            processWithRemoteAI = {
                remoteCalled = true
                "不会走到这里"
            }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.ConfirmationRequired(
                message = "请确认修改",
                stage = AIAssistantInteractionStage.Confirmation
            ),
            result
        )
        assertTrue(!remoteCalled)
    }

    @Test
    fun execute_returnsReplyResult_whenRouteIsModificationFlowAndConfirmationContinues() = runTest {
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.MODIFY_TRANSACTION)
        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.ModificationFlow(
                request = ModificationExecutionRequest(
                    message = "确认",
                    butlerId = butler.id,
                    pendingState = pendingModificationState(),
                    stage = AIAssistantInteractionStage.Confirmation
                )
            )

        val result = coordinator.execute(
            message = "确认",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = null,
            continuePendingClarification = { _, _ -> ClarificationFlowResult.Finish("不会走到这里") },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("修改完成", shouldClearPending = true) },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.Reply(
                message = "修改完成",
                stage = AIAssistantInteractionStage.Reply
            ),
            result
        )
    }

    @Test
    fun execute_preservesOriginalMessage_whenContinuationTriggersAnotherClarification() = runTest {
        val pendingState = pendingClarificationState()
        val resumedMessage = "帮我记一笔午饭 25元"
        val payload = AIAssistantContinuationPayload(
            originalMessage = pendingState.originalMessage,
            resumedMessage = resumedMessage,
            trigger = pendingState.trigger,
            nextStep = AIAssistantContinuationStep.ExecuteLocally
        )
        val reasoningResult = reasoningResult(AIReasoningEngine.UserIntent.RECORD_TRANSACTION)

        coEvery { aiReasoningEngine.reason(any(), any(), any()) } returns reasoningResult
        coEvery { messageOrchestrator.route(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            AIAssistantMessageRoute.LocalActions(
                actions = listOf(AIReasoningEngine.AIAction.RequestClarification("这笔是收入还是支出？")),
                stage = AIAssistantInteractionStage.Clarification
            )
        coEvery { messageOrchestrator.decideContinuation(any(), any()) } returns
            AIAssistantContinuationDecision.ExecuteLocally(
                AIAssistantMessageRoute.LocalActions(
                    actions = listOf(AIReasoningEngine.AIAction.RequestClarification("这笔是收入还是支出？")),
                    stage = AIAssistantInteractionStage.Clarification
                )
            )
        coEvery { aiReasoningEngine.executeActions(any()) } returns "这笔是收入还是支出？"

        val result = coordinator.execute(
            message = "25元",
            currentButler = butler,
            conversationHistory = emptyList(),
            isNetworkAvailable = true,
            currentUseBuiltinConfig = false,
            currentAIConfig = AIConfig(isEnabled = true, apiKey = "key"),
            pendingInteractionState = PendingInteractionState.Clarification(pendingState),
            continuePendingClarification = { _, _ ->
                ClarificationFlowResult.ContinueWithPayload(
                    ClarificationContinuationRequest(
                        originalMessage = pendingState.originalMessage,
                        resumedMessage = resumedMessage,
                        trigger = pendingState.trigger
                    )
                )
            },
            clearPendingClarificationAfterSuccessfulContinuation = {},
            restorePendingClarification = {},
            handleModificationConfirmation = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            handleTransactionModification = { _, _ -> ModificationFlowResult.Finish("不会走到这里") },
            processWithRemoteAI = { "不会走到这里" }
        )

        assertEquals(
            AIAssistantMessageExecutionResult.ClarificationRequired(
                originalMessage = pendingState.originalMessage,
                question = "这笔是收入还是支出？",
                continuationPayload = payload,
                stage = AIAssistantInteractionStage.Clarification
            ),
            result
        )
    }

    private fun reasoningResult(intent: AIReasoningEngine.UserIntent) = AIReasoningEngine.ReasoningResult(
        intent = intent,
        confidence = 0.9f,
        actions = emptyList(),
        reasoningExplanation = "test"
    )

    private fun pendingModificationState() = PendingModificationState(
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

    private fun pendingClarificationState() = PendingClarificationState(
        originalMessage = "帮我记一笔午饭",
        question = "这笔金额是多少？",
        trigger = ClarificationTrigger.TRANSACTION_AMOUNT
    )
}
