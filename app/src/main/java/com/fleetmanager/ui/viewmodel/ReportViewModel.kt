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
import java.util.*
import javax.inject.Inject

enum class SortOption(val displayName: String) {
    DATE_DESC("Most Recent"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
    TYPE("By Type")
}

enum class EntryTypeFilter(val displayName: String) {
    ALL("All Entries"),
    INCOME_ONLY("Income Only"),
    EXPENSES_ONLY("Expenses Only")
}

data class GroupedTotal(
    val label: String,
    val amount: Double,
    val count: Int
) {
    val displayAmount: String
        get() = if (amount >= 0) {
            "+$${String.format("%.2f", amount)}"
        } else {
            "-$${String.format("%.2f", kotlin.math.abs(amount))}"
        }
}

data class ReportUiState(
    val allEntries: List<ReportEntry> = emptyList(),
    val filteredEntries: List<ReportEntry> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val availableTypes: List<String> = emptyList(),
    val selectedDriver: String? = null,
    val selectedVehicle: String? = null,
    val selectedType: String? = null,
    val selectedEntryType: EntryTypeFilter = EntryTypeFilter.ALL,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
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
    
    val totalsByDriver: List<GroupedTotal>
        get() = filteredEntries
            .groupBy { it.driverName }
            .map { (driver, entries) ->
                GroupedTotal(
                    label = driver,
                    amount = entries.sumOf { if (it.isIncome) it.amount else -it.amount },
                    count = entries.size
                )
            }
            .sortedByDescending { it.amount }
    
    val totalsByVehicle: List<GroupedTotal>
        get() = filteredEntries
            .groupBy { it.vehicle }
            .map { (vehicle, entries) ->
                GroupedTotal(
                    label = vehicle,
                    amount = entries.sumOf { if (it.isIncome) it.amount else -it.amount },
                    count = entries.size
                )
            }
            .sortedByDescending { it.amount }
    
    val totalsByType: List<GroupedTotal>
        get() = filteredEntries
            .groupBy { it.typeDisplayName }
            .map { (type, entries) ->
                GroupedTotal(
                    label = type,
                    amount = entries.sumOf { if (it.isIncome) it.amount else -it.amount },
                    count = entries.size
                )
            }
            .sortedByDescending { it.amount }
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
                updateState { currentState ->
                    val newState = currentState.copy(
                        allEntries = allEntries,
                        drivers = drivers,
                        vehicles = vehicles,
                        availableTypes = availableTypes,
                        isLoading = false,
                        errorMessage = null
                    )
                    newState.copy(filteredEntries = applySortingAndFilters(newState))
                }
            }
        }
    }
    
    fun updateDriverFilter(driverName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedDriver = driverName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun updateVehicleFilter(vehicleDisplayName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedVehicle = vehicleDisplayName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun updateTypeFilter(typeName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedType = typeName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun updateEntryTypeFilter(entryType: EntryTypeFilter) {
        updateState { currentState ->
            val newState = currentState.copy(selectedEntryType = entryType)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun updateDateRange(startDate: Date?, endDate: Date?) {
        updateState { currentState ->
            val newState = currentState.copy(startDate = startDate, endDate = endDate)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun updateSortOption(sortOption: SortOption) {
        updateState { currentState ->
            val newState = currentState.copy(sortOption = sortOption)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun clearAllFilters() {
        updateState { currentState ->
            val newState = currentState.copy(
                selectedDriver = null,
                selectedVehicle = null,
                selectedType = null,
                selectedEntryType = EntryTypeFilter.ALL,
                startDate = null,
                endDate = null
            )
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    fun exportData(exportAction: suspend () -> Unit) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            exportAction()
        }
    }
    
    private fun applySortingAndFilters(state: ReportUiState): List<ReportEntry> {
        var filtered = state.allEntries
        
        // Apply driver filter
        state.selectedDriver?.let { driver ->
            filtered = filtered.filter { it.driverName == driver }
        }
        
        // Apply vehicle filter
        state.selectedVehicle?.let { selectedVehicleDisplayName ->
            val matchingVehicle = state.vehicles.find { it.displayName == selectedVehicleDisplayName }
            matchingVehicle?.let { vehicle ->
                filtered = filtered.filter { entry ->
                    entry.vehicle == vehicle.licensePlate || 
                    entry.vehicle == vehicle.displayName ||
                    entry.vehicle == "${vehicle.make} ${vehicle.model}" ||
                    entry.vehicle.contains(vehicle.licensePlate, ignoreCase = true)
                }
            }
        }
        
        // Apply type filter
        state.selectedType?.let { type ->
            filtered = filtered.filter { it.typeDisplayName == type }
        }
        
        // Apply entry type filter (income/expense)
        when (state.selectedEntryType) {
            EntryTypeFilter.INCOME_ONLY -> filtered = filtered.filter { it.isIncome }
            EntryTypeFilter.EXPENSES_ONLY -> filtered = filtered.filter { !it.isIncome }
            EntryTypeFilter.ALL -> { /* No additional filtering needed */ }
        }
        
        // Apply date range filter
        state.startDate?.let { startDate ->
            filtered = filtered.filter { it.date >= startDate }
        }
        state.endDate?.let { endDate ->
            // Add one day to endDate to include entries from the end date
            val endOfDay = Calendar.getInstance().apply {
                time = endDate
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            filtered = filtered.filter { it.date < endOfDay }
        }
        
        // Apply sorting
        return when (state.sortOption) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortOption.DATE_ASC -> filtered.sortedBy { it.date }
            SortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortOption.AMOUNT_ASC -> filtered.sortedBy { it.amount }
            SortOption.TYPE -> filtered.sortedBy { it.typeDisplayName }
        }
    }
}