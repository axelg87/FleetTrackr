package com.fleetmanager.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.ui.screens.analytics.model.AnalyticsData
import com.fleetmanager.ui.screens.analytics.model.AnalyticsPanel
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import com.fleetmanager.ui.screens.analytics.utils.MockDataProvider
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Time filter options for analytics
 */
enum class TimeFilter {
    ALL_TIME,
    LAST_3_MONTHS,
    THIS_MONTH
}

/**
 * Data class to hold filtered analytics data
 */
data class FilteredData(
    val entries: List<DailyEntry>,
    val expenses: List<Expense>,
    val startDate: LocalDate,
    val endDate: LocalDate
)

private data class AnalyticsRealtimeSnapshot(
    val entries: List<DailyEntry> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList()
)

private data class AnalyticsProcessingContext(
    val snapshot: AnalyticsRealtimeSnapshot,
    val timeFilter: TimeFilter,
    val selectedDriverId: String?,
    val costSelection: CostSelection
)

data class DriverFilterOption(
    val id: String?,
    val label: String
) {
    companion object {
        val AllDrivers = DriverFilterOption(null, "All Drivers")
    }
}

data class DriverFilterState(
    val options: List<DriverFilterOption> = listOf(DriverFilterOption.AllDrivers),
    val selectedOption: DriverFilterOption = DriverFilterOption.AllDrivers
)

enum class CostFactor {
    SALARY,
    EXPENSES,
    INSTALLMENTS,
    INSURANCE
}

data class CostSelection(
    val includeSalary: Boolean = true,
    val includeOperationalExpenses: Boolean = true,
    val includeVehicleInstallments: Boolean = true,
    val includeVehicleInsurance: Boolean = true
) {
    fun isEnabled(factor: CostFactor): Boolean = when (factor) {
        CostFactor.SALARY -> includeSalary
        CostFactor.EXPENSES -> includeOperationalExpenses
        CostFactor.INSTALLMENTS -> includeVehicleInstallments
        CostFactor.INSURANCE -> includeVehicleInsurance
    }

    fun withFactor(factor: CostFactor, isEnabled: Boolean): CostSelection = when (factor) {
        CostFactor.SALARY -> copy(includeSalary = isEnabled)
        CostFactor.EXPENSES -> copy(includeOperationalExpenses = isEnabled)
        CostFactor.INSTALLMENTS -> copy(includeVehicleInstallments = isEnabled)
        CostFactor.INSURANCE -> copy(includeVehicleInsurance = isEnabled)
    }

    fun allEnabled(): Boolean = includeSalary && includeOperationalExpenses &&
        includeVehicleInstallments && includeVehicleInsurance
}

/**
 * ViewModel for Analytics Screen.
 * Manages comprehensive analytics data including trends, comparisons, and projections.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val userFirestoreService: UserFirestoreService,
    private val firestoreService: FirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    private val _analyticsData = MutableStateFlow(AnalyticsData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()

    private val _comprehensiveMetrics = MutableStateFlow(ComprehensiveAnalyticsMetrics())
    val comprehensiveMetrics: StateFlow<ComprehensiveAnalyticsMetrics> = _comprehensiveMetrics.asStateFlow()
    
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    private val _selectedPanel = MutableStateFlow<AnalyticsPanel?>(null)
    val selectedPanel: StateFlow<AnalyticsPanel?> = _selectedPanel.asStateFlow()
    
    private val _timeFilter = MutableStateFlow(TimeFilter.LAST_3_MONTHS)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    private val _driverOptions = MutableStateFlow(listOf(DriverFilterOption.AllDrivers))
    private val _selectedDriverId = MutableStateFlow<String?>(null)
    private val _costSelection = MutableStateFlow(CostSelection())
    val costSelection: StateFlow<CostSelection> = _costSelection.asStateFlow()
    val driverFilterState: StateFlow<DriverFilterState> = combine(
        _driverOptions,
        _selectedDriverId
    ) { options, selectedId ->
        val selectedOption = options.firstOrNull { it.id == selectedId } ?: DriverFilterOption.AllDrivers
        DriverFilterState(
            options = options,
            selectedOption = selectedOption
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = DriverFilterState()
    )
    
    // User profile state
    val userProfile: StateFlow<UserDto> = userFirestoreService.getCurrentUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = UserDto("", "Loading...", "", UserRole.DRIVER, null)
        )
    
    init {
        loadEntriesForMonth(_currentMonth.value)
        loadAnalyticsData()
    }
    
    fun loadEntriesForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Calculate start and end dates for the month
                val startDate = yearMonth.atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .let { Date.from(it) }
                
                val endDate = yearMonth.atEndOfMonth()
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .let { Date.from(it) }
                
                combine(
                    fleetRepository.getAllDailyEntriesRealtime(),
                    firestoreService.getDriversFlow(),
                    vehicleFirestoreService.getVehiclesFlow()
                ) { entries, drivers, vehicles ->
                    val driverNameMap = drivers.associateBy({ it.id }, { it.name })
                    val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
                    entries.map { entry ->
                        entry.withResolvedDisplayData(
                            driverDisplayName = driverNameMap[entry.driverId],
                            vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                        )
                    }
                }
                    .collect { allEntries ->
                        // Apply d-1 logic: exclude current day from all calculations
                        val today = LocalDate.now()
                        val filteredEntries = allEntries.filter { entry ->
                            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
                            entryDate.isBefore(today) // Exclude current day (d-1 logic)
                        }
                        
                        // Group ALL entries by date for calendar overview (not just current month)
                        val entriesData = filteredEntries.groupBy { entry ->
                            AnalyticsUtils.dateToLocalDate(entry.date)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            entriesData = entriesData,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load entries"
                )
            }
        }
    }
    
    fun onMonthChanged(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
        loadEntriesForMonth(yearMonth)
        loadAnalyticsData()
    }
    
    /**
     * Load comprehensive analytics data with realtime updates
     */
    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _analyticsData.value = _analyticsData.value.copy(isLoading = true)
            
            try {
                // Use realtime data sources like dashboard
                val realtimeDataFlow = fleetRepository.getAllDailyEntriesRealtime()
                    .combine(fleetRepository.getAllExpensesRealtime()) { entries, expenses ->
                        AnalyticsRealtimeSnapshot(entries = entries, expenses = expenses)
                    }
                    .combine(firestoreService.getDriversFlow()) { snapshot, drivers ->
                        snapshot.copy(drivers = drivers)
                    }
                    .combine(vehicleFirestoreService.getVehiclesFlow()) { snapshot, vehicles ->
                        snapshot.copy(vehicles = vehicles)
                    }

                combine(
                    realtimeDataFlow,
                    _timeFilter,
                    _selectedDriverId,
                    _costSelection
                ) { snapshot, timeFilter, selectedDriverId, costSelection ->
                    AnalyticsProcessingContext(
                        snapshot = snapshot,
                        timeFilter = timeFilter,
                        selectedDriverId = selectedDriverId,
                        costSelection = costSelection
                    )
                }
                    .collect { context ->
                        val snapshot = context.snapshot
                        val timeFilter = context.timeFilter
                        val selectedDriverId = context.selectedDriverId
                        val costSelection = context.costSelection
                        val driverOptions = buildList {
                            add(DriverFilterOption.AllDrivers)
                            addAll(snapshot.drivers.sortedBy { it.name }.map { DriverFilterOption(it.id, it.name) })
                        }
                        if (_driverOptions.value != driverOptions) {
                            _driverOptions.value = driverOptions
                        }

                        val resolvedDriverId = selectedDriverId.takeIf { id ->
                            driverOptions.any { option -> option.id == id }
                        } ?: run {
                            if (selectedDriverId != null) {
                                _selectedDriverId.value = null
                            }
                            null
                        }

                        val driverNameMap = snapshot.drivers.associateBy({ it.id }, { it.name })
                        val driverNameToId = snapshot.drivers.associate { normalizeName(it.name) to it.id }
                        val vehicleNameMap = snapshot.vehicles.associateBy({ it.id }, { it.displayName })
                        val enrichedEntries = snapshot.entries.map { entry ->
                            entry.withResolvedDisplayData(
                                driverDisplayName = driverNameMap[entry.driverId],
                                vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                            )
                        }

                        // Apply d-1 logic: exclude current day from all calculations
                        val today = LocalDate.now()
                        val entriesExcludingToday = enrichedEntries.filter { entry ->
                            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
                            entryDate.isBefore(today) // Exclude current day (d-1 logic)
                        }
                        val expensesExcludingToday = snapshot.expenses.filter { expense ->
                            val expenseDate = AnalyticsUtils.dateToLocalDate(expense.date)
                            expenseDate.isBefore(today) // Exclude current day (d-1 logic)
                        }

                        val entriesByDriver = resolvedDriverId?.let { id ->
                            entriesExcludingToday.filter { entry ->
                                resolveDriverId(entry, driverNameToId) == id
                            }
                        } ?: entriesExcludingToday

                        val expensesByDriver = resolvedDriverId?.let { id ->
                            val normalizedDriverName = driverNameMap[id]?.let { normalizeName(it) }
                            expensesExcludingToday.filter { expense ->
                                when {
                                    expense.driverId.isNotBlank() -> expense.driverId == id
                                    expense.userId.isNotBlank() -> expense.userId == id
                                    normalizedDriverName != null -> normalizeName(expense.driverName) == normalizedDriverName
                                    else -> false
                                }
                            }
                        } ?: expensesExcludingToday

                        _comprehensiveMetrics.value = calculateComprehensiveMetrics(
                            entries = entriesByDriver,
                            expenses = expensesByDriver,
                            drivers = snapshot.drivers,
                            vehicles = snapshot.vehicles,
                            targetMonth = _currentMonth.value,
                            selectedDriverId = resolvedDriverId,
                            costSelection = costSelection
                        )

                        // Filter data based on selected time filter
                        val (filteredEntries, filteredExpenses, startDate, endDate) = filterDataByTimeRange(entriesByDriver, expensesByDriver, timeFilter)

                        // If no data available, use mock data for demonstration
                        val analyticsData = if (filteredEntries.isEmpty() && filteredExpenses.isEmpty()) {
                            if (resolvedDriverId != null) {
                                AnalyticsData()
                            } else {
                                MockDataProvider.generateMockAnalyticsData()
                            }
                        } else {
                            calculateAnalyticsData(filteredEntries, filteredExpenses, startDate, endDate)
                        }

                        _analyticsData.value = analyticsData.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                
            } catch (e: Exception) {
                _analyticsData.value = _analyticsData.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }
    
    /**
     * Filter data based on selected time filter
     */
    private fun filterDataByTimeRange(
        entries: List<DailyEntry>,
        expenses: List<Expense>,
        timeFilter: TimeFilter
    ): FilteredData {
        val currentDate = LocalDate.now()
        val yesterdayDate = currentDate.minusDays(1) // d-1 logic: end date should be yesterday
        val (startDate, endDate) = when (timeFilter) {
            TimeFilter.ALL_TIME -> {
                // For all time, use earliest entry date or 1 year ago as fallback
                val earliestDate = entries.minByOrNull { it.date }?.let { 
                    AnalyticsUtils.dateToLocalDate(it.date) 
                } ?: yesterdayDate.minusYears(1)
                earliestDate to yesterdayDate
            }
            TimeFilter.LAST_3_MONTHS -> {
                yesterdayDate.minusMonths(3) to yesterdayDate
            }
            TimeFilter.THIS_MONTH -> {
                currentDate.withDayOfMonth(1) to yesterdayDate
            }
        }
        
        val startDateAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateAsDate = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        
        val filteredEntries = entries.filter { it.date >= startDateAsDate && it.date <= endDateAsDate }
        val filteredExpenses = expenses.filter { it.date >= startDateAsDate && it.date <= endDateAsDate }
        
        return FilteredData(filteredEntries, filteredExpenses, startDate, endDate)
    }
    
    /**
     * Calculate analytics data from raw entries and expenses
     */
    private fun calculateAnalyticsData(
        entries: List<DailyEntry>,
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): AnalyticsData {
        // Calculate trends
        val trendData = AnalyticsCalculator.calculateTrendData(entries, expenses, startDate, endDate)
        
        // Calculate driver performance
        val driverPerformance = AnalyticsCalculator.calculateDriverPerformance(entries)
        
        // Calculate vehicle ROI
        val vehicleROI = AnalyticsCalculator.calculateVehicleROI(entries, expenses)
        
        // Calculate day of week analysis
        val dayOfWeekAnalysis = AnalyticsCalculator.calculateDayOfWeekAnalysis(entries)
        
        // Calculate expense breakdown
        val expenseBreakdown = AnalyticsCalculator.calculateExpenseBreakdown(expenses)
        
        // Detect anomalies
        val anomalies = AnalyticsCalculator.detectAnomalies(entries, expenses)
        
        // Calculate monthly comparison with smart date handling
        val today = LocalDate.now()
        val yesterday = today.minusDays(1) // d-1 logic
        val currentMonth = today.withDayOfMonth(1)
        val previousMonthYearMonth = YearMonth.from(currentMonth.minusMonths(1))
        
        // Smart date comparison: use same day of month or last day of previous month if day doesn't exist
        val comparisonDayInPreviousMonth = getSmartComparisonDate(yesterday, previousMonthYearMonth)
        
        val currentMonthEntries = entries.filter { entry ->
            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
            entryDate.month == today.month && entryDate.year == today.year && entryDate <= yesterday
        }
        
        val previousMonthEntries = entries.filter { entry ->
            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
            entryDate.month == previousMonthYearMonth.month && entryDate.year == previousMonthYearMonth.year && entryDate <= comparisonDayInPreviousMonth
        }
        
        val monthlyComparison = if (previousMonthEntries.isNotEmpty()) {
            AnalyticsCalculator.calculateMonthlyComparison(
                currentMonthEntries,
                previousMonthEntries,
                AnalyticsUtils.getCurrentMonthName(),
                AnalyticsUtils.getPreviousMonthName()
            )
        } else null
        
        // Calculate projection using d-1 logic
        val projection = if (currentMonthEntries.isNotEmpty()) {
            AnalyticsCalculator.calculateProjection(currentMonthEntries, dayOfWeekAnalysis, yesterday)
        } else null
        
        return AnalyticsData(
            trendData = trendData,
            driverPerformance = driverPerformance,
            vehicleROI = vehicleROI,
            dayOfWeekAnalysis = dayOfWeekAnalysis,
            expenseBreakdown = expenseBreakdown,
            anomalies = anomalies,
            monthlyComparison = monthlyComparison,
            projection = projection
        )
    }

    private fun calculateComprehensiveMetrics(
        entries: List<DailyEntry>,
        expenses: List<Expense>,
        drivers: List<Driver>,
        vehicles: List<Vehicle>,
        targetMonth: YearMonth,
        selectedDriverId: String?,
        costSelection: CostSelection
    ): ComprehensiveAnalyticsMetrics {
        val scopedDrivers = if (selectedDriverId != null) {
            drivers.filter { it.id == selectedDriverId }
        } else {
            drivers.filter { it.isActive }.ifEmpty { drivers }
        }

        val driverIdsInScope = scopedDrivers.map { it.id }.toSet()

        val entriesForMonth = entries.filter { entry ->
            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
            YearMonth.from(entryDate) == targetMonth &&
                (driverIdsInScope.isEmpty() || driverIdsInScope.contains(entry.driverId))
        }

        val expensesForMonth = expenses.filter { expense ->
            val expenseDate = AnalyticsUtils.dateToLocalDate(expense.date)
            val expenseDriverId = expense.driverId.takeIf { it.isNotBlank() } ?: expense.userId
            YearMonth.from(expenseDate) == targetMonth &&
                (driverIdsInScope.isEmpty() || driverIdsInScope.contains(expenseDriverId))
        }

        val vehiclesById = vehicles.associateBy { it.id }
        val vehiclesByDisplayName = vehicles.associateBy { normalizeName(it.displayName) }
        val driverNameToId = drivers.associate { driver -> normalizeName(driver.name) to driver.id }
        val vehicleCostsByDriver = calculateVehicleCostAssignments(
            entries,
            vehiclesById,
            vehiclesByDisplayName,
            targetMonth,
            driverNameToId
        )
        val relevantDriverIds = if (driverIdsInScope.isEmpty()) {
            vehicleCostsByDriver.keys
        } else {
            driverIdsInScope
        }
        val assignedVehicleCosts = relevantDriverIds.fold(VehicleMonthlyCostBreakdown()) { acc, driverId ->
            acc + (vehicleCostsByDriver[driverId] ?: VehicleMonthlyCostBreakdown())
        }
        val fallbackVehicleCosts = vehicles
            .filter { it.isActive }
            .ifEmpty { vehicles }
            .fold(VehicleMonthlyCostBreakdown()) { acc, vehicle -> acc + vehicleMonthlyCost(vehicle) }
        val aggregatedVehicleCosts = when {
            selectedDriverId != null -> assignedVehicleCosts
            assignedVehicleCosts.total > 0.0 -> assignedVehicleCosts
            else -> fallbackVehicleCosts
        }
        val driverSpecificVehicleCosts = selectedDriverId?.let { driverId ->
            vehicleCostsByDriver[driverId] ?: VehicleMonthlyCostBreakdown()
        } ?: aggregatedVehicleCosts

        val totalIncome = entriesForMonth.sumOf { it.totalEarnings }

        val driverFixedCostsBase = scopedDrivers.sumOf { driver ->
            driver.salary + (driver.annualVisaCost / 12.0) + (driver.annualLicenseCost / 12.0)
        }
        val selectedDriverFixedCosts = if (costSelection.includeSalary) driverFixedCostsBase else 0.0

        val selectedVehicleInstallments = if (costSelection.includeVehicleInstallments) {
            aggregatedVehicleCosts.installment
        } else {
            0.0
        }
        val selectedVehicleInsurance = if (costSelection.includeVehicleInsurance) {
            aggregatedVehicleCosts.insurance
        } else {
            0.0
        }
        val selectedVehicleFixedCosts = selectedVehicleInstallments + selectedVehicleInsurance

        val variableExpensesBase = expensesForMonth.sumOf { it.amount }
        val selectedVariableExpenses = if (costSelection.includeOperationalExpenses) {
            variableExpensesBase
        } else {
            0.0
        }

        val totalExpenses = selectedDriverFixedCosts + selectedVehicleFixedCosts + selectedVariableExpenses
        val vehicleCostRatio = if (totalIncome > 0 && selectedVehicleFixedCosts > 0.0) {
            selectedVehicleFixedCosts / totalIncome
        } else {
            0.0
        }
        val netOperationalProfit = totalIncome - totalExpenses

        val hasData = entriesForMonth.isNotEmpty() || expensesForMonth.isNotEmpty()

        val singleDriverName = scopedDrivers.singleOrNull()?.name
        val singleVehicleName = if (selectedDriverId != null) {
            entriesForMonth
                .groupBy { it.vehicleId }
                .maxByOrNull { (_, driverEntries) -> driverEntries.size }?.key
                ?.let { vehicleId -> vehiclesById[vehicleId]?.displayName }
        } else {
            vehicles.filter { it.isActive }.singleOrNull()?.displayName
        }

        return ComprehensiveAnalyticsMetrics(
            driverNetIncome = netOperationalProfit,
            driverFixedCosts = selectedDriverFixedCosts,
            vehicleCostRatio = vehicleCostRatio,
            vehicleFixedCosts = selectedVehicleFixedCosts,
            totalIncome = totalIncome,
            variableExpenses = selectedVariableExpenses,
            netOperationalProfit = netOperationalProfit,
            driverName = singleDriverName,
            vehicleName = singleVehicleName,
            hasData = hasData,
            totalExpenses = totalExpenses,
            driverVehicleCost = driverSpecificVehicleCosts.selectedTotal(costSelection)
        )
    }

    private fun calculateVehicleCostAssignments(
        entries: List<DailyEntry>,
        vehiclesById: Map<String, Vehicle>,
        vehiclesByDisplayName: Map<String, Vehicle>,
        targetMonth: YearMonth,
        driverNameToId: Map<String, String>
    ): Map<String, VehicleMonthlyCostBreakdown> {
        if (entries.isEmpty()) return emptyMap()

        val monthEndInstant = targetMonth.atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        return entries
            .groupBy { entry -> resolveDriverId(entry, driverNameToId) }
            .mapNotNull { (resolvedDriverId, driverEntries) ->
                val driverId = resolvedDriverId ?: return@mapNotNull null
                val assignmentEntry = driverEntries
                    .filter { entry -> entry.date.toInstant() <= monthEndInstant }
                    .maxByOrNull { entry -> entry.date.time }
                    ?: return@mapNotNull null

                val vehicle = resolveVehicle(assignmentEntry, vehiclesById, vehiclesByDisplayName)
                    ?: return@mapNotNull null
                driverId to vehicleMonthlyCost(vehicle)
            }
            .toMap()
    }

    private fun vehicleMonthlyCost(vehicle: Vehicle): VehicleMonthlyCostBreakdown {
        return VehicleMonthlyCostBreakdown(
            installment = vehicle.installment ?: 0.0,
            insurance = vehicle.annualInsuranceAmount / 12.0
        )
    }

    private data class VehicleMonthlyCostBreakdown(
        val installment: Double = 0.0,
        val insurance: Double = 0.0
    ) {
        val total: Double get() = installment + insurance

        fun selectedTotal(selection: CostSelection): Double {
            val selectedInstallments = if (selection.includeVehicleInstallments) installment else 0.0
            val selectedInsurance = if (selection.includeVehicleInsurance) insurance else 0.0
            return selectedInstallments + selectedInsurance
        }

        operator fun plus(other: VehicleMonthlyCostBreakdown): VehicleMonthlyCostBreakdown {
            return VehicleMonthlyCostBreakdown(
                installment = installment + other.installment,
                insurance = insurance + other.insurance
            )
        }
    }

    private fun resolveDriverId(entry: DailyEntry, driverNameToId: Map<String, String>): String? {
        val explicitId = entry.driverId.takeIf { it.isNotBlank() }
        if (explicitId != null) {
            return explicitId
        }

        val normalizedName = entry.driverName.takeIf { it.isNotBlank() }?.let { normalizeName(it) }
        return normalizedName?.let { driverNameToId[it] }
    }

    private fun resolveVehicle(
        entry: DailyEntry,
        vehiclesById: Map<String, Vehicle>,
        vehiclesByDisplayName: Map<String, Vehicle>
    ): Vehicle? {
        val vehicleById = entry.vehicleId.takeIf { it.isNotBlank() }?.let { vehiclesById[it] }
        if (vehicleById != null) {
            return vehicleById
        }

        val normalizedDisplayName = entry.vehicle.takeIf { it.isNotBlank() }?.let { normalizeName(it) }
        return normalizedDisplayName?.let { vehiclesByDisplayName[it] }
    }

    private fun normalizeName(raw: String): String {
        return raw.trim().lowercase(Locale.getDefault())
    }
    
    fun onDaySelected(date: LocalDate, entries: List<DailyEntry>) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedDayEntries = entries
        )
    }
    
    fun clearDaySelection() {
        _uiState.value = _uiState.value.copy(
            selectedDate = null,
            selectedDayEntries = null
        )
    }
    
    /**
     * GENERALIZATION: Panel selection for menu navigation
     */
    fun selectPanel(panel: AnalyticsPanel) {
        _selectedPanel.value = panel
    }
    
    fun showAllPanels() {
        _selectedPanel.value = null
    }
    
    fun isShowingAllPanels(): Boolean {
        return _selectedPanel.value == null
    }
    
    /**
     * Change time filter for analytics data
     */
    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun setDriverFilter(option: DriverFilterOption) {
        _selectedDriverId.value = option.id
    }

    fun onCostFactorToggled(factor: CostFactor, isEnabled: Boolean) {
        _costSelection.value = _costSelection.value.withFactor(factor, isEnabled)
    }

    /**
     * Calculate the income level for a given day based on total earnings.
     * This determines the color coding for calendar days.
     */
    fun getIncomeLevel(entries: List<DailyEntry>): IncomeLevel {
        val totalIncome = entries.sumOf { it.totalEarnings }
        
        return when {
            totalIncome >= HIGH_INCOME_THRESHOLD -> IncomeLevel.HIGH
            totalIncome >= MEDIUM_INCOME_THRESHOLD -> IncomeLevel.MEDIUM
            totalIncome > 0 -> IncomeLevel.LOW
            else -> IncomeLevel.NONE
        }
    }
    
    /**
     * Smart date comparison for monthly analysis.
     * Handles different month lengths intelligently (e.g., comparing Feb 28 to Jan 31)
     */
    private fun getSmartComparisonDate(currentDate: LocalDate, targetMonth: YearMonth): LocalDate {
        val dayOfMonth = currentDate.dayOfMonth
        val maxDayInTargetMonth = targetMonth.lengthOfMonth()
        
        return if (dayOfMonth <= maxDayInTargetMonth) {
            // Same day exists in target month
            targetMonth.atDay(dayOfMonth)
        } else {
            // Day doesn't exist in target month (e.g., Jan 31 -> Feb 28/29)
            targetMonth.atEndOfMonth()
        }
    }
    
    companion object {
        // Configurable thresholds - can be moved to Settings later
        const val HIGH_INCOME_THRESHOLD = 250.0 // AED
        const val MEDIUM_INCOME_THRESHOLD = 100.0 // AED
    }
}

/**
 * UI State for Analytics Screen
 */
data class AnalyticsUiState(
    val entriesData: Map<LocalDate, List<DailyEntry>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate? = null,
    val selectedDayEntries: List<DailyEntry>? = null
)

data class ComprehensiveAnalyticsMetrics(
    val driverNetIncome: Double = 0.0,
    val driverFixedCosts: Double = 0.0,
    val vehicleCostRatio: Double = 0.0,
    val vehicleFixedCosts: Double = 0.0,
    val totalIncome: Double = 0.0,
    val variableExpenses: Double = 0.0,
    val netOperationalProfit: Double = 0.0,
    val driverName: String? = null,
    val vehicleName: String? = null,
    val hasData: Boolean = false,
    val totalExpenses: Double = 0.0,
    val driverVehicleCost: Double = 0.0
)

/**
 * Income levels for color coding calendar days
 */
enum class IncomeLevel {
    NONE,    // No income or negative
    LOW,     // Below medium threshold
    MEDIUM,  // Between medium and high threshold  
    HIGH     // Above high threshold
}