package com.fleetmanager.domain.usecase

import com.fleetmanager.domain.model.DailyEntry
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal fun aggregateTotalsByProvider(entries: List<DailyEntry>): Map<String, Double> {
    val totals = mutableMapOf<String, Double>()
    entries.forEach { entry ->
        entry.earnings.forEach { earning ->
            val key = normalizeProvider(earning.provider)
            totals[key] = (totals[key] ?: 0.0) + earning.totalAmount
        }
    }
    return totals
}

internal fun resolveProviderDisplayNames(entries: List<DailyEntry>): Map<String, String> {
    val result = linkedMapOf<String, String>()
    entries.forEach { entry ->
        entry.earnings.forEach { earning ->
            val key = normalizeProvider(earning.provider)
            val displayName = earning.provider.trim().ifBlank { "Unknown" }
            result.putIfAbsent(key, displayName)
        }
    }
    return result
}

internal fun normalizeProvider(provider: String): String {
    return provider.trim().lowercase(Locale.getDefault()).ifBlank { "unknown" }
}

internal fun generateDailyTrends(
    entries: List<DailyEntry>,
    referenceDate: Date,
    providerKeys: Set<String>
): Map<String, List<Double>> {
    if (providerKeys.isEmpty()) return emptyMap()

    val daysBack = 7
    val result = providerKeys.associateWith { MutableList(daysBack) { 0.0 } }.toMutableMap()

    (daysBack downTo 1).forEachIndexed { index, offset ->
        val start = startOfDay(referenceDate, offset)
        val end = Date(start.time + TimeUnit.DAYS.toMillis(1))

        val windowEntries = entries.filter { it.date >= start && it.date < end }
        providerKeys.forEach { key ->
            val totalForDay = windowEntries.sumOf { entry ->
                entry.earnings
                    .filter { normalizeProvider(it.provider) == key }
                    .sumOf { it.totalAmount }
            }
            result[key]?.set(index, totalForDay)
        }
    }

    return result.mapValues { it.value.toList() }
}

internal fun startOfDay(referenceDate: Date, daysAgo: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = referenceDate
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}
