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
     * Navigate to Reports screen with filter context
     * This method ensures both navigation and filter application work together
     */
    fun navigateToReportsWithFilter(
        navController: NavHostController,
        filterContext: FilterContext,
        bottomNavItems: List<BottomNavItem>,
        onPagerNavigate: (Int) -> Unit
    ) {
        // Set the filter context first
        _pendingFilterContext.value = filterContext
        
        // Find the Reports tab index
        val reportsIndex = bottomNavItems.indexOfFirst { it.screen == Screen.Reports }
        
        if (reportsIndex >= 0) {
            // Navigate using pager to ensure UI consistency
            onPagerNavigate(reportsIndex)
        } else {
            // Fallback to direct navigation if Reports tab not found
            navController.navigate(Screen.Reports.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    /**
     * Navigate to a specific tab by route
     * DRY principle: Single method for tab navigation
     */
    fun navigateToTab(
        route: String,
        navController: NavHostController,
        bottomNavItems: List<BottomNavItem>,
        onPagerNavigate: (Int) -> Unit
    ) {
        val tabIndex = bottomNavItems.indexOfFirst { it.screen.route == route }
        
        if (tabIndex >= 0) {
            // Use pager navigation for tabs
            onPagerNavigate(tabIndex)
        } else {
            // Fallback for non-tab routes
            navController.navigate(route)
        }
    }
    
    /**
     * Consume the pending filter context (one-time use)
     */
    fun consumePendingFilterContext(): FilterContext? {
        val context = _pendingFilterContext.value
        _pendingFilterContext.value = null
        return context
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