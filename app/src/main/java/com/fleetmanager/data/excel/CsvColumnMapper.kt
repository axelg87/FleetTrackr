package com.fleetmanager.data.excel

import android.util.Log

/**
 * Handles intelligent CSV column mapping and detection
 */
class CsvColumnMapper {
    
    companion object {
        private const val TAG = "CsvColumnMapper"
        
        // Expected column names (case-insensitive) - flexible matching
        private val DATE_COLUMNS = listOf(
            "date", "fecha", "تاريخ", "datum", "data", "day", "jour", "dia"
        )
        private val CAREEM_COLUMNS = listOf(
            "careem", "كريم", "careem earnings", "careem_earnings"
        )
        private val UBER_COLUMNS = listOf(
            "uber", "اوبر", "uber earnings", "uber_earnings"
        )
        private val YANGO_COLUMNS = listOf(
            "yango", "يانجو", "yango earnings", "yango_earnings"
        )
        private val PRIVATE_COLUMNS = listOf(
            "private", "خاص", "private jobs", "private_jobs", "personal", "freelance"
        )
        private val DRIVER_COLUMNS = listOf(
            "driver", "سائق", "conductor", "name", "full name", "fullname", "employee"
        )
        private val VEHICLE_COLUMNS = listOf(
            "vehicle", "car", "مركبة", "سيارة", "auto", "vehicle name", "car name"
        )
    }
    
    /**
     * Maps CSV headers to standardized column names
     */
    fun mapColumns(headerRow: Array<String>): ColumnMapping {
        val mapping = mutableMapOf<String, Int>()
        val warnings = mutableListOf<String>()
        
        Log.d(TAG, "Mapping columns for headers: ${headerRow.joinToString(", ")}")

        headerRow.forEachIndexed { index, header ->
            val cleanHeader = header.lowercase().trim()
            Log.d(TAG, "Analyzing header[$index]: '$cleanHeader'")

            val columnType = when {
                matchesAny(cleanHeader, DATE_COLUMNS) -> "date"
                matchesAny(cleanHeader, CAREEM_COLUMNS) -> "careem"
                matchesAny(cleanHeader, UBER_COLUMNS) -> "uber"
                matchesAny(cleanHeader, YANGO_COLUMNS) -> "yango"
                matchesAny(cleanHeader, PRIVATE_COLUMNS) -> "private"
                matchesAny(cleanHeader, DRIVER_COLUMNS) -> "driver"
                matchesAny(cleanHeader, VEHICLE_COLUMNS) -> "vehicle"
                else -> null
            }
            
            columnType?.let {
                mapping[it] = index
                Log.d(TAG, "  -> Mapped as $it column")
            } ?: Log.d(TAG, "  -> No mapping found")
        }
        
        // Validate and generate warnings
        val validationResult = validateMapping(mapping, headerRow)
        warnings.addAll(validationResult.warnings)
        
        return ColumnMapping(mapping, validationResult.errors, warnings)
    }
    
    private fun matchesAny(header: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            header.contains(pattern.lowercase()) || pattern.lowercase().contains(header)
        }
    }
    
    private fun validateMapping(
        mapping: Map<String, Int>, 
        headerRow: Array<String>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Only date is critical
        if (!mapping.containsKey("date")) {
            errors.add("Missing critical column: date. Available: ${headerRow.joinToString(", ")}")
        }
        
        // Warn about missing recommended columns
        val recommendedColumns = listOf("driver", "vehicle")
        val missingRecommended = recommendedColumns.filter { !mapping.containsKey(it) }
        if (missingRecommended.isNotEmpty()) {
            warnings.add("Missing recommended columns: ${missingRecommended.joinToString(", ")} - will use placeholder values")
        }
        
        // Check for earnings columns
        val earningsColumns = listOf("careem", "uber", "yango", "private")
        val hasEarnings = earningsColumns.any { mapping.containsKey(it) }
        if (!hasEarnings) {
            warnings.add("No earnings columns found - all entries will have zero earnings")
        }
        
        return ValidationResult(errors, warnings)
    }
}

data class ColumnMapping(
    val mapping: Map<String, Int>,
    val errors: List<String>,
    val warnings: List<String>
) {
    val isValid: Boolean get() = errors.isEmpty()
}

private data class ValidationResult(
    val errors: List<String>,
    val warnings: List<String>
)