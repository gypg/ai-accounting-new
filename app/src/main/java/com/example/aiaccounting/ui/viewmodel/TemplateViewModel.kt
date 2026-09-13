package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.TransactionTemplate
import com.example.aiaccounting.data.repository.TransactionTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TransactionTemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    val templates = templateRepository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            templateRepository.createDefaultTemplates()
        }
    }

    fun addTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            try {
                templateRepository.insertTemplate(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            try {
                templateRepository.updateTemplate(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            try {
                templateRepository.deleteTemplate(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class TemplateUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
