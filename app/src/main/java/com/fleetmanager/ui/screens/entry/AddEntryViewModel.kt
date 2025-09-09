package com.fleetmanager.ui.screens.entry

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.model.DailyEntry
import com.fleetmanager.data.model.Driver
import com.fleetmanager.data.model.Vehicle
import com.fleetmanager.data.repository.FleetRepository
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
    val date: Date = Date(),
    val uberEarnings: String = "",
    val yangoEarnings: String = "",
    val privateJobsEarnings: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = selectedDriver != null && selectedVehicle != null &&
                (uberEarnings.toDoubleOrNull() ?: 0.0) >= 0 &&
                (yangoEarnings.toDoubleOrNull() ?: 0.0) >= 0 &&
                (privateJobsEarnings.toDoubleOrNull() ?: 0.0) >= 0
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val fleetRepository: FleetRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            // Load drivers
            fleetRepository.getAllActiveDrivers()
                .catch { }
                .collect { drivers ->
                    _uiState.value = _uiState.value.copy(drivers = drivers)
                    
                    // Auto-add sample drivers if none exist
                    if (drivers.isEmpty()) {
                        addSampleData()
                    }
                }
        }
        
        viewModelScope.launch {
            // Load vehicles
            fleetRepository.getAllActiveVehicles()
                .catch { }
                .collect { vehicles ->
                    _uiState.value = _uiState.value.copy(vehicles = vehicles)
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
        
        sampleDrivers.forEach { fleetRepository.saveDriver(it) }
        sampleVehicles.forEach { fleetRepository.saveVehicle(it) }
    }
    
    fun selectDriver(driver: Driver) {
        _uiState.value = _uiState.value.copy(selectedDriver = driver)
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
    }
    
    fun updateUberEarnings(value: String) {
        _uiState.value = _uiState.value.copy(uberEarnings = value)
    }
    
    fun updateYangoEarnings(value: String) {
        _uiState.value = _uiState.value.copy(yangoEarnings = value)
    }
    
    fun updatePrivateJobsEarnings(value: String) {
        _uiState.value = _uiState.value.copy(privateJobsEarnings = value)
    }
    
    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }
    
    fun updatePhotoUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }
    
    fun toggleDriverDropdown(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(driverDropdownExpanded = expanded)
    }
    
    fun toggleVehicleDropdown(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(vehicleDropdownExpanded = expanded)
    }
    
    fun saveEntry() {
        val currentState = _uiState.value
        if (!currentState.canSave) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            
            try {
                val entry = DailyEntry(
                    id = UUID.randomUUID().toString(),
                    date = currentState.date,
                    driverName = currentState.selectedDriver!!.name,
                    vehicle = currentState.selectedVehicle!!.displayName,
                    uberEarnings = currentState.uberEarnings.toDoubleOrNull() ?: 0.0,
                    yangoEarnings = currentState.yangoEarnings.toDoubleOrNull() ?: 0.0,
                    privateJobsEarnings = currentState.privateJobsEarnings.toDoubleOrNull() ?: 0.0,
                    notes = currentState.notes,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                fleetRepository.saveDailyEntry(entry, currentState.photoUri)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save entry"
                )
            }
        }
    }
}