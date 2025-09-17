package com.fleetmanager.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.utils.rememberStableLambda0
import com.fleetmanager.ui.viewmodel.DashboardViewModel
import com.fleetmanager.ui.model.FilterContext
import com.fleetmanager.domain.model.DailyEntry
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToReportsWithFilter: ((FilterContext) -> Unit)? = null,
    onEntryClick: ((String) -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Set up navigation callback
    LaunchedEffect(onNavigateToReportsWithFilter) {
        if (onNavigateToReportsWithFilter != null) {
            viewModel.setNavigationCallback(onNavigateToReportsWithFilter)
        }
    }
    
    // Create stable lambdas to prevent unnecessary recompositions
    val onAddClick: () -> Unit = rememberStableLambda0({ onAddEntryClick() })
    val onExpenseClick: () -> Unit = rememberStableLambda0({ onAddExpenseClick() })
    val onSyncClick: () -> Unit = rememberStableLambda0({ viewModel.syncNow() })

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            StatsGrid(stats = uiState.quickStats)
        }

        // Earnings by Source
        if (uiState.earningsStats.isNotEmpty()) {
            item {
                SectionHeader(title = "Earnings by Source")
            }
            
            item {
                StatsGrid(stats = uiState.earningsStats)
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
                    entry = entry.toDailyEntry(),
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
                .padding(16.dp)
        )
    }
}


// Extension function to convert RecentEntry to DailyEntry for unified display
fun RecentEntry.toDailyEntry(): DailyEntry {
    return DailyEntry(
        id = this.id,
        date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(this.date) ?: Date(),
        driverName = this.driverName,
        vehicle = "", // RecentEntry doesn't have vehicle info, we'll need to enhance the data model
        uberEarnings = 0.0, // These would need to be added to RecentEntry if we want to show breakdown
        yangoEarnings = 0.0,
        privateJobsEarnings = this.totalEarnings, // For now, put all earnings in private
        notes = "",
        createdAt = Date(),
        updatedAt = Date(),
        isSynced = true
    )
}