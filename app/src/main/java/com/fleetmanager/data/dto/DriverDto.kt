package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data Transfer Object for Driver.
 * Used for Room database operations.
 */
@Entity(tableName = "drivers")
data class DriverDto(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val name: String,
    val isActive: Boolean = true,
    val salary: Double = 0.0,
    val annualLicenseCost: Double = 0.0,
    val annualVisaCost: Double = 0.0
)