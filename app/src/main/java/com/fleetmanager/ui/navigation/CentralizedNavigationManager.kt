@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Clean Navigation State Manager
 * 
 * Single source of truth with proper separation of concerns:
 * - UI State: What the user sees (current page index)
 * - Navigation Events: User actions (swipe, tap)
 * - Side Effects: NavController updates, pager animations
 */
@Stable
class CentralizedNavigationManager(
    private val navController: NavHostController,
    private val bottomNavItems: List<BottomNavItem>
) {
    
    // SINGLE SOURCE OF TRUTH
    private val _currentPageIndex = MutableStateFlow(0)
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
     * SINGLE UPDATE ENTRYPOINT
     * All navigation state changes go through this method
     */
    fun updateCurrentPage(pageIndex: Int, updateNavController: Boolean = false) {
        if (pageIndex in 0 until pageCount && pageIndex != _currentPageIndex.value) {
            _currentPageIndex.value = pageIndex
            
            // Side effect: Update NavController if requested
            if (updateNavController) {
                val route = getRouteForPage(pageIndex)
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    
    /**
     * Handle bottom navigation tap
     * Updates state and lets UI react
     */
    fun onBottomNavTap(route: String) {
        val pageIndex = getPageIndexForRoute(route)
        updateCurrentPage(pageIndex, updateNavController = false)
    }
    
    /**
     * Handle pager swipe completion
     * Updates state and NavController
     */
    fun onPagerSwipeComplete(pageIndex: Int) {
        updateCurrentPage(pageIndex, updateNavController = true)
    }
    
    /**
     * Sync with external route changes (e.g., deep links, back navigation)
     */
    @Composable
    fun SyncWithExternalNavigation() {
        val currentRoute = currentRoute
        
        LaunchedEffect(currentRoute) {
            val expectedPageIndex = getPageIndexForRoute(currentRoute)
            if (expectedPageIndex != _currentPageIndex.value) {
                _currentPageIndex.value = expectedPageIndex
            }
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
     * Navigate to any route (for non-tab screens)
     */
    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/**
 * Clean Swipe Navigation State
 * 
 * Handles only pager-specific logic, delegates state management to CentralizedNavigationManager
 */
@Stable
class SwipeNavigationState(
    val pagerState: PagerState,
    private val navigationManager: CentralizedNavigationManager
) {
    
    /**
     * Set up reactive synchronization
     * Single direction flows to avoid circular dependencies
     */
    @Composable
    fun SetupSynchronization() {
        val coroutineScope = rememberCoroutineScope()
        
        // React to navigation state changes → animate pager
        val currentPageIndex by navigationManager.currentPageIndex.collectAsState()
        LaunchedEffect(currentPageIndex) {
            if (pagerState.currentPage != currentPageIndex && !pagerState.isScrollInProgress) {
                coroutineScope.launch {
                    try {
                        pagerState.animateScrollToPage(currentPageIndex)
                    } catch (e: Exception) {
                        // Handle animation cancellation gracefully
                        pagerState.scrollToPage(currentPageIndex)
                    }
                }
            }
        }
        
        // React to pager swipes → update navigation state
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { currentPage ->
                    // Only update when scroll is complete to avoid intermediate states
                    if (!pagerState.isScrollInProgress && currentPage in 0 until navigationManager.pageCount) {
                        navigationManager.onPagerSwipeComplete(currentPage)
                    }
                }
        }
        
        // Sync with external navigation changes
        navigationManager.SyncWithExternalNavigation()
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
 * Remember swipe navigation state
 */
@Composable
fun rememberSwipeNavigationState(
    navigationManager: CentralizedNavigationManager,
    currentRoute: String?
): SwipeNavigationState {
    
    // Initialize pager state with current route
    val initialPage = remember(currentRoute) {
        navigationManager.getPageIndexForRoute(currentRoute)
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { navigationManager.pageCount }
    )
    
    return remember(navigationManager, pagerState) {
        SwipeNavigationState(pagerState, navigationManager)
    }
}