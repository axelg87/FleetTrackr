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
    val isActive: Boolean = true,

    @get:PropertyName("price")
    val price: Double = 0.0,

    @get:PropertyName("deposit")
    val deposit: Double? = null,

    @get:PropertyName("installment")
    val installment: Double? = null,

    @get:PropertyName("installmentDurationMonths")
    val installmentDurationMonths: Int? = null,

    @get:PropertyName("serviceStartDate")
    val serviceStartDate: java.util.Date? = null,

    @get:PropertyName("serviceEndDate")
    val serviceEndDate: java.util.Date? = null,

    @get:PropertyName("annualInsuranceAmount")
    val annualInsuranceAmount: Double = 0.0,

    @get:PropertyName("fuelTankCapacity")
    val fuelTankCapacity: Double? = null,

    @get:PropertyName("fuelConsumptionPer100Km")
    val fuelConsumptionPer100Km: Double? = null
) {
    val displayName: String
        get() = "$make $model ($year) - $licensePlate"

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                make.isNotBlank() &&
                model.isNotBlank() &&
                year > 1900 &&
                year <= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1 &&
                licensePlate.isNotBlank() &&
                price >= 0.0 &&
                (deposit == null || deposit >= 0.0) &&
                (installment == null || installment >= 0.0) &&
                (installmentDurationMonths == null || installmentDurationMonths > 0) &&
                annualInsuranceAmount >= 0.0 &&
                (fuelTankCapacity == null || fuelTankCapacity > 0.0) &&
                (fuelConsumptionPer100Km == null || fuelConsumptionPer100Km > 0.0)
    }
}