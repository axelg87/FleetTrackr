package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.usecase.GetActiveDriversUseCase
import com.fleetmanager.domain.usecase.GetActiveVehiclesUseCase
import com.fleetmanager.domain.usecase.SaveExpenseUseCase
import com.fleetmanager.domain.usecase.SaveDriverUseCase
import com.fleetmanager.domain.usecase.SaveVehicleUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddExpenseUiState(
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val selectedExpenseType: ExpenseType = ExpenseType.FUEL,
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
        get() = selectedDriver != null && 
                selectedVehicle != null &&
                amount.isNotBlank() &&
                amountError == null &&
                notesError == null &&
                (amount.toDoubleOrNull() ?: 0.0) > 0
    
    val hasValidationErrors: Boolean
        get() = amountError != null || notesError != null
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val getActiveDriversUseCase: GetActiveDriversUseCase,
    private val getActiveVehiclesUseCase: GetActiveVehiclesUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val saveDriverUseCase: SaveDriverUseCase,
    private val saveVehicleUseCase: SaveVehicleUseCase,
    private val validator: InputValidator
) : BaseViewModel<AddExpenseUiState>() {
    
    override fun getInitialState() = AddExpenseUiState()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        executeAsync {
            // Load drivers
            getActiveDriversUseCase()
                .catch { }
                .collect { drivers ->
                    updateState { it.copy(drivers = drivers) }
                    
                    // Auto-add sample drivers if none exist
                    if (drivers.isEmpty()) {
                        addSampleData()
                    }
                }
        }
        
        executeAsync {
            // Load vehicles
            getActiveVehiclesUseCase()
                .catch { }
                .collect { vehicles ->
                    updateState { it.copy(vehicles = vehicles) }
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
        updateState { it.copy(selectedDriver = driver) }
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        updateState { it.copy(selectedVehicle = vehicle) }
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
                driverName = currentState.selectedDriver!!.name,
                vehicle = currentState.selectedVehicle!!.displayName,
                notes = currentState.notes,
                createdAt = Date(),
                updatedAt = Date()
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
}