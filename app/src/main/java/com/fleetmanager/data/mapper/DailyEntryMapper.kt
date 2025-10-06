package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.domain.model.DailyEntry

/**
 * Mapper to convert between DailyEntry domain model and DailyEntryDto.
 * Room handles List<ProviderEarning> conversion via TypeConverter automatically.
 */
object DailyEntryMapper {
    
    fun toDomain(dto: DailyEntryDto): DailyEntry {
        return DailyEntry(
            id = dto.id,
            userId = dto.userId,
            date = dto.date,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            providers = dto.providers,
            notes = dto.notes,
            photoUrls = dto.photoUrls,
            odometer = dto.odometer,
            isSynced = dto.isSynced,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
    
    fun toDto(domain: DailyEntry): DailyEntryDto {
        return DailyEntryDto(
            id = domain.id,
            userId = domain.userId,
            date = domain.date,
            driverId = domain.driverId,
            vehicleId = domain.vehicleId,
            providers = domain.providers,
            notes = domain.notes,
            photoUrls = domain.photoUrls,
            odometer = domain.odometer,
            isSynced = domain.isSynced,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    fun toDomainList(dtoList: List<DailyEntryDto>): List<DailyEntry> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<DailyEntry>): List<DailyEntryDto> {
        return domainList.map { toDto(it) }
    }
}
