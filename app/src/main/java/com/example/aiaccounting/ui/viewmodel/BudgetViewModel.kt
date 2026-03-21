package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetProgress
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 预算ViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
  private val budgetRepository: BudgetRepository,
  private val categoryRepository: CategoryRepository
) : ViewModel() {

  private val calendar = Calendar.getInstance()
  private val _currentYear = MutableStateFlow(calendar.get(Calendar.YEAR))
  private val _currentMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1)

  val currentYear: StateFlow<Int> = _currentYear.asStateFlow()
  val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

  // 预算列表
  val budgets: StateFlow<List<BudgetProgress>> = combine(
    _currentYear,
    _currentMonth
  ) { year, month ->
    budgetRepository.getMonthlyBudgetProgress(year, month)
  }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // 总预算进度
  val totalBudget: StateFlow<BudgetProgress?> = combine(
    _currentYear,
    _currentMonth
  ) { year, month ->
    budgetRepository.getTotalBudgetProgress(year, month)
  }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // 需要提醒的预算
  val alertBudgets: StateFlow<List<BudgetProgress>> = combine(
    _currentYear,
    _currentMonth
  ) { year, month ->
    budgetRepository.getAlertBudgets(year, month)
  }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // 分类列表（用于创建分类预算）
  val expenseCategories = categoryRepository.getExpenseCategories()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // UI状态
  private val _uiState = MutableStateFlow(BudgetUiState())
  val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

  /**
   * 设置当前年月
   */
  fun setYearMonth(year: Int, month: Int) {
    _currentYear.value = year
    _currentMonth.value = month
  }

  /**
   * 创建总预算
   */
  fun createTotalBudget(amount: Double, alertThreshold: Double = 0.8) {
    viewModelScope.launch {
      try {
        val budget = Budget(
          name = "${_currentYear.value}年${_currentMonth.value}月总预算",
          amount = amount,
          categoryId = null,
          period = com.example.aiaccounting.data.local.entity.BudgetPeriod.MONTHLY,
          year = _currentYear.value,
          month = _currentMonth.value,
          alertThreshold = alertThreshold
        )
        budgetRepository.insertBudget(budget)
        _uiState.update { it.copy(
          message = "总预算创建成功",
          isSuccess = true
        ) }
      } catch (e: Exception) {
        _uiState.update { it.copy(
          message = "创建失败: ${e.message}",
          isSuccess = false
        ) }
      }
    }
  }

  /**
   * 创建分类预算
   */
  fun createCategoryBudget(
    categoryId: Long,
    categoryName: String,
    amount: Double,
    alertThreshold: Double = 0.8
  ) {
    viewModelScope.launch {
      try {
        val budget = Budget(
          name = "${categoryName}预算",
          amount = amount,
          categoryId = categoryId,
          period = com.example.aiaccounting.data.local.entity.BudgetPeriod.MONTHLY,
          year = _currentYear.value,
          month = _currentMonth.value,
          alertThreshold = alertThreshold
        )
        budgetRepository.insertBudget(budget)
        _uiState.update { it.copy(
          message = "分类预算创建成功",
          isSuccess = true
        ) }
      } catch (e: Exception) {
        _uiState.update { it.copy(
          message = "创建失败: ${e.message}",
          isSuccess = false
        ) }
      }
    }
  }

  /**
   * 更新预算
   */
  fun updateBudget(budget: Budget) {
    viewModelScope.launch {
      try {
        budgetRepository.updateBudget(budget)
        _uiState.update { it.copy(
          message = "预算更新成功",
          isSuccess = true
        ) }
      } catch (e: Exception) {
        _uiState.update { it.copy(
          message = "更新失败: ${e.message}",
          isSuccess = false
        ) }
      }
    }
  }

  /**
   * 删除预算
   */
  fun deleteBudget(budget: Budget) {
    viewModelScope.launch {
      try {
        budgetRepository.deleteBudget(budget)
        _uiState.update { it.copy(
          message = "预算删除成功",
          isSuccess = true
        ) }
      } catch (e: Exception) {
        _uiState.update { it.copy(
          message = "删除失败: ${e.message}",
          isSuccess = false
        ) }
      }
    }
  }

  /**
   * 清除消息
   */
  fun clearMessage() {
    _uiState.update { it.copy(message = null) }
  }
}

/**
 * 预算UI状态
 */
data class BudgetUiState(
  val isLoading: Boolean = false,
  val message: String? = null,
  val isSuccess: Boolean = false
)
