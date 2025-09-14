# Excel Import Feature Implementation

## Overview
The Excel import feature allows administrators to import past income data from Excel files (.xlsx or .xls) into the Fleet Manager app. This feature includes automatic creation of drivers and vehicles, support for legacy Careem earnings, and comprehensive error handling.

## Implementation Summary

### 1. Core Components Added

#### ExcelImportService (`/data/excel/ExcelImportService.kt`)
- **Purpose**: Handles Excel file parsing and data extraction
- **Features**:
  - Supports both .xlsx and .xls formats using Apache POI
  - Multi-language column detection (English, Arabic, Spanish)
  - Flexible date format parsing (dd/mm/yyyy, yyyy-mm-dd, etc.)
  - Comprehensive validation and error reporting
  - Handles empty rows and invalid data gracefully

#### ExcelImportManager (`/data/excel/ExcelImportManager.kt`)  
- **Purpose**: Orchestrates the import process with Firestore integration
- **Features**:
  - Progress tracking with detailed status updates
  - Automatic driver and vehicle creation
  - Permission-based access control (Admin only)
  - Batch processing with error recovery
  - Real-time progress callbacks

#### FilePickerHelper (`/ui/utils/FilePickerHelper.kt`)
- **Purpose**: Provides file selection utilities for Compose UI
- **Features**:
  - Excel file validation
  - Error handling for invalid file types
  - Compose-friendly file picker integration

### 2. Database Schema Updates

#### DailyEntry Model Enhanced
Added support for Careem earnings:
```kotlin
@get:PropertyName("careemEarnings")
val careemEarnings: Double = 0.0
```

Updated total earnings calculation:
```kotlin
val totalEarnings: Double
    get() = uberEarnings + yangoEarnings + privateJobsEarnings + careemEarnings
```

### 3. UI Components

#### Admin Control Section
- Added "Import Excel Entries" button in Settings screen
- Only visible to users with ADMIN role
- Integrated with file picker for seamless user experience

#### Import Progress Dialog
- Real-time progress tracking with percentage and step descriptions
- Error and warning display
- Dismissible when complete
- Professional Material 3 design

### 4. Expected Excel File Format

The import supports Excel files with the following columns (case-insensitive):

| Column | Alternative Names | Required | Type | Description |
|--------|------------------|----------|------|-------------|
| Date | fecha, تاريخ | Yes | Date | dd/mm/yyyy or yyyy-mm-dd |
| Careem | كريم | No | Number | Legacy earnings (import only) |
| Uber | اوبر | No | Number | Uber earnings |
| Yango | يانجو | No | Number | Yango earnings |
| Private | خاص, private jobs | No | Number | Private job earnings |
| Driver | سائق, conductor | Yes | Text | Driver name |
| Vehicle | car, مركبة, سيارة | Yes | Text | Vehicle identifier |

### 5. Import Process Flow

1. **File Selection**: User selects Excel file through file picker
2. **Validation**: File format and structure validation
3. **Parsing**: Extract data with comprehensive error checking
4. **Entity Creation**: Automatically create missing drivers/vehicles
5. **Data Import**: Save entries to Firestore with progress tracking
6. **Completion**: Show summary with success/error counts

### 6. Error Handling

#### File-Level Errors
- Invalid file format
- Missing required columns
- Corrupted Excel files

#### Row-Level Errors  
- Invalid date formats
- Missing required fields (Date, Driver, Vehicle)
- Negative earnings values
- Values exceeding limits

#### System-Level Errors
- Permission denied
- Firestore connection issues
- Authentication failures

### 7. Key Features

#### Careem Legacy Support
- Careem earnings are imported and stored in `careemEarnings` field
- Included in total earnings calculations
- **Not displayed in manual "Add Income" form** (import-only)

#### Automatic Entity Creation
- Creates Driver records for unknown driver names
- Creates Vehicle records for unknown vehicles
- Prevents duplicates through name-based matching

#### Multi-Language Support
- Column headers in English, Arabic, and Spanish
- Flexible date format parsing
- Unicode text support

#### Permission System
- Only ADMIN users can import Excel files
- Role-based access control integrated
- Security validation at multiple levels

### 8. Dependencies Added

```kotlin
// Excel parsing
implementation("org.apache.poi:poi:5.2.4")
implementation("org.apache.poi:poi-ooxml:5.2.4")
```

### 9. Usage Instructions

1. **Admin Access**: Ensure user has ADMIN role
2. **Navigate**: Go to Settings > Admin Controls
3. **Import**: Tap "Import Excel Entries" button
4. **Select File**: Choose Excel file (.xlsx or .xls)
5. **Monitor**: Watch progress dialog for real-time updates
6. **Review**: Check completion status and any errors/warnings

### 10. Technical Notes

#### Performance Considerations
- Processes files in memory (suitable for typical fleet data volumes)
- Progress callbacks prevent UI blocking
- Batch Firestore operations for efficiency

#### Data Integrity
- All entries marked as `isSynced: true`
- Creation and update timestamps set to import date
- Comprehensive validation before database insertion

#### Error Recovery
- Continues processing after individual row errors
- Detailed error reporting for troubleshooting
- Partial imports supported (successful rows are saved)

## Implementation Complete

The Excel import feature is now fully implemented and ready for use. The system provides a robust, user-friendly way to import historical income data while maintaining data integrity and providing comprehensive error handling.