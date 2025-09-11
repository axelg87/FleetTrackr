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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    onNavigateBack: () -> Unit
) {
    var selectedDriver by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var uberEarnings by remember { mutableStateOf("") }
    var yangoEarnings by remember { mutableStateOf("") }
    var privateJobsEarnings by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()
    
    val drivers = listOf(
        "John Smith",
        "Maria Garcia",
        "Ahmed Hassan",
        "Sarah Johnson",
        "Mike Wilson"
    )
    
    val vehicles = listOf(
        "ABC-123 - Toyota Camry",
        "XYZ-789 - Honda Accord", 
        "DEF-456 - Hyundai Elantra",
        "GHI-123 - Nissan Altima",
        "JKL-789 - Ford Fusion"
    )

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
            var driverExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = driverExpanded,
                onExpandedChange = { expanded: Boolean -> driverExpanded = !driverExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDriver,
                    onValueChange = { _: String -> },
                    readOnly = true,
                    label = { Text(text = "Driver") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = driverExpanded,
                    onDismissRequest = { driverExpanded = false }
                ) {
                    drivers.forEach { driver: String ->
                        DropdownMenuItem(
                            text = { Text(text = driver) },
                            onClick = {
                                selectedDriver = driver
                                driverExpanded = false
                            }
                        )
                    }
                }
            }

            // Vehicle Selection
            var vehicleExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = vehicleExpanded,
                onExpandedChange = { expanded: Boolean -> vehicleExpanded = !vehicleExpanded }
            ) {
                OutlinedTextField(
                    value = selectedVehicle,
                    onValueChange = { _: String -> },
                    readOnly = true,
                    label = { Text(text = "Vehicle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = vehicleExpanded,
                    onDismissRequest = { vehicleExpanded = false }
                ) {
                    vehicles.forEach { vehicle: String ->
                        DropdownMenuItem(
                            text = { Text(text = vehicle) },
                            onClick = {
                                selectedVehicle = vehicle
                                vehicleExpanded = false
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
                value = uberEarnings,
                onValueChange = { value: String -> uberEarnings = value },
                label = { Text(text = "Uber Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text(text = "$") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            // Yango Earnings
            OutlinedTextField(
                value = yangoEarnings,
                onValueChange = { value: String -> yangoEarnings = value },
                label = { Text(text = "Yango Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text(text = "$") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            // Private Jobs Earnings
            OutlinedTextField(
                value = privateJobsEarnings,
                onValueChange = { value: String -> privateJobsEarnings = value },
                label = { Text(text = "Private Jobs Earnings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text(text = "$") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { value: String -> notes = value },
                label = { Text(text = "Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 3,
                placeholder = { Text(text = "Additional details...") }
            )

            // Error message
            error?.let { errorMessage: String ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    when {
                        selectedDriver.isEmpty() -> {
                            error = "Please select a driver"
                        }
                        selectedVehicle.isEmpty() -> {
                            error = "Please select a vehicle"
                        }
                        uberEarnings.isEmpty() && yangoEarnings.isEmpty() && privateJobsEarnings.isEmpty() -> {
                            error = "Please enter at least one earnings amount"
                        }
                        uberEarnings.isNotEmpty() && uberEarnings.toDoubleOrNull() == null -> {
                            error = "Please enter a valid Uber earnings amount"
                        }
                        yangoEarnings.isNotEmpty() && yangoEarnings.toDoubleOrNull() == null -> {
                            error = "Please enter a valid Yango earnings amount"
                        }
                        privateJobsEarnings.isNotEmpty() && privateJobsEarnings.toDoubleOrNull() == null -> {
                            error = "Please enter a valid Private Jobs earnings amount"
                        }
                        else -> {
                            error = null
                            isLoading = true
                            // Here you would typically save the income entry
                            // For now, just simulate saving and navigate back
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Save Income Entry")
                }
            }
        }
    }
}