package com.fleetmanager.ui.navigation

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized Navigation State Manager
 * 
 * Single source of truth for horizontal swipe navigation using StateFlow.
 * Manages bidirectional synchronization between swipe gestures and bottom navigation tabs.
 */
@Stable
class NavigationStateManager(
    private val bottomNavItems: List<BottomNavItem>
) {
    
    // Single source of truth - current page index
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    /**
     * Called when user swipes to a different page
     */
    fun onSwipeChanged(index: Int) {
        if (index in bottomNavItems.indices && index != _currentPageIndex.value) {
            _currentPageIndex.value = index
        }
    }
    
    /**
     * Called when user taps a bottom navigation tab
     */
    fun onTabClicked(index: Int) {
        if (index in bottomNavItems.indices && index != _currentPageIndex.value) {
            _currentPageIndex.value = index
        }
    }
    
    /**
     * Get the current route based on page index
     */
    fun getCurrentRoute(): String {
        val currentIndex = _currentPageIndex.value
        return if (currentIndex in bottomNavItems.indices) {
            bottomNavItems[currentIndex].screen.route
        } else {
            bottomNavItems[0].screen.route
        }
    }
    
    /**
     * Set current page by route (for initialization)
     */
    fun setCurrentPageByRoute(route: String?) {
        val index = bottomNavItems.indexOfFirst { it.screen.route == route }
        if (index >= 0 && index != _currentPageIndex.value) {
            _currentPageIndex.value = index
        }
    }
    
    /**
     * Get total number of pages
     */
    val pageCount: Int get() = bottomNavItems.size
    
    /**
     * Check if route is swipeable
     */
    fun isSwipeableRoute(route: String?): Boolean {
        return bottomNavItems.any { it.screen.route == route }
    }
}