@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen

/**
 * Swipeable Main Content
 * 
 * Wraps the main tab screens in a HorizontalPager for swipe navigation.
 * Clean separation of concerns - this component only handles the pager layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableMainContent(
    swipeNavigationState: SwipeNavigationState,
    currentRoute: String?,
    bottomNavItems: List<BottomNavItem>,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    // Sync pager state with navigation changes
    swipeNavigationState.SyncWithNavigation(currentRoute)
    
    // Only show pager for main tab screens
    if (swipeNavigationState.swipeManager.shouldEnableSwipe(currentRoute)) {
        HorizontalPager(
            state = swipeNavigationState.pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val screenRoute = bottomNavItems.getOrNull(pageIndex)?.screen?.route
            
            Box(modifier = Modifier.fillMaxSize()) {
                when (screenRoute) {
                    Screen.Dashboard.route -> {
                        DashboardScreen(
                            onAddEntryClick = onAddEntryClick,
                            onAddExpenseClick = onAddExpenseClick,
                            onNavigateToProfile = onNavigateToProfile,
                            onEntryClick = onEntryClick
                        )
                    }
                    Screen.History.route -> {
                        EntryListScreen(
                            onAddEntryClick = onAddEntryClick,
                            onAddExpenseClick = onAddExpenseClick,
                            onEntryClick = onEntryClick,
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                    Screen.Analytics.route -> {
                        AnalyticsScreen(
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                    Screen.Reports.route -> {
                        ReportScreen(
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                    Screen.Settings.route -> {
                        SettingsScreen(
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
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