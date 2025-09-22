package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.validation.InputValidator
import com.fleetmanager.domain.validation.ValidationResult
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
                { validator.validateLicensePlate(vehicle.licensePlate) },
                { validator.validateNonNegativeAmount(vehicle.price, "Vehicle price") },
                { validator.validateNonNegativeAmount(vehicle.deposit, "Deposit", required = false) },
                { validator.validateNonNegativeAmount(vehicle.installment, "Installment", required = false) },
                {
                    if (vehicle.installment != null || vehicle.installmentDurationMonths != null) {
                        validator.validatePositiveInt(
                            vehicle.installmentDurationMonths,
                            "Installment duration",
                            required = true
                        )
                    } else {
                        ValidationResult.Success
                    }
                },
                { validator.validateNonNegativeAmount(vehicle.annualInsuranceAmount, "Annual insurance amount") },
                {
                    validator.validatePositiveAmount(
                        vehicle.fuelTankCapacity,
                        "Fuel tank capacity",
                        required = false
                    )
                },
                {
                    validator.validatePositiveAmount(
                        vehicle.fuelConsumptionPer100Km,
                        "Fuel consumption",
                        required = false
                    )
                }
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