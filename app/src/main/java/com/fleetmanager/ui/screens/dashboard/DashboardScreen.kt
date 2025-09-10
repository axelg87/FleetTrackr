package com.fleetmanager.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*

@Composable
fun DashboardScreen(
    onAddEntryClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(title = "Dashboard")
        }

        // Quick Stats
        item {
            StatsGrid(stats = uiState.quickStats)
        }

        // Quick Actions
        item {
            SectionHeader(title = "Quick Actions")
        }

        item {
            QuickActionsRow(
                actions = listOf(
                    ActionItem(
                        text = "Add Entry",
                        icon = Icons.Default.Add,
                        onClick = onAddEntryClick,
                        isPrimary = true
                    ),
                    ActionItem(
                        text = "Sync",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.syncNow() },
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
                    description = "Start by adding your first daily entry",
                    actionText = "Add Entry",
                    onActionClick = onAddEntryClick
                )
            }
        } else {
            items(uiState.recentEntries) { entry ->
                ActivityItemCard(
                    icon = Icons.Default.DirectionsCar,
                    title = entry.driverName,
                    subtitle = entry.date,
                    value = "$${String.format("%.0f", entry.totalEarnings)}",
                    valueLabel = "${entry.totalRides} rides"
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
}