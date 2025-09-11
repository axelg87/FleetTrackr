package com.fleetmanager.data.repository

import android.net.Uri
import android.util.Log
import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.local.dao.ExpenseDao
import com.fleetmanager.data.mapper.DailyEntryMapper
import com.fleetmanager.data.mapper.DriverMapper
import com.fleetmanager.data.mapper.VehicleMapper
import com.fleetmanager.data.mapper.ExpenseMapper
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.StorageService
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FleetRepositoryImpl @Inject constructor(
    private val dailyEntryDao: DailyEntryDao,
    private val driverDao: DriverDao,
    private val vehicleDao: VehicleDao,
    private val expenseDao: ExpenseDao,
    private val firestoreService: FirestoreService,
    private val storageService: StorageService
) : FleetRepository {
    
    companion object {
        private const val TAG = "FleetRepositoryImpl"
    }
    
    // Daily Entries - Offline-first approach
    override fun getAllDailyEntries(): Flow<List<DailyEntry>> = 
        dailyEntryDao.getAllEntries().map { DailyEntryMapper.toDomainList(it) }
    
    override fun getDailyEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntry>> = 
        dailyEntryDao.getEntriesByDateRange(startDate, endDate).map { DailyEntryMapper.toDomainList(it) }
    
    override fun getDailyEntryById(id: String): Flow<DailyEntry?> = flow {
        val dto = dailyEntryDao.getEntryById(id)
        emit(dto?.let { DailyEntryMapper.toDomain(it) })
    }
    
    override suspend fun saveDailyEntry(entry: DailyEntry, photoUri: Uri?, photoUris: List<Uri>) {
        val entryToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "${entry.id}_${System.currentTimeMillis()}")
                    }
                    entry.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, entry.id))
                    entry.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(isSynced = false)
                }
            }
            else -> entry
        }
        
        // Always save locally first
        dailyEntryDao.insertEntry(DailyEntryMapper.toDto(entryToSave))
        
        // Try to sync to Firestore
        if (entryToSave.isSynced || (photoUri == null && photoUris.isEmpty())) {
            try {
                Log.d(TAG, "Attempting to save daily entry to Firestore: ${entryToSave.id}")
                firestoreService.saveDailyEntry(entryToSave.copy(isSynced = true))
                dailyEntryDao.markAsSynced(entryToSave.id)
                Log.d(TAG, "Successfully saved daily entry to Firestore: ${entryToSave.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save daily entry to Firestore: ${entryToSave.id}", e)
                // Will be synced later by WorkManager
            }
        }
    }
    
    override suspend fun deleteDailyEntry(entryId: String) {
        val entry = dailyEntryDao.getEntryById(entryId)
        if (entry != null) {
            dailyEntryDao.deleteEntry(entry)
            try {
                firestoreService.deleteDailyEntry(entryId)
            } catch (e: Exception) {
                // Ignore remote delete errors
            }
        }
    }
    
    override suspend fun syncUnsyncedEntries() {
        val unsyncedEntries = dailyEntryDao.getUnsyncedEntries()
        unsyncedEntries.forEach { entryDto ->
            try {
                val entry = DailyEntryMapper.toDomain(entryDto)
                val syncedEntry = entry.copy(isSynced = true)
                
                firestoreService.saveDailyEntry(syncedEntry)
                dailyEntryDao.updateEntry(DailyEntryMapper.toDto(syncedEntry))
            } catch (e: Exception) {
                // Keep as unsynced for next attempt
            }
        }
    }
    
    override suspend fun fetchAndCacheRemoteEntries() {
        try {
            val remoteEntries = firestoreService.getDailyEntries()
            dailyEntryDao.insertEntries(DailyEntryMapper.toDtoList(remoteEntries))
        } catch (e: Exception) {
            // Ignore errors, local data is still available
        }
    }
    
    override suspend fun getTotalEarningsForPeriod(startDate: Date, endDate: Date): Double {
        return dailyEntryDao.getTotalEarningsForPeriod(startDate, endDate)
    }
    
    // Drivers
    override fun getAllActiveDrivers(): Flow<List<Driver>> = 
        driverDao.getAllActiveDrivers().map { DriverMapper.toDomainList(it) }
    
    override fun getAllDrivers(): Flow<List<Driver>> = 
        driverDao.getAllDrivers().map { DriverMapper.toDomainList(it) }
    
    override suspend fun saveDriver(driver: Driver) {
        driverDao.insertDriver(DriverMapper.toDto(driver))
        try {
            firestoreService.saveDriver(driver)
        } catch (e: Exception) {
            // Will be synced later
        }
    }
    
    override suspend fun syncDrivers() {
        try {
            val remoteDrivers = firestoreService.getDrivers()
            driverDao.insertDrivers(DriverMapper.toDtoList(remoteDrivers))
        } catch (e: Exception) {
            // Keep local data
        }
    }
    
    // Vehicles
    override fun getAllActiveVehicles(): Flow<List<Vehicle>> = 
        vehicleDao.getAllActiveVehicles().map { VehicleMapper.toDomainList(it) }
    
    override suspend fun saveVehicle(vehicle: Vehicle) {
        vehicleDao.insertVehicle(VehicleMapper.toDto(vehicle))
        try {
            firestoreService.saveVehicle(vehicle)
        } catch (e: Exception) {
            // Will be synced later
        }
    }
    
    override suspend fun syncVehicles() {
        try {
            val remoteVehicles = firestoreService.getVehicles()
            vehicleDao.insertVehicles(VehicleMapper.toDtoList(remoteVehicles))
        } catch (e: Exception) {
            // Keep local data
        }
    }
    
    // Expenses - Offline-first approach
    override fun getAllExpenses(): Flow<List<Expense>> = 
        expenseDao.getAllExpenses().map { ExpenseMapper.toDomainList(it) }
    
    override fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> = 
        expenseDao.getExpensesByDateRange(startDate, endDate).map { ExpenseMapper.toDomainList(it) }
    
    override fun getExpenseById(id: String): Flow<Expense?> = flow {
        val dto = expenseDao.getExpenseById(id)
        emit(dto?.let { ExpenseMapper.toDomain(it) })
    }
    
    override suspend fun saveExpense(expense: Expense, photoUri: Uri?, photoUris: List<Uri>) {
        val expenseToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "${expense.id}_${System.currentTimeMillis()}")
                    }
                    expense.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    expense.copy(isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, expense.id))
                    expense.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    expense.copy(isSynced = false)
                }
            }
            else -> expense
        }
        
        // Always save locally first
        expenseDao.insertExpense(ExpenseMapper.toDto(expenseToSave))
        
        // Try to sync to Firestore
        if (expenseToSave.isSynced || (photoUri == null && photoUris.isEmpty())) {
            try {
                Log.d(TAG, "Attempting to save expense to Firestore: ${expenseToSave.id}")
                firestoreService.saveExpense(expenseToSave.copy(isSynced = true))
                expenseDao.markAsSynced(expenseToSave.id)
                Log.d(TAG, "Successfully saved expense to Firestore: ${expenseToSave.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save expense to Firestore: ${expenseToSave.id}", e)
                // Will be synced later by WorkManager
            }
        }
    }
    
    override suspend fun deleteExpense(expenseId: String) {
        val expense = expenseDao.getExpenseById(expenseId)
        if (expense != null) {
            expenseDao.deleteExpense(expense)
            try {
                firestoreService.deleteExpense(expenseId)
            } catch (e: Exception) {
                // Ignore remote delete errors
            }
        }
    }
    
    override suspend fun getTotalExpensesForPeriod(startDate: Date, endDate: Date): Double {
        return expenseDao.getTotalExpensesForPeriod(startDate, endDate)
    }
    
    override suspend fun syncExpenses() {
        val unsyncedExpenses = expenseDao.getUnsyncedExpenses()
        unsyncedExpenses.forEach { expenseDto ->
            try {
                val expense = ExpenseMapper.toDomain(expenseDto)
                val syncedExpense = expense.copy(isSynced = true)
                
                firestoreService.saveExpense(syncedExpense)
                expenseDao.updateExpense(ExpenseMapper.toDto(syncedExpense))
            } catch (e: Exception) {
                // Keep as unsynced for next attempt
            }
        }
        
        try {
            val remoteExpenses = firestoreService.getExpenses()
            expenseDao.insertExpenses(ExpenseMapper.toDtoList(remoteExpenses))
        } catch (e: Exception) {
            // Keep local data
        }
    }
}