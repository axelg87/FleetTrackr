package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.dto.DriverDto
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    
    @Query("SELECT * FROM drivers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveDrivers(): Flow<List<DriverDto>>
    
    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<DriverDto>>
    
    @Query("SELECT * FROM drivers WHERE id = :id")
    suspend fun getDriverById(id: String): DriverDto?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverDto)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<DriverDto>)
    
    @Update
    suspend fun updateDriver(driver: DriverDto)
    
    @Delete
    suspend fun deleteDriver(driver: DriverDto)
}