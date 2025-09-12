package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.VehicleDto
import com.fleetmanager.domain.model.Vehicle

/**
 * Mapper to convert between Vehicle domain model and VehicleDto.
 */
object VehicleMapper {
    
    fun toDomain(dto: VehicleDto): Vehicle {
        return Vehicle(
            id = dto.id,
            userId = dto.userId,
            make = dto.make,
            model = dto.model,
            year = dto.year,
            licensePlate = dto.licensePlate,
            isActive = dto.isActive
        )
    }
    
    fun toDto(domain: Vehicle): VehicleDto {
        return VehicleDto(
            id = domain.id,
            userId = domain.userId,
            make = domain.make,
            model = domain.model,
            year = domain.year,
            licensePlate = domain.licensePlate,
            isActive = domain.isActive
        )
    }
    
    fun toDomainList(dtoList: List<VehicleDto>): List<Vehicle> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<Vehicle>): List<VehicleDto> {
        return domainList.map { toDto(it) }
    }
}