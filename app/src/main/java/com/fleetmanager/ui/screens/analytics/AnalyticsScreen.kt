package com.fleetmanager.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.screens.analytics.components.*

/**
 * Analytics Screen with comprehensive analytics features including trends, 
 * comparisons, ROI analysis, and performance insights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Analytics Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 1. Trends Over Time
        TrendsChart(
            trendData = analyticsData.trendData,
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
        
        // 4. Driver Performance Comparison
        DriverComparison(
            driverPerformance = analyticsData.driverPerformance,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 5. Top Drivers Leaderboard
        TopDriversLeaderboard(
            driverPerformance = analyticsData.driverPerformance,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 6. Vehicle ROI Analysis
        VehicleROIAnalysis(
            vehicleROI = analyticsData.vehicleROI,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 7. Day of Week Analysis
        DayOfWeekChart(
            dayOfWeekAnalysis = analyticsData.dayOfWeekAnalysis,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 8. Expense Deep Dive
        ExpenseDeepDive(
            expenseBreakdown = analyticsData.expenseBreakdown,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 9. Anomaly Detection
        AnomalyDetection(
            anomalies = analyticsData.anomalies,
            isLoading = analyticsData.isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 10. Calendar Overview (moved to bottom as requested)
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