package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.TrendData
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/**
 * Trends Over Time component showing income, expenses, and profit trends
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsChart(
    trendData: List<TrendData>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.DAILY) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trends Over Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Daily income and expense patterns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Period selector
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (trendData.isEmpty()) {
                EmptyTrendsState()
            } else {
                // Legend
                TrendLegend()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Chart
                TrendChart(
                    data = trendData,
                    period = selectedPeriod
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary stats
                TrendSummary(trendData = trendData)
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TrendPeriod,
    onPeriodSelected: (TrendPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TrendPeriod.values().forEach { period ->
            val isSelected = period == selectedPeriod
            
            TextButton(
                onClick = { onPeriodSelected(period) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else Color.Transparent,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = period.displayName,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TrendLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = Color(0xFF4CAF50),
            label = "Income"
        )
        LegendItem(
            color = Color(0xFFF44336),
            label = "Expenses"
        )
        LegendItem(
            color = Color(0xFF2196F3),
            label = "Net Profit"
        )
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
private fun TrendChart(
    data: List<TrendData>,
    period: TrendPeriod
) {
    if (data.isEmpty()) return
    
    val maxIncome = data.maxOf { it.income }
    val maxExpenses = data.maxOf { it.expenses }
    val maxValue = max(maxIncome, maxExpenses)
    val minProfit = data.minOf { it.netProfit }
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    ) {
        drawTrendChart(
            data = data,
            maxValue = maxValue,
            minProfit = minProfit,
            canvasSize = size
        )
    }
}

private fun DrawScope.drawTrendChart(
    data: List<TrendData>,
    maxValue: Double,
    minProfit: Double,
    canvasSize: Size
) {
    if (data.isEmpty()) return
    
    val padding = 40f
    val chartWidth = canvasSize.width - (padding * 2)
    val chartHeight = canvasSize.height - (padding * 2)
    
    val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)
    
    // Colors
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)
    val profitColor = Color(0xFF2196F3)
    
    // Draw income line
    val incomePath = Path()
    data.forEachIndexed { index, trendData ->
        val x = padding + (index * stepX)
        val y = padding + chartHeight - ((trendData.income / maxValue) * chartHeight).toFloat()
        
        if (index == 0) {
            incomePath.moveTo(x, y)
        } else {
            incomePath.lineTo(x, y)
        }
    }
    
    drawPath(
        path = incomePath,
        color = incomeColor,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Draw expense line
    val expensePath = Path()
    data.forEachIndexed { index, trendData ->
        val x = padding + (index * stepX)
        val y = padding + chartHeight - ((trendData.expenses / maxValue) * chartHeight).toFloat()
        
        if (index == 0) {
            expensePath.moveTo(x, y)
        } else {
            expensePath.lineTo(x, y)
        }
    }
    
    drawPath(
        path = expensePath,
        color = expenseColor,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Draw profit line (can be negative)
    val profitPath = Path()
    val zeroLine = if (minProfit < 0) {
        padding + chartHeight - ((0 - minProfit) / (maxValue - minProfit) * chartHeight).toFloat()
    } else {
        padding + chartHeight
    }
    
    data.forEachIndexed { index, trendData ->
        val x = padding + (index * stepX)
        val y = if (minProfit < 0) {
            padding + chartHeight - (((trendData.netProfit - minProfit) / (maxValue - minProfit)) * chartHeight).toFloat()
        } else {
            padding + chartHeight - ((trendData.netProfit / maxValue) * chartHeight).toFloat()
        }
        
        if (index == 0) {
            profitPath.moveTo(x, y)
        } else {
            profitPath.lineTo(x, y)
        }
    }
    
    drawPath(
        path = profitPath,
        color = profitColor,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Draw data points
    data.forEachIndexed { index, trendData ->
        val x = padding + (index * stepX)
        
        // Income point
        val incomeY = padding + chartHeight - ((trendData.income / maxValue) * chartHeight).toFloat()
        drawCircle(
            color = incomeColor,
            radius = 4.dp.toPx(),
            center = Offset(x, incomeY)
        )
        
        // Expense point
        val expenseY = padding + chartHeight - ((trendData.expenses / maxValue) * chartHeight).toFloat()
        drawCircle(
            color = expenseColor,
            radius = 4.dp.toPx(),
            center = Offset(x, expenseY)
        )
        
        // Profit point
        val profitY = if (minProfit < 0) {
            padding + chartHeight - (((trendData.netProfit - minProfit) / (maxValue - minProfit)) * chartHeight).toFloat()
        } else {
            padding + chartHeight - ((trendData.netProfit / maxValue) * chartHeight).toFloat()
        }
        drawCircle(
            color = profitColor,
            radius = 4.dp.toPx(),
            center = Offset(x, profitY)
        )
    }
}

@Composable
private fun TrendSummary(trendData: List<TrendData>) {
    val totalIncome = trendData.sumOf { it.income }
    val totalExpenses = trendData.sumOf { it.expenses }
    val totalProfit = totalIncome - totalExpenses
    val averageIncome = if (trendData.isNotEmpty()) totalIncome / trendData.size else 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Period Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total Income",
                    value = AnalyticsCalculator.formatCurrency(totalIncome),
                    color = Color(0xFF4CAF50)
                )
                SummaryItem(
                    label = "Total Expenses",
                    value = AnalyticsCalculator.formatCurrency(totalExpenses),
                    color = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Net Profit",
                    value = AnalyticsCalculator.formatCurrency(totalProfit),
                    color = if (totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                SummaryItem(
                    label = "Avg Daily Income",
                    value = AnalyticsCalculator.formatCurrency(averageIncome),
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun EmptyTrendsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No trend data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add some entries to see trends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class TrendPeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}