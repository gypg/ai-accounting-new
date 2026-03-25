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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsUserModelModeTest {

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

 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteBound() } returns flowOf(false)
 every { aiConfigRepository.getModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
 every { aiUsageRepository.getUsageStats() } returns flowOf(AIUsageStats())
 coEvery { networkUtils.isNetworkAvailable() } returns true

 // Additional flows needed by init block
 every { aiConfigRepository.getGatewayBaseUrl(any()) } returns flowOf("https://new.gateway.example")
 every { aiConfigRepository.getInviteApiBaseUrl() } returns flowOf("")
 every { aiConfigRepository.getInviteRpm() } returns flowOf(0)
 every { aiConfigRepository.getInviteCodeMasked() } returns flowOf("")
 every { aiConfigRepository.getPreferredNetworkRoute() } returns flowOf(null)
 every { modelPerformanceRepository.getRecommendation(any(), any()) } returns flowOf(null)
 }

 @After
 fun tearDown() {
 Dispatchers.resetMain()
 }

 @Test
 fun updateUserModelMode_persistsToRepository() = runTest {
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

 vm.updateUserModelMode(AIConfigRepository.ModelSelectionMode.FIXED)
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 1) { aiConfigRepository.setModelSelectionMode(AIConfigRepository.ModelSelectionMode.FIXED) }
 }

 @Test
 fun updateModel_whenUserModeAuto_switchesToFixed() = runTest {
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

 coVerify(exactly = 1) { aiConfigRepository.setModelSelectionMode(AIConfigRepository.ModelSelectionMode.FIXED) }
 }
}
