package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for deleting a vehicle from the fleet.
 */
class DeleteVehicleUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> {
        return try {
            repository.deleteVehicle(vehicleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
