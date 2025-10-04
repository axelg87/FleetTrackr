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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.AddEntryViewModel
import com.fleetmanager.ui.components.DriverInputComponent
import coil.compose.AsyncImage
import com.fleetmanager.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onNavigateBack: () -> Unit,
    entryId: String? = null,
    prefillDate: String? = null,
    prefillDriverId: String? = null,
    viewModel: AddEntryViewModel = hiltViewModel()
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
    
    LaunchedEffect(entryId) {
        if (!entryId.isNullOrBlank()) {
            viewModel.loadEntryForEdit(entryId)
        }
    }

    LaunchedEffect(prefillDate, prefillDriverId) {
        if (!prefillDate.isNullOrBlank()) {
            viewModel.applyNotificationPrefill(prefillDate, prefillDriverId)
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
                title = {
                    val titleRes = if (uiState.isEditing) R.string.edit_entry else R.string.add_entry
                    Text(stringResource(titleRes))
                },
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
                    label = { Text(stringResource(R.string.date)) },
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
            
            // Driver input with role-based restrictions
            DriverInputComponent(
                driverInput = uiState.driverInput,
                allDriverNames = uiState.allDriverNames,
                isDropdownExpanded = uiState.driverDropdownExpanded,
                onDriverInputChange = viewModel::updateDriverInput,
                onToggleDropdown = viewModel::toggleDriverDropdown,
                userRole = uiState.userRole,
                currentUserName = uiState.currentUserProfile?.name,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.driver_name)
            )
            
            // Vehicle dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.vehicleDropdownExpanded,
                onExpandedChange = viewModel::toggleVehicleDropdown
            ) {
                OutlinedTextField(
                    value = uiState.vehicleInput,
                    onValueChange = viewModel::updateVehicleInput,
                    readOnly = false,
                    label = { Text(stringResource(R.string.vehicle)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.vehicleDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = uiState.vehicleDropdownExpanded,
                    onDismissRequest = { viewModel.toggleVehicleDropdown(false) }
                ) {
                    if (uiState.allVehicleNames.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No vehicles available") },
                            onClick = { }
                        )
                    } else {
                        uiState.allVehicleNames.forEach { vehicleName ->
                            DropdownMenuItem(
                                text = { Text(vehicleName) },
                                onClick = {
                                    viewModel.updateVehicleInput(vehicleName)
                                    viewModel.toggleVehicleDropdown(false)
                                }
                            )
                        }
                    }
                }
            }
            
            OutlinedTextField(
                value = uiState.odometer,
                onValueChange = viewModel::updateOdometer,
                label = { Text(stringResource(R.string.odometer)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.odometerError != null
            )

            uiState.odometerError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = stringResource(R.string.earnings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            uiState.earnings.forEach { earning ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = earning.provider,
                                onValueChange = { viewModel.updateEarningProvider(earning.id, it) },
                                label = { Text(stringResource(R.string.provider)) },
                                modifier = Modifier.weight(1f)
                            )

                            if (uiState.earnings.size > 1) {
                                IconButton(onClick = { viewModel.removeEarningInput(earning.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_provider)
                                    )
                                }
                            }
                        }

                        if (earning.providerError != null) {
                            Text(
                                text = earning.providerError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = earning.card,
                                onValueChange = { viewModel.updateEarningCard(earning.id, it) },
                                label = { Text(stringResource(R.string.card_earnings)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = earning.cash,
                                onValueChange = { viewModel.updateEarningCash(earning.id, it) },
                                label = { Text(stringResource(R.string.cash_earnings)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = earning.tips,
                                onValueChange = { viewModel.updateEarningTips(earning.id, it) },
                                label = { Text(stringResource(R.string.tips)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = earning.tripCount,
                                onValueChange = { viewModel.updateEarningTripCount(earning.id, it) },
                                label = { Text(stringResource(R.string.trip_count)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = earning.hoursOnline,
                                onValueChange = { viewModel.updateEarningHoursOnline(earning.id, it) },
                                label = { Text(stringResource(R.string.hours_online)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        earning.amountError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = viewModel::addEarningInput,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_provider))
            }

            // Notes field
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(R.string.notes)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
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
                        text = "Photos",
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

                    if (uiState.existingPhotoUrls.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.existingPhotoUrls) { url ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Existing photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
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
            val saveButtonLabel = if (uiState.isEditing) R.string.update_entry else R.string.save
            Button(
                onClick = viewModel::saveEntry,
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
                        text = stringResource(saveButtonLabel),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Add some bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
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
