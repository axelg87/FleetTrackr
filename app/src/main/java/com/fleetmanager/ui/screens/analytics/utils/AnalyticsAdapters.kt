package com.fleetmanager.ui.screens.analytics.utils

import com.fleetmanager.ui.screens.analytics.components.ChartDataPoint
import com.fleetmanager.ui.screens.analytics.components.ChartSeries
import com.fleetmanager.ui.screens.analytics.components.ChartSeriesType
import com.fleetmanager.ui.screens.analytics.components.LeaderboardItem
import com.fleetmanager.ui.screens.analytics.model.*

/**
 * GENERALIZATION: Adapter functions to convert domain models to generic chart/leaderboard formats
 * Eliminates boilerplate conversion logic across components
 */
object AnalyticsAdapters {

    /**
     * Convert TrendData to chart series for line chart
     */
    fun trendDataToChartSeries(trendData: List<TrendData>): List<ChartSeries> {
        return listOf(
            ChartSeries(
                name = "Income",
                data = trendData.map { trend ->
                    ChartDataPoint(
                        label = trend.date.toString(),
                        value = trend.income,
                        color = AnalyticsUtils.ChartColors.INCOME
                    )
                },
                color = AnalyticsUtils.ChartColors.INCOME,
                type = ChartSeriesType.LINE
            ),
            ChartSeries(
                name = "Expenses", 
                data = trendData.map { trend ->
                    ChartDataPoint(
                        label = trend.date.toString(),
                        value = trend.expenses,
                        color = AnalyticsUtils.ChartColors.EXPENSES
                    )
                },
                color = AnalyticsUtils.ChartColors.EXPENSES,
                type = ChartSeriesType.LINE
            ),
            ChartSeries(
                name = "Net Profit",
                data = trendData.map { trend ->
                    ChartDataPoint(
                        label = trend.date.toString(),
                        value = trend.netProfit,
                        color = AnalyticsUtils.ChartColors.PROFIT
                    )
                },
                color = AnalyticsUtils.ChartColors.PROFIT,
                type = ChartSeriesType.LINE
            )
        )
    }

    /**
     * Convert DriverPerformance to leaderboard items
     */
    fun driverPerformanceToLeaderboard(drivers: List<DriverPerformance>): List<LeaderboardItem> {
        return drivers.map { driver ->
            LeaderboardItem(
                id = driver.driverName,
                name = driver.driverName,
                primaryValue = driver.totalRevenue,
                primaryLabel = "Total Revenue",
                secondaryValue = driver.averageRevenuePerDay,
                secondaryLabel = "Avg/Day",
                tertiaryValue = "${driver.activeDays} days",
                tertiaryLabel = "Active Days",
                metadata = mapOf(
                    "activeDays" to driver.activeDays,
                    "totalTrips" to driver.totalTrips
                )
            )
        }
    }

    /**
     * Convert VehicleROI to leaderboard items
     */
    fun vehicleROIToLeaderboard(vehicles: List<VehicleROI>): List<LeaderboardItem> {
        return vehicles.map { vehicle ->
            LeaderboardItem(
                id = vehicle.vehicleName,
                name = vehicle.vehicleName,
                primaryValue = vehicle.netProfit,
                primaryLabel = "Net Profit",
                secondaryValue = vehicle.totalIncome,
                secondaryLabel = "Income",
                tertiaryValue = "${AnalyticsUtils.formatDecimal(vehicle.roi)}%",
                tertiaryLabel = "ROI",
                metadata = mapOf(
                    "roi" to vehicle.roi,
                    "expenses" to vehicle.totalExpenses
                )
            )
        }
    }

    /**
     * Convert DayOfWeekAnalysis to chart data
     */
    fun dayOfWeekToChartData(analysis: List<DayOfWeekAnalysis>): List<ChartDataPoint> {
        return analysis.map { dayAnalysis ->
            ChartDataPoint(
                label = AnalyticsUtils.getDayDisplayName(dayAnalysis.dayOfWeek),
                value = dayAnalysis.averageIncome,
                color = AnalyticsUtils.getDayOfWeekColor(dayAnalysis.dayOfWeek),
                metadata = mapOf(
                    "totalDays" to dayAnalysis.totalDays,
                    "totalIncome" to dayAnalysis.totalIncome,
                    "dayOfWeek" to dayAnalysis.dayOfWeek
                )
            )
        }
    }

    /**
     * Convert ExpenseBreakdown to chart data
     */
    fun expenseBreakdownToChartData(breakdown: List<ExpenseBreakdown>): List<ChartDataPoint> {
        return breakdown.map { expense ->
            ChartDataPoint(
                label = expense.expenseType.displayName,
                value = expense.totalAmount,
                color = AnalyticsUtils.getExpenseTypeColor(expense.expenseType),
                metadata = mapOf(
                    "percentage" to expense.percentage,
                    "count" to expense.count,
                    "expenseType" to expense.expenseType
                )
            )
        }
    }

    /**
     * Convert DriverPerformance to chart data for comparison
     */
    fun driverPerformanceToChartData(drivers: List<DriverPerformance>): List<ChartDataPoint> {
        return drivers.map { driver ->
            ChartDataPoint(
                label = driver.driverName,
                value = driver.totalRevenue,
                color = AnalyticsUtils.Colors.INFO,
                secondaryValue = driver.averageRevenuePerDay,
                metadata = mapOf(
                    "activeDays" to driver.activeDays,
                    "totalTrips" to driver.totalTrips
                )
            )
        }
    }

    /**
     * Convert VehicleROI to chart data for comparison
     */
    fun vehicleROIToChartData(vehicles: List<VehicleROI>): List<ChartDataPoint> {
        return vehicles.map { vehicle ->
            ChartDataPoint(
                label = vehicle.vehicleName,
                value = vehicle.netProfit,
                color = AnalyticsUtils.getROIColor(vehicle.roi),
                secondaryValue = vehicle.roi,
                metadata = mapOf(
                    "income" to vehicle.totalIncome,
                    "expenses" to vehicle.totalExpenses,
                    "roi" to vehicle.roi
                )
            )
        }
    }

    /**
     * Create summary statistics for any numeric dataset
     */
    fun createSummaryStats(
        data: List<Double>,
        label: String = "Value"
    ): Map<String, String> {
        if (data.isEmpty()) return emptyMap()
        
        val total = data.sum()
        val average = total / data.size
        val max = data.maxOrNull() ?: 0.0
        val min = data.minOrNull() ?: 0.0
        
        return mapOf(
            "total" to AnalyticsUtils.formatCurrency(total),
            "average" to AnalyticsUtils.formatCurrency(average),
            "max" to AnalyticsUtils.formatCurrency(max),
            "min" to AnalyticsUtils.formatCurrency(min),
            "count" to data.size.toString()
        )
    }

    /**
     * Create performance insights from leaderboard data
     */
    fun generateLeaderboardInsights(items: List<LeaderboardItem>): String {
        if (items.isEmpty()) return "No data available for insights."
        
        val topPerformer = items.firstOrNull()
        val bottomPerformer = items.lastOrNull()
        val average = items.sumOf { it.primaryValue } / items.size
        
        return when {
            items.size == 1 -> "Only one item in the leaderboard. Add more data for comparisons."
            
            topPerformer != null && bottomPerformer != null -> {
                val gap = topPerformer.primaryValue - bottomPerformer.primaryValue
                val gapPercentage = if (bottomPerformer.primaryValue > 0) {
                    (gap / bottomPerformer.primaryValue) * 100
                } else 100.0
                
                when {
                    gapPercentage > 100 -> "${topPerformer.name} significantly outperforms others with ${AnalyticsUtils.formatCurrency(gap)} more than the lowest performer."
                    gapPercentage > 50 -> "There's a notable performance gap. ${topPerformer.name} leads by ${AnalyticsUtils.formatPercentage(gapPercentage)}."
                    gapPercentage > 20 -> "Performance is moderately varied across the leaderboard."
                    else -> "Very consistent performance across all items in the leaderboard."
                }
            }
            
            else -> "Performance analysis available with more data points."
        }
    }
}