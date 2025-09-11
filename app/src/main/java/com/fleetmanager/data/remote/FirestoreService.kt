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
    
    private fun getUserCollection(collection: String) = 
        firestore.collection("users")
            .document(authService.getCurrentUserId() ?: "")
            .collection(collection)
    
    private fun requireAuth(): String {
        val userId = authService.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            Log.e(TAG, "User not authenticated! Cannot perform Firestore operations.")
            throw IllegalStateException("User must be authenticated to access Firestore")
        }
        Log.d(TAG, "User authenticated with ID: $userId")
        return userId
    }
    
    // Daily Entries
    suspend fun saveDailyEntry(entry: DailyEntry) {
        val userId = requireAuth()
        Log.d(TAG, "Saving daily entry to Firestore for user $userId: ${entry.id}")
        try {
            getUserCollection("dailyEntries")
                .document(entry.id)
                .set(entry)
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
        return getUserCollection("dailyEntries")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<DailyEntry>() }
    }
    
    fun getDailyEntriesFlow(): Flow<List<DailyEntry>> {
        return getUserCollection("dailyEntries")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<DailyEntry>() }
            }
    }
    
    suspend fun deleteDailyEntry(entryId: String) {
        getUserCollection("dailyEntries")
            .document(entryId)
            .delete()
            .await()
    }
    
    // Drivers
    suspend fun saveDriver(driver: Driver) {
        try {
            getUserCollection("drivers")
                .document(driver.id)
                .set(driver)
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
        return getUserCollection("drivers")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Driver>() }
    }
    
    // Vehicles
    suspend fun saveVehicle(vehicle: Vehicle) {
        try {
            getUserCollection("vehicles")
                .document(vehicle.id)
                .set(vehicle)
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
        return getUserCollection("vehicles")
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
     *        // Users can only access their own data
     *        match /users/{userId}/{document=**} {
     *          allow read, write: if request.auth != null && request.auth.uid == userId;
     *        }
     *        
     *        // Expenses collection structure: users/{userId}/expenses/{expenseId}
     *        match /users/{userId}/expenses/{expenseId} {
     *          allow read, write: if request.auth != null && request.auth.uid == userId;
     *        }
     *      }
     *    }
     *    ```
     * 
     * 3. FIRESTORE COLLECTION STRUCTURE:
     *    The app will automatically create these collections:
     *    - users/{userId}/expenses/{expenseId}
     *      - Fields: id, type, amount, date, driver, car, notes, photos, createdAt, updatedAt
     *    - users/{userId}/dailyEntries/{entryId}
     *    - users/{userId}/drivers/{driverId}  
     *    - users/{userId}/vehicles/{vehicleId}
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
            getUserCollection("expenses")
                .document(expense.id)
                .set(expense)
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
        return getUserCollection("expenses")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Expense>() }
    }
    
    fun getExpensesFlow(): Flow<List<Expense>> {
        return getUserCollection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<Expense>() }
            }
    }
    
    suspend fun deleteExpense(expenseId: String) {
        getUserCollection("expenses")
            .document(expenseId)
            .delete()
            .await()
    }
}