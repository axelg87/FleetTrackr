package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting active vehicles.
 */
class GetActiveVehiclesUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<List<Vehicle>> {
        return repository.getAllActiveVehicles()
    }
}