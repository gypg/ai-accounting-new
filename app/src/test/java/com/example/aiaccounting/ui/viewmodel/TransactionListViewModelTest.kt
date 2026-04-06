package com.example.aiaccounting.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.logging.AppLogLogger
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    @MockK(relaxed = true)
    private lateinit var appLogLogger: AppLogLogger

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TransactionListViewModel
    private val currentMonthTimestamp = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { transactionRepository.getAllTransactions() } returns flowOf(
            listOf(
                Transaction(
                    id = 1,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.EXPENSE,
                    amount = 20.0,
                    date = currentMonthTimestamp,
                    aiSourceType = "AI_REMOTE",
                    aiTraceId = "trace-1"
                ),
                Transaction(
                    id = 2,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.EXPENSE,
                    amount = 30.0,
                    date = currentMonthTimestamp,
                    aiSourceType = "AI_LOCAL",
                    aiTraceId = "trace-2"
                ),
                Transaction(
                    id = 3,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.INCOME,
                    amount = 40.0,
                    date = currentMonthTimestamp,
                    aiSourceType = "MANUAL",
                    aiTraceId = null
                )
            )
        )
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())

        viewModel = TransactionListViewModel(transactionRepository, categoryRepository, appLogLogger)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filteredTransactions_filtersAiSources_whenSourceFilterChanges() = runTest {
        val job = backgroundScope.launch { viewModel.filteredTransactions.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSourceFilter("ai")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(1L, 2L), viewModel.filteredTransactions.value.map { it.id })

        viewModel.setSourceFilter("manual")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(3L), viewModel.filteredTransactions.value.map { it.id })

        viewModel.setSourceFilter("remote_ai")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.filteredTransactions.value.map { it.id })
        job.cancel()
    }

    @Test
    fun filteredTransactions_combinesSourceAndTypeFilters() = runTest {
        val job = backgroundScope.launch { viewModel.filteredTransactions.collect { } }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSourceFilter("ai")
        viewModel.setFilterType("income")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(emptyList<Long>(), viewModel.filteredTransactions.value.map { it.id })

        viewModel.setFilterType("expense")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(1L, 2L), viewModel.filteredTransactions.value.map { it.id })
        job.cancel()
    }
}
