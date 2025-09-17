package com.fleetmanager.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fleetmanager.ui.model.FilterContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation Manager following SOLID principles
 * Single Responsibility: Manages all navigation logic
 * Open/Closed: Extensible for new navigation patterns
 * Dependency Inversion: Depends on abstractions, not concrete implementations
 */
@Singleton
class NavigationManager @Inject constructor() {
    
    // Private mutable state
    private val _pendingFilterContext = MutableStateFlow<FilterContext?>(null)
    
    // Public read-only state
    val pendingFilterContext: StateFlow<FilterContext?> = _pendingFilterContext.asStateFlow()
    
    
    /**
     * Consume the pending filter context (one-time use)
     */
    fun consumePendingFilterContext(): FilterContext? {
        val context = _pendingFilterContext.value
        _pendingFilterContext.value = null
        return context
    }
    
    /**
     * Set pending filter context
     */
    fun setPendingFilterContext(filterContext: FilterContext) {
        _pendingFilterContext.value = filterContext
    }
    
    /**
     * Clear any pending filter context
     */
    fun clearPendingFilterContext() {
        _pendingFilterContext.value = null
    }
    
    /**
     * Check if there's a pending filter context
     */
    fun hasPendingFilterContext(): Boolean {
        return _pendingFilterContext.value != null
    }
}