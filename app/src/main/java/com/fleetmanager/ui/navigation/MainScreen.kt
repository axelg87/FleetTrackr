@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen
import com.fleetmanager.ui.viewmodel.NavigationViewModel as UserNavigationViewModel
import javax.inject.Inject

/**
 * Enterprise-grade MainScreen with single source of truth architecture
 * 
 * Uses only one HorizontalPager with a shared PagerState.
 * All navigation (swipe + tab click) updates the centralized NavigationState.
 * The BottomNavigationBar and HorizontalPager observe this state and update reactively.
 * 
 * No NavController is used for main navigation - pure StateFlow architecture.
 */
@Composable
fun MainScreen(
    navigationState: NavigationState,
    onAddEntryClick: () -> Unit = {},
    onAddExpenseClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onEntryClick: (String) -> Unit = {}
) {
    // Get user role for bottom nav filtering
    val userNavigationViewModel: UserNavigationViewModel = hiltViewModel()
    val userRole by userNavigationViewModel.userRole.collectAsState()
    val bottomNavItems = userRole?.let { getBottomNavItemsForRole(it) } ?: allBottomNavItems
    
    // Single PagerState - shared across the entire screen
    val pagerState = rememberPagerState(
        initialPage = navigationState.getCurrentPageIndex(),
        pageCount = { bottomNavItems.size }
    )
    
    // Observe the centralized navigation state
    val currentPageIndex by navigationState.currentPageIndex.collectAsState()
    
    // Synchronization Logic - MainScreen observes and synchronizes
    LaunchedEffect(currentPageIndex) {
        // Tab click → Pager: animateScrollToPage(index)
        if (pagerState.currentPage != currentPageIndex) {
            pagerState.animateScrollToPage(currentPageIndex)
        }
    }
    
    LaunchedEffect(pagerState) {
        // Swipe → Tab highlight: snapshotFlow with distinctUntilChanged
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (!pagerState.isScrollInProgress && currentPageIndex != page) {
                    navigationState.updatePageIndex(page)
                }
            }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedIndex = currentPageIndex,
                bottomNavItems = bottomNavItems,
                onClick = { index -> navigationState.updatePageIndex(index) }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            beyondBoundsPageCount = 0 // Only render current page to prevent freezes
        ) { pageIndex ->
            val screenRoute = bottomNavItems.getOrNull(pageIndex)?.screen?.route
            
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
                else -> {
                    // Fallback to Dashboard for unknown routes
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