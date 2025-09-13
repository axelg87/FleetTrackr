package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import kotlin.math.*

/**
 * GENERALIZATION: Reusable chart component that supports multiple chart types
 * Eliminates duplication between TrendsChart, ExpenseDeepDive, DayOfWeekChart, etc.
 */

// Chart data models
data class ChartDataPoint(
    val label: String,
    val value: Double,
    val color: Color,
    val secondaryValue: Double = 0.0, // For dual-axis charts
    val metadata: Map<String, Any> = emptyMap()
)

data class ChartSeries(
    val name: String,
    val data: List<ChartDataPoint>,
    val color: Color,
    val type: ChartSeriesType = ChartSeriesType.LINE
)

enum class ChartType {
    LINE,           // Multi-line chart (TrendsChart)
    BAR_HORIZONTAL, // Horizontal bars (DriverComparison, DayOfWeekChart)
    BAR_VERTICAL,   // Vertical bars
    PIE,            // Pie chart (ExpenseDeepDive)
    DONUT,          // Donut chart variation
    PROGRESS_BAR    // Progress bars with labels
}

enum class ChartSeriesType {
    LINE, BAR, AREA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericChart(
    title: String,
    subtitle: String? = null,
    chartType: ChartType,
    data: List<ChartDataPoint> = emptyList(),
    series: List<ChartSeries> = emptyList(),
    isLoading: Boolean = false,
    showLegend: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    onDataPointClick: ((ChartDataPoint) -> Unit)? = null,
    customContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            ChartHeader(title = title, subtitle = subtitle)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                ChartLoadingState(height = height)
            } else if (data.isEmpty() && series.isEmpty()) {
                ChartEmptyState(height = height)
            } else {
                // Legend
                if (showLegend && (series.isNotEmpty() || data.any { it.color != Color.Transparent })) {
                    ChartLegend(data = data, series = series)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Chart content
                when (chartType) {
                    ChartType.LINE -> {
                        LineChart(
                            series = series,
                            height = height,
                            onDataPointClick = onDataPointClick
                        )
                    }
                    ChartType.BAR_HORIZONTAL -> {
                        HorizontalBarChart(
                            data = data,
                            height = height,
                            onDataPointClick = onDataPointClick
                        )
                    }
                    ChartType.BAR_VERTICAL -> {
                        VerticalBarChart(
                            data = data,
                            height = height,
                            onDataPointClick = onDataPointClick
                        )
                    }
                    ChartType.PIE, ChartType.DONUT -> {
                        PieChart(
                            data = data,
                            isDonut = chartType == ChartType.DONUT,
                            onDataPointClick = onDataPointClick
                        )
                    }
                    ChartType.PROGRESS_BAR -> {
                        ProgressBarChart(
                            data = data,
                            onDataPointClick = onDataPointClick
                        )
                    }
                }
                
                // Custom content (for additional components)
                customContent?.invoke()
            }
        }
    }
}

@Composable
private fun ChartHeader(
    title: String,
    subtitle: String?
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartLoadingState(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ChartEmptyState(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add some data to see the chart",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartLegend(
    data: List<ChartDataPoint>,
    series: List<ChartSeries>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Show series legend if available
        if (series.isNotEmpty()) {
            series.forEach { seriesItem ->
                LegendItem(
                    color = seriesItem.color,
                    label = seriesItem.name
                )
            }
        } else {
            // Show data legend for single series charts
            data.distinctBy { it.color }.take(3).forEach { dataPoint ->
                if (dataPoint.color != Color.Transparent) {
                    LegendItem(
                        color = dataPoint.color,
                        label = dataPoint.label
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LineChart(
    series: List<ChartSeries>,
    height: androidx.compose.ui.unit.Dp,
    onDataPointClick: ((ChartDataPoint) -> Unit)?
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 8.dp)
    ) {
        drawLineChart(
            series = series,
            canvasSize = size
        )
    }
}

@Composable
private fun HorizontalBarChart(
    data: List<ChartDataPoint>,
    height: androidx.compose.ui.unit.Dp,
    onDataPointClick: ((ChartDataPoint) -> Unit)?
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    
    LazyColumn(
        modifier = Modifier.heightIn(max = height),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(data) { dataPoint ->
            HorizontalBarItem(
                dataPoint = dataPoint,
                maxValue = maxValue,
                onClick = { onDataPointClick?.invoke(dataPoint) }
            )
        }
    }
}

@Composable
private fun HorizontalBarItem(
    dataPoint: ChartDataPoint,
    maxValue: Double,
    onClick: () -> Unit
) {
    val progressPercentage = AnalyticsUtils.calculateProgress(dataPoint.value, maxValue)
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "bar_progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dataPoint.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.3f)
            )
            
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .height(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            color = dataPoint.color,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = AnalyticsUtils.formatCurrency(dataPoint.value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = AnalyticsUtils.formatWholeNumber((progressPercentage * 100).toDouble()) + "%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = dataPoint.color,
                modifier = Modifier.weight(0.2f)
            )
        }
    }
}

@Composable
private fun VerticalBarChart(
    data: List<ChartDataPoint>,
    height: androidx.compose.ui.unit.Dp,
    onDataPointClick: ((ChartDataPoint) -> Unit)?
) {
    // Implementation for vertical bar chart
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        drawVerticalBarChart(
            data = data,
            canvasSize = size
        )
    }
}

@Composable
private fun PieChart(
    data: List<ChartDataPoint>,
    isDonut: Boolean,
    onDataPointClick: ((ChartDataPoint) -> Unit)?
) {
    val totalValue = data.sumOf { it.value }
    
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawPieChart(
                data = data,
                totalValue = totalValue,
                isDonut = isDonut,
                canvasSize = size
            )
        }
    }
}

@Composable
private fun ProgressBarChart(
    data: List<ChartDataPoint>,
    onDataPointClick: ((ChartDataPoint) -> Unit)?
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { dataPoint ->
            ProgressBarItem(
                dataPoint = dataPoint,
                maxValue = maxValue,
                onClick = { onDataPointClick?.invoke(dataPoint) }
            )
        }
    }
}

@Composable
private fun ProgressBarItem(
    dataPoint: ChartDataPoint,
    maxValue: Double,
    onClick: () -> Unit
) {
    val progressPercentage = AnalyticsUtils.calculateProgress(dataPoint.value, maxValue)
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dataPoint.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = AnalyticsUtils.formatCurrency(dataPoint.value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = dataPoint.color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        color = dataPoint.color,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// Drawing functions
private fun DrawScope.drawLineChart(
    series: List<ChartSeries>,
    canvasSize: Size
) {
    if (series.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    val maxValue = series.flatMap { it.data }.maxOfOrNull { it.value } ?: 1.0
    val dataPointCount = series.firstOrNull()?.data?.size ?: 0
    
    if (dataPointCount <= 1) return
    
    val stepX = chartWidth / (dataPointCount - 1).coerceAtLeast(1)
    
    series.forEach { seriesItem ->
        val path = Path()
        
        seriesItem.data.forEachIndexed { index, dataPoint ->
            val x = padding + (index * stepX)
            val y = padding + chartHeight - ((dataPoint.value / maxValue) * chartHeight).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = seriesItem.color,
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Draw data points
        seriesItem.data.forEachIndexed { index, dataPoint ->
            val x = padding + (index * stepX)
            val y = padding + chartHeight - ((dataPoint.value / maxValue) * chartHeight).toFloat()
            
            drawCircle(
                color = seriesItem.color,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawVerticalBarChart(
    data: List<ChartDataPoint>,
    canvasSize: Size
) {
    if (data.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    val barWidth = chartWidth / data.size * 0.8f
    val barSpacing = chartWidth / data.size * 0.2f
    
    data.forEachIndexed { index, dataPoint ->
        val barHeight = ((dataPoint.value / maxValue) * chartHeight).toFloat()
        val x = padding + (index * (barWidth + barSpacing))
        val y = padding + chartHeight - barHeight
        
        drawRect(
            color = dataPoint.color,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawPieChart(
    data: List<ChartDataPoint>,
    totalValue: Double,
    isDonut: Boolean,
    canvasSize: Size
) {
    if (data.isEmpty() || totalValue == 0.0) return
    
    val center = canvasSize.center
    val radius = minOf(canvasSize.width, canvasSize.height) / 2 * 0.8f
    
    var startAngle = -90f // Start from top
    
    data.forEach { dataPoint ->
        val sweepAngle = ((dataPoint.value / totalValue) * 360).toFloat()
        
        drawArc(
            color = dataPoint.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        startAngle += sweepAngle
    }
    
    // Draw center circle for donut effect
    if (isDonut) {
        drawCircle(
            color = androidx.compose.ui.graphics.Color.White,
            radius = radius * 0.4f,
            center = center
        )
    }
}