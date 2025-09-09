package com.fleetmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val isActive: Boolean = true
) {
    val displayName: String
        get() = "$make $model ($year) - $licensePlate"
}