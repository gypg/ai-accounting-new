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
                remoteResponse = "好的，今天支出18元"
            )
        } returns RemoteResponseDecision.ReturnRemoteReply("好的，今天支出18元")

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
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
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
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
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.TransportFailure)
        val message = (result as AIAssistantRemoteExecutionResult.TransportFailure).message
        assertEquals("服务暂时不可用，请稍后重试。", message)
        assertTrue(!message.contains("boom"))
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsIncompleteResponseAfterRetry_whenResponseIsStillIncomplete() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("```json\n{\"action\":\"add_transaction\"")
            }
        }
        coEvery { usageFailureRecorder() } returns Unit

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.IncompleteResponseAfterRetry)
        coVerify(exactly = 2) { usageFailureRecorder() }
    }

    @Test
    fun execute_retriesIncompleteResponse_andReturnsRemoteReply_whenSecondAttemptComplete() = runTest {
        var attempts = 0
        val handler = handlerWithStream {
            flow {
                attempts += 1
                if (attempts == 1) {
                    emit("```json\n{\"action\":\"add_transaction\"")
                } else {
                    emit("你好呀")
                }
            }
        }
        coEvery { usageFailureRecorder() } returns Unit
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery { interpreter.interpret(remoteResponse = "你好呀") } returns
            RemoteResponseDecision.ReturnRemoteReply("你好呀")

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.RemoteReply)
        assertEquals("你好呀", (result as AIAssistantRemoteExecutionResult.RemoteReply).reply)
        coVerify(exactly = 1) { usageFailureRecorder() }
        coVerify(exactly = 1) { usageSuccessRecorder() }
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
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
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
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.ActionExecutionRequested)
        val envelope = (result as AIAssistantRemoteExecutionResult.ActionExecutionRequested).envelope
        assertEquals(1, envelope.actions.size)
        assertTrue(envelope.actions.single() is AIAssistantTypedAction.AddTransaction)
    }

    @Test
    fun execute_returnsQueryBeforeExecutionRequested_whenWrappedChoicesContentContainsEnvelope() = runTest {
        val wrappedEnvelopeResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\"actions\":[{\"action\":\"add_transaction\",\"amount\":25,\"transactionType\":\"expense\",\"accountRef\":{\"id\":12,\"name\":\"日常卡\",\"kind\":\"account\"},\"categoryRef\":{\"id\":34,\"name\":\"餐饮\",\"kind\":\"category\"},\"note\":\"长文本账单\",\"date\":0}],\"reply\":\"已整理\"}"
                  }
                }
              ]
            }
        """.trimIndent()

        val realInterpreter = AIAssistantRemoteResponseInterpreter()
        val handler = AIAssistantRemoteExecutionHandler(
            streamCollector = AIAssistantRemoteStreamCollector(
                sendChatStream = { _, _ ->
                    flow { emit(wrappedEnvelopeResponse) }
                },
                recordUsageFailure = usageFailureRecorder,
                timeoutMillis = 1_000L
            ),
            integrityChecker = integrityChecker,
            interpreter = realInterpreter,
            recordUsageSuccess = usageSuccessRecorder,
            recordUsageFailure = usageFailureRecorder
        )
        coEvery { usageSuccessRecorder() } returns Unit

        val result = handler.execute(
            messages = listOf(
                ChatMessage(MessageRole.USER, "帮我整理这段超长记账文本：${"午饭 25 元 ".repeat(1200)}")
            ),
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested)
        result as AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested
        assertEquals(1, result.envelope.actions.size)
        val action = result.envelope.actions.single()
        assertTrue(action is AIAssistantTypedAction.AddTransaction)
        action as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
        assertEquals(12L, action.accountRef.id)
        assertEquals(34L, action.categoryRef.id)
        coVerify(exactly = 1) { usageSuccessRecorder() }
    }

    @Test
    fun execute_retriesEnvelopeCorrection_andReturnsActionExecution_whenSecondResponseIsEnvelope() = runTest {
        val correctionMessage = "请仅返回动作 envelope"
        val seenUserMessages = mutableListOf<String>()
        val envelope = AIAssistantActionEnvelope(
            actions = listOf(AIAssistantTypedAction.Query(target = "accounts"))
        )

        val handler = AIAssistantRemoteExecutionHandler(
            streamCollector = AIAssistantRemoteStreamCollector(
                sendChatStream = { chatMessages, _ ->
                    val lastUser = chatMessages.lastOrNull { it.role == MessageRole.USER }?.content.orEmpty()
                    seenUserMessages += lastUser
                    if (lastUser == correctionMessage) {
                        flow { emit("{\"action\":\"query_accounts\"}") }
                    } else {
                        flow { emit("好的，我来帮你处理。") }
                    }
                },
                recordUsageFailure = usageFailureRecorder,
                timeoutMillis = 1_000L
            ),
            integrityChecker = integrityChecker,
            interpreter = interpreter,
            recordUsageSuccess = usageSuccessRecorder,
            recordUsageFailure = usageFailureRecorder,
            buildBookkeepingEnvelopeCorrectionMessage = { correctionMessage }
        )

        coEvery { usageSuccessRecorder() } returns Unit
        coEvery { usageFailureRecorder() } returns Unit
        coEvery { interpreter.interpret(remoteResponse = "好的，我来帮你处理。") } returns
            RemoteResponseDecision.ReturnRemoteReply("好的，我来帮你处理。")
        coEvery { interpreter.interpret(remoteResponse = "{\"action\":\"query_accounts\"}") } returns
            RemoteResponseDecision.ExecuteActions(envelope)

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.ActionExecutionRequested)
        result as AIAssistantRemoteExecutionResult.ActionExecutionRequested
        assertEquals(envelope, result.envelope)
        assertEquals(listOf("帮我记一笔午饭 25 元", correctionMessage), seenUserMessages)
        coVerify(exactly = 1) { usageSuccessRecorder() }
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsTransactionActionMissing_afterEnvelopeCorrectionRetryStillReturnsPlainText() = runTest {
        val correctionMessage = "请仅返回动作 envelope"
        val seenUserMessages = mutableListOf<String>()

        val handler = AIAssistantRemoteExecutionHandler(
            streamCollector = AIAssistantRemoteStreamCollector(
                sendChatStream = { chatMessages, _ ->
                    val lastUser = chatMessages.lastOrNull { it.role == MessageRole.USER }?.content.orEmpty()
                    seenUserMessages += lastUser
                    if (lastUser == correctionMessage) {
                        flow { emit("仍然是解释文本") }
                    } else {
                        flow { emit("好的，我来帮你处理。") }
                    }
                },
                recordUsageFailure = usageFailureRecorder,
                timeoutMillis = 1_000L
            ),
            integrityChecker = integrityChecker,
            interpreter = interpreter,
            recordUsageSuccess = usageSuccessRecorder,
            recordUsageFailure = usageFailureRecorder,
            buildBookkeepingEnvelopeCorrectionMessage = { correctionMessage }
        )

        coEvery { usageSuccessRecorder() } returns Unit
        coEvery { usageFailureRecorder() } returns Unit
        coEvery { interpreter.interpret(remoteResponse = "好的，我来帮你处理。") } returns
            RemoteResponseDecision.ReturnRemoteReply("好的，我来帮你处理。")
        coEvery { interpreter.interpret(remoteResponse = "仍然是解释文本") } returns
            RemoteResponseDecision.ReturnRemoteReply("仍然是解释文本")

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.TransactionActionMissing)
        result as AIAssistantRemoteExecutionResult.TransactionActionMissing
        assertTrue(result.retriedWithEnvelopeCorrection)
        assertEquals(listOf("帮我记一笔午饭 25 元", correctionMessage), seenUserMessages)
        coVerify(exactly = 0) { usageSuccessRecorder() }
        coVerify(exactly = 2) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsRemoteReply_whenRequirementAllowsReply_andInterpreterReturnsRemoteReply() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("你好呀")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                remoteResponse = "你好呀"
            )
        } returns RemoteResponseDecision.ReturnRemoteReply("你好呀")

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.RemoteReply)
        assertEquals("你好呀", (result as AIAssistantRemoteExecutionResult.RemoteReply).reply)
    }

    @Test
    fun execute_retriesWhenReplyBlank_andReturnsRemoteReply_whenSecondAttemptHasText() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("```json\n{\"foo\":\"bar\"}\n```")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery { usageFailureRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                remoteResponse = "```json\n{\"foo\":\"bar\"}\n```"
            )
        } returnsMany listOf(
            RemoteResponseDecision.ReturnRemoteReply(""),
            RemoteResponseDecision.ReturnRemoteReply("你好呀")
        )

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.RemoteReply)
        assertEquals("你好呀", (result as AIAssistantRemoteExecutionResult.RemoteReply).reply)
        coVerify(exactly = 1) { usageSuccessRecorder() }
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun execute_returnsEmptyRemoteReply_afterRetryWhenReplyStaysBlank() = runTest {
        val handler = handlerWithStream {
            flow {
                emit("```json\n{\"foo\":\"bar\"}\n```")
            }
        }
        coEvery { usageSuccessRecorder() } returns Unit
        coEvery { usageFailureRecorder() } returns Unit
        coEvery {
            interpreter.interpret(
                remoteResponse = "```json\n{\"foo\":\"bar\"}\n```"
            )
        } returns RemoteResponseDecision.ReturnRemoteReply("")

        val result = handler.execute(
            messages = messages,
            config = config,
            responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
        )

        assertTrue(result is AIAssistantRemoteExecutionResult.EmptyRemoteReply)
        result as AIAssistantRemoteExecutionResult.EmptyRemoteReply
        assertTrue(result.retried)
        coVerify(exactly = 0) { usageSuccessRecorder() }
        coVerify(exactly = 2) { usageFailureRecorder() }
    }
}
