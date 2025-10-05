package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data Transfer Object for DailyEntry.
 * Used for Room database operations.
 */
@Entity(tableName = "daily_entries")
data class DailyEntryDto(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val date: Date,
    val driverId: String = "",
    val vehicleId: String = "",
    val odometer: Int? = null,
    val uberEarnings: Double,
    val uberHoursOnline: Double = 0.0,
    val uberCashEarnings: Double = 0.0,
    val uberCardEarnings: Double = 0.0,
    val uberTips: Double = 0.0,
    val yangoEarnings: Double,
    val yangoHoursOnline: Double = 0.0,
    val yangoCashEarnings: Double = 0.0,
    val yangoCardEarnings: Double = 0.0,
    val yangoTips: Double = 0.0,
    val privateJobsEarnings: Double,
    val privateJobsHoursOnline: Double = 0.0,
    val privateJobsCashEarnings: Double = 0.0,
    val privateJobsCardEarnings: Double = 0.0,
    val privateJobsTips: Double = 0.0,
    val notes: String,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val photoUrls: List<String> = emptyList(),
    val localPhotoPaths: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)