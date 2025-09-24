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
import java.util.concurrent.TimeUnit
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

data class AnalyticsSourceData(
    val entries: List<DailyEntry>,
    val expenses: List<Expense>,
    val drivers: List<Driver>,
    val vehicles: List<Vehicle>,
    val timeFilter: TimeFilter
)

/**
 * ViewModel for Analytics Screen.
 * Manages comprehensive analytics data including trends, comparisons, and projections.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val userFirestoreService: UserFirestoreService
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
                    fleetRepository.getAllDrivers(),
                    fleetRepository.getAllActiveVehicles()
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
                combine(
                    fleetRepository.getAllDailyEntriesRealtime(),
                    fleetRepository.getAllExpensesRealtime(),
                    fleetRepository.getAllDrivers(),
                    fleetRepository.getAllActiveVehicles(),
                    _timeFilter
                ) { entries, expenses, drivers, vehicles, timeFilter ->
                    val driverNameMap = drivers.associateBy({ it.id }, { it.name })
                    val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
                    val enrichedEntries = entries.map { entry ->
                        entry.withResolvedDisplayData(
                            driverDisplayName = driverNameMap[entry.driverId],
                            vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                        )
                    }
                    AnalyticsSourceData(
                        entries = enrichedEntries,
                        expenses = expenses,
                        drivers = drivers,
                        vehicles = vehicles,
                        timeFilter = timeFilter
                    )
                }.collect { sourceData ->

                    // Apply d-1 logic: exclude current day from all calculations
                    val today = LocalDate.now()
                    val entriesExcludingToday = sourceData.entries.filter { entry ->
                        val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
                        entryDate.isBefore(today) // Exclude current day (d-1 logic)
                    }
                    val expensesExcludingToday = sourceData.expenses.filter { expense ->
                        val expenseDate = AnalyticsUtils.dateToLocalDate(expense.date)
                        expenseDate.isBefore(today) // Exclude current day (d-1 logic)
                    }

                    _comprehensiveMetrics.value = calculateComprehensiveMetrics(
                        entries = entriesExcludingToday,
                        expenses = expensesExcludingToday,
                        drivers = sourceData.drivers,
                        vehicles = sourceData.vehicles,
                        targetMonth = _currentMonth.value
                    )

                    // Filter data based on selected time filter
                    val (filteredEntries, filteredExpenses, startDate, endDate) = filterDataByTimeRange(entriesExcludingToday, expensesExcludingToday, sourceData.timeFilter)
                    
                    // If no data available, use mock data for demonstration
                    val analyticsData = if (filteredEntries.isEmpty() && filteredExpenses.isEmpty()) {
                        MockDataProvider.generateMockAnalyticsData()
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
        targetMonth: YearMonth
    ): ComprehensiveAnalyticsMetrics {
        val selectedDriver = drivers.firstOrNull { it.isActive } ?: drivers.firstOrNull()
        val selectedVehicle = vehicles.firstOrNull { it.isActive } ?: vehicles.firstOrNull()

        val entriesForMonth = entries.filter { entry ->
            val entryDate = AnalyticsUtils.dateToLocalDate(entry.date)
            YearMonth.from(entryDate) == targetMonth
        }.filter { entry ->
            val driverMatches = selectedDriver?.id?.let { entry.driverId == it } ?: true
            val vehicleMatches = selectedVehicle?.id?.let { entry.vehicleId == it } ?: true
            driverMatches && vehicleMatches
        }

        val expensesForMonth = expenses.filter { expense ->
            val expenseDate = AnalyticsUtils.dateToLocalDate(expense.date)
            YearMonth.from(expenseDate) == targetMonth
        }.filter { expense ->
            val driverMatches = selectedDriver?.name?.let { expense.driverName.equals(it, ignoreCase = true) } ?: true
            val vehicleMatches = selectedVehicle?.let { vehicle ->
                expense.vehicle.equals(vehicle.displayName, ignoreCase = true) ||
                    expense.vehicle.contains(vehicle.licensePlate, ignoreCase = true)
            } ?: true
            driverMatches && vehicleMatches
        }

        val totalIncome = entriesForMonth.sumOf { it.totalEarnings }

        val driverSalary = selectedDriver?.salary ?: 0.0
        val driverVisaMonthly = (selectedDriver?.annualVisaCost ?: 0.0) / 12.0
        val driverLicenseMonthly = (selectedDriver?.annualLicenseCost ?: 0.0) / 12.0
        val driverFixedCosts = driverSalary + driverVisaMonthly + driverLicenseMonthly

        val vehicleInstallment = selectedVehicle?.installment ?: 0.0
        val vehicleInsuranceMonthly = (selectedVehicle?.annualInsuranceAmount ?: 0.0) / 12.0
        val vehicleFixedCosts = vehicleInstallment + vehicleInsuranceMonthly

        val variableExpenses = expensesForMonth.sumOf { it.amount }

        val totalFixedCosts = driverFixedCosts + vehicleFixedCosts
        val vehicleCostRatio = if (totalIncome > 0) vehicleFixedCosts / totalIncome else 0.0
        val driverNetIncome = totalIncome - driverFixedCosts
        val netOperationalProfit = totalIncome - totalFixedCosts - variableExpenses

        val hasData = totalIncome > 0 || variableExpenses > 0 || totalFixedCosts > 0

        return ComprehensiveAnalyticsMetrics(
            driverNetIncome = driverNetIncome,
            driverFixedCosts = driverFixedCosts,
            vehicleCostRatio = vehicleCostRatio,
            vehicleFixedCosts = vehicleFixedCosts,
            totalIncome = totalIncome,
            variableExpenses = variableExpenses,
            netOperationalProfit = netOperationalProfit,
            driverName = selectedDriver?.name,
            vehicleName = selectedVehicle?.displayName,
            hasData = hasData
        )
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
    val hasData: Boolean = false
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