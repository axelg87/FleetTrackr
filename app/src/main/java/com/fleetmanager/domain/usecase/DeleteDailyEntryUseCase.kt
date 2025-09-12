package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for deleting a daily entry.
 */
class DeleteDailyEntryUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    
    suspend operator fun invoke(entryId: String) {
        repository.deleteDailyEntry(entryId)
    }
}