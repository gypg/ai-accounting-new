package com.example.aiaccounting.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.aiaccounting.data.exporter.LogCsvExporter
import com.example.aiaccounting.data.local.entity.AppLogEntry
import com.example.aiaccounting.data.repository.AppLogRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LogBrowserViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var appLogRepository: AppLogRepository

    @MockK
    private lateinit var logCsvExporter: LogCsvExporter

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LogBrowserViewModel

    @org.junit.Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { appLogRepository.getRecentLogs(any()) } returns flowOf(
            listOf(
                AppLogEntry(
                    id = "1",
                    level = "INFO",
                    source = "AI",
                    category = "ai_bookkeeping",
                    message = "AI 添加交易成功",
                    details = "amount=25"
                ),
                AppLogEntry(
                    id = "2",
                    level = "ERROR",
                    source = "AI",
                    category = "ai_bookkeeping",
                    message = "AI 添加交易失败",
                    details = "保存失败"
                )
            )
        )

        viewModel = LogBrowserViewModel(appLogRepository, logCsvExporter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filteredLogs_filtersByStatus() = runTest {
        val job = backgroundScope.launch { viewModel.filteredLogs.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatusFilter("error")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), viewModel.filteredLogs.value.map { it.id })
        job.cancel()
    }

    @Test
    fun filteredLogs_filtersByQuery() = runTest {
        val job = backgroundScope.launch { viewModel.filteredLogs.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setQuery("失败")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), viewModel.filteredLogs.value.map { it.id })
        job.cancel()
    }

    @Test
    fun exportFilteredLogs_exportsCurrentFilteredList() = runTest {
        val exportFile = File("logs.csv")
        coEvery { logCsvExporter.exportLogs(any(), any()) } returns Result.success(exportFile)

        val job = backgroundScope.launch { viewModel.filteredLogs.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setStatusFilter("success")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.exportFilteredLogs()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(exportFile, viewModel.uiState.value.lastExportFile)
        job.cancel()
    }
}
