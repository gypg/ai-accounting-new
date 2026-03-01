package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Budget entity
 */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY startDate DESC")
    fun getAllActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): Budget?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1")
    fun getBudgetsByCategory(categoryId: Long): Flow<List<Budget>>

    @Query("""
        SELECT * FROM budgets 
        WHERE categoryId = :categoryId 
        AND startDate <= :currentDate 
        AND (endDate IS NULL OR endDate >= :currentDate)
        AND isActive = 1
    """)
    suspend fun getCurrentBudgetForCategory(categoryId: Long, currentDate: Long): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Long)

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :budgetId")
    suspend fun deactivateBudget(budgetId: Long)

    @Query("""
        SELECT * FROM budgets
        WHERE isActive = 1
        AND startDate <= :endDate
        AND (endDate IS NULL OR endDate >= :startDate)
    """)
    fun getBudgetsWithSpent(startDate: Long, endDate: Long): Flow<List<Budget>>

    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getActiveBudgetCount(): Int

    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1 LIMIT 1")
    fun getBudgetByCategory(categoryId: Long): Flow<Budget?>

    @Query("UPDATE budgets SET updatedAt = :updatedAt WHERE id = :budgetId")
    suspend fun updateBudgetTimestamp(budgetId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE categoryId = :categoryId AND isActive = 1)")
    fun hasBudgetForCategory(categoryId: Long): Flow<Boolean>
}

