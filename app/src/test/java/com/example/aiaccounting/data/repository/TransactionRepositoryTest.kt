package com.example.aiaccounting.data.repository

import android.content.Context
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class TransactionRepositoryTest {

    @MockK
    private lateinit var transactionDao: TransactionDao

    private lateinit var repository: TransactionRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = mockk(relaxed = true)
        repository = TransactionRepository(transactionDao, context)
    }

    @Test
    fun `getAllTransactions should return flow from dao`() = runTest {
        // Given
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getAllTransactions() } returns flowOf(transactions)

        // When
        val result = repository.getAllTransactions().first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.getAllTransactions() }
    }

    @Test
    fun `getAllTransactionsList should return list from dao`() = runTest {
        // Given
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getAllTransactions() } returns flowOf(transactions)

        // When
        val result = repository.getAllTransactionsList()

        // Then
        assertEquals(transactions, result)
    }

    @Test
    fun `getTransactionById should return transaction from dao`() = runTest {
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
        coEvery { transactionDao.getTransactionById(transactionId) } returns expectedTransaction

        // When
        val result = repository.getTransactionById(transactionId)

        // Then
        assertEquals(expectedTransaction, result)
        coVerify { transactionDao.getTransactionById(transactionId) }
    }

    @Test
    fun `getTransactionById should return null when not found`() = runTest {
        // Given
        val transactionId = 999L
        coEvery { transactionDao.getTransactionById(transactionId) } returns null

        // When
        val result = repository.getTransactionById(transactionId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getTransactionsByAccount should return flow from dao`() = runTest {
        // Given
        val accountId = 1L
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = accountId,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getTransactionsByAccount(accountId) } returns flowOf(transactions)

        // When
        val result = repository.getTransactionsByAccount(accountId).first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.getTransactionsByAccount(accountId) }
    }

    @Test
    fun `getTransactionsByCategory should return flow from dao`() = runTest {
        // Given
        val categoryId = 1L
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = categoryId,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getTransactionsByCategory(categoryId) } returns flowOf(transactions)

        // When
        val result = repository.getTransactionsByCategory(categoryId).first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.getTransactionsByCategory(categoryId) }
    }

    @Test
    fun `getTransactionsByType should return flow from dao`() = runTest {
        // Given
        val type = TransactionType.INCOME
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = type,
                amount = 1000.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getTransactionsByType(type) } returns flowOf(transactions)

        // When
        val result = repository.getTransactionsByType(type).first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.getTransactionsByType(type) }
    }

    @Test
    fun `getTransactionsByDateRange should return flow from dao`() = runTest {
        // Given
        val startDate = 0L
        val endDate = 9999999999L
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = 1000L
            )
        )
        every { transactionDao.getTransactionsByDateRange(startDate, endDate) } returns flowOf(transactions)

        // When
        val result = repository.getTransactionsByDateRange(startDate, endDate).first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.getTransactionsByDateRange(startDate, endDate) }
    }

    @Test
    fun `insertTransaction should return id from dao`() = runTest {
        // Given
        val transaction = Transaction(
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        val expectedId = 1L
        coEvery { transactionDao.insertTransaction(transaction) } returns expectedId

        // When
        val result = repository.insertTransaction(transaction)

        // Then
        assertEquals(expectedId, result)
        coVerify { transactionDao.insertTransaction(transaction) }
    }

    @Test
    fun `insertTransactions should call dao`() = runTest {
        // Given
        val transactions = listOf(
            Transaction(
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        coEvery { transactionDao.insertTransactions(transactions) } just Runs

        // When
        repository.insertTransactions(transactions)

        // Then
        coVerify { transactionDao.insertTransactions(transactions) }
    }

    @Test
    fun `updateTransaction should call dao with updated timestamp`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionDao.updateTransaction(any()) } just Runs

        // When
        repository.updateTransaction(transaction)

        // Then
        coVerify { transactionDao.updateTransaction(any()) }
    }

    @Test
    fun `deleteTransaction should call dao`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            accountId = 1L,
            categoryId = 1L,
            type = TransactionType.EXPENSE,
            amount = 100.0,
            date = System.currentTimeMillis()
        )
        coEvery { transactionDao.deleteTransaction(transaction) } just Runs

        // When
        repository.deleteTransaction(transaction)

        // Then
        coVerify { transactionDao.deleteTransaction(transaction) }
    }

    @Test
    fun `deleteTransactionById should call dao`() = runTest {
        // Given
        val transactionId = 1L
        coEvery { transactionDao.deleteTransactionById(transactionId) } just Runs

        // When
        repository.deleteTransactionById(transactionId)

        // Then
        coVerify { transactionDao.deleteTransactionById(transactionId) }
    }

    @Test
    fun `getTotalIncome should return amount from dao`() = runTest {
        // Given
        val startDate = 0L
        val endDate = 9999999999L
        val expectedAmount = 5000.0
        coEvery { transactionDao.getTotalAmountByDateRange(TransactionType.INCOME, startDate, endDate) } returns expectedAmount

        // When
        val result = repository.getTotalIncome(startDate, endDate)

        // Then
        assertEquals(expectedAmount, result, 0.01)
        coVerify { transactionDao.getTotalAmountByDateRange(TransactionType.INCOME, startDate, endDate) }
    }

    @Test
    fun `getTotalIncome should return zero when null`() = runTest {
        // Given
        val startDate = 0L
        val endDate = 9999999999L
        coEvery { transactionDao.getTotalAmountByDateRange(TransactionType.INCOME, startDate, endDate) } returns null

        // When
        val result = repository.getTotalIncome(startDate, endDate)

        // Then
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `getTotalExpense should return amount from dao`() = runTest {
        // Given
        val startDate = 0L
        val endDate = 9999999999L
        val expectedAmount = 3000.0
        coEvery { transactionDao.getTotalAmountByDateRange(TransactionType.EXPENSE, startDate, endDate) } returns expectedAmount

        // When
        val result = repository.getTotalExpense(startDate, endDate)

        // Then
        assertEquals(expectedAmount, result, 0.01)
    }

    @Test
    fun `getCategoryTotal should return amount from dao`() = runTest {
        // Given
        val categoryId = 1L
        val type = TransactionType.EXPENSE
        val startDate = 0L
        val endDate = 9999999999L
        val expectedAmount = 1000.0
        coEvery { transactionDao.getCategoryTotal(categoryId, type, startDate, endDate) } returns expectedAmount

        // When
        val result = repository.getCategoryTotal(categoryId, type, startDate, endDate)

        // Then
        assertEquals(expectedAmount, result, 0.01)
        coVerify { transactionDao.getCategoryTotal(categoryId, type, startDate, endDate) }
    }

    @Test
    fun `getAccountTotal should return amount from dao`() = runTest {
        // Given
        val accountId = 1L
        val type = TransactionType.EXPENSE
        val startDate = 0L
        val endDate = 9999999999L
        val expectedAmount = 2000.0
        coEvery { transactionDao.getAccountTotal(accountId, type, startDate, endDate) } returns expectedAmount

        // When
        val result = repository.getAccountTotal(accountId, type, startDate, endDate)

        // Then
        assertEquals(expectedAmount, result, 0.01)
        coVerify { transactionDao.getAccountTotal(accountId, type, startDate, endDate) }
    }

    @Test
    fun `searchTransactions should return flow from dao`() = runTest {
        // Given
        val query = "test"
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.searchTransactions(query) } returns flowOf(transactions)

        // When
        val result = repository.searchTransactions(query).first()

        // Then
        assertEquals(transactions, result)
        verify { transactionDao.searchTransactions(query) }
    }

    @Test
    fun `getTransactionCount should return count from dao`() = runTest {
        // Given
        val expectedCount = 10
        coEvery { transactionDao.getTransactionCount() } returns expectedCount

        // When
        val result = repository.getTransactionCount()

        // Then
        assertEquals(expectedCount, result)
        coVerify { transactionDao.getTransactionCount() }
    }

    @Test
    fun `getFirstTransactionDate should return date from dao`() = runTest {
        // Given
        val expectedDate = 1000L
        coEvery { transactionDao.getFirstTransactionDate() } returns expectedDate

        // When
        val result = repository.getFirstTransactionDate()

        // Then
        assertEquals(expectedDate, result)
        coVerify { transactionDao.getFirstTransactionDate() }
    }

    @Test
    fun `getLastTransactionDate should return date from dao`() = runTest {
        // Given
        val expectedDate = 9999L
        coEvery { transactionDao.getLastTransactionDate() } returns expectedDate

        // When
        val result = repository.getLastTransactionDate()

        // Then
        assertEquals(expectedDate, result)
        coVerify { transactionDao.getLastTransactionDate() }
    }

    @Test
    fun `getMonthRange should return correct range for January 2024`() {
        // Given
        val year = 2024
        val month = 1 // January

        // When
        val (start, end) = repository.getMonthRange(year, month)

        // Then
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))

        calendar.timeInMillis = end
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `getYearRange should return correct range for 2024`() {
        // Given
        val year = 2024

        // When
        val (start, end) = repository.getYearRange(year)

        // Then
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))

        calendar.timeInMillis = end
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, calendar.get(Calendar.MONTH))
        assertEquals(31, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `getRecentTransactions should return flow from dao with calculated dates`() = runTest {
        // Given
        val limit = 20
        every { transactionDao.getRecentTransactions(any(), any(), limit) } returns flowOf(emptyList())

        // When
        repository.getRecentTransactions(limit).first()

        // Then
        verify { transactionDao.getRecentTransactions(any(), any(), limit) }
    }

    @Test
    fun `getRecentTransactionsSync should return list from flow`() = runTest {
        // Given
        val limit = 10
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 100.0,
                date = System.currentTimeMillis()
            )
        )
        every { transactionDao.getRecentTransactions(any(), any(), limit) } returns flowOf(transactions)

        // When
        val result = repository.getRecentTransactionsSync(limit)

        // Then
        assertEquals(transactions, result)
    }
}
