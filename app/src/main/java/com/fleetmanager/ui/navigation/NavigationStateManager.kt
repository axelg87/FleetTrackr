@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized Navigation State Manager
 * 
 * Single source of truth for navigation state using StateFlow<Int> for currentPageIndex.
 * Handles both bottom navigation taps and horizontal swipe gestures with proper synchronization.
 * 
 * Architecture:
 * - StateFlow<Int> as the only source of truth for current page
 * - Bidirectional sync between BottomNav and Pager
 * - Clean separation of concerns with SOLID principles
 */
@Stable
class NavigationStateManager(
    private val bottomNavItems: List<BottomNavItem>,
    private val navigationState: NavigationState
) {
    
    // Single source of truth for current page index
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    // Internal flags to prevent navigation loops
    private var isUpdatingFromPager = false
    private var isUpdatingFromBottomNav = false
    
    /**
     * Update page index (called by PagerState changes)
     */
    fun updatePage(index: Int) {
        if (!isUpdatingFromBottomNav && index in bottomNavItems.indices && index != _currentPageIndex.value) {
            isUpdatingFromPager = true
            _currentPageIndex.value = index
            
            // Navigate to the corresponding route
            val targetRoute = bottomNavItems[index].screen.route
            navigationState.navigateTo(targetRoute)
            
            isUpdatingFromPager = false
        }
    }
    
    /**
     * Handle bottom navigation click
     */
    fun onBottomNavClick(route: String) {
        val targetIndex = bottomNavItems.indexOfFirst { it.screen.route == route }
        if (targetIndex >= 0 && targetIndex != _currentPageIndex.value && !isUpdatingFromPager) {
            isUpdatingFromBottomNav = true
            _currentPageIndex.value = targetIndex
            navigationState.navigateTo(route)
            isUpdatingFromBottomNav = false
        }
    }
    
    /**
     * Sync with external navigation changes (when user navigates via other means)
     */
    fun syncWithRoute(currentRoute: String?) {
        val routeIndex = bottomNavItems.indexOfFirst { it.screen.route == currentRoute }
        if (routeIndex >= 0 && routeIndex != _currentPageIndex.value && !isUpdatingFromPager && !isUpdatingFromBottomNav) {
            _currentPageIndex.value = routeIndex
        }
    }
    
    /**
     * Get total number of pages
     */
    val pageCount: Int get() = bottomNavItems.size
    
    /**
     * Check if the given route is swipeable
     */
    fun isSwipeableRoute(route: String?): Boolean {
        return bottomNavItems.any { it.screen.route == route }
    }
    
    /**
     * Get route for a given page index
     */
    fun getRouteForPage(index: Int): String? {
        return bottomNavItems.getOrNull(index)?.screen?.route
    }
}

/**
 * Remember NavigationStateManager
 */
@Composable
fun rememberNavigationStateManager(
    bottomNavItems: List<BottomNavItem>,
    navigationState: NavigationState
): NavigationStateManager {
    return remember(bottomNavItems, navigationState) {
        NavigationStateManager(bottomNavItems, navigationState)
    }
}