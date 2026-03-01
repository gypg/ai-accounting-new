package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.repository.AIConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Assistant
 * Note: AI service integration temporarily disabled
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val conversationRepository: AIConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    val conversations = conversationRepository.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { conversationList ->
                _uiState.update { it.copy(conversations = conversationList) }
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Save user message
                conversationRepository.addUserMessage(message)

                // AI service temporarily disabled - just show a placeholder response
                conversationRepository.addAssistantMessage(
                    "AI service is temporarily unavailable. This is a placeholder response."
                )

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearConversations() {
        viewModelScope.launch {
            try {
                conversationRepository.clearAllConversations()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun bookmarkConversation(conversationId: Long, isBookmarked: Boolean) {
        viewModelScope.launch {
            try {
                conversationRepository.updateBookmarkStatus(conversationId, isBookmarked)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true, error = null) }
    }

    fun hideConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for AI Assistant screen
 */
data class AIAssistantUiState(
    val conversations: List<AIConversation> = emptyList(),
    val isLoading: Boolean = false,
    val isParsing: Boolean = false,
    val error: String? = null,
    val isAIConfigured: Boolean = false,
    val showConfigDialog: Boolean = false,
    val parsedTransaction: ParsedTransaction? = null
)

/**
 * Parsed transaction data class (placeholder)
 */
data class ParsedTransaction(
    val amount: Double? = null,
    val category: String? = null,
    val description: String? = null,
    val date: Long? = null,
    val type: String? = null
)
