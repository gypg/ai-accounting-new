package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import com.example.aiaccounting.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Budget management
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    val budgets = budgetRepository.getAllBudgets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Add a new budget
     */
    fun addBudget(categoryId: Long, amount: Double, period: BudgetPeriod) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val budget = Budget(
                    categoryId = categoryId,
                    amount = amount,
                    period = period,
                    spent = 0.0
                )
                
                budgetRepository.insertBudget(budget)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Edit an existing budget
     */
    fun editBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                budgetRepository.updateBudget(budget)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Delete a budget
     */
    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                budgetRepository.deleteBudgetById(budgetId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Update budget spent amount
     */
    fun updateBudgetSpent(budgetId: Long, spent: Double) {
        viewModelScope.launch {
            try {
                budgetRepository.updateBudgetSpent(budgetId, spent)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Get budget by category
     */
    fun getBudgetByCategory(categoryId: Long): Flow<Budget?> {
        return budgetRepository.getBudgetByCategory(categoryId)
    }

    /**
     * Check if category has budget
     */
    fun hasBudgetForCategory(categoryId: Long): Flow<Boolean> {
        return budgetRepository.hasBudgetForCategory(categoryId)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Budget screen
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingBudget: Budget? = null
)
