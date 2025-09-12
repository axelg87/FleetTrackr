package com.fleetmanager.data.dto

import com.fleetmanager.domain.model.UserRole

data class UserDto(
    val id: String,
    val name: String,
    val role: UserRole
)