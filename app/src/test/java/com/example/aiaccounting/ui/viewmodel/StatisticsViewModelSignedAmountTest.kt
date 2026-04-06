package com.example.aiaccounting.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.ui.components.charts.MonthlyData
import com.example.aiaccounting.ui.components.charts.calculateTrendChartMaxValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class StatisticsViewModelSignedAmountTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var appLogLogger: AppLogLogger

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        appLogLogger = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun trendChartMaxValue_usesOnlyVisibleSeries() {
        val data = listOf(
            MonthlyData(month = "01月", income = 1200.0, expense = 12.0),
            MonthlyData(month = "02月", income = 800.0, expense = 18.0)
        )

        assertEquals(21.6, calculateTrendChartMaxValue(data, showIncome = false, showExpense = true), 0.001)
        assertEquals(1440.0, calculateTrendChartMaxValue(data, showIncome = true, showExpense = false), 0.001)
    }

    @Test
    fun statistics_treatsNegativeExpenseAmountsAsPositiveTotals() = runTest {
        val currentMonthTimestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        every { transactionRepository.getAllTransactions() } returns flowOf(
            listOf(
                Transaction(
                    id = 1,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.EXPENSE,
                    amount = -6.0,
                    date = currentMonthTimestamp
                ),
                Transaction(
                    id = 2,
                    accountId = 1,
                    categoryId = 2,
                    type = TransactionType.EXPENSE,
                    amount = -25.0,
                    date = currentMonthTimestamp
                ),
                Transaction(
                    id = 3,
                    accountId = 1,
                    categoryId = 3,
                    type = TransactionType.INCOME,
                    amount = 188.0,
                    date = currentMonthTimestamp
                )
            )
        )
        every { categoryRepository.getAllCategories() } returns flowOf(
            listOf(
                Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE),
                Category(id = 2, name = "娱乐", type = TransactionType.EXPENSE),
                Category(id = 3, name = "红包", type = TransactionType.INCOME)
            )
        )

        val viewModel = StatisticsViewModel(transactionRepository, categoryRepository, appLogLogger)
        val collectionJob = backgroundScope.launch { viewModel.statistics.collect { } }
        advanceUntilIdle()

        assertEquals(188.0, viewModel.statistics.value.totalIncome, 0.001)
        assertEquals(31.0, viewModel.statistics.value.totalExpense, 0.001)

        collectionJob.cancel()
    }

    @Test
    fun statistics_supports_day_level_filter_inside_selected_month() = runTest {
        val aprilSixMorning = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 6, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val aprilSixEvening = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 6, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val aprilSeven = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 7, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        every { transactionRepository.getAllTransactions() } returns flowOf(
            listOf(
                Transaction(
                    id = 1,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.EXPENSE,
                    amount = -12.0,
                    date = aprilSixMorning
                ),
                Transaction(
                    id = 2,
                    accountId = 1,
                    categoryId = 2,
                    type = TransactionType.INCOME,
                    amount = 200.0,
                    date = aprilSixEvening
                ),
                Transaction(
                    id = 3,
                    accountId = 1,
                    categoryId = 1,
                    type = TransactionType.EXPENSE,
                    amount = -30.0,
                    date = aprilSeven
                )
            )
        )
        every { categoryRepository.getAllCategories() } returns flowOf(
            listOf(
                Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE),
                Category(id = 2, name = "工资", type = TransactionType.INCOME)
            )
        )

        val viewModel = StatisticsViewModel(transactionRepository, categoryRepository, appLogLogger)
        val collectionJob = backgroundScope.launch { viewModel.statistics.collect { } }
        advanceUntilIdle()

        viewModel.setTimeFilter("2026-04")
        advanceUntilIdle()
        assertEquals(200.0, viewModel.statistics.value.totalIncome, 0.001)
        assertEquals(42.0, viewModel.statistics.value.totalExpense, 0.001)

        viewModel.setTimeFilter("2026-04-06")
        advanceUntilIdle()
        assertEquals(200.0, viewModel.statistics.value.totalIncome, 0.001)
        assertEquals(12.0, viewModel.statistics.value.totalExpense, 0.001)

        collectionJob.cancel()
    }
}
