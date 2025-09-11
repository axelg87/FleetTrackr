package com.fleetmanager.ui.model

import androidx.compose.ui.graphics.Color
import java.util.*

/**
 * Data models for chart visualization
 */
data class PieChartData(
    val slices: List<PieSlice>
) {
    val total: Double get() = slices.sumOf { it.value }
}

data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color
) {
    fun getPercentage(total: Double): Float = 
        if (total > 0) (value / total * 100).toFloat() else 0f
}

data class BarChartData(
    val bars: List<BarData>,
    val title: String = "",
    val xAxisLabel: String = "",
    val yAxisLabel: String = ""
)

data class BarData(
    val label: String,
    val value: Double,
    val color: Color
)

data class TimeSeriesData(
    val points: List<TimePoint>,
    val title: String = ""
)

data class TimePoint(
    val date: Date,
    val value: Double,
    val label: String = ""
)

/**
 * Helper to generate chart data from report entries
 */
object ChartDataGenerator {
    
    fun generatePieChartByType(entries: List<ReportEntry>): PieChartData {
        val typeGroups = entries.groupBy { it.typeDisplayName }
        val slices = typeGroups.mapIndexed { index, (type, typeEntries) ->
            PieSlice(
                label = type,
                value = typeEntries.sumOf { kotlin.math.abs(it.amount) },
                color = getColorForIndex(index)
            )
        }.sortedByDescending { it.value }
        
        return PieChartData(slices)
    }
    
    fun generatePieChartByDriver(entries: List<ReportEntry>): PieChartData {
        val driverGroups = entries.groupBy { it.driverName }
        val slices = driverGroups.mapIndexed { index, (driver, driverEntries) ->
            PieSlice(
                label = driver,
                value = driverEntries.sumOf { kotlin.math.abs(it.amount) },
                color = getColorForIndex(index)
            )
        }.sortedByDescending { it.value }
        
        return PieChartData(slices)
    }
    
    fun generateBarChartByMonth(entries: List<ReportEntry>): BarChartData {
        val calendar = Calendar.getInstance()
        val monthGroups = entries.groupBy { entry ->
            calendar.time = entry.date
            "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
        }
        
        val bars = monthGroups.map { (month, monthEntries) ->
            val netAmount = monthEntries.sumOf { if (it.isIncome) it.amount else -it.amount }
            BarData(
                label = month,
                value = netAmount,
                color = if (netAmount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }.sortedBy { it.label }
        
        return BarChartData(
            bars = bars,
            title = "Monthly Net Amount",
            xAxisLabel = "Month",
            yAxisLabel = "Amount ($)"
        )
    }
    
    fun generateBarChartByWeek(entries: List<ReportEntry>): BarChartData {
        val calendar = Calendar.getInstance()
        val weekGroups = entries.groupBy { entry ->
            calendar.time = entry.date
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.time
        }
        
        val bars = weekGroups.map { (weekStart, weekEntries) ->
            val netAmount = weekEntries.sumOf { if (it.isIncome) it.amount else -it.amount }
            calendar.time = weekStart
            val weekLabel = "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
            
            BarData(
                label = weekLabel,
                value = netAmount,
                color = if (netAmount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }.sortedBy { it.label }
        
        return BarChartData(
            bars = bars,
            title = "Weekly Net Amount",
            xAxisLabel = "Week",
            yAxisLabel = "Amount ($)"
        )
    }
    
    private fun getColorForIndex(index: Int): Color {
        val colors = listOf(
            Color(0xFF2196F3), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFF9800), // Orange
            Color(0xFF9C27B0), // Purple
            Color(0xFFF44336), // Red
            Color(0xFF00BCD4), // Cyan
            Color(0xFF795548), // Brown
            Color(0xFF607D8B), // Blue Grey
            Color(0xFFE91E63), // Pink
            Color(0xFF8BC34A), // Light Green
        )
        return colors[index % colors.size]
    }
}