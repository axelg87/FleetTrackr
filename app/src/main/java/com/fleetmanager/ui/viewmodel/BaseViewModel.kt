package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 * Implements common patterns like loading states and error handling.
 */
abstract class BaseViewModel<T> : ViewModel() {
    
    protected val _uiState = MutableStateFlow(getInitialState())
    val uiState: StateFlow<T> = _uiState.asStateFlow()
    
    /**
     * Get the initial state for this ViewModel
     */
    protected abstract fun getInitialState(): T
    
    /**
     * Execute a suspend function safely with error handling
     */
    protected fun executeAsync(
        onLoading: ((Boolean) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                onLoading?.invoke(true)
                block()
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Unknown error occurred")
            } finally {
                onLoading?.invoke(false)
            }
        }
    }
    
    /**
     * Update the UI state safely
     */
    protected fun updateState(update: (T) -> T) {
        _uiState.value = update(_uiState.value)
    }
}