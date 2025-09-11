package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data Transfer Object for Expense.
 * Used for Room database operations.
 */
@Entity(tableName = "expenses")
data class ExpenseDto(
    @PrimaryKey
    val id: String,
    val type: String, // ExpenseType enum as string
    val amount: Double,
    val date: Date,
    val driverName: String,
    val vehicle: String,
    val notes: String = "",
    val photoUrls: List<String> = emptyList(),
    val localPhotoPaths: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)