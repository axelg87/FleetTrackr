package com.fleetmanager.domain.model

import java.util.Date

/**
 * Domain model for daily entry.
 * This represents the business entity without any framework dependencies.
 */
data class DailyEntry(
    val id: String,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val uberEarnings: Double,
    val yangoEarnings: Double,
    val privateJobsEarnings: Double,
    val notes: String,
    val photoUrls: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val totalEarnings: Double
        get() = uberEarnings + yangoEarnings + privateJobsEarnings

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                driverName.isNotBlank() &&
                vehicle.isNotBlank() &&
                uberEarnings >= 0 &&
                yangoEarnings >= 0 &&
                privateJobsEarnings >= 0 &&
                uberEarnings <= 999999.99 &&
                yangoEarnings <= 999999.99 &&
                privateJobsEarnings <= 999999.99 &&
                notes.length <= 5000
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) errors.add("ID cannot be blank")
        if (driverName.isBlank()) errors.add("Driver name cannot be blank")
        if (vehicle.isBlank()) errors.add("Vehicle cannot be blank")
        if (uberEarnings < 0) errors.add("Uber earnings cannot be negative")
        if (yangoEarnings < 0) errors.add("Yango earnings cannot be negative")
        if (privateJobsEarnings < 0) errors.add("Private jobs earnings cannot be negative")
        if (uberEarnings > 999999.99) errors.add("Uber earnings is too large")
        if (yangoEarnings > 999999.99) errors.add("Yango earnings is too large")
        if (privateJobsEarnings > 999999.99) errors.add("Private jobs earnings is too large")
        if (notes.length > 5000) errors.add("Notes too long (max 5000 characters)")
        
        return errors
    }
}