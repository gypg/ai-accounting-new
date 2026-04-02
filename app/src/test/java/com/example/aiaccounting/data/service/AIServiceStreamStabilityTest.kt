package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.data.repository.AIModelPerformanceRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class AIServiceStreamStabilityTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendChatStream_whenTransportFails_propagatesExceptionInsteadOfEmittingFailureText() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        val timeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .callTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val service = AIService(
            client = timeoutClient,
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = runCatching {
            service.sendChatStream(
                messages = listOf(ChatMessage(MessageRole.USER, "hi")),
                config = config
            ).toList()
        }.exceptionOrNull()

        assertTrue(error != null)
        assertTrue(error?.message?.contains("请求失败") != true)
    }

    @Test
    fun sendChatStream_whenPayloadIsOversized_usesSingleNonStreamRequest() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "ok"}}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "unexpected-second-call"}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )
        val oversizedMessage = "午饭 25 元 ".repeat(1200)

        val chunks = service.sendChatStream(
            messages = listOf(ChatMessage(MessageRole.USER, oversizedMessage)),
            config = config
        ).toList()

        assertEquals(listOf("ok"), chunks)
        assertEquals(1, server.requestCount)
        val onlyRequest = server.takeRequest()
        val requestBody = if (onlyRequest.getHeader("Content-Encoding") == "gzip") {
            ungzip(onlyRequest.body)
        } else {
            onlyRequest.body.readUtf8()
        }
        assertTrue(requestBody.contains("\"stream\":false"))
    }

    @Test
    fun sendChatStream_whenPayloadIsOversized_andNonStreamReturnsActionEnvelopeContent_emitsEnvelopeJsonChunk() = kotlinx.coroutines.test.runTest {
        val envelopeContent = "{\"actions\":[{\"action\":\"add_transaction\",\"amount\":25,\"transactionType\":\"expense\",\"accountRef\":{\"id\":12,\"name\":\"日常卡\",\"kind\":\"account\"},\"categoryRef\":{\"id\":34,\"name\":\"餐饮\",\"kind\":\"category\"},\"note\":\"长文本账单\",\"date\":0}],\"reply\":\"已整理长文本账单\"}"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": ${JSONObject.quote(envelopeContent)} }}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val chunks = service.sendChatStream(
            messages = listOf(ChatMessage(MessageRole.USER, "帮我整理这段超长记账文本：${"午饭 25 元 ".repeat(1200)}")),
            config = config
        ).toList()

        assertEquals(1, chunks.size)
        assertEquals(envelopeContent, chunks.single())
        assertEquals(1, server.requestCount)

        val onlyRequest = server.takeRequest()
        val requestBody = if (onlyRequest.getHeader("Content-Encoding") == "gzip") {
            ungzip(onlyRequest.body)
        } else {
            onlyRequest.body.readUtf8()
        }
        assertTrue(requestBody.contains("\"stream\":false"))
    }

    @Test
    fun chat_whenLongTextInput_usesReducedMaxTokensBudget() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "ok"}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )
        val longMessage = "午饭 25 元 ".repeat(1200)

        val response = service.chat(
            messages = listOf(ChatMessage(MessageRole.USER, longMessage)),
            config = config
        )

        assertEquals("ok", response)
        val request = server.takeRequest()
        val body = if (request.getHeader("Content-Encoding") == "gzip") {
            ungzip(request.body)
        } else {
            request.body.readUtf8()
        }
        assertTrue(body.contains("\"max_tokens\":2500"))
    }

    @Test
    fun chat_whenBookkeepingPrompt_usesReducedMaxTokensBudget() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "ok"}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )
        val messages = listOf(
            ChatMessage(
                MessageRole.SYSTEM,
                "请仅返回可执行动作 envelope。仅返回 JSON 对象：{\"actions\":[...],\"reply\":\"...\"}"
            ),
            ChatMessage(MessageRole.USER, "帮我记一笔午饭 25 元")
        )

        val response = service.chat(messages = messages, config = config)

        assertEquals("ok", response)
        val request = server.takeRequest()
        val body = if (request.getHeader("Content-Encoding") == "gzip") {
            ungzip(request.body)
        } else {
            request.body.readUtf8()
        }
        assertTrue(body.contains("\"max_tokens\":2000"))
    }

    @Test
    fun chat_whenChoicesMissingContent_retriesAndSucceeds() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {}}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "ok"}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val response = service.chat(
            messages = listOf(ChatMessage(MessageRole.USER, "hi")),
            config = config
        )

        assertEquals("ok", response)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun chat_whenChoicesMissingContentAfterRetry_fails() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {}}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk<AIModelPerformanceRepository>(relaxed = true)
        )
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = runCatching {
            service.chat(
                messages = listOf(ChatMessage(MessageRole.USER, "hi")),
                config = config
            )
        }.exceptionOrNull()

        assertTrue(error != null)
        assertFalse(error?.message?.contains("请求失败") == true)
        assertEquals(2, server.requestCount)
    }

    private fun ungzip(buffer: Buffer): String {
        return GZIPInputStream(buffer.inputStream()).bufferedReader().use { it.readText() }
    }
}
