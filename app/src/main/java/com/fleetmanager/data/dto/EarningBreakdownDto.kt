package com.fleetmanager.data.dto

/**
 * Local persistence representation of the provider earning breakdown.
 */
data class EarningBreakdownDto(
    val provider: String = "",
    val cardEarnings: Double = 0.0,
    val cashEarnings: Double = 0.0,
    val tips: Double = 0.0,
    val tripCount: Int = 0,
    val hoursOnline: Double = 0.0
)
