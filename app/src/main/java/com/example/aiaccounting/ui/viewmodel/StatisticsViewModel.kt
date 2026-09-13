package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.ui.components.charts.MonthlyData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appLogLogger: AppLogLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    val statistics = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        _uiState
    ) { transactions, categories, state ->
        val currentCalendar = Calendar.getInstance()
        appLogLogger.debug(
            source = "UI",
            category = "statistics_refresh",
            message = "统计数据刷新",
            details = "transactions=${transactions.size},categories=${categories.size},filter=${state.timeFilter},tab=${state.selectedTab},year=${currentCalendar.get(Calendar.YEAR)},month=${currentCalendar.get(Calendar.MONTH) + 1},isEmpty=${transactions.isEmpty()}"
        )
        calculateStatistics(transactions, categories, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsData()
    )

    private fun calculateStatistics(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        categories: List<com.example.aiaccounting.data.local.entity.Category>,
        state: StatisticsUiState
    ): StatisticsData {
        val calendar = Calendar.getInstance()

        // 根据时间筛选过滤交易
        val filteredTransactions = when {
            // 处理年月日格式筛选 (如 "2024-03-15")
            state.timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = state.timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1 // Calendar月份从0开始
                val filterDay = parts[2].toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear &&
                    calendar.get(Calendar.MONTH) == filterMonth &&
                    calendar.get(Calendar.DAY_OF_MONTH) == filterDay
                }
            }
            // 处理年月格式筛选 (如 "2024-03")
            state.timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = state.timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1 // Calendar月份从0开始
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear &&
                    calendar.get(Calendar.MONTH) == filterMonth
                }
            }
            // 处理年份筛选 (如 "2024")
            state.timeFilter.matches(Regex("\\d{4}")) -> {
                val filterYear = state.timeFilter.toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear
                }
            }
            // 处理年份筛选 (如 "year-2024")
            state.timeFilter.startsWith("year-") -> {
                val filterYear = state.timeFilter.substringAfter("year-").toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear
                }
            }
            // 新增快捷时间筛选
            state.timeFilter == "today" -> {
                val today = Calendar.getInstance()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                }
            }
            state.timeFilter == "yesterday" -> {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
                }
            }
            state.timeFilter == "this_week" -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                transactions.filter { it.date >= weekStart.timeInMillis }
            }
            state.timeFilter == "this_month" -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            state.timeFilter == "this_year" -> {
                val currentYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            state.timeFilter == "current" -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            state.timeFilter == "last" -> {
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == lastMonth &&
                    calendar.get(Calendar.YEAR) == lastYear
                }
            }
            state.timeFilter == "3months" -> {
                val threeMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -3)
                }.timeInMillis
                transactions.filter { it.date >= threeMonthsAgo }
            }
            state.timeFilter == "6months" -> {
                val sixMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }.timeInMillis
                transactions.filter { it.date >= sixMonthsAgo }
            }
            state.timeFilter == "1year" -> {
                val oneYearAgo = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                }.timeInMillis
                transactions.filter { it.date >= oneYearAgo }
            }
            state.timeFilter == "all" -> transactions
            else -> transactions
        }

        // 根据选中的标签过滤
        val typeFiltered = when (state.selectedTab) {
            "income" -> filteredTransactions.filter { it.type == TransactionType.INCOME }
            "expense" -> filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            else -> filteredTransactions
        }

        val totalIncome = filteredTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { kotlin.math.abs(it.amount) }
        val totalExpense = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { kotlin.math.abs(it.amount) }

        // 创建分类ID到名称和颜色的映射
        val categoryMap = categories.associateBy { it.id }

        // 分类统计
        val categoryStats = typeFiltered
            .groupBy { it.categoryId }
            .map { (categoryId, transList) ->
                val amount = transList.sumOf { kotlin.math.abs(it.amount) }
                val total = if (state.selectedTab == "income") totalIncome else totalExpense
                val category = categoryMap[categoryId]
                CategoryStat(
                    name = category?.name ?: "未分类",
                    amount = amount,
                    percentage = if (total > 0) (amount / total).toFloat() else 0f,
                    color = category?.color ?: "#2196F3"
                )
            }
            .sortedByDescending { it.amount }

        // 计算各种趋势数据
        val monthlyData = calculateMonthlyTrend(typeFiltered, state.timeFilter, state.selectedTab)
        val dailyData = calculateDailyTrend(filteredTransactions, state.timeFilter)
        val weeklyData = calculateWeeklyTrend(filteredTransactions, state.timeFilter)

        appLogLogger.debug(
            source = "UI",
            category = "statistics_trend_detail",
            message = "统计趋势明细",
            details = "filter=${state.timeFilter},tab=${state.selectedTab},typeFiltered=${typeFiltered.size},monthlyPoints=${monthlyData.size},nonZeroIncome=${monthlyData.count { it.income > 0 }},nonZeroExpense=${monthlyData.count { it.expense > 0 }},transactionsReturned=${typeFiltered.size}"
        )

        return StatisticsData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categoryStats = categoryStats,
            monthlyTrend = monthlyData,
            transactions = typeFiltered,
            dailyTrend = dailyData,
            weeklyTrend = weeklyData
        )
    }

    /**
     * 计算月度趋势数据
     */
    private fun calculateMonthlyTrend(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        timeFilter: String,
        selectedTab: String
    ): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM月", Locale.getDefault())

        // 确定时间范围
        val (startTime, endTime) = when {
            // 处理年月日格式筛选 (如 "2024-03-15")
            timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1
                val filterDay = parts[2].toInt()
                calendar.set(filterYear, filterMonth, filterDay, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.set(filterYear, filterMonth, filterDay, 23, 59, 59)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年月格式筛选 (如 "2024-03")
            timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1
                calendar.set(filterYear, filterMonth, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年份筛选 (如 "2024")
            timeFilter.matches(Regex("\\d{4}")) -> {
                val filterYear = timeFilter.toInt()
                calendar.set(filterYear, 0, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年份筛选 (如 "year-2024")
            timeFilter.startsWith("year-") -> {
                val filterYear = timeFilter.substringAfter("year-").toInt()
                calendar.set(filterYear, 0, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "current" || timeFilter == "this_month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "this_year" -> {
                calendar.set(Calendar.MONTH, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "last" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "3months" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.MONTH, -2)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 3)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "6months" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.MONTH, -5)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 6)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "1year" -> {
                calendar.set(Calendar.MONTH, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "all" -> {
                // 显示所有数据，从最早的记录开始
                val earliestTransaction = transactions.minByOrNull { it.date }
                val start = earliestTransaction?.date ?: calendar.timeInMillis
                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            else -> {
                // 默认显示最近6个月
                val end = calendar.timeInMillis
                calendar.add(Calendar.MONTH, -6)
                Pair(calendar.timeInMillis, end)
            }
        }

        // 过滤时间范围内的交易
        val filteredTransactions = transactions.filter {
            it.date in startTime..endTime
        }

        // 按月份分组统计
        val monthlyGroups = filteredTransactions.groupBy { transaction ->
            monthFormat.format(Date(transaction.date))
        }

        // 生成所有月份（包括没有数据的月份）
        val result = mutableListOf<MonthlyData>()
        calendar.timeInMillis = startTime

        while (calendar.timeInMillis < endTime) {
            val monthKey = monthFormat.format(calendar.time)
            val monthTransactions = monthlyGroups[monthKey] ?: emptyList()

            val income = if (selectedTab == "expense") {
                0.0
            } else {
                monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { kotlin.math.abs(it.amount) }
            }
            val expense = if (selectedTab == "income") {
                0.0
            } else {
                monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { kotlin.math.abs(it.amount) }
            }

            result.add(
                MonthlyData(
                    month = displayFormat.format(calendar.time),
                    income = income,
                    expense = expense
                )
            )

            calendar.add(Calendar.MONTH, 1)
        }

        return result
    }

    fun setSelectedTab(tab: String) {
        appLogLogger.info(
            source = "UI",
            category = "statistics_interaction",
            message = "切换统计标签",
            details = "selectedTab=$tab"
        )
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setTimeFilter(filter: String) {
        appLogLogger.info(
            source = "UI",
            category = "statistics_interaction",
            message = "切换统计时间筛选",
            details = "timeFilter=$filter"
        )
        _uiState.update { it.copy(timeFilter = filter) }
    }

    fun setStartDate(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun setMergeSubCategories(merge: Boolean) {
        _uiState.update { it.copy(mergeSubCategories = merge) }
    }

    fun setShowAllCategories(show: Boolean) {
        _uiState.update { it.copy(showAllCategories = show) }
    }

    /**
     * 计算每日趋势数据
     */
    private fun calculateDailyTrend(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        timeFilter: String
    ): List<DailyData> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val range = when {
            timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val day = parts[2].toInt()
                val start = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                start to end
            }
            timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val start = Calendar.getInstance().apply {
                    set(year, month, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                start to end
            }
            else -> {
                val daysToShow = when (timeFilter) {
                    "current", "last" -> 30
                    "3months" -> 90
                    "6months" -> 180
                    "1year" -> 365
                    "all" -> 30
                    else -> 30
                }
                val end = Calendar.getInstance()
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -daysToShow + 1)
                }
                start to end
            }
        }

        val startDate = range.first
        val endDate = range.second

        val dailyGroups = transactions.groupBy { transaction ->
            fullDateFormat.format(Date(transaction.date))
        }

        val result = mutableListOf<DailyData>()
        calendar.timeInMillis = startDate.timeInMillis

        while (calendar.timeInMillis <= endDate.timeInMillis) {
            val dateKey = fullDateFormat.format(calendar.time)
            val dayTransactions = dailyGroups[dateKey] ?: emptyList()

            val income = dayTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { kotlin.math.abs(it.amount) }
            val expense = dayTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { kotlin.math.abs(it.amount) }

            result.add(
                DailyData(
                    date = dateFormat.format(calendar.time),
                    income = income,
                    expense = expense,
                    dayOfWeek = dayOfWeekFormat.format(calendar.time)
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    /**
     * 计算每周趋势数据
     */
    private fun calculateWeeklyTrend(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        timeFilter: String
    ): List<WeeklyData> {
        val calendar = Calendar.getInstance()
        val weekFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        // 根据时间筛选确定周数
        val weeksToShow = when (timeFilter) {
            "current", "last" -> 4
            "3months" -> 12
            "6months" -> 24
            "1year" -> 52
            "all" -> 12
            else -> 4
        }

        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, -weeksToShow + 1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        // 按周分组
        val weeklyGroups = transactions.groupBy { transaction ->
            calendar.timeInMillis = transaction.date
            calendar.get(Calendar.WEEK_OF_YEAR)
        }

        // 生成所有周
        val result = mutableListOf<WeeklyData>()
        calendar.timeInMillis = startDate.timeInMillis

        for (weekNum in 0 until weeksToShow) {
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            val weekTransactions = weeklyGroups[weekOfYear] ?: emptyList()

            val income = weekTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { kotlin.math.abs(it.amount) }
            val expense = weekTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { kotlin.math.abs(it.amount) }

            val weekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val weekEnd = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1) // 移动到下周

            result.add(
                WeeklyData(
                    week = "第${weekNum + 1}周",
                    income = income,
                    expense = expense,
                    startDate = weekFormat.format(Date(weekStart)),
                    endDate = weekFormat.format(Date(weekEnd))
                )
            )
        }

        return result
    }
}

data class StatisticsUiState(
    val selectedTab: String = "expense",
    val timeFilter: String = "current",
    val startDate: String? = null,
    val endDate: String? = null,
    val mergeSubCategories: Boolean = false,
    val showAllCategories: Boolean = true
)

data class StatisticsData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val monthlyTrend: List<MonthlyData> = emptyList(),
    val transactions: List<com.example.aiaccounting.data.local.entity.Transaction> = emptyList(),
    val dailyTrend: List<DailyData> = emptyList(),
    val weeklyTrend: List<WeeklyData> = emptyList()
)

// 每日数据
data class DailyData(
    val date: String,
    val income: Double,
    val expense: Double,
    val dayOfWeek: String
)

// 每周数据
data class WeeklyData(
    val week: String,
    val income: Double,
    val expense: Double,
    val startDate: String,
    val endDate: String
)

data class CategoryStat(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: String
)
