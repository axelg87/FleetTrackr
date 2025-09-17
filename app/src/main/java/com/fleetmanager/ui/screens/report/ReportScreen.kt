package com.fleetmanager.ui.screens.report

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.ui.components.charts.SimpleBarChart
import com.fleetmanager.ui.components.charts.SimplePieChart
import com.fleetmanager.ui.components.CalendarFilterComponent
import com.fleetmanager.ui.model.FilterContext
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
    onNavigateToProfile: (() -> Unit)? = null,
    filterContext: FilterContext? = null,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val reportExporter = remember { ReportExporter() }
    
    // Apply filter context if provided - runs when filterContext changes
    LaunchedEffect(filterContext) {
        filterContext?.let { context ->
            viewModel.applyFilterContext(context)
        }
    }
    
    // Lifecycle-aware filter application - ensures filters are always applied when screen is visible
    // This runs every time the ReportScreen composable is displayed (including when navigating back)
    LaunchedEffect(key1 = true) {
        // Re-apply current filter state to ensure UI is properly updated
        viewModel.refreshFilters()
    }
    
    // Track current tab for charts/totals
    var selectedTab by remember { mutableStateOf(0) }
    
    // Use ViewModel state for filter panel collapse
    val isFilterExpanded = uiState.isFilterPanelExpanded
    
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
        viewModel.exportData { 
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
        // Screen Header
        item {
            ScreenHeader(
                title = "Reports",
                userName = userProfile.name,
                profilePictureUrl = userProfile.profilePictureUrl,
                onProfileClick = onNavigateToProfile?.let { rememberProfileClickHandler(it) }
            )
        }
        
        // Export Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ElevatedButton(
                    onClick = onExport,
                    enabled = uiState.filteredEntries.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
            }
        }
        
        // Enhanced Filters Section
        item {
            CollapsibleFiltersSection(
                isExpanded = isFilterExpanded,
                onToggleExpanded = { viewModel.toggleFilterPanel() },
                drivers = uiState.driverUsers.map { it.name },
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
private fun CollapsibleFiltersSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hasActiveFilters = selectedDriver != null || selectedVehicle != null || 
                        selectedType != null || selectedEntryType != EntryTypeFilter.ALL || 
                        startDate != null || endDate != null
                    
                    Icon(
                        imageVector = if (hasActiveFilters) Icons.Default.FilterAlt else Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isExpanded) "Hide Filters" else "Show Filters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Show active filters summary when collapsed
                        if (!isExpanded) {
                            val activeFilters = mutableListOf<String>()
                            selectedDriver?.let { activeFilters.add("Driver: $it") }
                            selectedVehicle?.let { activeFilters.add("Vehicle: $it") }
                            selectedType?.let { activeFilters.add("Type: $it") }
                            if (selectedEntryType != EntryTypeFilter.ALL) {
                                activeFilters.add("Entry: ${selectedEntryType.displayName}")
                            }
                            if (startDate != null || endDate != null) {
                                activeFilters.add("Date Range")
                            }
                            
                            if (activeFilters.isNotEmpty()) {
                                Text(
                                    text = "${activeFilters.size} filter${if (activeFilters.size == 1) "" else "s"} active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hasFilters = selectedDriver != null || selectedVehicle != null || 
                        selectedType != null || selectedEntryType != EntryTypeFilter.ALL || 
                        startDate != null || endDate != null
                    
                    if (hasFilters) {
                        FilledTonalButton(
                            onClick = onClearFilters,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear")
                        }
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse filters" else "Expand filters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Animated content
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            
            // Enhanced Calendar Filter (replaces the basic DateRangeFilter)
            CalendarFilterComponent(
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(
    startDate: Date?,
    endDate: Date?,
    onDateRangeChange: (Date?, Date?) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartDatePicker = true }
            ) {
                OutlinedTextField(
                    value = startDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    label = { Text("Start Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Select start date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            // End Date Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndDatePicker = true }
            ) {
                OutlinedTextField(
                    value = endDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    label = { Text("End Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Select end date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        // Start Date Picker Dialog
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate?.time
            )
            DatePickerDialog(
                onDateSelected = { dateMillis ->
                    dateMillis?.let {
                        onDateRangeChange(Date(it), endDate)
                    }
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false },
                datePickerState = datePickerState
            )
        }
        
        // End Date Picker Dialog
        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = endDate?.time
            )
            DatePickerDialog(
                onDateSelected = { dateMillis ->
                    dateMillis?.let {
                        onDateRangeChange(startDate, Date(it))
                    }
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false },
                datePickerState = datePickerState
            )
        }
    }
}

@Composable
private fun EntryTypeFilterSection(
    selectedEntryType: EntryTypeFilter,
    onEntryTypeChange: (EntryTypeFilter) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Entry Type",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // First row with first two options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntryTypeFilter.values().take(2).forEach { entryType ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selectedEntryType == entryType,
                                onClick = { onEntryTypeChange(entryType) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEntryType == entryType,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entryType.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedEntryType == entryType) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
            
            // Second row with remaining options
            if (EntryTypeFilter.values().size > 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    EntryTypeFilter.values().drop(2).forEach { entryType ->
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entryType.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedEntryType == entryType) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
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
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Entries",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = totalAmount,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAmount.startsWith("+")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "Net Amount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${total.count} entries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = total.displayAmount,
                    style = MaterialTheme.typography.bodyLarge,
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Charts",
                style = MaterialTheme.typography.titleLarge,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp),
                        maxHeight = 240f
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
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}