package com.fleetmanager.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.fleetmanager.auth.AuthService
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage,
    private val authService: AuthService
) {
    
    private fun getUserStorage() = 
        storage.reference
            .child("users")
            .child(authService.getCurrentUserId() ?: "")
            .child("photos")
    
    suspend fun uploadPhoto(uri: Uri, entryId: String): String {
        val fileName = "${entryId}_${UUID.randomUUID()}.jpg"
        val photoRef = getUserStorage().child(fileName)
        
        photoRef.putFile(uri).await()
        return photoRef.downloadUrl.await().toString()
    }
    
    suspend fun deletePhoto(photoUrl: String) {
        try {
            storage.getReferenceFromUrl(photoUrl).delete().await()
        } catch (e: Exception) {
            // Photo might not exist, ignore error
        }
    }
}