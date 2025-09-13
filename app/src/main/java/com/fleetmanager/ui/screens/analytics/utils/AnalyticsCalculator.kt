package com.fleetmanager.ui.screens.analytics.utils

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.ui.screens.analytics.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Centralized utility class for all analytics calculations.
 * Implements DRY principle by providing reusable calculation logic.
 */
object AnalyticsCalculator {

    // Configurable thresholds
    private const val HIGH_INCOME_THRESHOLD = 250.0
    private const val MEDIUM_INCOME_THRESHOLD = 100.0
    private const val ANOMALY_THRESHOLD_PERCENTAGE = 0.5 // 50%

    /**
     * Calculate trend data for a given period
     */
    fun calculateTrendData(
        entries: List<DailyEntry>,
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TrendData> {
        val entriesByDate = entries.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }
        val expensesByDate = expenses.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }

        val trendData = mutableListOf<TrendData>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val dayEntries = entriesByDate[currentDate] ?: emptyList()
            val dayExpenses = expensesByDate[currentDate] ?: emptyList()
            
            val income = dayEntries.sumOf { it.totalEarnings }
            val expenseTotal = dayExpenses.sumOf { it.amount }
            
            trendData.add(
                TrendData(
                    date = currentDate,
                    income = income,
                    expenses = expenseTotal,
                    netProfit = income - expenseTotal
                )
            )
            
            currentDate = currentDate.plusDays(1)
        }

        return trendData
    }

    /**
     * Calculate driver performance metrics
     */
    fun calculateDriverPerformance(entries: List<DailyEntry>): List<DriverPerformance> {
        return entries.groupBy { it.driverName }
            .map { (driverName, driverEntries) ->
                val totalRevenue = driverEntries.sumOf { it.totalEarnings }
                val uniqueDates = driverEntries.map { 
                    AnalyticsUtils.dateToLocalDate(it.date)
                }.toSet()
                val activeDays = uniqueDates.size
                val averageRevenuePerDay = if (activeDays > 0) totalRevenue / activeDays else 0.0

                DriverPerformance(
                    driverName = driverName,
                    totalRevenue = totalRevenue,
                    averageRevenuePerDay = averageRevenuePerDay,
                    activeDays = activeDays
                )
            }
            .sortedByDescending { it.totalRevenue }
    }

    /**
     * Calculate vehicle ROI analysis
     */
    fun calculateVehicleROI(
        entries: List<DailyEntry>,
        expenses: List<Expense>
    ): List<VehicleROI> {
        val incomeByVehicle = entries.groupBy { it.vehicle }
            .mapValues { (_, entries) -> entries.sumOf { it.totalEarnings } }
        
        val expensesByVehicle = expenses.groupBy { it.vehicle }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

        val allVehicles = (incomeByVehicle.keys + expensesByVehicle.keys).toSet()

        return allVehicles.map { vehicle ->
            val totalIncome = incomeByVehicle[vehicle] ?: 0.0
            val totalExpenses = expensesByVehicle[vehicle] ?: 0.0
            val netProfit = totalIncome - totalExpenses
            val roi = if (totalExpenses > 0) (netProfit / totalExpenses) * 100 else 0.0

            VehicleROI(
                vehicleName = vehicle,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                roi = roi
            )
        }.sortedByDescending { it.roi }
    }

    /**
     * Calculate day of week analysis
     */
    fun calculateDayOfWeekAnalysis(entries: List<DailyEntry>): List<DayOfWeekAnalysis> {
        val entriesByDayOfWeek = entries.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date).dayOfWeek
        }

        return DayOfWeek.values().map { dayOfWeek ->
            val dayEntries = entriesByDayOfWeek[dayOfWeek] ?: emptyList()
            val totalIncome = dayEntries.sumOf { it.totalEarnings }
            val uniqueDates = dayEntries.map { 
                AnalyticsUtils.dateToLocalDate(it.date)
            }.toSet().size
            val averageIncome = if (uniqueDates > 0) totalIncome / uniqueDates else 0.0

            DayOfWeekAnalysis(
                dayOfWeek = dayOfWeek,
                averageIncome = averageIncome,
                totalDays = uniqueDates,
                totalIncome = totalIncome
            )
        }
    }

    /**
     * Calculate expense breakdown by type
     */
    fun calculateExpenseBreakdown(expenses: List<Expense>): List<ExpenseBreakdown> {
        val totalExpenses = expenses.sumOf { it.amount }
        
        return expenses.groupBy { it.type }
            .map { (type, typeExpenses) ->
                val typeTotal = typeExpenses.sumOf { it.amount }
                val percentage = if (totalExpenses > 0) (typeTotal / totalExpenses) * 100 else 0.0

                ExpenseBreakdown(
                    expenseType = type,
                    totalAmount = typeTotal,
                    percentage = percentage,
                    count = typeExpenses.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Detect anomalies in income and expenses
     */
    fun detectAnomalies(
        entries: List<DailyEntry>,
        expenses: List<Expense>
    ): List<AnomalyData> {
        val anomalies = mutableListOf<AnomalyData>()
        
        // Calculate averages
        val averageIncome = calculateAverageIncome(entries)
        val averageExpenses = calculateAverageExpenses(expenses)
        
        // Check income anomalies
        val incomeByDate = entries.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }.mapValues { (_, entries) -> entries.sumOf { it.totalEarnings } }
        
        incomeByDate.forEach { (date, income) ->
            val deviationFromAverage = abs(income - averageIncome) / averageIncome
            
            when {
                income == 0.0 && averageIncome > 0 -> {
                    anomalies.add(
                        AnomalyData(
                            date = date,
                            type = AnomalyType.ZERO_INCOME,
                            actualValue = income,
                            expectedValue = averageIncome,
                            deviation = deviationFromAverage,
                            reason = "No income recorded"
                        )
                    )
                }
                income < averageIncome * (1 - ANOMALY_THRESHOLD_PERCENTAGE) -> {
                    anomalies.add(
                        AnomalyData(
                            date = date,
                            type = AnomalyType.LOW_INCOME,
                            actualValue = income,
                            expectedValue = averageIncome,
                            deviation = deviationFromAverage,
                            reason = "Income ${String.format("%.0f", deviationFromAverage * 100)}% below average"
                        )
                    )
                }
            }
        }
        
        // Check expense anomalies
        val expensesByDate = expenses.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }.mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
        
        expensesByDate.forEach { (date, expenseTotal) ->
            if (expenseTotal > averageExpenses * (1 + ANOMALY_THRESHOLD_PERCENTAGE)) {
                val deviationFromAverage = abs(expenseTotal - averageExpenses) / averageExpenses
                anomalies.add(
                    AnomalyData(
                        date = date,
                        type = AnomalyType.HIGH_EXPENSES,
                        actualValue = expenseTotal,
                        expectedValue = averageExpenses,
                        deviation = deviationFromAverage,
                        reason = "Expenses ${String.format("%.0f", deviationFromAverage * 100)}% above average"
                    )
                )
            }
        }
        
        return anomalies.sortedByDescending { it.date }
    }

    /**
     * Calculate monthly comparison
     */
    fun calculateMonthlyComparison(
        currentMonthEntries: List<DailyEntry>,
        previousMonthEntries: List<DailyEntry>,
        currentMonthName: String,
        previousMonthName: String
    ): MonthlyComparison {
        val currentTotal = currentMonthEntries.sumOf { it.totalEarnings }
        val previousTotal = previousMonthEntries.sumOf { it.totalEarnings }
        
        val growthAmount = currentTotal - previousTotal
        val growthPercentage = if (previousTotal > 0) (growthAmount / previousTotal) * 100 else 0.0

        return MonthlyComparison(
            currentMonth = currentMonthName,
            currentTotal = currentTotal,
            previousMonth = previousMonthName,
            previousTotal = previousTotal,
            growthPercentage = growthPercentage,
            growthAmount = growthAmount
        )
    }

    /**
     * Calculate projection for end of month
     */
    fun calculateProjection(
        currentMonthEntries: List<DailyEntry>,
        currentDate: LocalDate
    ): ProjectionData {
        val currentTotal = currentMonthEntries.sumOf { it.totalEarnings }
        val dayOfMonth = currentDate.dayOfMonth
        val totalDaysInMonth = currentDate.lengthOfMonth()
        
        val dailyAverage = if (dayOfMonth > 0) currentTotal / dayOfMonth else 0.0
        val projectedTotal = dailyAverage * totalDaysInMonth
        
        return ProjectionData(
            currentMonthTotal = currentTotal,
            projectedMonthTotal = projectedTotal,
            daysElapsed = dayOfMonth,
            totalDaysInMonth = totalDaysInMonth,
            dailyAverage = dailyAverage,
            comparisonToPrevious = 0.0 // Will be calculated when previous month data is available
        )
    }

    /**
     * Get income level for color coding
     */
    fun getIncomeLevel(totalIncome: Double): IncomeLevel {
        return when {
            totalIncome >= HIGH_INCOME_THRESHOLD -> IncomeLevel.HIGH
            totalIncome >= MEDIUM_INCOME_THRESHOLD -> IncomeLevel.MEDIUM
            totalIncome > 0 -> IncomeLevel.LOW
            else -> IncomeLevel.NONE
        }
    }

    /**
     * REFACTOR: Formatting functions moved to AnalyticsUtils for better organization
     * These are kept as delegates for backward compatibility during refactor
     */
    fun formatCurrency(amount: Double): String = AnalyticsUtils.formatCurrency(amount)
    fun formatPercentage(percentage: Double): String = AnalyticsUtils.formatPercentage(percentage)

    private fun calculateAverageIncome(entries: List<DailyEntry>): Double {
        val incomeByDate = entries.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }.mapValues { (_, entries) -> entries.sumOf { it.totalEarnings } }
        
        return if (incomeByDate.isNotEmpty()) {
            incomeByDate.values.sum() / incomeByDate.size
        } else 0.0
    }

    private fun calculateAverageExpenses(expenses: List<Expense>): Double {
        val expensesByDate = expenses.groupBy { 
            AnalyticsUtils.dateToLocalDate(it.date)
        }.mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
        
        return if (expensesByDate.isNotEmpty()) {
            expensesByDate.values.sum() / expensesByDate.size
        } else 0.0
    }
}