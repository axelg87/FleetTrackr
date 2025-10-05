package com.fleetmanager.data.excel

import com.fleetmanager.domain.model.DailyEntry
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
        
        // Build providers list from CSV data
        val providers = buildList {
            if (rowData.uber > 0) {
                add(
                    com.fleetmanager.domain.model.ProviderEarning(
                        type = com.fleetmanager.domain.model.ProviderType.UBER,
                        amount = rowData.uber,
                        currency = "AED"
                    )
                )
            }
            
            if (rowData.careem > 0) {
                add(
                    com.fleetmanager.domain.model.ProviderEarning(
                        type = com.fleetmanager.domain.model.ProviderType.CAREEM,
                        amount = rowData.careem,
                        currency = "AED"
                    )
                )
            }
            
            if (rowData.yango > 0) {
                add(
                    com.fleetmanager.domain.model.ProviderEarning(
                        type = com.fleetmanager.domain.model.ProviderType.YANGO,
                        amount = rowData.yango,
                        currency = "AED"
                    )
                )
            }
            
            if (rowData.private > 0) {
                add(
                    com.fleetmanager.domain.model.ProviderEarning(
                        type = com.fleetmanager.domain.model.ProviderType.PRIVATE,
                        amount = rowData.private,
                        currency = "AED"
                    )
                )
            }
        }
        
        return DailyEntry(
            id = UUID.randomUUID().toString(),
            userId = "PLACEHOLDER", // Will be corrected by ImportManager
            date = rowData.date, // Already parsed as UTC from American CSV format
            driverId = rowData.driver.trim().lowercase(),
            driverName = rowData.driver,
            vehicleId = rowData.vehicle.trim().lowercase(),
            vehicle = rowData.vehicle,
            providers = providers,
            notes = "Imported from CSV",
            photoUrls = emptyList(),
            isSynced = true,
            createdAt = currentUtcTime, // Import timestamp
            updatedAt = currentUtcTime  // Import timestamp
        )
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