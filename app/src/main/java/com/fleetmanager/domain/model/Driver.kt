package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Domain model for driver.
 * This represents the business entity without any framework dependencies.
 * Firestore-compatible with no-arg constructor and property annotations.
 */
data class Driver(
    @get:PropertyName("id")
    val id: String = "",
    
    @get:PropertyName("userId")
    val userId: String = "",
    
    @get:PropertyName("name")
    val name: String = "",

    @get:PropertyName("isActive")
    val isActive: Boolean = true,

    @get:PropertyName("salary")
    val salary: Double = 0.0,

    @get:PropertyName("annualLicenseCost")
    val annualLicenseCost: Double = 0.0,

    @get:PropertyName("annualVisaCost")
    val annualVisaCost: Double = 0.0
) {
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                salary >= 0.0 &&
                annualLicenseCost >= 0.0 &&
                annualVisaCost >= 0.0
    }
}