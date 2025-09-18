@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.snapshotFlow

@Composable
fun MainScreen(
    bottomNavItems: List<BottomNavItem>,
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    val currentIndex by NavigationState.currentPageIndex.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { bottomNavItems.size }
    )

    // Gate flags to prevent feedback loops
    var isAnimatingToTarget = remember { false }

    // Tab click -> Pager (via NavigationState)
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && !isAnimatingToTarget) {
            isAnimatingToTarget = true
            try {
                pagerState.animateScrollToPage(currentIndex)
            } finally {
                isAnimatingToTarget = false
            }
        }
    }

    // Swipe -> Tab highlight (update NavigationState)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .filter { !isAnimatingToTarget }
            .collectLatest { page ->
                NavigationState.setCurrentPage(page)
            }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = bottomNavItems,
                selectedIndex = currentIndex,
                onClick = { index -> NavigationState.setCurrentPage(index) }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 0,
                key = { index -> bottomNavItems.getOrNull(index)?.screen?.route ?: "page_$index" }
            ) { index ->
                when (bottomNavItems[index].screen) {
                    Screen.Dashboard -> DashboardScreen(
                        onAddEntryClick = onAddEntryClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onNavigateToProfile = onNavigateToProfile,
                        onEntryClick = onEntryClick
                    )
                    Screen.History -> EntryListScreen(
                        onAddEntryClick = onAddEntryClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onEntryClick = onEntryClick,
                        onNavigateToProfile = onNavigateToProfile
                    )
                    Screen.Analytics -> AnalyticsScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                    Screen.Reports -> ReportScreen(
                        onNavigateToProfile = onNavigateToProfile
                    )
                    Screen.Settings -> SettingsScreen(
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
}

