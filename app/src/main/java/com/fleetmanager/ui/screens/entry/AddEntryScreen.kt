package com.fleetmanager.ui.screens.entry

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fleetmanager.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.updatePhotoUri(it) }
    }
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_entry)) },
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
            // Date field
            OutlinedTextField(
                value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(uiState.date),
                onValueChange = { },
                label = { Text(stringResource(R.string.date)) },
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Driver dropdown
            ExposedDropdownMenuBox(
                expanded = uiState.driverDropdownExpanded,
                onExpandedChange = viewModel::toggleDriverDropdown
            ) {
                OutlinedTextField(
                    value = uiState.selectedDriver?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.driver_name)) },
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
                    value = uiState.selectedVehicle?.displayName ?: "",
                    onValueChange = { },
                    readOnly = true,
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
            
            // Earnings fields
            OutlinedTextField(
                value = uiState.uberEarnings,
                onValueChange = viewModel::updateUberEarnings,
                label = { Text(stringResource(R.string.uber_earnings)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = uiState.yangoEarnings,
                onValueChange = viewModel::updateYangoEarnings,
                label = { Text(stringResource(R.string.yango_earnings)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = uiState.privateJobsEarnings,
                onValueChange = viewModel::updatePrivateJobsEarnings,
                label = { Text(stringResource(R.string.private_jobs)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
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
                        text = stringResource(R.string.photo),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    if (uiState.photoUri != null) {
                        AsyncImage(
                            model = uiState.photoUri,
                            contentDescription = "Selected photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.photoUri != null) "Change Photo" else stringResource(R.string.select_photo)
                        )
                    }
                }
            }
            
            // Error message
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Save button
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
                        text = stringResource(R.string.save),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Add some bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}