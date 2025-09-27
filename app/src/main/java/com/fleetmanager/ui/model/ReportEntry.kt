package com.fleetmanager.ui.model

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType
import java.util.Date

/**
 * Unified data model for displaying both income and expense entries in reports.
 */
data class ReportEntry(
    val id: String,
    val type: ReportEntryType,
    val amount: Double,
    val driverName: String,
    val vehicle: String,
    val date: Date,
    val notes: String = "",
    val isIncome: Boolean
) {
    
    val displayAmount: String
        get() = if (isIncome) "+AEDAED{String.format("%.2f", amount)}" else "-AEDAED{String.format("%.2f", amount)}"
    
    val typeDisplayName: String
        get() = when (type) {
            is ReportEntryType.Income -> type.displayName
            is ReportEntryType.Expense -> type.expenseType.displayName
        }
}

sealed class ReportEntryType {
    data class Income(val displayName: String) : ReportEntryType()
    data class Expense(val expenseType: ExpenseType) : ReportEntryType()
}

/**
 * Extension functions to convert domain models to ReportEntry
 */
fun DailyEntry.toReportEntries(): List<ReportEntry> {
    val entries = mutableListOf<ReportEntry>()
    val driverLabel = driverName.ifBlank { driverId }
    val vehicleLabel = vehicle.ifBlank { vehicleId }

    if (uberEarnings > 0) {
        entries.add(
            ReportEntry(
                id = "AED{id}_uber",
                type = ReportEntryType.Income("Uber"),
                amount = uberEarnings,
                driverName = driverLabel,
                vehicle = vehicleLabel,
                date = date,
                notes = notes,
                isIncome = true
            )
        )
    }
    
    if (yangoEarnings > 0) {
        entries.add(
            ReportEntry(
                id = "AED{id}_yango",
                type = ReportEntryType.Income("Yango"),
                amount = yangoEarnings,
                driverName = driverLabel,
                vehicle = vehicleLabel,
                date = date,
                notes = notes,
                isIncome = true
            )
        )
    }
    
    if (privateJobsEarnings > 0) {
        entries.add(
            ReportEntry(
                id = "AED{id}_private",
                type = ReportEntryType.Income("Private"),
                amount = privateJobsEarnings,
                driverName = driverLabel,
                vehicle = vehicleLabel,
                date = date,
                notes = notes,
                isIncome = true
            )
        )
    }
    
    return entries
}

fun Expense.toReportEntry(): ReportEntry {
    return ReportEntry(
        id = id,
        type = ReportEntryType.Expense(type),
        amount = amount,
        driverName = driverName,
        vehicle = vehicle,
        date = date,
        notes = notes,
        isIncome = false
    )
}