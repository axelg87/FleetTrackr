package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all daily entries with real-time Firestore updates.
 * This replaces single-fetch logic with snapshot listeners.
 */
class GetAllEntriesRealtimeUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<List<DailyEntry>> {
        return repository.getAllDailyEntriesRealtime()
    }
}