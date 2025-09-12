package com.fleetmanager.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {
    
    companion object {
        private const val TAG = "UserFirestoreService"
        private const val USERS_COLLECTION = "users"
    }
    
    private fun getCollection() = firestore.collection(USERS_COLLECTION)
    
    // Get user profile from Firestore
    fun getUserProfile(userId: String): Flow<UserDto> {
        return getCollection()
            .document(userId)
            .snapshots()
            .map { document ->
                if (document.exists()) {
                    UserDto(
                        id = document.id,
                        name = document.getString("displayName") ?: document.getString("name") ?: "Unknown User",
                        role = try {
                            UserRole.valueOf((document.getString("role") ?: "DRIVER").uppercase())
                        } catch (e: Exception) {
                            UserRole.DRIVER
                        }
                    )
                } else {
                    // Default user profile if document doesn't exist
                    UserDto(
                        id = userId,
                        name = "Unknown User",
                        role = UserRole.DRIVER
                    )
                }
            }
    }
    
    // Get current user's profile
    fun getCurrentUserProfile(): Flow<UserDto> {
        val userId = authService.getCurrentUserId() ?: ""
        return getUserProfile(userId)
    }
    
    // Simple method to get current user's role (default to DRIVER if not found)
    suspend fun getCurrentUserRole(): UserRole {
        val userId = authService.getCurrentUserId() ?: return UserRole.DRIVER
        return try {
            val userDoc = getCollection().document(userId).get().await()
            val roleString = userDoc.getString("role") ?: "DRIVER"
            UserRole.valueOf(roleString.uppercase())
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch user role, defaulting to DRIVER", e)
            UserRole.DRIVER
        }
    }
    
    // Create user document if it doesn't exist (called on first sign-in)
    suspend fun saveUserIfMissing() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found")
            return
        }
        
        val userId = currentUser.uid
        Log.d(TAG, "Checking user document for: $userId")
        
        try {
            val userDoc = getCollection().document(userId).get().await()
            
            if (!userDoc.exists()) {
                Log.d(TAG, "Creating new user document for: $userId")
                
                val userData = mapOf(
                    "id" to userId,
                    "name" to (currentUser.displayName ?: "Unknown User"),
                    "role" to UserRole.DRIVER.name,
                    "email" to (currentUser.email ?: ""),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                getCollection()
                    .document(userId)
                    .set(userData)
                    .await()
                
                Log.d(TAG, "✅ User document created successfully for: $userId")
                toastHelper.showMessage(context, "✅ User profile created")
            } else {
                Log.d(TAG, "User document already exists for: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create user document: ${e.message}", e)
            toastHelper.showError(context, "❌ Failed to create user: ${e.message}")
            throw e
        }
    }
    
    // Get drivers from the 'drivers' collection
    suspend fun getDriverUsers(): List<UserDto> {
        return try {
            firestore.collection("drivers")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: "Unknown Driver",
                            role = UserRole.DRIVER
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse driver: ${document.id}", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch drivers: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getDriverUsersFlow(): Flow<List<UserDto>> {
        return firestore.collection("drivers")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    try {
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: "Unknown Driver",
                            role = UserRole.DRIVER
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse driver: ${document.id}", e)
                        null
                    }
                }
            }
    }
    
    // Admin-only method for creating new drivers
    suspend fun createDriverUser(name: String, email: String = ""): UserDto {
        val driverId = java.util.UUID.randomUUID().toString()
        val driverData = mapOf(
            "id" to driverId,
            "name" to name,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("drivers")
            .document(driverId)
            .set(driverData)
            .await()
        
        return UserDto(
            id = driverId,
            name = name,
            role = UserRole.DRIVER
        )
    }
}