package com.fleetmanager.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Enum representing different earning provider types
 */
enum class ProviderType {
    UBER,
    CAREEM,
    YANGO,
    PRIVATE,
    OTHER
}

/**
 * Represents earnings from a single provider
 */
data class ProviderEarning(
    val type: ProviderType,
    val amount: Double,
    val currency: String = "AED",
    val tripsCount: Int? = null,
    val meta: Map<String, Any?>? = null
)

/**
 * Domain model for daily entry.
 * This represents the business entity without any framework dependencies.
 * 
 * Firebase Firestore compatible with proper field mapping.
 * New provider-based model.
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
    
    @get:PropertyName("providers")
    val providers: List<ProviderEarning> = emptyList(),
    
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
    /**
     * Calculate total earnings from all providers
     */
    val totalEarnings: Double
        get() = providers.sumOf { it.amount }

    /**
     * Get earnings amount for specific provider type(s)
     */
    fun amountFor(vararg types: ProviderType): Double {
        return providers
            .filter { it.type in types }
            .sumOf { it.amount }
    }

    /**
     * Backward compatibility: Uber earnings
     */
    @get:Exclude
    val uberEarnings: Double
        get() = amountFor(ProviderType.UBER)

    /**
     * Backward compatibility: Careem earnings
     */
    @get:Exclude
    val careemEarnings: Double
        get() = amountFor(ProviderType.CAREEM)

    /**
     * Backward compatibility: Yango earnings
     */
    @get:Exclude
    val yangoEarnings: Double
        get() = amountFor(ProviderType.YANGO)

    /**
     * Backward compatibility: Private jobs earnings
     */
    @get:Exclude
    val privateJobsEarnings: Double
        get() = amountFor(ProviderType.PRIVATE)

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                driverId.isNotBlank() &&
                vehicleId.isNotBlank() &&
                providers.all { it.amount >= 0 && it.amount <= 999999.99 } &&
                notes.length <= 5000
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) errors.add("ID cannot be blank")
        if (driverId.isBlank()) errors.add("Driver ID cannot be blank")
        if (vehicleId.isBlank()) errors.add("Vehicle ID cannot be blank")
        
        providers.forEach { provider ->
            if (provider.amount < 0) {
                errors.add("${provider.type.name} earnings cannot be negative")
            }
            if (provider.amount > 999999.99) {
                errors.add("${provider.type.name} earnings is too large")
            }
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
