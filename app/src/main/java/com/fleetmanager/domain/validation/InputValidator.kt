package com.fleetmanager.domain.validation

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for input validation and sanitization.
 */
@Singleton
class InputValidator @Inject constructor() {
    
    companion object {
        private const val MAX_STRING_LENGTH = 1000
        private const val MAX_NOTES_LENGTH = 5000
        private const val MIN_YEAR = 1900
        private val CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR)
        private val LICENSE_PLATE_REGEX = Regex("^[A-Z0-9-]{3,10}$")
        private val NAME_REGEX = Regex("^[a-zA-Z\\s'-]{2,50}$")
    }
    
    /**
     * Validates and sanitizes a text input.
     */
    fun validateText(
        input: String?, 
        fieldName: String, 
        required: Boolean = true,
        maxLength: Int = MAX_STRING_LENGTH
    ): ValidationResult {
        val sanitized = sanitizeText(input)
        
        if (required && sanitized.isBlank()) {
            return ValidationResult.Error("$fieldName is required")
        }
        
        if (sanitized.length > maxLength) {
            return ValidationResult.Error("$fieldName must be $maxLength characters or less")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates a person's name.
     */
    fun validateName(name: String?, fieldName: String = "Name"): ValidationResult {
        val sanitized = sanitizeText(name)
        
        if (sanitized.isBlank()) {
            return ValidationResult.Error("$fieldName is required")
        }
        
        if (!NAME_REGEX.matches(sanitized)) {
            return ValidationResult.Error("$fieldName contains invalid characters")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates earnings amount.
     */
    fun validateEarnings(amount: String?, fieldName: String): ValidationResult {
        if (amount.isNullOrBlank()) {
            return ValidationResult.Error("$fieldName is required")
        }
        
        val sanitized = sanitizeNumericInput(amount)
        val value = sanitized.toDoubleOrNull()
        
        if (value == null) {
            return ValidationResult.Error("$fieldName must be a valid number")
        }
        
        if (value < 0) {
            return ValidationResult.Error("$fieldName cannot be negative")
        }
        
        if (value > 999999.99) {
            return ValidationResult.Error("$fieldName is too large")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates vehicle year.
     */
    fun validateYear(year: Int): ValidationResult {
        if (year < MIN_YEAR) {
            return ValidationResult.Error("Year must be $MIN_YEAR or later")
        }
        
        if (year > CURRENT_YEAR + 1) {
            return ValidationResult.Error("Year cannot be more than one year in the future")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates license plate.
     */
    fun validateLicensePlate(plate: String?): ValidationResult {
        val sanitized = sanitizeText(plate).uppercase()
        
        if (sanitized.isBlank()) {
            return ValidationResult.Error("License plate is required")
        }
        
        if (!LICENSE_PLATE_REGEX.matches(sanitized)) {
            return ValidationResult.Error("License plate format is invalid")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates notes field.
     */
    fun validateNotes(notes: String?): ValidationResult {
        val sanitized = sanitizeText(notes)
        
        if (sanitized.length > MAX_NOTES_LENGTH) {
            return ValidationResult.Error("Notes must be $MAX_NOTES_LENGTH characters or less")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Validates date is not in the future.
     */
    fun validateDate(date: Date): ValidationResult {
        val now = Date()
        val oneDayFromNow = Date(now.time + 24 * 60 * 60 * 1000) // Allow up to 1 day in future for timezone issues
        
        if (date.after(oneDayFromNow)) {
            return ValidationResult.Error("Date cannot be in the future")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Sanitizes text input by trimming and removing potentially harmful characters.
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input
            .trim()
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // Remove control characters
            .replace(Regex("\\s+"), " ") // Replace multiple whitespace with single space
    }
    
    /**
     * Sanitizes numeric input by removing non-numeric characters except decimal point.
     */
    fun sanitizeNumericInput(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input
            .trim()
            .replace(Regex("[^0-9.]"), "")
            .let { sanitized ->
                // Ensure only one decimal point
                val parts = sanitized.split(".")
                if (parts.size > 2) {
                    "${parts[0]}.${parts.drop(1).joinToString("")}"
                } else {
                    sanitized
                }
            }
    }
    
    /**
     * Validates multiple fields and returns the first error found.
     */
    fun validateAll(vararg validations: () -> ValidationResult): ValidationResult {
        for (validation in validations) {
            val result = validation()
            if (result.isError) {
                return result
            }
        }
        return ValidationResult.Success
    }
}