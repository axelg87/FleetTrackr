package com.fleetmanager.ui.model

import java.util.*

/**
 * Data class to represent filter context when navigating between screens.
 * Used to pass filter parameters from Dashboard tiles to Report screen.
 */
data class FilterContext(
    val startDate: Date? = null,
    val endDate: Date? = null,
    val sourceFilter: String? = null, // "uber", "yango", "private", etc.
    val timeRange: TimeRange? = null
)

/**
 * Enum to represent common time ranges for filtering
 */
enum class TimeRange {
    THIS_WEEK,
    THIS_MONTH,
    LAST_24H
}

/**
 * Extension functions to create FilterContext easily
 */
object FilterContextFactory {
    
    fun createThisWeekFilter(): FilterContext {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeek = calendar.time
        
        return FilterContext(
            startDate = startOfWeek,
            endDate = endOfWeek,
            timeRange = TimeRange.THIS_WEEK
        )
    }
    
    fun createThisMonthFilter(): FilterContext {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.time
        
        return FilterContext(
            startDate = startOfMonth,
            endDate = endOfMonth,
            timeRange = TimeRange.THIS_MONTH
        )
    }
    
    fun createLast24HFilter(): FilterContext {
        val now = Date()
        val yesterday = Date(now.time - 24 * 60 * 60 * 1000)
        
        return FilterContext(
            startDate = yesterday,
            endDate = now,
            timeRange = TimeRange.LAST_24H
        )
    }
    
    fun createUberEarningsFilter(timeRange: TimeRange): FilterContext {
        val baseFilter = when (timeRange) {
            TimeRange.THIS_WEEK -> createThisWeekFilter()
            TimeRange.THIS_MONTH -> createThisMonthFilter()
            TimeRange.LAST_24H -> createLast24HFilter()
        }
        return baseFilter.copy(sourceFilter = "uber")
    }
    
    fun createYangoEarningsFilter(timeRange: TimeRange): FilterContext {
        val baseFilter = when (timeRange) {
            TimeRange.THIS_WEEK -> createThisWeekFilter()
            TimeRange.THIS_MONTH -> createThisMonthFilter()
            TimeRange.LAST_24H -> createLast24HFilter()
        }
        return baseFilter.copy(sourceFilter = "yango")
    }
    
    fun createPrivateEarningsFilter(timeRange: TimeRange): FilterContext {
        val baseFilter = when (timeRange) {
            TimeRange.THIS_WEEK -> createThisWeekFilter()
            TimeRange.THIS_MONTH -> createThisMonthFilter()
            TimeRange.LAST_24H -> createLast24HFilter()
        }
        return baseFilter.copy(sourceFilter = "private")
    }
}