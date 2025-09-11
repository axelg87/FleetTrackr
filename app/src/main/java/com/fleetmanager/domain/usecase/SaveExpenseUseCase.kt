package com.fleetmanager.domain.usecase

import android.net.Uri
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for saving an expense entry.
 * Encapsulates the business logic and validation for saving expenses.
 */
class SaveExpenseUseCase @Inject constructor(
    private val repository: FleetRepository,
    private val validator: InputValidator
) {
    
    suspend operator fun invoke(
        expense: Expense, 
        photoUri: Uri? = null, 
        photoUris: List<Uri> = emptyList()
    ): Result<Unit> {
        return try {
            // Comprehensive validation
            val validationResult = validateExpense(expense)
            if (validationResult.isError) {
                return Result.failure(IllegalArgumentException(validationResult.getErrorMessage()))
            }
            
            // Sanitize and create clean expense
            val sanitizedExpense = sanitizeExpense(expense)
            
            // Save the expense
            repository.saveExpense(sanitizedExpense, photoUri, photoUris)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateExpense(expense: Expense): com.fleetmanager.domain.validation.ValidationResult {
        return validator.validateAll(
            { validator.validateText(expense.id, "Expense ID") },
            { validator.validateName(expense.driverName, "Driver name") },
            { validator.validateText(expense.vehicle, "Vehicle") },
            { validator.validateEarnings(expense.amount.toString(), "Amount") },
            { validator.validateNotes(expense.notes) },
            { validator.validateDate(expense.date) }
        )
    }
    
    private fun sanitizeExpense(expense: Expense): Expense {
        return expense.copy(
            driverName = validator.sanitizeText(expense.driverName),
            vehicle = validator.sanitizeText(expense.vehicle),
            notes = validator.sanitizeText(expense.notes)
        )
    }
}