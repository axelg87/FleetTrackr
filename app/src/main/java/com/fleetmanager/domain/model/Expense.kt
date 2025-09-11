package com.fleetmanager.domain.model

import java.util.Date

/**
 * Domain model for expense entry.
 * This represents the business entity without any framework dependencies.
 */
data class Expense(
    val id: String,
    val type: ExpenseType,
    val amount: Double,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val notes: String = "",
    val photoUrls: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                amount > 0 &&
                driverName.isNotBlank() &&
                vehicle.isNotBlank() &&
                amount <= 999999.99 &&
                notes.length <= 5000
    }
    
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) errors.add("ID cannot be blank")
        if (amount <= 0) errors.add("Amount must be greater than zero")
        if (amount > 999999.99) errors.add("Amount is too large")
        if (driverName.isBlank()) errors.add("Driver name cannot be blank")
        if (vehicle.isBlank()) errors.add("Vehicle cannot be blank")
        if (notes.length > 5000) errors.add("Notes too long (max 5000 characters)")
        
        return errors
    }
}

enum class ExpenseType(val displayName: String) {
    FUEL("Fuel"),
    CAR_WASH("Car Wash"),
    FINE("Fine"),
    MAINTENANCE("Maintenance"),
    OTHER("Other");
    
    companion object {
        fun fromDisplayName(displayName: String): ExpenseType? {
            return values().find { it.displayName == displayName }
        }
    }
}