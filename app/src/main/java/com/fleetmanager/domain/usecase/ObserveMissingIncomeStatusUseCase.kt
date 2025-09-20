package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.domain.repository.FleetRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Observes whether the currently authenticated driver has submitted an income entry
 * for the provided target date. The comparison is executed against the Dubai timezone
 * to remain consistent with server-side scheduling.
 */
class ObserveMissingIncomeStatusUseCase @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val authRepository: AuthRepository
) {

    private val zoneId: ZoneId = ZoneId.of("Asia/Dubai")

    operator fun invoke(targetDateIso: String): Flow<Boolean> {
        val userId = authRepository.currentUserId ?: return flowOf(false)
        val targetDate = runCatching { LocalDate.parse(targetDateIso) }.getOrElse { return flowOf(false) }
        val startOfDay = targetDate.atStartOfDay(zoneId).toInstant()
        val startOfNextDay = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        return fleetRepository.getAllDailyEntriesRealtime()
            .map { entries ->
                entries
                    .filter { it.userId == userId }
                    .none { entry ->
                        val entryInstant = entry.date.toInstant()
                        entryInstant >= startOfDay && entryInstant < startOfNextDay
                    }
            }
            .distinctUntilChanged()
    }
}
