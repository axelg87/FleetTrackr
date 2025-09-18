package com.fleetmanager.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-grade Navigation State Singleton
 * Single source of truth for navigation state using StateFlow<Int>
 * 
 * This is the ONLY component allowed to mutate the current page index.
 * All navigation (swipe + tab click) must update this shared state.
 */
@Singleton
class NavigationState @Inject constructor() {
    
    // Private mutable state - only this class can modify it
    private val _currentPageIndex = MutableStateFlow(0)
    
    // Public read-only state for observers
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    /**
     * Update the current page index
     * This is the single point of mutation for navigation state
     */
    fun updatePageIndex(index: Int) {
        if (index >= 0 && _currentPageIndex.value != index) {
            _currentPageIndex.value = index
        }
    }
    
    /**
     * Get current page index value
     */
    fun getCurrentPageIndex(): Int = _currentPageIndex.value
}