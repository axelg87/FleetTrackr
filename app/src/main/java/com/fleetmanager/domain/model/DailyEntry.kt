package com.fleetmanager.domain.model

import java.util.Date

/**
 * Domain model for daily entry.
 * This represents the business entity without any framework dependencies.
 */
data class DailyEntry(
    val id: String,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val uberEarnings: Double,
    val yangoEarnings: Double,
    val privateJobsEarnings: Double,
    val notes: String,
    val photoUrls: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val totalEarnings: Double
        get() = uberEarnings + yangoEarnings + privateJobsEarnings

    fun isValid(): Boolean {
        return id.isNotBlank() &&
                driverName.isNotBlank() &&
                vehicle.isNotBlank() &&
                uberEarnings >= 0 &&
                yangoEarnings >= 0 &&
                privateJobsEarnings >= 0
    }
}