package com.fleetmanager.domain.repository

import android.net.Uri

/**
 * Domain repository interface for storage operations.
 */
interface StorageRepository {
    suspend fun uploadPhoto(uri: Uri, fileName: String): Result<String>
    suspend fun deletePhoto(url: String): Result<Unit>
}