package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Domain model for vehicle.
 * This represents the business entity without any framework dependencies.
 * Firestore-compatible with no-arg constructor and property annotations.
 */
data class Vehicle(
    @get:PropertyName("id")
    val id: String = "",
    
    @get:PropertyName("userId")
    val userId: String = "",
    
    @get:PropertyName("make")
    val make: String = "",
    
    @get:PropertyName("model")
    val model: String = "",
    
    @get:PropertyName("year")
    val year: Int = 0,
    
    @get:PropertyName("licensePlate")
    val licensePlate: String = "",
    
    @get:PropertyName("isActive")
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