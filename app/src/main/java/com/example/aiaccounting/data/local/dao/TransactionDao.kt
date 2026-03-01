package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction entity
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByType(type: com.example.aiaccounting.data.local.entity.TransactionType): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate 
        AND type = :type
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: com.example.aiaccounting.data.local.entity.TransactionType
    ): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :monthStart AND date < :monthEnd 
        ORDER BY date DESC, createdAt DESC
        LIMIT :limit
    """)
    fun getRecentTransactions(monthStart: Long, monthEnd: Long, limit: Int = 20): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :type AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getTotalAmountByDateRange(
        type: com.example.aiaccounting.data.local.entity.TransactionType,
        startDate: Long,
        endDate: Long
    ): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE categoryId = :categoryId AND type = :type 
        AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getCategoryTotal(
        categoryId: Long,
        type: com.example.aiaccounting.data.local.entity.TransactionType,
        startDate: Long,
        endDate: Long
    ): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE accountId = :accountId AND type = :type 
        AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getAccountTotal(
        accountId: Long,
        type: com.example.aiaccounting.data.local.entity.TransactionType,
        startDate: Long,
        endDate: Long
    ): Double?

    @Query("""
        SELECT * FROM transactions 
        WHERE note LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'
        ORDER BY date DESC, createdAt DESC
    """)
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT MIN(date) FROM transactions")
    suspend fun getFirstTransactionDate(): Long?

    @Query("SELECT MAX(date) FROM transactions")
    suspend fun getLastTransactionDate(): Long?
}