package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case for getting dashboard data with real-time Firestore updates.
 * Encapsulates the business logic for calculating dashboard statistics using snapshot listeners.
 */
class GetDashboardDataRealtimeUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<DashboardData> {
        return combine(
            repository.getAllDailyEntriesRealtime(),
            repository.getAllDrivers(),
            repository.getAllActiveVehicles()
        ) { entries, drivers, vehicles ->
            calculateDashboardData(entries, drivers, vehicles)
        }
    }

    private fun calculateDashboardData(
        entries: List<DailyEntry>,
        drivers: List<Driver>,
        vehicles: List<Vehicle>
    ): DashboardData {
        val driverNameMap = drivers.associateBy({ it.id }, { it.name })
        val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
        val enrichedEntries = entries.map { entry ->
            entry.withResolvedDisplayData(
                driverDisplayName = driverNameMap[entry.driverId],
                vehicleDisplayName = vehicleNameMap[entry.vehicleId]
            )
        }

        val now = Date()
        val calendar = Calendar.getInstance()

        // This month
        calendar.time = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        val thisMonthEntries = enrichedEntries.filter { it.date >= startOfMonth }
        val thisMonthEarnings = thisMonthEntries.sumOf { it.totalEarnings }
        val thisMonthUberEarnings = thisMonthEntries.sumOf { it.uberEarnings }
        val thisMonthYangoEarnings = thisMonthEntries.sumOf { it.yangoEarnings }
        val thisMonthPrivateEarnings = thisMonthEntries.sumOf { it.privateJobsEarnings }
        
        // This week (Monday to Sunday)
        calendar.time = now
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.time
        val thisWeekEntries = enrichedEntries.filter { it.date >= startOfWeek }
        val thisWeekEarnings = thisWeekEntries.sumOf { it.totalEarnings }
        val thisWeekUberEarnings = thisWeekEntries.sumOf { it.uberEarnings }
        val thisWeekYangoEarnings = thisWeekEntries.sumOf { it.yangoEarnings }
        val thisWeekPrivateEarnings = thisWeekEntries.sumOf { it.privateJobsEarnings }
        
        // Last 40 hours (but labeled as 24h in UI)
        val last40Hours = Date(now.time - TimeUnit.HOURS.toMillis(40))
        val last24hEarnings = enrichedEntries
            .filter { it.date >= last40Hours }
            .sumOf { it.totalEarnings }

        // Active drivers count
        val activeDriversCount = entries.distinctBy { it.driverId }.size

        // Recent entries (last 5)
        val recentEntries = enrichedEntries
            .sortedByDescending { it.date }
            .take(5)
        
        return DashboardData(
            thisMonthEarnings = thisMonthEarnings,
            thisWeekEarnings = thisWeekEarnings,
            last24hEarnings = last24hEarnings,
            activeDriversCount = activeDriversCount,
            recentEntries = recentEntries,
            thisMonthUberEarnings = thisMonthUberEarnings,
            thisMonthYangoEarnings = thisMonthYangoEarnings,
            thisMonthPrivateEarnings = thisMonthPrivateEarnings,
            thisWeekUberEarnings = thisWeekUberEarnings,
            thisWeekYangoEarnings = thisWeekYangoEarnings,
            thisWeekPrivateEarnings = thisWeekPrivateEarnings
        )
    }
}