package com.fleetmanager.data.excel

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles flexible date parsing from American CSV format to UTC
 */
class CsvDateParser {
    
    companion object {
        private const val TAG = "CsvDateParser"
    }
    
    // American CSV formats (MM/dd/yyyy) - convert to UTC European format for Firestore
    private val americanDateFormats = listOf(
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("MM-dd-yyyy", Locale.US),
        SimpleDateFormat("MM.dd.yyyy", Locale.US),
        SimpleDateFormat("M/dd/yyyy", Locale.US),
        SimpleDateFormat("MM/d/yyyy", Locale.US),
        SimpleDateFormat("M/d/yyyy", Locale.US),
        SimpleDateFormat("M-dd-yyyy", Locale.US),
        SimpleDateFormat("MM-d-yyyy", Locale.US),
        SimpleDateFormat("M-d-yyyy", Locale.US),
        SimpleDateFormat("M.dd.yyyy", Locale.US),
        SimpleDateFormat("MM.d.yyyy", Locale.US),
        SimpleDateFormat("M.d.yyyy", Locale.US),
        // Short year formats
        SimpleDateFormat("MM/dd/yy", Locale.US),
        SimpleDateFormat("MM-dd-yy", Locale.US),
        SimpleDateFormat("M/d/yy", Locale.US),
        // ISO formats (fallback)
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US)
    )
    
    /**
     * Parses American format date from CSV and converts to UTC for Firestore consistency
     */
    fun parseDate(dateString: String, rowNumber: Int): DateParseResult {
        if (dateString.isBlank()) {
            return DateParseResult.Error("Row $rowNumber: Date is blank")
        }

        Log.d(TAG, "Parsing American date format: '$dateString'")
        
        for (format in americanDateFormats) {
            try {
                format.isLenient = false
                format.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC for Firestore consistency
                val parsedDate = format.parse(dateString)
                
                Log.d(TAG, "Successfully parsed '$dateString' using ${format.toPattern()} as UTC: $parsedDate")
                return DateParseResult.Success(parsedDate)
                
            } catch (e: Exception) {
                // Try next format
            }
        }

        val errorMsg = "Row $rowNumber: Invalid date format '$dateString'. Expected American format: MM/dd/yyyy (e.g., 12/25/2023)"
        Log.e(TAG, errorMsg)
        return DateParseResult.Error(errorMsg)
    }
}

sealed class DateParseResult {
    data class Success(val date: Date) : DateParseResult()
    data class Error(val message: String) : DateParseResult()
}