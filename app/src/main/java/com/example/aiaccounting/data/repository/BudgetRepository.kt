package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Budget data
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    /**
     * Get all active budgets
     */
    fun getAllActiveBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllActiveBudgets()
    }

    /**
     * Get budget by ID
     */
    suspend fun getBudgetById(budgetId: Long): Budget? {
        return budgetDao.getBudgetById(budgetId)
    }

    /**
     * Get budgets by category
     */
    fun getBudgetsByCategory(categoryId: Long): Flow<List<Budget>> {
        return budgetDao.getBudgetsByCategory(categoryId)
    }

    /**
     * Get current budget for category
     */
    suspend fun getCurrentBudgetForCategory(categoryId: Long): Budget? {
        val currentDate = System.currentTimeMillis()
        return budgetDao.getCurrentBudgetForCategory(categoryId, currentDate)
    }

    /**
     * Insert new budget
     */
    suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget)
    }

    /**
     * Update budget
     */
    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Delete budget
     */
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    /**
     * Delete budget by ID
     */
    suspend fun deleteBudgetById(budgetId: Long) {
        budgetDao.deleteBudgetById(budgetId)
    }

    /**
     * Deactivate budget
     */
    suspend fun deactivateBudget(budgetId: Long) {
        budgetDao.deactivateBudget(budgetId)
    }

    /**
     * Get budgets with spent amounts
     */
    fun getBudgetsWithSpent(startDate: Long, endDate: Long): Flow<List<BudgetDao.BudgetWithSpent>> {
        return budgetDao.getBudgetsWithSpent(startDate, endDate)
    }

    /**
     * Get active budget count
     */
    suspend fun getActiveBudgetCount(): Int {
        return budgetDao.getActiveBudgetCount()
    }

    /**
     * Check if category is over budget
     */
    suspend fun isOverBudget(categoryId: Long): Boolean {
        val budget = getCurrentBudgetForCategory(categoryId) ?: return false
        val budgetsWithSpent = getBudgetsWithSpent(budget.startDate, budget.endDate ?: System.currentTimeMillis()).first()
        return budgetsWithSpent.any { it.id == budget.id && it.isOverBudget }
    }

    /**
     * Get all budgets
     */
    fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets()
    }

    /**
     * Get budget by category
     */
    fun getBudgetByCategory(categoryId: Long): Flow<Budget?> {
        return budgetDao.getBudgetByCategory(categoryId)
    }

    /**
     * Update budget spent amount
     */
    suspend fun updateBudgetSpent(budgetId: Long, spent: Double) {
        budgetDao.updateBudgetSpent(budgetId, spent)
    }

    /**
     * Check if category has budget
     */
    fun hasBudgetForCategory(categoryId: Long): Flow<Boolean> {
        return budgetDao.hasBudgetForCategory(categoryId)
    }
}