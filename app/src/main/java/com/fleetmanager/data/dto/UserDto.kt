package com.fleetmanager.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data Transfer Object for User.
 * Used for Room database operations to cache user data locally.
 */
@Entity(tableName = "users")
data class UserDto(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val email: String,
    val role: String, // UserRole enum as string (lowercase)
    val createdAt: Date,
    val photoUrl: String? = null,
    val lastSyncedAt: Date = Date()
)