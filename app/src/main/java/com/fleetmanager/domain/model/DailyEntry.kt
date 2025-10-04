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

    @get:PropertyName("earnings")
    val earnings: List<EarningEntry> = emptyList(),

    @get:PropertyName("odometer")
    val odometer: Double? = null,

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
        get() = earnings.sumOf { it.totalAmount }

    fun getEarningsFor(provider: String): EarningEntry? {
        if (provider.isBlank()) return null
        return earnings.firstOrNull { it.provider.equals(provider, ignoreCase = true) }
    }

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                driverId.isNotBlank() &&
                vehicleId.isNotBlank() &&
                earnings.all { earning -> earning.isValid() } &&
                notes.length <= 5000 &&
                (odometer == null || odometer >= 0)
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (id.isBlank()) errors.add("ID cannot be blank")
        if (driverId.isBlank()) errors.add("Driver ID cannot be blank")
        if (vehicleId.isBlank()) errors.add("Vehicle ID cannot be blank")

        earnings.forEach { earning ->
            errors.addAll(earning.getValidationErrors())
        }

        if (odometer != null && odometer < 0) {
            errors.add("Odometer cannot be negative")
        }

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
