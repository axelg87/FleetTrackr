package com.fleetmanager.data.repository

import android.net.Uri
import com.fleetmanager.data.remote.StorageService
import com.fleetmanager.domain.repository.StorageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storageService: StorageService
) : StorageRepository {
    
    override suspend fun uploadPhoto(uri: Uri, fileName: String): Result<String> {
        return try {
            val url = storageService.uploadPhoto(uri, fileName)
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deletePhoto(url: String): Result<Unit> {
        return try {
            storageService.deletePhoto(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}