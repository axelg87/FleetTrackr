package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

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
    val isActive: Boolean = true,
    val price: Double = 0.0,
    val deposit: Double? = null,
    val installment: Double? = null,
    val installmentDurationMonths: Int? = null,
    val serviceStartDate: Date? = null,
    val serviceEndDate: Date? = null,
    val annualInsuranceAmount: Double = 0.0,
    val fuelTankCapacity: Double? = null,
    val fuelConsumptionPer100Km: Double? = null
)