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
    OTHER;
    
    companion object {
        fun fromString(value: String?): ProviderType {
            return try {
                valueOf(value?.uppercase() ?: "OTHER")
            } catch (e: Exception) {
                OTHER
            }
        }
    }
}

/**
 * Represents earnings from a single provider
 * Firestore-compatible data class
 */
data class ProviderEarning(
    @get:PropertyName("type")
    val type: ProviderType = ProviderType.OTHER,

    @get:PropertyName("amount")
    val amount: Double = 0.0,

    @get:PropertyName("card")
    val cardAmount: Double = 0.0,

    @get:PropertyName("cash")
    val cashAmount: Double = 0.0,

    @get:PropertyName("tips")
    val tipsAmount: Double = 0.0,

    @get:PropertyName("hoursOnline")
    val hoursOnline: Double? = null,

    @get:PropertyName("currency")
    val currency: String = "AED",

    @get:PropertyName("tripsCount")
    val tripsCount: Int? = null,

    @get:PropertyName("meta")
    val meta: Map<String, Any?>? = null
) {
    // No-arg constructor for Firestore
    constructor() : this(
        type = ProviderType.OTHER,
        amount = 0.0,
        cardAmount = 0.0,
        cashAmount = 0.0,
        tipsAmount = 0.0,
        hoursOnline = null,
        currency = "AED",
        tripsCount = null,
        meta = null
    )

    val hasBreakdown: Boolean
        get() = cardAmount > 0.0 || cashAmount > 0.0 || tipsAmount > 0.0

    val totalAmount: Double
        get() = when {
            hasBreakdown -> cardAmount + cashAmount + tipsAmount
            else -> amount
        }
}

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

    @get:PropertyName("odometer")
    val odometer: Double? = null,

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
        get() = providers.sumOf { it.totalAmount }

    /**
     * Get earnings amount for specific provider type(s)
     */
    fun amountFor(vararg types: ProviderType): Double {
        return providers
            .filter { it.type in types }
            .sumOf { it.totalAmount }
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
        val hasValidProvider = providers.any { it.totalAmount > 0.0 }
        val providersValid = providers.all { provider ->
            val total = provider.totalAmount
            total >= 0 && total <= 999999.99 &&
                    provider.cashAmount >= 0 &&
                    provider.cardAmount >= 0 &&
                    provider.tipsAmount >= 0 &&
                    (provider.hoursOnline == null || provider.hoursOnline >= 0)
        }

        val odometerValid = odometer == null || odometer >= 0

        return id.isNotBlank() &&
                driverId.isNotBlank() &&
                vehicleId.isNotBlank() &&
                providersValid &&
                hasValidProvider &&
                odometerValid &&
                notes.length <= 5000
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (id.isBlank()) errors.add("ID cannot be blank")
        if (driverId.isBlank()) errors.add("Driver ID cannot be blank")
        if (vehicleId.isBlank()) errors.add("Vehicle ID cannot be blank")

        val hasProvider = providers.any { it.totalAmount > 0.0 }
        if (!hasProvider) {
            errors.add("At least one provider must have earnings")
        }

        providers.forEach { provider ->
            val total = provider.totalAmount
            if (total < 0) {
                errors.add("${provider.type.name} earnings cannot be negative")
            }
            if (total > 999999.99) {
                errors.add("${provider.type.name} earnings is too large")
            }
            if (provider.cashAmount < 0) {
                errors.add("${provider.type.name} cash earnings cannot be negative")
            }
            if (provider.cardAmount < 0) {
                errors.add("${provider.type.name} card earnings cannot be negative")
            }
            if (provider.tipsAmount < 0) {
                errors.add("${provider.type.name} tips cannot be negative")
            }
            provider.hoursOnline?.let { hours ->
                if (hours < 0) {
                    errors.add("${provider.type.name} hours online cannot be negative")
                }
            }
        }

        if (notes.length > 5000) errors.add("Notes too long (max 5000 characters)")
        odometer?.let { value ->
            if (value < 0) {
                errors.add("Odometer cannot be negative")
            }
        }

        return errors
    }

    fun providerFor(type: ProviderType): ProviderEarning? {
        return providers.firstOrNull { it.type == type }
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
