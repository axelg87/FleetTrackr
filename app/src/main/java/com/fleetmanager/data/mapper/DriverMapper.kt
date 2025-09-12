package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.DriverDto
import com.fleetmanager.domain.model.Driver

/**
 * Mapper to convert between Driver domain model and DriverDto.
 */
object DriverMapper {
    
    fun toDomain(dto: DriverDto): Driver {
        return Driver(
            id = dto.id,
            userId = dto.userId,
            name = dto.name,
            isActive = dto.isActive
        )
    }
    
    fun toDto(domain: Driver): DriverDto {
        return DriverDto(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            isActive = domain.isActive
        )
    }
    
    fun toDomainList(dtoList: List<DriverDto>): List<Driver> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<Driver>): List<DriverDto> {
        return domainList.map { toDto(it) }
    }
}