package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Account management
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    val accounts = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accountList ->
                _uiState.update { it.copy(accounts = accountList) }
            }
        }
    }

    fun createAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        icon: String,
        color: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val account = Account(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    icon = icon,
                    color = color,
                    isDefault = accounts.value.flatten().isEmpty() // First account is default
                )
                
                accountRepository.insertAccount(account)
                _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                accountRepository.updateAccount(account)
                _uiState.update { it.copy(isLoading = false, showEditDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                accountRepository.deleteAccount(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun archiveAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                accountRepository.archiveAccount(accountId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setDefaultAccount(account: Account) {
        viewModelScope.launch {
            try {
                accountRepository.updateAccount(account.copy(isDefault = true))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateBalance(accountId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                accountRepository.updateBalance(accountId, amount)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, error = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(account: Account) {
        _uiState.update { it.copy(showEditDialog = true, editingAccount = account, error = null) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingAccount = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun getTotalBalance(): Double {
        return accountRepository.getTotalBalance()
    }
}

/**
 * UI State for Account screen
 */
data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: Account? = null
)