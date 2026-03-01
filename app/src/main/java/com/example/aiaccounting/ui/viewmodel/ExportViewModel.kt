package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.exporter.ExcelExporter
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for Export functionality
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    val transactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadExportOptions()
    }

    private fun loadExportOptions() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1

            _uiState.update { 
                it.copy(
                    selectedYear = currentYear,
                    selectedMonth = currentMonth,
                    availableYears = (2020..currentYear).toList().reversed(),
                    availableMonths = (1..12).toList()
                )
            }
        }
    }

    /**
     * Export all transactions
     */
    fun exportAllTransactions(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null) }
                
                val allTransactions = transactionRepository.getAllTransactionsList()
                
                if (allTransactions.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, error = "暂无交易记录") }
                    onResult(Result.failure(Exception("暂无交易记录")))
                    return@launch
                }

                val result = excelExporter.exportTransactions(
                    transactions = allTransactions,
                    fileName = "all_transactions_${System.currentTimeMillis()}.xlsx"
                )

                _uiState.update { it.copy(isExporting = false) }
                onResult(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Export current month transactions
     */
    fun exportCurrentMonth(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null) }
                
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                
                exportMonth(year, month, onResult)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Export specific month transactions
     */
    fun exportMonth(year: Int, month: Int, onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null) }
                
                val (monthStart, monthEnd) = transactionRepository.getMonthRange(year, month)
                val monthTransactions = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)
                    .first()
                
                if (monthTransactions.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, error = "${year}年${month}月暂无交易记录") }
                    onResult(Result.failure(Exception("${year}年${month}月暂无交易记录")))
                    return@launch
                }

                val totalIncome = transactionRepository.getTotalIncome(monthStart, monthEnd)
                val totalExpense = transactionRepository.getTotalExpense(monthStart, monthEnd)

                val result = excelExporter.exportMonthlySummary(
                    year = year,
                    month = month,
                    transactions = monthTransactions,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    fileName = "monthly_summary_${year}_${month}.xlsx"
                )

                _uiState.update { it.copy(isExporting = false) }
                onResult(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Export custom date range transactions
     */
    fun exportDateRange(startDate: Long, endDate: Long, onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null) }
                
                val rangeTransactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
                    .first()
                
                if (rangeTransactions.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, error = "该时间段暂无交易记录") }
                    onResult(Result.failure(Exception("该时间段暂无交易记录")))
                    return@launch
                }

                val totalIncome = transactionRepository.getTotalIncome(startDate, endDate)
                val totalExpense = transactionRepository.getTotalExpense(startDate, endDate)

                val result = excelExporter.exportTransactions(
                    transactions = rangeTransactions,
                    fileName = "transactions_${startDate}_${endDate}.xlsx"
                )

                _uiState.update { it.copy(isExporting = false) }
                onResult(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Export by transaction type
     */
    fun exportByType(type: TransactionType, onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null) }
                
                val typeTransactions = transactionRepository.getTransactionsByType(type)
                    .first()
                
                if (typeTransactions.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, error = "暂无${type.name}记录") }
                    onResult(Result.failure(Exception("暂无${type.name}记录")))
                    return@launch
                }

                val result = excelExporter.exportTransactions(
                    transactions = typeTransactions,
                    fileName = "${type.name}_transactions_${System.currentTimeMillis()}.xlsx"
                )

                _uiState.update { it.copy(isExporting = false) }
                onResult(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
                onResult(Result.failure(e))
            }
        }
    }

    fun setSelectedYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }

    fun setSelectedMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun setSelectedExportType(type: ExportType) {
        _uiState.update { it.copy(selectedExportType = type) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Export screen
 */
data class ExportUiState(
    val isExporting: Boolean = false,
    val error: String? = null,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val availableYears: List<Int> = emptyList(),
    val availableMonths: List<Int> = emptyList(),
    val selectedExportType: ExportType = ExportType.CURRENT_MONTH,
    val exportedFile: File? = null
)

/**
 * Export type options
 */
enum class ExportType {
    ALL_TRANSACTIONS,
    CURRENT_MONTH,
    SPECIFIC_MONTH,
    CUSTOM_RANGE,
    INCOME_ONLY,
    EXPENSE_ONLY
}