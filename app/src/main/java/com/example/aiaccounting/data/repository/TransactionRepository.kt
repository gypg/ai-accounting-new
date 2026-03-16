package com.example.aiaccounting.data.repository

import android.content.Context
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.widget.WidgetDataSyncHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Transaction data
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Get all transactions
     */
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getRecentTransactionsList(limit: Int): List<Transaction> {
        return transactionDao.getRecentTransactionsList(limit)
    }

    suspend fun getTransactionsByDateRangeList(startDate: Long, endDate: Long, limit: Int): List<Transaction> {
        return transactionDao.getTransactionsByDateRangeList(startDate, endDate, limit)
    }

    /**
     * Get all transactions as list
     */
    suspend fun getAllTransactionsList(): List<Transaction> {
        return transactionDao.getAllTransactions().first()
    }

    /**
     * Get transaction by ID
     */
    suspend fun getTransactionById(transactionId: Long): Transaction? {
        return transactionDao.getTransactionById(transactionId)
    }

    /**
     * Get transactions by account
     */
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId)
    }

    /**
     * Get transactions by category
     */
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }

    /**
     * Get transactions by type (income/expense)
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    /**
     * Get transactions by date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    /**
     * Get transactions by date range and type
     */
    fun getTransactionsByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRangeAndType(startDate, endDate, type)
    }

    /**
     * Get recent transactions for current month
     */
    fun getRecentTransactions(limit: Int = 20): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        return transactionDao.getRecentTransactions(monthStart, monthEnd, limit)
    }

    /**
     * Get recent transactions synchronously (for AI operations)
     */
    suspend fun getRecentTransactionsSync(limit: Int = 10): List<Transaction> {
        return getRecentTransactions(limit).first()
    }

    /**
     * Insert new transaction
     */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        // 更新小组件数据
        WidgetDataSyncHelper.onTransactionInserted(context, transaction)
        return id
    }

    /**
     * Insert multiple transactions
     */
    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }

    /**
     * Update transaction
     */
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Delete transaction
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
        // 更新小组件数据
        WidgetDataSyncHelper.onTransactionDeleted(context, transaction)
    }

    /**
     * Delete transaction by ID
     */
    suspend fun deleteTransactionById(transactionId: Long) {
        transactionDao.deleteTransactionById(transactionId)
    }

    /**
     * Delete all transactions by category ID
     */
    suspend fun deleteTransactionsByCategory(categoryId: Long) {
        transactionDao.deleteTransactionsByCategory(categoryId)
    }

    /**
     * Delete all transactions by account ID
     */
    suspend fun deleteTransactionsByAccount(accountId: Long) {
        transactionDao.deleteTransactionsByAccount(accountId)
    }

    /**
     * Get total income for date range
     */
    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double {
        return transactionDao.getTotalAmountByDateRange(TransactionType.INCOME, startDate, endDate) ?: 0.0
    }

    /**
     * Get total expense for date range
     */
    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double {
        return transactionDao.getTotalAmountByDateRange(TransactionType.EXPENSE, startDate, endDate) ?: 0.0
    }

    /**
     * Get total by category for date range
     */
    suspend fun getCategoryTotal(
        categoryId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getCategoryTotal(categoryId, type, startDate, endDate) ?: 0.0
    }

    /**
     * Get total by account for date range
     */
    suspend fun getAccountTotal(
        accountId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getAccountTotal(accountId, type, startDate, endDate) ?: 0.0
    }

    /**
     * Search transactions
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    /**
     * Get transaction count
     */
    suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }

    /**
     * Get first transaction date
     */
    suspend fun getFirstTransactionDate(): Long? {
        return transactionDao.getFirstTransactionDate()
    }

    /**
     * Get last transaction date
     */
    suspend fun getLastTransactionDate(): Long? {
        return transactionDao.getLastTransactionDate()
    }

    /**
     * Get month start and end timestamps
     */
    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    /**
     * Get current month range
     */
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return getMonthRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    /**
     * Get year range
     */
    fun getYearRange(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(year, 11, 31, 23, 59, 59)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
}