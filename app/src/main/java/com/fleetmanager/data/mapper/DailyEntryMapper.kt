package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.data.dto.EarningBreakdownDto
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.EarningBreakdown

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
            uberEarnings = dto.uberEarnings,
            yangoEarnings = dto.yangoEarnings,
            careemEarnings = dto.careemEarnings,
            privateJobsEarnings = dto.privateJobsEarnings,
            earningsBreakdown = dto.earningsBreakdown.map { it.toDomain() },
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
            uberEarnings = domain.uberEarnings,
            yangoEarnings = domain.yangoEarnings,
            careemEarnings = domain.careemEarnings,
            privateJobsEarnings = domain.privateJobsEarnings,
            earningsBreakdown = domain.earningsBreakdown.map { it.toDto() },
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

private fun EarningBreakdownDto.toDomain(): EarningBreakdown {
    return EarningBreakdown(
        provider = provider,
        cardEarnings = cardEarnings,
        cashEarnings = cashEarnings,
        tips = tips,
        tripCount = tripCount,
        hoursOnline = hoursOnline
    )
}

private fun EarningBreakdown.toDto(): EarningBreakdownDto {
    return EarningBreakdownDto(
        provider = provider,
        cardEarnings = cardEarnings,
        cashEarnings = cashEarnings,
        tips = tips,
        tripCount = tripCount,
        hoursOnline = hoursOnline
    )
}