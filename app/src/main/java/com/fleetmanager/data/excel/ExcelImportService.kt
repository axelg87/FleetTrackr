package com.fleetmanager.data.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.opencsv.CSVReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class ExcelImportResult(
    val entries: List<DailyEntry>,
    val driversToCreate: List<Driver>,
    val vehiclesToCreate: List<Vehicle>,
    val errors: List<String>,
    val warnings: List<String>
)

data class ExcelRowData(
    val rowNumber: Int,
    val date: Date?,
    val careem: Double?,
    val uber: Double?,
    val yango: Double?,
    val private: Double?,
    val driver: String?,
    val vehicle: String?
)

@Singleton
class ExcelImportService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ExcelImportService"
        
        // Expected column names (case-insensitive)
        private val DATE_COLUMNS = listOf("date", "fecha", "تاريخ")
        private val CAREEM_COLUMNS = listOf("careem", "كريم")
        private val UBER_COLUMNS = listOf("uber", "اوبر")
        private val YANGO_COLUMNS = listOf("yango", "يانجو")
        private val PRIVATE_COLUMNS = listOf("private", "خاص", "private jobs", "private job")
        private val DRIVER_COLUMNS = listOf("driver", "سائق", "conductor")
        private val VEHICLE_COLUMNS = listOf("vehicle", "car", "مركبة", "سيارة", "vehiculo")
    }

    /**
     * Import CSV file and parse it into DailyEntry objects
     * @param uri The URI of the CSV file
     * @param userId The current user ID
     * @return ExcelImportResult containing entries, entities to create, and errors
     */
    suspend fun importExcelFile(uri: Uri, userId: String): ExcelImportResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val entries = mutableListOf<DailyEntry>()
        val driversToCreate = mutableSetOf<Driver>()
        val vehiclesToCreate = mutableSetOf<Vehicle>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ExcelImportResult(
                    entries = emptyList(),
                    driversToCreate = emptyList(),
                    vehiclesToCreate = emptyList(),
                    errors = listOf("Could not open file"),
                    warnings = emptyList()
                )

            val csvReader = CSVReader(InputStreamReader(inputStream))
            val allRows = csvReader.readAll()
            
            if (allRows.isEmpty()) {
                return ExcelImportResult(
                    entries = emptyList(),
                    driversToCreate = emptyList(),
                    vehiclesToCreate = emptyList(),
                    errors = listOf("CSV file is empty"),
                    warnings = warnings
                )
            }

            // Find column indices from header row
            val columnMapping = findColumnMapping(allRows[0], errors)
            if (columnMapping.isEmpty()) {
                return ExcelImportResult(
                    entries = emptyList(),
                    driversToCreate = emptyList(),
                    vehiclesToCreate = emptyList(),
                    errors = errors + "Could not find required columns in CSV file",
                    warnings = warnings
                )
            }

            // Process data rows (skip header)
            for (rowIndex in 1 until allRows.size) {
                val row = allRows[rowIndex]
                val rowNumber = rowIndex + 1

                // Skip empty rows
                if (isEmptyRow(row)) {
                    continue
                }

                val rowData = parseRow(row, columnMapping, rowNumber, errors, warnings)
                if (rowData != null) {
                    val entry = createDailyEntry(rowData, userId, errors, warnings)
                    if (entry != null) {
                        entries.add(entry)
                        
                        // Track drivers and vehicles to create
                        rowData.driver?.let { driverName ->
                            if (driverName.isNotBlank()) {
                                driversToCreate.add(
                                    Driver(
                                        id = UUID.randomUUID().toString(),
                                        name = driverName.trim(),
                                        isActive = true,
                                        userId = userId
                                    )
                                )
                            }
                        }
                        
                        rowData.vehicle?.let { vehicleName ->
                            if (vehicleName.isNotBlank()) {
                                vehiclesToCreate.add(
                                    Vehicle(
                                        id = UUID.randomUUID().toString(),
                                        make = vehicleName.trim().split(" ").getOrNull(0) ?: vehicleName.trim(),
                                        model = vehicleName.trim().split(" ").drop(1).joinToString(" ").ifEmpty { "Unknown" },
                                        year = 2020, // Default year
                                        licensePlate = "IMPORT-${UUID.randomUUID().toString().take(8)}",
                                        isActive = true,
                                        userId = userId
                                    )
                                )
                            }
                        }
                    }
                }
            }

            csvReader.close()
            inputStream.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error importing Excel file", e)
            errors.add("Error reading Excel file: ${e.message}")
        }

        return ExcelImportResult(
            entries = entries,
            driversToCreate = driversToCreate.toList(),
            vehiclesToCreate = vehiclesToCreate.toList(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun findColumnMapping(headerRow: Array<String>, errors: MutableList<String>): Map<String, Int> {
        val columnMapping = mutableMapOf<String, Int>()

        for (cellIndex in headerRow.indices) {
            val headerValue = headerRow[cellIndex].lowercase().trim()

            when {
                DATE_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["date"] = cellIndex
                CAREEM_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["careem"] = cellIndex
                UBER_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["uber"] = cellIndex
                YANGO_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["yango"] = cellIndex
                PRIVATE_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["private"] = cellIndex
                DRIVER_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["driver"] = cellIndex
                VEHICLE_COLUMNS.any { it.equals(headerValue, ignoreCase = true) } -> 
                    columnMapping["vehicle"] = cellIndex
            }
        }

        // Validate required columns
        val requiredColumns = listOf("date", "driver", "vehicle")
        val missingColumns = requiredColumns.filter { !columnMapping.containsKey(it) }
        if (missingColumns.isNotEmpty()) {
            errors.add("Missing required columns: ${missingColumns.joinToString(", ")}")
        }

        return columnMapping
    }

    private fun parseRow(
        row: Array<String>,
        columnMapping: Map<String, Int>,
        rowNumber: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): ExcelRowData? {
        try {
            val date = columnMapping["date"]?.let { colIndex ->
                if (colIndex < row.size) {
                    parseDate(row[colIndex], rowNumber, errors)
                } else null
            }

            val careem = columnMapping["careem"]?.let { colIndex ->
                if (colIndex < row.size) {
                    parseDouble(row[colIndex], "Careem", rowNumber, errors)
                } else 0.0
            } ?: 0.0

            val uber = columnMapping["uber"]?.let { colIndex ->
                if (colIndex < row.size) {
                    parseDouble(row[colIndex], "Uber", rowNumber, errors)
                } else 0.0
            } ?: 0.0

            val yango = columnMapping["yango"]?.let { colIndex ->
                if (colIndex < row.size) {
                    parseDouble(row[colIndex], "Yango", rowNumber, errors)
                } else 0.0
            } ?: 0.0

            val private = columnMapping["private"]?.let { colIndex ->
                if (colIndex < row.size) {
                    parseDouble(row[colIndex], "Private", rowNumber, errors)
                } else 0.0
            } ?: 0.0

            val driver = columnMapping["driver"]?.let { colIndex ->
                if (colIndex < row.size) row[colIndex].trim() else null
            }

            val vehicle = columnMapping["vehicle"]?.let { colIndex ->
                if (colIndex < row.size) row[colIndex].trim() else null
            }

            // Validate required fields
            if (date == null) {
                errors.add("Row $rowNumber: Invalid or missing date")
                return null
            }

            if (driver.isNullOrBlank()) {
                errors.add("Row $rowNumber: Missing driver name")
                return null
            }

            if (vehicle.isNullOrBlank()) {
                errors.add("Row $rowNumber: Missing vehicle name")
                return null
            }

            // Check if all earnings are zero
            if (careem == 0.0 && uber == 0.0 && yango == 0.0 && private == 0.0) {
                warnings.add("Row $rowNumber: All earnings are zero")
            }

            return ExcelRowData(
                rowNumber = rowNumber,
                date = date,
                careem = careem,
                uber = uber,
                yango = yango,
                private = private,
                driver = driver,
                vehicle = vehicle
            )

        } catch (e: Exception) {
            errors.add("Row $rowNumber: Error parsing row - ${e.message}")
            return null
        }
    }

    private fun createDailyEntry(
        rowData: ExcelRowData,
        userId: String,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): DailyEntry? {
        try {
            val entry = DailyEntry(
                id = UUID.randomUUID().toString(),
                userId = userId,
                date = rowData.date!!,
                driverName = rowData.driver!!,
                vehicle = rowData.vehicle!!,
                uberEarnings = rowData.uber ?: 0.0,
                yangoEarnings = rowData.yango ?: 0.0,
                privateJobsEarnings = rowData.private ?: 0.0,
                careemEarnings = rowData.careem ?: 0.0,
                notes = "Imported from Excel",
                photoUrls = emptyList(),
                isSynced = true,
                createdAt = rowData.date!!,
                updatedAt = rowData.date!!
            )

            // Validate entry
            if (!entry.isValid()) {
                val validationErrors = entry.getValidationErrors()
                errors.add("Row ${rowData.rowNumber}: ${validationErrors.joinToString(", ")}")
                return null
            }

            return entry

        } catch (e: Exception) {
            errors.add("Row ${rowData.rowNumber}: Error creating entry - ${e.message}")
            return null
        }
    }

    private fun parseDate(dateString: String, rowNumber: Int, errors: MutableList<String>): Date? {
        if (dateString.isBlank()) return null

        val dateFormats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        )

        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(dateString)
            } catch (e: Exception) {
                // Try next format
            }
        }

        errors.add("Row $rowNumber: Invalid date format '$dateString'. Expected formats: dd/MM/yyyy or yyyy-MM-dd")
        return null
    }

    private fun parseDouble(value: String, fieldName: String, rowNumber: Int, errors: MutableList<String>): Double? {
        if (value.isBlank()) return 0.0

        return try {
            // Remove common currency symbols and spaces
            val cleanValue = value.replace(Regex("[^\\d.-]"), "")
            val doubleValue = cleanValue.toDouble()
            
            if (doubleValue < 0) {
                errors.add("Row $rowNumber: $fieldName cannot be negative ($doubleValue)")
                return null
            }
            
            if (doubleValue > 999999.99) {
                errors.add("Row $rowNumber: $fieldName value too large ($doubleValue)")
                return null
            }
            
            doubleValue
        } catch (e: Exception) {
            errors.add("Row $rowNumber: Invalid $fieldName value '$value'")
            null
        }
    }

    private fun isEmptyRow(row: Array<String>): Boolean {
        return row.all { it.isBlank() }
    }
}