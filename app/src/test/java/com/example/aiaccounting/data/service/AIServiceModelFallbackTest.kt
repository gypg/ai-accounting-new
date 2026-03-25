package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.data.repository.AIModelPerformanceRepository
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class AIServiceModelFallbackTest {

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
    fun chat_whenRequestBodyIsLarge_sendsGzipCompressedPayload() = kotlinx.coroutines.test.runTest {
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
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )
        val largeMessage = "x".repeat(2048)

        val result = service.chat(
            messages = listOf(ChatMessage(role = MessageRole.USER, content = largeMessage)),
            config = cfg
        )

        assertEquals("ok", result)
        val request = server.takeRequest()
        assertEquals("gzip", request.getHeader("Content-Encoding"))
        val decompressedBody = ungzip(request.body)
        assertTrue(decompressedBody.contains("\"model\":\"fixed-model\""))
        assertTrue(decompressedBody.contains(largeMessage))
    }

    @Test
    fun fetchModels_whenServerReturnsEtag_reusesCachedModelsOnNotModified() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", "\"models-v1\"")
                .setBody(
                    """
                    {
                      "data": [
                        {"id": "model-a", "name": "Model A", "description": "a"}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "",
            isEnabled = true
        )

        val first = service.fetchModels(cfg)
        val second = service.fetchModels(cfg)

        assertEquals(listOf("model-a"), first.map { it.id })
        assertEquals(listOf("model-a"), second.map { it.id })

        val firstReq = server.takeRequest()
        val secondReq = server.takeRequest()
        assertTrue(firstReq.getHeader("If-None-Match").isNullOrBlank())
        assertEquals("\"models-v1\"", secondReq.getHeader("If-None-Match"))
    }

    @Test
    fun testConnection_whenModelListNotModified_stillFallsBackUsingCachedModels() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", "\"models-v1\"")
                .setBody(
                    """
                    {
                      "data": [
                        {"id": "slow-model", "name": "slow-model", "description": "rec"},
                        {"id": "backup-model", "name": "backup-model", "description": "alt"}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
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
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "",
            isEnabled = true
        )

        val cached = service.fetchModels(cfg)
        assertEquals(listOf("slow-model", "backup-model"), cached.map { it.id })

        val error = service.testConnection(cfg)

        assertNull(error)

        val warmupReq = server.takeRequest()
        val conditionalModelsReq = server.takeRequest()
        val firstTestReq = server.takeRequest()
        val fallbackReq = server.takeRequest()

        assertNotNull(warmupReq)
        assertEquals("\"models-v1\"", conditionalModelsReq.getHeader("If-None-Match"))
        assertTrue(firstTestReq.body.readUtf8().contains("\"model\":\"slow-model\""))
        assertTrue(fallbackReq.body.readUtf8().contains("\"model\":\"backup-model\""))
    }

    @Test
    fun testConnection_whenFixedModelTimesOut_retriesSameModelAndSucceeds() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "late"}}
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

        val timeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val service = AIService(
            client = OkHttpClient(),
            testClient = timeoutClient,
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertNull(error)
        val firstReq = server.takeRequest()
        val secondReq = server.takeRequest()
        assertTrue(firstReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertTrue(secondReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun testConnection_whenFixedModelReturns500_retriesSameModelAndSucceeds() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("upstream unavailable")
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
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertNull(error)
        val firstReq = server.takeRequest()
        val secondReq = server.takeRequest()
        assertTrue(firstReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertTrue(secondReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun testConnection_whenFixedModelTimeoutsTwice_returnsTimeoutMessage() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody("{}")
        )
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody("{}")
        )

        val timeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val service = AIService(
            client = OkHttpClient(),
            testClient = timeoutClient,
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertEquals("连接超时，请检查网络或稍后重试", error)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun chat_whenFixedModelFirstAttemptTimesOut_retriesSameModelAndSucceeds() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE)
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

        val timeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .callTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val service = AIService(
            client = timeoutClient,
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val result = service.chat(
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "hi")),
            config = cfg
        )

        assertEquals("ok", result)
        val firstReq = server.takeRequest()
        val secondReq = server.takeRequest()
        assertTrue(firstReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertTrue(secondReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun chat_whenFixedModelUnavailable_doesNotFallback() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = runCatching {
            service.chat(
                messages = listOf(ChatMessage(role = MessageRole.USER, content = "hi")),
                config = cfg
            )
        }.exceptionOrNull()

        assertTrue(error?.message?.contains("model_unavailable") == true)
        assertEquals(1, server.requestCount)
        val onlyReq = server.takeRequest()
        assertTrue(onlyReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
    }

    @Test
    fun chat_whenAutoPrimaryModelUnavailable_retriesWithAlternateModel() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        {"id": "slow-model", "name": "slow-model", "description": "rec"},
                        {"id": "backup-model", "name": "backup-model", "description": "alt"}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
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
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "",
            isEnabled = true
        )

        val result = service.chat(
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "hi")),
            config = cfg
        )

        assertEquals("ok", result)
        val modelsReq = server.takeRequest()
        val firstReq = server.takeRequest()
        val fallbackReq = server.takeRequest()
        assertTrue(modelsReq.path?.endsWith("/v1/models") == true || modelsReq.path?.endsWith("/models") == true)
        assertTrue(firstReq.body.readUtf8().contains("\"model\":\"slow-model\""))
        assertTrue(fallbackReq.body.readUtf8().contains("\"model\":\"backup-model\""))
    }

    @Test
    fun testConnection_whenAutoPrimaryModelUnavailable_retriesWithAlternateModel() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        {"id": "slow-model", "name": "slow-model", "description": "rec"},
                        {"id": "backup-model", "name": "backup-model", "description": "alt"}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
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
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertNull(error)

        val modelsReq = server.takeRequest()
        val firstTestReq = server.takeRequest()
        val secondTestReq = server.takeRequest()

        assertTrue(modelsReq.path?.endsWith("/v1/models") == true || modelsReq.path?.endsWith("/models") == true)
        assertTrue(firstTestReq.body.readUtf8().contains("\"model\":\"slow-model\""))
        assertTrue(secondTestReq.body.readUtf8().contains("\"model\":\"backup-model\""))
    }

    @Test
    fun testConnection_whenAutoPrimaryModelTimesOut_retriesSameModelBeforeAlternateModel() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        {"id": "slow-model", "name": "slow-model", "description": "rec"},
                        {"id": "backup-model", "name": "backup-model", "description": "alt"}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "late"}}
                      ]
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setBodyDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "still late"}}
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

        val timeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .writeTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val service = AIService(
            client = OkHttpClient(),
            testClient = timeoutClient,
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertNull(error)

        val modelsReq = server.takeRequest()
        val firstTestReq = server.takeRequest()
        val retryReq = server.takeRequest()
        val fallbackReq = server.takeRequest()

        assertTrue(modelsReq.path?.endsWith("/v1/models") == true || modelsReq.path?.endsWith("/models") == true)
        assertTrue(firstTestReq.body.readUtf8().contains("\"model\":\"slow-model\""))
        assertTrue(retryReq.body.readUtf8().contains("\"model\":\"slow-model\""))
        assertTrue(fallbackReq.body.readUtf8().contains("\"model\":\"backup-model\""))
    }

    @Test
    fun testConnection_whenFixedModelUnavailable_returnsSpecificGuidanceWithoutFallback() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertEquals("当前模型不可用，请切换模型或改用自动优选", error)
        val onlyReq = server.takeRequest()
        assertTrue(onlyReq.body.readUtf8().contains("\"model\":\"fixed-model\""))
        assertEquals(0, server.requestCount - 1)
    }

    @Test
    fun testConnection_whenServerReturns429_mapsToRateLimitMessage() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("too many requests")
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient(),
            modelPerformanceRepository = mockk(relaxed = true)
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "fixed-model",
            isEnabled = true
        )

        val error = service.testConnection(cfg)

        assertEquals("请求过于频繁，请稍后再试", error)
    }

    private fun ungzip(buffer: Buffer): String {
        return GZIPInputStream(buffer.inputStream()).bufferedReader().use { it.readText() }
    }
}
