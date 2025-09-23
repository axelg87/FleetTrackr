package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.model.UserRole
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
    
    suspend operator fun invoke(driver: Driver, userRole: UserRole): Result<Unit> {
        return try {
            if (!PermissionManager.canManageDrivers(userRole)) {
                return Result.failure(SecurityException("Insufficient permissions to manage drivers."))
            }

            // Validate the driver
            val validationResult = validator.validateAll(
                { validator.validateText(driver.id, "Driver ID") },
                { validator.validateName(driver.name, "Driver name") },
                { validator.validateNonNegativeAmount(driver.salary, "Driver salary") },
                { validator.validateNonNegativeAmount(driver.annualLicenseCost, "Annual license cost") },
                { validator.validateNonNegativeAmount(driver.annualVisaCost, "Annual visa cost") }
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