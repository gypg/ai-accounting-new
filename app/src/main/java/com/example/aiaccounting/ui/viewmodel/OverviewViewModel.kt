package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.data.repository.ButlerRepository
import com.example.aiaccounting.ui.components.charts.MonthlyData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val butlerRepository: ButlerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    // 共享的交易数据流，避免多次查询
    private val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 分类数据流 - 用于获取真实分类名称
    val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentTransactions = transactionRepository.getRecentTransactions(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 合并所有统计数据为一个 Flow，避免多次 stateIn() 的内存开销
    private val allStats = allTransactions
        .map { transactions ->
            OverviewStats(
                monthly = calculateMonthlyStats(transactions),
                yearlyTrend = calculateYearlyTrend(transactions),
                today = calculateTodayStats(transactions),
                week = calculateWeekStats(transactions)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverviewStats()
        )

    val monthlyStats = allStats.map { it.monthly }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats())

    val yearlyTrendData = allStats.map { it.yearlyTrend }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStats = allStats.map { it.today }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayStats())

    val weekStats = allStats.map { it.week }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayStats())

    val currentButlerName: StateFlow<String> = butlerRepository.currentButlerId
        .map { id ->
            val builtIn = com.example.aiaccounting.data.model.ButlerManager.getButlerById(id)
            builtIn?.name ?: "自定义管家"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }
    }

    private fun calculateMonthlyStats(transactions: List<Transaction>): MonthlyStats {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 年度统计
        val yearStart = Calendar.getInstance().apply {
            set(currentYear, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearEnd = Calendar.getInstance().apply {
            set(currentYear + 1, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearlyTransactions = transactions.filter { it.date in yearStart until yearEnd }
        val totalIncome = yearlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = yearlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // 月度统计
        val monthStart = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1, 0, 0, 0)
        }.timeInMillis

        val monthEnd = Calendar.getInstance().apply {
            set(currentYear, currentMonth + 1, 1, 0, 0, 0)
        }.timeInMillis

        val monthlyTransactions = transactions.filter { it.date in monthStart until monthEnd }
        val monthlyIncome = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return MonthlyStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense
        )
    }

    /**
     * 计算年度趋势数据（按月）
     */
    private fun calculateYearlyTrend(transactions: List<Transaction>): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val monthlyData = mutableListOf<MonthlyData>()

        for (month in 0..11) {
            val monthStart = Calendar.getInstance().apply {
                set(currentYear, month, 1, 0, 0, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(currentYear, month + 1, 1, 0, 0, 0)
            }.timeInMillis

            val monthTransactions = transactions.filter { it.date in monthStart until monthEnd }
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            monthlyData.add(
                MonthlyData(
                    month = "${month + 1}月",
                    income = income,
                    expense = expense
                )
            )
        }

        return monthlyData
    }

    /**
     * 计算当天统计
     */
    private fun calculateTodayStats(transactions: List<Transaction>): DayStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val todayEnd = calendar.timeInMillis

        val todayTransactions = transactions.filter { it.date in todayStart until todayEnd }
        val income = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DayStats(
            income = income,
            expense = expense,
            count = todayTransactions.size
        )
    }

    /**
     * 计算当周统计
     * 使用与马年主题相同的计算逻辑
     */
    private fun calculateWeekStats(transactions: List<Transaction>): DayStats {
        val today = Calendar.getInstance()
        val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        
        // 计算本周一（如果今天是周日，DAY_OF_WEEK=1，需要回退6天；其他情况回退到周一）
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        
        val monday = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = monday.timeInMillis
        
        // 计算本周日（周一 + 6天）
        val sunday = Calendar.getInstance().apply {
            timeInMillis = monday.timeInMillis
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val weekEnd = sunday.timeInMillis

        val weekTransactions = transactions.filter { it.date in weekStart..weekEnd }
        val income = weekTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = weekTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DayStats(
            income = income,
            expense = expense,
            count = weekTransactions.size
        )
    }
}

data class OverviewUiState(
    val isBalanceVisible: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MonthlyStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0
)

data class DayStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val count: Int = 0
)

data class OverviewStats(
    val monthly: MonthlyStats = MonthlyStats(),
    val yearlyTrend: List<MonthlyData> = emptyList(),
    val today: DayStats = DayStats(),
    val week: DayStats = DayStats()
)
