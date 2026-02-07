package com.ez2bg.anotherthread.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks API connection status for displaying connectivity issues to users.
 */
object ConnectionStateHolder {
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _failureCount = MutableStateFlow(0)
    val failureCount: StateFlow<Int> = _failureCount.asStateFlow()

    /**
     * Record a successful API call - clears error state.
     */
    fun recordSuccess() {
        _isConnected.value = true
        _lastError.value = null
        _failureCount.value = 0
    }

    /**
     * Record a failed API call.
     */
    fun recordFailure(error: Throwable) {
        _failureCount.value++
        _lastError.value = error.message ?: "Connection failed"

        // After 2 consecutive failures, mark as disconnected
        if (_failureCount.value >= 2) {
            _isConnected.value = false
        }

        // Log the error
        println("API Error [${_failureCount.value}]: ${error.message}")
    }
}
