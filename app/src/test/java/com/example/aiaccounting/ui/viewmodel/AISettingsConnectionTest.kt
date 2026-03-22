package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.InviteGatewayService
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsConnectionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var aiConfigRepository: AIConfigRepository
    private lateinit var aiService: AIService
    private lateinit var aiUsageRepository: AIUsageRepository
    private lateinit var networkUtils: NetworkUtils
    private lateinit var inviteGatewayService: InviteGatewayService
    private lateinit var deviceIdProvider: DeviceIdProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        aiConfigRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        aiUsageRepository = mockk(relaxed = true)
        networkUtils = mockk(relaxed = true)
        inviteGatewayService = mockk(relaxed = true)
        deviceIdProvider = mockk(relaxed = true)

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
            networkUtils = networkUtils,
            inviteGatewayService = inviteGatewayService,
            deviceIdProvider = deviceIdProvider
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
}
