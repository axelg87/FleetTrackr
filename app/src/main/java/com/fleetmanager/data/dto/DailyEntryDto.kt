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
    val earnings: List<EarningDto> = emptyList(),
    val odometer: Double? = null,
    val notes: String,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val photoUrls: List<String> = emptyList(),
    val localPhotoPaths: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val totalEarnings: Double = 0.0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
