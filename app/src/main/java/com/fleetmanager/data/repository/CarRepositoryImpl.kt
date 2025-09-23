package com.fleetmanager.data.repository

import com.fleetmanager.data.remote.CarFirestoreService
import com.fleetmanager.domain.model.Car
import com.fleetmanager.domain.repository.CarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepositoryImpl @Inject constructor(
    private val carFirestoreService: CarFirestoreService
) : CarRepository {

    override fun getCarsStream(): Flow<List<Car>> = carFirestoreService.getCarsFlow()

    override suspend fun saveCar(car: Car) {
        carFirestoreService.saveCar(car)
    }

    override suspend fun deleteCar(carId: String) {
        carFirestoreService.deleteCar(carId)
    }
}
