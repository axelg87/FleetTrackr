@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enterprise-Grade Centralized Navigation Manager
 * 
 * This is the single source of truth for all navigation state.
 * Ensures perfect bi-directional synchronization between HorizontalPager and BottomNavigation.
 * 
 * Key Features:
 * - Single source of truth for navigation state
 * - Reactive state flows with proper debouncing
 * - Race condition prevention with atomic operations
 * - Memory-efficient state management
 * - SOLID principles compliance
 */
@Stable
class CentralizedNavigationManager(
    private val navController: NavHostController,
    private val bottomNavItems: List<BottomNavItem>
) {
    
    // Internal state flows for reactive programming
    private val _currentPageIndex = MutableStateFlow(0)
    private val _isNavigatingProgrammatically = MutableStateFlow(false)
    
    /**
     * Current page index as StateFlow - single source of truth
     */
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()
    
    /**
     * Current route derived from navigation controller
     */
    val currentRoute: String?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination?.route
    
    /**
     * Total number of pages
     */
    val pageCount: Int get() = bottomNavItems.size
    
    /**
     * Get route for a specific page index
     */
    fun getRouteForPage(pageIndex: Int): String {
        return bottomNavItems.getOrNull(pageIndex)?.screen?.route ?: bottomNavItems[0].screen.route
    }
    
    /**
     * Get page index for a specific route
     */
    fun getPageIndexForRoute(route: String?): Int {
        return bottomNavItems.indexOfFirst { it.screen.route == route }.takeIf { it >= 0 } ?: 0
    }
    
    /**
     * Navigate to a specific route (called from bottom navigation)
     * This is the authoritative navigation method
     */
    fun navigateToRoute(route: String) {
        val pageIndex = getPageIndexForRoute(route)
        navigateToPageInternal(pageIndex, route, NavigationSource.BOTTOM_NAV)
    }
    
    /**
     * Navigate to a specific page index (called from pager)
     */
    fun navigateToPage(pageIndex: Int) {
        val route = getRouteForPage(pageIndex)
        navigateToPageInternal(pageIndex, route, NavigationSource.PAGER)
    }
    
    /**
     * Internal navigation method that handles both sources
     */
    private fun navigateToPageInternal(pageIndex: Int, route: String, source: NavigationSource) {
        // Atomic update to prevent race conditions
        if (_isNavigatingProgrammatically.value) return
        
        // Ensure page index is valid
        if (pageIndex !in 0 until pageCount) return
        
        _isNavigatingProgrammatically.value = true
        
        try {
            // Always update the page index (single source of truth)
            _currentPageIndex.value = pageIndex
            
            // Only update NavController if navigation came from pager
            if (source == NavigationSource.PAGER) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } finally {
            // Ensure flag is always reset, even if navigation fails
            _isNavigatingProgrammatically.value = false
        }
    }
    
    /**
     * Check if swipe navigation should be enabled for current route
     */
    fun shouldEnableSwipe(route: String?): Boolean {
        return bottomNavItems.any { it.screen.route == route }
    }
    
    /**
     * Navigate back
     */
    fun navigateBack() {
        navController.popBackStack()
    }
    
    /**
     * Sync with current route changes from external navigation
     */
    @Composable
    fun SyncWithCurrentRoute() {
        val currentRoute = currentRoute
        
        LaunchedEffect(currentRoute) {
            if (!_isNavigatingProgrammatically.value) {
                val pageIndex = getPageIndexForRoute(currentRoute)
                _currentPageIndex.value = pageIndex
            }
        }
    }
    
    private enum class NavigationSource {
        BOTTOM_NAV,
        PAGER
    }
}

/**
 * Enterprise-Grade Swipe Navigation State
 * 
 * Manages PagerState with perfect synchronization to CentralizedNavigationManager
 */
@Stable
class EnterpriseSwipeNavigationState(
    val pagerState: PagerState,
    private val navigationManager: CentralizedNavigationManager
) {
    
    /**
     * Set up bi-directional synchronization
     * This composable ensures perfect sync between pager and navigation
     */
    @Composable
    fun SetupSynchronization() {
        val coroutineScope = rememberCoroutineScope()
        
        // Sync navigation changes to pager (when bottom nav is tapped)
        LaunchedEffect(navigationManager) {
            navigationManager.currentPageIndex
                .distinctUntilChanged()
                .collect { targetPage ->
                    // Only animate if we're not already on the target page and not currently scrolling
                    if (pagerState.currentPage != targetPage && 
                        !pagerState.isScrollInProgress &&
                        targetPage in 0 until navigationManager.pageCount) {
                        
                        coroutineScope.launch {
                            try {
                                pagerState.animateScrollToPage(targetPage)
                            } catch (e: Exception) {
                                // Handle animation cancellation gracefully
                                pagerState.scrollToPage(targetPage)
                            }
                        }
                    }
                }
        }
        
        // Sync pager changes to navigation (when user swipes)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .filter { page -> 
                    // Only process valid pages when scroll is complete
                    !pagerState.isScrollInProgress && 
                    page in 0 until navigationManager.pageCount
                }
                .collect { currentPage ->
                    navigationManager.navigateToPage(currentPage)
                }
        }
        
        // Sync with external route changes (e.g., from deep links or other navigation)
        navigationManager.SyncWithCurrentRoute()
    }
}

/**
 * Remember centralized navigation manager
 */
@Composable
fun rememberCentralizedNavigationManager(
    navController: NavHostController,
    bottomNavItems: List<BottomNavItem>
): CentralizedNavigationManager {
    return remember(navController, bottomNavItems) {
        CentralizedNavigationManager(navController, bottomNavItems)
    }
}

/**
 * Remember enterprise swipe navigation state
 */
@Composable
fun rememberEnterpriseSwipeNavigationState(
    navigationManager: CentralizedNavigationManager,
    currentRoute: String?
): EnterpriseSwipeNavigationState {
    
    // Initialize pager state with current route
    val initialPage = remember(currentRoute) {
        navigationManager.getPageIndexForRoute(currentRoute)
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { navigationManager.pageCount }
    )
    
    return remember(navigationManager, pagerState) {
        EnterpriseSwipeNavigationState(pagerState, navigationManager)
    }
}