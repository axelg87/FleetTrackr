package com.fleetmanager.data.mapper

import com.fleetmanager.data.dto.ExpenseDto
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.ExpenseType

/**
 * Mapper to convert between Expense domain model and ExpenseDto.
 */
object ExpenseMapper {
    
    fun toDomain(dto: ExpenseDto): Expense {
        return Expense(
            id = dto.id,
            type = ExpenseType.valueOf(dto.type),
            amount = dto.amount,
            date = dto.date,
            driverName = dto.driverName,
            vehicle = dto.vehicle,
            notes = dto.notes,
            photoUrls = dto.photoUrls,
            isSynced = dto.isSynced,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
    
    fun toDto(domain: Expense): ExpenseDto {
        return ExpenseDto(
            id = domain.id,
            type = domain.type.name,
            amount = domain.amount,
            date = domain.date,
            driverName = domain.driverName,
            vehicle = domain.vehicle,
            notes = domain.notes,
            photoUrls = domain.photoUrls,
            isSynced = domain.isSynced,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    fun toDomainList(dtoList: List<ExpenseDto>): List<Expense> {
        return dtoList.map { toDomain(it) }
    }
    
    fun toDtoList(domainList: List<Expense>): List<ExpenseDto> {
        return domainList.map { toDto(it) }
    }
}