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
import org.junit.Before
import org.junit.Test

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
        // 1st call: chat completion returns model unavailable
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("model not found")
        )

        // 2nd call: models list
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

        // 3rd call: chat completion success
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

        // Verify request bodies used different models
        val firstReq = server.takeRequest()
        val modelsReq = server.takeRequest()
        val secondReq = server.takeRequest()

        val firstBody = firstReq.body.readUtf8()
        val secondBody = secondReq.body.readUtf8()

        // First attempt uses the configured model
        assert(firstBody.contains("\"model\":\"bad-model\""))
        // Second attempt uses the recommended model (picked from /v1/models)
        assert(secondBody.contains("\"model\":\"openai/gpt-oss-120b\""))

        // Ensure the intermediate request was to /v1/models
        assert(modelsReq.path?.endsWith("/v1/models") == true || modelsReq.path?.endsWith("/models") == true)
    }
}
