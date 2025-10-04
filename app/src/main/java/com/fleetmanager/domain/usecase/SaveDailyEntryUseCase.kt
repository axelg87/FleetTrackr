package com.fleetmanager.domain.usecase

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.EarningEntry
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import com.fleetmanager.domain.validation.ValidationResult
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
    
    private fun validateEntry(entry: DailyEntry): ValidationResult {
        val baseValidation = validator.validateAll(
            { validator.validateText(entry.id, "Entry ID") },
            { validator.validateText(entry.driverId, "Driver ID") },
            { validator.validateText(entry.vehicleId, "Vehicle ID") },
            { validateEarningsList(entry.earnings) },
            { validator.validateNotes(entry.notes) },
            { validator.validateDate(entry.date) }
        )

        if (baseValidation.isError) {
            return baseValidation
        }

        return ValidationResult.Success
    }

    private fun sanitizeEntry(entry: DailyEntry): DailyEntry {
        return entry.copy(
            notes = validator.sanitizeText(entry.notes)
        )
    }

    private fun validateEarningsList(earnings: List<EarningEntry>): ValidationResult {
        if (earnings.isEmpty()) {
            return ValidationResult.Error("At least one earning source is required")
        }

        val seenProviders = mutableSetOf<String>()

        earnings.forEachIndexed { index, earning ->
            val providerLabel = earning.provider.ifBlank { "Provider ${index + 1}" }

            val providerValidation = validator.validateText(earning.provider, "Provider name")
            if (providerValidation.isError) {
                return providerValidation
            }

            val normalizedProvider = earning.provider.trim().lowercase()
            if (!seenProviders.add(normalizedProvider)) {
                return ValidationResult.Error("Duplicate provider '${earning.provider}' detected")
            }

            val validations = listOf(
                validator.validateNonNegativeAmount(earning.cardEarnings, "$providerLabel card earnings", required = false),
                validator.validateNonNegativeAmount(earning.cashEarnings, "$providerLabel cash earnings", required = false),
                validator.validateNonNegativeAmount(earning.tips, "$providerLabel tips", required = false),
                validator.validateNonNegativeAmount(earning.hoursOnline, "$providerLabel hours online", required = false)
            )

            validations.forEach { result ->
                if (result.isError) {
                    return result
                }
            }

            if (earning.tripCount < 0) {
                return ValidationResult.Error("$providerLabel trip count cannot be negative")
            }

            if (earning.totalAmount <= 0.0) {
                return ValidationResult.Error("$providerLabel must have earnings greater than 0")
            }
        }

        return ValidationResult.Success
    }
}
