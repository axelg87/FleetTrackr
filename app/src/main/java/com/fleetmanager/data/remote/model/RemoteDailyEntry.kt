package com.fleetmanager.data.remote.model

import java.util.Date

/**
 * Firestore representation of a daily entry using the new earnings schema.
 */
data class RemoteDailyEntry(
    val id: String = "",
    val userId: String = "",
    val date: Date? = null,
    val driverId: String = "",
    val vehicleId: String = "",
    val earnings: List<RemoteEarning> = emptyList(),
    val odometer: Double? = null,
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val isSynced: Boolean = true,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

/**
 * Firestore representation of an earning breakdown for a provider.
 */
data class RemoteEarning(
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
