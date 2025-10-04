package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a single provider earning record for a daily entry.
 */
data class EarningEntry(
    @get:PropertyName("provider")
    val provider: String = "",

    @get:PropertyName("cardEarnings")
    val cardEarnings: Double = 0.0,

    @get:PropertyName("cashEarnings")
    val cashEarnings: Double = 0.0,

    @get:PropertyName("tips")
    val tips: Double = 0.0,

    @get:PropertyName("tripCount")
    val tripCount: Int = 0,

    @get:PropertyName("hoursOnline")
    val hoursOnline: Double = 0.0
) {
    val totalAmount: Double
        get() = cardEarnings + cashEarnings + tips

    fun isValid(): Boolean {
        return provider.isNotBlank() &&
                cardEarnings >= 0 &&
                cashEarnings >= 0 &&
                tips >= 0 &&
                tripCount >= 0 &&
                hoursOnline >= 0
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        if (provider.isBlank()) errors.add("Provider name is required")
        if (cardEarnings < 0) errors.add("Card earnings for $provider cannot be negative")
        if (cashEarnings < 0) errors.add("Cash earnings for $provider cannot be negative")
        if (tips < 0) errors.add("Tips for $provider cannot be negative")
        if (tripCount < 0) errors.add("Trip count for $provider cannot be negative")
        if (hoursOnline < 0) errors.add("Hours online for $provider cannot be negative")
        return errors
    }
}
