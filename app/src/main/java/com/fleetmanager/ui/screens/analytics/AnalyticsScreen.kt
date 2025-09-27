package com.fleetmanager.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.util.Locale
import com.fleetmanager.ui.screens.analytics.ComprehensiveAnalyticsMetrics
import com.fleetmanager.ui.screens.analytics.components.*
import com.fleetmanager.ui.screens.analytics.model.AnalyticsCategory
import com.fleetmanager.ui.screens.analytics.model.AnalyticsData
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsAdapters
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

enum class AnalyticsScopeFilter {
    INCOME_ONLY,
    ALL
}

/**
 * Analytics Screen with comprehensive analytics features including trends,
 * comparisons, ROI analysis, and performance insights.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Material 3 scaffold & FlowRow are still experimental.
@Composable
fun AnalyticsScreen(
    onNavigateToProfile: (() -> Unit)? = null,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val comprehensiveMetrics by viewModel.comprehensiveMetrics.collectAsState()
    val driverFilterState by viewModel.driverFilterState.collectAsState()
    val costSelection by viewModel.costSelection.collectAsState()

    var selectedCategory by rememberSaveable { mutableStateOf(AnalyticsCategory.PERFORMANCE) }
    var selectedScope by rememberSaveable { mutableStateOf(AnalyticsScopeFilter.INCOME_ONLY) }

    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            AnalyticsCategoryNavigationBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Screen Header
            com.fleetmanager.ui.components.ScreenHeader(
                title = "Analytics",
                userName = userProfile.name,
                profilePictureUrl = userProfile.profilePictureUrl,
                onProfileClick = onNavigateToProfile?.let { com.fleetmanager.ui.components.rememberProfileClickHandler(it) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DriverFilterRow(
                state = driverFilterState,
                onDriverSelected = { option -> viewModel.setDriverFilter(option) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Time Filter
            TimeFilterRow(
                selectedFilter = timeFilter,
                onFilterSelected = { filter -> viewModel.setTimeFilter(filter) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnalyticsScopeRow(
                selectedScope = selectedScope,
                onScopeSelected = { selectedScope = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show panels within the selected category for quicker access
            ShowCategoryPanels(
                category = selectedCategory,
                uiState = uiState,
                analyticsData = analyticsData,
                viewModel = viewModel,
                scopeFilter = selectedScope,
                comprehensiveMetrics = comprehensiveMetrics,
                driverFilterState = driverFilterState,
                costSelection = costSelection,
                onCostSelectionChanged = viewModel::onCostFactorToggled
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

    }

    // Handle day selection dialog
    if (uiState.selectedDayEntries != null) {
        DayEntriesDialog(
            date = uiState.selectedDate,
            entries = uiState.selectedDayEntries,
            onDismiss = { viewModel.clearDaySelection() }
        )
    }
}

/**
 * GENERALIZATION: Show analytics panels for the selected category
 */
@Composable
private fun ShowCategoryPanels(
    category: AnalyticsCategory,
    uiState: AnalyticsUiState,
    analyticsData: AnalyticsData,
    viewModel: AnalyticsViewModel,
    scopeFilter: AnalyticsScopeFilter,
    comprehensiveMetrics: ComprehensiveAnalyticsMetrics,
    driverFilterState: DriverFilterState,
    costSelection: CostSelection,
    onCostSelectionChanged: (CostFactor, Boolean) -> Unit
) {
    when (category) {
        AnalyticsCategory.PERFORMANCE -> {
            if (scopeFilter == AnalyticsScopeFilter.INCOME_ONLY) {
                MonthlyGoalPlanner(
                    projectionData = analyticsData.projection,
                    isLoading = analyticsData.isLoading,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (analyticsData.monthlyComparison != null) {
                    MonthlyComparisonCard(
                        monthlyComparison = analyticsData.monthlyComparison,
                        isLoading = analyticsData.isLoading,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (analyticsData.projection != null) {
                    ProjectionEstimation(
                        projectionData = analyticsData.projection,
                        isLoading = analyticsData.isLoading,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            } else {
                AllAnalyticsTiles(
                    metrics = comprehensiveMetrics,
                    driverFilterOption = driverFilterState.selectedOption,
                    costSelection = costSelection,
                    onCostSelectionChanged = onCostSelectionChanged,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        AnalyticsCategory.DRIVERS -> {
            GenericLeaderboard(
                title = "Top Drivers",
                subtitle = "Leading drivers by total revenue",
                data = AnalyticsAdapters.driverPerformanceToLeaderboard(analyticsData.driverPerformance),
                style = LeaderboardStyle.PODIUM,
                maxItems = 5,
                isLoading = analyticsData.isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DriverComparison(
                driverPerformance = analyticsData.driverPerformance,
                isLoading = analyticsData.isLoading
            )
        }

        AnalyticsCategory.VEHICLES -> {
            GenericLeaderboard(
                title = "Vehicle ROI",
                subtitle = "Return on investment analysis",
                icon = Icons.Default.DirectionsCar,
                data = AnalyticsAdapters.vehicleROIToLeaderboard(analyticsData.vehicleROI),
                style = LeaderboardStyle.CARDS,
                isLoading = analyticsData.isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            VehicleROIAnalysis(
                vehicleROI = analyticsData.vehicleROI,
                isLoading = analyticsData.isLoading
            )
        }

        AnalyticsCategory.PATTERNS -> {
            GenericChart(
                title = "Weekly Patterns",
                subtitle = "Average income by day of week",
                chartType = ChartType.BAR_HORIZONTAL,
                data = AnalyticsAdapters.dayOfWeekToChartData(analyticsData.dayOfWeekAnalysis),
                isLoading = analyticsData.isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GenericChart(
                title = "Expense Breakdown",
                subtitle = "Expenses grouped by category",
                chartType = ChartType.PIE,
                data = AnalyticsAdapters.expenseBreakdownToChartData(analyticsData.expenseBreakdown),
                isLoading = analyticsData.isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        AnalyticsCategory.INSIGHTS -> {
            AnomalyDetection(
                anomalies = analyticsData.anomalies,
                isLoading = analyticsData.isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnalyticsSection(
                title = "Calendar Overview",
                description = "Daily earnings visualization"
            ) {
                CalendarView(
                    entriesData = uiState.entriesData,
                    isLoading = uiState.isLoading,
                    onDayClick = { date, entries ->
                        viewModel.onDaySelected(date, entries)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsScopeRow(
    selectedScope: AnalyticsScopeFilter,
    onScopeSelected: (AnalyticsScopeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Analytics Focus",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsScopeFilter.values().forEach { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { onScopeSelected(scope) },
                        label = {
                            Text(
                                text = when (scope) {
                                    AnalyticsScopeFilter.INCOME_ONLY -> "Income Only"
                                    AnalyticsScopeFilter.ALL -> "All"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverFilterRow(
    state: DriverFilterState,
    onDriverSelected: (DriverFilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMultipleDrivers = state.options.size > 1
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && hasMultipleDrivers,
        onExpandedChange = {
            if (hasMultipleDrivers) {
                expanded = !expanded
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = state.selectedOption.label,
            onValueChange = {},
            readOnly = true,
            enabled = hasMultipleDrivers,
            label = { Text("Driver") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && hasMultipleDrivers)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded && hasMultipleDrivers,
            onDismissRequest = { expanded = false }
        ) {
            state.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onDriverSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun AllAnalyticsTiles(
    metrics: ComprehensiveAnalyticsMetrics,
    driverFilterOption: DriverFilterOption,
    costSelection: CostSelection,
    onCostSelectionChanged: (CostFactor, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Comprehensive Financial Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CostSelectionControls(
            selection = costSelection,
            onCostSelectionChanged = onCostSelectionChanged,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!metrics.hasData) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No financial data available for the selected month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        val hasIncome = metrics.totalIncome > 0
        val hasVehicleCostValue = metrics.vehicleFixedCosts > 0
        val vehicleCostsEnabled = costSelection.includeVehicleInstallments || costSelection.includeVehicleInsurance
        val ratioColor = when {
            !hasIncome -> MaterialTheme.colorScheme.onSurface
            metrics.vehicleCostRatio < 0.4 -> AnalyticsUtils.Colors.SUCCESS
            metrics.vehicleCostRatio < 0.6 -> AnalyticsUtils.Colors.WARNING
            else -> AnalyticsUtils.Colors.ERROR
        }
        val ratioText = if (hasIncome) {
            String.format(Locale.US, "%.2f", metrics.vehicleCostRatio)
        } else {
            "N/A"
        }
        val ratioSubtitle = when {
            !hasIncome -> "Vehicle costs unavailable due to no income"
            hasVehicleCostValue -> "AED $ratioText in selected vehicle costs per AED 1 earned"
            vehicleCostsEnabled -> "No vehicle costs recorded for selected factors"
            else -> "No vehicle cost factors selected"
        }
        val ratioValue = when {
            !hasIncome -> ratioText
            hasVehicleCostValue -> "AED $ratioText"
            vehicleCostsEnabled -> "AED 0.00"
            else -> "AED 0.00"
        }
        val ratioValueColor = when {
            !hasIncome -> MaterialTheme.colorScheme.onSurface
            hasVehicleCostValue -> ratioColor
            else -> MaterialTheme.colorScheme.onSurface
        }
        val ratioBackground = when {
            !hasIncome || !hasVehicleCostValue -> MaterialTheme.colorScheme.surfaceVariant
            else -> ratioColor.copy(alpha = 0.12f)
        }
        val netIncomeSubtitle = if (costSelection.allEnabled()) {
            "after fixed and variable costs"
        } else {
            "after selected cost factors"
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsSummaryTile(
                title = "Driver Net Income",
                subtitle = netIncomeSubtitle,
                value = AnalyticsUtils.formatCurrency(metrics.driverNetIncome),
                badgeLabel = if (driverFilterOption.id == null) "ALL" else "DRIVER",
                valueColor = if (metrics.driverNetIncome >= 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
            ) {
                metrics.driverName?.let { name ->
                    Text(
                        text = "Driver: $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (driverFilterOption.id != null) {
                    Text(
                        text = "Selected vehicle cost: ${AnalyticsUtils.formatCurrency(metrics.driverVehicleCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Selected fixed costs: ${AnalyticsUtils.formatCurrency(metrics.driverFixedCosts)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Selected expenses: ${AnalyticsUtils.formatCurrency(metrics.totalExpenses)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!costSelection.allEnabled()) {
                    Text(
                        text = "Cost filters applied",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnalyticsSummaryTile(
                title = "Vehicle Cost Efficiency",
                subtitle = ratioSubtitle,
                value = ratioValue,
                badgeLabel = if (driverFilterOption.id == null) "ALL" else "DRIVER",
                containerColor = ratioBackground,
                valueColor = ratioValueColor
            ) {
                metrics.vehicleName?.let { vehicle ->
                    Text(
                        text = vehicle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Selected vehicle costs: ${AnalyticsUtils.formatCurrency(metrics.vehicleFixedCosts)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val totalFixedCosts = metrics.driverFixedCosts + metrics.vehicleFixedCosts
            val breakdownSubtitle = buildString {
                append("Income ${AnalyticsUtils.formatCurrency(metrics.totalIncome)} • ")
                append("Variable ${AnalyticsUtils.formatCurrency(metrics.variableExpenses)} • ")
                append("Fixed ${AnalyticsUtils.formatCurrency(totalFixedCosts)}")
                if (!costSelection.allEnabled()) {
                    append(" • Filters applied")
                }
            }
            AnalyticsSummaryTile(
                title = "Net Operational Profit",
                subtitle = breakdownSubtitle,
                value = AnalyticsUtils.formatCurrency(metrics.netOperationalProfit),
                badgeLabel = if (driverFilterOption.id == null) "ALL" else "DRIVER",
                valueColor = if (metrics.netOperationalProfit >= 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
            )
        }
    }
}

private data class CostSelectionOption(
    val factor: CostFactor,
    val label: String,
    val description: String
)

@Composable
@OptIn(ExperimentalLayoutApi::class) // FlowRow support is still experimental in Compose.
private fun CostSelectionControls(
    selection: CostSelection,
    onCostSelectionChanged: (CostFactor, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cost Factors",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose which costs are included in the overview tiles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val options = remember {
                listOf(
                    CostSelectionOption(
                        factor = CostFactor.SALARY,
                        label = "Salary",
                        description = "Monthly driver salary"
                    ),
                    CostSelectionOption(
                        factor = CostFactor.VISA_LICENSE_FEES,
                        label = "Visa & license",
                        description = "Recurring visa and license fees"
                    ),
                    CostSelectionOption(
                        factor = CostFactor.EXPENSES,
                        label = "Operational expenses",
                        description = "Daily operational expenses and reimbursements"
                    ),
                    CostSelectionOption(
                        factor = CostFactor.INSTALLMENTS,
                        label = "Vehicle installments",
                        description = "Monthly loan or lease payments"
                    ),
                    CostSelectionOption(
                        factor = CostFactor.INSURANCE,
                        label = "Vehicle insurance",
                        description = "Prorated annual insurance premiums"
                    )
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEach { option ->
                    CostSelectionOptionChip(
                        option = option,
                        selected = selection.isEnabled(option.factor),
                        onSelectionChanged = { isSelected ->
                            onCostSelectionChanged(option.factor, isSelected)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // FilterChip is still experimental in Material 3.
@Composable
private fun CostSelectionOptionChip(
    option: CostSelectionOption,
    selected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 220.dp)
    ) {
        FilterChip(
            selected = selected,
            onClick = { onSelectionChanged(!selected) },
            label = {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = if (selected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                }
            } else {
                null
            }
        )
        Text(
            text = option.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AnalyticsSummaryTile(
    title: String,
    subtitle: String,
    value: String,
    badgeLabel: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )

                content?.invoke()
            }

            badgeLabel?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * GENERALIZATION: Show individual selected panel with enhanced features
 */
@Composable
private fun DriverLeaderboardSummary(driverPerformance: List<com.fleetmanager.ui.screens.analytics.model.DriverPerformance>) {
    if (driverPerformance.isEmpty()) return
    
    val totalRevenue = driverPerformance.sumOf { it.totalRevenue }
    val averageRevenue = totalRevenue / driverPerformance.size
    val topPerformer = driverPerformance.maxByOrNull { it.totalRevenue }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Team Performance Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Team Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        text = "Team Average",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(averageRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (topPerformer != null) {
                    Column {
                        Text(
                            text = "Champion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topPerformer.driverName.split(" ").firstOrNull() ?: topPerformer.driverName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.GOLD
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyInsightsCard(dayOfWeekAnalysis: List<com.fleetmanager.ui.screens.analytics.model.DayOfWeekAnalysis>) {
    if (dayOfWeekAnalysis.isEmpty()) return
    
    val bestDay = dayOfWeekAnalysis.maxByOrNull { it.averageIncome }
    val worstDay = dayOfWeekAnalysis.minByOrNull { it.averageIncome }
    val weekendAverage = dayOfWeekAnalysis.filter { 
        it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY 
    }.takeIf { it.isNotEmpty() }?.let { weekend ->
        weekend.sumOf { it.averageIncome } / weekend.size
    } ?: 0.0
    
    val weekdayAverage = dayOfWeekAnalysis.filter { 
        it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) 
    }.takeIf { it.isNotEmpty() }?.let { weekdays ->
        weekdays.sumOf { it.averageIncome } / weekdays.size
    } ?: 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Weekly Performance Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (bestDay != null && worstDay != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Best Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AnalyticsUtils.getDayDisplayName(bestDay.dayOfWeek),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.SUCCESS
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Weekend vs Weekday",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val comparison = if (weekdayAverage > 0) {
                            ((weekendAverage - weekdayAverage) / weekdayAverage) * 100
                        } else 0.0
                        Text(
                            text = "${if (comparison > 0) "+" else ""}${AnalyticsUtils.formatDecimal(comparison)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison > 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable component for analytics sections.
 * This provides consistent styling and makes it easy to add new sections.
 */
@Composable
private fun AnalyticsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            content()
        }
    }
}

/**
 * Time filter row component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFilterRow(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Time Filter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { 
                            Text(
                                text = when (filter) {
                                    TimeFilter.ALL_TIME -> "All Time"
                                    TimeFilter.LAST_3_MONTHS -> "Last 3 Months"
                                    TimeFilter.THIS_MONTH -> "This Month"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}