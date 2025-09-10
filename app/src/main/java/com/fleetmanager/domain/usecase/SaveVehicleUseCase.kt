package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for saving a vehicle.
 * Encapsulates the business logic and validation for saving vehicles.
 */
class SaveVehicleUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    suspend operator fun invoke(vehicle: Vehicle): Result<Unit> {
        return try {
            // Validate the vehicle
            if (!vehicle.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid vehicle data"))
            }
            
            // Save the vehicle
            repository.saveVehicle(vehicle)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}