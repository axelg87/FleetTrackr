package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.FleetRepository
import javax.inject.Inject

/**
 * Use case for deleting an expense.
 */
class DeleteExpenseUseCase @Inject constructor(
    private val repository: FleetRepository
) {

    suspend operator fun invoke(expenseId: String) {
        repository.deleteExpense(expenseId)
    }
}
