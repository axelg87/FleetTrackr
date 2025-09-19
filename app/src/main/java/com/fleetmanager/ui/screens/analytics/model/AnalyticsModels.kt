package com.fleetmanager.ui.screens.analytics.model

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import java.time.LocalDate
import java.time.DayOfWeek

/**
 * Data models specific to analytics features
 */

data class TrendData(
    val date: LocalDate,
    val income: Double,
    val expenses: Double,
    val netProfit: Double
)

data class DriverPerformance(
    val driverName: String,
    val totalRevenue: Double,
    val averageRevenuePerDay: Double,
    val activeDays: Int,
    val totalTrips: Int = 0 // If trip data becomes available
)

data class VehicleROI(
    val vehicleName: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val roi: Double // (netProfit / totalExpenses) * 100
)

data class DayOfWeekAnalysis(
    val dayOfWeek: DayOfWeek,
    val averageIncome: Double,
    val totalDays: Int,
    val totalIncome: Double
)

data class ExpenseBreakdown(
    val expenseType: ExpenseType,
    val totalAmount: Double,
    val percentage: Double,
    val count: Int
)

data class AnomalyData(
    val date: LocalDate,
    val type: AnomalyType,
    val actualValue: Double,
    val expectedValue: Double,
    val deviation: Double,
    val reason: String
)

enum class AnomalyType {
    LOW_INCOME,
    HIGH_EXPENSES,
    ZERO_INCOME,
    UNUSUAL_PATTERN
}

data class MonthlyComparison(
    val currentMonth: String,
    val currentTotal: Double,
    val previousMonth: String,
    val previousTotal: Double,
    val growthPercentage: Double,
    val growthAmount: Double
)

data class ProjectionData(
    val currentMonthTotal: Double,
    val projectedMonthTotal: Double,
    val daysElapsed: Int,
    val totalDaysInMonth: Int,
    val dailyAverage: Double,
    val comparisonToPrevious: Double,
    val activeRevenueDays: Int = 0,
    val activeDayAverage: Double = 0.0
)

/**
 * Aggregated analytics data for the UI
 */
data class AnalyticsData(
    val trendData: List<TrendData> = emptyList(),
    val driverPerformance: List<DriverPerformance> = emptyList(),
    val vehicleROI: List<VehicleROI> = emptyList(),
    val dayOfWeekAnalysis: List<DayOfWeekAnalysis> = emptyList(),
    val expenseBreakdown: List<ExpenseBreakdown> = emptyList(),
    val anomalies: List<AnomalyData> = emptyList(),
    val monthlyComparison: MonthlyComparison? = null,
    val projection: ProjectionData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)