package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import java.util.Calendar

data class AddEntryUiState(
    val driverUsers: List<UserDto> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val date: Date,
    val uberEarnings: String = "",
    val yangoEarnings: String = "",
    val privateJobsEarnings: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val existingPhotoUrls: List<String> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val uberEarningsError: String? = null,
    val yangoEarningsError: String? = null,
    val privateJobsEarningsError: String? = null,
    val notesError: String? = null,
    val userRole: UserRole? = null,
    val currentUserProfile: UserDto? = null,
    val entryId: String? = null,
    val userId: String = "",
    val createdAt: Date? = null,
    val isEditing: Boolean = false
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
    private val getEntryByIdUseCase: com.fleetmanager.domain.usecase.GetEntryByIdUseCase
) : BaseViewModel<AddEntryUiState>() {
    
    override fun getInitialState() = AddEntryUiState(
        date = getDefaultDate()
    )
    
    /**
     * Calculate the default date based on the 2PM rule:
     * - If current time is before 2:00 PM, use yesterday's date
     * - Otherwise, use today's date
     */
    private fun getDefaultDate(): Date {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        return if (currentHour < 14) { // Before 2:00 PM (14:00)
            // Use yesterday's date
            now.add(Calendar.DAY_OF_MONTH, -1)
            now.time
        } else {
            // Use today's date
            now.time
        }
    }
    
    init {
        loadFirestoreData()
        loadUserProfile()
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
                    val autoFilledDriverName = if (shouldAutoFillDriver(currentState)) {
                        val userProfile = currentState.currentUserProfile
                        driverUsers.firstOrNull { it.id == userProfile?.id }?.name
                            ?: userProfile?.name
                            ?: currentState.driverInput
                    } else {
                        currentState.driverInput
                    }

                    currentState.copy(
                        driverUsers = driverUsers,
                        vehicles = vehicles,
                        driverInput = autoFilledDriverName
                    )
                }
            }
        }
    }

    private fun loadUserProfile() {
        executeAsync(
            onError = { error ->
                // Don't show error for user profile loading, just continue
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { currentState ->
                    val updatedState = currentState.copy(
                        currentUserProfile = userProfile,
                        userRole = userProfile.role
                    )

                    if (shouldAutoFillDriver(updatedState)) {
                        val driverName = updatedState.driverUsers.firstOrNull { it.id == userProfile.id }?.name
                            ?: userProfile.name

                        updatedState.copy(driverInput = driverName)
                    } else {
                        updatedState
                    }
                }
            }
        }
    }

    private fun shouldAutoFillDriver(state: AddEntryUiState): Boolean {
        val role = state.userRole ?: state.currentUserProfile?.role
        if (state.isEditing || state.driverInput.isNotBlank()) {
            return false
        }
        return role == UserRole.DRIVER || role == UserRole.MANAGER
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

        val driverId = currentState.selectedDriver?.id
            ?: currentState.driverUsers.firstOrNull { it.name.equals(currentState.driverInput, ignoreCase = true) }?.id
            ?: ""
        val vehicleId = currentState.selectedVehicle?.id
            ?: currentState.vehicles.firstOrNull { it.displayName == currentState.vehicleInput }?.id
            ?: ""

        if (driverId.isBlank() || vehicleId.isBlank()) {
            updateState {
                it.copy(errorMessage = "Please select a valid driver and vehicle")
            }
            return
        }

        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isSaving = false, errorMessage = error) }
            }
        ) {
            val now = Date()
            val entryIdToUse = currentState.entryId ?: UUID.randomUUID().toString()
            val createdAt = currentState.createdAt ?: now

            val entry = DailyEntry(
                id = entryIdToUse,
                userId = currentState.userId,
                date = currentState.date,
                driverId = driverId,
                driverName = currentState.driverInput,
                vehicleId = vehicleId,
                vehicle = currentState.vehicleInput,
                uberEarnings = currentState.uberEarnings.toDoubleOrNull() ?: 0.0,
                yangoEarnings = currentState.yangoEarnings.toDoubleOrNull() ?: 0.0,
                privateJobsEarnings = currentState.privateJobsEarnings.toDoubleOrNull() ?: 0.0,
                notes = currentState.notes,
                photoUrls = currentState.existingPhotoUrls,
                createdAt = createdAt,
                updatedAt = now
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

    fun loadEntryForEdit(entryId: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = false, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .filterNotNull()
                .firstOrNull()
                ?.let { entry ->
                    updateState { state ->
                        state.copy(
                            entryId = entry.id,
                            userId = entry.userId,
                            isEditing = true,
                            date = entry.date,
                            driverInput = entry.driverName,
                            vehicleInput = entry.vehicle,
                            uberEarnings = entry.uberEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            yangoEarnings = entry.yangoEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            privateJobsEarnings = entry.privateJobsEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            notes = entry.notes,
                            existingPhotoUrls = entry.photoUrls,
                            createdAt = entry.createdAt,
                            errorMessage = null,
                            isSaved = false
                        )
                    }
                } ?: updateState {
                it.copy(errorMessage = "Entry not found")
            }
        }
    }
}