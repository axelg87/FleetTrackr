package com.fleetmanager.ui.screens.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.EntryListViewModel
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.utils.rememberStableLambda0
import com.fleetmanager.ui.utils.rememberStableLambda1
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EntryListScreen(
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    viewModel: EntryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Create stable lambdas to prevent unnecessary recompositions
    val onAddClick: () -> Unit = rememberStableLambda0({ onAddEntryClick() })
    val onExpenseClick: () -> Unit = rememberStableLambda0({ onAddExpenseClick() })
    val onItemClick: (String) -> Unit = rememberStableLambda1({ entryId: String -> onEntryClick(entryId) })
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            ScreenHeader(title = "History")
        }
        
        // Role information card (for testing/debugging)
        if (uiState.userRole != com.fleetmanager.domain.model.UserRole.DRIVER || 
            uiState.canEdit || uiState.canDelete) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Role: ${uiState.userRole.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = com.fleetmanager.domain.model.RolePermissions.getRoleDescription(uiState.userRole),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        when {
            uiState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            uiState.entries.isEmpty() -> {
                item {
                    EmptyState(
                        icon = Icons.Default.Assignment,
                        title = "No entries yet",
                        description = "Add your first daily entry or expense to get started"
                    )
                }
            }
            
            else -> {
                items(
                    items = uiState.entries,
                    key = { it.id }
                ) { entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { onItemClick(entry.id) }
                    )
                }
            }
        }
    }
        
        // Floating Action Button Menu (only show if user can create)
        if (uiState.userRole == com.fleetmanager.domain.model.UserRole.DRIVER || 
            uiState.userRole == com.fleetmanager.domain.model.UserRole.MANAGER ||
            uiState.userRole == com.fleetmanager.domain.model.UserRole.ADMIN) {
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
}

@Composable
fun EntryCard(
    entry: DailyEntry,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    ListItemCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.driverName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.vehicle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.format(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", entry.totalEarnings)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!entry.isSynced) {
                    Text(
                        text = "⏳ Syncing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        if (entry.notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        EarningsChips(
            earnings = listOf(
                EarningItem("Uber", entry.uberEarnings),
                EarningItem("Yango", entry.yangoEarnings),
                EarningItem("Private", entry.privateJobsEarnings)
            )
        )
    }
}