package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
  val isBiometricEnabled: Boolean = false,
  val isPinSet: Boolean = false,
  val isAIConfigured: Boolean = false,
  val aiModel: String = "",
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for Settings Screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val securityManager: SecurityManager
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    loadSettings()
  }

  private fun loadSettings() {
    viewModelScope.launch {
      _uiState.update { it.copy(
        isBiometricEnabled = securityManager.isBiometricEnabled(),
        isPinSet = securityManager.getPinState() is SecurityManager.PinState.Set
      ) }
    }
  }

  /**
   * Toggle biometric authentication
   */
  fun toggleBiometric(enabled: Boolean) {
    viewModelScope.launch {
      securityManager.setBiometricEnabled(enabled)
      _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }
  }

  /**
   * Change PIN
   */
  fun changePin(oldPin: String, newPin: String, onComplete: (Boolean) -> Unit) {
    viewModelScope.launch {
      val success = securityManager.changePin(oldPin, newPin)
      if (success) {
        _uiState.update { it.copy(isPinSet = true) }
      }
      onComplete(success)
    }
  }

  /**
   * Clear PIN
   */
  fun clearPin() {
    viewModelScope.launch {
      val success = securityManager.clearPin()
      if (success) {
        _uiState.update { it.copy(isPinSet = false) }
      }
    }
  }

  /**
   * Refresh PIN status
   */
  fun refreshPinStatus() {
    viewModelScope.launch {
      _uiState.update { it.copy(
        isPinSet = securityManager.getPinState() is SecurityManager.PinState.Set
      ) }
    }
  }

  /**
   * Clear error
   */
  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }
}
