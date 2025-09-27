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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val expensesState = MutableStateFlow<List<Expense>>(emptyList())

    init {
        observeLocalExpenses()
        observeRemoteExpenses()
    }
    
    // Daily Entries - Offline-first approach
    override fun getAllDailyEntries(): Flow<List<DailyEntry>> = 
        dailyEntryDao.getAllEntries().map { DailyEntryMapper.toDomainList(it) }
    
    override fun getAllDailyEntriesRealtime(): Flow<List<DailyEntry>> = 
        firestoreService.getDailyEntriesFlow()
    
    override fun getDailyEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntry>> = 
        dailyEntryDao.getEntriesByDateRange(startDate, endDate).map { DailyEntryMapper.toDomainList(it) }
    
    override fun getDailyEntryById(id: String): Flow<DailyEntry?> = flow {
        try {
            // First try to get from local database
            dailyEntryDao.getEntryByIdFlow(id).map { dto ->
                dto?.let { DailyEntryMapper.toDomain(it) }
            }.collect { localEntry ->
                if (localEntry != null) {
                    emit(localEntry)
                } else {
                    // If not found locally, try to fetch from Firestore
                    try {
                        val remoteEntry = firestoreService.getDailyEntryById(id)
                        if (remoteEntry != null) {
                            // Cache it locally for future use
                            dailyEntryDao.insertEntry(DailyEntryMapper.toDto(remoteEntry))
                            emit(remoteEntry)
                        } else {
                            emit(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch entry from Firestore: AED{e.message}", e)
                        emit(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry by ID: AED{e.message}", e)
            emit(null)
        }
    }
    
    override suspend fun saveDailyEntry(entry: DailyEntry, photoUri: Uri?, photoUris: List<Uri>) {
        val userId = authService.getCurrentUserId() ?: ""
        val existingOwnerId = entry.userId.takeIf { it.isNotBlank() } ?: userId
        val entryToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "AED{entry.id}_AED{System.currentTimeMillis()}")
                    }
                    entry.copy(userId = existingOwnerId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(userId = existingOwnerId, isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, entry.id))
                    entry.copy(userId = existingOwnerId, photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    entry.copy(userId = existingOwnerId, isSynced = false)
                }
            }
            else -> entry.copy(userId = existingOwnerId)
        }
        
        // Always save locally first
        dailyEntryDao.insertEntry(DailyEntryMapper.toDto(entryToSave))
        
        // Try to sync to Firestore
        if (entryToSave.isSynced || (photoUri == null && photoUris.isEmpty())) {
            try {
                Log.d(TAG, "Attempting to save daily entry to Firestore: AED{entryToSave.id}")
                firestoreService.saveDailyEntry(entryToSave.copy(isSynced = true))
                dailyEntryDao.markAsSynced(entryToSave.id)
                Log.d(TAG, "Successfully saved daily entry to Firestore: AED{entryToSave.id}")
            } catch (e: Exception) {
                val errorMessage = "Failed to save daily entry to Firestore: AED{e.message}"
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
                val errorMessage = "Failed to sync daily entry AED{entryDto.id}: AED{e.message}"
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
        val currentUserId = authService.getCurrentUserId() ?: ""
        val ownerId = driver.userId.takeIf { it.isNotBlank() } ?: currentUserId
        val driverToPersist = driver.copy(userId = ownerId)
        driverDao.insertDriver(DriverMapper.toDto(driverToPersist))
        try {
            firestoreService.saveDriver(driverToPersist)
        } catch (e: Exception) {
            val errorMessage = "Failed to save driver to Firestore: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            // Will be synced later
        }
    }

    override suspend fun deleteDriver(driverId: String) {
        val driverDto = driverDao.getDriverById(driverId)

        try {
            firestoreService.deleteDriver(driverId)

            if (driverDto != null) {
                driverDao.deleteDriver(driverDto)
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to delete driver from Firestore: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw RuntimeException(errorMessage, e)
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

    override fun getAllVehicles(): Flow<List<Vehicle>> =
        vehicleDao.getAllVehicles().map { VehicleMapper.toDomainList(it) }

    override suspend fun saveVehicle(vehicle: Vehicle) {
        val currentUserId = authService.getCurrentUserId() ?: ""
        val ownerId = vehicle.userId.takeIf { it.isNotBlank() } ?: currentUserId
        val vehicleToPersist = vehicle.copy(userId = ownerId)
        vehicleDao.insertVehicle(VehicleMapper.toDto(vehicleToPersist))
        try {
            firestoreService.saveVehicle(vehicleToPersist)
        } catch (e: Exception) {
            val errorMessage = "Failed to save vehicle to Firestore: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            // Will be synced later
        }
    }

    override suspend fun deleteVehicle(vehicleId: String) {
        val vehicleDto = vehicleDao.getVehicleById(vehicleId)

        try {
            firestoreService.deleteVehicle(vehicleId)

            if (vehicleDto != null) {
                vehicleDao.deleteVehicle(vehicleDto)
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to delete vehicle from Firestore: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw RuntimeException(errorMessage, e)
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
    override fun getAllExpenses(): Flow<List<Expense>> = expensesState.asStateFlow()

    override fun getAllExpensesRealtime(): Flow<List<Expense>> = expensesState.asStateFlow()
    
    override fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> = 
        expenseDao.getExpensesByDateRange(startDate, endDate).map { ExpenseMapper.toDomainList(it) }
    
    override fun getExpenseById(id: String): Flow<Expense?> = flow {
        try {
            val localExpense = expenseDao.getExpenseById(id)
            if (localExpense != null) {
                emit(ExpenseMapper.toDomain(localExpense))
            } else {
                val remoteExpense = firestoreService.getExpenseById(id)?.ensureDriverIdentity()
                if (remoteExpense != null) {
                    expenseDao.insertExpense(ExpenseMapper.toDto(remoteExpense))
                }
                emit(remoteExpense)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting expense by ID: AED{e.message}", e)
            emit(null)
        }
    }

    private fun observeLocalExpenses() {
        repositoryScope.launch {
            expenseDao.getAllExpenses()
                .map { ExpenseMapper.toDomainList(it) }
                .collect { expenses ->
                    expensesState.value = expenses
                }
        }
    }

    private fun observeRemoteExpenses() {
        repositoryScope.launch {
            try {
                val userRole = firestoreService.getCurrentUserRole()
                firestoreService.getExpensesFlowForRole(userRole).collect { remoteExpenses ->
                    val syncedExpenses = remoteExpenses
                        .map { it.ensureDriverIdentity().copy(isSynced = true) }
                    if (syncedExpenses.isEmpty()) {
                        expenseDao.deleteAllSynced()
                    } else {
                        expenseDao.deleteSyncedNotIn(syncedExpenses.map { it.id })
                        expenseDao.insertExpenses(ExpenseMapper.toDtoList(syncedExpenses))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to observe remote expenses: AED{e.message}", e)
            }
        }
    }
    
    override suspend fun saveExpense(expense: Expense, photoUri: Uri?, photoUris: List<Uri>) {
        val currentUserId = authService.getCurrentUserId() ?: ""
        val resolvedDriverId = expense.driverId
            .takeIf { it.isNotBlank() }
            ?: expense.userId.takeIf { it.isNotBlank() }
            ?: currentUserId

        val baseExpense = expense.copy(
            driverId = resolvedDriverId,
            userId = resolvedDriverId
        )

        val expenseToSave = when {
            photoUris.isNotEmpty() -> {
                try {
                    val photoUrls = photoUris.map { uri ->
                        storageService.uploadPhoto(uri, "AED{expense.id}_AED{System.currentTimeMillis()}")
                    }
                    baseExpense.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    baseExpense.copy(isSynced = false)
                }
            }
            photoUri != null -> {
                try {
                    val photoUrls = listOf(storageService.uploadPhoto(photoUri, expense.id))
                    baseExpense.copy(photoUrls = photoUrls, isSynced = true)
                } catch (e: Exception) {
                    // Save locally if upload fails
                    baseExpense.copy(isSynced = false)
                }
            }
            else -> baseExpense
        }.ensureDriverIdentity(resolvedDriverId)

        // Always save locally first
        expenseDao.insertExpense(ExpenseMapper.toDto(expenseToSave))

        // Try to sync to Firestore
        if (expenseToSave.isSynced || (photoUri == null && photoUris.isEmpty())) {
            try {
                Log.d(TAG, "Attempting to save expense to Firestore: AED{expenseToSave.id}")
                firestoreService.saveExpense(expenseToSave.copy(isSynced = true))
                expenseDao.markAsSynced(expenseToSave.id)
                Log.d(TAG, "Successfully saved expense to Firestore: AED{expenseToSave.id}")
            } catch (e: Exception) {
                val errorMessage = "Failed to save expense to Firestore: AED{e.message}"
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
        }
        try {
            firestoreService.deleteExpense(expenseId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete expense from Firestore: AED{e.message}", e)
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
                val syncedExpense = expense.ensureDriverIdentity().copy(isSynced = true)

                firestoreService.saveExpense(syncedExpense)
                expenseDao.updateExpense(ExpenseMapper.toDto(syncedExpense))
            } catch (e: Exception) {
                val errorMessage = "Failed to sync expense AED{expenseDto.id}: AED{e.message}"
                Log.e(TAG, errorMessage, e)
                toastHelper.showError(context, errorMessage)
                // Keep as unsynced for next attempt
            }
        }
        
        try {
            val remoteExpenses = firestoreService.getExpenses().map { it.ensureDriverIdentity() }
            expenseDao.insertExpenses(ExpenseMapper.toDtoList(remoteExpenses))
        } catch (e: Exception) {
            // Keep local data
        }
    }
}

private fun Expense.ensureDriverIdentity(defaultId: String = ""): Expense {
    val resolvedDriverId = when {
        driverId.isNotBlank() -> driverId
        userId.isNotBlank() -> userId
        defaultId.isNotBlank() -> defaultId
        else -> ""
    }
    val resolvedUserId = when {
        userId.isNotBlank() -> userId
        resolvedDriverId.isNotBlank() -> resolvedDriverId
        defaultId.isNotBlank() -> defaultId
        else -> ""
    }

    return copy(
        driverId = resolvedDriverId,
        userId = resolvedUserId
    )
}