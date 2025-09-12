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
    val isActive: Boolean = true
) {
    fun isValid(): Boolean {
        return id.isNotBlank() && name.isNotBlank()
    }
}