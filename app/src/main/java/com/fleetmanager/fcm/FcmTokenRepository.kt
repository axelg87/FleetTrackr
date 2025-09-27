package com.fleetmanager.fcm

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {
    
    companion object {
        private const val TAG = "FcmTokenRepository"
        private const val USERS_COLLECTION = "users"
        private const val FCM_TOKEN_FIELD = "fcm_token"
        private const val TOKEN_UPDATED_AT_FIELD = "fcm_token_updated_at"
    }
    
    /**
     * Get the current FCM token
     */
    suspend fun getCurrentToken(): String? {
        return try {
            val token = messaging.token.await()
            Log.d(TAG, "FCM Token retrieved: AED{token?.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }
    
    /**
     * Save FCM token to Firestore for the specified user
     */
    suspend fun saveTokenForUser(userId: String, token: String): Boolean {
        return try {
            val tokenData = mapOf(
                FCM_TOKEN_FIELD to token,
                TOKEN_UPDATED_AT_FIELD to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(tokenData)
                .await()
            
            Log.d(TAG, "FCM token saved successfully for user: AEDuserId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token for user: AEDuserId", e)
            
            // If update fails (document might not exist), try to set the data
            try {
                val tokenData = mapOf(
                    FCM_TOKEN_FIELD to token,
                    TOKEN_UPDATED_AT_FIELD to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                Log.d(TAG, "FCM token set successfully for user: AEDuserId")
                true
            } catch (setException: Exception) {
                Log.e(TAG, "Failed to set FCM token for user: AEDuserId", setException)
                false
            }
        }
    }
    
    /**
     * Get FCM token for a specific user from Firestore
     */
    suspend fun getTokenForUser(userId: String): String? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val token = document.getString(FCM_TOKEN_FIELD)
            Log.d(TAG, "Retrieved FCM token for user AEDuserId: AED{token?.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token for user: AEDuserId", e)
            null
        }
    }
    
    /**
     * Remove FCM token for a user (useful during sign out)
     */
    suspend fun removeTokenForUser(userId: String): Boolean {
        return try {
            val updates = mapOf(
                FCM_TOKEN_FIELD to null,
                TOKEN_UPDATED_AT_FIELD to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "FCM token removed for user: AEDuserId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove FCM token for user: AEDuserId", e)
            false
        }
    }
    
    /**
     * Initialize FCM token for the current user
     * This should be called after user authentication
     */
    suspend fun initializeTokenForUser(userId: String): Boolean {
        val token = getCurrentToken()
        return if (token != null) {
            saveTokenForUser(userId, token)
        } else {
            Log.w(TAG, "Cannot initialize FCM token - token is null")
            false
        }
    }
    
    /**
     * Subscribe to a topic (useful for broadcasting to all users or groups)
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            messaging.subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to topic: AEDtopic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: AEDtopic", e)
            false
        }
    }
    
    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            messaging.unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from topic: AEDtopic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic: AEDtopic", e)
            false
        }
    }
    
    /**
     * Flow to observe token changes
     */
    fun observeTokenChanges(): Flow<String?> = flow {
        try {
            val token = getCurrentToken()
            emit(token)
        } catch (e: Exception) {
            Log.e(TAG, "Error in token observation", e)
            emit(null)
        }
    }
}