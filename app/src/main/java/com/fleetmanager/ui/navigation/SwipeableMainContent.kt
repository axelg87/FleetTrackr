@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen

/**
 * Swipeable Main Content
 * 
 * Clean implementation using NavigationStateManager as single source of truth.
 * Features:
 * - HorizontalPager with proper state synchronization
 * - LaunchedEffect for bidirectional sync with NavigationStateManager
 * - Single-screen rendering with beyondBoundsPageCount = 0
 * - 60fps smooth animations
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableMainContent(
    navigationStateManager: NavigationStateManager,
    bottomNavItems: List<BottomNavItem>,
    currentRoute: String?,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    // Only show pager for swipeable routes
    if (!navigationStateManager.isSwipeableRoute(currentRoute)) return
    
    val currentPageIndex by navigationStateManager.currentPageIndex.collectAsState()
    
    // Create PagerState with proper initialization
    val pagerState = rememberPagerState(
        initialPage = currentPageIndex,
        pageCount = { navigationStateManager.pageCount }
    )
    
    // Sync NavigationStateManager -> PagerState (bottom nav clicks)
    LaunchedEffect(currentPageIndex) {
        if (pagerState.currentPage != currentPageIndex) {
            pagerState.animateScrollToPage(currentPageIndex)
        }
    }
    
    // Sync PagerState -> NavigationStateManager (swipe gestures)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentPageIndex && !pagerState.isScrollInProgress) {
            navigationStateManager.updatePage(pagerState.currentPage)
        }
    }
    
    // Sync with external route changes
    LaunchedEffect(currentRoute) {
        navigationStateManager.syncWithRoute(currentRoute)
    }
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val containerSize = IntSize(
            width = with(LocalDensity.current) { maxWidth.roundToPx() },
            height = with(LocalDensity.current) { maxHeight.roundToPx() }
        )
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondBoundsPageCount = 0, // Only render current page for performance
            key = { pageIndex -> 
                // Stable key for proper state preservation
                bottomNavItems.getOrNull(pageIndex)?.screen?.route ?: "page_$pageIndex"
            }
        ) { pageIndex ->
            PagerPage(
                pageIndex = pageIndex,
                bottomNavItems = bottomNavItems,
                containerSize = containerSize,
                onAddEntryClick = onAddEntryClick,
                onAddExpenseClick = onAddExpenseClick,
                onNavigateToProfile = onNavigateToProfile,
                onEntryClick = onEntryClick
            )
        }
    }
}

/**
 * Individual Pager Page with defensive sizing and state preservation
 */
@Composable
private fun PagerScope.PagerPage(
    pageIndex: Int,
    bottomNavItems: List<BottomNavItem>,
    containerSize: IntSize,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    val screenRoute = bottomNavItems.getOrNull(pageIndex)?.screen?.route
    
    // Use rememberSaveable to preserve screen state without keeping screens mounted
    val screenStateKey = rememberSaveable(screenRoute) { screenRoute ?: "unknown" }
    
    // Defensive Box with exact constraints to prevent layout issues
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                // Ensure size stability - could add logging here for debugging
            }
    ) {
        when (screenRoute) {
            Screen.Dashboard.route -> {
                key(screenStateKey) {
                    DashboardScreen(
                        onAddEntryClick = onAddEntryClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onNavigateToProfile = onNavigateToProfile,
                        onEntryClick = onEntryClick
                    )
                }
            }
            Screen.History.route -> {
                key(screenStateKey) {
                    EntryListScreen(
                        onAddEntryClick = onAddEntryClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onEntryClick = onEntryClick,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
            Screen.Analytics.route -> {
                key(screenStateKey) {
                    AnalyticsScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
            Screen.Reports.route -> {
                key(screenStateKey) {
                    ReportScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
            Screen.Settings.route -> {
                key(screenStateKey) {
                    SettingsScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
            else -> {
                // Fallback to Dashboard for unknown routes
                key("fallback_dashboard") {
                    DashboardScreen(
                        onAddEntryClick = onAddEntryClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onNavigateToProfile = onNavigateToProfile,
                        onEntryClick = onEntryClick
                    )
                }
            }
        }
    }
}

/**
 * Check if current route supports swipe navigation
 */
fun isSwipeableRoute(route: String?): Boolean {
    return when (route) {
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Analytics.route,
        Screen.Reports.route,
        Screen.Settings.route -> true
        else -> false
    }
}