package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.model.Driver
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    
    @Query("SELECT * FROM drivers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveDrivers(): Flow<List<Driver>>
    
    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<Driver>>
    
    @Query("SELECT * FROM drivers WHERE id = :id")
    suspend fun getDriverById(id: String): Driver?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<Driver>)
    
    @Update
    suspend fun updateDriver(driver: Driver)
    
    @Delete
    suspend fun deleteDriver(driver: Driver)
}