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
    val driverId: String,
    val driverName: String,
    val vehicleId: String,
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
    val isActive: Boolean,
    val salary: Double,
    val annualLicenseCost: Double,
    val annualVisaCost: Double
)

@Stable
data class UiVehicle(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val isActive: Boolean,
    val displayName: String,
    val price: Double,
    val deposit: Double?,
    val installment: Double?,
    val installmentDurationMonths: Int?,
    val serviceStartDate: Date?,
    val serviceEndDate: Date?,
    val annualInsuranceAmount: Double,
    val fuelTankCapacity: Double?,
    val fuelConsumptionPer100Km: Double?
)

/**
 * Extension functions to convert domain models to UI models.
 */
fun DailyEntry.toUiModel(dateFormatter: java.text.SimpleDateFormat): UiDailyEntry {
    return UiDailyEntry(
        id = id,
        date = date,
        driverId = driverId,
        driverName = driverName,
        vehicleId = vehicleId,
        vehicle = vehicle,
        uberEarnings = uberEarnings,
        yangoEarnings = yangoEarnings,
        privateJobsEarnings = privateJobsEarnings,
        notes = notes,
        photoUrls = photoUrls,
        isSynced = isSynced,
        totalEarnings = totalEarnings,
        formattedDate = dateFormatter.format(date),
        formattedTotalEarnings = "AEDAED{String.format("%.2f", totalEarnings)}"
    )
}

fun Driver.toUiModel(): UiDriver {
    return UiDriver(
        id = id,
        name = name,
        isActive = isActive,
        salary = salary,
        annualLicenseCost = annualLicenseCost,
        annualVisaCost = annualVisaCost
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
        displayName = displayName,
        price = price,
        deposit = deposit,
        installment = installment,
        installmentDurationMonths = installmentDurationMonths,
        serviceStartDate = serviceStartDate,
        serviceEndDate = serviceEndDate,
        annualInsuranceAmount = annualInsuranceAmount,
        fuelTankCapacity = fuelTankCapacity,
        fuelConsumptionPer100Km = fuelConsumptionPer100Km
    )
}