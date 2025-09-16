package com.fleetmanager.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.domain.model.ExpenseTypeItem
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.remote.ExpenseTypeFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.usecase.SaveExpenseUseCase
import com.fleetmanager.domain.validation.InputValidator
import com.fleetmanager.ui.utils.ToastHelper
import com.fleetmanager.ui.utils.LocalizedStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddExpenseUiState(
    val driverUsers: List<UserDto> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val expenseTypes: List<ExpenseTypeItem> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val selectedExpenseType: ExpenseType = ExpenseType.OTHER,
    val date: Date = Date(),
    val amount: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val expenseTypeDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val notesError: String? = null
) {
    val canSave: Boolean
        get() = driverInput.isNotBlank() && 
                vehicleInput.isNotBlank() &&
                amount.isNotBlank() &&
                amountError == null &&
                notesError == null &&
                (amount.toDoubleOrNull() ?: 0.0) > 0
    
    val hasValidationErrors: Boolean
        get() = amountError != null || notesError != null
    
    // Driver names from Firestore only
    val allDriverNames: List<String>
        get() = driverUsers.map { it.name }.sorted()
    
    // Vehicle names from Firestore only
    val allVehicleNames: List<String>
        get() = vehicles.map { it.displayName }.sorted()
    
    // Expense types from Firestore only
    val allExpenseTypes: List<String>
        get() = expenseTypes.map { it.displayName }.sorted()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val expenseTypeFirestoreService: ExpenseTypeFirestoreService,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val validator: InputValidator,
    private val toastHelper: ToastHelper,
    @ApplicationContext private val context: Context
) : BaseViewModel<AddExpenseUiState>() {
    
    override fun getInitialState() = AddExpenseUiState()
    
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
                vehicleFirestoreService.getVehiclesFlow(),
                expenseTypeFirestoreService.getExpenseTypesFlow()
            ) { driverUsers, vehicles, expenseTypes ->
                Triple(driverUsers, vehicles, expenseTypes)
            }.collect { (driverUsers, vehicles, expenseTypes) ->
                updateState { currentState ->
                    // If this is the first load and we have expense types, select the first one
                    val selectedExpenseType = if (currentState.expenseTypes.isEmpty() && expenseTypes.isNotEmpty()) {
                        // Find the matching ExpenseType enum for the first expense type, or use OTHER as fallback
                        ExpenseType.values().find { it.displayName == expenseTypes.first().displayName } ?: ExpenseType.OTHER
                    } else {
                        currentState.selectedExpenseType
                    }
                    
                    currentState.copy(
                        driverUsers = driverUsers,
                        vehicles = vehicles,
                        expenseTypes = expenseTypes,
                        selectedExpenseType = selectedExpenseType
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
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                type = currentState.selectedExpenseType,
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                date = currentState.date,
                driverName = currentState.driverInput,
                vehicle = currentState.vehicleInput,
                notes = currentState.notes,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val result = saveExpenseUseCase(expense, currentState.photoUri, currentState.photoUris)
            result.fold(
                onSuccess = {
                    toastHelper.showSuccess(context, LocalizedStrings.Success.EXPENSE_ENTRY_ADDED)
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
}