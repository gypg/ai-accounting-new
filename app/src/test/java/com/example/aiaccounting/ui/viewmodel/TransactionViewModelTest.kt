package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.AIOperationTraceRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TagRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.widget.WidgetUpdateService
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TransactionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @MockK
    private lateinit var accountRepository: AccountRepository

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    @MockK
    private lateinit var widgetUpdateService: WidgetUpdateService

    @MockK
    private lateinit var tagRepository: TagRepository

    @MockK
    private lateinit var aiOperationTraceRepository: AIOperationTraceRepository

    private lateinit var viewModel: TransactionViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)

        // Setup default mock behavior
        every { transactionRepository.getAllTransactions() } returns flowOf(emptyList())
        every { transactionRepository.getRecentTransactions(any()) } returns flowOf(emptyList())
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        every { tagRepository.getAllTags() } returns flowOf(emptyList())
        every { aiOperationTraceRepository.getTracesByTraceId(any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.getCurrentMonthRange() } returns Pair(0L, 9999999999L)
        coEvery { transactionRepository.getTotalIncome(any(), any()) } returns 1000.0
        coEvery { transactionRepository.getTotalExpense(any(), any()) } returns 500.0
        coEvery { widgetUpdateService.updateWidgetStats(any()) } just Runs

        viewModel = TransactionViewModel(
            transactionRepository,
            accountRepository,
            categoryRepository,
            tagRepository,
            aiOperationTraceRepository,
            widgetUpdateService,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() = runTest {
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<Transaction>(), initialState.transactions)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertFalse(initialState.showAddDialog)
        assertFalse(initialState.showEditDialog)
        assertNull(initialState.editingTransaction)
    }

    @Test
    fun `createTransaction should call repository insert`() = runTest {
        // Given
        val transactionId = 1L
        coEvery { transactionRepository.insertTransaction(any()) } returns transactionId

        // When
        viewModel.createTransaction(
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis(),
            note = "Test transaction"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `createTransaction should update loading state`() = runTest {
        // Given
        coEvery { transactionRepository.insertTransaction(any()) } returns 1L

        // When - Start creation
        viewModel.createTransaction(
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis(),
            note = "Test"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `updateTransaction should call repository update`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionRepository.updateTransaction(any()) } just Runs

        // When
        viewModel.updateTransaction(transaction)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { transactionRepository.updateTransaction(transaction) }
    }

    @Test
    fun `deleteTransaction should call repository delete`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionRepository.deleteTransaction(any()) } just Runs

        // When
        viewModel.deleteTransaction(transaction)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { transactionRepository.deleteTransaction(transaction) }
    }

    @Test
    fun `showAddDialog should update state`() {
        // When
        viewModel.showAddDialog()

        // Then
        assertTrue(viewModel.uiState.value.showAddDialog)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `hideAddDialog should update state`() {
        // Given
        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)

        // When
        viewModel.hideAddDialog()

        // Then
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `showEditDialog should update state with transaction`() {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )

        // When
        viewModel.showEditDialog(transaction)

        // Then
        assertTrue(viewModel.uiState.value.showEditDialog)
        assertEquals(transaction, viewModel.uiState.value.editingTransaction)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `hideEditDialog should update state`() {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        viewModel.showEditDialog(transaction)

        // When
        viewModel.hideEditDialog()

        // Then
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingTransaction)
    }

    @Test
    fun `clearError should set error to null`() {
        // Given - Set error manually
        viewModel.showAddDialog()
        viewModel.hideAddDialog()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `getTransactionById should return transaction from repository`() = runTest {
        // Given
        val transactionId = 1L
        val expectedTransaction = Transaction(
            id = transactionId,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionRepository.getTransactionById(transactionId) } returns expectedTransaction

        // When
        val result = viewModel.getTransactionById(transactionId)

        // Then
        assertEquals(expectedTransaction, result)
        coVerify { transactionRepository.getTransactionById(transactionId) }
    }

    @Test
    fun `getTransactionById should return null when not found`() = runTest {
        // Given
        val transactionId = 999L
        coEvery { transactionRepository.getTransactionById(transactionId) } returns null

        // When
        val result = viewModel.getTransactionById(transactionId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getMonthSummary should return correct summary`() {
        // Given - Set up state with known values
        val year = 2024
        val month = 1
        every { transactionRepository.getMonthRange(year, month) } returns Pair(0L, 9999999999L)

        // When
        val summary = viewModel.getMonthSummary(year, month)

        // Then
        assertNotNull(summary)
        assertEquals(viewModel.uiState.value.monthIncome, summary.income, 0.01)
        assertEquals(viewModel.uiState.value.monthExpense, summary.expense, 0.01)
        assertEquals(viewModel.uiState.value.monthBalance, summary.balance, 0.01)
    }

    @Test
    fun `getYearSummary should return correct year`() {
        // Given
        val year = 2024
        every { transactionRepository.getYearRange(year) } returns Pair(0L, 9999999999L)

        // When
        val summary = viewModel.getYearSummary(year)

        // Then
        assertEquals(year, summary.year)
        assertNotNull(summary)
    }

    @Test
    fun `addTransaction should delegate to createTransaction`() = runTest {
        // Given
        val amount = 100.0
        val type = TransactionType.EXPENSE
        val accountId = 1L
        val categoryId = 1L
        val date = java.util.Date()
        val note = "Test note"

        coEvery { transactionRepository.insertTransaction(any()) } returns 1L

        // When
        viewModel.addTransaction(amount, type, accountId, categoryId, date, note)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { transactionRepository.insertTransaction(any()) }
        coVerify { widgetUpdateService.updateWidgetStats(context) }
    }

    @Test
    fun `deleteTransaction by id should call repository`() = runTest {
        // Given
        val transactionId = 1L
        val transaction = Transaction(
            id = transactionId,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionRepository.getTransactionById(transactionId) } returns transaction
        coEvery { transactionRepository.deleteTransaction(any()) } just Runs

        // When
        viewModel.deleteTransaction(transactionId)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { transactionRepository.getTransactionById(transactionId) }
        coVerify { transactionRepository.deleteTransaction(transaction) }
    }

    @Test
    fun `searchTransactions should return flow from repository`() {
        // Given
        val query = "test"
        val searchFlow: Flow<List<Transaction>> = flowOf(emptyList())
        every { transactionRepository.searchTransactions(query) } returns searchFlow

        // When
        val result = viewModel.searchTransactions(query)

        // Then
        assertEquals(searchFlow, result)
    }

    @Test
    fun `loadTraceDetails should populate trace details when repository returns records`() = runTest {
        val traceId = "trace-123"
        val traces = listOf(
            AIOperationTrace(
                id = "1",
                traceId = traceId,
                sourceType = "AI_REMOTE",
                actionType = "ADD_TRANSACTION",
                entityType = "TRANSACTION",
                summary = "已创建交易"
            )
        )
        every { aiOperationTraceRepository.getTracesByTraceId(traceId) } returns flowOf(traces)

        viewModel.loadTraceDetails(traceId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTraceLoading)
        assertEquals(traceId, viewModel.uiState.value.activeTraceId)
        assertEquals(traces, viewModel.uiState.value.traceDetails)
        assertNull(viewModel.uiState.value.traceError)
        verify { aiOperationTraceRepository.getTracesByTraceId(traceId) }
    }

    @Test
    fun `loadTraceDetails should expose empty state when no records found`() = runTest {
        val traceId = "trace-empty"
        every { aiOperationTraceRepository.getTracesByTraceId(traceId) } returns flowOf(emptyList())

        viewModel.loadTraceDetails(traceId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTraceLoading)
        assertEquals(traceId, viewModel.uiState.value.activeTraceId)
        assertTrue(viewModel.uiState.value.traceDetails.isEmpty())
        assertEquals("未找到留痕记录", viewModel.uiState.value.traceError)
    }

    @Test
    fun `clearTraceDetails should reset trace state`() = runTest {
        val traceId = "trace-reset"
        val traces = listOf(
            AIOperationTrace(
                id = "1",
                traceId = traceId,
                sourceType = "AI_LOCAL",
                actionType = "CREATE_CATEGORY",
                entityType = "CATEGORY",
                summary = "已创建分类"
            )
        )
        every { aiOperationTraceRepository.getTracesByTraceId(traceId) } returns flowOf(traces)
        viewModel.loadTraceDetails(traceId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearTraceDetails()

        assertFalse(viewModel.uiState.value.isTraceLoading)
        assertNull(viewModel.uiState.value.activeTraceId)
        assertTrue(viewModel.uiState.value.traceDetails.isEmpty())
        assertNull(viewModel.uiState.value.traceError)
    }
}
