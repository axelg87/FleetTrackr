package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.User
import java.util.Date

/**
 * Mapper between User domain model and UserDto data model
 */
object UserMapper {
    
    fun toDomain(dto: UserDto): User {
        return User(
            uid = dto.uid,
            displayName = dto.displayName,
            email = dto.email,
            role = dto.role,
            createdAt = dto.createdAt,
            photoUrl = dto.photoUrl
        )
    }
    
    fun toDto(domain: User): UserDto {
        return UserDto(
            uid = domain.uid,
            displayName = domain.displayName,
            email = domain.email,
            role = domain.role,
            createdAt = domain.createdAt,
            photoUrl = domain.photoUrl,
            lastSyncedAt = Date()
        )
    }
    
    fun toDomainList(dtoList: List<UserDto>): List<User> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<User>): List<UserDto> {
        return domainList.map { toDto(it) }
    }
}