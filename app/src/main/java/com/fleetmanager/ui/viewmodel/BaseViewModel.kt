package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
    
    // Track active jobs for cleanup
    private val activeJobs = mutableListOf<Job>()
    
    /**
     * Get the initial state for this ViewModel
     */
    protected abstract fun getInitialState(): T
    
    /**
     * Reset the ViewModel to its initial state
     * Called when user authentication state changes
     */
    protected open fun resetToInitialState() {
        // Cancel all active jobs first
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        
        // Reset state to initial
        _uiState.value = getInitialState()
    }
    
    /**
     * Execute a suspend function safely with error handling
     */
    protected fun executeAsync(
        onLoading: ((Boolean) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        val job = viewModelScope.launch {
            try {
                onLoading?.invoke(true)
                block()
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Unknown error occurred")
            } finally {
                onLoading?.invoke(false)
            }
        }
        activeJobs.add(job)
        return job
    }
    
    /**
     * Update the UI state safely
     */
    protected fun updateState(update: (T) -> T) {
        _uiState.value = update(_uiState.value)
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel all active jobs (including Firestore listeners)
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
    }
}