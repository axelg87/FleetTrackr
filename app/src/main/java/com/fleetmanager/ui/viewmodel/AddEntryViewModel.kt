package com.fleetmanager.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
import com.fleetmanager.domain.validation.InputValidator
import com.fleetmanager.ui.utils.ToastHelper
import com.fleetmanager.ui.utils.LocalizedStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddEntryUiState(
    val driverUsers: List<UserDto> = emptyList(),
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
    
    // Driver names from Firestore only
    val allDriverNames: List<String>
        get() = driverUsers.map { it.name }.sorted()
    
    // Vehicle names from Firestore only
    val allVehicleNames: List<String>
        get() = vehicles.map { it.displayName }.sorted()
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val saveDailyEntryUseCase: SaveDailyEntryUseCase,
    private val validator: InputValidator,
    private val toastHelper: ToastHelper,
    @ApplicationContext private val context: Context
) : BaseViewModel<AddEntryUiState>() {
    
    override fun getInitialState() = AddEntryUiState()
    
    init {
        loadFirestoreData()
    }
    
    
    private fun loadFirestoreData() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = "Failed to load data: $error") }
            }
        ) {
            combine(
                userFirestoreService.getDriverUsersFlow(),
                vehicleFirestoreService.getVehiclesFlow()
            ) { driverUsers, vehicles ->
                Pair(driverUsers, vehicles)
            }.collect { (driverUsers, vehicles) ->
                updateState { currentState ->
                    currentState.copy(
                        driverUsers = driverUsers,
                        vehicles = vehicles
                    )
                }
            }
        }
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
                selectedDriver = null // We no longer maintain selectedDriver state
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
                    toastHelper.showSuccess(context, LocalizedStrings.Success.INCOME_ENTRY_ADDED)
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