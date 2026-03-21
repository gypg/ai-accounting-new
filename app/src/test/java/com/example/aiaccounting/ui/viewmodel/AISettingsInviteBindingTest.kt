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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsInviteBindingTest {

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

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteBound() } returns flowOf(false)
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)
 every { aiConfigRepository.getModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.FIXED)
 every { aiUsageRepository.getUsageStats() } returns flowOf(AIUsageStats())
 coEvery { networkUtils.isNetworkAvailable() } returns true

 // Additional flows needed by init block
 every { aiConfigRepository.getGatewayBaseUrl(any()) } returns flowOf("https://new.gateway.example")
 every { aiConfigRepository.getInviteApiBaseUrl() } returns flowOf("")
 every { aiConfigRepository.getInviteRpm() } returns flowOf(0)
 every { aiConfigRepository.getInviteCodeMasked() } returns flowOf("")
 }

 @After
 fun tearDown() {
 Dispatchers.resetMain()
 }

 @Test
 fun bindInviteCode_whenNetworkUnavailable_setsErrorAndDoesNotCallBootstrap() = runTest {
 coEvery { networkUtils.isNetworkAvailable() } returns false

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.bindInviteCode(inviteCode = "inv_test", gatewayBaseUrl = "https://api.gdmon.dpdns.org")
 testDispatcher.scheduler.advanceUntilIdle()

 assertFalse(vm.uiState.value.isBindingInvite)
 val result = vm.uiState.value.inviteBindResult
 assertNotNull(result)
 assertEquals(
 "网络不可用，请检查网络连接",
 (result as InviteBindResult.Error).message
 )

 coVerify(exactly = 0) { inviteGatewayService.bootstrap(any(), any(), any()) }
 coVerify(exactly = 0) { aiConfigRepository.saveInviteBinding(any(), any(), any(), any(), any()) }
 }

 @Test
 fun bindInviteCode_whenInviteCodeBlank_setsError() = runTest {
 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.bindInviteCode(inviteCode = " ", gatewayBaseUrl = "https://api.gdmon.dpdns.org")
 testDispatcher.scheduler.advanceUntilIdle()

 assertEquals(
 "请输入邀请码",
 (vm.uiState.value.inviteBindResult as InviteBindResult.Error).message
 )
 coVerify(exactly = 0) { inviteGatewayService.bootstrap(any(), any(), any()) }
 }

 @Test
 fun bindInviteCode_whenBootstrapSuccess_savesConfigAndMarksInviteBound() = runTest {
 every { deviceIdProvider.getStableDeviceId() } returns "android_id_123"
 coEvery {
 inviteGatewayService.bootstrap(
 inviteCode = "inv_ok",
 deviceId = "android_id_123",
 gatewayBaseUrl = any()
 )
 } returns InviteGatewayService.BootstrapResult.Success(
 token = "tok_xxx",
 apiBaseUrl = "https://api.gdmon.dpdns.org/v1",
 rpm = 60
 )

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.bindInviteCode(inviteCode = "inv_ok", gatewayBaseUrl = "https://api.gdmon.dpdns.org")
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 1) {
 aiConfigRepository.setInviteModelSelectionMode(AIConfigRepository.ModelSelectionMode.AUTO)
 }
 coVerify(exactly = 1) {
 aiConfigRepository.saveInviteBinding(
 inviteCode = "inv_ok",
 token = "tok_xxx",
 apiBaseUrl = "https://api.gdmon.dpdns.org/v1",
 model = "openai/gpt-oss-120b",
 rpm = 60
 )
 }
 // saveInviteBinding already implies inviteBound=true; ensure legacy marker is not called

 val result = vm.uiState.value.inviteBindResult
 assertNotNull(result)
 assertEquals(60, (result as InviteBindResult.Success).rpm)
 assertEquals("https://api.gdmon.dpdns.org/v1", result.apiBaseUrl)

 assertFalse(vm.uiState.value.isBindingInvite)
 // Should be true immediately to avoid accidentally persisting invite token into user config
 assertEquals(true, vm.uiState.value.inviteBound)
 }

 @Test
 fun bindInviteCode_success_immediatelyDisablesSaveConfig() = runTest {
 every { deviceIdProvider.getStableDeviceId() } returns "android_id_123"
 coEvery { inviteGatewayService.bootstrap(any(), any(), any()) } returns InviteGatewayService.BootstrapResult.Success(
 token = "tok_xxx",
 apiBaseUrl = "https://api.gdmon.dpdns.org/v1",
 rpm = 60
 )

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.bindInviteCode(inviteCode = "inv_ok", gatewayBaseUrl = "https://api.gdmon.dpdns.org")
 testDispatcher.scheduler.advanceUntilIdle()

 vm.saveConfig()
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.saveAIConfig(any()) }
 }

 @Test
 fun bindInviteCode_whenGatewayReturnsError_setsFriendlyMessageAndDoesNotSave() = runTest {
 every { deviceIdProvider.getStableDeviceId() } returns "android_id_123"
 coEvery { inviteGatewayService.bootstrap(any(), any(), any()) } returns
 InviteGatewayService.BootstrapResult.ApiError(code = "invite_already_used")

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.bindInviteCode(inviteCode = "inv_used", gatewayBaseUrl = "https://api.gdmon.dpdns.org")
 testDispatcher.scheduler.advanceUntilIdle()

 assertEquals(
 "邀请码已被其他设备使用",
 (vm.uiState.value.inviteBindResult as InviteBindResult.Error).message
 )
 coVerify(exactly = 0) { aiConfigRepository.saveInviteBinding(any(), any(), any(), any(), any()) }
 }

 @Test
 fun updateModel_whenInviteBound_doesNotClearInviteBinding() = runTest {
 // Simulate invite bound state via flow
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.updateModel("some-model")
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }
 }

 @Test
 fun updateProvider_whenInviteBound_doesNotClearInviteBinding() = runTest {
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.updateProvider(com.example.aiaccounting.data.model.AIProvider.CUSTOM)
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }
 }

 @Test
 fun updateApiKey_whenInviteBound_doesNotClearInviteBinding() = runTest {
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.updateApiKey("k")
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }
 }

 @Test
 fun updateApiUrl_whenInviteBound_doesNotClearInviteBinding() = runTest {
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.updateApiUrl("https://example.com")
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }
 }

 @Test
 fun updateInviteModel_persistsInviteModel_withoutClearingBinding() = runTest {
 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.updateInviteModel("openai/gpt-oss-120b")
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 1) { aiConfigRepository.updateInviteModel("openai/gpt-oss-120b") }
 coVerify(exactly = 0) { aiConfigRepository.clearInviteBinding() }
 }


 @Test
 fun saveConfig_whenInviteBound_doesNotPersistUserConfig() = runTest {
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.saveConfig()
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 0) { aiConfigRepository.saveAIConfig(any()) }
 }

 @Test
 fun saveConfig_whenInviteBoundAndBuiltinEnabled_persistsUserConfig() = runTest {
 every { aiConfigRepository.getInviteBound() } returns flowOf(true)
 every { aiConfigRepository.getUseBuiltin() } returns flowOf(true)

 // avoid init collectors interfering with tests
 every { aiConfigRepository.getAIConfig() } returns flowOf(AIConfig())
 every { aiConfigRepository.getInviteModelSelectionMode() } returns flowOf(AIConfigRepository.ModelSelectionMode.AUTO)

 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )
 testDispatcher.scheduler.advanceUntilIdle()

 vm.saveConfig()
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 1) { aiConfigRepository.saveAIConfig(any()) }
 }

 @Test
 fun clearInviteBinding_callsRepositoryClear() = runTest {
 val vm = AISettingsViewModel(
 aiConfigRepository = aiConfigRepository,
 aiService = aiService,
 aiUsageRepository = aiUsageRepository,
 networkUtils = networkUtils,
 inviteGatewayService = inviteGatewayService,
 deviceIdProvider = deviceIdProvider
 )

 vm.clearInviteBinding()
 testDispatcher.scheduler.advanceUntilIdle()

 coVerify(exactly = 1) { aiConfigRepository.clearInviteBinding() }
 }
}
