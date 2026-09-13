package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.BudgetProgress
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.data.local.entity.YearlyWealthAnalysis
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.data.repository.ButlerRepository
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIOperationTraceRepository
import com.example.aiaccounting.data.repository.YearlyWealthAnalysisRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.ui.components.charts.MonthlyData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val butlerRepository: ButlerRepository,
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val aiOperationTraceRepository: AIOperationTraceRepository,
    private val yearlyWealthAnalysisRepository: YearlyWealthAnalysisRepository,
    private val appLogLogger: AppLogLogger
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

    private val _selectedYearMonth = MutableStateFlow(selectedYearMonth())
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _yearlyAnalysisLoading = MutableStateFlow(false)
    private val _yearlyAnalysisError = MutableStateFlow<String?>(null)

    // 合并所有统计数据为一个 Flow，避免多次 stateIn() 的内存开销
    private val allStats = combine(allTransactions, _selectedYear) { transactions, selectedYear ->
        OverviewStats(
            monthly = calculateMonthlyStats(transactions, selectedYear),
            yearlyTrend = calculateYearlyTrend(transactions, selectedYear),
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

    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val yearlyAnalysis: StateFlow<YearlyWealthAnalysis?> = _selectedYear
        .flatMapLatest { year ->
            yearlyWealthAnalysisRepository.observeByYear(year)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val yearlyAnalysisLoading: StateFlow<Boolean> = _yearlyAnalysisLoading.asStateFlow()
    val yearlyAnalysisError: StateFlow<String?> = _yearlyAnalysisError.asStateFlow()

    val yearlyBudgetProgress: StateFlow<BudgetProgress?> = _selectedYear
        .flatMapLatest { year ->
            budgetRepository.getYearlyTotalBudgetProgress(year)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalBudgetProgress: StateFlow<BudgetProgress?> = combine(_selectedYear, _selectedYearMonth) { selectedYear, (_, month) ->
        selectedYear to month
    }
        .flatMapLatest { (year, month) ->
            budgetRepository.getTotalBudgetProgress(year, month)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentButlerName: StateFlow<String> = butlerRepository.currentButlerId
        .map { id ->
            val builtIn = com.example.aiaccounting.data.model.ButlerManager.getButlerById(id)
            builtIn?.name ?: "自定义管家"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            combine(accounts, recentTransactions, categories) { accountList, recentTransactionList, categoryList ->
                Triple(accountList, recentTransactionList, categoryList)
            }.collect { (accountList, recentTransactionList, categoryList) ->
                appLogLogger.debug(
                    source = "UI",
                    category = "overview_refresh",
                    message = "总览账户数据刷新",
                    details = "accounts=${accountList.size},recentTransactions=${recentTransactionList.size},categories=${categoryList.size},isEmpty=${accountList.isEmpty() && recentTransactionList.isEmpty()}"
                )
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshCurrentYearMonth()
            }
        }
    }

    fun logOverviewScreenEnter(
        theme: String,
        selectedYear: Int,
        accountCount: Int,
        categoryCount: Int,
        recentTransactionCount: Int
    ) {
        appLogLogger.info(
            source = "UI",
            category = "screen_enter",
            message = "进入总览页",
            details = "screen=Overview,theme=$theme,year=$selectedYear,accounts=$accountCount,categories=$categoryCount,recentTransactions=$recentTransactionCount"
        )
    }

    fun logOverviewAddMenuOpened(theme: String) {
        appLogLogger.info(
            source = "UI",
            category = "entry_menu_open",
            message = "打开记账入口菜单",
            details = "screen=Overview,theme=$theme,menu=AddTransactionMenu"
        )
    }

    fun logOverviewEntrySelected(theme: String, entry: String) {
        appLogLogger.info(
            source = "UI",
            category = "entry_selection",
            message = "选择记账入口",
            details = "screen=Overview,theme=$theme,entry=$entry"
        )
    }

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }
    }

    fun refreshCurrentYearMonth() {
        val previousYear = _selectedYearMonth.value.first
        val nextYearMonth = selectedYearMonth()
        _selectedYearMonth.value = nextYearMonth
        if (_selectedYear.value == previousYear) {
            _selectedYear.value = nextYearMonth.first
        }
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
        _yearlyAnalysisError.value = null
    }

    fun buildYearlyExpenseCategoryStats(limit: Int = 5): List<CategoryStat> {
        val year = _selectedYear.value
        val yearRange = transactionRepository.getYearRange(year)
        val yearTransactions = allTransactions.value.filter { it.date in yearRange.first..yearRange.second }
        val expenseTransactions = yearTransactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTransactions.sumOf { kotlin.math.abs(it.amount) }
        val categoryMap = categories.value.associateBy { it.id }
        return expenseTransactions
            .groupBy { it.categoryId }
            .map { (categoryId, transactions) ->
                val amount = transactions.sumOf { kotlin.math.abs(it.amount) }
                val category = categoryMap[categoryId]
                CategoryStat(
                    name = category?.name ?: "未分类",
                    amount = amount,
                    percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f,
                    color = category?.color ?: "#2196F3"
                )
            }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    fun generateYearlyAnalysis() {
        val targetYear = _selectedYear.value
        viewModelScope.launch {
            _yearlyAnalysisLoading.value = true
            _yearlyAnalysisError.value = null
            runCatching {
                val config = aiConfigRepository.getAIConfig().first()
                require(config.isEnabled && config.apiUrl.isNotBlank()) { "请先在 AI 设置中启用可用的云端 AI 配置" }

                val monthly = monthlyStats.value
                val trend = yearlyTrendData.value
                val topCategories = buildYearlyExpenseCategoryStats(limit = 5)
                val highestExpenseMonth = trend.maxByOrNull { it.expense }
                val highestIncomeMonth = trend.maxByOrNull { it.income }
                val snapshot = buildString {
                    append("year=")
                    append(targetYear)
                    append(";income=")
                    append(monthly.totalIncome)
                    append(";expense=")
                    append(monthly.totalExpense)
                    append(";balance=")
                    append(monthly.totalIncome - monthly.totalExpense)
                    append(";topCategories=")
                    append(topCategories.joinToString("|") { "${it.name}:${it.amount}" })
                }
                val prompt = buildString {
                    append("请你作为专业财富分析师，用中文分析这一年的个人财务状况。要求：1) 先总结财富增长或亏损情况；2) 解释主要支出流向；3) 指出年度波动最大的月份；4) 给出3条具体建议；5) 用自然、简洁、可读的段落输出，不要使用JSON。\n\n")
                    append("年份：")
                    append(targetYear)
                    append("\n")
                    append("年度总收入：")
                    append(monthly.totalIncome)
                    append("\n")
                    append("年度总支出：")
                    append(monthly.totalExpense)
                    append("\n")
                    append("年度结余：")
                    append(monthly.totalIncome - monthly.totalExpense)
                    append("\n")
                    append("月度趋势：")
                    append(trend.joinToString("；") { "${it.month} 收入${it.income} 支出${it.expense}" })
                    append("\n")
                    append("主要支出分类：")
                    append(topCategories.joinToString("；") { "${it.name} ${it.amount} 占比${String.format("%.1f", it.percentage * 100)}%" })
                    append("\n")
                    append("最高支出月份：")
                    append(highestExpenseMonth?.month ?: "无")
                    append("\n")
                    append("最高收入月份：")
                    append(highestIncomeMonth?.month ?: "无")
                }
                val traceId = UUID.randomUUID().toString()
                val response = aiService.chat(
                    messages = listOf(
                        ChatMessage(MessageRole.SYSTEM, "你是严谨的财务分析助手，擅长总结年度收支趋势、主要亏损去向和可执行建议。"),
                        ChatMessage(MessageRole.USER, prompt)
                    ),
                    config = config
                )
                aiOperationTraceRepository.insertTrace(
                    com.example.aiaccounting.data.local.entity.AIOperationTrace(
                        id = UUID.randomUUID().toString(),
                        traceId = traceId,
                        sourceType = "AI_REMOTE",
                        actionType = "YEARLY_WEALTH_ANALYSIS",
                        entityType = "yearly_wealth_analysis",
                        entityId = targetYear.toString(),
                        summary = "生成年度财富分析",
                        details = snapshot,
                        success = true
                    )
                )
                yearlyWealthAnalysisRepository.upsert(
                    YearlyWealthAnalysis(
                        year = targetYear,
                        analysisText = response.trim(),
                        model = config.model.ifBlank { config.provider.name },
                        traceId = traceId,
                        snapshotJson = snapshot,
                        createdAt = yearlyAnalysis.value?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _yearlyAnalysisError.value = throwable.message ?: "年度 AI 分析失败"
            }
            _yearlyAnalysisLoading.value = false
        }
    }

    private fun calculateMonthlyStats(transactions: List<Transaction>, selectedYear: Int): MonthlyStats {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)

        // 年度统计
        val yearStart = Calendar.getInstance().apply {
            set(selectedYear, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearEnd = Calendar.getInstance().apply {
            set(selectedYear + 1, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearlyTransactions = transactions.filter { it.date in yearStart until yearEnd }
        val totalIncome = yearlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
        val totalExpense = yearlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

        // 月度统计
        val monthStart = Calendar.getInstance().apply {
            set(selectedYear, currentMonth, 1, 0, 0, 0)
        }.timeInMillis

        val monthEnd = Calendar.getInstance().apply {
            set(selectedYear, currentMonth + 1, 1, 0, 0, 0)
        }.timeInMillis

        val monthlyTransactions = transactions.filter { it.date in monthStart until monthEnd }
        val monthlyIncome = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
        val monthlyExpense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

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
    private fun calculateYearlyTrend(transactions: List<Transaction>, selectedYear: Int): List<MonthlyData> {
        val monthlyData = mutableListOf<MonthlyData>()

        for (month in 0..11) {
            val monthStart = Calendar.getInstance().apply {
                set(selectedYear, month, 1, 0, 0, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(selectedYear, month + 1, 1, 0, 0, 0)
            }.timeInMillis

            val monthTransactions = transactions.filter { it.date in monthStart until monthEnd }
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

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
        val income = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
        val expense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

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
        val income = weekTransactions.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
        val expense = weekTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

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

private fun selectedYearMonth(): Pair<Int, Int> {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.YEAR) to (calendar.get(Calendar.MONTH) + 1)
}
