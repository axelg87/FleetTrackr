package com.fleetmanager.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fleetmanager.domain.repository.FleetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fleetRepository: FleetRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Sync unsynced entries to remote
            fleetRepository.syncUnsyncedEntries()
            
            // Sync unsynced expenses to remote
            fleetRepository.syncExpenses()
            
            // Fetch and cache remote data
            fleetRepository.fetchAndCacheRemoteEntries()
            fleetRepository.syncDrivers()
            fleetRepository.syncVehicles()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}