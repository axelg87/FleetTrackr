package com.fleetmanager.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.SettingsViewModel
import com.fleetmanager.ui.components.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(title = "Settings")
        }

        // Account Section
        item {
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "Manage your account details",
                    onClick = { /* TODO: Navigate to profile */ }
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
        if (uiState.isAdmin) {
            item {
                AdminSection(
                    onAddDriver = { name, email -> viewModel.addDriver(name, email) },
                    onAddVehicle = { make, model, year, licensePlate -> 
                        viewModel.addVehicle(make, model, year, licensePlate) 
                    },
                    onAddExpenseType = { name, displayName -> 
                        viewModel.addExpenseType(name, displayName) 
                    }
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
                    subtitle = "Export your data to CSV",
                    onClick = { viewModel.exportData() }
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

        // Show error if any
        uiState.error?.let { error ->
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
private fun AdminSection(
    onAddDriver: (String, String) -> Unit,
    onAddVehicle: (String, String, Int, String) -> Unit,
    onAddExpenseType: (String, String) -> Unit
) {
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddExpenseTypeDialog by remember { mutableStateOf(false) }

    SettingsSection(title = "Admin Controls") {
        SettingsItem(
            icon = Icons.Default.PersonAdd,
            title = "Add Driver",
            subtitle = "Create a new driver user account",
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
    }

    // Add Driver Dialog
    if (showAddDriverDialog) {
        AddDriverDialog(
            onDismiss = { showAddDriverDialog = false },
            onConfirm = { name, email ->
                onAddDriver(name, email)
                showAddDriverDialog = false
            }
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { make, model, year, licensePlate ->
                onAddVehicle(make, model, year, licensePlate)
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
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Driver") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Driver Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, email) },
                enabled = name.isNotBlank()
            ) {
                Text("Add Driver")
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
    onConfirm: (String, String, Int, String) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Vehicle") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val yearInt = year.toIntOrNull() ?: 0
                    onConfirm(make, model, yearInt, licensePlate) 
                },
                enabled = make.isNotBlank() && model.isNotBlank() && 
                         year.toIntOrNull() != null && licensePlate.isNotBlank()
            ) {
                Text("Add Vehicle")
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
