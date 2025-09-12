package com.fleetmanager.domain.model

/**
 * Domain model for driver.
 * This represents the business entity without any framework dependencies.
 */
data class Driver(
    val id: String,
    val userId: String = "",
    val name: String,
    val isActive: Boolean = true
) {
    fun isValid(): Boolean {
        return id.isNotBlank() && name.isNotBlank()
    }
}