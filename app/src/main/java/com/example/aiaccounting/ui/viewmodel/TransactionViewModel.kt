package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for Transaction management
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    val transactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentTransactions = transactionRepository.getRecentTransactions(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadTransactions()
        loadMonthSummary()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { transactionList ->
                _uiState.update { it.copy(transactions = transactionList) }
            }
        }
    }

    private fun loadMonthSummary() {
        viewModelScope.launch {
            val (monthStart, monthEnd) = transactionRepository.getCurrentMonthRange()
            val income = transactionRepository.getTotalIncome(monthStart, monthEnd)
            val expense = transactionRepository.getTotalExpense(monthStart, monthEnd)
            
            _uiState.update { 
                it.copy(
                    monthIncome = income,
                    monthExpense = expense,
                    monthBalance = income - expense
                )
            }
        }
    }

    fun createTransaction(
        accountId: Long,
        categoryId: Long,
        type: TransactionType,
        amount: Double,
        date: Long,
        note: String = "",
        tags: String = ""
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val transaction = Transaction(
                    accountId = accountId,
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    date = date,
                    note = note,
                    tags = tags
                )
                
                transactionRepository.insertTransaction(transaction)
                _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
                loadMonthSummary() // Update summary
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                transactionRepository.updateTransaction(transaction)
                _uiState.update { it.copy(isLoading = false, showEditDialog = false) }
                loadMonthSummary() // Update summary
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
                loadMonthSummary() // Update summary
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsByType(type)
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionRepository.searchTransactions(query)
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, error = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(transaction: Transaction) {
        _uiState.update { it.copy(showEditDialog = true, editingTransaction = transaction, error = null) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingTransaction = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getMonthSummary(year: Int, month: Int): MonthSummary {
        val (start, end) = transactionRepository.getMonthRange(year, month)
        return MonthSummary(
            income = _uiState.value.monthIncome,
            expense = _uiState.value.monthExpense,
            balance = _uiState.value.monthBalance
        )
    }

    fun getYearSummary(year: Int): YearSummary {
        val (start, end) = transactionRepository.getYearRange(year)
        return YearSummary(
            year = year,
            income = 0.0, // Calculate from repository
            expense = 0.0, // Calculate from repository
            balance = 0.0
        )
    }
}

/**
 * UI State for Transaction screen
 */
data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTransaction: Transaction? = null,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthBalance: Double = 0.0
)

/**
 * Month summary data class
 */
data class MonthSummary(
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * Year summary data class
 */
data class YearSummary(
    val year: Int,
    val income: Double,
    val expense: Double,
    val balance: Double
)