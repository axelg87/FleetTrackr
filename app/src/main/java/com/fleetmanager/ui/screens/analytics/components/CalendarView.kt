package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.ui.screens.analytics.IncomeLevel
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar View component for Analytics screen.
 * Shows monthly calendar with color-coded income indicators.
 */
@Composable
fun CalendarView(
    entriesData: Map<LocalDate, List<DailyEntry>>,
    isLoading: Boolean,
    onDayClick: (LocalDate, List<DailyEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val daysOfWeek = remember { daysOfWeek() }
    
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    Column(modifier = modifier) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CalendarHeader(
                state = state,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            DaysOfWeekHeader(daysOfWeek = daysOfWeek)
            
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    CalendarDay(
                        day = day,
                        entries = entriesData[day.date] ?: emptyList(),
                        onClick = { date, entries ->
                            if (entries.isNotEmpty()) {
                                onDayClick(date, entries)
                            }
                        }
                    )
                }
            )
        }
    }
}

/**
 * Calendar header with month navigation
 */
@Composable
private fun CalendarHeader(
    state: CalendarState,
    modifier: Modifier = Modifier
) {
    val visibleMonth = rememberUpdatedState(state.firstVisibleMonth)
    val coroutineScope = rememberCoroutineScope()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(visibleMonth.value.yearMonth.previousMonth)
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month"
            )
        }
        
        Text(
            text = visibleMonth.value.yearMonth.displayText(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        IconButton(
            onClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(visibleMonth.value.yearMonth.nextMonth)
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month"
            )
        }
    }
}

/**
 * Days of week header
 */
@Composable
private fun DaysOfWeekHeader(
    daysOfWeek: List<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual calendar day cell
 */
@Composable
private fun CalendarDay(
    day: CalendarDay,
    entries: List<DailyEntry>,
    onClick: (LocalDate, List<DailyEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    val incomeLevel = getIncomeLevel(entries)
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val isYesterday = day.date == LocalDate.now().minusDays(1) // d-1 logic: highlight yesterday instead
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                color = when {
                    isYesterday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Highlight yesterday (d-1)
                    isToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) // Dim today (excluded from analysis)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = entries.isNotEmpty() && !isToday) { // Disable clicking on today
                onClick(day.date, entries)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Dim today
                    isYesterday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 14.sp,
                fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Normal
            )
            
            // Income indicator dot (don't show for today since it's excluded from d-1 analysis)
            if (entries.isNotEmpty() && isCurrentMonth && !isToday) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(getIncomeColor(incomeLevel))
                )
            }
        }
    }
}

/**
 * Calculate income level based on total earnings
 */
private fun getIncomeLevel(entries: List<DailyEntry>): IncomeLevel {
    val totalIncome = entries.sumOf { it.totalEarnings }
    
    return when {
        totalIncome >= 250.0 -> IncomeLevel.HIGH
        totalIncome >= 100.0 -> IncomeLevel.MEDIUM
        totalIncome > 0 -> IncomeLevel.LOW
        else -> IncomeLevel.NONE
    }
}

/**
 * REFACTOR: Moved color logic to AnalyticsUtils for consistency
 * Get color for income level
 */
@Composable
private fun getIncomeColor(incomeLevel: IncomeLevel): Color {
    return when (incomeLevel) {
        IncomeLevel.HIGH -> AnalyticsUtils.Colors.SUCCESS
        IncomeLevel.MEDIUM -> AnalyticsUtils.Colors.WARNING
        IncomeLevel.LOW -> AnalyticsUtils.Colors.ERROR
        IncomeLevel.NONE -> Color.Transparent
    }
}

/**
 * Format YearMonth for display
 */
private fun YearMonth.displayText(): String {
    return "AED{this.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} AED{this.year}"
}