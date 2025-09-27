package com.fleetmanager.ui.screens.vehicles

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
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.ui.components.ListItemCard
import com.fleetmanager.ui.components.VehicleDetailDialog
import com.fleetmanager.ui.components.VehicleFormDialog
import com.fleetmanager.ui.viewmodel.VehicleManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: VehicleManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showForm by remember { mutableStateOf(false) }
    var vehicleBeingEdited by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleForDetails by remember { mutableStateOf<Vehicle?>(null) }
    var vehiclePendingDelete by remember { mutableStateOf<Vehicle?>(null) }

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
                        text = "Vehicles",
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
            FloatingActionButton(onClick = {
                vehicleBeingEdited = null
                showForm = true
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.vehicles.isEmpty() && uiState.isLoading -> {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.vehicles.isEmpty() -> {
                    Text(
                        text = "No vehicles available",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    VehicleList(
                        vehicles = uiState.vehicles,
                        onView = { vehicleForDetails = it },
                        onEdit = {
                            vehicleBeingEdited = it
                            showForm = true
                        },
                        onDelete = { vehiclePendingDelete = it }
                    )
                }
            }
        }
    }

    if (showForm) {
        VehicleFormDialog(
            title = if (vehicleBeingEdited == null) "Add Vehicle" else "Edit Vehicle",
            initialVehicle = vehicleBeingEdited,
            onDismiss = { showForm = false },
            onConfirm = { vehicle ->
                viewModel.saveVehicle(vehicle)
                showForm = false
            }
        )
    }

    vehicleForDetails?.let { vehicle ->
        VehicleDetailDialog(vehicle = vehicle, onDismiss = { vehicleForDetails = null })
    }

    vehiclePendingDelete?.let { vehicle ->
        ConfirmDeleteVehicleDialog(
            vehicle = vehicle,
            onDismiss = { vehiclePendingDelete = null },
            onConfirm = {
                viewModel.deleteVehicle(vehicle.id)
                vehiclePendingDelete = null
            }
        )
    }
}

@Composable
private fun VehicleList(
    vehicles: List<Vehicle>,
    onView: (Vehicle) -> Unit,
    onEdit: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vehicles) { vehicle ->
            ListItemCard(onClick = { onView(vehicle) }) {
                VehicleListItemContent(
                    vehicle = vehicle,
                    onView = { onView(vehicle) },
                    onEdit = { onEdit(vehicle) },
                    onDelete = { onDelete(vehicle) }
                )
            }
        }
    }
}

@Composable
private fun VehicleListItemContent(
    vehicle: Vehicle,
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
            Text(text = vehicle.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "License: AED{vehicle.licensePlate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (vehicle.isActive) "Status: Active" else "Status: Inactive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onView) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Vehicle")
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Vehicle")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Vehicle")
            }
        }
    }
}

@Composable
private fun ConfirmDeleteVehicleDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Vehicle") },
        text = { Text(text = "Are you sure you want to delete AED{vehicle.displayName}?") },
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
