package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting a daily entry by ID.
 */
class GetEntryByIdUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    operator fun invoke(entryId: String): Flow<DailyEntry?> {
        require(entryId.isNotBlank()) { "Entry ID cannot be blank" }
        return repository.getDailyEntryById(entryId)
    }
}