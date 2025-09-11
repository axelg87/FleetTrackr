package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.usecase.GetActiveDriversUseCase
import com.fleetmanager.domain.usecase.GetActiveVehiclesUseCase
import com.fleetmanager.domain.usecase.GetReportDataUseCase
import com.fleetmanager.ui.model.ReportEntry
import com.fleetmanager.ui.model.toReportEntries
import com.fleetmanager.ui.model.toReportEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReportUiState(
    val allEntries: List<ReportEntry> = emptyList(),
    val filteredEntries: List<ReportEntry> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val availableTypes: List<String> = emptyList(),
    val selectedDriver: String? = null,
    val selectedVehicle: String? = null,
    val selectedType: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val totalEntries: Int
        get() = filteredEntries.size
    
    val totalAmount: Double
        get() = filteredEntries.sumOf { if (it.isIncome) it.amount else -it.amount }
    
    val totalAmountDisplay: String
        get() = if (totalAmount >= 0) {
            "+$${String.format("%.2f", totalAmount)}"
        } else {
            "-$${String.format("%.2f", kotlin.math.abs(totalAmount))}"
        }
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getReportDataUseCase: GetReportDataUseCase,
    private val getActiveDriversUseCase: GetActiveDriversUseCase,
    private val getActiveVehiclesUseCase: GetActiveVehiclesUseCase
) : BaseViewModel<ReportUiState>() {
    
    override fun getInitialState() = ReportUiState()
    
    init {
        loadReportData()
    }
    
    private fun loadReportData() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            combine(
                getReportDataUseCase(),
                getActiveDriversUseCase(),
                getActiveVehiclesUseCase()
            ) { reportData, drivers, vehicles ->
                val allEntries = mutableListOf<ReportEntry>()
                
                // Convert daily entries to report entries
                reportData.dailyEntries.forEach { dailyEntry ->
                    allEntries.addAll(dailyEntry.toReportEntries())
                }
                
                // Convert expenses to report entries
                reportData.expenses.forEach { expense ->
                    allEntries.add(expense.toReportEntry())
                }
                
                // Sort by date descending
                allEntries.sortByDescending { it.date }
                
                // Get unique types for filtering
                val availableTypes = allEntries.map { it.typeDisplayName }.distinct().sorted()
                
                Triple(allEntries, drivers, vehicles to availableTypes)
            }.collect { (allEntries, drivers, vehiclesAndTypes) ->
                val (vehicles, availableTypes) = vehiclesAndTypes
                updateState {
                    it.copy(
                        allEntries = allEntries,
                        filteredEntries = allEntries,
                        drivers = drivers,
                        vehicles = vehicles,
                        availableTypes = availableTypes,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }
    
    fun updateDriverFilter(driverName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedDriver = driverName)
            newState.copy(filteredEntries = applyFilters(newState))
        }
    }
    
    fun updateVehicleFilter(vehicleDisplayName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedVehicle = vehicleDisplayName)
            newState.copy(filteredEntries = applyFilters(newState))
        }
    }
    
    fun updateTypeFilter(typeName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedType = typeName)
            newState.copy(filteredEntries = applyFilters(newState))
        }
    }
    
    fun clearAllFilters() {
        updateState { currentState ->
            currentState.copy(
                selectedDriver = null,
                selectedVehicle = null,
                selectedType = null,
                filteredEntries = currentState.allEntries
            )
        }
    }
    
    private fun applyFilters(state: ReportUiState): List<ReportEntry> {
        var filtered = state.allEntries
        
        state.selectedDriver?.let { driver ->
            filtered = filtered.filter { it.driverName == driver }
        }
        
        state.selectedVehicle?.let { selectedVehicleDisplayName ->
            // Find the vehicle that matches the display name and get its actual identifier
            val matchingVehicle = state.vehicles.find { it.displayName == selectedVehicleDisplayName }
            matchingVehicle?.let { vehicle ->
                // The vehicle field in entries might contain different identifiers
                // We need to match against possible vehicle identifiers
                filtered = filtered.filter { entry ->
                    entry.vehicle == vehicle.licensePlate || 
                    entry.vehicle == vehicle.displayName ||
                    entry.vehicle == "${vehicle.make} ${vehicle.model}" ||
                    entry.vehicle.contains(vehicle.licensePlate, ignoreCase = true)
                }
            }
        }
        
        state.selectedType?.let { type ->
            filtered = filtered.filter { it.typeDisplayName == type }
        }
        
        return filtered
    }
}