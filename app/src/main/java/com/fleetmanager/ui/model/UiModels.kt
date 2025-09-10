package com.fleetmanager.ui.model

import androidx.compose.runtime.Stable
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import java.util.Date

/**
 * Stable UI models for better Compose performance.
 * These models are optimized for UI rendering and prevent unnecessary recompositions.
 */

@Stable
data class UiDailyEntry(
    val id: String,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val uberEarnings: Double,
    val yangoEarnings: Double,
    val privateJobsEarnings: Double,
    val notes: String,
    val photoUrls: List<String>,
    val isSynced: Boolean,
    val totalEarnings: Double,
    val formattedDate: String,
    val formattedTotalEarnings: String
)

@Stable
data class UiDriver(
    val id: String,
    val name: String,
    val isActive: Boolean
)

@Stable
data class UiVehicle(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val isActive: Boolean,
    val displayName: String
)

/**
 * Extension functions to convert domain models to UI models.
 */
fun DailyEntry.toUiModel(dateFormatter: java.text.SimpleDateFormat): UiDailyEntry {
    return UiDailyEntry(
        id = id,
        date = date,
        driverName = driverName,
        vehicle = vehicle,
        uberEarnings = uberEarnings,
        yangoEarnings = yangoEarnings,
        privateJobsEarnings = privateJobsEarnings,
        notes = notes,
        photoUrls = photoUrls,
        isSynced = isSynced,
        totalEarnings = totalEarnings,
        formattedDate = dateFormatter.format(date),
        formattedTotalEarnings = "$${String.format("%.2f", totalEarnings)}"
    )
}

fun Driver.toUiModel(): UiDriver {
    return UiDriver(
        id = id,
        name = name,
        isActive = isActive
    )
}

fun Vehicle.toUiModel(): UiVehicle {
    return UiVehicle(
        id = id,
        make = make,
        model = model,
        year = year,
        licensePlate = licensePlate,
        isActive = isActive,
        displayName = displayName
    )
}