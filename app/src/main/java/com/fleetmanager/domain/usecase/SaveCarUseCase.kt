package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Car
import com.fleetmanager.domain.repository.CarRepository
import com.fleetmanager.domain.validation.InputValidator
import com.fleetmanager.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Use case responsible for validating and saving car entities.
 */
class SaveCarUseCase @Inject constructor(
    private val repository: CarRepository,
    private val validator: InputValidator
) {
    suspend operator fun invoke(car: Car): Result<Unit> {
        return try {
            val validationResult = validator.validateAll(
                { validator.validateText(car.id, "Car ID") },
                { validator.validateText(car.nickname, "Nickname", required = false, maxLength = 60) },
                { validator.validateText(car.make, "Make") },
                { validator.validateText(car.model, "Model") },
                { car.year?.let { validator.validateYear(it) } ?: ValidationResult.Success },
                { validator.validateLicensePlate(car.licensePlate) },
                { validator.validateText(car.color, "Color", required = false, maxLength = 40) }
            )

            if (validationResult.isError) {
                return Result.failure(IllegalArgumentException(validationResult.getErrorMessage()))
            }

            val sanitizedCar = car.copy(
                nickname = validator.sanitizeText(car.nickname),
                make = validator.sanitizeText(car.make),
                model = validator.sanitizeText(car.model),
                licensePlate = validator.sanitizeText(car.licensePlate).uppercase(),
                color = validator.sanitizeText(car.color)
            )

            repository.saveCar(sanitizedCar)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
