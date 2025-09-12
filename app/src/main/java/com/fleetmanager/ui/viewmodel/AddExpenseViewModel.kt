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
import com.fleetmanager.ui.utils.PhotoUploadDiagnostic
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
    val driverInput: String = "",
    val vehicleInput: String = "",
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
        get() = driverInput.isNotBlank() && 
                vehicleInput.isNotBlank() &&
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
    private val validator: InputValidator,
    private val photoUploadDiagnostic: PhotoUploadDiagnostic
) : BaseViewModel<AddExpenseUiState>() {
    
    override fun getInitialState() = AddExpenseUiState(
        drivers = getSampleDrivers(),
        vehicles = getSampleVehicles()
    )
    
    init {
        loadInitialData()
    }
    
    private fun getSampleDrivers(): List<Driver> {
        return listOf(
            Driver("sample_driver_1", "", "Usman"),
            Driver("sample_driver_2", "", "Ahmed"),
            Driver("sample_driver_3", "", "Rashid")
        )
    }
    
    private fun getSampleVehicles(): List<Vehicle> {
        return listOf(
            Vehicle("sample_vehicle_1", "", "Mitsubishi", "Outlander 1", 2020, "ABC-123"),
            Vehicle("sample_vehicle_2", "", "Mitsubishi", "Outlander 2", 2021, "XYZ-789"),
            Vehicle("sample_vehicle_3", "", "Mitsubishi", "Outlander 3", 2022, "DEF-456")
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