package com.fleetmanager.data.excel

import android.util.Log
import java.util.*

/**
 * Handles parsing individual CSV rows into structured data
 */
class CsvRowParser(
    private val dateParser: CsvDateParser = CsvDateParser()
) {
    
    companion object {
        private const val TAG = "CsvRowParser"
    }
    
    /**
     * Parses a CSV row into structured data
     */
    fun parseRow(
        row: Array<String>,
        columnMapping: ColumnMapping,
        rowNumber: Int
    ): RowParseResult {
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            Log.d(TAG, "Parsing row $rowNumber: ${row.joinToString(", ")}")
            
            // Parse date (critical field)
            val dateResult = columnMapping.mapping["date"]?.let { colIndex ->
                if (colIndex < row.size) {
                    dateParser.parseDate(row[colIndex], rowNumber)
                } else {
                    DateParseResult.Error("Row $rowNumber: Date column index out of bounds")
                }
            } ?: DateParseResult.Error("Row $rowNumber: No date column mapped")
            
            val date = when (dateResult) {
                is DateParseResult.Success -> dateResult.date
                is DateParseResult.Error -> {
                    errors.add(dateResult.message)
                    return RowParseResult.Error(errors, warnings)
                }
            }
            
            // Parse earnings (optional fields)
            val careem = parseDouble(row, columnMapping.mapping["careem"], "Careem", rowNumber, errors)
            val uber = parseDouble(row, columnMapping.mapping["uber"], "Uber", rowNumber, errors)
            val yango = parseDouble(row, columnMapping.mapping["yango"], "Yango", rowNumber, errors)
            val private = parseDouble(row, columnMapping.mapping["private"], "Private", rowNumber, errors)
            
            // Parse driver (use placeholder if missing)
            val driver = columnMapping.mapping["driver"]?.let { colIndex ->
                if (colIndex < row.size) row[colIndex].trim() else null
            }
            
            val finalDriver = if (driver.isNullOrBlank()) {
                warnings.add("Row $rowNumber: Missing driver name, using 'Unknown Driver'")
                "Unknown Driver"
            } else driver
            
            // Parse vehicle (use placeholder if missing)
            val vehicle = columnMapping.mapping["vehicle"]?.let { colIndex ->
                if (colIndex < row.size) row[colIndex].trim() else null
            }
            
            val finalVehicle = if (vehicle.isNullOrBlank()) {
                warnings.add("Row $rowNumber: Missing vehicle name, using 'Unknown Vehicle'")
                "Unknown Vehicle"
            } else vehicle
            
            // Check if all earnings are zero
            if (careem == 0.0 && uber == 0.0 && yango == 0.0 && private == 0.0) {
                warnings.add("Row $rowNumber: All earnings are zero")
            }
            
            val rowData = CsvRowData(
                rowNumber = rowNumber,
                date = date,
                careem = careem,
                uber = uber,
                yango = yango,
                private = private,
                driver = finalDriver,
                vehicle = finalVehicle
            )
            
            return RowParseResult.Success(rowData, warnings)
            
        } catch (e: Exception) {
            val errorMsg = "Row $rowNumber: Error parsing row - ${e.message}"
            Log.e(TAG, errorMsg, e)
            errors.add(errorMsg)
            return RowParseResult.Error(errors, warnings)
        }
    }
    
    private fun parseDouble(
        row: Array<String>,
        colIndex: Int?,
        fieldName: String,
        rowNumber: Int,
        errors: MutableList<String>
    ): Double {
        if (colIndex == null || colIndex >= row.size) return 0.0
        
        val value = row[colIndex].trim()
        if (value.isBlank()) return 0.0

        return try {
            // Remove common currency symbols and spaces
            val cleanValue = value.replace(Regex("[^\\d.-]"), "")
            val doubleValue = cleanValue.toDouble()
            
            when {
                doubleValue < 0 -> {
                    errors.add("Row $rowNumber: $fieldName cannot be negative ($doubleValue)")
                    0.0
                }
                doubleValue > 999999.99 -> {
                    errors.add("Row $rowNumber: $fieldName value too large ($doubleValue)")
                    0.0
                }
                else -> doubleValue
            }
        } catch (e: Exception) {
            errors.add("Row $rowNumber: Invalid $fieldName value '$value'")
            0.0
        }
    }
}

data class CsvRowData(
    val rowNumber: Int,
    val date: Date,
    val careem: Double,
    val uber: Double,
    val yango: Double,
    val private: Double,
    val driver: String,
    val vehicle: String
)

sealed class RowParseResult {
    data class Success(val data: CsvRowData, val warnings: List<String>) : RowParseResult()
    data class Error(val errors: List<String>, val warnings: List<String>) : RowParseResult()
}