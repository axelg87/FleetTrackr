package com.fleetmanager.data.dto

import com.fleetmanager.domain.model.EarningEntry

/**
 * DTO representation of an earning entry for Room persistence.
 */
data class EarningDto(
    val provider: String = "",
    val cardEarnings: Double = 0.0,
    val cashEarnings: Double = 0.0,
    val tips: Double = 0.0,
    val tripCount: Int = 0,
    val hoursOnline: Double = 0.0
) {
    fun toDomain(): EarningEntry = EarningEntry(
        provider = provider,
        cardEarnings = cardEarnings,
        cashEarnings = cashEarnings,
        tips = tips,
        tripCount = tripCount,
        hoursOnline = hoursOnline
    )

    companion object {
        fun fromDomain(entry: EarningEntry): EarningDto = EarningDto(
            provider = entry.provider,
            cardEarnings = entry.cardEarnings,
            cashEarnings = entry.cashEarnings,
            tips = entry.tips,
            tripCount = entry.tripCount,
            hoursOnline = entry.hoursOnline
        )
    }
}
