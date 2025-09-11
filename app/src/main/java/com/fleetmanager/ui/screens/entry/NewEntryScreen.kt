package com.fleetmanager.ui.screens.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.viewmodel.AddEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "New Income Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Income Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Income Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Record your daily earnings from various sources",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Driver Selection
            ExposedDropdownMenuBox(
                expanded = uiState.driverDropdownExpanded,
                onExpandedChange = { viewModel.toggleDriverDropdown() }
            ) {
                OutlinedTextField(
                    value = uiState.selectedDriver?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Driver") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.driverDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = uiState.driverDropdownExpanded,
                    onDismissRequest = { viewModel.toggleDriverDropdown() }
                ) {
                    uiState.drivers.forEach { driver ->
                        DropdownMenuItem(
                            text = { Text(driver.name) },
                            onClick = {
                                viewModel.selectDriver(driver)
                                viewModel.toggleDriverDropdown()
                            }
                        )
                    }
                }
            }

            // Vehicle Selection
            ExposedDropdownMenuBox(
                expanded = uiState.vehicleDropdownExpanded,
                onExpandedChange = { viewModel.toggleVehicleDropdown() }
            ) {
                OutlinedTextField(
                    value = uiState.selectedVehicle?.plateNumber ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Vehicle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.vehicleDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = uiState.vehicleDropdownExpanded,
                    onDismissRequest = { viewModel.toggleVehicleDropdown() }
                ) {
                    uiState.vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text("${vehicle.plateNumber} - ${vehicle.model}") },
                            onClick = {
                                viewModel.selectVehicle(vehicle)
                                viewModel.toggleVehicleDropdown()
                            }
                        )
                    }
                }
            }

            // Earnings Section
            Text(
                text = "Earnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Uber Earnings
            OutlinedTextField(
                value = uiState.uberEarnings,
                onValueChange = viewModel::updateUberEarnings,
                label = { Text("Uber Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )

            // Yango Earnings
            OutlinedTextField(
                value = uiState.yangoEarnings,
                onValueChange = viewModel::updateYangoEarnings,
                label = { Text("Yango Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )

            // Private Jobs Earnings
            OutlinedTextField(
                value = uiState.privateJobsEarnings,
                onValueChange = viewModel::updatePrivateJobsEarnings,
                label = { Text("Private Jobs Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 3
            )

            // Error message
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Save Button
            Button(
                onClick = viewModel::saveEntry,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.selectedDriver != null && uiState.selectedVehicle != null
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Income Entry")
                }
            }
        }
    }
}