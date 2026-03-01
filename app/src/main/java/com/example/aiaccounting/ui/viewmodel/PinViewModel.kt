package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for PIN authentication
 */
@HiltViewModel
class PinViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _remainingLockTime = MutableStateFlow(0L)
    val remainingLockTime: StateFlow<Long> = _remainingLockTime.asStateFlow()

    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()

    init {
        checkPinStatus()
        startLockTimer()
    }

    private fun checkPinStatus() {
        _isPinSet.value = securityManager.isPinSet()
        _failedAttempts.value = securityManager.getFailedAttempts()
        _isLocked.value = securityManager.isLocked()
        _remainingLockTime.value = securityManager.getRemainingLockTime()
    }

    /**
     * Setup PIN for the first time
     */
    fun setupPin(pin: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = securityManager.setupPin(pin)
            if (success) {
                _isPinSet.value = true
                _failedAttempts.value = 0
                _currentPin.value = pin
            }
            onComplete(success)
        }
    }

    /**
     * Validate PIN
     */
    fun validatePin(pin: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = securityManager.validatePin(pin)
            checkPinStatus()
            if (success) {
                _currentPin.value = pin
            }
            onComplete(success)
        }
    }

    /**
     * Change PIN
     */
    fun changePin(oldPin: String, newPin: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = securityManager.changePin(oldPin, newPin)
            onComplete(success)
        }
    }

    /**
     * Start lock timer to check if lock period has expired
     */
    private fun startLockTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000) // Check every second
                if (_isLocked.value) {
                    val remaining = securityManager.getRemainingLockTime()
                    _remainingLockTime.value = remaining
                    
                    if (remaining == 0L) {
                        _isLocked.value = false
                        _failedAttempts.value = 0
                    }
                }
            }
        }
    }

    /**
     * Refresh lock status
     */
    fun refreshLockStatus() {
        checkPinStatus()
    }
}