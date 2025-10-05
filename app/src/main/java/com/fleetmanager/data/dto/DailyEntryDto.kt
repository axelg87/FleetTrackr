package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fleetmanager.domain.model.ProviderEarning
import java.util.Date

/**
 * Data Transfer Object for DailyEntry.
 * Used for Room database operations.
 * Room automatically converts List<ProviderEarning> using TypeConverter.
 */
@Entity(tableName = "daily_entries")
data class DailyEntryDto(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val date: Date,
    val driverId: String = "",
    val vehicleId: String = "",
    val providers: List<ProviderEarning> = emptyList(), // Room uses TypeConverter for this
    val notes: String,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val photoUrls: List<String> = emptyList(),
    val localPhotoPaths: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
