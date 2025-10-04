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
        get() = if (isIncome) "+AED ${String.format("%.2f", amount)}" else "-AED ${String.format("%.2f", amount)}"
    
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

    earnings
        .filter { it.totalAmount > 0 }
        .forEach { earning ->
            val providerLabel = earning.provider.trim().ifBlank { "Earnings" }
            val safeProviderId = providerLabel
                .lowercase()
                .replace("[^a-z0-9]".toRegex(), "_")
                .replace(Regex("_+"), "_")
                .trim('_')

            entries.add(
                ReportEntry(
                    id = "${id}_${safeProviderId}",
                    type = ReportEntryType.Income(providerLabel),
                    amount = earning.totalAmount,
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
