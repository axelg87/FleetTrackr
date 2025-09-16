package com.fleetmanager.ui.utils

/**
 * Centralized localized strings for toast messages and UI text.
 * This provides a foundation for future localization support.
 */
object LocalizedStrings {
    
    // Success Messages
    object Success {
        const val DRIVER_ADDED = "Driver added successfully"
        const val VEHICLE_ADDED = "Vehicle added successfully" 
        const val EXPENSE_TYPE_ADDED = "Expense type added successfully"
        const val CSV_IMPORT_COMPLETED = "CSV import completed successfully"
        const val INCOME_ENTRY_ADDED = "New income entry added successfully"
        const val EXPENSE_ENTRY_ADDED = "New expense entry added successfully"
    }
    
    // Error Messages
    object Error {
        const val DRIVER_ADD_FAILED = "Failed to add driver"
        const val VEHICLE_ADD_FAILED = "Failed to add vehicle"
        const val EXPENSE_TYPE_ADD_FAILED = "Failed to add expense type"
        const val CSV_IMPORT_FAILED = "CSV import failed"
        const val INCOME_ENTRY_SAVE_FAILED = "Failed to save income entry"
        const val EXPENSE_ENTRY_SAVE_FAILED = "Failed to save expense entry"
        const val PROFILE_LOAD_FAILED = "Failed to load profile"
        const val PERMISSION_DENIED = "You don't have permission to perform this action"
    }
    
    // Warning Messages
    object Warning {
        const val CSV_IMPORT_WITH_ERRORS = "CSV import completed with errors"
        const val VALIDATION_REQUIRED = "Please fill in all required fields"
    }
    
    // Info Messages
    object Info {
        const val SYNC_IN_PROGRESS = "Syncing data..."
        const val LOADING = "Loading..."
        const val SAVING = "Saving..."
    }
    
    // Profile Related
    object Profile {
        const val TITLE = "Profile"
        const val EDIT_PROFILE = "Edit Profile"
        const val FULL_NAME = "Full Name"
        const val EMAIL = "Email"
        const val ROLE = "Role"
        const val USER_ID = "User ID"
        const val NOT_PROVIDED = "Not provided"
    }
    
    // Common Actions
    object Actions {
        const val SAVE = "Save"
        const val CANCEL = "Cancel"
        const val EDIT = "Edit"
        const val DELETE = "Delete"
        const val REFRESH = "Refresh"
        const val BACK = "Back"
        const val CLOSE = "Close"
    }
}