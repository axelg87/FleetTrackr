package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for saving a vehicle.
 * Encapsulates the business logic and validation for saving vehicles.
 */
class SaveVehicleUseCase @Inject constructor(
    private val repository: FleetRepository,
    private val validator: InputValidator
) {
    
    suspend operator fun invoke(vehicle: Vehicle): Result<Unit> {
        return try {
            // Validate the vehicle
            val validationResult = validator.validateAll(
                { validator.validateText(vehicle.id, "Vehicle ID") },
                { validator.validateText(vehicle.make, "Vehicle make") },
                { validator.validateText(vehicle.model, "Vehicle model") },
                { validator.validateYear(vehicle.year) },
                { validator.validateLicensePlate(vehicle.licensePlate) }
            )
            
            if (validationResult.isError) {
                return Result.failure(IllegalArgumentException(validationResult.getErrorMessage()))
            }
            
            // Sanitize and create clean vehicle
            val sanitizedVehicle = vehicle.copy(
                make = validator.sanitizeText(vehicle.make),
                model = validator.sanitizeText(vehicle.model),
                licensePlate = validator.sanitizeText(vehicle.licensePlate).uppercase()
            )
            
            // Save the vehicle
            repository.saveVehicle(sanitizedVehicle)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}