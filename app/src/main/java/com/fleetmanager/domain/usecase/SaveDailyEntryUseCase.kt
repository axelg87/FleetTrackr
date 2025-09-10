package com.fleetmanager.domain.usecase

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for saving a daily entry.
 * Encapsulates the business logic and validation for saving entries.
 */
class SaveDailyEntryUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    suspend operator fun invoke(
        entry: DailyEntry, 
        photoUri: Uri? = null, 
        photoUris: List<Uri> = emptyList()
    ): Result<Unit> {
        return try {
            // Validate the entry
            if (!entry.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid entry data"))
            }
            
            // Save the entry
            repository.saveDailyEntry(entry, photoUri, photoUris)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}