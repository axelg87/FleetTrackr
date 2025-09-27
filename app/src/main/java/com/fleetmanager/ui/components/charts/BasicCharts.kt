package com.fleetmanager.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fleetmanager.ui.model.BarChartData
import com.fleetmanager.ui.model.BarData
import com.fleetmanager.ui.model.PieChartData
import com.fleetmanager.ui.model.PieSlice
import kotlin.math.*

/**
 * Simple pie chart component using Canvas
 */
@Composable
fun SimplePieChart(
    data: PieChartData,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    if (data.slices.isEmpty()) {
        Box(
            modifier = modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }
    
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie chart
        Canvas(
            modifier = Modifier.size(240.dp)
        ) {
            drawPieChart(data, size)
        }
        
        if (showLegend) {
            Spacer(modifier = Modifier.height(20.dp))
            PieChartLegend(data = data)
        }
    }
}

private fun DrawScope.drawPieChart(data: PieChartData, canvasSize: Size) {
    val total = data.total
    if (total <= 0) return
    
    val radius = minOf(canvasSize.width, canvasSize.height) / 2 * 0.8f
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    
    var startAngle = -90f // Start from top
    
    data.slices.forEach { slice ->
        val sweepAngle = (slice.value / total * 360).toFloat()
        
        drawArc(
            color = slice.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = Size(radius * 2, radius * 2)
        )
        
        startAngle += sweepAngle
    }
}

@Composable
private fun PieChartLegend(data: PieChartData) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 180.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(data.slices) { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${slice.getPercentage(data.total).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "AED ${String.format("%.2f", slice.value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 70.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Simple bar chart component
 */
@Composable
fun SimpleBarChart(
    data: BarChartData,
    modifier: Modifier = Modifier,
    maxHeight: Float = 200f
) {
    if (data.bars.isEmpty()) {
        Box(
            modifier = modifier
                .height(maxHeight.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (data.title.isNotEmpty()) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight.dp)
        ) {
            drawBarChart(data, size)
        }
    }
}

private fun DrawScope.drawBarChart(data: BarChartData, canvasSize: Size) {
    if (data.bars.isEmpty()) return
    
    val maxValue = data.bars.maxOfOrNull { abs(it.value) }?.toFloat() ?: 0f
    val minValue = data.bars.minOfOrNull { it.value }?.toFloat() ?: 0f
    
    // Handle case where all values are zero
    if (maxValue == 0f && minValue == 0f) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    if (chartWidth <= 0 || chartHeight <= 0) return
    
    val barWidth = minOf(chartWidth / data.bars.size * 0.8f, 60f) // Cap max bar width
    val barSpacing = chartWidth / data.bars.size * 0.2f
    
    // Calculate zero line position
    val zeroY = if (minValue >= 0) {
        canvasSize.height - padding
    } else if (maxValue <= 0) {
        padding
    } else {
        padding + (chartHeight * maxValue / (maxValue - minValue))
    }
    
    // Draw zero line
    drawLine(
        color = Color.Gray,
        start = Offset(padding, zeroY),
        end = Offset(canvasSize.width - padding, zeroY),
        strokeWidth = 1.dp.toPx()
    )
    
    data.bars.forEachIndexed { index, bar ->
        val x = padding + (index * (barWidth + barSpacing)) + (barSpacing / 2)
        
        val barHeight = if (maxValue != minValue) {
            (abs(bar.value.toFloat()) / (maxValue - minValue)) * chartHeight
        } else {
            0f
        }
        
        val barTop = if (bar.value >= 0) {
            zeroY - barHeight
        } else {
            zeroY
        }
        
        val barBottom = if (bar.value >= 0) {
            zeroY
        } else {
            zeroY + barHeight
        }
        
        // Draw bar
        drawRect(
            color = bar.color,
            topLeft = Offset(x, barTop),
            size = Size(barWidth, abs(barBottom - barTop))
        )
    }
}