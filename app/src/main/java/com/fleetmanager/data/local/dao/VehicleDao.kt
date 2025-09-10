package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.dto.VehicleDto
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    
    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY make, model ASC")
    fun getAllActiveVehicles(): Flow<List<VehicleDto>>
    
    @Query("SELECT * FROM vehicles ORDER BY make, model ASC")
    fun getAllVehicles(): Flow<List<VehicleDto>>
    
    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: String): VehicleDto?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleDto)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleDto>)
    
    @Update
    suspend fun updateVehicle(vehicle: VehicleDto)
    
    @Delete
    suspend fun deleteVehicle(vehicle: VehicleDto)
}