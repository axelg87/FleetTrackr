package com.fleetmanager.ui.screens.entry

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.EntryListViewModel
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.utils.rememberStableLambda0
import com.fleetmanager.ui.utils.rememberStableLambda1
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.ui.viewmodel.HistoryFilter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EntryListScreen(
    onAddEntryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    onEditExpense: (String) -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    viewModel: EntryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    // State for delete confirmation dialog
    var entryToDelete by remember { mutableStateOf<DailyEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Create stable lambdas to prevent unnecessary recompositions
    val onAddClick: () -> Unit = rememberStableLambda0({ onAddEntryClick() })
    val onExpenseClick: () -> Unit = rememberStableLambda0({ onAddExpenseClick() })
    
    val context = LocalContext.current

    LaunchedEffect(uiState.bulkEditMessage) {
        uiState.bulkEditMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearBulkEditMessage()
        }
    }

    LaunchedEffect(uiState.bulkEditError) {
        uiState.bulkEditError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearBulkEditError()
        }
    }

    var showBulkEditDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            ScreenHeader(
                title = "History",
                userName = userProfile.name,
                profilePictureUrl = userProfile.profilePictureUrl,
                onProfileClick = onNavigateToProfile?.let { rememberProfileClickHandler(it) }
            )
        }
        
        // Role indicator for managers and admins
        if (PermissionManager.canViewAll(userRole)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Viewing as ${userRole.name} - Showing all entries",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (PermissionManager.canEdit(userRole)) {
            item {
                HistoryFilterRow(
                    selectedFilter = uiState.filter,
                    onFilterSelected = viewModel::updateFilter
                )
            }
        }

        if (PermissionManager.canEdit(userRole) && uiState.filter == HistoryFilter.INCOME) {
            item {
                if (uiState.isSelectionMode) {
                    BulkEditSelectionBar(
                        selectedCount = uiState.selectedEntryIds.size,
                        totalCount = uiState.entries.size,
                        onApply = { showBulkEditDialog = true },
                        onCancel = viewModel::cancelBulkEdit,
                        onSelectAll = viewModel::selectAllEntries,
                        isApplying = uiState.isBulkEditing
                    )
                } else {
                    BulkEditStartBar(
                        onStart = viewModel::startBulkEdit,
                        enabled = uiState.entries.isNotEmpty()
                    )
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
            
            uiState.filter == HistoryFilter.INCOME && uiState.entries.isEmpty() -> {
                item {
                    EmptyState(
                        icon = Icons.Default.Assignment,
                        title = "No entries yet",
                        description = "Add your first daily entry or expense to get started"
                    )
                }
            }

            uiState.filter == HistoryFilter.EXPENSE && uiState.expenses.isEmpty() -> {
                item {
                    EmptyState(
                        icon = Icons.Default.Assignment,
                        title = "No expenses yet",
                        description = "Record an expense to see it here"
                    )
                }
            }

            else -> {
                when (uiState.filter) {
                    HistoryFilter.INCOME -> {
                        items(
                            items = uiState.entries,
                            key = { it.id }
                        ) { entry ->
                            DailyEntryTile(
                                entry = entry,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleEntrySelection(entry.id)
                                    } else {
                                        onEntryClick(entry.id)
                                    }
                                },
                                onDelete = {
                                    entryToDelete = entry
                                    showDeleteDialog = true
                                },
                                showDeleteButton = PermissionManager.canDelete(userRole),
                                showEditButton = PermissionManager.canEdit(userRole),
                                onEdit = { onEditEntry(entry.id) },
                                selectionMode = uiState.isSelectionMode,
                                isSelected = uiState.selectedEntryIds.contains(entry.id),
                                onSelectionChange = { viewModel.toggleEntrySelection(entry.id) }
                            )
                        }
                    }
                    HistoryFilter.EXPENSE -> {
                        items(
                            items = uiState.expenses,
                            key = { it.id }
                        ) { expense ->
                            ExpenseListItem(
                                expense = expense,
                                onClick = { onEditExpense(expense.id) },
                                showEditButton = PermissionManager.canEdit(userRole),
                                onEdit = { onEditExpense(expense.id) }
                            )
                        }
                    }
                }
            }
        }
    }

        // Floating Action Button Menu - all roles can create
        if (!uiState.isSelectionMode) {
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

        // Delete confirmation dialog
        if (showDeleteDialog && entryToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    entryToDelete = null
                },
                title = { Text("Delete Entry") },
                text = { 
                    Text("Are you sure you want to delete the entry for ${entryToDelete!!.driverName} on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(entryToDelete!!.date)}? This action cannot be undone.") 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            entryToDelete?.let { entry ->
                                viewModel.deleteEntry(entry.id) {
                                    // Entry deleted successfully
                                }
                            }
                            showDeleteDialog = false
                            entryToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteDialog = false
                            entryToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showBulkEditDialog) {
            BulkEditDialog(
                driverOptions = uiState.driverUsers,
                vehicleOptions = uiState.vehicles,
                isProcessing = uiState.isBulkEditing,
                onDismiss = {
                    showBulkEditDialog = false
                    if (uiState.selectedEntryIds.isEmpty()) {
                        viewModel.cancelBulkEdit()
                    }
                },
                onConfirm = { driverId, vehicleId ->
                    viewModel.bulkUpdateSelectedEntries(driverId, vehicleId)
                    showBulkEditDialog = false
                }
            )
        }
    }
}

// FilterChip is still marked experimental in Material3; opt-in keeps the admin filter UX aligned with other list chips.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterRow(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedFilter == HistoryFilter.INCOME,
            onClick = { onFilterSelected(HistoryFilter.INCOME) },
            label = { Text("Income") }
        )
        FilterChip(
            selected = selectedFilter == HistoryFilter.EXPENSE,
            onClick = { onFilterSelected(HistoryFilter.EXPENSE) },
            label = { Text("Expenses") }
        )
    }
}

// OutlinedCard + selection affordance use experimental defaults, but provide the most consistent Material3 bulk edit surface.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditStartBar(
    onStart: () -> Unit,
    enabled: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bulk edit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Assign drivers and vehicles to multiple entries at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onStart,
                enabled = enabled
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start")
            }
        }
    }
}

@Composable
private fun BulkEditSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    isApplying: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Selected $selectedCount of $totalCount entries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onSelectAll, enabled = totalCount > 0) {
                    Text("Select all")
                }
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = onApply,
                    enabled = selectedCount > 0 && !isApplying
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update")
                    }
                }
            }
        }
    }
}

// Using ExposedDropdownMenuBox for dropdown selectors; API is still marked experimental.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditDialog(
    driverOptions: List<UserDto>,
    vehicleOptions: List<Vehicle>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedDriverId by remember(driverOptions) {
        mutableStateOf(driverOptions.firstOrNull()?.id.orEmpty())
    }
    var selectedVehicleId by remember(vehicleOptions) {
        mutableStateOf(vehicleOptions.firstOrNull()?.id.orEmpty())
    }
    var driverExpanded by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk edit entries") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = driverExpanded,
                    onExpandedChange = { driverExpanded = it }
                ) {
                    OutlinedTextField(
                        value = driverOptions.firstOrNull { it.id == selectedDriverId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Driver") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = driverExpanded,
                        onDismissRequest = { driverExpanded = false }
                    ) {
                        if (driverOptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No drivers available") },
                                onClick = { }
                            )
                        } else {
                            driverOptions.forEach { driver ->
                                DropdownMenuItem(
                                    text = { Text(driver.name) },
                                    onClick = {
                                        selectedDriverId = driver.id
                                        driverExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = vehicleExpanded,
                    onExpandedChange = { vehicleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = vehicleOptions.firstOrNull { it.id == selectedVehicleId }?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleExpanded,
                        onDismissRequest = { vehicleExpanded = false }
                    ) {
                        if (vehicleOptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No vehicles available") },
                                onClick = { }
                            )
                        } else {
                            vehicleOptions.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.displayName) },
                                    onClick = {
                                        selectedVehicleId = vehicle.id
                                        vehicleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedDriverId, selectedVehicleId) },
                enabled = driverOptions.isNotEmpty() && vehicleOptions.isNotEmpty() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExpenseListItem(
    expense: Expense,
    onClick: () -> Unit,
    showEditButton: Boolean,
    onEdit: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val disabledClick: () -> Unit = {}
    val itemClick: () -> Unit = if (showEditButton) onClick else disabledClick
    ListItemCard(onClick = itemClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.driverName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expense.vehicle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormatter.format(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                if (expense.notes.isNotEmpty()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (showEditButton) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit expense")
                    }
                }
            }
        }
    }
}
