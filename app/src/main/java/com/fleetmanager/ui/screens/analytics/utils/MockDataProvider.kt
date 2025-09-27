package com.fleetmanager.ui.screens.analytics.utils

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.ui.screens.analytics.model.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import kotlin.random.Random

/**
 * Provides mock data for analytics when Firestore is empty or for testing purposes.
 */
object MockDataProvider {

    private val driverNames = listOf("Ahmed Ali", "Mohammed Hassan", "Omar Khalil", "Youssef Ibrahim", "Khalid Saeed")
    private val vehicleNames = listOf("Toyota Camry - ABC123", "Honda Accord - XYZ789", "Nissan Altima - DEF456", "Hyundai Sonata - GHI789", "Kia Optima - JKL012")

    /**
     * Generate mock daily entries for the last 90 days
     */
    fun generateMockDailyEntries(): List<DailyEntry> {
        val entries = mutableListOf<DailyEntry>()
        val endDate = LocalDate.now().minusDays(1) // d-1 logic: exclude today
        val startDate = endDate.minusDays(90)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            // Skip some days randomly to simulate real usage patterns
            if (Random.nextDouble() > 0.3) { // 70% chance of having entries
                val entriesForDay = Random.nextInt(1, 4) // 1-3 entries per day
                
                repeat(entriesForDay) {
                    entries.add(generateRandomDailyEntry(currentDate))
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return entries
    }

    /**
     * Generate mock expenses for the last 90 days
     */
    fun generateMockExpenses(): List<Expense> {
        val expenses = mutableListOf<Expense>()
        val endDate = LocalDate.now().minusDays(1) // d-1 logic: exclude today
        val startDate = endDate.minusDays(90)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            // Random chance of having expenses (less frequent than income)
            if (Random.nextDouble() > 0.7) { // 30% chance of having expenses
                val expensesForDay = Random.nextInt(1, 3) // 1-2 expenses per day
                
                repeat(expensesForDay) {
                    expenses.add(generateRandomExpense(currentDate))
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return expenses
    }

    /**
     * Generate mock analytics data with realistic values
     */
    fun generateMockAnalyticsData(): AnalyticsData {
        val entries = generateMockDailyEntries()
        val expenses = generateMockExpenses()
        
        val endDate = LocalDate.now().minusDays(1) // d-1 logic: exclude today
        val startDate = endDate.minusDays(30)

        val dayOfWeekAnalysis = AnalyticsCalculator.calculateDayOfWeekAnalysis(entries)

        return AnalyticsData(
            trendData = AnalyticsCalculator.calculateTrendData(entries, expenses, startDate, endDate),
            driverPerformance = AnalyticsCalculator.calculateDriverPerformance(entries),
            vehicleROI = AnalyticsCalculator.calculateVehicleROI(entries, expenses),
            dayOfWeekAnalysis = dayOfWeekAnalysis,
            expenseBreakdown = AnalyticsCalculator.calculateExpenseBreakdown(expenses),
            anomalies = AnalyticsCalculator.detectAnomalies(entries, expenses),
            monthlyComparison = generateMockMonthlyComparison(),
            projection = AnalyticsCalculator.calculateProjection(
                entries.filter {
                    val entryDate = AnalyticsUtils.dateToLocalDate(it.date)
                    AnalyticsUtils.isCurrentMonth(entryDate)
                },
                dayOfWeekAnalysis,
                endDate // Use yesterday instead of today
            )
        )
    }

    private fun generateRandomDailyEntry(date: LocalDate): DailyEntry {
        val driver = driverNames.random()
        val vehicle = vehicleNames.random()
        val dateAsDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

        return DailyEntry(
            id = UUID.randomUUID().toString(),
            userId = "mock_user",
            date = dateAsDate,
            driverId = driver.lowercase().replace(" ", "_").replace("'", ""),
            driverName = driver,
            vehicleId = vehicle.lowercase().replace(" ", "_").replace("'", ""),
            vehicle = vehicle,
            uberEarnings = if (Random.nextDouble() > 0.3) Random.nextDouble() * 200 + 50 else 0.0,
            yangoEarnings = if (Random.nextDouble() > 0.4) Random.nextDouble() * 150 + 30 else 0.0,
            privateJobsEarnings = if (Random.nextDouble() > 0.6) Random.nextDouble() * 100 + 20 else 0.0,
            notes = "Mock entry for AEDdate",
            isSynced = true,
            createdAt = dateAsDate,
            updatedAt = dateAsDate
        )
    }

    private fun generateRandomExpense(date: LocalDate): Expense {
        val driver = driverNames.random()
        val vehicle = vehicleNames.random()
        val dateAsDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val expenseType = ExpenseType.values().random()
        val driverId = driver.lowercase().replace(" ", "_").replace("'", "")

        val amount = when (expenseType) {
            ExpenseType.FUEL -> Random.nextDouble() * 80 + 40 // 40-120 AED
            ExpenseType.MAINTENANCE -> Random.nextDouble() * 200 + 100 // 100-300 AED
            ExpenseType.SERVICE -> Random.nextDouble() * 300 + 150 // 150-450 AED
            ExpenseType.CAR_WASH -> Random.nextDouble() * 30 + 15 // 15-45 AED
            ExpenseType.FINE -> Random.nextDouble() * 400 + 200 // 200-600 AED
            ExpenseType.OTHER -> Random.nextDouble() * 100 + 25 // 25-125 AED
        }

        return Expense(
            id = UUID.randomUUID().toString(),
            userId = driverId,
            driverId = driverId,
            type = expenseType,
            amount = amount,
            date = dateAsDate,
            driverName = driver,
            vehicle = vehicle,
            notes = "Mock AED{expenseType.displayName} expense",
            isSynced = true,
            createdAt = dateAsDate,
            updatedAt = dateAsDate
        )
    }

    private fun generateMockMonthlyComparison(): MonthlyComparison {
        val currentMonth = AnalyticsUtils.getCurrentMonthName()
        val previousMonth = AnalyticsUtils.getPreviousMonthName()
        
        val previousTotal = Random.nextDouble() * 8000 + 5000 // 5000-13000 AED
        val growthRate = Random.nextDouble() * 0.4 - 0.2 // -20% to +20%
        val currentTotal = previousTotal * (1 + growthRate)

        return MonthlyComparison(
            currentMonth = currentMonth,
            currentTotal = currentTotal,
            previousMonth = previousMonth,
            previousTotal = previousTotal,
            growthPercentage = growthRate * 100,
            growthAmount = currentTotal - previousTotal
        )
    }
}