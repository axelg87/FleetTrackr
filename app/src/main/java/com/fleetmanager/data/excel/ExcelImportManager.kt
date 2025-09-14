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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
    private val authService: AuthService
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
            return@withContext ImportProgress(
                currentStep = "Authentication failed",
                progress = 0,
                errors = listOf("User must be authenticated to import data")
            )
        }

        val userRole = firestoreService.getCurrentUserRole()
        if (!PermissionManager.canImportData(userRole)) {
            return@withContext ImportProgress(
                currentStep = "Permission denied",
                progress = 0,
                errors = listOf("Only admins can import Excel data")
            )
        }

        try {
            // Step 1: Parse Excel file
            onProgress(ImportProgress(
                currentStep = "Parsing Excel file...",
                progress = 10
            ))

            val importResult = excelImportService.importExcelFile(uri, userId)
            
            if (importResult.errors.isNotEmpty()) {
                return@withContext ImportProgress(
                    currentStep = "Excel parsing failed",
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

            // Step 2: Get existing drivers and vehicles
            onProgress(ImportProgress(
                currentStep = "Checking existing drivers and vehicles...",
                progress = 20,
                totalEntries = totalEntries
            ))

            val existingDrivers = firestoreService.getDrivers().map { it.name.lowercase() }.toSet()
            val existingVehicles = firestoreService.getVehicles().map { it.displayName.lowercase() }.toSet()

            // Step 3: Create missing drivers
            val driversToCreate = importResult.driversToCreate.filter { 
                !existingDrivers.contains(it.name.lowercase())
            }.distinctBy { it.name.lowercase() }

            if (driversToCreate.isNotEmpty()) {
                onProgress(ImportProgress(
                    currentStep = "Creating ${driversToCreate.size} new drivers...",
                    progress = 30,
                    totalEntries = totalEntries
                ))

                driversToCreate.forEach { driver ->
                    try {
                        firestoreService.saveDriver(driver)
                        Log.d(TAG, "Created driver: ${driver.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create driver: ${driver.name}", e)
                    }
                }
            }

            // Step 4: Create missing vehicles
            val vehiclesToCreate = importResult.vehiclesToCreate.filter { 
                !existingVehicles.contains(it.displayName.lowercase())
            }.distinctBy { it.displayName.lowercase() }

            if (vehiclesToCreate.isNotEmpty()) {
                onProgress(ImportProgress(
                    currentStep = "Creating ${vehiclesToCreate.size} new vehicles...",
                    progress = 40,
                    totalEntries = totalEntries
                ))

                vehiclesToCreate.forEach { vehicle ->
                    try {
                        firestoreService.saveVehicle(vehicle)
                        Log.d(TAG, "Created vehicle: ${vehicle.displayName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create vehicle: ${vehicle.displayName}", e)
                    }
                }
            }

            // Step 5: Import entries
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
                    firestoreService.saveDailyEntry(entry)
                    processedEntries++
                    
                    val progressPercent = 50 + ((processedEntries.toFloat() / totalEntries) * 40).toInt()
                    onProgress(ImportProgress(
                        currentStep = "Importing entries... ($processedEntries/$totalEntries)",
                        progress = progressPercent,
                        totalEntries = totalEntries,
                        processedEntries = processedEntries,
                        errors = errors,
                        warnings = warnings
                    ))
                    
                    Log.d(TAG, "Imported entry for ${entry.driverName} on ${entry.date}")
                } catch (e: Exception) {
                    val errorMessage = "Failed to import entry for ${entry.driverName} on ${entry.date}: ${e.message}"
                    errors.add(errorMessage)
                    Log.e(TAG, errorMessage, e)
                }
            }

            // Final result
            val finalStep = if (errors.isEmpty()) {
                "Import completed successfully"
            } else {
                "Import completed with ${errors.size} errors"
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
            Log.e(TAG, "Error during Excel import", e)
            return@withContext ImportProgress(
                currentStep = "Import failed",
                progress = 0,
                errors = listOf("Import failed: ${e.message}")
            )
        }
    }
}

// Extension function to check if user can import data
private fun PermissionManager.canImportData(userRole: UserRole): Boolean {
    return canEdit(userRole) // Only admins can import
}

// Extension property for Vehicle display name
private val Vehicle.displayName: String
    get() = "$make $model".trim()