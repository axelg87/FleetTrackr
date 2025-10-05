package com.fleetmanager.domain.model

/**
 * Represents the earning details for a specific provider including
 * payment method breakdown and operational metadata.
 */
data class EarningBreakdown(
    val provider: String = "",
    val cardEarnings: Double = 0.0,
    val cashEarnings: Double = 0.0,
    val tips: Double = 0.0,
    val tripCount: Int = 0,
    val hoursOnline: Double = 0.0
) {
    val total: Double
        get() = cardEarnings + cashEarnings + tips
}
