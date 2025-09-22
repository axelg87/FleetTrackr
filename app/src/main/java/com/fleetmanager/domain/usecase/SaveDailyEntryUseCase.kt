package com.fleetmanager.domain.usecase

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for saving a daily entry.
 * Encapsulates the business logic and validation for saving entries.
 */
class SaveDailyEntryUseCase @Inject constructor(
    private val repository: FleetRepository,
    private val validator: InputValidator
) {
    
    suspend operator fun invoke(
        entry: DailyEntry, 
        photoUri: Uri? = null, 
        photoUris: List<Uri> = emptyList()
    ): Result<Unit> {
        return try {
            // Comprehensive validation
            val validationResult = validateEntry(entry)
            if (validationResult.isError) {
                return Result.failure(IllegalArgumentException(validationResult.getErrorMessage()))
            }
            
            // Sanitize and create clean entry
            val sanitizedEntry = sanitizeEntry(entry)
            
            // Save the entry
            repository.saveDailyEntry(sanitizedEntry, photoUri, photoUris)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateEntry(entry: DailyEntry): com.fleetmanager.domain.validation.ValidationResult {
        return validator.validateAll(
            { validator.validateText(entry.id, "Entry ID") },
            { validator.validateText(entry.driverId, "Driver ID") },
            { validator.validateText(entry.vehicleId, "Vehicle ID") },
            { validator.validateEarnings(entry.uberEarnings.toString(), "Uber earnings") },
            { validator.validateEarnings(entry.yangoEarnings.toString(), "Yango earnings") },
            { validator.validateEarnings(entry.privateJobsEarnings.toString(), "Private jobs earnings") },
            { validator.validateNotes(entry.notes) },
            { validator.validateDate(entry.date) }
        )
    }
    
    private fun sanitizeEntry(entry: DailyEntry): DailyEntry {
        return entry.copy(
            notes = validator.sanitizeText(entry.notes)
        )
    }
}