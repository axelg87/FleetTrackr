package com.fleetmanager.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.fleetmanager.auth.AuthService
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {
    
    companion object {
        private const val TAG = "StorageService"
    }
    
    private fun getUserStorage() = 
        storage.reference
            .child("users")
            .child(authService.getCurrentUserId() ?: "")
            .child("photos")
    
    suspend fun uploadPhoto(uri: Uri, entryId: String): String {
        val fileName = "${entryId}_${UUID.randomUUID()}.jpg"
        val photoRef = getUserStorage().child(fileName)
        
        return try {
            photoRef.putFile(uri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded photo: $fileName")
            downloadUrl
        } catch (e: Exception) {
            val errorMessage = "Failed to upload photo: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun deletePhoto(photoUrl: String) {
        try {
            storage.getReferenceFromUrl(photoUrl).delete().await()
            Log.d(TAG, "Successfully deleted photo: $photoUrl")
        } catch (e: Exception) {
            val errorMessage = "Failed to delete photo: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            // Photo might not exist, don't rethrow the exception
        }
    }
}