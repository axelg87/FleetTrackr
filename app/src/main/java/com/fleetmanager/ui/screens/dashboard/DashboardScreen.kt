package com.fleetmanager.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.navigation.DashboardShortcut
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.utils.rememberStableLambda0
import com.fleetmanager.ui.viewmodel.DashboardViewModel
import com.fleetmanager.ui.viewmodel.MonthFilter

@Composable
fun DashboardScreen(
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    onEntryClick: ((String) -> Unit)? = null,
    onReportShortcut: (DashboardShortcut) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Navigation is now handled directly through the callback parameter
    
    // Create stable lambdas to prevent unnecessary recompositions
    val onAddClick: () -> Unit = rememberStableLambda0({ onAddEntryClick() })
    val onExpenseClick: () -> Unit = rememberStableLambda0({ onAddExpenseClick() })
    val onSyncClick: () -> Unit = rememberStableLambda0({ viewModel.syncNow() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            ScreenHeader(
                title = "Dashboard",
                userName = uiState.userProfile?.name,
                profilePictureUrl = uiState.userProfile?.profilePictureUrl,
                onProfileClick = onNavigateToProfile?.let { rememberProfileClickHandler(it) }
            )
        }

        // Quick Stats
        item {
            StatsGrid(
                stats = uiState.quickStats,
                onStatClick = { stat ->
                    stat.shortcut?.let(onReportShortcut)
                }
            )
        }

        // Earnings by Source
        if (uiState.earningsStats.isNotEmpty()) {
            item {
                SectionHeader(title = "Earnings by Source")
            }
            
            item {
                StatsGrid(
                    stats = uiState.earningsStats,
                    onStatClick = { stat ->
                        stat.shortcut?.let(onReportShortcut)
                    }
                )
            }
        }

        // Quick Actions
        item {
            SectionHeader(title = "Quick Actions")
        }

        item {
            QuickActionsRow(
                actions = listOf(
                    ActionItem(
                        text = "Sync",
                        icon = Icons.Default.Refresh,
                        onClick = onSyncClick,
                        isPrimary = false
                    )
                )
            )
        }

        // Recent Activity
        item {
            SectionHeader(title = "Recent Activity")
        }

        if (uiState.recentEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Assignment,
                    title = "No entries yet",
                    description = "Start by adding your first daily entry or expense"
                )
            }
        } else {
            items(uiState.recentEntries) { entry ->
                DailyEntryTile(
                    entry = entry,
                    onClick = { onEntryClick?.invoke(entry.id) }
                )
            }
        }

        // Show sync status if needed
        uiState.error?.let { error ->
            item {
                StatusCard(
                    type = StatusType.Error,
                    message = error
                )
            }
        }
    }
        
        // Floating Action Button Menu
        FloatingActionButtonMenu(
            items = createDefaultFabMenuItems(
                onIncomeClick = onAddClick,
                onExpenseClick = onExpenseClick
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 112.dp)
        )

        MonthFilterChips(
            selectedFilter = uiState.monthFilter,
            onFilterSelected = viewModel::onMonthFilterSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class) // FilterChip remains experimental in Material3.
@Composable
private fun MonthFilterChips(
    selectedFilter: MonthFilter,
    onFilterSelected: (MonthFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Filter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            MonthFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.chipLabel) }
                )
            }
        }
    }
}

