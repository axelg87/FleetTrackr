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
            // Validate that it's an Excel file
            val mimeType = context.contentResolver.getType(uri)
            val fileName = uri.lastPathSegment?.lowercase() ?: ""
            
            when {
                mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                mimeType == "application/vnd.ms-excel" ||
                fileName.endsWith(".xlsx") ||
                fileName.endsWith(".xls") -> {
                    onFileSelected(uri)
                }
                else -> {
                    onError("Please select a valid Excel file (.xlsx or .xls)")
                }
            }
        } else {
            onError("No file selected")
        }
    }
    
    return {
        try {
            launcher.launch("application/*")
        } catch (e: Exception) {
            onError("Failed to open file picker: ${e.message}")
        }
    }
}

/**
 * Creates an intent for picking Excel files
 */
fun createExcelFilePickerIntent(): Intent {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel" // .xls
        ))
        addCategory(Intent.CATEGORY_OPENABLE)
    }
    
    return Intent.createChooser(intent, "Select Excel File")
}

/**
 * Validates if a URI points to an Excel file
 */
fun isExcelFile(uri: Uri, context: android.content.Context): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    val fileName = uri.lastPathSegment?.lowercase() ?: ""
    
    return mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
           mimeType == "application/vnd.ms-excel" ||
           fileName.endsWith(".xlsx") ||
           fileName.endsWith(".xls")
}