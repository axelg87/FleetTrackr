package com.fleetmanager.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.model.ReportEntry
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Create stable lambdas to prevent unnecessary recompositions
    val onDriverFilterChange = remember { { driver: String? ->
        viewModel.updateDriverFilter(driver)
    } }
    val onVehicleFilterChange = remember { { vehicle: String? ->
        viewModel.updateVehicleFilter(vehicle)
    } }
    val onTypeFilterChange = remember { { type: String? ->
        viewModel.updateTypeFilter(type)
    } }
    val onClearFilters = remember { {
        viewModel.clearAllFilters()
    } }
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            ScreenHeader(title = "Reports")
        }
        
        // Filters Section
        item {
            FiltersSection(
                drivers = uiState.drivers.map { it.name },
                vehicles = uiState.vehicles.map { it.displayName },
                types = uiState.availableTypes,
                selectedDriver = uiState.selectedDriver,
                selectedVehicle = uiState.selectedVehicle,
                selectedType = uiState.selectedType,
                onDriverChange = onDriverFilterChange,
                onVehicleChange = onVehicleFilterChange,
                onTypeChange = onTypeFilterChange,
                onClearFilters = onClearFilters
            )
        }
        
        // Summary Section
        item {
            SummarySection(
                totalEntries = uiState.totalEntries,
                totalAmount = uiState.totalAmountDisplay
            )
        }
        
        // Entries List
        if (uiState.filteredEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Assessment,
                    title = "No entries found",
                    description = "Try adjusting your filters or add some entries"
                )
            }
        } else {
            items(uiState.filteredEntries) { entry ->
                ReportEntryCard(entry = entry)
            }
        }
        
        // Error handling
        uiState.errorMessage?.let { error ->
            item {
                StatusCard(
                    type = StatusType.Error,
                    message = error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSection(
    drivers: List<String>,
    vehicles: List<String>,
    types: List<String>,
    selectedDriver: String?,
    selectedVehicle: String?,
    selectedType: String?,
    onDriverChange: (String?) -> Unit,
    onVehicleChange: (String?) -> Unit,
    onTypeChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (selectedDriver != null || selectedVehicle != null || selectedType != null) {
                    TextButton(onClick = onClearFilters) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
            
            // Filter dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Driver",
                    options = drivers,
                    selectedOption = selectedDriver,
                    onOptionSelected = onDriverChange,
                    modifier = Modifier.weight(1f)
                )
                
                FilterDropdown(
                    label = "Vehicle",
                    options = vehicles,
                    selectedOption = selectedVehicle,
                    onOptionSelected = onVehicleChange,
                    modifier = Modifier.weight(1f)
                )
            }
            
            FilterDropdown(
                label = "Type",
                options = types,
                selectedOption = selectedType,
                onOptionSelected = onTypeChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("All ${label.lowercase()}s") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All ${label.lowercase()}s") },
                onClick = {
                    onOptionSelected(null)
                    expanded = false
                }
            )
            
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SummarySection(
    totalEntries: Int,
    totalAmount: String
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalEntries.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total Entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalAmount,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (totalAmount.startsWith("+")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = "Net Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ReportEntryCard(
    entry: ReportEntry,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.typeDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = entry.displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isIncome) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Driver: ${entry.driverName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Vehicle: ${entry.vehicle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Text(
                    text = dateFormatter.format(entry.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End
                )
            }
            
            if (entry.notes.isNotBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}