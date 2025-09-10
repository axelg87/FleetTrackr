package com.fleetmanager.domain.validation

/**
 * Represents the result of a validation operation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    fun getErrorMessage(): String? {
        return when (this) {
            is Error -> message
            is Success -> null
        }
    }
}