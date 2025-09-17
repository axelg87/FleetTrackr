package com.fleetmanager.ui.viewmodel

import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.preferences.ReportFilterPreferences
import com.fleetmanager.data.preferences.ReportPreferencesDataStore
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.remote.ExpenseTypeFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.ExpenseTypeItem
import com.fleetmanager.domain.usecase.GetActiveDriversUseCase
import com.fleetmanager.domain.usecase.GetActiveVehiclesUseCase
import com.fleetmanager.domain.usecase.GetReportDataRealtimeUseCase
import com.fleetmanager.ui.model.ReportEntry
import com.fleetmanager.ui.model.toReportEntries
import com.fleetmanager.ui.model.toReportEntry
import com.fleetmanager.ui.model.FilterContext
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val driverUsers: List<UserDto> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val expenseTypes: List<ExpenseTypeItem> = emptyList(),
    val availableTypes: List<String> = emptyList(),
    val selectedDriver: String? = null,
    val selectedVehicle: String? = null,
    val selectedType: String? = null,
    val selectedEntryType: EntryTypeFilter = EntryTypeFilter.ALL,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isFilterPanelExpanded: Boolean = false, // Default to collapsed
    val currentUserId: String? = null,
    val isCurrentUserDriver: Boolean = false
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
    private val getReportDataRealtimeUseCase: GetReportDataRealtimeUseCase,
    private val getActiveDriversUseCase: GetActiveDriversUseCase,
    private val getActiveVehiclesUseCase: GetActiveVehiclesUseCase,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val expenseTypeFirestoreService: ExpenseTypeFirestoreService,
    private val authService: AuthService,
    private val reportPreferencesDataStore: ReportPreferencesDataStore
) : BaseViewModel<ReportUiState>() {
    
    override fun getInitialState() = ReportUiState()
    
    // User profile state
    val userProfile: StateFlow<UserDto> = userFirestoreService.getCurrentUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserDto("", "Loading...", "", com.fleetmanager.domain.model.UserRole.DRIVER, null)
        )
    
    init {
        loadReportData()
        loadUserContext()
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
                getReportDataRealtimeUseCase(),
                userFirestoreService.getDriverUsersFlow(),
                vehicleFirestoreService.getVehiclesFlow(),
                expenseTypeFirestoreService.getExpenseTypesFlow()
            ) { reportData, driverUsers, vehicles, expenseTypes ->
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
                
                // Get unique types for filtering (combine from entries and expense types)
                val typesFromEntries = allEntries.map { it.typeDisplayName }.distinct()
                val typesFromExpenseTypes = expenseTypes.map { it.displayName }
                val availableTypes = (typesFromEntries + typesFromExpenseTypes).distinct().sorted()
                
                Pair(allEntries, Triple(driverUsers, vehicles, Pair(expenseTypes, availableTypes)))
            }.collect { (allEntries, data) ->
                val (driverUsers, vehicles, expenseTypesAndTypes) = data
                val (expenseTypes, availableTypes) = expenseTypesAndTypes
                updateState { currentState ->
                    val newState = currentState.copy(
                        allEntries = allEntries,
                        driverUsers = driverUsers,
                        vehicles = vehicles,
                        expenseTypes = expenseTypes,
                        availableTypes = availableTypes,
                        isLoading = false,
                        errorMessage = null
                    )
                    newState.copy(filteredEntries = applySortingAndFilters(newState))
                }
                
                // Load preferences after data is loaded
                loadUserPreferences()
            }
        }
    }
    
    fun updateDriverFilter(driverName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedDriver = driverName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
    }
    
    fun updateVehicleFilter(vehicleDisplayName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedVehicle = vehicleDisplayName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
    }
    
    fun updateTypeFilter(typeName: String?) {
        updateState { currentState ->
            val newState = currentState.copy(selectedType = typeName)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
    }
    
    fun updateEntryTypeFilter(entryType: EntryTypeFilter) {
        updateState { currentState ->
            val newState = currentState.copy(selectedEntryType = entryType)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
    }
    
    fun updateDateRange(startDate: Date?, endDate: Date?) {
        updateState { currentState ->
            val newState = currentState.copy(startDate = startDate, endDate = endDate)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
    }
    
    fun updateSortOption(sortOption: SortOption) {
        updateState { currentState ->
            val newState = currentState.copy(sortOption = sortOption)
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
        saveCurrentPreferences()
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
        saveCurrentPreferences()
    }
    
    fun toggleFilterPanel() {
        updateState { currentState ->
            currentState.copy(isFilterPanelExpanded = !currentState.isFilterPanelExpanded)
        }
    }
    
    /**
     * Apply filter context from dashboard tile navigation.
     * This method sets up the filters based on the provided context and expands the filter panel.
     */
    fun applyFilterContext(filterContext: FilterContext) {
        updateState { currentState ->
            val newState = currentState.copy(
                startDate = filterContext.startDate,
                endDate = filterContext.endDate,
                selectedType = filterContext.sourceFilter?.let { source ->
                    when (source.lowercase()) {
                        "uber" -> "Uber"
                        "yango" -> "Yango"  
                        "private" -> "Private Jobs"
                        else -> null
                    }
                },
                isFilterPanelExpanded = false // Keep collapsed by default
            )
            // Immediately apply filters when context is applied
            newState.copy(filteredEntries = applySortingAndFilters(newState))
        }
    }
    
    /**
     * Refresh filters - re-applies current filter state.
     * This ensures filters are properly applied when screen gains focus.
     */
    fun refreshFilters() {
        updateState { currentState ->
            currentState.copy(filteredEntries = applySortingAndFilters(currentState))
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
    
    private fun loadUserContext() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            authService.currentUser.collect { user ->
                val userId = user?.uid
                updateState { currentState ->
                    currentState.copy(
                        currentUserId = userId,
                        isCurrentUserDriver = userId != null && isUserADriver(userId, currentState.driverUsers)
                    )
                }
            }
        }
    }
    
    private fun loadUserPreferences() {
        val currentState = _uiState.value
        val userId = currentState.currentUserId ?: return
        
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            reportPreferencesDataStore.getFilterPreferences(userId).collect { preferences ->
                updateState { state ->
                    // Auto-fill driver and vehicle for drivers
                    val autoSelectedDriver = if (state.isCurrentUserDriver) {
                        // Find the driver that matches the current user
                        val matchingDriver = state.driverUsers.find { driver ->
                            driver.id == userId
                        }
                        preferences.selectedDriver ?: matchingDriver?.name
                    } else {
                        preferences.selectedDriver
                    }
                    
                    // For drivers, try to auto-select their last used vehicle
                    val autoSelectedVehicle = if (state.isCurrentUserDriver) {
                        preferences.selectedVehicle ?: findDriversLastUsedVehicle(autoSelectedDriver, state.allEntries)
                    } else {
                        preferences.selectedVehicle
                    }
                    
                    val newState = state.copy(
                        selectedDriver = autoSelectedDriver,
                        selectedVehicle = autoSelectedVehicle,
                        selectedType = preferences.selectedType,
                        selectedEntryType = preferences.selectedEntryType,
                        startDate = preferences.startDate,
                        endDate = preferences.endDate,
                        sortOption = preferences.sortOption
                    )
                    newState.copy(filteredEntries = applySortingAndFilters(newState))
                }
            }
        }
    }
    
    private fun findDriversLastUsedVehicle(driverName: String?, entries: List<ReportEntry>): String? {
        if (driverName == null) return null
        
        return entries
            .filter { it.driverName == driverName }
            .maxByOrNull { it.date }
            ?.vehicle
    }
    
    private fun saveCurrentPreferences() {
        val currentState = _uiState.value
        val userId = currentState.currentUserId ?: return
        
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            val preferences = ReportFilterPreferences(
                selectedDriver = currentState.selectedDriver,
                selectedVehicle = currentState.selectedVehicle,
                selectedType = currentState.selectedType,
                selectedEntryType = currentState.selectedEntryType,
                startDate = currentState.startDate,
                endDate = currentState.endDate,
                sortOption = currentState.sortOption
            )
            reportPreferencesDataStore.saveFilterPreferences(userId, preferences)
        }
    }
    
    private fun isUserADriver(userId: String, driverUsers: List<UserDto>): Boolean {
        return driverUsers.any { driver -> 
            driver.id == userId
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