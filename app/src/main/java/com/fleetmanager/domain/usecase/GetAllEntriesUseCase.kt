package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all daily entries.
 * Encapsulates the business logic for retrieving entries.
 */
class GetAllEntriesUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(): Flow<List<DailyEntry>> {
        return repository.getAllDailyEntries()
    }
}