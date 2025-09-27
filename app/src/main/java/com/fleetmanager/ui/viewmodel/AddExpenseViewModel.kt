package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.domain.model.ExpenseTypeItem
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.remote.ExpenseTypeFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.SaveExpenseUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

data class AddExpenseUiState(
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val expenseTypes: List<ExpenseTypeItem> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedDriverId: String? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val selectedExpenseType: ExpenseType = ExpenseType.OTHER,
    val date: Date = Date(),
    val amount: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val existingPhotoUrls: List<String> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val expenseTypeDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val notesError: String? = null,
    val userRole: UserRole? = null,
    val currentUserProfile: UserDto? = null,
    val expenseId: String? = null,
    val userId: String = "",
    val createdAt: Date? = null,
    val isEditing: Boolean = false
) {
    val canSave: Boolean
        get() = driverInput.isNotBlank() &&
                resolvedDriverId != null &&
                vehicleInput.isNotBlank() &&
                amount.isNotBlank() &&
                amountError == null &&
                notesError == null &&
                (amount.toDoubleOrNull() ?: 0.0) > 0

    val hasValidationErrors: Boolean
        get() = amountError != null || notesError != null

    val resolvedDriverId: String?
        get() {
            selectedDriver?.id?.let { return it }
            selectedDriverId?.takeUnless { it.isBlank() }?.let { return it }
            if (driverInput.isBlank()) return null
            return drivers.firstOrNull { driver ->
                driver.name.equals(driverInput, ignoreCase = true) || driver.id.equals(driverInput, ignoreCase = true)
            }?.id
        }

    // Driver names from Firestore only
    val allDriverNames: List<String>
        get() = drivers.map { it.name }.sorted()
    
    // Vehicle names from Firestore only
    val allVehicleNames: List<String>
        get() = vehicles.map { it.displayName }.sorted()
    
    // Expense types from Firestore only
    val allExpenseTypes: List<String>
        get() = expenseTypes.map { it.displayName }.sorted()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val firestoreService: FirestoreService,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val expenseTypeFirestoreService: ExpenseTypeFirestoreService,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val validator: InputValidator,
    private val getExpenseByIdUseCase: com.fleetmanager.domain.usecase.GetExpenseByIdUseCase
) : BaseViewModel<AddExpenseUiState>() {
    
    override fun getInitialState() = AddExpenseUiState()
    
    init {
        loadFirestoreData()
        loadUserProfile()
    }
    
    
    private fun loadFirestoreData() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = "Failed to load data: AEDerror") }
            }
        ) {
            combine(
                firestoreService.getDriversFlow(),
                vehicleFirestoreService.getVehiclesFlow(),
                expenseTypeFirestoreService.getExpenseTypesFlow()
            ) { drivers, vehicles, expenseTypes ->
                Triple(drivers, vehicles, expenseTypes)
            }.collect { (drivers, vehicles, expenseTypes) ->
                updateState { currentState ->
                    // If this is the first load and we have expense types, select the first one
                    val selectedExpenseType = if (currentState.expenseTypes.isEmpty() && expenseTypes.isNotEmpty()) {
                        // Find the matching ExpenseType enum for the first expense type, or use OTHER as fallback
                        ExpenseType.values().find { it.displayName == expenseTypes.first().displayName } ?: ExpenseType.OTHER
                    } else {
                        currentState.selectedExpenseType
                    }

                    val shouldAutoFill = shouldAutoFillDriver(currentState)
                    val userProfile = currentState.currentUserProfile
                    val candidateDriverId = when {
                        shouldAutoFill -> drivers.firstOrNull { it.userId == userProfile?.id }?.id
                            ?: userProfile?.id
                        !currentState.selectedDriverId.isNullOrBlank() -> currentState.selectedDriverId
                        else -> drivers.firstOrNull { driver ->
                            driver.name.equals(currentState.driverInput, ignoreCase = true) ||
                                driver.id.equals(currentState.driverInput, ignoreCase = true)
                        }?.id
                    }

                    val resolvedDriver = candidateDriverId?.let { id ->
                        drivers.firstOrNull { it.id == id }
                    }

                    val driverInputValue = when {
                        shouldAutoFill -> resolvedDriver?.name
                            ?: userProfile?.name
                            ?: currentState.driverInput
                        currentState.driverInput.isBlank() && resolvedDriver != null -> resolvedDriver.name
                        else -> currentState.driverInput
                    }

                    currentState.copy(
                        drivers = drivers,
                        vehicles = vehicles,
                        expenseTypes = expenseTypes,
                        selectedExpenseType = selectedExpenseType,
                        selectedDriver = resolvedDriver,
                        selectedDriverId = candidateDriverId ?: currentState.selectedDriverId,
                        driverInput = driverInputValue
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
                        val matchingDriver = updatedState.drivers.firstOrNull { it.userId == userProfile.id }
                        val driverName = matchingDriver?.name ?: userProfile.name

                        updatedState.copy(
                            driverInput = driverName,
                            selectedDriver = matchingDriver ?: updatedState.selectedDriver,
                            selectedDriverId = matchingDriver?.id ?: userProfile.id
                        )
                    } else {
                        updatedState
                    }
                }
            }
        }
    }

    private fun shouldAutoFillDriver(state: AddExpenseUiState): Boolean {
        val role = state.userRole ?: state.currentUserProfile?.role
        if (state.isEditing || state.driverInput.isNotBlank()) {
            return false
        }
        return role == UserRole.DRIVER || role == UserRole.MANAGER
    }
    
    fun selectDriver(driver: Driver) {
        updateState {
            it.copy(
                selectedDriver = driver,
                selectedDriverId = driver.id,
                driverInput = driver.name
            )
        }
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        updateState { it.copy(selectedVehicle = vehicle, vehicleInput = vehicle.displayName) }
    }
    
    fun updateDriverInput(input: String) {
        updateState { state ->
            val matchingDriver = state.drivers.firstOrNull { driver ->
                driver.name.equals(input, ignoreCase = true) || driver.id.equals(input, ignoreCase = true)
            }
            state.copy(
                driverInput = input,
                selectedDriver = matchingDriver,
                selectedDriverId = matchingDriver?.id
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
    
    fun selectExpenseType(expenseType: ExpenseType) {
        updateState { it.copy(selectedExpenseType = expenseType) }
    }
    
    fun updateAmount(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Amount").getErrorMessage()
        updateState { 
            it.copy(
                amount = sanitized,
                amountError = error
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
    
    fun toggleExpenseTypeDropdown(expanded: Boolean) {
        updateState { it.copy(expenseTypeDropdownExpanded = expanded) }
    }
    
    fun toggleDatePicker(show: Boolean) {
        updateState { it.copy(showDatePicker = show) }
    }
    
    fun saveExpense() {
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
            val now = Date()
            val expenseId = currentState.expenseId ?: UUID.randomUUID().toString()
            val createdAt = currentState.createdAt ?: now
            val driverId = currentState.resolvedDriverId

            if (driverId.isNullOrBlank()) {
                updateState { it.copy(isSaving = false, errorMessage = "Please select a driver from the list") }
                return@executeAsync
            }

            val driverName = currentState.drivers.firstOrNull { it.id == driverId }?.name
                ?: currentState.driverInput

            val expense = Expense(
                id = expenseId,
                userId = driverId,
                driverId = driverId,
                type = currentState.selectedExpenseType,
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                date = currentState.date,
                driverName = driverName,
                vehicle = currentState.vehicleInput,
                notes = currentState.notes,
                photoUrls = currentState.existingPhotoUrls,
                createdAt = createdAt,
                updatedAt = now
            )

            val result = saveExpenseUseCase(expense, currentState.photoUri, currentState.photoUris)
            result.fold(
                onSuccess = {
                    updateState { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { error ->
                    updateState { 
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save expense"
                        )
                    }
                }
            )
        }
    }

    fun loadExpenseForEdit(expenseId: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = false, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            getExpenseByIdUseCase(expenseId)
                .filterNotNull()
                .firstOrNull()
                ?.let { expense ->
                    val resolvedDriverId = expense.driverId.takeIf { it.isNotBlank() } ?: expense.userId
                    updateState { state ->
                        state.copy(
                            expenseId = expense.id,
                            userId = expense.userId,
                            isEditing = true,
                            date = expense.date,
                            selectedExpenseType = expense.type,
                            amount = expense.amount.takeIf { it != 0.0 }?.toString() ?: "",
                            driverInput = expense.driverName,
                            selectedDriverId = resolvedDriverId,
                            selectedDriver = state.drivers.firstOrNull { it.id == resolvedDriverId }
                                ?: state.selectedDriver,
                            vehicleInput = expense.vehicle,
                            notes = expense.notes,
                            existingPhotoUrls = expense.photoUrls,
                            createdAt = expense.createdAt,
                            errorMessage = null,
                            isSaved = false
                        )
                    }
                } ?: updateState {
                it.copy(errorMessage = "Expense not found")
            }
        }
    }
}