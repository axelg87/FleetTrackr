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
    val driverName: String,
    val vehicle: String,
    val uberEarnings: Double,
    val yangoEarnings: Double,
    val privateJobsEarnings: Double,
    val notes: String,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val photoUrls: List<String> = emptyList(),
    val localPhotoPaths: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)