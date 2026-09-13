package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIModelPerformanceRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.InviteGatewayService
import com.example.aiaccounting.utils.DeviceIdProvider
import com.example.aiaccounting.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsBuiltinInviteDecoupleTest {

 private val testDispatcher = StandardTestDispatcher()

 private lateinit var aiConfigRepository: AIConfigRepository
 private lateinit var aiService: AIService
 private lateinit var aiUsageRepository: AIUsageRepository
 private lateinit var modelPerformanceRepository: AIModelPerformanceRepository
 private lateinit var networkUtils: NetworkUtils
 private lateinit var inviteGatewayService: InviteGatewayService
 private lateinit var deviceIdProvider: DeviceIdProvider

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

 // Simulate already invite-bound state; builtin is off initially.
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)
 every { aiConfigRepository.getAIConfig() } returns flowOf(
 AIConfig(
 provider = com.example.aiaccounting.data.model.AIProvider.CUSTOM,
 apiKey = "tok_invite",
 apiUrl = "https://api.gdmon.dpdns.org/v1",
 model = "",
 isEnabled = true
 )
 )
 every { aiConfigRepository.getModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.FIXED)
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
 every { aiUsageRepository.getUsageStats() } returns flowOf(AIUsageStats())
 coEvery { networkUtils.isNetworkAvailable() } returns true

 // Additional flows needed by init block
 every { aiConfigRepository.getGatewayBaseUrl(any()) } returns flowOf("https://new.gateway.example")
 every { aiConfigRepository.getInviteApiBaseUrl() } returns flowOf("https://new.gateway.example/v1")
 every { aiConfigRepository.getInviteRpm() } returns flowOf(60)
 every { aiConfigRepository.getInviteCodeMasked() } returns flowOf("inv_****test")
 every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(null)
 every { modelPerformanceRepository.getRecommendation(any(), any()) } returns flowOf(null)
 }

 @After
 fun tearDown() {
 Dispatchers.resetMain()
 }

 @Test
 fun toggleBuiltinConfig_off_doesNotClearInviteBound() = runTest {
 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 modelPerformanceRepository = modelPerformanceRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 testDispatcher.scheduler.advanceUntilIdle()

 // Turn builtin on then off.
 vm.toggleBuiltinConfig(true)
 testDispatcher.scheduler.advanceUntilIdle()
 vm.toggleBuiltinConfig(false)
 testDispatcher.scheduler.advanceUntilIdle()

 // Repository should be asked to flip builtin flag, but invite binding should not be cleared.
 coVerify(exactly = 1) { aiConfigRepository.setUseBuiltin(true) }
 coVerify(exactly = 1) { aiConfigRepository.setUseBuiltin(false) }
 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }

 // UI should still consider invite bound after toggling off.
 assertEquals(true, vm.uiState.value.inviteBound)
 }

 @Test
 fun updateModel_whenInviteBound_doesNotSwitchUserModeToFixed() = runTest {
 // Make user mode AUTO to ensure the guard is effective.
 every { aiConfigRepository.getModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 modelPerformanceRepository = modelPerformanceRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 testDispatcher.scheduler.advanceUntilIdle()

 vm.updateModel("openai/gpt-oss-120b")
 testDispatcher.scheduler.advanceUntilIdle()

 // Should not persist user mode changes when invite bound.
 coVerify(exactly = 0) { aiConfigRepository.setModelSelectionMode(AIConfigRepository.ModelSelectionMode.FIXED) }
 }
}
