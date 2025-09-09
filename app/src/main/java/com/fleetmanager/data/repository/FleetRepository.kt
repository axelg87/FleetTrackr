package com.fleetmanager.data.repository

import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.model.DailyEntry
import com.fleetmanager.data.model.Driver
import com.fleetmanager.data.model.Vehicle
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.StorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import android.net.Uri
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FleetRepository @Inject constructor(
    private val dailyEntryDao: DailyEntryDao,
    private val driverDao: DriverDao,
    private val vehicleDao: VehicleDao,
    private val firestoreService: FirestoreService,
    private val storageService: StorageService
) {
    
    // Daily Entries - Offline-first approach
    fun getAllDailyEntries(): Flow<List<DailyEntry>> = dailyEntryDao.getAllEntries()
    
    fun getDailyEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntry>> = 
        dailyEntryDao.getEntriesByDateRange(startDate, endDate)
    
    suspend fun saveDailyEntry(entry: DailyEntry, photoUri: Uri? = null) {
        val entryToSave = if (photoUri != null) {
            try {
                val photoUrl = storageService.uploadPhoto(photoUri, entry.id)
                entry.copy(photoUrl = photoUrl, isSynced = true)
            } catch (e: Exception) {
                // Save locally if upload fails
                entry.copy(localPhotoPath = photoUri.toString(), isSynced = false)
            }
        } else {
            entry
        }
        
        // Always save locally first
        dailyEntryDao.insertEntry(entryToSave)
        
        // Try to sync to Firestore
        if (entryToSave.isSynced || photoUri == null) {
            try {
                firestoreService.saveDailyEntry(entryToSave.copy(isSynced = true))
                dailyEntryDao.markAsSynced(entryToSave.id)
            } catch (e: Exception) {
                // Will be synced later by WorkManager
            }
        }
    }
    
    suspend fun syncUnsyncedEntries() {
        val unsyncedEntries = dailyEntryDao.getUnsyncedEntries()
        unsyncedEntries.forEach { entry ->
            try {
                val syncedEntry = if (entry.localPhotoPath != null) {
                    val photoUrl = storageService.uploadPhoto(Uri.parse(entry.localPhotoPath), entry.id)
                    entry.copy(photoUrl = photoUrl, localPhotoPath = null, isSynced = true)
                } else {
                    entry.copy(isSynced = true)
                }
                
                firestoreService.saveDailyEntry(syncedEntry)
                dailyEntryDao.updateEntry(syncedEntry)
            } catch (e: Exception) {
                // Keep as unsynced for next attempt
            }
        }
    }
    
    suspend fun fetchAndCacheRemoteEntries() {
        try {
            val remoteEntries = firestoreService.getDailyEntries()
            dailyEntryDao.insertEntries(remoteEntries)
        } catch (e: Exception) {
            // Ignore errors, local data is still available
        }
    }
    
    suspend fun getTotalEarningsForPeriod(startDate: Date, endDate: Date): Double {
        return dailyEntryDao.getTotalEarningsForPeriod(startDate, endDate)
    }
    
    // Drivers
    fun getAllActiveDrivers(): Flow<List<Driver>> = driverDao.getAllActiveDrivers()
    
    suspend fun saveDriver(driver: Driver) {
        driverDao.insertDriver(driver)
        try {
            firestoreService.saveDriver(driver)
        } catch (e: Exception) {
            // Will be synced later
        }
    }
    
    suspend fun syncDrivers() {
        try {
            val remoteDrivers = firestoreService.getDrivers()
            driverDao.insertDrivers(remoteDrivers)
        } catch (e: Exception) {
            // Keep local data
        }
    }
    
    // Vehicles
    fun getAllActiveVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllActiveVehicles()
    
    suspend fun saveVehicle(vehicle: Vehicle) {
        vehicleDao.insertVehicle(vehicle)
        try {
            firestoreService.saveVehicle(vehicle)
        } catch (e: Exception) {
            // Will be synced later
        }
    }
    
    suspend fun syncVehicles() {
        try {
            val remoteVehicles = firestoreService.getVehicles()
            vehicleDao.insertVehicles(remoteVehicles)
        } catch (e: Exception) {
            // Keep local data
        }
    }
}