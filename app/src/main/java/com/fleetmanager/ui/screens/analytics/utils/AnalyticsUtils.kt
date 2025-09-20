package com.fleetmanager.ui.screens.analytics.utils

import androidx.compose.ui.graphics.Color
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.ui.screens.analytics.model.AnomalyType
import com.fleetmanager.ui.theme.AccentNavy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

/**
 * Centralized utilities for Analytics components to eliminate duplication.
 * Contains shared color logic, formatting helpers, date utilities, and chart configurations.
 */
object AnalyticsUtils {

    // ==================== COLOR UTILITIES ====================
    
    /**
     * REFACTOR: Extracted from multiple components - standardized color thresholds
     * Used in: TrendsChart, DriverComparison, VehicleROI, MonthlyComparison
     */
    object Colors {
        // Primary colors used across analytics
        val SUCCESS = Color(0xFF4CAF50) // Green - positive performance
        val WARNING = Color(0xFFFF9800) // Orange - moderate/caution
        val ERROR = Color(0xFFF44336) // Red - negative/concerning
        val INFO = Color(0xFF2196F3) // Blue - neutral information
        val NEUTRAL = Color(0xFF607D8B) // Gray - no change/neutral
        
        // Ranking colors for leaderboards
        val GOLD = Color(0xFFFFD700)
        val SILVER = Color(0xFFC0C0C0)
        val BRONZE = Color(0xFFCD7F32)
        
        // Anomaly severity colors
        val CRITICAL = Color(0xFFD32F2F)
        val HIGH = Color(0xFFFF5722)
        val MEDIUM = Color(0xFFFF9800)
        val LOW = Color(0xFFFFC107)
        val UNKNOWN = Color(0xFF9E9E9E)
    }
    
    /**
     * REFACTOR: Centralized from CalendarView.kt and multiple components
     * Get color for income level based on thresholds
     */
    fun getIncomeColor(amount: Double): Color {
        return when {
            amount >= 250.0 -> Colors.SUCCESS // High income
            amount >= 100.0 -> Colors.WARNING // Medium income
            amount > 0 -> Colors.ERROR // Low income
            else -> Color.Transparent // No income
        }
    }
    
    /**
     * REFACTOR: Extracted from VehicleROI.kt
     * Get color for ROI performance
     */
    fun getROIColor(roi: Double): Color {
        return when {
            roi > 20 -> Colors.SUCCESS // Great ROI
            roi > 0 -> Colors.WARNING // Positive ROI
            else -> Colors.ERROR // Negative ROI
        }
    }
    
    /**
     * REFACTOR: Extracted from MonthlyComparison.kt
     * Get color for growth percentage
     */
    fun getGrowthColor(growthPercentage: Double): Color {
        return when {
            growthPercentage > 0 -> Colors.SUCCESS // Positive growth
            growthPercentage < 0 -> Colors.ERROR // Negative growth
            else -> Colors.NEUTRAL // No change
        }
    }
    
    /**
     * REFACTOR: Extracted from ProjectionEstimation.kt
     * Get color for confidence level
     */
    fun getConfidenceColor(confidence: Double): Color {
        return when {
            confidence >= 90 -> Colors.SUCCESS
            confidence >= 75 -> Colors.INFO
            confidence >= 60 -> Colors.WARNING
            else -> Colors.ERROR
        }
    }
    
    /**
     * REFACTOR: Extracted from ExpenseDeepDive.kt
     * Get color for expense type
     */
    fun getExpenseTypeColor(expenseType: ExpenseType): Color {
        return when (expenseType) {
            ExpenseType.FUEL -> Colors.INFO // Blue
            ExpenseType.SERVICE -> Colors.SUCCESS // Green
            ExpenseType.CAR_WASH -> Color(0xFF00BCD4) // Cyan
            ExpenseType.FINE -> Colors.ERROR // Red
            ExpenseType.MAINTENANCE -> Colors.WARNING // Orange
            ExpenseType.OTHER -> AccentNavy // Professional accent blue
        }
    }
    
    /**
     * REFACTOR: Extracted from AnomalyDetection.kt
     * Get color and icon for anomaly type
     */
    fun getAnomalyColor(type: AnomalyType): Color {
        return when (type) {
            AnomalyType.LOW_INCOME -> Colors.ERROR
            AnomalyType.HIGH_EXPENSES -> Colors.WARNING
            AnomalyType.ZERO_INCOME -> AccentNavy // Professional accent blue
            AnomalyType.UNUSUAL_PATTERN -> Colors.NEUTRAL
        }
    }
    
    /**
     * REFACTOR: Extracted from AnomalyDetection.kt
     * Get color for severity level
     */
    fun getSeverityColor(severity: String): Color {
        return when (severity) {
            "Critical" -> Colors.CRITICAL
            "High" -> Colors.HIGH
            "Medium" -> Colors.MEDIUM
            "Low" -> Colors.LOW
            else -> Colors.UNKNOWN
        }
    }
    
    /**
     * REFACTOR: Extracted from DayOfWeekChart.kt
     * Get color for day of week
     */
    fun getDayOfWeekColor(dayOfWeek: DayOfWeek): Color {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> Colors.INFO
            DayOfWeek.TUESDAY -> Colors.SUCCESS
            DayOfWeek.WEDNESDAY -> Colors.WARNING
            DayOfWeek.THURSDAY -> AccentNavy // Professional accent blue
            DayOfWeek.FRIDAY -> Colors.ERROR
            DayOfWeek.SATURDAY -> Colors.NEUTRAL
            DayOfWeek.SUNDAY -> Color(0xFF795548) // Brown
        }
    }
    
    /**
     * REFACTOR: Extracted from DriverComparison.kt and TopDriversLeaderboard.kt
     * Get ranking color with transparency for backgrounds
     */
    fun getRankingBackgroundColor(rank: Int): Color {
        return when (rank) {
            1 -> Colors.GOLD.copy(alpha = 0.1f)
            2 -> Colors.SILVER.copy(alpha = 0.1f)
            3 -> Colors.BRONZE.copy(alpha = 0.1f)
            else -> Color.Transparent
        }
    }
    
    /**
     * REFACTOR: Extracted from DriverComparison.kt and TopDriversLeaderboard.kt
     * Get ranking color for badges
     */
    fun getRankingColor(rank: Int): Color {
        return when (rank) {
            1 -> Colors.GOLD
            2 -> Colors.SILVER
            3 -> Colors.BRONZE
            else -> Colors.INFO
        }
    }

    // ==================== FORMATTING UTILITIES ====================
    
    /**
     * REFACTOR: Moving from AnalyticsCalculator to centralize all formatting
     * Format currency values consistently across all components
     */
    fun formatCurrency(amount: Double): String {
        return "AED ${String.format("%.2f", amount)}"
    }
    
    /**
     * REFACTOR: Moving from AnalyticsCalculator to centralize all formatting
     * Format percentage values consistently
     */
    fun formatPercentage(percentage: Double): String {
        return "${String.format("%.1f", percentage)}%"
    }
    
    /**
     * REFACTOR: Extracted from multiple components using String.format("%.1f", value)
     * Format decimal values to 1 decimal place
     */
    fun formatDecimal(value: Double): String {
        return String.format("%.1f", value)
    }
    
    /**
     * REFACTOR: Extracted from multiple components using String.format("%.0f", value)
     * Format values to whole numbers
     */
    fun formatWholeNumber(value: Double): String {
        return String.format("%.0f", value)
    }

    // ==================== DATE UTILITIES ====================
    
    /**
     * REFACTOR: Extracted from AnalyticsCalculator and multiple components
     * Convert Date to LocalDate consistently
     */
    fun dateToLocalDate(date: Date): LocalDate {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    /**
     * REFACTOR: Extracted from DayOfWeekChart.kt and other components
     * Get display name for day of week
     */
    fun getDayDisplayName(dayOfWeek: DayOfWeek, style: TextStyle = TextStyle.FULL): String {
        return dayOfWeek.getDisplayName(style, Locale.getDefault())
    }
    
    /**
     * REFACTOR: Extracted from MockDataProvider.kt and AnalyticsViewModel.kt
     * Check if date is in current month (d-1 logic: exclude today)
     */
    fun isCurrentMonth(date: LocalDate): Boolean {
        val now = LocalDate.now()
        val today = LocalDate.now()
        return date.month == now.month && date.year == now.year && date.isBefore(today)
    }
    
    /**
     * REFACTOR: Extracted from MockDataProvider.kt and AnalyticsViewModel.kt
     * Check if date is in previous month (d-1 logic applied)
     */
    fun isPreviousMonth(date: LocalDate): Boolean {
        val previousMonth = LocalDate.now().minusMonths(1)
        return date.month == previousMonth.month && date.year == previousMonth.year
    }
    
    /**
     * REFACTOR: Extracted from multiple components
     * Get current month name
     */
    fun getCurrentMonthName(): String {
        return LocalDate.now().month.name
    }
    
    /**
     * REFACTOR: Extracted from multiple components
     * Get previous month name
     */
    fun getPreviousMonthName(): String {
        return LocalDate.now().minusMonths(1).month.name
    }

    // ==================== PERFORMANCE UTILITIES ====================
    
    /**
     * REFACTOR: Extracted from DayOfWeekChart.kt
     * Get performance level description
     */
    fun getPerformanceLevel(averageIncome: Double): String {
        return when {
            averageIncome >= 300 -> "Excellent"
            averageIncome >= 200 -> "Good"
            averageIncome >= 100 -> "Average"
            averageIncome > 0 -> "Below Avg"
            else -> "No Data"
        }
    }
    
    /**
     * REFACTOR: Extracted from AnomalyDetection.kt
     * Get severity level from deviation
     */
    fun getSeverityLevel(deviation: Double): String {
        return when {
            deviation > 0.8 -> "Critical"
            deviation > 0.5 -> "High"
            deviation > 0.3 -> "Medium"
            else -> "Low"
        }
    }
    
    /**
     * REFACTOR: Extracted from ProjectionEstimation.kt
     * Get confidence label
     */
    fun getConfidenceLabel(confidence: Double): String {
        return when {
            confidence >= 90 -> "Very High"
            confidence >= 75 -> "High"
            confidence >= 60 -> "Moderate"
            else -> "Low"
        }
    }

    // ==================== CHART UTILITIES ====================
    
    /**
     * REFACTOR: Extracted from TrendsChart.kt - standardized chart colors
     * Get standard chart colors for income/expense/profit
     */
    object ChartColors {
        val INCOME = Colors.SUCCESS
        val EXPENSES = Colors.ERROR
        val PROFIT = Colors.INFO
    }
    
    /**
     * REFACTOR: Extracted from multiple components
     * Calculate progress percentage safely
     */
    fun calculateProgress(value: Double, maxValue: Double): Float {
        return if (maxValue > 0) (value / maxValue).toFloat() else 0f
    }
    
    /**
     * REFACTOR: Extracted from multiple components
     * Get alpha color for backgrounds
     */
    fun getAlphaColor(color: Color, alpha: Float = 0.1f): Color {
        return color.copy(alpha = alpha)
    }

    // ==================== INSIGHT GENERATORS ====================
    
    /**
     * REFACTOR: Extracted from MonthlyComparison.kt
     * Get growth description
     */
    fun getGrowthDescription(growthPercentage: Double): String {
        return when {
            growthPercentage > 20 -> "Exceptional growth this month!"
            growthPercentage > 10 -> "Strong performance improvement"
            growthPercentage > 5 -> "Steady growth trend"
            growthPercentage > 0 -> "Slight improvement"
            growthPercentage == 0.0 -> "Performance unchanged"
            growthPercentage > -5 -> "Minor decline"
            growthPercentage > -10 -> "Noticeable decrease"
            growthPercentage > -20 -> "Significant decline"
            else -> "Major performance drop"
        }
    }
    
    /**
     * REFACTOR: Extracted from VehicleROI.kt
     * Get ROI interpretation
     */
    fun getROIInterpretation(roi: Double): String {
        return when {
            roi > 50 -> "Excellent performance! This vehicle is highly profitable."
            roi > 20 -> "Good performance. Strong return on investment."
            roi > 0 -> "Positive ROI but room for improvement."
            roi == 0.0 -> "Breaking even. Consider optimizing operations."
            else -> "Losing money. Immediate attention needed."
        }
    }
}