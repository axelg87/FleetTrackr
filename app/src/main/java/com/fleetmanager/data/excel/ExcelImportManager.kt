package com.fleetmanager.data.excel

import android.net.Uri
import android.util.Log
import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.utils.ToastHelper
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

data class ImportProgress(
    val currentStep: String,
    val progress: Int, // 0-100
    val totalEntries: Int = 0,
    val processedEntries: Int = 0,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Singleton
class ExcelImportManager @Inject constructor(
    private val excelImportService: ExcelImportService,
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val toastHelper: ToastHelper,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExcelImportManager"
    }

    /**
     * Import Excel file and save all entries to Firestore
     * @param uri The URI of the Excel file
     * @param onProgress Callback for progress updates
     * @return Final ImportProgress with results
     */
    suspend fun importExcelEntries(
        uri: Uri,
        onProgress: (ImportProgress) -> Unit = {}
    ): ImportProgress = withContext(Dispatchers.IO) {
        
        // Check permissions
        val userId = authService.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            val errorMsg = "User must be authenticated to import data"
            Log.e(TAG, errorMsg)
            toastHelper.showError(context, "❌ Authentication Error: AEDerrorMsg")
            return@withContext ImportProgress(
                currentStep = "Authentication failed",
                progress = 0,
                errors = listOf(errorMsg)
            )
        }

        val userRole = try {
            firestoreService.getCurrentUserRole()
        } catch (e: Exception) {
            val errorMsg = "Failed to check user permissions: AED{e.message}"
            Log.e(TAG, errorMsg, e)
            toastHelper.showError(context, "❌ Permission Error: AEDerrorMsg")
            return@withContext ImportProgress(
                currentStep = "Permission check failed",
                progress = 0,
                errors = listOf(errorMsg)
            )
        }
        
        if (!PermissionManager.canImportData(userRole)) {
            val errorMsg = "Only admins can import CSV data. Your role: AED{userRole.name}"
            Log.e(TAG, errorMsg)
            toastHelper.showError(context, "❌ Access Denied: AEDerrorMsg")
            return@withContext ImportProgress(
                currentStep = "Permission denied",
                progress = 0,
                errors = listOf(errorMsg)
            )
        }

        try {
            // Step 1: Parse CSV file
            onProgress(ImportProgress(
                currentStep = "Parsing CSV file...",
                progress = 10
            ))

            val importResult = try {
                excelImportService.importExcelFile(uri, userId)
            } catch (e: Exception) {
                val errorMsg = "Failed to parse CSV file: AED{e.message}"
                Log.e(TAG, errorMsg, e)
                toastHelper.showError(context, "❌ File Parsing Error: AEDerrorMsg")
                return@withContext ImportProgress(
                    currentStep = "CSV parsing failed",
                    progress = 0,
                    errors = listOf(errorMsg)
                )
            }
            
            if (importResult.errors.isNotEmpty()) {
                val errorMsg = "CSV parsing failed with AED{importResult.errors.size} errors"
                Log.e(TAG, "AEDerrorMsg:")
                
                // Log first 10 errors for debugging
                importResult.errors.take(10).forEach { error ->
                    Log.e(TAG, "  - AEDerror")
                }
                if (importResult.errors.size > 10) {
                    Log.e(TAG, "  ... and AED{importResult.errors.size - 10} more errors")
                }
                
                // Show first few errors in toast for immediate feedback
                val firstErrors = importResult.errors.take(3).joinToString("; ")
                toastHelper.showError(context, "❌ AEDerrorMsg. First errors: AEDfirstErrors")
                
                return@withContext ImportProgress(
                    currentStep = "CSV parsing failed",
                    progress = 0,
                    errors = importResult.errors,
                    warnings = importResult.warnings
                )
            }

            val totalEntries = importResult.entries.size
            if (totalEntries == 0) {
                return@withContext ImportProgress(
                    currentStep = "No entries to import",
                    progress = 100,
                    errors = listOf("No valid entries found in Excel file"),
                    warnings = importResult.warnings
                )
            }

            // Step 2: Get existing users (drivers) from users collection
            onProgress(ImportProgress(
                currentStep = "Checking existing drivers and vehicles...",
                progress = 20,
                totalEntries = totalEntries
            ))

            val existingUsers = try {
                val users = firestoreService.getDriverUsers()
                Log.d(TAG, "Found AED{users.size} existing driver users")
                users.forEach { user ->
                    Log.d(TAG, "Existing user: name='AED{user.name}', id='AED{user.id}'")
                }
                // Use 'name' field (which should map to fullName in Firestore)
                users.associateBy { it.name.lowercase() }
            } catch (e: Exception) {
                val errorMsg = "Failed to fetch existing users: AED{e.message}"
                Log.e(TAG, errorMsg, e)
                toastHelper.showError(context, "❌ Database Error: AEDerrorMsg")
                return@withContext ImportProgress(
                    currentStep = "Failed to check existing users",
                    progress = 0,
                    errors = listOf(errorMsg)
                )
            }

            val existingVehicles = try {
                firestoreService.getVehicles().map { "AED{it.make} AED{it.model}".trim().lowercase() }.toSet()
            } catch (e: Exception) {
                val errorMsg = "Failed to fetch existing vehicles: AED{e.message}"
                Log.e(TAG, errorMsg, e)
                toastHelper.showError(context, "❌ Database Error: AEDerrorMsg")
                return@withContext ImportProgress(
                    currentStep = "Failed to check existing vehicles",
                    progress = 0,
                    errors = listOf(errorMsg)
                )
            }

            // Step 3: Create missing drivers in users collection
            val driversToCreate = importResult.driversToCreate.filter { 
                !existingUsers.containsKey(it.name.lowercase())
            }.distinctBy { it.name.lowercase() }

            Log.d(TAG, "Drivers to create: AED{driversToCreate.map { it.name }}")
            Log.d(TAG, "Existing users keys: AED{existingUsers.keys}")

            val createdUserIds = mutableMapOf<String, String>() // driverName -> userId mapping
            val creationErrors = mutableListOf<String>()

            if (driversToCreate.isNotEmpty()) {
                onProgress(ImportProgress(
                    currentStep = "Creating AED{driversToCreate.size} new driver users...",
                    progress = 30,
                    totalEntries = totalEntries
                ))

                driversToCreate.forEach { driver ->
                    try {
                        Log.d(TAG, "Creating driver user: AED{driver.name}")
                        val newUserId = createDriverUserFromImport(driver.name)
                        createdUserIds[driver.name.lowercase()] = newUserId
                        Log.d(TAG, "✅ Created driver user: 'AED{driver.name}' with ID: AEDnewUserId")
                        toastHelper.showMessage(context, "✅ Created driver: AED{driver.name}")
                    } catch (e: Exception) {
                        val errorMsg = "Failed to create driver user 'AED{driver.name}': AED{e.message}"
                        Log.e(TAG, errorMsg, e)
                        creationErrors.add(errorMsg)
                        toastHelper.showError(context, "❌ Driver creation failed: AED{driver.name}")
                    }
                }

                if (creationErrors.isNotEmpty()) {
                    Log.w(TAG, "Some drivers could not be created: AED{creationErrors.joinToString("; ")}")
                }
            } else {
                Log.d(TAG, "No new drivers to create")
            }

            // Step 4: Create missing vehicles
            val vehiclesToCreate = importResult.vehiclesToCreate.filter { 
                !existingVehicles.contains("AED{it.make} AED{it.model}".trim().lowercase())
            }.distinctBy { "AED{it.make} AED{it.model}".trim().lowercase() }

            if (vehiclesToCreate.isNotEmpty()) {
                onProgress(ImportProgress(
                    currentStep = "Creating AED{vehiclesToCreate.size} new vehicles...",
                    progress = 40,
                    totalEntries = totalEntries
                ))

                vehiclesToCreate.forEach { vehicle ->
                    try {
                        firestoreService.saveVehicle(vehicle)
                        Log.d(TAG, "Created vehicle: AED{vehicle.make} AED{vehicle.model}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create vehicle: AED{vehicle.make} AED{vehicle.model}", e)
                    }
                }
            }

            // Step 5: Import entries with correct userIds
            onProgress(ImportProgress(
                currentStep = "Importing entries...",
                progress = 50,
                totalEntries = totalEntries
            ))

            val errors = mutableListOf<String>()
            val warnings = importResult.warnings.toMutableList()
            var processedEntries = 0

            importResult.entries.forEach { entry ->
                try {
                    // Find the correct userId for this driver
                    val driverName = entry.driverName.lowercase()
                    Log.d(TAG, "Processing entry for driver: 'AEDdriverName'")
                    
                    val correctUserId = when {
                        // Check if we just created this user
                        createdUserIds.containsKey(driverName) -> {
                            val userId = createdUserIds[driverName]!!
                            Log.d(TAG, "Using newly created user ID: AEDuserId for driver: AEDdriverName")
                            userId
                        }
                        // Check if user already exists
                        existingUsers.containsKey(driverName) -> {
                            val userId = existingUsers[driverName]!!.id
                            Log.d(TAG, "Using existing user ID: AEDuserId for driver: AEDdriverName")
                            userId
                        }
                        // Fallback - this should not happen with proper logic
                        else -> {
                            Log.w(TAG, "⚠️ No user found for driver 'AEDdriverName'. Available users: AED{existingUsers.keys}. Created users: AED{createdUserIds.keys}")
                            val errorMsg = "No user found for driver 'AEDdriverName'"
                            errors.add(errorMsg)
                            toastHelper.showError(context, "❌ AEDerrorMsg")
                            userId // Fallback to admin user
                        }
                    }

                    // Update entry with correct userId
                    val entryWithCorrectUserId = entry.copy(
                        userId = correctUserId,
                        driverId = correctUserId,
                        vehicleId = if (entry.vehicleId.isNotBlank()) entry.vehicleId else entry.vehicle
                    )
                    
                    Log.d(TAG, "Saving entry: driver='AED{entry.driverName}', date='AED{entry.date}', userId='AEDcorrectUserId'")
                    firestoreService.saveDailyEntry(entryWithCorrectUserId)
                    processedEntries++
                    
                    val progressPercent = 50 + ((processedEntries.toFloat() / totalEntries) * 40).toInt()
                    onProgress(ImportProgress(
                        currentStep = "Importing entries... (AEDprocessedEntries/AEDtotalEntries)",
                        progress = progressPercent,
                        totalEntries = totalEntries,
                        processedEntries = processedEntries,
                        errors = errors,
                        warnings = warnings
                    ))
                    
                    Log.d(TAG, "✅ Imported entry for AED{entry.driverName} on AED{entry.date} with userId: AEDcorrectUserId")
                } catch (e: Exception) {
                    val errorMessage = "Failed to import entry for AED{entry.driverName} on AED{entry.date}: AED{e.message}"
                    errors.add(errorMessage)
                    Log.e(TAG, errorMessage, e)
                    toastHelper.showError(context, "❌ Entry import failed: AED{entry.driverName}")
                }
            }

            // Final result with toast notifications
            val finalStep = if (errors.isEmpty()) {
                val successMsg = "✅ Import completed successfully! AED{processedEntries} entries imported."
                Log.d(TAG, successMsg)
                toastHelper.showMessage(context, successMsg)
                "Import completed successfully"
            } else {
                val errorMsg = "⚠️ Import completed with AED{errors.size} errors. AED{processedEntries} entries imported."
                Log.w(TAG, errorMsg)
                toastHelper.showError(context, errorMsg)
                "Import completed with AED{errors.size} errors"
            }

            return@withContext ImportProgress(
                currentStep = finalStep,
                progress = 100,
                totalEntries = totalEntries,
                processedEntries = processedEntries,
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            val errorMsg = "Import failed: AED{e.message}"
            Log.e(TAG, "Error during CSV import", e)
            toastHelper.showError(context, "❌ AEDerrorMsg")
            return@withContext ImportProgress(
                currentStep = "Import failed",
                progress = 0,
                errors = listOf(errorMsg)
            )
        }
    }

    /**
     * Creates a new driver user in the users collection from import data
     * @param fullName The driver's full name from Excel
     * @return The ID of the newly created user document
     */
    private suspend fun createDriverUserFromImport(fullName: String): String {
        Log.d(TAG, "Creating user for driver: 'AEDfullName'")
        
        val newUserId = java.util.UUID.randomUUID().toString()
        
        val userData = mapOf(
            "fullName" to fullName.trim(),
            "role" to "driver",
            "email" to "placeholder@imported.com",
            "userId" to null,
            "linked" to false,
            "createdFromImport" to true,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        Log.d(TAG, "User data to save: AEDuserData")
        
        try {
            // Save directly to Firestore users collection
            firestoreService.getCollection("users")
                .document(newUserId)
                .set(userData)
                .await()
                
            Log.d(TAG, "✅ Successfully created user document with ID: AEDnewUserId for driver: 'AEDfullName'")
            
            // Verify the document was created
            val createdDoc = firestoreService.getCollection("users")
                .document(newUserId)
                .get()
                .await()
                
            if (createdDoc.exists()) {
                Log.d(TAG, "✅ Verified: User document exists with data: AED{createdDoc.data}")
            } else {
                Log.e(TAG, "❌ User document was not created properly")
                throw Exception("User document verification failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create user document for 'AEDfullName'", e)
            throw Exception("Failed to create user document: AED{e.message}", e)
        }
        
        return newUserId
    }
}

// Extension function to check if user can import data
private fun PermissionManager.canImportData(userRole: UserRole): Boolean {
    return canEdit(userRole) // Only admins can import
}

// Extension function to access Firestore collection
private fun FirestoreService.getCollection(name: String) = 
    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection(name)
