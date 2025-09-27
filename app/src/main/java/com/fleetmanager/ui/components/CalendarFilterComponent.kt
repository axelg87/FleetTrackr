package com.fleetmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Reusable calendar filter component that provides both date range picker and calendar view options.
 * Can be used in Reports and other screens that need date filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterComponent(
    startDate: Date?,
    endDate: Date?,
    onDateRangeChange: (Date?, Date?) -> Unit,
    modifier: Modifier = Modifier,
    showCalendarView: Boolean = false
) {
    var isCalendarViewExpanded by remember { mutableStateOf(showCalendarView) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with toggle option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Toggle between date picker and calendar view
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isCalendarViewExpanded = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Range Picker",
                            tint = if (!isCalendarViewExpanded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { isCalendarViewExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar View",
                            tint = if (isCalendarViewExpanded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            if (isCalendarViewExpanded) {
                // Calendar View Mode
                CalendarRangeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onDateRangeChange = onDateRangeChange
                )
            } else {
                // Date Range Picker Mode (existing functionality)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = startDate?.let { dateFormatter.format(it) } ?: "",
                            onValueChange = { },
                            label = { Text("Start Date") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select start date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // End Date Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = endDate?.let { dateFormatter.format(it) } ?: "",
                            onValueChange = { },
                            label = { Text("End Date") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select end date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            
            // Quick date range buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateButton(
                    text = "This Week",
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                        val startOfWeek = calendar.time
                        calendar.add(Calendar.DAY_OF_WEEK, 6)
                        val endOfWeek = calendar.time
                        onDateRangeChange(startOfWeek, endOfWeek)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickDateButton(
                    text = "This Month",
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val startOfMonth = calendar.time
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val endOfMonth = calendar.time
                        onDateRangeChange(startOfMonth, endOfMonth)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickDateButton(
                    text = "Clear",
                    onClick = { onDateRangeChange(null, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    
    // Date picker dialogs
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.time
        )
        DatePickerDialog(
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    onDateRangeChange(Date(it), endDate)
                }
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            datePickerState = datePickerState
        )
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.time
        )
        DatePickerDialog(
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    onDateRangeChange(startDate, Date(it))
                }
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            datePickerState = datePickerState
        )
    }
}

@Composable
private fun QuickDateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CalendarRangeSelector(
    startDate: Date?,
    endDate: Date?,
    onDateRangeChange: (Date?, Date?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple month view with clickable dates for range selection
    // This is a simplified version - for a full calendar, you'd use a proper calendar library
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val calendar = Calendar.getInstance()
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newCalendar = currentMonth.clone() as Calendar
                    newCalendar.add(Calendar.MONTH, -1)
                    currentMonth = newCalendar
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            IconButton(
                onClick = {
                    val newCalendar = currentMonth.clone() as Calendar
                    newCalendar.add(Calendar.MONTH, 1)
                    currentMonth = newCalendar
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
        
        // For now, show a simplified message about calendar view
        // In a full implementation, you'd show the actual calendar grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Calendar View",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use the date range picker above or quick buttons for date selection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (startDate != null || endDate != null) {
                    Text(
                        text = "Selected: AED{startDate?.let { SimpleDateFormat("MMM dd", Locale.getDefault()).format(it) } ?: "Start"} - AED{endDate?.let { SimpleDateFormat("MMM dd", Locale.getDefault()).format(it) } ?: "End"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}