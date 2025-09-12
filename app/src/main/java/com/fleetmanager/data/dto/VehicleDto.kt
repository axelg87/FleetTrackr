package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data Transfer Object for Vehicle.
 * Used for Room database operations.
 */
@Entity(tableName = "vehicles")
data class VehicleDto(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val isActive: Boolean = true
)