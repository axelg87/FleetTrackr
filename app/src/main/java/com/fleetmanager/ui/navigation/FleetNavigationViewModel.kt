package com.fleetmanager.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.fleetmanager.ui.model.FilterContext
import com.fleetmanager.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation ViewModel following SOLID principles
 * Manages navigation state and coordinates between different navigation methods
 */
@HiltViewModel
class FleetNavigationViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {
    
    // Current pager page index
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    // Navigation state to prevent circular updates
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()
    
    // Pending filter context from navigation manager
    val pendingFilterContext: StateFlow<FilterContext?> = navigationManager.pendingFilterContext
    
    /**
     * Navigate to Reports with filter context
     * This is the main method called from dashboard tiles
     */
    fun navigateToReportsWithFilter(
        navController: NavHostController,
        filterContext: FilterContext,
        bottomNavItems: List<BottomNavItem>
    ) {
        if (_isNavigating.value) return
        
        viewModelScope.launch {
            _isNavigating.value = true
            
            // Set the filter context first
            navigationManager.setPendingFilterContext(filterContext)
            
            // Find the Reports tab index
            val reportsIndex = bottomNavItems.indexOfFirst { it.screen == Screen.Reports }
            
            if (reportsIndex >= 0) {
                // Update page index immediately for UI consistency
                _currentPageIndex.value = reportsIndex
                
                // Navigate to Reports route
                navController.navigate(Screen.Reports.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            
            _isNavigating.value = false
        }
    }
    
    /**
     * Navigate to a specific tab
     */
    fun navigateToTab(
        route: String,
        navController: NavHostController,
        bottomNavItems: List<BottomNavItem>
    ) {
        if (_isNavigating.value) return
        
        viewModelScope.launch {
            _isNavigating.value = true
            
            val tabIndex = bottomNavItems.indexOfFirst { it.screen.route == route }
            
            if (tabIndex >= 0) {
                // Update page index immediately for UI consistency
                _currentPageIndex.value = tabIndex
                
                // Navigate to the route
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            
            _isNavigating.value = false
        }
    }
    
    /**
     * Update current page index (called by pager)
     */
    fun updateCurrentPageIndex(index: Int) {
        if (!_isNavigating.value) {
            _currentPageIndex.value = index
        }
    }
    
    /**
     * Set navigating state - used to prevent circular navigation updates
     */
    fun setNavigating(isNavigating: Boolean) {
        _isNavigating.value = isNavigating
    }
    
    /**
     * Consume pending filter context
     */
    fun consumePendingFilterContext(): FilterContext? {
        return navigationManager.consumePendingFilterContext()
    }
    
    /**
     * Clear any pending filter context
     */
    fun clearPendingFilterContext() {
        navigationManager.clearPendingFilterContext()
    }
}