package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Domain model for user information.
 * This represents the user entity stored in Firestore users collection.
 * 
 * Firestore Document Structure:
 * users/{uid} -> {
 *   "displayName": "John Doe",
 *   "email": "john@example.com", 
 *   "role": "driver", // or "manager", "admin"
 *   "createdAt": Timestamp,
 *   "photoUrl": "https://...", // optional
 * }
 */
data class User(
    @get:PropertyName("uid")
    val uid: String = "",
    
    @get:PropertyName("displayName")
    val displayName: String = "",
    
    @get:PropertyName("email") 
    val email: String = "",
    
    @get:PropertyName("role")
    val role: String = UserRole.DRIVER.name.lowercase(), // Store as lowercase string
    
    @get:PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @get:PropertyName("photoUrl")
    val photoUrl: String? = null
) {
    /**
     * Get the UserRole enum from the string role field
     */
    fun getUserRole(): UserRole {
        return UserRole.fromString(role)
    }
    
    /**
     * Create a copy with a new role
     */
    fun withRole(newRole: UserRole): User {
        return copy(role = newRole.name.lowercase())
    }
    
    /**
     * Check if this user has the specified role
     */
    fun hasRole(role: UserRole): Boolean {
        return getUserRole() == role
    }
    
    /**
     * Check if this user is an admin
     */
    fun isAdmin(): Boolean = hasRole(UserRole.ADMIN)
    
    /**
     * Check if this user is a manager
     */
    fun isManager(): Boolean = hasRole(UserRole.MANAGER)
    
    /**
     * Check if this user is a driver
     */
    fun isDriver(): Boolean = hasRole(UserRole.DRIVER)
    
    /**
     * Validate user data
     */
    fun isValid(): Boolean {
        return uid.isNotBlank() &&
                displayName.isNotBlank() &&
                email.isNotBlank() &&
                email.contains("@") &&
                role.isNotBlank()
    }
    
    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (uid.isBlank()) errors.add("UID cannot be blank")
        if (displayName.isBlank()) errors.add("Display name cannot be blank")
        if (email.isBlank()) errors.add("Email cannot be blank")
        if (email.isNotBlank() && !email.contains("@")) errors.add("Invalid email format")
        if (role.isBlank()) errors.add("Role cannot be blank")
        
        return errors
    }
    
    companion object {
        /**
         * Create a new User from Firebase Auth user data
         */
        fun fromFirebaseUser(
            uid: String,
            displayName: String?,
            email: String?,
            photoUrl: String? = null,
            role: UserRole = UserRole.DRIVER
        ): User {
            return User(
                uid = uid,
                displayName = displayName ?: "Unknown User",
                email = email ?: "",
                role = role.name.lowercase(),
                createdAt = Date(),
                photoUrl = photoUrl
            )
        }
    }
}