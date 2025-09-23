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
import com.fleetmanager.domain.model.ExpenseTypeItem
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.data.dto.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    
    // Get user profile from Firestore
    fun getUserProfile(userId: String): Flow<UserDto> {
        return getCollection("users")
            .document(userId)
            .snapshots()
            .map { document ->
                if (document.exists()) {
                    UserDto(
                        id = document.id,
                        name = document.getString("displayName") ?: document.getString("name") ?: "Unknown User",
                        role = try {
                            UserRole.valueOf((document.getString("role") ?: "DRIVER").uppercase())
                        } catch (e: Exception) {
                            UserRole.DRIVER
                        }
                    )
                } else {
                    // Default user profile if document doesn't exist
                    UserDto(
                        id = userId,
                        name = "Unknown User",
                        role = UserRole.DRIVER
                    )
                }
            }
    }
    
    // Get current user's profile
    fun getCurrentUserProfile(): Flow<UserDto> {
        val userId = authService.getCurrentUserId() ?: ""
        return getUserProfile(userId)
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
    
    // Create user document if it doesn't exist (called on first sign-in)
    suspend fun saveUserIfMissing() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found")
            return
        }
        
        val userId = currentUser.uid
        Log.d(TAG, "Checking user document for: $userId")
        
        try {
            val userDoc = getCollection("users").document(userId).get().await()
            
            if (!userDoc.exists()) {
                Log.d(TAG, "Creating new user document for: $userId")
                
                val userData = mapOf(
                    "id" to userId,
                    "name" to (currentUser.displayName ?: "Unknown User"),
                    "role" to UserRole.DRIVER.name,
                    "email" to (currentUser.email ?: ""),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                getCollection("users")
                    .document(userId)
                    .set(userData)
                    .await()
                
                Log.d(TAG, "✅ User document created successfully for: $userId")
                toastHelper.showMessage(context, "✅ User profile created")
            } else {
                Log.d(TAG, "User document already exists for: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create user document: ${e.message}", e)
            toastHelper.showError(context, "❌ Failed to create user: ${e.message}")
            throw e
        }
    }
    
    // Daily Entries
    suspend fun saveDailyEntry(entry: DailyEntry) {
        val currentUserId = requireAuth()
        val targetUserId = entry.userId.takeIf { it.isNotBlank() } ?: currentUserId
        Log.d(TAG, "Saving daily entry to Firestore for user $targetUserId: ${entry.id}")
        try {
            // Add userId field to the entry
            val entryWithUserId = entry.copy(userId = targetUserId)
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
    
    suspend fun getDailyEntryById(entryId: String): DailyEntry? {
        return try {
            val userId = requireAuth()
            val userRole = getCurrentUserRole()
            
            val document = getCollection("entries")
                .document(entryId)
                .get()
                .await()
            
            val entry = document.toObject<DailyEntry>()
            
            // Check if user has permission to view this entry
            if (entry != null) {
                if (PermissionManager.canViewAll(userRole) || entry.userId == userId) {
                    entry
                } else {
                    Log.w(TAG, "User $userId does not have permission to view entry $entryId")
                    null
                }
            } else {
                Log.w(TAG, "Entry not found: $entryId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get daily entry by ID: ${e.message}", e)
            null
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
        val currentUserId = requireAuth()
        try {
            // Preserve the original owner when present to keep driver ↔ user linkage intact
            val ownerId = driver.userId.takeIf { it.isNotBlank() }
                ?: currentUserId
            val driverWithOwner = driver.copy(userId = ownerId)

            getCollection("drivers")
                .document(driver.id)
                .set(driverWithOwner)
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
        val userRole = getCurrentUserRole()

        val query = if (PermissionManager.canViewAllDriverData(userRole)) {
            getCollection("drivers")
        } else {
            getCollection("drivers").whereEqualTo("userId", userId)
        }

        return query
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Driver>() }
    }

    fun getDriversFlow(): Flow<List<Driver>> = flow {
        val userId = requireAuth()
        val userRole = getCurrentUserRole()

        val query = if (PermissionManager.canViewAllDriverData(userRole)) {
            getCollection("drivers")
        } else {
            getCollection("drivers").whereEqualTo("userId", userId)
        }

        emitAll(
            query.snapshots().map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject<Driver>()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse driver document: ${document.id}", e)
                        null
                    }
                }
            }
        )
    }

    suspend fun deleteDriver(driverId: String) {
        try {
            getCollection("drivers")
                .document(driverId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted driver: $driverId")
        } catch (e: Exception) {
            val errorMessage = "Failed to delete driver: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    // Vehicles
    suspend fun saveVehicle(vehicle: Vehicle) {
        val currentUserId = requireAuth()
        try {
            // Preserve original ownership when present (drivers may create their own vehicles)
            val ownerId = vehicle.userId.takeIf { it.isNotBlank() }
                ?: currentUserId
            val vehicleWithOwner = vehicle.copy(userId = ownerId)

            getCollection("vehicles")
                .document(vehicle.id)
                .set(vehicleWithOwner)
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
        val userRole = getCurrentUserRole()

        val query = if (PermissionManager.canViewAllVehicleData(userRole)) {
            getCollection("vehicles")
        } else {
            getCollection("vehicles").whereEqualTo("userId", userId)
        }

        return query
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Vehicle>() }
    }

    suspend fun deleteVehicle(vehicleId: String) {
        try {
            getCollection("vehicles")
                .document(vehicleId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted vehicle: $vehicleId")
        } catch (e: Exception) {
            val errorMessage = "Failed to delete vehicle: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
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
        val currentUserId = requireAuth()
        val targetUserId = expense.userId.takeIf { it.isNotBlank() } ?: currentUserId
        Log.d(TAG, "Saving expense to Firestore for user $targetUserId: ${expense.id}")
        try {
            // Add userId field to the expense
            val expenseWithUserId = expense.copy(userId = targetUserId)
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

    suspend fun getExpenseById(expenseId: String): Expense? {
        return try {
            val userId = requireAuth()
            val userRole = getCurrentUserRole()

            val document = getCollection("expenses")
                .document(expenseId)
                .get()
                .await()

            val expense = document.toObject<Expense>()

            if (expense != null) {
                if (PermissionManager.canViewAll(userRole) || expense.userId == userId) {
                    expense
                } else {
                    Log.w(TAG, "User $userId does not have permission to view expense $expenseId")
                    null
                }
            } else {
                Log.w(TAG, "Expense not found: $expenseId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get expense by ID: ${e.message}", e)
            null
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
    
    // ============== NEW COLLECTIONS FOR REPORTS ==============
    
    // Vehicles Collection (Global - shared across all users)
    suspend fun saveVehicleToCollection(vehicle: Vehicle) {
        try {
            getCollection("vehicles")
                .document(vehicle.id)
                .set(vehicle)
                .await()
            Log.d(TAG, "Successfully saved vehicle to collection: ${vehicle.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save vehicle: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getVehiclesFromCollection(): List<Vehicle> {
        return try {
            getCollection("vehicles")
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<Vehicle>() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch vehicles: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getVehiclesFromCollectionFlow(): Flow<List<Vehicle>> {
        return getCollection("vehicles")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<Vehicle>() }
            }
    }
    
    // Expense Types Collection (Global - shared across all users)
    suspend fun saveExpenseType(expenseType: ExpenseTypeItem) {
        try {
            getCollection("expenseTypes")
                .document(expenseType.id)
                .set(expenseType)
                .await()
            Log.d(TAG, "Successfully saved expense type: ${expenseType.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save expense type: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getExpenseTypes(): List<ExpenseTypeItem> {
        return try {
            getCollection("expenseTypes")
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<ExpenseTypeItem>() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expense types: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getExpenseTypesFlow(): Flow<List<ExpenseTypeItem>> {
        return getCollection("expenseTypes")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<ExpenseTypeItem>() }
            }
    }
    
    // Users Collection - Get drivers for reports
    suspend fun getDriverUsers(): List<UserDto> {
        return try {
            getCollection("users")
                .whereEqualTo("role", UserRole.DRIVER.name)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("displayName") ?: document.getString("fullName") ?: "Unknown Driver",
                            role = UserRole.DRIVER
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse driver user: ${document.id}", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch driver users: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getDriverUsersFlow(): Flow<List<UserDto>> {
        return getCollection("users")
            .whereEqualTo("role", UserRole.DRIVER.name)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    try {
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("displayName") ?: document.getString("fullName") ?: "Unknown Driver",
                            role = UserRole.DRIVER
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse driver user: ${document.id}", e)
                        null
                    }
                }
            }
    }
    
    // Admin-only methods for creating new items
    suspend fun createDriverUser(name: String, email: String = ""): UserDto {
        val userRole = getCurrentUserRole()
        if (!PermissionManager.canEdit(userRole)) {
            throw SecurityException("Only admins can create driver users")
        }
        
        val driverId = java.util.UUID.randomUUID().toString()
        val userData = mapOf(
            "id" to driverId,
            "name" to name,
            "role" to UserRole.DRIVER.name,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        getCollection("users")
            .document(driverId)
            .set(userData)
            .await()
        
        return UserDto(
            id = driverId,
            name = name,
            role = UserRole.DRIVER
        )
    }
    
    suspend fun createVehicle(make: String, model: String, year: Int, licensePlate: String): Vehicle {
        val userRole = getCurrentUserRole()
        if (!PermissionManager.canEdit(userRole)) {
            throw SecurityException("Only admins can create vehicles")
        }
        
        val vehicleId = java.util.UUID.randomUUID().toString()
        val vehicle = Vehicle(
            id = vehicleId,
            make = make,
            model = model,
            year = year,
            licensePlate = licensePlate,
            isActive = true
        )
        
        saveVehicleToCollection(vehicle)
        return vehicle
    }
    
    suspend fun createExpenseType(name: String, displayName: String): ExpenseTypeItem {
        val userRole = getCurrentUserRole()
        if (!PermissionManager.canEdit(userRole)) {
            throw SecurityException("Only admins can create expense types")
        }
        
        val expenseTypeId = java.util.UUID.randomUUID().toString()
        val expenseType = ExpenseTypeItem(
            id = expenseTypeId,
            name = name.uppercase().replace(" ", "_"),
            displayName = displayName,
            isActive = true
        )
        
        saveExpenseType(expenseType)
        return expenseType
    }
    
    // Initialize default data if collections are empty
    suspend fun initializeDefaultData() {
        try {
            // Check if expense types exist, if not create defaults
            val existingExpenseTypes = getExpenseTypes()
            if (existingExpenseTypes.isEmpty()) {
                Log.d(TAG, "Initializing default expense types...")
                val defaultExpenseTypes = listOf(
                    ExpenseTypeItem(
                        id = "fuel",
                        name = "FUEL",
                        displayName = "Fuel",
                        isActive = true
                    ),
                    ExpenseTypeItem(
                        id = "maintenance",
                        name = "MAINTENANCE",
                        displayName = "Maintenance",
                        isActive = true
                    ),
                    ExpenseTypeItem(
                        id = "service",
                        name = "SERVICE",
                        displayName = "Service",
                        isActive = true
                    ),
                    ExpenseTypeItem(
                        id = "car_wash",
                        name = "CAR_WASH",
                        displayName = "Car Wash",
                        isActive = true
                    ),
                    ExpenseTypeItem(
                        id = "fine",
                        name = "FINE",
                        displayName = "Fine",
                        isActive = true
                    ),
                    ExpenseTypeItem(
                        id = "other",
                        name = "OTHER",
                        displayName = "Other",
                        isActive = true
                    )
                )
                
                defaultExpenseTypes.forEach { expenseType ->
                    saveExpenseType(expenseType)
                }
                Log.d(TAG, "✅ Default expense types initialized")
            }
            
            // Check if vehicles exist, if not create sample vehicles
            val existingVehicles = getVehiclesFromCollection()
            if (existingVehicles.isEmpty()) {
                Log.d(TAG, "Initializing sample vehicles...")
                val sampleVehicles = listOf(
                    Vehicle(
                        id = "vehicle_1",
                        make = "Toyota",
                        model = "Camry",
                        year = 2020,
                        licensePlate = "ABC-123",
                        isActive = true
                    ),
                    Vehicle(
                        id = "vehicle_2",
                        make = "Honda",
                        model = "Civic",
                        year = 2019,
                        licensePlate = "XYZ-789",
                        isActive = true
                    ),
                    Vehicle(
                        id = "vehicle_3",
                        make = "Mitsubishi",
                        model = "Outlander",
                        year = 2021,
                        licensePlate = "DEF-456",
                        isActive = true
                    )
                )
                
                sampleVehicles.forEach { vehicle ->
                    saveVehicleToCollection(vehicle)
                }
                Log.d(TAG, "✅ Sample vehicles initialized")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default data: ${e.message}", e)
        }
    }
}