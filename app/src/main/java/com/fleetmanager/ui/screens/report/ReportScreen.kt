package com.fleetmanager.ui.screens.report

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.components.charts.SimpleBarChart
import com.fleetmanager.ui.components.charts.SimplePieChart
import com.fleetmanager.ui.model.ChartDataGenerator
import com.fleetmanager.ui.model.ReportEntry
import com.fleetmanager.ui.utils.ReportExporter
import com.fleetmanager.ui.utils.collectAsStateWithLifecycle
import com.fleetmanager.ui.viewmodel.EntryTypeFilter
import com.fleetmanager.ui.viewmodel.ReportViewModel
import com.fleetmanager.ui.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reportExporter = remember { ReportExporter() }
    
    // Track current tab for charts/totals
    var selectedTab by remember { mutableStateOf(0) }
    
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
    val onEntryTypeFilterChange = remember { { entryType: EntryTypeFilter ->
        viewModel.updateEntryTypeFilter(entryType)
    } }
    val onDateRangeChange = remember { { startDate: Date?, endDate: Date? ->
        viewModel.updateDateRange(startDate, endDate)
    } }
    val onSortOptionChange = remember { { sortOption: SortOption ->
        viewModel.updateSortOption(sortOption)
    } }
    val onClearFilters = remember { {
        viewModel.clearAllFilters()
    } }
    val onExport = remember { {
        viewModel.executeAsync { 
            when (val result = reportExporter.exportToCsv(context, uiState.filteredEntries)) {
                is com.fleetmanager.ui.utils.ExportResult.Success -> {
                    Toast.makeText(context, "Report exported to: ${result.filePath}", Toast.LENGTH_LONG).show()
                }
                is com.fleetmanager.ui.utils.ExportResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
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
        // Screen Header with Export Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenHeader(title = "Reports")
                OutlinedButton(
                    onClick = onExport,
                    enabled = uiState.filteredEntries.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
            }
        }
        
        // Enhanced Filters Section
        item {
            EnhancedFiltersSection(
                drivers = uiState.drivers.map { it.name },
                vehicles = uiState.vehicles.map { it.displayName },
                types = uiState.availableTypes,
                selectedDriver = uiState.selectedDriver,
                selectedVehicle = uiState.selectedVehicle,
                selectedType = uiState.selectedType,
                selectedEntryType = uiState.selectedEntryType,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                sortOption = uiState.sortOption,
                onDriverChange = onDriverFilterChange,
                onVehicleChange = onVehicleFilterChange,
                onTypeChange = onTypeFilterChange,
                onEntryTypeChange = onEntryTypeFilterChange,
                onDateRangeChange = onDateRangeChange,
                onSortOptionChange = onSortOptionChange,
                onClearFilters = onClearFilters
            )
        }
        
        // Summary Section with Totals
        item {
            EnhancedSummarySection(
                totalEntries = uiState.totalEntries,
                totalAmount = uiState.totalAmountDisplay,
                totalsByDriver = uiState.totalsByDriver,
                totalsByVehicle = uiState.totalsByVehicle,
                totalsByType = uiState.totalsByType,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
        
        // Charts Section
        item {
            ChartsSection(
                entries = uiState.filteredEntries
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
private fun EnhancedFiltersSection(
    drivers: List<String>,
    vehicles: List<String>,
    types: List<String>,
    selectedDriver: String?,
    selectedVehicle: String?,
    selectedType: String?,
    selectedEntryType: EntryTypeFilter,
    startDate: Date?,
    endDate: Date?,
    sortOption: SortOption,
    onDriverChange: (String?) -> Unit,
    onVehicleChange: (String?) -> Unit,
    onTypeChange: (String?) -> Unit,
    onEntryTypeChange: (EntryTypeFilter) -> Unit,
    onDateRangeChange: (Date?, Date?) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
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
                        text = "Filters & Sorting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                val hasFilters = selectedDriver != null || selectedVehicle != null || 
                    selectedType != null || selectedEntryType != EntryTypeFilter.ALL || 
                    startDate != null || endDate != null
                
                if (hasFilters) {
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
            
            // Date Range Filter
            DateRangeFilter(
                startDate = startDate,
                endDate = endDate,
                onDateRangeChange = onDateRangeChange
            )
            
            // Entry Type Filter
            EntryTypeFilterSection(
                selectedEntryType = selectedEntryType,
                onEntryTypeChange = onEntryTypeChange
            )
            
            // Traditional filters
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Type",
                    options = types,
                    selectedOption = selectedType,
                    onOptionSelected = onTypeChange,
                    modifier = Modifier.weight(1f)
                )
                
                // Sort Options
                SortDropdown(
                    selectedSortOption = sortOption,
                    onSortOptionSelected = onSortOptionChange,
                    modifier = Modifier.weight(1f)
                )
            }
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
private fun DateRangeFilter(
    startDate: Date?,
    endDate: Date?,
    onDateRangeChange: (Date?, Date?) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    startDate?.let { calendar.time = it }
                    
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selectedDate = Calendar.getInstance().apply {
                                set(year, month, day)
                            }.time
                            onDateRangeChange(selectedDate, endDate)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(startDate?.let { dateFormatter.format(it) } ?: "Start Date")
            }
            
            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    endDate?.let { calendar.time = it }
                    
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selectedDate = Calendar.getInstance().apply {
                                set(year, month, day)
                            }.time
                            onDateRangeChange(startDate, selectedDate)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(endDate?.let { dateFormatter.format(it) } ?: "End Date")
            }
        }
    }
}

@Composable
private fun EntryTypeFilterSection(
    selectedEntryType: EntryTypeFilter,
    onEntryTypeChange: (EntryTypeFilter) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Entry Type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EntryTypeFilter.values().forEach { entryType ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = selectedEntryType == entryType,
                            onClick = { onEntryTypeChange(entryType) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedEntryType == entryType,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entryType.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    selectedSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSortOption.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Sort by") },
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
            SortOption.values().forEach { sortOption ->
                DropdownMenuItem(
                    text = { Text(sortOption.displayName) },
                    onClick = {
                        onSortOptionSelected(sortOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EnhancedSummarySection(
    totalEntries: Int,
    totalAmount: String,
    totalsByDriver: List<com.fleetmanager.ui.viewmodel.GroupedTotal>,
    totalsByVehicle: List<com.fleetmanager.ui.viewmodel.GroupedTotal>,
    totalsByType: List<com.fleetmanager.ui.viewmodel.GroupedTotal>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall totals
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            
            // Grouped totals tabs
            if (totalsByDriver.isNotEmpty() || totalsByVehicle.isNotEmpty() || totalsByType.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTab
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("By Driver") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text("By Vehicle") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { onTabSelected(2) },
                        text = { Text("By Type") }
                    )
                }
                
                // Tab content
                when (selectedTab) {
                    0 -> GroupedTotalsList(totals = totalsByDriver)
                    1 -> GroupedTotalsList(totals = totalsByVehicle)
                    2 -> GroupedTotalsList(totals = totalsByType)
                }
            }
        }
    }
}

@Composable
private fun GroupedTotalsList(
    totals: List<com.fleetmanager.ui.viewmodel.GroupedTotal>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        totals.take(5).forEach { total ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = total.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${total.count} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Text(
                    text = total.displayAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (total.amount >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
        
        if (totals.size > 5) {
            Text(
                text = "... and ${totals.size - 5} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ChartsSection(
    entries: List<ReportEntry>
) {
    if (entries.isEmpty()) return
    
    var selectedChartTab by remember { mutableStateOf(0) }
    
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TabRow(
                selectedTabIndex = selectedChartTab
            ) {
                Tab(
                    selected = selectedChartTab == 0,
                    onClick = { selectedChartTab = 0 },
                    text = { Text("By Type") }
                )
                Tab(
                    selected = selectedChartTab == 1,
                    onClick = { selectedChartTab = 1 },
                    text = { Text("By Driver") }
                )
                Tab(
                    selected = selectedChartTab == 2,
                    onClick = { selectedChartTab = 2 },
                    text = { Text("Over Time") }
                )
            }
            
            when (selectedChartTab) {
                0 -> {
                    val pieData = ChartDataGenerator.generatePieChartByType(entries)
                    SimplePieChart(
                        data = pieData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                1 -> {
                    val pieData = ChartDataGenerator.generatePieChartByDriver(entries)
                    SimplePieChart(
                        data = pieData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                2 -> {
                    val barData = ChartDataGenerator.generateBarChartByMonth(entries)
                    SimpleBarChart(
                        data = barData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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