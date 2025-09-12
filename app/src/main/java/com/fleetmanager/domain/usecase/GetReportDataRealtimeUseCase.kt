package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case for getting unified report data with real-time Firestore updates.
 * Combines daily entries and expenses using snapshot listeners.
 */
class GetReportDataRealtimeUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    data class ReportData(
        val dailyEntries: List<DailyEntry>,
        val expenses: List<Expense>
    )
    
    operator fun invoke(): Flow<ReportData> {
        return combine(
            repository.getAllDailyEntriesRealtime(),
            repository.getAllExpensesRealtime()
        ) { dailyEntries, expenses ->
            ReportData(
                dailyEntries = dailyEntries,
                expenses = expenses
            )
        }
    }
}