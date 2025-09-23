package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val repository: FleetRepository
) {
    operator fun invoke(expenseId: String): Flow<Expense?> {
        require(expenseId.isNotBlank()) { "Expense ID cannot be blank" }
        return repository.getExpenseById(expenseId)
    }
}
