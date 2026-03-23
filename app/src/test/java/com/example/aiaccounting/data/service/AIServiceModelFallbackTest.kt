package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

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
    fun chat_whenModelUnavailable_retriesWithRecommendedModel() = kotlinx.coroutines.test.runTest {
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
                      "data": [
                        {"id": "openai/gpt-oss-120b", "name": "gpt-oss-120b", "description": "rec"},
                        {"id": "some-other", "name": "other", "description": "alt"}
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
            testClient = OkHttpClient()
        )
        val cfg = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "k",
            apiUrl = server.url("/v1").toString().removeSuffix("/"),
            model = "bad-model",
            isEnabled = true
        )

        val result = service.chat(
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "hi")),
            config = cfg
        )

        assertEquals("ok", result)

        val firstReq = server.takeRequest()
        val modelsReq = server.takeRequest()
        val secondReq = server.takeRequest()

        val firstBody = firstReq.body.readUtf8()
        val secondBody = secondReq.body.readUtf8()

        assertTrue(firstBody.contains("\"model\":\"bad-model\""))
        assertTrue(secondBody.contains("\"model\":\"openai/gpt-oss-120b\""))
        assertTrue(modelsReq.path?.endsWith("/v1/models") == true || modelsReq.path?.endsWith("/models") == true)
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
            testClient = OkHttpClient()
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
    fun testConnection_whenAutoPrimaryModelTimesOut_retriesWithAlternateModel() = kotlinx.coroutines.test.runTest {
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
            testClient = timeoutClient
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
    fun testConnection_whenFixedModelUnavailable_returnsSpecificGuidanceWithoutFallback() = kotlinx.coroutines.test.runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
        )

        val service = AIService(
            client = OkHttpClient(),
            testClient = OkHttpClient()
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
            testClient = OkHttpClient()
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
}
