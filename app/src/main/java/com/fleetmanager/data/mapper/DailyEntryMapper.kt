package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.domain.model.DailyEntry

/**
 * Mapper to convert between DailyEntry domain model and DailyEntryDto.
 */
object DailyEntryMapper {
    
    fun toDomain(dto: DailyEntryDto): DailyEntry {
        return DailyEntry(
            id = dto.id,
            userId = dto.userId,
            date = dto.date,
            driverId = dto.driverId,
            vehicleId = dto.vehicleId,
            odometer = dto.odometer,
            uberEarnings = dto.uberEarnings,
            uberHoursOnline = dto.uberHoursOnline,
            uberCashEarnings = dto.uberCashEarnings,
            uberCardEarnings = dto.uberCardEarnings,
            uberTips = dto.uberTips,
            yangoEarnings = dto.yangoEarnings,
            yangoHoursOnline = dto.yangoHoursOnline,
            yangoCashEarnings = dto.yangoCashEarnings,
            yangoCardEarnings = dto.yangoCardEarnings,
            yangoTips = dto.yangoTips,
            privateJobsEarnings = dto.privateJobsEarnings,
            privateJobsHoursOnline = dto.privateJobsHoursOnline,
            privateJobsCashEarnings = dto.privateJobsCashEarnings,
            privateJobsCardEarnings = dto.privateJobsCardEarnings,
            privateJobsTips = dto.privateJobsTips,
            notes = dto.notes,
            photoUrls = dto.photoUrls,
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
            odometer = domain.odometer,
            uberEarnings = domain.uberEarnings,
            uberHoursOnline = domain.uberHoursOnline,
            uberCashEarnings = domain.uberCashEarnings,
            uberCardEarnings = domain.uberCardEarnings,
            uberTips = domain.uberTips,
            yangoEarnings = domain.yangoEarnings,
            yangoHoursOnline = domain.yangoHoursOnline,
            yangoCashEarnings = domain.yangoCashEarnings,
            yangoCardEarnings = domain.yangoCardEarnings,
            yangoTips = domain.yangoTips,
            privateJobsEarnings = domain.privateJobsEarnings,
            privateJobsHoursOnline = domain.privateJobsHoursOnline,
            privateJobsCashEarnings = domain.privateJobsCashEarnings,
            privateJobsCardEarnings = domain.privateJobsCardEarnings,
            privateJobsTips = domain.privateJobsTips,
            notes = domain.notes,
            photoUrls = domain.photoUrls,
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