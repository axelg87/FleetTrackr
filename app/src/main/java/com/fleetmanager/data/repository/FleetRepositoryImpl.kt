package com.fleetmanager.data.repository

import android.content.Context
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
import com.fleetmanager.auth.AuthService
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val storageService: StorageService,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) : FleetRepository {
    
    companion object {
        private const val TAG = "FleetRepositoryImpl"
    }
    
    // Daily Entries - Offline-first approach
    override fun getAllDailyEntries(): Flow<List<DailyEntry>> = 
        dailyEntryDao.getAllEntries().map { DailyEntryMapper.toDomainList(it) }
    
    override fun getAllDailyEntriesRealtime(): Flow<List<DailyEntry>> = 
        firestoreService.getDailyEntriesFlow()
    
    override fun getDailyEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntry>> = 
        dailyEntryDao.getEntriesByDateRange(startDate, endDate).map { DailyEntryMapper.toDomainList(it) }
    
    override fun getDailyEntryById(id: String): Flow<DailyEntry?> = flow {
        val dto = dailyEntryDao.getEntryById(id)
        emit(dto?.let { DailyEntryMapper.toDomain(it) })
    }
    
    override suspend fun saveDailyEntry(entry: DailyEntry, photoUri: Uri?, photoUris: List<Uri>) {
        val userId = authService.getCurrentUserId() ?: ""
        val entryToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "${entry.id}_${System.currentTimeMillis()}")
                    }
                    entry.copy(userId = userId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(userId = userId, isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, entry.id))
                    entry.copy(userId = userId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(userId = userId, isSynced = false)
                }
            }
            else -> entry.copy(userId = userId)
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
                val errorMessage = "Failed to save daily entry to Firestore: ${e.message}"
                Log.e(TAG, errorMessage, e)
                toastHelper.showError(context, errorMessage)
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
                val errorMessage = "Failed to sync daily entry ${entryDto.id}: ${e.message}"
                Log.e(TAG, errorMessage, e)
                toastHelper.showError(context, errorMessage)
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
        val userId = authService.getCurrentUserId() ?: ""
        val driverWithUserId = driver.copy(userId = userId)
        driverDao.insertDriver(DriverMapper.toDto(driverWithUserId))
        try {
            firestoreService.saveDriver(driverWithUserId)
        } catch (e: Exception) {
            val errorMessage = "Failed to save driver to Firestore: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
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
        val userId = authService.getCurrentUserId() ?: ""
        val vehicleWithUserId = vehicle.copy(userId = userId)
        vehicleDao.insertVehicle(VehicleMapper.toDto(vehicleWithUserId))
        try {
            firestoreService.saveVehicle(vehicleWithUserId)
        } catch (e: Exception) {
            val errorMessage = "Failed to save vehicle to Firestore: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
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
    
    override fun getAllExpensesRealtime(): Flow<List<Expense>> = 
        firestoreService.getExpensesFlow()
    
    override fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> = 
        expenseDao.getExpensesByDateRange(startDate, endDate).map { ExpenseMapper.toDomainList(it) }
    
    override fun getExpenseById(id: String): Flow<Expense?> = flow {
        val dto = expenseDao.getExpenseById(id)
        emit(dto?.let { ExpenseMapper.toDomain(it) })
    }
    
    override suspend fun saveExpense(expense: Expense, photoUri: Uri?, photoUris: List<Uri>) {
        val userId = authService.getCurrentUserId() ?: ""
        val expenseToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "${expense.id}_${System.currentTimeMillis()}")
                    }
                    expense.copy(userId = userId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    expense.copy(userId = userId, isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, expense.id))
                    expense.copy(userId = userId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    expense.copy(userId = userId, isSynced = false)
                }
            }
            else -> expense.copy(userId = userId)
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
                val errorMessage = "Failed to save expense to Firestore: ${e.message}"
                Log.e(TAG, errorMessage, e)
                toastHelper.showError(context, errorMessage)
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
                val errorMessage = "Failed to sync expense ${expenseDto.id}: ${e.message}"
                Log.e(TAG, errorMessage, e)
                toastHelper.showError(context, errorMessage)
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