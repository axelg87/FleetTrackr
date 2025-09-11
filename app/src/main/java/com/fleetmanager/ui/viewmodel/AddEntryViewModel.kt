package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.usecase.GetActiveDriversUseCase
import com.fleetmanager.domain.usecase.GetActiveVehiclesUseCase
import com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
import com.fleetmanager.domain.usecase.SaveDriverUseCase
import com.fleetmanager.domain.usecase.SaveVehicleUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddEntryUiState(
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val date: Date = Date(),
    val uberEarnings: String = "",
    val yangoEarnings: String = "",
    val privateJobsEarnings: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val uberEarningsError: String? = null,
    val yangoEarningsError: String? = null,
    val privateJobsEarningsError: String? = null,
    val notesError: String? = null
) {
    val canSave: Boolean
        get() = driverInput.isNotBlank() && 
                vehicleInput.isNotBlank() &&
                uberEarningsError == null &&
                yangoEarningsError == null &&
                privateJobsEarningsError == null &&
                notesError == null &&
                (uberEarnings.isNotBlank() || yangoEarnings.isNotBlank() || privateJobsEarnings.isNotBlank())
    
    val hasValidationErrors: Boolean
        get() = uberEarningsError != null || 
                yangoEarningsError != null || 
                privateJobsEarningsError != null || 
                notesError != null
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val getActiveDriversUseCase: GetActiveDriversUseCase,
    private val getActiveVehiclesUseCase: GetActiveVehiclesUseCase,
    private val saveDailyEntryUseCase: SaveDailyEntryUseCase,
    private val saveDriverUseCase: SaveDriverUseCase,
    private val saveVehicleUseCase: SaveVehicleUseCase,
    private val validator: InputValidator
) : BaseViewModel<AddEntryUiState>() {
    
    override fun getInitialState() = AddEntryUiState(
        drivers = getSampleDrivers(),
        vehicles = getSampleVehicles()
    )
    
    init {
        loadInitialData()
    }
    
    private fun getSampleDrivers(): List<Driver> {
        return listOf(
            Driver("sample_driver_1", "Usman"),
            Driver("sample_driver_2", "Ahmed"),
            Driver("sample_driver_3", "Rashid")
        )
    }
    
    private fun getSampleVehicles(): List<Vehicle> {
        return listOf(
            Vehicle("sample_vehicle_1", "Mitsubishi", "Outlander 1", 2020, "ABC-123"),
            Vehicle("sample_vehicle_2", "Mitsubishi", "Outlander 2", 2021, "XYZ-789"),
            Vehicle("sample_vehicle_3", "Mitsubishi", "Outlander 3", 2022, "DEF-456")
        )
    }
    
    private fun loadInitialData() {
        executeAsync {
            // Load drivers from database and merge with sample data
            getActiveDriversUseCase()
                .catch { }
                .collect { dbDrivers ->
                    val allDrivers = (getSampleDrivers() + dbDrivers).distinctBy { it.name }
                    updateState { it.copy(drivers = allDrivers) }
                }
        }
        
        executeAsync {
            // Load vehicles from database and merge with sample data
            getActiveVehiclesUseCase()
                .catch { }
                .collect { dbVehicles ->
                    val allVehicles = (getSampleVehicles() + dbVehicles).distinctBy { it.displayName }
                    updateState { it.copy(vehicles = allVehicles) }
                }
        }
    }
    
    private suspend fun addSampleData() {
        // Add some sample drivers and vehicles for demo
        val sampleDrivers = listOf(
            Driver("driver_1", "John Smith"),
            Driver("driver_2", "Maria Garcia"),
            Driver("driver_3", "Ahmed Hassan")
        )
        
        val sampleVehicles = listOf(
            Vehicle("vehicle_1", "Toyota", "Camry", 2020, "ABC-123"),
            Vehicle("vehicle_2", "Honda", "Accord", 2019, "XYZ-789"),
            Vehicle("vehicle_3", "Hyundai", "Elantra", 2021, "DEF-456")
        )
        
        sampleDrivers.forEach { saveDriverUseCase(it) }
        sampleVehicles.forEach { saveVehicleUseCase(it) }
    }
    
    fun selectDriver(driver: Driver) {
        updateState { it.copy(selectedDriver = driver, driverInput = driver.name) }
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        updateState { it.copy(selectedVehicle = vehicle, vehicleInput = vehicle.displayName) }
    }
    
    fun updateDriverInput(input: String) {
        updateState { 
            it.copy(
                driverInput = input,
                selectedDriver = it.drivers.find { driver -> driver.name == input }
            ) 
        }
    }
    
    fun updateVehicleInput(input: String) {
        updateState { 
            it.copy(
                vehicleInput = input,
                selectedVehicle = it.vehicles.find { vehicle -> vehicle.displayName == input }
            ) 
        }
    }
    
    fun updateUberEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Uber earnings").getErrorMessage()
        updateState { 
            it.copy(
                uberEarnings = sanitized,
                uberEarningsError = error
            ) 
        }
    }
    
    fun updateYangoEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Yango earnings").getErrorMessage()
        updateState { 
            it.copy(
                yangoEarnings = sanitized,
                yangoEarningsError = error
            ) 
        }
    }
    
    fun updatePrivateJobsEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Private jobs earnings").getErrorMessage()
        updateState { 
            it.copy(
                privateJobsEarnings = sanitized,
                privateJobsEarningsError = error
            ) 
        }
    }
    
    fun updateNotes(value: String) {
        val sanitized = validator.sanitizeText(value)
        val error = validator.validateNotes(sanitized).getErrorMessage()
        updateState { 
            it.copy(
                notes = sanitized,
                notesError = error
            ) 
        }
    }
    
    fun updatePhotoUri(uri: Uri?) {
        updateState { it.copy(photoUri = uri) }
    }
    
    fun addPhotoUris(uris: List<Uri>) {
        updateState { currentState ->
            val currentUris = currentState.photoUris.toMutableList()
            currentUris.addAll(uris)
            currentState.copy(photoUris = currentUris)
        }
    }
    
    fun removePhotoUri(uri: Uri) {
        updateState { currentState ->
            val currentUris = currentState.photoUris.toMutableList()
            currentUris.remove(uri)
            currentState.copy(photoUris = currentUris)
        }
    }
    
    fun updateDate(date: Date) {
        updateState { it.copy(date = date) }
    }
    
    fun toggleDriverDropdown(expanded: Boolean) {
        updateState { it.copy(driverDropdownExpanded = expanded) }
    }
    
    fun toggleVehicleDropdown(expanded: Boolean) {
        updateState { it.copy(vehicleDropdownExpanded = expanded) }
    }
    
    fun toggleDatePicker(show: Boolean) {
        updateState { it.copy(showDatePicker = show) }
    }
    
    fun saveEntry() {
        val currentState = uiState.value
        if (!currentState.canSave) return
        
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isSaving = false, errorMessage = error) }
            }
        ) {
            val entry = DailyEntry(
                id = UUID.randomUUID().toString(),
                date = currentState.date,
                driverName = currentState.driverInput,
                vehicle = currentState.vehicleInput,
                uberEarnings = currentState.uberEarnings.toDoubleOrNull() ?: 0.0,
                yangoEarnings = currentState.yangoEarnings.toDoubleOrNull() ?: 0.0,
                privateJobsEarnings = currentState.privateJobsEarnings.toDoubleOrNull() ?: 0.0,
                notes = currentState.notes,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val result = saveDailyEntryUseCase(entry, currentState.photoUri, currentState.photoUris)
            result.fold(
                onSuccess = {
                    updateState { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { error ->
                    updateState { 
                        it.copy(
                            isSaving = false, 
                            errorMessage = error.message ?: "Failed to save entry"
                        ) 
                    }
                }
            )
        }
    }
}