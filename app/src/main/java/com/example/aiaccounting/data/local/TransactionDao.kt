package com.example.aiaccounting.data.local

import androidx.room.*
import com.example.aiaccounting.data.model.Transaction
import com.example.aiaccounting.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE accountId = :accountId 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE categoryId = :categoryId 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate
        AND excludeFromTotal = 0
    """)
    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Date,
        endDate: Date
    ): Flow<Double?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
