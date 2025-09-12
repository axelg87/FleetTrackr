package com.fleetmanager.domain.model

/**
 * Domain model for vehicle.
 * This represents the business entity without any framework dependencies.
 */
data class Vehicle(
    val id: String,
    val userId: String = "",
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val isActive: Boolean = true
) {
    val displayName: String
        get() = "$make $model ($year) - $licensePlate"

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                make.isNotBlank() &&
                model.isNotBlank() &&
                year > 1900 &&
                year <= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1 &&
                licensePlate.isNotBlank()
    }
}