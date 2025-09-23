package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for deleting a driver.
 */
class DeleteDriverUseCase @Inject constructor(
    private val repository: FleetRepository
) {

    suspend operator fun invoke(driverId: String, userRole: UserRole): Result<Unit> {
        return try {
            if (!PermissionManager.canManageDrivers(userRole)) {
                return Result.failure(SecurityException("Insufficient permissions to manage drivers."))
            }

            repository.deleteDriver(driverId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
