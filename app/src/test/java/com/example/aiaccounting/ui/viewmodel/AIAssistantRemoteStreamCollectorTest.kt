package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantRemoteStreamCollectorTest {

    private val usageFailureRecorder = mockk<suspend () -> Unit>()
    private val config = AIConfig(isEnabled = true, apiKey = "key")
    private val messages = listOf(ChatMessage(MessageRole.USER, "你好"))

    @Test
    fun collect_returnsSuccessAndRecordsUsage_whenChunksComplete() = runTest {
        val collector = AIAssistantRemoteStreamCollector(
            sendChatStream = { _, _ ->
                flow {
                    emit("好的，")
                    emit("今天支出")
                    emit("18元")
                }
            },
            recordUsageFailure = usageFailureRecorder,
            timeoutMillis = 1_000L
        )

        val result = collector.collect(messages, config)

        assertTrue(result is RemoteStreamCollectionResult.Success)
        assertEquals("好的，今天支出18元", (result as RemoteStreamCollectionResult.Success).response)
    }

    @Test
    fun collect_returnsTimeoutAndRecordsFailure_whenStreamExceedsLimit() = runTest {
        val collector = AIAssistantRemoteStreamCollector(
            sendChatStream = { _, _ ->
                flow {
                    delay(50)
                    emit("late")
                }
            },
            recordUsageFailure = usageFailureRecorder,
            timeoutMillis = 10L
        )
        coEvery { usageFailureRecorder() } returns Unit

        val result = collector.collect(messages, config)

        assertTrue(result is RemoteStreamCollectionResult.Timeout)
        coVerify(exactly = 1) { usageFailureRecorder() }
    }

    @Test
    fun collect_returnsFailureAndRecordsFailure_whenStreamThrows() = runTest {
        val collector = AIAssistantRemoteStreamCollector(
            sendChatStream = { _, _ -> throw IllegalStateException("boom") },
            recordUsageFailure = usageFailureRecorder,
            timeoutMillis = 1_000L
        )
        coEvery { usageFailureRecorder() } returns Unit

        val result = collector.collect(messages, config)

        assertTrue(result is RemoteStreamCollectionResult.Failure)
        assertEquals("boom", (result as RemoteStreamCollectionResult.Failure).message)
        coVerify(exactly = 1) { usageFailureRecorder() }
    }
}
