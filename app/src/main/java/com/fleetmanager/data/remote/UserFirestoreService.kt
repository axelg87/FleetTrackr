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
                        email = document.getString("email") ?: "",
                        role = try {
                            UserRole.valueOf((document.getString("role") ?: "DRIVER").uppercase())
                        } catch (e: Exception) {
                            UserRole.DRIVER
                        },
                        profilePictureUrl = document.getString("profilePictureUrl")
                    )
                } else {
                    // Default user profile if document doesn't exist
                    UserDto(
                        id = userId,
                        name = "Unknown User",
                        email = "",
                        role = UserRole.DRIVER,
                        profilePictureUrl = null
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
    
    // Get all users except ADMIN (so DRIVER and MANAGER users)
    suspend fun getDriverUsers(): List<UserDto> {
        return try {
            getCollection()
                .whereIn("role", listOf(UserRole.DRIVER.name, UserRole.MANAGER.name))
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    try {
                        val roleString = document.getString("role") ?: "DRIVER"
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("displayName") ?: "Unknown User",
                            email = document.getString("email") ?: "",
                            role = try {
                                UserRole.valueOf(roleString.uppercase())
                            } catch (e: Exception) {
                                UserRole.DRIVER
                            },
                            profilePictureUrl = document.getString("profilePictureUrl")
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse user: ${document.id}", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch users: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getDriverUsersFlow(): Flow<List<UserDto>> {
        return getCollection()
            .whereIn("role", listOf(UserRole.DRIVER.name, UserRole.MANAGER.name))
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    try {
                        val roleString = document.getString("role") ?: "DRIVER"
                        UserDto(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("displayName") ?: "Unknown User",
                            email = document.getString("email") ?: "",
                            role = try {
                                UserRole.valueOf(roleString.uppercase())
                            } catch (e: Exception) {
                                UserRole.DRIVER
                            },
                            profilePictureUrl = document.getString("profilePictureUrl")
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse user: ${document.id}", e)
                        null
                    }
                }
            }
    }
    
    // Admin-only method for creating new driver users in users collection
    suspend fun createDriverUser(name: String, email: String = ""): UserDto {
        val userId = java.util.UUID.randomUUID().toString()
        val userData = mapOf(
            "id" to userId,
            "name" to name,
            "displayName" to name, // For compatibility
            "email" to email,
            "role" to UserRole.DRIVER.name,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        getCollection()
            .document(userId)
            .set(userData)
            .await()
        
        Log.d(TAG, "✅ Created new driver user: $name ($userId)")
        
        return UserDto(
            id = userId,
            name = name,
            email = email,
            role = UserRole.DRIVER,
            profilePictureUrl = null
        )
    }
    
    // Update user profile information (name and email)
    suspend fun updateUserProfile(name: String, email: String) {
        val userId = authService.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        
        try {
            val updateData = mapOf(
                "name" to name,
                "displayName" to name, // For compatibility
                "email" to email,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            getCollection()
                .document(userId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "✅ User profile updated successfully for: $userId")
            toastHelper.showMessage(context, "✅ Profile updated successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update user profile: ${e.message}", e)
            toastHelper.showError(context, "❌ Failed to update profile: ${e.message}")
            throw e
        }
    }
    
    // Update user profile picture URL
    suspend fun updateProfilePicture(profilePictureUrl: String) {
        val userId = authService.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        
        try {
            val updateData = mapOf(
                "profilePictureUrl" to profilePictureUrl,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            getCollection()
                .document(userId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "✅ Profile picture updated successfully for: $userId")
            toastHelper.showMessage(context, "✅ Profile picture updated successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update profile picture: ${e.message}", e)
            toastHelper.showError(context, "❌ Failed to update profile picture: ${e.message}")
            throw e
        }
    }
    
    // Remove user profile picture
    suspend fun removeProfilePicture() {
        val userId = authService.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        
        try {
            val updateData = mapOf(
                "profilePictureUrl" to null,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            getCollection()
                .document(userId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "✅ Profile picture removed successfully for: $userId")
            toastHelper.showMessage(context, "✅ Profile picture removed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove profile picture: ${e.message}", e)
            toastHelper.showError(context, "❌ Failed to remove profile picture: ${e.message}")
            throw e
        }
    }
}