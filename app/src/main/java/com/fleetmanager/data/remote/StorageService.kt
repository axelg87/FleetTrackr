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
    
    /**
     * Verify Firebase Storage configuration and connectivity
     */
    suspend fun verifyStorageConfiguration(): Boolean {
        return try {
            val userId = requireAuth()
            val testRef = storage.reference.child("users").child(userId).child("photos")
            // Try to get metadata to verify connectivity
            testRef.metadata.await()
            Log.d(TAG, "Firebase Storage configuration verified successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage configuration verification failed", e)
            when {
                e.message?.contains("storage", ignoreCase = true) == true -> {
                    Log.e(TAG, "Firebase Storage might not be properly configured in Firebase Console")
                }
                e.message?.contains("permission", ignoreCase = true) == true -> {
                    Log.e(TAG, "Storage security rules might be preventing access")
                }
            }
            false
        }
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
            // Verify authentication first
            val userId = requireAuth()
            
            val fileName = "${entryId}_${UUID.randomUUID()}.jpg"
            val photoRef = getUserStorage().child(fileName)
            val storagePath = "users/$userId/photos/$fileName"
            
            Log.d(TAG, "Starting photo upload:")
            Log.d(TAG, "  - File name: $fileName")
            Log.d(TAG, "  - Storage path: $storagePath")
            Log.d(TAG, "  - Source URI: $uri")
            Log.d(TAG, "  - User ID: $userId")
            
            // Verify the source URI exists and is accessible
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytesAvailable = inputStream.available()
                    Log.d(TAG, "Source file size: $bytesAvailable bytes")
                    if (bytesAvailable == 0) {
                        throw IllegalArgumentException("Selected file is empty or not accessible")
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception accessing photo URI: $uri", e)
                throw IllegalArgumentException("Permission denied to access selected photo. Please grant storage permissions and try again.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing photo URI: $uri", e)
                throw IllegalArgumentException("Cannot access selected photo. Please try selecting the photo again.", e)
            }
            
            val uploadTask = photoRef.putFile(uri).await()
            Log.d(TAG, "Upload task completed successfully")
            
            val downloadUrl = photoRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded photo: $fileName")
            Log.d(TAG, "Download URL: $downloadUrl")
            return downloadUrl
        } catch (e: IllegalStateException) {
            // Authentication error - user not signed in
            val errorMessage = "Please sign in to upload photos"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        } catch (e: IllegalArgumentException) {
            // File access error
            Log.e(TAG, "File access error: ${e.message}", e)
            toastHelper.showError(context, e.message ?: "Cannot access selected photo")
            throw e
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("object doesn't exist", ignoreCase = true) == true -> 
                    "Selected photo no longer exists. Please try selecting the photo again."
                e.message?.contains("Object does not exist at location", ignoreCase = true) == true ->
                    "Selected photo cannot be found. Please try selecting the photo again."
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Permission denied. Please make sure you're signed in and try again."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection and try again."
                e.message?.contains("storage", ignoreCase = true) == true -> 
                    "Storage service unavailable. Please try again later."
                else -> "Failed to upload photo: ${e.message}"
            }
            Log.e(TAG, "Photo upload failed: $errorMessage", e)
            Log.e(TAG, "Exception details: ${e.javaClass.simpleName}")
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