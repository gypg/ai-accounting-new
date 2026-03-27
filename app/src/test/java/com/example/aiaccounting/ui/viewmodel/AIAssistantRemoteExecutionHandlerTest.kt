package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantRemoteExecutionHandlerTest {

    private val interpreter = mockk<AIAssistantRemoteResponseInterpreter>()
    private val integrityChecker = AIAssistantRemoteResponseIntegrityChecker()
    private val usageSuccessRecorder = mockk<suspend () -> Unit>()
    private val usageFailureRecorder = mockk<suspend () -> Unit>()

    private val config = AIConfig(isEnabled = true, apiKey = "key")
    private val messages = listOf(ChatMessage(MessageRole.USER, "帮我记一笔午饭 25 元"))

    private fun handlerWithStream(
        timeoutMillis: Long = 1_000L,
        stream: () -> kotlinx.coroutines.flow.Flow<String>
    ): AIAssistantRemoteExecutionHandler {
        return AIAssistantRemoteExecutionHandler(
            streamCollector = AIAssistantRemoteStreamCollector(
                sendChatStream = { _, _ -> stream() },
                recordUsageFailure = usageFailureRecorder,
                timeoutMillis = timeoutMillis
            ),
            integrityChecker = integrityChecker,
            interpreter = interpreter,
            recordUsageSuccess = usageSuccessRecorder,
            recordUsageFailure = usageFailureRecorder
        )
    }

    @Test
    fun execute_returnsRemoteReply_afterCollectingAllStreamChunks() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("好的，")
                emit("今天支出")
                emit("18元")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                userMessage = "你好",
                remoteResponse = "好的，今天支出18元"
            )
        } returns RemoteResponseDecision.ReturnRemoteReply("好的，今天支出18元")

        val result = handler.execute(
            userMessage = "你好",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.RemoteReply)
        assertEquals("好的，今天支出18元", (result as AIAssistantRemoteExecutionResult.RemoteReply).reply)
        coVerify(exactly = 1) { usageSuccessRecorder() }
    }

    @Test
    fun execute_returnsTimeout_whenStreamTimesOut() = runTest {
        val handler = handlerWithStream(timeoutMillis = 10L) {
            flow {
                kotlinx.coroutines.delay(50)
                emit("late")
            }
        }
        coEvery { usageFailureRecorder() } returns Unit

        val result = handler.execute(
            userMessage = "你好",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.Timeout)
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsTransportFailure_whenStreamingThrows() = runTest {
        val handler = handlerWithStream {
            throw IllegalStateException("boom")
        }
        coEvery { usageFailureRecorder() } returns Unit

        val result = handler.execute(
            userMessage = "你好",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.TransportFailure)
        assertEquals("boom", (result as AIAssistantRemoteExecutionResult.TransportFailure).message)
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsIncompleteResponse_whenResponseIsIncomplete() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("```json\n{\"action\":\"add_transaction\"")
            }
        }
        coEvery { usageFailureRecorder() } returns Unit

        val result = handler.execute(
            userMessage = "帮我记一笔午饭 25 元",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.IncompleteResponse)
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsQueryBeforeExecutionRequested_whenInterpreterRequestsQueryBeforeExecution() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("{\"action\":\"add_transaction\",\"amount\":25}")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                userMessage = "帮我记一笔午饭 25 元",
                remoteResponse = "{\"action\":\"add_transaction\",\"amount\":25}"
            )
        } returns RemoteResponseDecision.QueryBeforeExecute(
            AIAssistantActionEnvelope(
                actions = listOf(
                    AIAssistantTypedAction.AddTransaction(
                        amount = 25.0,
                        transactionTypeRaw = "expense",
                        categoryRef = AIAssistantEntityReference(
                            id = null,
                            name = "",
                            rawIdText = "",
                            kind = "category"
                        ),
                        accountRef = AIAssistantEntityReference(
                            id = null,
                            name = "",
                            rawIdText = "",
                            kind = "account"
                        ),
                        note = "",
                        dateTimestamp = 0L
                    )
                )
            )
        )

        val result = handler.execute(
            userMessage = "帮我记一笔午饭 25 元",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested)
        val envelope = (result as AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested).envelope
        assertEquals(1, envelope.actions.size)
        assertTrue(envelope.actions.single() is AIAssistantTypedAction.AddTransaction)
    }

    @Test
    fun execute_returnsActionExecutionRequested_whenInterpreterRequestsActionExecution() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("{\"action\":\"add_transaction\",\"amount\":25}")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                userMessage = "帮我记一笔午饭 25 元",
                remoteResponse = "{\"action\":\"add_transaction\",\"amount\":25}"
            )
        } returns RemoteResponseDecision.ExecuteActions(
            AIAssistantActionEnvelope(
                actions = listOf(
                    AIAssistantTypedAction.AddTransaction(
                        amount = 25.0,
                        transactionTypeRaw = "expense",
                        categoryRef = AIAssistantEntityReference(
                            id = null,
                            name = "",
                            rawIdText = "",
                            kind = "category"
                        ),
                        accountRef = AIAssistantEntityReference(
                            id = null,
                            name = "",
                            rawIdText = "",
                            kind = "account"
                        ),
                        note = "",
                        dateTimestamp = 0L
                    )
                )
            )
        )

        val result = handler.execute(
            userMessage = "帮我记一笔午饭 25 元",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.ActionExecutionRequested)
        val envelope = (result as AIAssistantRemoteExecutionResult.ActionExecutionRequested).envelope
        assertEquals(1, envelope.actions.size)
        assertTrue(envelope.actions.single() is AIAssistantTypedAction.AddTransaction)
    }

    @Test
    fun execute_returnsLocalFallbackRequested_whenInterpreterRequestsFallback() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("好的，我来帮你处理。")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                userMessage = "帮我记一笔午饭 25 元",
                remoteResponse = "好的，我来帮你处理。"
            )
        } returns RemoteResponseDecision.FallbackToLocalTransaction("好的，我来帮你处理。")

        val result = handler.execute(
            userMessage = "帮我记一笔午饭 25 元",
            messages = messages,
            config = config
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.LocalFallbackRequested)
        assertEquals(
            "好的，我来帮你处理。",
            (result as AIAssistantRemoteExecutionResult.LocalFallbackRequested).remoteReply
        )
        coVerify(exactly = 1) { usageSuccessRecorder() }
    }
}
