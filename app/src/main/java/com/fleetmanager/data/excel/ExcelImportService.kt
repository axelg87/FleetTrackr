package com.fleetmanager.data.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.opencsv.CSVReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class ExcelImportResult(
    val entries: List<DailyEntry>,
    val driversToCreate: List<Driver>,
    val vehiclesToCreate: List<Vehicle>,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Refactored CSV import service - handles American CSV format with UTC conversion
 * Separated into focused, single-responsibility classes for maintainability
 */
@Singleton
class ExcelImportService @Inject constructor(
    private val context: Context,
    private val columnMapper: CsvColumnMapper = CsvColumnMapper(),
    private val rowParser: CsvRowParser = CsvRowParser(),
    private val entryFactory: CsvEntryFactory = CsvEntryFactory()
) {
    companion object {
        private const val TAG = "ExcelImportService"
    }

    /**
     * Import CSV file with American date format and parse into DailyEntry objects
     * Dates are converted from American format (MM/dd/yyyy) to UTC for Firestore consistency
     */
    suspend fun importExcelFile(uri: Uri, userId: String): ExcelImportResult {
        Log.d(TAG, "Starting CSV import with American date format conversion to UTC")
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val entries = mutableListOf<DailyEntry>()
        val driversToCreate = mutableSetOf<Driver>()
        val vehiclesToCreate = mutableSetOf<Vehicle>()

        try {
            // Read CSV file
            val allRows = readCsvFile(uri) ?: return createErrorResult("Could not open file")
            
            if (allRows.isEmpty()) {
                return createErrorResult("CSV file is empty")
            }

            Log.d(TAG, "CSV loaded: ${allRows.size} rows, headers: ${allRows[0].joinToString(", ")}")

            // Map columns
            val columnMapping = columnMapper.mapColumns(allRows[0])
            if (!columnMapping.isValid) {
                errors.addAll(columnMapping.errors)
                return ExcelImportResult(emptyList(), emptyList(), emptyList(), errors, warnings)
            }
            
            warnings.addAll(columnMapping.warnings)
            Log.d(TAG, "Column mapping: ${columnMapping.mapping}")

            // Process data rows
            processDataRows(allRows, columnMapping, userId, entries, driversToCreate, vehiclesToCreate, errors, warnings)

            logImportSummary(allRows.size, entries.size, errors.size, warnings.size)

        } catch (e: Exception) {
            Log.e(TAG, "Error importing CSV file", e)
            errors.add("Error reading CSV file: ${e.message}")
        }

        return ExcelImportResult(
            entries = entries,
            driversToCreate = driversToCreate.toList(),
            vehiclesToCreate = vehiclesToCreate.toList(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun readCsvFile(uri: Uri): List<Array<String>>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val csvReader = CSVReader(InputStreamReader(inputStream))
            val allRows = csvReader.readAll()
            csvReader.close()
            inputStream.close()
            allRows
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV file", e)
            null
        }
    }
    
    private fun processDataRows(
        allRows: List<Array<String>>,
        columnMapping: ColumnMapping,
        userId: String,
        entries: MutableList<DailyEntry>,
        driversToCreate: MutableSet<Driver>,
        vehiclesToCreate: MutableSet<Vehicle>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        var processedRows = 0
        var skippedEmptyRows = 0
        
        for (rowIndex in 1 until allRows.size) {
            val row = allRows[rowIndex]
            val rowNumber = rowIndex + 1

            if (isEmptyRow(row)) {
                skippedEmptyRows++
                continue
            }

            processedRows++
            
            when (val result = rowParser.parseRow(row, columnMapping, rowNumber)) {
                is RowParseResult.Success -> {
                    warnings.addAll(result.warnings)
                    val entry = entryFactory.createDailyEntry(result.data, userId)
                    entries.add(entry)
                    
                    // Track entities to create
                    driversToCreate.add(entryFactory.createDriver(result.data.driver, userId))
                    vehiclesToCreate.add(entryFactory.createVehicle(result.data.vehicle, userId))
                }
                is RowParseResult.Error -> {
                    errors.addAll(result.errors)
                    warnings.addAll(result.warnings)
                }
            }
        }
        
        Log.d(TAG, "Processed $processedRows rows, skipped $skippedEmptyRows empty rows")
    }
    
    private fun createErrorResult(message: String) = ExcelImportResult(
        entries = emptyList(),
        driversToCreate = emptyList(),
        vehiclesToCreate = emptyList(),
        errors = listOf(message),
        warnings = emptyList()
    )
    
    private fun logImportSummary(totalRows: Int, entries: Int, errors: Int, warnings: Int) {
        Log.d(TAG, "Import summary: $totalRows total rows, $entries entries, $errors errors, $warnings warnings")
    }
    
    private fun isEmptyRow(row: Array<String>): Boolean {
        return row.all { it.isBlank() }
    }
}