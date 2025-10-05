package com.fleetmanager.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Domain model for daily entry.
 * This represents the business entity without any framework dependencies.
 * 
 * Firebase Firestore compatible with proper field mapping.
 */
data class DailyEntry(
    @get:PropertyName("id")
    val id: String = "",
    
    @get:PropertyName("userId")
    val userId: String = "",
    
    @get:PropertyName("date")
    val date: Date = Date(),

    @get:PropertyName("driverId")
    val driverId: String = "",

    @get:Exclude
    val driverName: String = "",

    @get:PropertyName("vehicleId")
    val vehicleId: String = "",

    @get:Exclude
    val vehicle: String = "",

    @get:PropertyName("odometer")
    val odometer: Int? = null,

    @get:PropertyName("uberEarnings")
    val uberEarnings: Double = 0.0,

    @get:PropertyName("uberHoursOnline")
    val uberHoursOnline: Double = 0.0,

    @get:PropertyName("uberCashEarnings")
    val uberCashEarnings: Double = 0.0,

    @get:PropertyName("uberCardEarnings")
    val uberCardEarnings: Double = 0.0,

    @get:PropertyName("uberTips")
    val uberTips: Double = 0.0,

    @get:PropertyName("yangoEarnings")
    val yangoEarnings: Double = 0.0,

    @get:PropertyName("yangoHoursOnline")
    val yangoHoursOnline: Double = 0.0,

    @get:PropertyName("yangoCashEarnings")
    val yangoCashEarnings: Double = 0.0,

    @get:PropertyName("yangoCardEarnings")
    val yangoCardEarnings: Double = 0.0,

    @get:PropertyName("yangoTips")
    val yangoTips: Double = 0.0,

    @get:PropertyName("privateJobsEarnings")
    val privateJobsEarnings: Double = 0.0,

    @get:PropertyName("privateJobsHoursOnline")
    val privateJobsHoursOnline: Double = 0.0,

    @get:PropertyName("privateJobsCashEarnings")
    val privateJobsCashEarnings: Double = 0.0,

    @get:PropertyName("privateJobsCardEarnings")
    val privateJobsCardEarnings: Double = 0.0,

    @get:PropertyName("privateJobsTips")
    val privateJobsTips: Double = 0.0,

    @get:PropertyName("careemEarnings")
    val careemEarnings: Double = 0.0,
    
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
    val totalEarnings: Double
        get() = uberEarnings + yangoEarnings + privateJobsEarnings + careemEarnings

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                driverId.isNotBlank() &&
                vehicleId.isNotBlank() &&
                (odometer == null || (odometer >= 0 && odometer <= 9_999_999)) &&
                uberEarnings >= 0 &&
                uberHoursOnline >= 0 &&
                uberCashEarnings >= 0 &&
                uberCardEarnings >= 0 &&
                uberTips >= 0 &&
                yangoEarnings >= 0 &&
                yangoHoursOnline >= 0 &&
                yangoCashEarnings >= 0 &&
                yangoCardEarnings >= 0 &&
                yangoTips >= 0 &&
                privateJobsEarnings >= 0 &&
                privateJobsHoursOnline >= 0 &&
                privateJobsCashEarnings >= 0 &&
                privateJobsCardEarnings >= 0 &&
                privateJobsTips >= 0 &&
                careemEarnings >= 0 &&
                uberEarnings <= 999999.99 &&
                uberCashEarnings <= 999999.99 &&
                uberCardEarnings <= 999999.99 &&
                uberTips <= 999999.99 &&
                yangoEarnings <= 999999.99 &&
                yangoCashEarnings <= 999999.99 &&
                yangoCardEarnings <= 999999.99 &&
                yangoTips <= 999999.99 &&
                privateJobsEarnings <= 999999.99 &&
                privateJobsCashEarnings <= 999999.99 &&
                privateJobsCardEarnings <= 999999.99 &&
                privateJobsTips <= 999999.99 &&
                careemEarnings <= 999999.99 &&
                notes.length <= 5000
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (id.isBlank()) errors.add("ID cannot be blank")
        if (driverId.isBlank()) errors.add("Driver ID cannot be blank")
        if (vehicleId.isBlank()) errors.add("Vehicle ID cannot be blank")
        if (odometer != null) {
            if (odometer < 0) errors.add("Odometer cannot be negative")
            if (odometer > 9_999_999) errors.add("Odometer is too large")
        }
        if (uberEarnings < 0) errors.add("Uber earnings cannot be negative")
        if (uberHoursOnline < 0) errors.add("Uber hours online cannot be negative")
        if (uberCashEarnings < 0) errors.add("Uber cash earnings cannot be negative")
        if (uberCardEarnings < 0) errors.add("Uber card earnings cannot be negative")
        if (uberTips < 0) errors.add("Uber tips cannot be negative")
        if (yangoEarnings < 0) errors.add("Yango earnings cannot be negative")
        if (yangoHoursOnline < 0) errors.add("Yango hours online cannot be negative")
        if (yangoCashEarnings < 0) errors.add("Yango cash earnings cannot be negative")
        if (yangoCardEarnings < 0) errors.add("Yango card earnings cannot be negative")
        if (yangoTips < 0) errors.add("Yango tips cannot be negative")
        if (privateJobsEarnings < 0) errors.add("Private jobs earnings cannot be negative")
        if (privateJobsHoursOnline < 0) errors.add("Private jobs hours online cannot be negative")
        if (privateJobsCashEarnings < 0) errors.add("Private jobs cash earnings cannot be negative")
        if (privateJobsCardEarnings < 0) errors.add("Private jobs card earnings cannot be negative")
        if (privateJobsTips < 0) errors.add("Private jobs tips cannot be negative")
        if (careemEarnings < 0) errors.add("Careem earnings cannot be negative")
        if (uberEarnings > 999999.99) errors.add("Uber earnings is too large")
        if (uberCashEarnings > 999999.99) errors.add("Uber cash earnings is too large")
        if (uberCardEarnings > 999999.99) errors.add("Uber card earnings is too large")
        if (uberTips > 999999.99) errors.add("Uber tips is too large")
        if (yangoEarnings > 999999.99) errors.add("Yango earnings is too large")
        if (yangoCashEarnings > 999999.99) errors.add("Yango cash earnings is too large")
        if (yangoCardEarnings > 999999.99) errors.add("Yango card earnings is too large")
        if (yangoTips > 999999.99) errors.add("Yango tips is too large")
        if (privateJobsEarnings > 999999.99) errors.add("Private jobs earnings is too large")
        if (privateJobsCashEarnings > 999999.99) errors.add("Private jobs cash earnings is too large")
        if (privateJobsCardEarnings > 999999.99) errors.add("Private jobs card earnings is too large")
        if (privateJobsTips > 999999.99) errors.add("Private jobs tips is too large")
        if (careemEarnings > 999999.99) errors.add("Careem earnings is too large")
        if (notes.length > 5000) errors.add("Notes too long (max 5000 characters)")

        return errors
    }

    fun withResolvedDisplayData(
        driverDisplayName: String?,
        vehicleDisplayName: String?
    ): DailyEntry {
        val resolvedDriverName = driverDisplayName?.takeIf { it.isNotBlank() }
            ?: driverName.takeIf { it.isNotBlank() }
            ?: driverId

        val resolvedVehicleName = vehicleDisplayName?.takeIf { it.isNotBlank() }
            ?: vehicle.takeIf { it.isNotBlank() }
            ?: vehicleId

        return copy(
            driverName = resolvedDriverName,
            vehicle = resolvedVehicleName
        )
    }
}