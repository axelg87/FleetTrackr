package com.fleetmanager.ui.screens.entry

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.AddExpenseViewModel
import coil.compose.AsyncImage
import com.fleetmanager.domain.model.ExpenseType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotoUris(uris)
        }
    }
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date field - Click anywhere to open date picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleDatePicker(true) }
            ) {
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(uiState.date),
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Date picker dialog
            if (uiState.showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date.time
                )
                DatePickerDialog(
                    onDateSelected = { dateMillis ->
                        dateMillis?.let {
                            viewModel.updateDate(Date(it))
                        }
                        viewModel.toggleDatePicker(false)
                    },
                    onDismiss = { viewModel.toggleDatePicker(false) },
                    datePickerState = datePickerState
                )
            }
            
            // Expense Type dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.expenseTypeDropdownExpanded,
                onExpandedChange = viewModel::toggleExpenseTypeDropdown
            ) {
                OutlinedTextField(
                    value = uiState.selectedExpenseType.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Expense Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.expenseTypeDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = uiState.expenseTypeDropdownExpanded,
                    onDismissRequest = { viewModel.toggleExpenseTypeDropdown(false) }
                ) {
                    ExpenseType.values().forEach { expenseType ->
                        DropdownMenuItem(
                            text = { Text(expenseType.displayName) },
                            onClick = {
                                viewModel.selectExpenseType(expenseType)
                                viewModel.toggleExpenseTypeDropdown(false)
                            }
                        )
                    }
                }
            }
            
            // Amount field
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } }
            )
            
            // Driver dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.driverDropdownExpanded,
                onExpandedChange = viewModel::toggleDriverDropdown
            ) {
                OutlinedTextField(
                    value = uiState.driverInput,
                    onValueChange = viewModel::updateDriverInput,
                    readOnly = false,
                    label = { Text("Driver") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.driverDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = uiState.driverDropdownExpanded,
                    onDismissRequest = { viewModel.toggleDriverDropdown(false) }
                ) {
                    uiState.drivers.forEach { driver ->
                        DropdownMenuItem(
                            text = { Text(driver.name) },
                            onClick = {
                                viewModel.selectDriver(driver)
                                viewModel.toggleDriverDropdown(false)
                            }
                        )
                    }
                }
            }
            
            // Vehicle dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.vehicleDropdownExpanded,
                onExpandedChange = viewModel::toggleVehicleDropdown
            ) {
                OutlinedTextField(
                    value = uiState.vehicleInput,
                    onValueChange = viewModel::updateVehicleInput,
                    readOnly = false,
                    label = { Text("Vehicle") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.vehicleDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = uiState.vehicleDropdownExpanded,
                    onDismissRequest = { viewModel.toggleVehicleDropdown(false) }
                ) {
                    uiState.vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.displayName) },
                            onClick = {
                                viewModel.selectVehicle(vehicle)
                                viewModel.toggleVehicleDropdown(false)
                            }
                        )
                    }
                }
            }
            
            // Notes field
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes (Optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.notesError != null,
                supportingText = uiState.notesError?.let { { Text(it) } }
            )
            
            // Photo section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Photos (Optional)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    // Display single photo if exists (legacy support)
                    if (uiState.photoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AsyncImage(
                                model = uiState.photoUri,
                                contentDescription = "Selected photo",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.updatePhotoUri(null) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo")
                            }
                        }
                    }
                    
                    // Display multiple photos
                    if (uiState.photoUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.photoUris) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    IconButton(
                                        onClick = { viewModel.removePhotoUri(uri) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Remove photo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Photo selection button
                    OutlinedButton(
                        onClick = {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Photos")
                    }
                }
            }
            
            // Error message
            uiState.errorMessage?.let { error ->
                if (error.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Save button
            Button(
                onClick = viewModel::saveExpense,
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Save Expense",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Add some bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}