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
            isActive = dto.isActive,
            price = dto.price,
            deposit = dto.deposit,
            installment = dto.installment,
            installmentDurationMonths = dto.installmentDurationMonths,
            serviceStartDate = dto.serviceStartDate,
            serviceEndDate = dto.serviceEndDate,
            annualInsuranceAmount = dto.annualInsuranceAmount,
            fuelTankCapacity = dto.fuelTankCapacity,
            fuelConsumptionPer100Km = dto.fuelConsumptionPer100Km
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
            isActive = domain.isActive,
            price = domain.price,
            deposit = domain.deposit,
            installment = domain.installment,
            installmentDurationMonths = domain.installmentDurationMonths,
            serviceStartDate = domain.serviceStartDate,
            serviceEndDate = domain.serviceEndDate,
            annualInsuranceAmount = domain.annualInsuranceAmount,
            fuelTankCapacity = domain.fuelTankCapacity,
            fuelConsumptionPer100Km = domain.fuelConsumptionPer100Km
        )
    }
    
    fun toDomainList(dtoList: List<VehicleDto>): List<Vehicle> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<Vehicle>): List<VehicleDto> {
        return domainList.map { toDto(it) }
    }
}