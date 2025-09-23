package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Car
import com.fleetmanager.domain.repository.CarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to stream the list of cars from the repository.
 */
class GetCarsUseCase @Inject constructor(
    private val repository: CarRepository
) {
    operator fun invoke(): Flow<List<Car>> = repository.getCarsStream()
}
