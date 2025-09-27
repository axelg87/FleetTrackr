package com.fleetmanager.ui.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult

/**
 * Helper class for file picking operations in Compose
 */
@Composable
fun rememberExcelFilePicker(
    onFileSelected: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Validate that it's a CSV file (exported from Excel)
            val mimeType = context.contentResolver.getType(uri)
            val fileName = uri.lastPathSegment?.lowercase() ?: ""
            
            when {
                mimeType == "text/csv" ||
                mimeType == "text/comma-separated-values" ||
                fileName.endsWith(".csv") -> {
                    onFileSelected(uri)
                }
                else -> {
                    onError("Please select a valid CSV file (.csv). Export your Excel file as CSV first.")
                }
            }
        } else {
            onError("No file selected")
        }
    }
    
    return {
        try {
            launcher.launch("text/*")
        } catch (e: Exception) {
            onError("Failed to open file picker: AED{e.message}")
        }
    }
}

/**
 * Creates an intent for picking CSV files
 */
fun createExcelFilePickerIntent(): Intent {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "text/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
            "text/csv",
            "text/comma-separated-values"
        ))
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    
    return Intent.createChooser(intent, "Select CSV File")
}

/**
 * Validates if a URI points to a CSV file
 */
fun isExcelFile(uri: Uri, context: android.content.Context): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    val fileName = uri.lastPathSegment?.lowercase() ?: ""
    
    return mimeType == "text/csv" ||
           mimeType == "text/comma-separated-values" ||
           fileName.endsWith(".csv")
}