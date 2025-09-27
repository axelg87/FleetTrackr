package com.fleetmanager.ui.screens.drivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.ui.components.DriverDetailDialog
import com.fleetmanager.ui.components.DriverFormDialog
import com.fleetmanager.ui.components.ListItemCard
import com.fleetmanager.ui.viewmodel.DriverManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: DriverManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showForm by remember { mutableStateOf(false) }
    var driverBeingEdited by remember { mutableStateOf<Driver?>(null) }
    var driverForDetails by remember { mutableStateOf<Driver?>(null) }
    var driverPendingDelete by remember { mutableStateOf<Driver?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Drivers",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (uiState.canManageDrivers) {
                FloatingActionButton(onClick = {
                    driverBeingEdited = null
                    showForm = true
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Driver")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.drivers.isEmpty() && uiState.isLoading -> {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.drivers.isEmpty() -> {
                    Text(
                        text = "No drivers available",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    DriverList(
                        drivers = uiState.drivers,
                        canManageDrivers = uiState.canManageDrivers,
                        onView = { driverForDetails = it },
                        onEdit = {
                            driverBeingEdited = it
                            showForm = true
                        },
                        onDelete = { driverPendingDelete = it }
                    )
                }
            }
        }
    }

    if (showForm) {
        DriverFormDialog(
            title = if (driverBeingEdited == null) "Add Driver" else "Edit Driver",
            initialDriver = driverBeingEdited,
            onDismiss = { showForm = false },
            onConfirm = { driver ->
                viewModel.saveDriver(driver)
                showForm = false
            }
        )
    }

    driverForDetails?.let { driver ->
        DriverDetailDialog(driver = driver, onDismiss = { driverForDetails = null })
    }

    driverPendingDelete?.let { driver ->
        ConfirmDeleteDriverDialog(
            driver = driver,
            onDismiss = { driverPendingDelete = null },
            onConfirm = {
                viewModel.deleteDriver(driver.id)
                driverPendingDelete = null
            }
        )
    }
}

@Composable
private fun DriverList(
    drivers: List<Driver>,
    canManageDrivers: Boolean,
    onView: (Driver) -> Unit,
    onEdit: (Driver) -> Unit,
    onDelete: (Driver) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(drivers) { driver ->
            ListItemCard(onClick = { onView(driver) }) {
                DriverListItemContent(
                    driver = driver,
                    canManageDrivers = canManageDrivers,
                    onView = { onView(driver) },
                    onEdit = { onEdit(driver) },
                    onDelete = { onDelete(driver) }
                )
            }
        }
    }
}

@Composable
private fun DriverListItemContent(
    driver: Driver,
    canManageDrivers: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driver.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "ID: AED{driver.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Salary: AED AED{"%.2f".format(driver.salary)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (driver.isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.bodySmall,
                color = if (driver.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onView) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Driver")
            }
            if (canManageDrivers) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Driver")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Driver")
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDriverDialog(
    driver: Driver,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Driver") },
        text = {
            Text(text = "Are you sure you want to delete AED{driver.name}? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
