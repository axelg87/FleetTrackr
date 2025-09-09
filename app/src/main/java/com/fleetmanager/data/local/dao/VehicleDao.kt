package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    
    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY make, model ASC")
    fun getAllActiveVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles ORDER BY make, model ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: String): Vehicle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)
    
    @Update
    suspend fun updateVehicle(vehicle: Vehicle)
    
    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
}