package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for saving a driver.
 * Encapsulates the business logic and validation for saving drivers.
 */
class SaveDriverUseCase @Inject constructor(
    private val repository: FleetRepository,
    private val validator: InputValidator
) {
    
    suspend operator fun invoke(driver: Driver): Result<Unit> {
        return try {
            // Validate the driver
            val validationResult = validator.validateAll(
                { validator.validateText(driver.id, "Driver ID") },
                { validator.validateName(driver.name, "Driver name") }
            )
            
            if (validationResult.isError) {
                return Result.failure(IllegalArgumentException(validationResult.getErrorMessage()))
            }
            
            // Sanitize and create clean driver
            val sanitizedDriver = driver.copy(
                name = validator.sanitizeText(driver.name)
            )
            
            // Save the driver
            repository.saveDriver(sanitizedDriver)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}