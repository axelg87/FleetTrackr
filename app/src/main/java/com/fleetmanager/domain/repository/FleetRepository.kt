package com.fleetmanager.domain.repository

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Domain repository interface for fleet management operations.
 * This defines the contract that the data layer must implement.
 */
interface FleetRepository {
    
    // Daily Entries
    fun getAllDailyEntries(): Flow<List<DailyEntry>>
    fun getDailyEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntry>>
    fun getDailyEntryById(id: String): Flow<DailyEntry?>
    suspend fun saveDailyEntry(entry: DailyEntry, photoUri: Uri? = null, photoUris: List<Uri> = emptyList())
    suspend fun deleteDailyEntry(entryId: String)
    suspend fun getTotalEarningsForPeriod(startDate: Date, endDate: Date): Double
    
    // Sync operations
    suspend fun syncUnsyncedEntries()
    suspend fun fetchAndCacheRemoteEntries()
    
    // Drivers
    fun getAllActiveDrivers(): Flow<List<Driver>>
    fun getAllDrivers(): Flow<List<Driver>>
    suspend fun saveDriver(driver: Driver)
    suspend fun syncDrivers()
    
    // Vehicles
    fun getAllActiveVehicles(): Flow<List<Vehicle>>
    suspend fun saveVehicle(vehicle: Vehicle)
    suspend fun syncVehicles()
}