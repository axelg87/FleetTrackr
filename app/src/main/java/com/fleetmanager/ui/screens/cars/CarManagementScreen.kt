package com.fleetmanager.ui.screens.cars

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
import com.fleetmanager.domain.model.Car
import com.fleetmanager.ui.components.CarDetailDialog
import com.fleetmanager.ui.components.CarFormDialog
import com.fleetmanager.ui.components.ListItemCard
import com.fleetmanager.ui.viewmodel.CarManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CarManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showForm by remember { mutableStateOf(false) }
    var carBeingEdited by remember { mutableStateOf<Car?>(null) }
    var carForDetails by remember { mutableStateOf<Car?>(null) }
    var carPendingDelete by remember { mutableStateOf<Car?>(null) }

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
                    Text(text = "Cars", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                carBeingEdited = null
                showForm = true
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Car")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.cars.isEmpty() && uiState.isLoading -> {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.cars.isEmpty() -> {
                    Text(
                        text = "No cars available",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    CarList(
                        cars = uiState.cars,
                        onView = { carForDetails = it },
                        onEdit = {
                            carBeingEdited = it
                            showForm = true
                        },
                        onDelete = { carPendingDelete = it }
                    )
                }
            }
        }
    }

    if (showForm) {
        CarFormDialog(
            title = if (carBeingEdited == null) "Add Car" else "Edit Car",
            initialCar = carBeingEdited,
            onDismiss = { showForm = false },
            onConfirm = { car ->
                viewModel.saveCar(car)
                showForm = false
            }
        )
    }

    carForDetails?.let { car ->
        CarDetailDialog(car = car, onDismiss = { carForDetails = null })
    }

    carPendingDelete?.let { car ->
        ConfirmDeleteCarDialog(
            car = car,
            onDismiss = { carPendingDelete = null },
            onConfirm = {
                viewModel.deleteCar(car.id)
                carPendingDelete = null
            }
        )
    }
}

@Composable
private fun CarList(
    cars: List<Car>,
    onView: (Car) -> Unit,
    onEdit: (Car) -> Unit,
    onDelete: (Car) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cars) { car ->
            ListItemCard(onClick = { onView(car) }) {
                CarListItemContent(
                    car = car,
                    onView = { onView(car) },
                    onEdit = { onEdit(car) },
                    onDelete = { onDelete(car) }
                )
            }
        }
    }
}

@Composable
private fun CarListItemContent(
    car: Car,
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
            Text(text = car.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "License: ${car.licensePlate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (car.isActive) "Status: Active" else "Status: Inactive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onView) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Car")
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Car")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Car")
            }
        }
    }
}

@Composable
private fun ConfirmDeleteCarDialog(
    car: Car,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Car") },
        text = { Text(text = "Are you sure you want to delete ${car.displayName}?") },
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
