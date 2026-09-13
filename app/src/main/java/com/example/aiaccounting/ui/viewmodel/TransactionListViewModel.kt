package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.logging.AppLogLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: com.example.aiaccounting.data.repository.CategoryRepository,
    private val appLogLogger: AppLogLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    val filteredTransactions = combine(
        _transactions,
        _uiState
    ) { transactions, state ->
        filterTransactions(transactions, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val monthlyStats = combine(
        _transactions,
        _uiState
    ) { transactions, state ->
        calculateMonthlyStats(transactions, state.selectedYear, state.selectedMonth)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySummary()
    )

    // 分类名称缓存，从 DB 加载
    private val _categoryMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, String>> = _categoryMap.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadTransactions()
        loadCategories()
    }

    fun logTransactionScreenEnter(theme: String, selectedView: String? = null) {
        appLogLogger.info(
            source = "UI",
            category = "screen_enter",
            message = "进入明细页",
            details = buildString {
                append("screen=TransactionList,theme=")
                append(theme)
                if (selectedView != null) {
                    append(",view=")
                    append(selectedView)
                }
            }
        )
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _categoryMap.value = categories.associate { it.id to it.name }
                _categories.value = categories
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            combine(_transactions, _categories, _uiState) { transactions, categories, state ->
                Triple(transactions, categories, state)
            }.collect { (transactions, categories, state) ->
                val visibleTransactions = filterTransactions(transactions, state)
                val hasAiTransactions = visibleTransactions.any { it.aiSourceType == "AI_LOCAL" || it.aiSourceType == "AI_REMOTE" }
                appLogLogger.debug(
                    source = "UI",
                    category = "transaction_list_refresh",
                    message = "明细交易列表刷新",
                    details = "transactions=${visibleTransactions.size},allTransactions=${transactions.size},categories=${categories.size},year=${state.selectedYear},month=${state.selectedMonth},typeFilter=${state.filterType},sourceFilter=${state.sourceFilter},sortBy=${state.sortBy},hasAiTransactions=$hasAiTransactions"
                )
            }
        }

        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { transactions ->
                _transactions.value = transactions
            }
        }
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        state: TransactionListUiState
    ): List<Transaction> {
        var filtered = transactions

        // 按月份筛选
        val calendar = Calendar.getInstance()
        filtered = filtered.filter { transaction ->
            calendar.timeInMillis = transaction.date
            calendar.get(Calendar.YEAR) == state.selectedYear &&
            calendar.get(Calendar.MONTH) + 1 == state.selectedMonth
        }

        // 按日期筛选
        if (state.selectedDate != null) {
            filtered = filtered.filter { transaction ->
                calendar.timeInMillis = transaction.date
                calendar.get(Calendar.DAY_OF_MONTH) == state.selectedDate
            }
        }

        // 按类型筛选
        filtered = when (state.filterType) {
            "income" -> filtered.filter { it.type == TransactionType.INCOME }
            "expense" -> filtered.filter { it.type == TransactionType.EXPENSE }
            else -> filtered
        }

        // 按来源筛选
        filtered = when (state.sourceFilter) {
            "ai" -> filtered.filter { it.aiSourceType == "AI_LOCAL" || it.aiSourceType == "AI_REMOTE" }
            "manual" -> filtered.filter { it.aiSourceType == "MANUAL" || it.aiSourceType.isBlank() }
            "remote_ai" -> filtered.filter { it.aiSourceType == "AI_REMOTE" }
            "local_ai" -> filtered.filter { it.aiSourceType == "AI_LOCAL" }
            else -> filtered
        }

        // 排序
        filtered = when (state.sortBy) {
            "amount" -> filtered.sortedByDescending { it.amount }
            else -> filtered.sortedByDescending { it.date }
        }

        return filtered
    }

    private fun calculateMonthlyStats(
        transactions: List<Transaction>,
        year: Int,
        month: Int
    ): MonthlySummary {
        val calendar = Calendar.getInstance()
        val monthlyTransactions = transactions.filter { transaction ->
            calendar.timeInMillis = transaction.date
            calendar.get(Calendar.YEAR) == year &&
            calendar.get(Calendar.MONTH) + 1 == month
        }

        val income = monthlyTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return MonthlySummary(income, expense)
    }

    fun prevMonth() {
        _uiState.update { state ->
            var newMonth = state.selectedMonth - 1
            var newYear = state.selectedYear
            if (newMonth < 1) {
                newMonth = 12
                newYear--
            }
            appLogLogger.info(
                source = "UI",
                category = "transaction_list_interaction",
                message = "切换明细月份",
                details = "action=prev_month,fromYear=${state.selectedYear},fromMonth=${state.selectedMonth},toYear=$newYear,toMonth=$newMonth"
            )
            state.copy(selectedMonth = newMonth, selectedYear = newYear, selectedDate = null)
        }
    }

    fun nextMonth() {
        _uiState.update { state ->
            var newMonth = state.selectedMonth + 1
            var newYear = state.selectedYear
            if (newMonth > 12) {
                newMonth = 1
                newYear++
            }
            appLogLogger.info(
                source = "UI",
                category = "transaction_list_interaction",
                message = "切换明细月份",
                details = "action=next_month,fromYear=${state.selectedYear},fromMonth=${state.selectedMonth},toYear=$newYear,toMonth=$newMonth"
            )
            state.copy(selectedMonth = newMonth, selectedYear = newYear, selectedDate = null)
        }
    }

    fun selectDate(date: Int?) {
        _uiState.update { state ->
            state.copy(selectedDate = if (date != null && state.selectedDate == date) null else date)
        }
    }

    fun setYearMonth(year: Int, month: Int) {
        require(month in 1..12) { "month must be between 1 and 12" }
        _uiState.update { state ->
            state.copy(selectedYear = year, selectedMonth = month, selectedDate = null)
        }
    }

    fun setFilterType(type: String) {
        appLogLogger.info(
            source = "UI",
            category = "transaction_list_interaction",
            message = "切换明细筛选类型",
            details = "filterType=$type"
        )
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSortBy(sortBy: String) {
        appLogLogger.info(
            source = "UI",
            category = "transaction_list_interaction",
            message = "切换明细排序",
            details = "sortBy=$sortBy"
        )
        _uiState.update { it.copy(sortBy = sortBy) }
    }

    fun setSourceFilter(sourceFilter: String) {
        appLogLogger.info(
            source = "UI",
            category = "transaction_list_interaction",
            message = "切换明细来源筛选",
            details = "sourceFilter=$sourceFilter"
        )
        _uiState.update { it.copy(sourceFilter = sourceFilter) }
    }

    fun buildTransactionDebugSummary(limit: Int = 20): String {
        return _transactions.value.take(limit).joinToString("\n") { transaction ->
            "id=${transaction.id},accountId=${transaction.accountId},categoryId=${transaction.categoryId},type=${transaction.type},amount=${transaction.amount},date=${transaction.date},note=${transaction.note}"
        }
    }

    /**
     * 获取分类名称（从缓存的分类 Map 中查找）
     */
    fun getCategoryName(categoryId: Long): String {
        return _categoryMap.value[categoryId] ?: "其他"
    }
}

data class TransactionListUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedDate: Int? = null,
    val filterType: String = "all",
    val sourceFilter: String = "all",
    val sortBy: String = "time",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MonthlySummary(
    val income: Double = 0.0,
    val expense: Double = 0.0
)

/**
 * 交易项UI数据 - 包含分类名称
 */
data class TransactionItemUiData(
    val transaction: Transaction,
    val categoryName: String = ""
)
