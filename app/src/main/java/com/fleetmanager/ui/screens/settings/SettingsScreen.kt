package com.fleetmanager.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.ui.viewmodel.SettingsViewModel
import com.fleetmanager.ui.components.*
import com.fleetmanager.data.excel.ImportProgress
import com.fleetmanager.ui.utils.rememberExcelFilePicker

@Composable
fun SettingsScreen(
    onNavigateToProfile: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    // File picker for Excel import
    val pickExcelFile = rememberExcelFilePicker(
        onFileSelected = { uri -> viewModel.importExcelFromUri(uri) },
        onError = { error -> viewModel.setError(error) }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                userName = userProfile.name,
                profilePictureUrl = userProfile.profilePictureUrl,
                onProfileClick = onNavigateToProfile?.let { rememberProfileClickHandler(it) }
            )
        }

        // Account Section
        item {
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "Manage your account details",
                    onClick = { onNavigateToProfile?.invoke() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    onClick = { viewModel.signOut() }
                )
            }
        }

        // Admin Section (Only visible to ADMIN users)
        if (uiState.canSeeAdminControls) {
            item {
                AdminSection(
                    onAddDriver = { driver -> viewModel.addDriver(driver) },
                    onAddVehicle = { vehicle -> viewModel.addVehicle(vehicle) },
                    onAddExpenseType = { name, displayName ->
                        viewModel.addExpenseType(name, displayName)
                    },
                    onImportExcel = { pickExcelFile() }
                )
            }
        }

        // Data & Sync Section
        item {
            SettingsSection(title = "Data & Sync") {
                SettingsToggleItem(
                    icon = Icons.Default.Sync,
                    title = "Auto Sync",
                    subtitle = if (uiState.autoSyncEnabled) "Enabled" else "Disabled",
                    checked = uiState.autoSyncEnabled,
                    onCheckedChange = { viewModel.toggleAutoSync(it) }
                )
                
                SettingsItem(
                    icon = Icons.Default.CloudSync,
                    title = "Sync Now",
                    subtitle = "Last synced: ${uiState.lastSyncTime}",
                    onClick = { viewModel.syncNow() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Data Export",
                    subtitle = if (uiState.isExporting) "Exporting..." else "Export your data to CSV",
                    onClick = { if (!uiState.isExporting) viewModel.exportData() }
                )
            }
        }

        // Notifications Section
        item {
            SettingsSection(title = "Notifications") {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    subtitle = if (uiState.notificationsEnabled) "Enabled" else "Disabled",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.Schedule,
                    title = "Daily Reminders",
                    subtitle = if (uiState.dailyRemindersEnabled) "Enabled" else "Disabled",
                    checked = uiState.dailyRemindersEnabled,
                    onCheckedChange = { viewModel.toggleDailyReminders(it) }
                )
            }
        }

        // App Section
        item {
            SettingsSection(title = "App") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = uiState.selectedTheme,
                    onClick = { /* TODO: Show theme picker */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English",
                    onClick = { /* TODO: Show language picker */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version ${uiState.appVersion}",
                    onClick = { /* TODO: Show about dialog */ }
                )
            }
        }

        // Support Section
        item {
            SettingsSection(title = "Support") {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help & FAQ",
                    subtitle = "Get help and find answers",
                    onClick = { /* TODO: Open help */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Feedback,
                    title = "Send Feedback",
                    subtitle = "Help us improve the app",
                    onClick = { /* TODO: Open feedback */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    subtitle = "Report issues or problems",
                    onClick = { /* TODO: Open bug report */ }
                )
            }
        }

        // Show sync status if syncing
        if (uiState.isSyncing) {
            item {
                StatusCard(
                    type = StatusType.Loading,
                    message = "Syncing data..."
                )
            }
        }
        
        // Show export status if exporting
        if (uiState.isExporting) {
            item {
                StatusCard(
                    type = StatusType.Loading,
                    message = "Exporting data to CSV..."
                )
            }
        }

        // Show error if any
        uiState.error?.let { error ->
            item {
                StatusCard(
                    type = StatusType.Error,
                    message = error
                )
            }
        }
        
        // Show import progress if importing
        if (uiState.isImporting || uiState.importProgress != null) {
            item {
                ImportProgressCard(
                    progress = uiState.importProgress,
                    onDismiss = { viewModel.clearImportProgress() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSection(
    onAddDriver: (Driver) -> Unit,
    onAddVehicle: (Vehicle) -> Unit,
    onAddExpenseType: (String, String) -> Unit,
    onImportExcel: () -> Unit
) {
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddExpenseTypeDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Admin Controls") {
        SettingsItem(
            icon = Icons.Default.PersonAdd,
            title = "Add Driver",
            subtitle = "Add a driver profile with cost details",
            onClick = { showAddDriverDialog = true }
        )
        
        SettingsItem(
            icon = Icons.Default.DirectionsCar,
            title = "Add Vehicle",
            subtitle = "Add a new vehicle to the fleet",
            onClick = { showAddVehicleDialog = true }
        )
        
        SettingsItem(
            icon = Icons.Default.Receipt,
            title = "Add Expense Type",
            subtitle = "Create a new expense category",
            onClick = { showAddExpenseTypeDialog = true }
        )
        
        SettingsItem(
            icon = Icons.Default.Upload,
            title = "Import CSV Entries",
            subtitle = "Import past income data from CSV file (exported from Excel)",
            onClick = onImportExcel
        )
    }

    // Add Driver Dialog
    if (showAddDriverDialog) {
        AddDriverDialog(
            onDismiss = { showAddDriverDialog = false },
            onConfirm = { driver ->
                onAddDriver(driver)
                showAddDriverDialog = false
            }
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { vehicle ->
                onAddVehicle(vehicle)
                showAddVehicleDialog = false
            }
        )
    }

    // Add Expense Type Dialog
    if (showAddExpenseTypeDialog) {
        AddExpenseTypeDialog(
            onDismiss = { showAddExpenseTypeDialog = false },
            onConfirm = { name, displayName ->
                onAddExpenseType(name, displayName)
                showAddExpenseTypeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDriverDialog(
    onDismiss: () -> Unit,
    onConfirm: (Driver) -> Unit
) {
    var driverId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var annualLicenseCost by remember { mutableStateOf("") }
    var annualVisaCost by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    val salaryValue = salary.trim().toDoubleOrNull()
    val licenseCostValue = annualLicenseCost.trim().toDoubleOrNull()
    val visaCostValue = annualVisaCost.trim().toDoubleOrNull()
    val isFormValid = driverId.isNotBlank() &&
            name.isNotBlank() &&
            salaryValue != null &&
            licenseCostValue != null &&
            visaCostValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Driver") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Provide the driver's full profile and compensation details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = driverId,
                    onValueChange = { driverId = it },
                    label = { Text("Driver ID *") },
                    placeholder = { Text("e.g., DRV-001") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g., John Smith") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary (AED) *") },
                    placeholder = { Text("e.g., 3500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = annualLicenseCost,
                    onValueChange = { annualLicenseCost = it },
                    label = { Text("Annual License Cost (AED) *") },
                    placeholder = { Text("e.g., 1200") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = annualVisaCost,
                    onValueChange = { annualVisaCost = it },
                    label = { Text("Annual Visa Cost (AED) *") },
                    placeholder = { Text("e.g., 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active Driver")
                        Text(
                            text = "Inactive drivers are kept for record only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        Driver(
                            id = driverId.trim(),
                            name = name.trim(),
                            salary = salaryValue ?: 0.0,
                            annualLicenseCost = licenseCostValue ?: 0.0,
                            annualVisaCost = visaCostValue ?: 0.0,
                            isActive = isActive
                        )
                    )
                },
                enabled = isFormValid
            ) {
                Text("Save Driver")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Vehicle) -> Unit
) {
    var vehicleId by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var installment by remember { mutableStateOf("") }
    var installmentDuration by remember { mutableStateOf("") }
    var serviceStartDate by remember { mutableStateOf("") }
    var serviceEndDate by remember { mutableStateOf("") }
    var annualInsuranceAmount by remember { mutableStateOf("") }
    var fuelTankCapacity by remember { mutableStateOf("") }
    var fuelConsumption by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    val yearValue = year.trim().toIntOrNull()
    val priceValue = price.trim().toDoubleOrNull()
    val depositValue = deposit.trim().toDoubleOrNull()
    val installmentValue = installment.trim().toDoubleOrNull()
    val installmentDurationValue = installmentDuration.trim().toIntOrNull()
    val annualInsuranceValue = annualInsuranceAmount.trim().toDoubleOrNull()
    val fuelTankValue = fuelTankCapacity.trim().toDoubleOrNull()
    val fuelConsumptionValue = fuelConsumption.trim().toDoubleOrNull()

    fun parseDate(input: String): java.util.Date? {
        if (input.isBlank()) return null
        return try {
            val localDate = java.time.LocalDate.parse(input)
            java.util.Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        } catch (e: java.time.format.DateTimeParseException) {
            null
        }
    }

    val serviceStartDateValue = parseDate(serviceStartDate)
    val serviceEndDateValue = parseDate(serviceEndDate)
    val areDatesValid = (serviceStartDate.isBlank() || serviceStartDateValue != null) &&
            (serviceEndDate.isBlank() || serviceEndDateValue != null)

    val isFormValid = vehicleId.isNotBlank() &&
            make.isNotBlank() &&
            model.isNotBlank() &&
            yearValue != null &&
            licensePlate.isNotBlank() &&
            priceValue != null &&
            annualInsuranceValue != null &&
            areDatesValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Vehicle") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = vehicleId,
                    onValueChange = { vehicleId = it },
                    label = { Text("Vehicle ID *") },
                    placeholder = { Text("e.g., VEH-1001") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text("Make *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Purchase Price (AED) *") },
                    placeholder = { Text("e.g., 85000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deposit,
                    onValueChange = { deposit = it },
                    label = { Text("Deposit (AED)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = installment,
                    onValueChange = { installment = it },
                    label = { Text("Monthly Installment (AED)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = installmentDuration,
                    onValueChange = { installmentDuration = it },
                    label = { Text("Installment Duration (months)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serviceStartDate,
                    onValueChange = { serviceStartDate = it },
                    label = { Text("Service Start Date (YYYY-MM-DD)") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (serviceStartDate.isNotBlank() && serviceStartDateValue == null) {
                            Text("Invalid date format")
                        }
                    }
                )
                OutlinedTextField(
                    value = serviceEndDate,
                    onValueChange = { serviceEndDate = it },
                    label = { Text("Service End Date (YYYY-MM-DD)") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (serviceEndDate.isNotBlank() && serviceEndDateValue == null) {
                            Text("Invalid date format")
                        }
                    }
                )
                OutlinedTextField(
                    value = annualInsuranceAmount,
                    onValueChange = { annualInsuranceAmount = it },
                    label = { Text("Annual Insurance Amount (AED) *") },
                    placeholder = { Text("e.g., 4500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fuelTankCapacity,
                    onValueChange = { fuelTankCapacity = it },
                    label = { Text("Fuel Tank Capacity (L)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fuelConsumption,
                    onValueChange = { fuelConsumption = it },
                    label = { Text("Fuel Consumption (L/100km)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active Vehicle")
                        Text(
                            text = "Inactive vehicles stay archived for history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        Vehicle(
                            id = vehicleId.trim(),
                            make = make.trim(),
                            model = model.trim(),
                            year = yearValue ?: 0,
                            licensePlate = licensePlate.trim(),
                            isActive = isActive,
                            price = priceValue ?: 0.0,
                            deposit = depositValue,
                            installment = installmentValue,
                            installmentDurationMonths = installmentDurationValue,
                            serviceStartDate = serviceStartDateValue,
                            serviceEndDate = serviceEndDateValue,
                            annualInsuranceAmount = annualInsuranceValue ?: 0.0,
                            fuelTankCapacity = fuelTankValue,
                            fuelConsumptionPer100Km = fuelConsumptionValue
                        )
                    )
                },
                enabled = isFormValid
            ) {
                Text("Save Vehicle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseTypeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Expense Type") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Internal Name *") },
                    placeholder = { Text("e.g., PARKING") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name *") },
                    placeholder = { Text("e.g., Parking Fees") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, displayName) },
                enabled = name.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Add Type")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportProgressCard(
    progress: ImportProgress?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CSV Import",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                if (progress?.progress == 100) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            progress?.let { prog ->
                Text(
                    text = prog.currentStep,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = prog.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${prog.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    if (prog.totalEntries > 0) {
                        Text(
                            text = "${prog.processedEntries}/${prog.totalEntries} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Show errors and warnings if any
                if (prog.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ ${prog.errors.size} errors occurred",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                if (prog.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ ${prog.warnings.size} warnings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
