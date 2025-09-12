package com.fleetmanager.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.fleetmanager.auth.AuthService
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {
    
    companion object {
        private const val TAG = "FirestoreService"
    }
    
    private fun getCollection(collection: String) = firestore.collection(collection)
    
    private fun requireAuth(): String {
        val userId = authService.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            Log.e(TAG, "User not authenticated! Cannot perform Firestore operations.")
            throw IllegalStateException("User must be authenticated to access Firestore")
        }
        Log.d(TAG, "User authenticated with ID: $userId")
        return userId
    }
    
    // Simple method to get current user's role (default to DRIVER if not found)
    suspend fun getCurrentUserRole(): UserRole {
        val userId = authService.getCurrentUserId() ?: return UserRole.DRIVER
        return try {
            val userDoc = getCollection("users").document(userId).get().await()
            val roleString = userDoc.getString("role") ?: "DRIVER"
            UserRole.valueOf(roleString.uppercase())
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch user role, defaulting to DRIVER", e)
            UserRole.DRIVER
        }
    }
    
    // Daily Entries
    suspend fun saveDailyEntry(entry: DailyEntry) {
        val userId = requireAuth()
        Log.d(TAG, "Saving daily entry to Firestore for user $userId: ${entry.id}")
        try {
            // Add userId field to the entry
            val entryWithUserId = entry.copy(userId = userId)
            getCollection("entries")
                .document(entry.id)
                .set(entryWithUserId)
                .await()
            Log.d(TAG, "Successfully saved daily entry to Firestore: ${entry.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save daily entry: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getDailyEntries(): List<DailyEntry> {
        val userId = requireAuth()
        val userRole = getCurrentUserRole()
        
        return if (PermissionManager.canViewAll(userRole)) {
            // Managers and Admins can see all entries
            getCollection("entries")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<DailyEntry>() }
        } else {
            // Drivers can only see their own entries
            getCollection("entries")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<DailyEntry>() }
        }
    }
    
    fun getDailyEntriesFlow(): Flow<List<DailyEntry>> {
        val userId = authService.getCurrentUserId() ?: ""
        return getCollection("entries")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<DailyEntry>() }
            }
    }
    
    // Role-based flow for entries
    fun getDailyEntriesFlowForRole(userRole: UserRole): Flow<List<DailyEntry>> {
        val userId = authService.getCurrentUserId() ?: ""
        
        return if (PermissionManager.canViewAll(userRole)) {
            // Managers and Admins can see all entries
            getCollection("entries")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toObject<DailyEntry>() }
                }
        } else {
            // Drivers can only see their own entries
            getCollection("entries")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toObject<DailyEntry>() }
                }
        }
    }
    
    
    suspend fun deleteDailyEntry(entryId: String) {
        getCollection("entries")
            .document(entryId)
            .delete()
            .await()
    }
    
    // Drivers
    suspend fun saveDriver(driver: Driver) {
        val userId = requireAuth()
        try {
            // Add userId field to the driver
            val driverWithUserId = driver.copy(userId = userId)
            getCollection("drivers")
                .document(driver.id)
                .set(driverWithUserId)
                .await()
            Log.d(TAG, "Successfully saved driver to Firestore: ${driver.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save driver: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getDrivers(): List<Driver> {
        val userId = requireAuth()
        return getCollection("drivers")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Driver>() }
    }
    
    // Vehicles
    suspend fun saveVehicle(vehicle: Vehicle) {
        val userId = requireAuth()
        try {
            // Add userId field to the vehicle
            val vehicleWithUserId = vehicle.copy(userId = userId)
            getCollection("vehicles")
                .document(vehicle.id)
                .set(vehicleWithUserId)
                .await()
            Log.d(TAG, "Successfully saved vehicle to Firestore: ${vehicle.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save vehicle: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getVehicles(): List<Vehicle> {
        val userId = requireAuth()
        return getCollection("vehicles")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Vehicle>() }
    }
    
    // Expenses
    /**
     * Saves an expense to Firebase Firestore.
     * 
     * FIREBASE SETUP REQUIRED:
     * ========================
     * 
     * 1. ENABLE FIRESTORE DATABASE:
     *    - Go to Firebase Console (https://console.firebase.google.com)
     *    - Select your project: "ag-motion" 
     *    - Navigate to "Firestore Database" in the left sidebar
     *    - Click "Create database"
     *    - Choose "Start in production mode" (recommended) or "test mode"
     *    - Select a location (choose closest to your users)
     * 
     * 2. FIRESTORE SECURITY RULES:
     *    Replace the default rules with these rules in the Firebase Console > Firestore Database > Rules:
     *    
     *    ```
     *    rules_version = '2';
     *    service cloud.firestore {
     *      match /databases/{database}/documents {
     *        // Flat collections with userId filtering
     *        match /entries/{entryId} {
     *          allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
     *        }
     *        
     *        match /expenses/{expenseId} {
     *          allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
     *        }
     *        
     *        match /drivers/{driverId} {
     *          allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
     *        }
     *        
     *        match /vehicles/{vehicleId} {
     *          allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
     *        }
     *      }
     *    }
     *    ```
     * 
     * 3. FIRESTORE COLLECTION STRUCTURE:
     *    The app will automatically create these flat collections:
     *    - entries/{entryId}
     *      - Fields: id, userId, date, driver, vehicle, uberEarnings, yangoEarnings, privateJobsEarnings, notes, photos, createdAt, updatedAt
     *    - expenses/{expenseId}
     *      - Fields: id, userId, type, amount, date, driver, car, notes, photos, createdAt, updatedAt
     *    - drivers/{driverId}
     *      - Fields: id, userId, name, isActive
     *    - vehicles/{vehicleId}
     *      - Fields: id, userId, make, model, year, licensePlate, isActive
     * 
     * 4. REQUIRED FIREBASE SERVICES:
     *    ✅ Authentication (already configured)
     *    ✅ Firestore Database (enable this)
     *    ✅ Cloud Storage (for photo uploads, already configured)
     * 
     * 5. VERIFY CONFIGURATION:
     *    - Ensure google-services.json is in app/ directory ✅
     *    - Firebase dependencies are in build.gradle.kts ✅
     *    - User must be signed in for Firestore operations to work
     * 
     * 6. DATA STRUCTURE EXAMPLE:
     *    Each expense document will contain:
     *    {
     *      "id": "uuid-string",
     *      "userId": "firebase-user-id",
     *      "type": "FUEL" | "MAINTENANCE" | "SERVICE" | "CAR_WASH" | "FINE" | "OTHER",
     *      "amount": 50.75,
     *      "date": Timestamp,
     *      "driver": "Ahmed",
     *      "car": "Mitsubishi Outlander 1", 
     *      "notes": "Fill up tank",
     *      "photos": ["https://storage.googleapis.com/..."],
     *      "createdAt": Timestamp,
     *      "updatedAt": Timestamp,
     *      "isSynced": true
     *    }
     */
    suspend fun saveExpense(expense: Expense) {
        val userId = requireAuth()
        Log.d(TAG, "Saving expense to Firestore for user $userId: ${expense.id}")
        try {
            // Add userId field to the expense
            val expenseWithUserId = expense.copy(userId = userId)
            getCollection("expenses")
                .document(expense.id)
                .set(expenseWithUserId)
                .await()
            Log.d(TAG, "Successfully saved expense to Firestore: ${expense.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save expense: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getExpenses(): List<Expense> {
        val userId = requireAuth()
        val userRole = getCurrentUserRole()
        
        return if (PermissionManager.canViewAll(userRole)) {
            // Managers and Admins can see all expenses
            getCollection("expenses")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<Expense>() }
        } else {
            // Drivers can only see their own expenses
            getCollection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<Expense>() }
        }
    }
    
    fun getExpensesFlow(): Flow<List<Expense>> {
        val userId = authService.getCurrentUserId() ?: ""
        return getCollection("expenses")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<Expense>() }
            }
    }
    
    // Role-based flow for expenses
    fun getExpensesFlowForRole(userRole: UserRole): Flow<List<Expense>> {
        val userId = authService.getCurrentUserId() ?: ""
        
        return if (PermissionManager.canViewAll(userRole)) {
            // Managers and Admins can see all expenses
            getCollection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toObject<Expense>() }
                }
        } else {
            // Drivers can only see their own expenses
            getCollection("expenses")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toObject<Expense>() }
                }
        }
    }
    
    
    suspend fun deleteExpense(expenseId: String) {
        getCollection("expenses")
            .document(expenseId)
            .delete()
            .await()
    }
}