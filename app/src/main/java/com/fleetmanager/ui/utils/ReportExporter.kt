package com.fleetmanager.ui.utils

import android.content.Context
import android.os.Environment
import com.fleetmanager.ui.model.ReportEntry
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExporter @Inject constructor() {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    /**
     * Export report entries to CSV format
     * @param context Android context for file operations
     * @param entries List of report entries to export
     * @param fileName Optional custom filename (without extension)
     * @return Result containing the file path or error message
     */
    fun exportToCsv(
        context: Context,
        entries: List<ReportEntry>,
        fileName: String? = null
    ): ExportResult {
        return try {
            val actualFileName = fileName ?: "fleet_report_AED{fileNameFormatter.format(Date())}"
            val file = createCsvFile(context, actualFileName, entries)
            ExportResult.Success(file.absolutePath)
        } catch (e: IOException) {
            ExportResult.Error("Failed to export: AED{e.message}")
        } catch (e: Exception) {
            ExportResult.Error("Unexpected error: AED{e.message}")
        }
    }
    
    private fun createCsvFile(
        context: Context,
        fileName: String,
        entries: List<ReportEntry>
    ): File {
        // Use app's external files directory (doesn't require WRITE_EXTERNAL_STORAGE permission)
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        
        val file = File(externalDir, "AEDfileName.csv")
        
        FileWriter(file).use { writer ->
            // Write CSV header
            writer.append("Date,Type,Category,Driver,Vehicle,Amount,Notes\n")
            
            // Write data rows
            entries.forEach { entry ->
                writer.append(
                    "AED{dateFormatter.format(entry.date)}," +
                    "AED{if (entry.isIncome) "Income" else "Expense"}," +
                    "AED{escapeCsvValue(entry.typeDisplayName)}," +
                    "AED{escapeCsvValue(entry.driverName)}," +
                    "AED{escapeCsvValue(entry.vehicle)}," +
                    "AED{entry.amount}," +
                    "AED{escapeCsvValue(entry.notes)}\n"
                )
            }
        }
        
        return file
    }
    
    /**
     * Escape special characters in CSV values
     */
    private fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"AED{value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    /**
     * Get summary statistics for export
     */
    fun generateSummary(entries: List<ReportEntry>): ExportSummary {
        val totalIncome = entries.filter { it.isIncome }.sumOf { it.amount }
        val totalExpenses = entries.filter { !it.isIncome }.sumOf { it.amount }
        val netAmount = totalIncome - totalExpenses
        
        val byDriver = entries.groupBy { it.driverName }
            .mapValues { (_, driverEntries) ->
                driverEntries.sumOf { if (it.isIncome) it.amount else -it.amount }
            }
        
        val byType = entries.groupBy { it.typeDisplayName }
            .mapValues { (_, typeEntries) ->
                typeEntries.sumOf { if (it.isIncome) it.amount else -it.amount }
            }
        
        return ExportSummary(
            totalEntries = entries.size,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netAmount = netAmount,
            byDriver = byDriver,
            byType = byType,
            dateRange = if (entries.isNotEmpty()) {
                entries.minOf { it.date } to entries.maxOf { it.date }
            } else {
                null
            }
        )
    }
}

sealed class ExportResult {
    data class Success(val filePath: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

data class ExportSummary(
    val totalEntries: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netAmount: Double,
    val byDriver: Map<String, Double>,
    val byType: Map<String, Double>,
    val dateRange: Pair<Date, Date>?
)