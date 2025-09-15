# CSV Import Format Debugging Guide

## 🚨 Current Issue: 152 Parsing Errors

You're getting 152 errors during CSV parsing. Here's how to debug and fix the issue:

## 📋 Required CSV Format

### Expected Column Headers (case-insensitive):
```csv
Date,Driver,Vehicle,Careem,Uber,Yango,Private
```

### Alternative Column Names Supported:
- **Date**: `date`, `fecha`, `تاريخ`
- **Driver**: `driver`, `سائق`, `conductor`  
- **Vehicle**: `vehicle`, `car`, `مركبة`, `سيارة`, `vehiculo`
- **Careem**: `careem`, `كريم`
- **Uber**: `uber`, `اوبر`
- **Yango**: `yango`, `يانجو`
- **Private**: `private`, `خاص`, `private jobs`, `private job`

## 🔍 Common Issues & Solutions

### 1. **Column Header Issues**
**Problem**: Headers don't match expected names
```csv
❌ Wrong: Date,Name,Car,CaremAmount,UberAmount
✅ Correct: Date,Driver,Vehicle,Careem,Uber
```

### 2. **Date Format Issues**
**Problem**: Dates not in European format
```csv
❌ Wrong: 2024-01-15, 01/15/2024, 15-Jan-2024
✅ Correct: 15/01/2024, 15-01-2024, 15/1/2024
```

### 3. **Missing Required Columns**
**Required**: Date, Driver, Vehicle
**Optional**: Careem, Uber, Yango, Private

### 4. **Empty or Invalid Data**
```csv
❌ Wrong: ,Muhammad,Toyota,50.00  (missing date)
❌ Wrong: 15/01/2024,,Toyota,50.00  (missing driver)
❌ Wrong: 15/01/2024,Muhammad,,50.00  (missing vehicle)
✅ Correct: 15/01/2024,Muhammad,Toyota Camry,50.00,75.25,30.50,25.00
```

## 🛠️ How to Fix Your CSV

### Step 1: Check Column Headers
1. Open your CSV file in Excel/text editor
2. Ensure first row has: `Date,Driver,Vehicle,Careem,Uber,Yango,Private`
3. Case doesn't matter, but spelling must match supported names

### Step 2: Fix Date Format  
1. Ensure all dates are in European format: `dd/MM/yyyy` or `dd-MM-yyyy`
2. Examples: `25/12/2023`, `25-12-2023`, `5/1/2023`

### Step 3: Check Required Fields
1. Every row must have: Date, Driver, Vehicle
2. Earnings fields (Careem, Uber, etc.) can be empty or 0

### Step 4: Sample Valid CSV
```csv
Date,Driver,Vehicle,Careem,Uber,Yango,Private
25/12/2023,Muhammad Usman,Toyota Camry,50.00,75.25,30.50,25.00
26/12/2023,Ahmed Ali,Honda Civic,0.00,80.00,35.00,20.00
27/12/2023,Omar Hassan,Mitsubishi Outlander,45.50,0.00,40.25,30.00
```

## 🔍 Debugging Steps

### Check Android Logs
Look for these log messages to identify the specific issue:

1. **Column Issues**:
   ```
   Header row: [your actual headers]
   Column mapping result: {date=0, driver=1, vehicle=2}
   Missing required columns: [missing columns]
   ```

2. **Date Issues**:
   ```
   Row X: Invalid date format 'your-date'. Expected European format...
   ```

3. **Data Issues**:
   ```
   Row X: Missing driver name
   Row X: Missing vehicle name
   ```

## 🎯 Quick Fix Template

Save this as your CSV file structure:
```csv
Date,Driver,Vehicle,Careem,Uber,Yango,Private
15/01/2024,Muhammad Usman,Toyota Camry,50.00,75.25,30.50,25.00
16/01/2024,Ahmed Ali,Honda Civic,0.00,80.00,35.00,20.00
```

## 📱 What to Look For

After fixing your CSV, you should see:
- ✅ "CSV file loaded with X total rows"
- ✅ "Column mapping result: {date=0, driver=1, vehicle=2, ...}"
- ✅ "Successfully created entries: X"

Instead of:
- ❌ "Missing required columns: ..."
- ❌ "Row X: Invalid date format..."
- ❌ "CSV parsing failed with 152 errors"

## 🔧 Next Steps

1. **Check your CSV file** against the format above
2. **Fix any column header issues**
3. **Ensure dates are in European format**
4. **Make sure no required fields are empty**
5. **Try the import again**

The detailed logs will now show you exactly what's wrong with each row!