package com.fleetmanager.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Domain model for expense entry.
 * This represents the business entity without any framework dependencies.
 * 
 * Firebase Firestore compatible with proper field mapping.
 */
data class Expense(
    @get:PropertyName("id")
    val id: String = "",
    
    @get:PropertyName("userId")
    val userId: String = "",
    
    @get:PropertyName("type")
    val type: ExpenseType = ExpenseType.OTHER,
    
    @get:PropertyName("amount")
    val amount: Double = 0.0,
    
    @get:PropertyName("date")
    val date: Date = Date(),
    
    @get:PropertyName("driver")
    val driverName: String = "",
    
    @get:PropertyName("car")
    val vehicle: String = "",
    
    @get:PropertyName("notes")
    val notes: String = "",
    
    @get:PropertyName("photos")
    val photoUrls: List<String> = emptyList(),
    
    @get:PropertyName("isSynced")
    val isSynced: Boolean = false,
    
    @get:PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @get:PropertyName("updatedAt")
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
    SERVICE("Service"),
    CAR_WASH("Car Wash"),
    FINE("Fine"),
    MAINTENANCE("Maintenance"),
    OTHER("Other");
    
    companion object {
        fun fromDisplayName(displayName: String): ExpenseType? {
            return values().find { it.displayName == displayName }
        }
        
        // For Firestore serialization - it will use the enum name (e.g., "FUEL")
        fun fromString(value: String): ExpenseType {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                OTHER // Default fallback
            }
        }
    }
}