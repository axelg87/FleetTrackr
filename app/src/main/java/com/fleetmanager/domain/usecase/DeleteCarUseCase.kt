package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.CarRepository
import javax.inject.Inject

/**
 * Use case for deleting a car entry.
 */
class DeleteCarUseCase @Inject constructor(
    private val repository: CarRepository
) {
    suspend operator fun invoke(carId: String): Result<Unit> {
        return try {
            repository.deleteCar(carId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
