package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ConversationRole
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.remote.AIRepository
import com.example.aiaccounting.data.remote.AIMessage
import com.example.aiaccounting.data.remote.ParsedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Assistant
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val conversationRepository: AIConversationRepository,
    private val aiRepository: AIRepository
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
        checkAIConfiguration()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllConversations().collect { conversationList ->
                _uiState.update { it.copy(conversations = conversationList) }
            }
        }
    }

    private fun checkAIConfiguration() {
        _uiState.update { it.copy(isAIConfigured = aiRepository.isConfigured()) }
    }

    fun configureAI(
        apiKey: String,
        baseUrl: String = "https://api.openai.com/v1/",
        model: String = "gpt-3.5-turbo",
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                aiRepository.configure(apiKey, baseUrl, model, temperature, maxTokens)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isAIConfigured = true,
                        showConfigDialog = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Save user message
                conversationRepository.addUserMessage(message)
                
                // Get recent conversations for context
                val recentConversations = conversationRepository.getRecentConversationsForContext(10)
                val messages = recentConversations.map { conv ->
                    AIMessage(
                        role = conv.role.name.lowercase(),
                        content = conv.content
                    )
                }
                
                // Send to AI
                val result = aiRepository.sendMessage(
                    messages = messages,
                    systemPrompt = aiRepository.getDefaultSystemPrompt()
                )
                
                result.fold(
                    onSuccess = { response ->
                        // Save assistant message
                        conversationRepository.addAssistantMessage(response)
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun parseTransaction(userInput: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isParsing = true, error = null) }
                
                val result = aiRepository.parseTransaction(userInput)
                
                result.fold(
                    onSuccess = { parsedTransaction ->
                        _uiState.update { 
                            it.copy(
                                isParsing = false,
                                parsedTransaction = parsedTransaction
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isParsing = false, error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isParsing = false, error = e.message) }
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

    fun clearParsedTransaction() {
        _uiState.update { it.copy(parsedTransaction = null) }
    }

    fun getAIConfiguration(): AIRepository.AIConfiguration? {
        return if (aiRepository.isConfigured()) {
            aiRepository.getConfiguration()
        } else {
            null
        }
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