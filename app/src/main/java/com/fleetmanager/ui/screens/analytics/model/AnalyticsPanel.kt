package com.fleetmanager.ui.screens.analytics.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * GENERALIZATION: Analytics panel definitions for menu navigation
 */

enum class AnalyticsPanel(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val category: AnalyticsCategory
) {
    // Performance Analytics
    TRENDS(
        displayName = "Trends",
        description = "Income and expense trends over time",
        icon = Icons.Default.TrendingUp,
        category = AnalyticsCategory.PERFORMANCE
    ),
    MONTHLY_COMPARISON(
        displayName = "Monthly Comparison",
        description = "Current vs previous month performance",
        icon = Icons.Default.CalendarMonth,
        category = AnalyticsCategory.PERFORMANCE
    ),
    PROJECTION(
        displayName = "Projections",
        description = "End-of-month revenue estimates",
        icon = Icons.Default.QueryStats,
        category = AnalyticsCategory.PERFORMANCE
    ),
    
    // Driver Analytics
    DRIVER_PERFORMANCE(
        displayName = "Driver Performance",
        description = "Compare driver revenue and activity",
        icon = Icons.Default.Person,
        category = AnalyticsCategory.DRIVERS
    ),
    TOP_DRIVERS(
        displayName = "Top Drivers",
        description = "Leading drivers leaderboard",
        icon = Icons.Default.EmojiEvents,
        category = AnalyticsCategory.DRIVERS
    ),
    
    // Vehicle Analytics
    VEHICLE_ROI(
        displayName = "Vehicle ROI",
        description = "Return on investment analysis",
        icon = Icons.Default.DirectionsCar,
        category = AnalyticsCategory.VEHICLES
    ),
    
    // Pattern Analytics
    DAY_OF_WEEK(
        displayName = "Weekly Patterns",
        description = "Average income by day of week",
        icon = Icons.Default.CalendarToday,
        category = AnalyticsCategory.PATTERNS
    ),
    EXPENSE_BREAKDOWN(
        displayName = "Expense Analysis",
        description = "Detailed expense breakdown",
        icon = Icons.Default.Receipt,
        category = AnalyticsCategory.PATTERNS
    ),
    
    // Insights Analytics
    ANOMALY_DETECTION(
        displayName = "Anomalies",
        description = "Unusual patterns and outliers",
        icon = Icons.Default.Warning,
        category = AnalyticsCategory.INSIGHTS
    ),
    CALENDAR_VIEW(
        displayName = "Calendar",
        description = "Daily earnings calendar view",
        icon = Icons.Default.DateRange,
        category = AnalyticsCategory.INSIGHTS
    );
    
    companion object {
        fun getByCategory(category: AnalyticsCategory): List<AnalyticsPanel> {
            return values().filter { it.category == category }
        }
        
        fun getAllCategories(): List<AnalyticsCategory> {
            return AnalyticsCategory.values().toList()
        }
    }
}

enum class AnalyticsCategory(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    PERFORMANCE(
        displayName = "Performance",
        description = "Revenue trends and projections",
        icon = Icons.Default.TrendingUp
    ),
    DRIVERS(
        displayName = "Drivers",
        description = "Driver performance and rankings",
        icon = Icons.Default.Person
    ),
    VEHICLES(
        displayName = "Vehicles",
        description = "Vehicle profitability analysis",
        icon = Icons.Default.DirectionsCar
    ),
    PATTERNS(
        displayName = "Patterns",
        description = "Time and expense patterns",
        icon = Icons.Default.Analytics
    ),
    INSIGHTS(
        displayName = "Insights",
        description = "Anomalies and detailed views",
        icon = Icons.Default.Insights
    )
}