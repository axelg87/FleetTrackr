package com.fleetmanager.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import com.fleetmanager.ui.screens.analytics.components.*
import com.fleetmanager.ui.screens.analytics.model.AnalyticsData
import com.fleetmanager.ui.screens.analytics.model.AnalyticsPanel
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsAdapters
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

/**
 * Analytics Screen with comprehensive analytics features including trends, 
 * comparisons, ROI analysis, and performance insights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToProfile: (() -> Unit)? = null,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    val selectedPanel by viewModel.selectedPanel.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Screen Header
        com.fleetmanager.ui.components.ScreenHeader(
            title = "Analytics",
            userName = userProfile.name,
            profilePictureUrl = userProfile.profilePictureUrl,
            onProfileClick = onNavigateToProfile?.let { com.fleetmanager.ui.components.rememberProfileClickHandler(it) },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Time Filter
        TimeFilterRow(
            selectedFilter = timeFilter,
            onFilterSelected = { filter -> viewModel.setTimeFilter(filter) },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // GENERALIZATION: Analytics menu for panel selection
        AnalyticsMenu(
            selectedPanel = selectedPanel,
            onPanelSelected = { panel -> viewModel.selectPanel(panel) },
            onShowAll = { viewModel.showAllPanels() },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // GENERALIZATION: Conditional panel rendering based on selection
        selectedPanel?.let { panel ->
            // Show selected panel only
            ShowSelectedPanel(
                panel = panel,
                uiState = uiState,
                analyticsData = analyticsData,
                viewModel = viewModel
            )
        } ?: run {
            // Show all panels (default view)
            ShowAllPanels(
                uiState = uiState,
                analyticsData = analyticsData,
                viewModel = viewModel
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
 * GENERALIZATION: Show all analytics panels in a scrollable view
 */
@Composable
private fun ShowAllPanels(
    uiState: AnalyticsUiState,
    analyticsData: AnalyticsData,
    viewModel: AnalyticsViewModel
) {
    // 1. Trends Over Time - using GenericChart
    GenericChart(
        title = "Trends Over Time",
        subtitle = "Daily income and expense patterns",
        chartType = ChartType.LINE,
        series = AnalyticsAdapters.trendDataToChartSeries(analyticsData.trendData),
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 2. Monthly Comparison
    if (analyticsData.monthlyComparison != null) {
        MonthlyComparisonCard(
            monthlyComparison = analyticsData.monthlyComparison,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
    
    // 3. Projection/Estimation
    if (analyticsData.projection != null) {
        ProjectionEstimation(
            projectionData = analyticsData.projection,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
    
    // 4. Top Drivers - using GenericLeaderboard
    GenericLeaderboard(
        title = "Top Drivers",
        subtitle = "Leading drivers by total revenue",
        data = AnalyticsAdapters.driverPerformanceToLeaderboard(analyticsData.driverPerformance),
        style = LeaderboardStyle.PODIUM,
        maxItems = 5,
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 5. Vehicle ROI - using GenericLeaderboard
    GenericLeaderboard(
        title = "Vehicle ROI",
        subtitle = "Return on investment analysis",
        icon = Icons.Default.DirectionsCar,
        data = AnalyticsAdapters.vehicleROIToLeaderboard(analyticsData.vehicleROI),
        style = LeaderboardStyle.CARDS,
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 6. Day of Week - using GenericChart
    GenericChart(
        title = "Weekly Patterns",
        subtitle = "Average income by day of week",
        chartType = ChartType.BAR_HORIZONTAL,
        data = AnalyticsAdapters.dayOfWeekToChartData(analyticsData.dayOfWeekAnalysis),
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 7. Expense Analysis - using GenericChart
    GenericChart(
        title = "Expense Breakdown",
        subtitle = "Expenses grouped by category",
        chartType = ChartType.PIE,
        data = AnalyticsAdapters.expenseBreakdownToChartData(analyticsData.expenseBreakdown),
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 8. Anomaly Detection
    AnomalyDetection(
        anomalies = analyticsData.anomalies,
        isLoading = analyticsData.isLoading,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // 9. Calendar Overview
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

/**
 * GENERALIZATION: Show individual selected panel with enhanced features
 */
@Composable
private fun ShowSelectedPanel(
    panel: AnalyticsPanel,
    uiState: AnalyticsUiState,
    analyticsData: AnalyticsData,
    viewModel: AnalyticsViewModel
) {
    when (panel) {
        AnalyticsPanel.TRENDS -> {
            GenericChart(
                title = "Trends Over Time",
                subtitle = "Daily income and expense patterns with detailed analysis",
                chartType = ChartType.LINE,
                series = AnalyticsAdapters.trendDataToChartSeries(analyticsData.trendData),
                isLoading = analyticsData.isLoading,
                height = 300.dp,
                customContent = {
                    Spacer(modifier = Modifier.height(16.dp))
                    TrendSummaryCard(analyticsData.trendData)
                }
            )
        }
        
        AnalyticsPanel.DRIVER_PERFORMANCE -> {
            DriverComparison(
                driverPerformance = analyticsData.driverPerformance,
                isLoading = analyticsData.isLoading
            )
        }
        
        AnalyticsPanel.TOP_DRIVERS -> {
            GenericLeaderboard(
                title = "Top Drivers Leaderboard",
                subtitle = "Complete driver performance rankings",
                data = AnalyticsAdapters.driverPerformanceToLeaderboard(analyticsData.driverPerformance),
                style = LeaderboardStyle.PODIUM,
                maxItems = 10,
                isLoading = analyticsData.isLoading,
                summaryContent = {
                    DriverLeaderboardSummary(analyticsData.driverPerformance)
                }
            )
        }
        
        AnalyticsPanel.VEHICLE_ROI -> {
            VehicleROIAnalysis(
                vehicleROI = analyticsData.vehicleROI,
                isLoading = analyticsData.isLoading
            )
        }
        
        AnalyticsPanel.DAY_OF_WEEK -> {
            GenericChart(
                title = "Weekly Patterns Analysis",
                subtitle = "Detailed day-of-week performance breakdown",
                chartType = ChartType.BAR_HORIZONTAL,
                data = AnalyticsAdapters.dayOfWeekToChartData(analyticsData.dayOfWeekAnalysis),
                isLoading = analyticsData.isLoading,
                height = 400.dp,
                customContent = {
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyInsightsCard(analyticsData.dayOfWeekAnalysis)
                }
            )
        }
        
        AnalyticsPanel.EXPENSE_BREAKDOWN -> {
            ExpenseDeepDive(
                expenseBreakdown = analyticsData.expenseBreakdown,
                isLoading = analyticsData.isLoading
            )
        }
        
        AnalyticsPanel.MONTHLY_COMPARISON -> {
            if (analyticsData.monthlyComparison != null) {
                MonthlyComparisonCard(
                    monthlyComparison = analyticsData.monthlyComparison,
                    isLoading = analyticsData.isLoading
                )
            }
        }
        
        AnalyticsPanel.PROJECTION -> {
            if (analyticsData.projection != null) {
                ProjectionEstimation(
                    projectionData = analyticsData.projection,
                    isLoading = analyticsData.isLoading
                )
            }
        }
        
        AnalyticsPanel.ANOMALY_DETECTION -> {
            AnomalyDetection(
                anomalies = analyticsData.anomalies,
                isLoading = analyticsData.isLoading
            )
        }
        
        AnalyticsPanel.CALENDAR_VIEW -> {
            AnalyticsSection(
                title = "Calendar Overview",
                description = "Interactive daily earnings calendar"
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

/**
 * GENERALIZATION: Enhanced summary cards for detailed panel views
 */
@Composable
private fun TrendSummaryCard(trendData: List<com.fleetmanager.ui.screens.analytics.model.TrendData>) {
    if (trendData.isEmpty()) return
    
    val totalIncome = trendData.sumOf { it.income }
    val totalExpenses = trendData.sumOf { it.expenses }
    val totalProfit = totalIncome - totalExpenses
    val averageIncome = totalIncome / trendData.size
    val bestDay = trendData.maxByOrNull { it.income }
    val worstDay = trendData.minByOrNull { it.income }
    
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
                text = "Period Analysis",
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
                        text = "Total Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalIncome),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsUtils.Colors.SUCCESS
                    )
                }
                
                Column {
                    Text(
                        text = "Total Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalExpenses),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsUtils.Colors.ERROR
                    )
                }
                
                Column {
                    Text(
                        text = "Net Profit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalProfit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalProfit >= 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
                    )
                }
            }
            
            if (bestDay != null && worstDay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            text = "${bestDay.date}: ${AnalyticsUtils.formatCurrency(bestDay.income)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AnalyticsUtils.Colors.SUCCESS
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Daily Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AnalyticsUtils.formatCurrency(averageIncome),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AnalyticsUtils.Colors.INFO
                        )
                    }
                }
            }
        }
    }
}

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