package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all expenses with real-time Firestore updates.
 * This replaces single-fetch logic with snapshot listeners.
 */
class GetAllExpensesRealtimeUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<List<Expense>> {
        return repository.getAllExpensesRealtime()
    }
}