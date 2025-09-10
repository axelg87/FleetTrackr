package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for saving a driver.
 * Encapsulates the business logic and validation for saving drivers.
 */
class SaveDriverUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    suspend operator fun invoke(driver: Driver): Result<Unit> {
        return try {
            // Validate the driver
            if (!driver.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid driver data"))
            }
            
            // Save the driver
            repository.saveDriver(driver)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}