package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting active drivers.
 */
class GetActiveDriversUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<List<Driver>> {
        return repository.getAllActiveDrivers()
    }
}