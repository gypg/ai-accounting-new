package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                // 获取本月数据
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                // 获取所有交易
                transactionRepository.getAllTransactions().collect { transactions ->
                    val currentMonthTransactions = transactions.filter { transaction ->
                        val transCalendar = Calendar.getInstance().apply {
                            time = transaction.date
                        }
                        transCalendar.get(Calendar.MONTH) == currentMonth &&
                        transCalendar.get(Calendar.YEAR) == currentYear
                    }

                    // 计算月度收支
                    val monthlyIncome = currentMonthTransactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount }

                    val monthlyExpense = currentMonthTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }

                    // 获取分类统计
                    val categories = categoryRepository.getAllCategories().first()

                    // 支出分类统计
                    val expenseByCategory = currentMonthTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                        .groupBy { it.categoryId }
                        .map { (categoryId, transList) ->
                            val category = categories.find { it.id == categoryId }
                            CategoryStat(
                                name = category?.name ?: "未知分类",
                                amount = transList.sumOf { it.amount },
                                color = category?.color?.let { 
                                    android.graphics.Color.parseColor(it) 
                                } ?: ColorTemplate.MATERIAL_COLORS[0]
                            )
                        }
                        .sortedByDescending { it.amount }

                    // 收入分类统计
                    val incomeByCategory = currentMonthTransactions
                        .filter { it.type == TransactionType.INCOME && it.categoryId != null }
                        .groupBy { it.categoryId }
                        .map { (categoryId, transList) ->
                            val category = categories.find { it.id == categoryId }
                            CategoryStat(
                                name = category?.name ?: "未知分类",
                                amount = transList.sumOf { it.amount },
                                color = category?.color?.let { 
                                    android.graphics.Color.parseColor(it) 
                                } ?: ColorTemplate.COLORFUL_COLORS[0]
                            )
                        }
                        .sortedByDescending { it.amount }

                    // 最近6个月趋势
                    val monthlyTrend = (0..5).map { monthOffset ->
                        val targetCalendar = Calendar.getInstance().apply {
                            add(Calendar.MONTH, -monthOffset)
                        }
                        val targetMonth = targetCalendar.get(Calendar.MONTH)
                        val targetYear = targetCalendar.get(Calendar.YEAR)

                        val monthTransactions = transactions.filter { transaction ->
                            val transCalendar = Calendar.getInstance().apply {
                                time = transaction.date
                            }
                            transCalendar.get(Calendar.MONTH) == targetMonth &&
                            transCalendar.get(Calendar.YEAR) == targetYear
                        }

                        val monthIncome = monthTransactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }

                        val monthExpense = monthTransactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }

                        MonthlyData(
                            month = "${targetMonth + 1}月",
                            income = monthIncome,
                            expense = monthExpense
                        )
                    }.reversed()

                    // 年度趋势（最近12个月）
                    val yearlyTrend = (0..11).map { monthOffset ->
                        val targetCalendar = Calendar.getInstance().apply {
                            add(Calendar.MONTH, -monthOffset)
                        }
                        val targetMonth = targetCalendar.get(Calendar.MONTH)
                        val targetYear = targetCalendar.get(Calendar.YEAR)

                        val monthTransactions = transactions.filter { transaction ->
                            val transCalendar = Calendar.getInstance().apply {
                                time = transaction.date
                            }
                            transCalendar.get(Calendar.MONTH) == targetMonth &&
                            transCalendar.get(Calendar.YEAR) == targetYear
                        }

                        val monthIncome = monthTransactions
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount }

                        val monthExpense = monthTransactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }

                        MonthlyData(
                            month = "${targetMonth + 1}月",
                            income = monthIncome,
                            expense = monthExpense
                        )
                    }.reversed()

                    // 月度对比（最近6个月）
                    val monthlyComparison = monthlyTrend.map { data ->
                        MonthlyComparison(
                            month = data.month,
                            income = data.income,
                            expense = data.expense
                        )
                    }

                    _uiState.update {
                        it.copy(
                            monthlyIncome = monthlyIncome,
                            monthlyExpense = monthlyExpense,
                            expenseByCategory = expenseByCategory,
                            incomeByCategory = incomeByCategory,
                            monthlyTrend = monthlyTrend,
                            yearlyTrend = yearlyTrend,
                            monthlyComparison = monthlyComparison
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun refreshStatistics() {
        loadStatistics()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// 数据类
data class StatisticsUiState(
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val expenseByCategory: List<CategoryStat> = emptyList(),
    val incomeByCategory: List<CategoryStat> = emptyList(),
    val monthlyTrend: List<MonthlyData> = emptyList(),
    val yearlyTrend: List<MonthlyData> = emptyList(),
    val monthlyComparison: List<MonthlyComparison> = emptyList(),
    val error: String? = null
)

data class CategoryStat(
    val name: String,
    val amount: Double,
    val color: Int
)

data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)

data class MonthlyComparison(
    val month: String,
    val income: Double,
    val expense: Double
)
