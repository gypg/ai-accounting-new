package com.example.aiaccounting.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@SmallTest
class TransactionDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        transactionDao = database.transactionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertTransaction() = runBlocking {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            subCategory = "午餐",
            accountId = 1,
            date = LocalDateTime.now(),
            note = "测试交易"
        )

        transactionDao.insertTransaction(transaction)
        val allTransactions = transactionDao.getAllTransactions().first()

        assertEquals(1, allTransactions.size)
        assertEquals(transaction.amount, allTransactions[0].amount, 0.01)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = LocalDateTime.now()
        )

        transactionDao.insertTransaction(transaction)
        transactionDao.deleteTransaction(transaction)
        val allTransactions = transactionDao.getAllTransactions().first()

        assertTrue(allTransactions.isEmpty())
    }

    @Test
    fun getTransactionsByDateRange() = runBlocking {
        val now = LocalDateTime.now()
        val transaction1 = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = now.minusDays(5)
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 200.0,
            type = TransactionType.INCOME,
            category = "工资",
            accountId = 1,
            date = now
        )

        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        val startDate = now.minusDays(3)
        val endDate = now.plusDays(1)
        val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()

        assertEquals(1, transactions.size)
        assertEquals(200.0, transactions[0].amount, 0.01)
    }

    @Test
    fun getTransactionsByCategory() = runBlocking {
        val transaction1 = Transaction(
            id = 1,
            amount = 50.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = LocalDateTime.now()
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "交通",
            accountId = 1,
            date = LocalDateTime.now()
        )

        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        val foodTransactions = transactionDao.getTransactionsByCategory("餐饮").first()

        assertEquals(1, foodTransactions.size)
        assertEquals("餐饮", foodTransactions[0].category)
    }

    @Test
    fun getTotalByTypeAndDateRange() = runBlocking {
        val now = LocalDateTime.now()
        val transaction1 = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = now
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 200.0,
            type = TransactionType.EXPENSE,
            category = "交通",
            accountId = 1,
            date = now
        )
        val transaction3 = Transaction(
            id = 3,
            amount = 5000.0,
            type = TransactionType.INCOME,
            category = "工资",
            accountId = 1,
            date = now
        )

        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)
        transactionDao.insertTransaction(transaction3)

        val startDate = now.minusDays(1)
        val endDate = now.plusDays(1)
        val totalExpense = transactionDao.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            startDate,
            endDate
        ).first()
        val totalIncome = transactionDao.getTotalByTypeAndDateRange(
            TransactionType.INCOME,
            startDate,
            endDate
        ).first()

        assertEquals(300.0, totalExpense ?: 0.0, 0.01)
        assertEquals(5000.0, totalIncome ?: 0.0, 0.01)
    }

    @Test
    fun searchTransactions() = runBlocking {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = LocalDateTime.now(),
            note = "午餐在麦当劳",
            merchant = "麦当劳"
        )

        transactionDao.insertTransaction(transaction)

        val searchResults = transactionDao.searchTransactions("麦当劳").first()

        assertEquals(1, searchResults.size)
        assertEquals("麦当劳", searchResults[0].merchant)
    }

    @Test
    fun getMonthlyStatistics() = runBlocking {
        val now = LocalDateTime.now()
        val transaction1 = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            accountId = 1,
            date = now
        )
        val transaction2 = Transaction(
            id = 2,
            amount = 200.0,
            type = TransactionType.INCOME,
            category = "工资",
            accountId = 1,
            date = now
        )

        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        val monthlyStats = transactionDao.getMonthlyStatistics(
            now.year,
            now.monthValue
        ).first()

        assertEquals(1, monthlyStats.expenseCount)
        assertEquals(1, monthlyStats.incomeCount)
        assertEquals(100.0, monthlyStats.totalExpense, 0.01)
        assertEquals(200.0, monthlyStats.totalIncome, 0.01)
    }
}
