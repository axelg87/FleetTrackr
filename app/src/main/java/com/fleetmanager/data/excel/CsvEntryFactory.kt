package com.fleetmanager.data.excel

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.EarningEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import java.util.*

/**
 * Factory for creating DailyEntry objects and related entities from CSV data
 */
class CsvEntryFactory {
    
    /**
     * Creates a DailyEntry from parsed CSV row data
     * Date is already parsed from American format and converted to UTC
     */
    fun createDailyEntry(rowData: CsvRowData, userId: String): DailyEntry {
        val currentUtcTime = Date() // Current time in UTC for audit trail
        
        val earnings = buildList {
            addIfPositive("Uber", rowData.uber)
            addIfPositive("Careem", rowData.careem)
            addIfPositive("Yango", rowData.yango)
            addIfPositive("Private", rowData.private)
        }

        return DailyEntry(
            id = UUID.randomUUID().toString(),
            userId = "PLACEHOLDER", // Will be corrected by ImportManager
            date = rowData.date, // Already parsed as UTC from American CSV format
            driverId = rowData.driver.trim().lowercase(),
            driverName = rowData.driver,
            vehicleId = rowData.vehicle.trim().lowercase(),
            vehicle = rowData.vehicle,
            earnings = earnings,
            notes = "Imported from CSV",
            photoUrls = emptyList(),
            isSynced = true,
            createdAt = currentUtcTime, // Import timestamp
            updatedAt = currentUtcTime  // Import timestamp
        )
    }

    private fun MutableList<EarningEntry>.addIfPositive(provider: String, amount: Double) {
        if (amount > 0.0) {
            add(EarningEntry(provider = provider, cardEarnings = amount))
        }
    }
    
    /**
     * Creates a Driver entity for auto-creation
     */
    fun createDriver(driverName: String, userId: String): Driver {
        return Driver(
            id = UUID.randomUUID().toString(),
            name = driverName.trim(),
            isActive = true,
            userId = userId
        )
    }
    
    /**
     * Creates a Vehicle entity for auto-creation
     */
    fun createVehicle(vehicleName: String, userId: String): Vehicle {
        val parts = vehicleName.trim().split(" ", limit = 2)
        return Vehicle(
            id = UUID.randomUUID().toString(),
            make = parts.getOrNull(0) ?: vehicleName.trim(),
            model = parts.getOrNull(1) ?: "Unknown",
            year = 2020, // Default year
            licensePlate = "IMPORT-${UUID.randomUUID().toString().take(8)}",
            isActive = true,
            userId = userId
        )
    }
}