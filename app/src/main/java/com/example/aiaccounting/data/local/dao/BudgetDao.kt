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
        SELECT b.*, 
               (SELECT SUM(t.amount) 
                FROM transactions t 
                WHERE t.categoryId = b.categoryId 
                AND t.type = 'EXPENSE'
                AND t.date >= :startDate 
                AND t.date <= :endDate) as spent
        FROM budgets b
        WHERE b.isActive = 1
        AND b.startDate <= :endDate
        AND (b.endDate IS NULL OR b.endDate >= :startDate)
    """)
    fun getBudgetsWithSpent(startDate: Long, endDate: Long): Flow<List<BudgetWithSpent>>

    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getActiveBudgetCount(): Int
}

/**
 * Data class for budget with spent amount
 */
data class BudgetWithSpent(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val period: com.example.aiaccounting.data.local.entity.BudgetPeriod,
    val startDate: Long,
    val endDate: Long?,
    val alertThreshold: Double,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val spent: Double?
) {
    val remaining: Double
        get() = (amount - (spent ?: 0.0))

    val percentageUsed: Double
        get() = if (amount > 0) ((spent ?: 0.0) / amount) else 0.0

    val isOverBudget: Boolean
        get() = (spent ?: 0.0) > amount

    val isNearLimit: Boolean
        get() = percentageUsed >= alertThreshold && !isOverBudget
}