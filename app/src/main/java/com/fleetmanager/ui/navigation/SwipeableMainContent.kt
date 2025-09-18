@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen

/**
 * Swipeable Main Content with Centralized Navigation Management
 * 
 * Uses NavigationStateManager as single source of truth for perfect bidirectional sync
 * between swipe gestures and bottom navigation tabs.
 */
@Composable
fun SwipeableMainContent(
    navigationStateManager: NavigationStateManager,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    val currentPageIndex by navigationStateManager.currentPageIndex.collectAsState()
    
    // Initialize PagerState with current page index
    val pagerState = rememberPagerState(
        initialPage = currentPageIndex,
        pageCount = { navigationStateManager.pageCount }
    )
    
    // Bidirectional synchronization
    SyncPagerWithNavigationState(
        pagerState = pagerState,
        navigationStateManager = navigationStateManager,
        currentPageIndex = currentPageIndex
    )
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondBoundsPageCount = 0 // Only render current page for performance
    ) { pageIndex ->
        SwipeablePage(
            pageIndex = pageIndex,
            onAddEntryClick = onAddEntryClick,
            onAddExpenseClick = onAddExpenseClick,
            onNavigateToProfile = onNavigateToProfile,
            onEntryClick = onEntryClick
        )
    }
}

/**
 * Handles bidirectional synchronization between PagerState and NavigationStateManager
 */
@Composable
private fun SyncPagerWithNavigationState(
    pagerState: PagerState,
    navigationStateManager: NavigationStateManager,
    currentPageIndex: Int
) {
    // Navigation State -> Pager (when user taps bottom nav)
    LaunchedEffect(currentPageIndex) {
        if (pagerState.currentPage != currentPageIndex) {
            pagerState.animateScrollToPage(currentPageIndex)
        }
    }
    
    // Pager -> Navigation State (when user swipes)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentPageIndex) {
            navigationStateManager.onSwipeChanged(pagerState.currentPage)
        }
    }
}

/**
 * Individual swipeable page content
 */
@Composable
private fun SwipeablePage(
    pageIndex: Int,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    // Use stable key for state preservation
    val pageKey = rememberSaveable { "page_$pageIndex" }
    
    Box(modifier = Modifier.fillMaxSize()) {
        key(pageKey) {
            when (pageIndex) {
                0 -> DashboardScreen(
                    onAddEntryClick = onAddEntryClick,
                    onAddExpenseClick = onAddExpenseClick,
                    onNavigateToProfile = onNavigateToProfile,
                    onEntryClick = onEntryClick
                )
                1 -> EntryListScreen(
                    onAddEntryClick = onAddEntryClick,
                    onAddExpenseClick = onAddExpenseClick,
                    onEntryClick = onEntryClick,
                    onNavigateToProfile = onNavigateToProfile
                )
                2 -> AnalyticsScreen(
                    onNavigateToProfile = onNavigateToProfile
                )
                3 -> ReportScreen(
                    onNavigateToProfile = onNavigateToProfile
                )
                4 -> SettingsScreen(
                    onNavigateToProfile = onNavigateToProfile
                )
                else -> DashboardScreen(
                    onAddEntryClick = onAddEntryClick,
                    onAddExpenseClick = onAddExpenseClick,
                    onNavigateToProfile = onNavigateToProfile,
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}