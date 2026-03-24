package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class NetworkSpeedTestServiceTest {

    private lateinit var apiServer: MockWebServer
    private lateinit var gatewayServer: MockWebServer

    @Before
    fun setUp() {
        apiServer = MockWebServer()
        gatewayServer = MockWebServer()
        apiServer.start()
        gatewayServer.start()
    }

    @After
    fun tearDown() {
        apiServer.shutdown()
        gatewayServer.shutdown()
    }

    @Test
    fun test_returnsFastestSuccessfulTarget() = kotlinx.coroutines.test.runTest {
        apiServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        gatewayServer.enqueue(MockResponse().setResponseCode(404))

        val service = NetworkSpeedTestService(
            aiTestClient = OkHttpClient(),
            gatewayClient = OkHttpClient()
        )

        val summary = service.test(
            config = AIConfig(apiUrl = apiServer.url("/v1").toString(), isEnabled = true),
            gatewayBaseUrl = gatewayServer.url("/").toString()
        )

        assertEquals(2, summary.targets.size)
        assertNotNull(summary.fastest)
        assertTrue(summary.fastest!!.target.label in listOf("API 节点", "邀请码网关"))
    }

    @Test
    fun test_returnsErrorWhenAllTargetsFail() = kotlinx.coroutines.test.runTest {
        val timeoutClient = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.MILLISECONDS)
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .writeTimeout(100, TimeUnit.MILLISECONDS)
            .build()

        apiServer.shutdown()
        gatewayServer.shutdown()

        val service = NetworkSpeedTestService(
            aiTestClient = timeoutClient,
            gatewayClient = timeoutClient
        )

        val summary = service.test(
            config = AIConfig(apiUrl = "https://non-existent.invalid/v1", isEnabled = true),
            gatewayBaseUrl = "https://another-invalid.invalid"
        )

        assertEquals(null, summary.fastest)
        assertNotNull(summary.errorMessage)
    }

    @Test
    fun test_returnsInvalidUrlErrorInsteadOfThrowing() = kotlinx.coroutines.test.runTest {
        val service = NetworkSpeedTestService(
            aiTestClient = OkHttpClient(),
            gatewayClient = OkHttpClient()
        )

        val summary = service.test(
            config = AIConfig(apiUrl = "not-a-url", isEnabled = true),
            gatewayBaseUrl = "bad:// url"
        )

        assertEquals(null, summary.fastest)
        assertNotNull(summary.errorMessage)
        assertTrue(summary.targets.all { it is NetworkSpeedTestResult.Error })
    }
}
