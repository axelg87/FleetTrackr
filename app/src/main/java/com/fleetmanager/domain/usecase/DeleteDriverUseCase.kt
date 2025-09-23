package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for deleting a driver.
 */
class DeleteDriverUseCase @Inject constructor(
    private val repository: FleetRepository
) {

    suspend operator fun invoke(driverId: String): Result<Unit> {
        return try {
            repository.deleteDriver(driverId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
