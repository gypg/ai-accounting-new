package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import com.example.aiaccounting.data.local.entity.BudgetProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预算仓库
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    // ==================== 基础CRUD ====================

    fun getAllActiveBudgets(): Flow<List<Budget>> = budgetDao.getAllActiveBudgets()

    fun getAvailableBudgetMonths(): Flow<List<Pair<Int, Int>>> {
        return budgetDao.getAllActiveBudgets().map { budgets ->
            budgets.flatMap { budget ->
                when (budget.period) {
                    BudgetPeriod.YEARLY -> (1..12).map { month -> budget.year to month }
                    BudgetPeriod.MONTHLY -> listOf(budget.year to (budget.month ?: 1))
                }
            }.distinct().sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        }
    }

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun getBudgetById(budgetId: Long): Budget? = budgetDao.getBudgetById(budgetId)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    suspend fun deleteBudgetById(budgetId: Long) = budgetDao.deleteBudgetById(budgetId)

    // ==================== 预算进度查询 ====================

    /**
     * 获取月度预算进度（包含总预算和分类预算）
     */
    fun getMonthlyBudgetProgress(year: Int, month: Int): Flow<List<BudgetProgress>> {
        return budgetDao.getBudgetsForMonth(year, month).map { budgets ->
            budgets.map { budget ->
                val spent = calculateSpentAmount(budget, year, month)
                BudgetProgress.calculate(budget, spent)
            }
        }
    }

    /**
     * 获取月度总预算进度
     */
    fun getTotalBudgetProgress(year: Int, month: Int): Flow<BudgetProgress?> {
        return budgetDao.getTotalBudget(year, month).map { budget ->
            budget?.let {
                val spent = calculateTotalSpent(year, month)
                BudgetProgress.calculate(it, spent)
            }
        }
    }

    /**
     * 获取年度总预算进度
     */
    fun getYearlyTotalBudgetProgress(year: Int): Flow<BudgetProgress?> {
        return budgetDao.getYearlyTotalBudget(year).map { budget ->
            budget?.let {
                val spent = calculateYearlyTotalSpent(year)
                BudgetProgress.calculate(it, spent)
            }
        }
    }

    /**
     * 获取分类预算进度
     */
    fun getCategoryBudgetProgress(categoryId: Long, year: Int, month: Int): Flow<BudgetProgress?> {
        return budgetDao.getCategoryBudget(categoryId, year, month).map { budget ->
            budget?.let {
                val spent = calculateCategorySpent(categoryId, year, month)
                BudgetProgress.calculate(it, spent)
            }
        }
    }

    /**
     * 获取需要提醒的预算（超过阈值或超支）
     */
    fun getAlertBudgets(year: Int, month: Int): Flow<List<BudgetProgress>> {
        return getMonthlyBudgetProgress(year, month).map { progresses ->
            progresses.filter { it.shouldAlert }
        }
    }

    // ==================== 计算支出金额 ====================

    /**
     * 计算预算的已支出金额
     */
    private suspend fun calculateSpentAmount(budget: Budget, year: Int, month: Int): Double {
        return if (budget.categoryId == null) {
            when (budget.period) {
                BudgetPeriod.YEARLY -> calculateYearlyTotalSpent(year)
                BudgetPeriod.MONTHLY -> calculateTotalSpent(year, month)
            }
        } else {
            // 分类预算 - 计算特定分类支出
            calculateCategorySpent(budget.categoryId, year, month)
        }
    }

    /**
     * 计算总支出
     */
    private suspend fun calculateTotalSpent(year: Int, month: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endDate = calendar.timeInMillis

        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .first()
            .filter { it.type.name.lowercase() == "expense" }
            .sumOf { kotlin.math.abs(it.amount) }
    }

    /**
     * 计算年度总支出
     */
    private suspend fun calculateYearlyTotalSpent(year: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        calendar.set(year, 11, 31, 23, 59, 59)
        val endDate = calendar.timeInMillis

        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .first()
            .filter { it.type.name.lowercase() == "expense" }
            .sumOf { kotlin.math.abs(it.amount) }
    }

    /**
     * 计算分类支出
     */
    private suspend fun calculateCategorySpent(categoryId: Long, year: Int, month: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis

        calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endDate = calendar.timeInMillis

        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .first()
            .filter { it.type.name.lowercase() == "expense" && it.categoryId == categoryId }
            .sumOf { kotlin.math.abs(it.amount) }
    }

    // ==================== 默认预算设置 ====================

    /**
     * 创建默认月度总预算
     */
    suspend fun createDefaultMonthlyBudget(amount: Double, year: Int, month: Int): Long {
        val budget = Budget(
            name = "${year}年${month}月总预算",
            amount = amount,
            categoryId = null,
            period = BudgetPeriod.MONTHLY,
            year = year,
            month = month,
            alertThreshold = 0.8
        )
        return budgetDao.insertBudget(budget)
    }

    /**
     * 创建默认年度总预算
     */
    suspend fun createDefaultYearlyBudget(amount: Double, year: Int): Long {
        val budget = Budget(
            name = "${year}年总预算",
            amount = amount,
            categoryId = null,
            period = BudgetPeriod.YEARLY,
            year = year,
            month = null,
            alertThreshold = 0.8
        )
        return budgetDao.insertBudget(budget)
    }

    /**
     * 创建分类预算
     */
    suspend fun createCategoryBudget(
        name: String,
        amount: Double,
        categoryId: Long,
        year: Int,
        month: Int
    ): Long {
        val budget = Budget(
            name = name,
            amount = amount,
            categoryId = categoryId,
            period = BudgetPeriod.MONTHLY,
            year = year,
            month = month,
            alertThreshold = 0.8
        )
        return budgetDao.insertBudget(budget)
    }

    // ==================== 统计 ====================

    suspend fun getActiveBudgetCount(): Int = budgetDao.getActiveBudgetCount()

    suspend fun hasBudgetForMonth(year: Int, month: Int): Boolean {
        return budgetDao.getBudgetsForMonth(year, month).first().isNotEmpty()
    }
}
