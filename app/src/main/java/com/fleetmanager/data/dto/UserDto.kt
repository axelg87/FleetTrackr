package com.fleetmanager.data.dto

import com.fleetmanager.domain.model.UserRole

data class UserDto(
    val id: String,
    val name: String,
    val role: UserRole,
    val email: String = "",
    val profilePictureUrl: String? = null
) {
    /**
     * Get user initials for display when no profile picture is available
     */
    val initials: String
        get() = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .takeIf { it.isNotEmpty() } ?: "U"
}