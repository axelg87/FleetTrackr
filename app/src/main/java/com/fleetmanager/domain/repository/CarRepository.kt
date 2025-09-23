package com.fleetmanager.domain.repository

import com.fleetmanager.domain.model.Car
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing cars stored in Firestore.
 */
interface CarRepository {
    fun getCarsStream(): Flow<List<Car>>
    suspend fun saveCar(car: Car)
    suspend fun deleteCar(carId: String)
}
