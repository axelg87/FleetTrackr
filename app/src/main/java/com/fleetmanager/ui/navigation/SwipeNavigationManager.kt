package com.fleetmanager.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.fleetmanager.domain.model.UserRole

/**
 * Centralized Swipe Navigation Manager
 * 
 * Manages horizontal swipe navigation between main screens using clean architecture principles.
 * Follows SOLID principles with single responsibility for swipe coordination.
 */
@Stable
class SwipeNavigationManager(
    private val navigationState: NavigationState,
    private val bottomNavItems: List<BottomNavItem>
) {
    
    /**
     * Get the index of current screen in the bottom nav items
     */
    fun getCurrentPageIndex(currentRoute: String?): Int {
        return bottomNavItems.indexOfFirst { it.screen.route == currentRoute }.takeIf { it >= 0 } ?: 0
    }
    
    /**
     * Navigate to page by index (used by PagerState)
     */
    fun navigateToPage(pageIndex: Int) {
        if (pageIndex in bottomNavItems.indices) {
            val targetRoute = bottomNavItems[pageIndex].screen.route
            navigationState.navigateTo(targetRoute)
        }
    }
    
    /**
     * Get the screen route for a given page index
     */
    fun getRouteForPage(pageIndex: Int): String {
        return if (pageIndex in bottomNavItems.indices) {
            bottomNavItems[pageIndex].screen.route
        } else {
            bottomNavItems[0].screen.route
        }
    }
    
    /**
     * Check if swipe navigation should be enabled for current route
     */
    fun shouldEnableSwipe(currentRoute: String?): Boolean {
        return bottomNavItems.any { it.screen.route == currentRoute }
    }
    
    /**
     * Get total number of swipeable pages
     */
    val pageCount: Int get() = bottomNavItems.size
}

/**
 * Remember swipe navigation manager
 */
@Composable
fun rememberSwipeNavigationManager(
    navigationState: NavigationState,
    bottomNavItems: List<BottomNavItem>
): SwipeNavigationManager {
    return remember(navigationState, bottomNavItems) {
        SwipeNavigationManager(navigationState, bottomNavItems)
    }
}

/**
 * Swipe Navigation State
 * 
 * Encapsulates PagerState and coordination logic for swipe navigation
 */
@Stable
class SwipeNavigationState(
    val pagerState: PagerState,
    val swipeManager: SwipeNavigationManager
) {
    
    /**
     * Sync pager state with navigation changes
     */
    @Composable
    fun SyncWithNavigation(currentRoute: String?) {
        val targetPage = swipeManager.getCurrentPageIndex(currentRoute)
        
        LaunchedEffect(currentRoute) {
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
        
        // Handle pager state changes (when user swipes)
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            if (!pagerState.isScrollInProgress) {
                val currentPageRoute = swipeManager.getRouteForPage(pagerState.currentPage)
                if (currentRoute != currentPageRoute) {
                    swipeManager.navigateToPage(pagerState.currentPage)
                }
            }
        }
    }
}

/**
 * Remember swipe navigation state
 */
@Composable
fun rememberSwipeNavigationState(
    swipeManager: SwipeNavigationManager,
    initialPage: Int = 0
): SwipeNavigationState {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { swipeManager.pageCount }
    )
    
    return remember(swipeManager, pagerState) {
        SwipeNavigationState(pagerState, swipeManager)
    }
}