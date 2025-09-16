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

/**
 * Firebase Storage Service for photo uploads
 * 
 * FIREBASE SETUP REQUIRED:
 * ========================
 * 
 * 1. ENABLE FIREBASE STORAGE:
 *    - Go to Firebase Console (https://console.firebase.google.com)
 *    - Select your project: "ag-motion"
 *    - Navigate to "Storage" in the left sidebar
 *    - If not enabled, click "Get started"
 * 
 * 2. STORAGE SECURITY RULES:
 *    Replace the default rules with these rules in Firebase Console → Storage → Rules:
 *    
 *    ```
 *    rules_version = '2';
 *    service firebase.storage {
 *      match /b/{bucket}/o {
 *        // Users can only access their own photos
 *        match /users/{userId}/photos/{allPaths=**} {
 *          allow read, write: if request.auth != null && request.auth.uid == userId;
 *        }
 *      }
 *    }
 *    ```
 * 
 * 3. STORAGE STRUCTURE:
 *    Photos are stored as: users/{userId}/photos/{filename}.jpg
 *    - Each user has their own isolated folder
 *    - Filenames are unique with UUID to prevent conflicts
 * 
 * 4. AUTHENTICATION REQUIRED:
 *    - User must be signed in to upload/access photos
 *    - Storage operations will fail with clear error if not authenticated
 */
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
            .child(requireAuth())
            .child("photos")
    
    private fun requireAuth(): String {
        val userId = authService.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            Log.e(TAG, "User not authenticated! Cannot perform Storage operations.")
            throw IllegalStateException("User must be authenticated to access Firebase Storage")
        }
        Log.d(TAG, "User authenticated with ID: $userId")
        return userId
    }
    
    suspend fun uploadPhoto(uri: Uri, entryId: String): String {
        try {
            val fileName = "${entryId}_${UUID.randomUUID()}.jpg"
            val photoRef = getUserStorage().child(fileName)
            
            Log.d(TAG, "Starting photo upload: $fileName")
            photoRef.putFile(uri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded photo: $fileName")
            return downloadUrl
        } catch (e: IllegalStateException) {
            // Authentication error - user not signed in
            val errorMessage = "Please sign in to upload photos"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("object doesn't exist", ignoreCase = true) == true -> 
                    "Storage location not accessible. Please check your internet connection and try again."
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permission denied. Please make sure you're signed in."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection."
                else -> "Failed to upload photo: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun uploadProfilePicture(uri: Uri): String {
        try {
            val fileName = "profile_picture_${UUID.randomUUID()}.jpg"
            val photoRef = getUserStorage().child("profile").child(fileName)
            
            Log.d(TAG, "Starting profile picture upload: $fileName")
            photoRef.putFile(uri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded profile picture: $fileName")
            return downloadUrl
        } catch (e: IllegalStateException) {
            // Authentication error - user not signed in
            val errorMessage = "Please sign in to upload profile picture"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("object doesn't exist", ignoreCase = true) == true -> 
                    "Storage location not accessible. Please check your internet connection and try again."
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permission denied. Please make sure you're signed in."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection."
                else -> "Failed to upload profile picture: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun deletePhoto(photoUrl: String) {
        try {
            // Verify authentication before attempting delete
            requireAuth()
            storage.getReferenceFromUrl(photoUrl).delete().await()
            Log.d(TAG, "Successfully deleted photo: $photoUrl")
        } catch (e: IllegalStateException) {
            // Authentication error - user not signed in
            val errorMessage = "Please sign in to delete photos"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            // Don't rethrow - photo deletion is not critical
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("object doesn't exist", ignoreCase = true) == true -> 
                    "Photo already deleted or doesn't exist"
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permission denied. Please make sure you're signed in."
                else -> "Failed to delete photo: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            // Don't show toast for photo deletion errors - they're not critical
            // Photo might not exist, don't rethrow the exception
        }
    }
}