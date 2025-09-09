package com.fleetmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "daily_entries")
data class DailyEntry(
    @PrimaryKey
    val id: String,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val uberEarnings: Double,
    val yangoEarnings: Double,
    val privateJobsEarnings: Double,
    val notes: String,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val totalEarnings: Double
        get() = uberEarnings + yangoEarnings + privateJobsEarnings
}