package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据访问对象
 */
@Dao
interface BudgetDao {
    // ==================== 基础CRUD ====================

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Long)

    // ==================== 查询方法 ====================

    /**
     * 获取月度预算
     */
    @Query("SELECT * FROM budgets WHERE period = 'MONTHLY' AND year = :year AND month = :month AND isActive = 1")
    fun getMonthlyBudgets(year: Int, month: Int): Flow<List<Budget>>

    /**
     * 获取年度预算
     */
    @Query("SELECT * FROM budgets WHERE period = 'YEARLY' AND year = :year AND isActive = 1")
    fun getYearlyBudgets(year: Int): Flow<List<Budget>>

    /**
     * 获取总预算（不关联分类的预算）
     */
    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND period = 'MONTHLY' AND year = :year AND month = :month AND isActive = 1")
    fun getTotalBudget(year: Int, month: Int): Flow<Budget?>

    /**
     * 获取分类预算
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND period = 'MONTHLY' AND year = :year AND month = :month AND isActive = 1")
    fun getCategoryBudget(categoryId: Long, year: Int, month: Int): Flow<Budget?>

    /**
     * 获取特定月份的所有预算
     */
    @Query("""
        SELECT * FROM budgets 
        WHERE isActive = 1 
        AND (
            (period = 'MONTHLY' AND year = :year AND month = :month)
            OR (period = 'YEARLY' AND year = :year)
        )
        ORDER BY categoryId ASC
    """)
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>>

    // ==================== 统计方法 ====================

    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getActiveBudgetCount(): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE categoryId IS NULL AND isActive = 1")
    suspend fun getTotalBudgetCount(): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE categoryId IS NOT NULL AND isActive = 1")
    suspend fun getCategoryBudgetCount(): Int

    // ==================== 激活/停用 ====================

    @Query("UPDATE budgets SET isActive = :isActive WHERE id = :budgetId")
    suspend fun setBudgetActive(budgetId: Long, isActive: Boolean)
}
