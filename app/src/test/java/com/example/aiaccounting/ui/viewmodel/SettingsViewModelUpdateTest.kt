package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.data.local.prefs.LogAutoClearPreferences
import com.example.aiaccounting.data.service.AppUpdateService
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.security.SecurityManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelUpdateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var securityManager: SecurityManager
    private lateinit var appStateManager: AppStateManager
    private lateinit var appLogLogger: AppLogLogger
    private lateinit var appUpdateService: AppUpdateService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        securityManager = mockk(relaxed = true)
        appStateManager = mockk(relaxed = true)
        appLogLogger = mockk(relaxed = true)
        appUpdateService = mockk(relaxed = true)

        every { securityManager.isBiometricEnabled() } returns false
        every { securityManager.getPinState() } returns SecurityManager.PinState.NotSet
        every { appStateManager.getLogAutoClearPreferences() } returns LogAutoClearPreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkForUpdates_whenUpdateAvailable_setsDialogState() = runTest {
        val release = AppUpdateService.AppReleaseInfo(
            tagName = "v1.8.4",
            versionName = "1.8.4",
            releaseName = "v1.8.4",
            body = "修复问题",
            htmlUrl = "https://github.com/gypg/ai-accounting-new/releases/tag/v1.8.4",
            publishedAt = "2026-04-07T10:00:00Z",
            apkAssetName = null,
            apkDownloadUrl = null,
            isPrerelease = false
        )
        coEvery { appUpdateService.checkLatestRelease(any()) } returns AppUpdateService.ReleaseCheckResult.Success(release)

        val vm = SettingsViewModel(securityManager, appStateManager, appLogLogger, appUpdateService)
        advanceUntilIdle()

        vm.checkForUpdates()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingUpdate)
        assertTrue(vm.uiState.value.updateDialogState is UpdateDialogState.UpdateAvailable)
    }

    @Test
    fun checkForUpdates_whenUpToDate_setsUpToDateDialog() = runTest {
        coEvery { appUpdateService.checkLatestRelease(any()) } returns AppUpdateService.ReleaseCheckResult.UpToDate

        val vm = SettingsViewModel(securityManager, appStateManager, appLogLogger, appUpdateService)
        advanceUntilIdle()

        vm.checkForUpdates()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingUpdate)
        assertTrue(vm.uiState.value.updateDialogState is UpdateDialogState.UpToDate)
    }

    @Test
    fun checkForUpdates_whenFailure_setsErrorDialog() = runTest {
        coEvery { appUpdateService.checkLatestRelease(any()) } returns AppUpdateService.ReleaseCheckResult.NetworkError("网络错误")

        val vm = SettingsViewModel(securityManager, appStateManager, appLogLogger, appUpdateService)
        advanceUntilIdle()

        vm.checkForUpdates()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingUpdate)
        val dialogState = vm.uiState.value.updateDialogState as UpdateDialogState.Error
        assertEquals("网络错误", dialogState.message)
    }
}
