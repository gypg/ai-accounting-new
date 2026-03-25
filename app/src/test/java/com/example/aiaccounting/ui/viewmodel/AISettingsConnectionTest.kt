package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIModelPerformanceRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.InviteGatewayService
import com.example.aiaccounting.data.service.NetworkSpeedTestResult
import com.example.aiaccounting.data.service.NetworkSpeedTestService
import com.example.aiaccounting.data.service.NetworkSpeedTestSummary
import com.example.aiaccounting.data.service.NetworkSpeedTestTarget
import com.example.aiaccounting.data.service.NetworkSpeedTestClientType
import com.example.aiaccounting.data.service.PreferredNetworkRoute
import com.example.aiaccounting.utils.DeviceIdProvider
import com.example.aiaccounting.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsConnectionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var aiConfigRepository: AIConfigRepository
    private lateinit var aiService: AIService
    private lateinit var aiUsageRepository: AIUsageRepository
    private lateinit var modelPerformanceRepository: AIModelPerformanceRepository
    private lateinit var networkUtils: NetworkUtils
    private lateinit var inviteGatewayService: InviteGatewayService
    private lateinit var deviceIdProvider: DeviceIdProvider
    private lateinit var networkSpeedTestService: NetworkSpeedTestService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        aiConfigRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        aiUsageRepository = mockk(relaxed = true)
        modelPerformanceRepository = mockk(relaxed = true)
        networkUtils = mockk(relaxed = true)
        inviteGatewayService = mockk(relaxed = true)
        deviceIdProvider = mockk(relaxed = true)
        networkSpeedTestService = mockk(relaxed = true)

        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig(isEnabled = true, apiKey = "k", apiUrl = "https://example.com"))
        every { aiConfigRepository.getInviteBound() } returns flowOf(false)
        every { aiConfigRepository.getModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
        every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
        every { aiUsageRepository.getUsageStats() } returns flowOf(AIUsageStats())
        every { aiConfigRepository.getGatewayBaseUrl(any()) } returns flowOf("https://new.gateway.example")
        every { aiConfigRepository.getInviteApiBaseUrl() } returns flowOf("")
        every { aiConfigRepository.getInviteRpm() } returns flowOf(0)
        every { aiConfigRepository.getInviteCodeMasked() } returns flowOf("")
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(null)
        every { modelPerformanceRepository.getRecommendation(any(), any()) } returns flowOf(null)
        coEvery { networkUtils.isNetworkAvailable() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testConnection_whenAutoFallbackFails_setsErrorResultAndStopsTesting() = runTest {
        coEvery { aiService.testConnection(any()) } returns "自动优选暂时不可用，请稍后重试"

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.testConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isTesting)
        assertEquals(
            "自动优选暂时不可用，请稍后重试",
            (vm.uiState.value.testResult as TestResult.Error).message
        )
    }

    @Test
    fun runNetworkSpeedTest_whenSuccessful_persistsPreferredRoute() = runTest {
        val fastestTarget = NetworkSpeedTestTarget(
            id = "gateway",
            label = "邀请码网关",
            url = "https://gateway.example/bootstrap",
            clientType = NetworkSpeedTestClientType.GATEWAY
        )
        coEvery { networkSpeedTestService.test(any(), any()) } returns NetworkSpeedTestSummary(
            targets = listOf(
                NetworkSpeedTestResult.Success(
                    target = fastestTarget,
                    latencyMs = 123,
                    statusCode = 200
                )
            ),
            fastest = NetworkSpeedTestResult.Success(
                target = fastestTarget,
                latencyMs = 123,
                statusCode = 200
            ),
            errorMessage = null
        )
        coEvery { aiConfigRepository.savePreferredNetworkRoute(any()) } returns Unit

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.runNetworkSpeedTest()
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify {
            aiConfigRepository.savePreferredNetworkRoute(
                match {
                    it.targetId == "gateway" &&
                        it.label == "邀请码网关" &&
                        it.latencyMs == 123L
                }
            )
        }
    }

    @Test
    fun init_whenPreferredRouteExists_exposesItInUiState() = runTest {
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(
            PreferredNetworkRoute(
                targetId = "api",
                label = "API 节点",
                latencyMs = 88,
                updatedAtMillis = 123456L
            )
        )

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("API 节点", vm.uiState.value.preferredRouteSummary?.label)
        assertEquals(88L, vm.uiState.value.preferredRouteSummary?.latencyMs)
    }

    @Test
    fun updateApiUrl_clearsPreferredRouteSummary() = runTest {
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(
            PreferredNetworkRoute(
                targetId = "api",
                label = "API 节点",
                latencyMs = 66,
                updatedAtMillis = 999L
            )
        )
        coEvery { aiConfigRepository.clearPreferredNetworkRoute() } returns Unit

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateApiUrl("https://changed.example/v1")
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { aiConfigRepository.clearPreferredNetworkRoute() }
        assertNull(vm.uiState.value.preferredRouteSummary)
        assertNull(vm.uiState.value.networkSpeedTestResult)
    }

    @Test
    fun updateApiUrl_clearsModelRecommendationSummary() = runTest {
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(null)
        every {
            modelPerformanceRepository.getRecommendation(any(), any())
        } returns flowOf(
            com.example.aiaccounting.data.service.RecommendedModelSummary(
                modelId = "model-a",
                reason = "根据最近成功率与延迟推荐",
                source = com.example.aiaccounting.data.service.ModelRecommendationSource.PERFORMANCE,
                latencyMs = 120,
                updatedAtMillis = 1234L
            )
        )
        coEvery { modelPerformanceRepository.clearForConfig(any()) } returns Unit

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchRemoteModels()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateApiUrl("https://changed.example/v1")
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { modelPerformanceRepository.clearForConfig(any()) }
        assertNull(vm.uiState.value.recommendedModelSummary)
    }

    @Test
    fun init_whenPreferredRouteLatencyIsHigh_exposesNetworkWarning() = runTest {
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(
            PreferredNetworkRoute(
                targetId = "api",
                label = "API 节点",
                latencyMs = 880,
                updatedAtMillis = 123456L
            )
        )

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("推荐线路延迟偏高", vm.uiState.value.networkHealthWarning?.title)
        assertTrue(vm.uiState.value.networkHealthWarning?.message?.contains("880ms") == true)
    }

    @Test
    fun runNetworkSpeedTest_whenLatencyIsHigh_setsWarning() = runTest {
        val fastestTarget = NetworkSpeedTestTarget(
            id = "gateway",
            label = "邀请码网关",
            url = "https://gateway.example/bootstrap",
            clientType = NetworkSpeedTestClientType.GATEWAY
        )
        coEvery { networkSpeedTestService.test(any(), any()) } returns NetworkSpeedTestSummary(
            targets = listOf(
                NetworkSpeedTestResult.Success(
                    target = fastestTarget,
                    latencyMs = 920,
                    statusCode = 200
                )
            ),
            fastest = NetworkSpeedTestResult.Success(
                target = fastestTarget,
                latencyMs = 920,
                statusCode = 200
            ),
            errorMessage = null
        )

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.runNetworkSpeedTest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("网络延迟较高", vm.uiState.value.networkHealthWarning?.title)
        assertTrue(vm.uiState.value.networkHealthWarning?.message?.contains("920ms") == true)
    }

    @Test
    fun runNetworkSpeedTest_whenAllTargetsFail_setsErrorWarning() = runTest {
        val target = NetworkSpeedTestTarget(
            id = "gateway",
            label = "邀请码网关",
            url = "https://gateway.example/bootstrap",
            clientType = NetworkSpeedTestClientType.GATEWAY
        )
        coEvery { networkSpeedTestService.test(any(), any()) } returns NetworkSpeedTestSummary(
            targets = listOf(
                NetworkSpeedTestResult.Error(
                    target = target,
                    message = "连接超时",
                    failureType = com.example.aiaccounting.data.service.NetworkSpeedTestFailureType.TIMEOUT
                )
            ),
            fastest = null,
            errorMessage = "网络测速失败"
        )

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.runNetworkSpeedTest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("网络状态异常", vm.uiState.value.networkHealthWarning?.title)
        assertEquals("连接超时", vm.uiState.value.networkHealthWarning?.message)
    }

    @Test
    fun setGatewayBaseUrl_clearsPreferredRouteSummary() = runTest {
        every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(
            PreferredNetworkRoute(
                targetId = "gateway",
                label = "邀请码网关",
                latencyMs = 45,
                updatedAtMillis = 999L
            )
        )
        coEvery { aiConfigRepository.clearPreferredNetworkRoute() } returns Unit

        val vm = AISettingsViewModel(
            aiConfigRepository = aiConfigRepository,
            aiService = aiService,
            aiUsageRepository = aiUsageRepository,
            modelPerformanceRepository = modelPerformanceRepository,
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider,
            networkSpeedTestService = networkSpeedTestService
        )

        testDispatcher.scheduler.advanceUntilIdle()
        vm.setGatewayBaseUrl("https://changed.gateway.example")
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { aiConfigRepository.clearPreferredNetworkRoute() }
        assertNull(vm.uiState.value.preferredRouteSummary)
        assertNull(vm.uiState.value.networkSpeedTestResult)
        assertNull(vm.uiState.value.networkHealthWarning)
    }
}
