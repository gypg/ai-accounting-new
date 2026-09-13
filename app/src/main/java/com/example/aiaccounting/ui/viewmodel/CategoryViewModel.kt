package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Category management
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incomeCategories = categoryRepository.getIncomeCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenseCategories = categoryRepository.getExpenseCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categoryList ->
                _uiState.update { it.copy(categories = categoryList) }
            }
        }
    }

    fun createCategory(
        name: String,
        type: TransactionType,
        icon: String,
        color: String,
        parentId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val category = Category(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    parentId = parentId,
                    order = categories.value.size
                )
                
                categoryRepository.insertCategory(category)
                _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                categoryRepository.updateCategory(category)
                _uiState.update { it.copy(isLoading = false, showEditDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog(type: TransactionType? = null) {
        _uiState.update { it.copy(showAddDialog = true, selectedType = type, error = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, selectedType = null) }
    }

    fun showEditDialog(category: Category) {
        _uiState.update { it.copy(showEditDialog = true, editingCategory = category, error = null) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingCategory = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getCategoriesByType(type: TransactionType): List<Category> {
        return categories.value.filter { it.type == type }
    }
}

/**
 * UI State for Category screen
 */
data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCategory: Category? = null,
    val selectedType: TransactionType? = null
)