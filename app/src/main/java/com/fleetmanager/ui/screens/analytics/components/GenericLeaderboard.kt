package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

/**
 * GENERALIZATION: Reusable leaderboard component for ranking displays
 * Eliminates duplication between TopDriversLeaderboard, DriverComparison, VehicleROI rankings
 */

// Leaderboard data models
data class LeaderboardItem(
    val id: String,
    val name: String,
    val primaryValue: Double,
    val primaryLabel: String = "Revenue",
    val secondaryValue: Double? = null,
    val secondaryLabel: String? = null,
    val tertiaryValue: String? = null,
    val tertiaryLabel: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

enum class LeaderboardStyle {
    PODIUM,     // Top 3 with podium display
    RANKED_LIST, // Numbered list with ranking
    PROGRESS_BARS, // Progress bars with values
    CARDS       // Card-based layout
}

enum class SortOption(val displayName: String) {
    PRIMARY_VALUE("Primary Value"),
    SECONDARY_VALUE("Secondary Value"),
    ALPHABETICAL("A-Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericLeaderboard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.EmojiEvents,
    data: List<LeaderboardItem>,
    style: LeaderboardStyle = LeaderboardStyle.RANKED_LIST,
    maxItems: Int = 10,
    showSortOptions: Boolean = true,
    isLoading: Boolean = false,
    customSortOptions: List<SortOption> = emptyList(),
    onItemClick: ((LeaderboardItem) -> Unit)? = null,
    summaryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { 
        mutableStateOf(
            customSortOptions.firstOrNull() ?: SortOption.PRIMARY_VALUE
        ) 
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            LeaderboardHeader(
                title = title,
                subtitle = subtitle,
                icon = icon
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                LeaderboardLoadingState()
            } else if (data.isEmpty()) {
                LeaderboardEmptyState()
            } else {
                // Sort options
                if (showSortOptions && (customSortOptions.isNotEmpty() || data.size > 3)) {
                    LeaderboardSortSelector(
                        options = customSortOptions.ifEmpty { 
                            listOf(SortOption.PRIMARY_VALUE, SortOption.ALPHABETICAL) 
                        },
                        selectedSort = selectedSort,
                        onSortSelected = { selectedSort = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Sorted data
                val sortedData = when (selectedSort) {
                    SortOption.PRIMARY_VALUE -> data.sortedByDescending { it.primaryValue }
                    SortOption.SECONDARY_VALUE -> data.sortedByDescending { it.secondaryValue ?: 0.0 }
                    SortOption.ALPHABETICAL -> data.sortedBy { it.name }
                }.take(maxItems)
                
                // Leaderboard content
                when (style) {
                    LeaderboardStyle.PODIUM -> {
                        PodiumLeaderboard(
                            data = sortedData.take(3),
                            onItemClick = onItemClick
                        )
                        
                        if (sortedData.size > 3) {
                            Spacer(modifier = Modifier.height(16.dp))
                            RankedListLeaderboard(
                                data = sortedData.drop(3),
                                startRank = 4,
                                onItemClick = onItemClick
                            )
                        }
                    }
                    LeaderboardStyle.RANKED_LIST -> {
                        RankedListLeaderboard(
                            data = sortedData,
                            onItemClick = onItemClick
                        )
                    }
                    LeaderboardStyle.PROGRESS_BARS -> {
                        ProgressBarLeaderboard(
                            data = sortedData,
                            onItemClick = onItemClick
                        )
                    }
                    LeaderboardStyle.CARDS -> {
                        CardLeaderboard(
                            data = sortedData,
                            onItemClick = onItemClick
                        )
                    }
                }
                
                // Summary content
                summaryContent?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    it()
                }
            }
        }
    }
}

@Composable
private fun LeaderboardHeader(
    title: String,
    subtitle: String?,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AnalyticsUtils.Colors.GOLD,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeaderboardLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LeaderboardEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No leaderboard data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add some data to see rankings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardSortSelector(
    options: List<SortOption>,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedSort
            
            TextButton(
                onClick = { onSortSelected(option) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else Color.Transparent,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.displayName,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PodiumLeaderboard(
    data: List<LeaderboardItem>,
    onItemClick: ((LeaderboardItem) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place
        if (data.size >= 2) {
            PodiumPosition(
                item = data[1],
                rank = 2,
                height = 80.dp,
                color = AnalyticsUtils.Colors.SILVER,
                onClick = { onItemClick?.invoke(data[1]) }
            )
        }
        
        // 1st place (tallest)
        if (data.isNotEmpty()) {
            PodiumPosition(
                item = data[0],
                rank = 1,
                height = 100.dp,
                color = AnalyticsUtils.Colors.GOLD,
                onClick = { onItemClick?.invoke(data[0]) }
            )
        }
        
        // 3rd place
        if (data.size >= 3) {
            PodiumPosition(
                item = data[2],
                rank = 3,
                height = 60.dp,
                color = AnalyticsUtils.Colors.BRONZE,
                onClick = { onItemClick?.invoke(data[2]) }
            )
        }
    }
}

@Composable
private fun PodiumPosition(
    item: LeaderboardItem,
    rank: Int,
    height: Dp,
    color: Color,
    onClick: () -> Unit
) {
    val animatedHeight by animateFloatAsState(
        targetValue = height.value,
        animationSpec = tween(1000),
        label = "podium_height"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // Crown for 1st place
        if (rank == 1) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AnalyticsUtils.Colors.GOLD,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Item avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = AnalyticsUtils.getAlphaColor(color, 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Item name
        Text(
            text = item.name.split(" ").firstOrNull() ?: item.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        // Primary value
        Text(
            text = AnalyticsUtils.formatCurrency(item.primaryValue),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Podium base
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(animatedHeight.dp)
                .background(
                    color = AnalyticsUtils.getAlphaColor(color, 0.3f),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RankedListLeaderboard(
    data: List<LeaderboardItem>,
    startRank: Int = 1,
    onItemClick: ((LeaderboardItem) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(data) { index, item ->
            val rank = startRank + index
            RankedListItem(
                item = item,
                rank = rank,
                maxValue = data.maxOfOrNull { it.primaryValue } ?: 1.0,
                onClick = { onItemClick?.invoke(item) }
            )
        }
    }
}

@Composable
private fun RankedListItem(
    item: LeaderboardItem,
    rank: Int,
    maxValue: Double,
    onClick: () -> Unit
) {
    val progressPercentage = AnalyticsUtils.calculateProgress(item.primaryValue, maxValue)
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "rank_progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AnalyticsUtils.getRankingBackgroundColor(rank)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AnalyticsUtils.getAlphaColor(AnalyticsUtils.getRankingColor(rank), 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnalyticsUtils.getRankingColor(rank)
                )
            }
            
            // Item info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.secondaryValue?.let { value ->
                        Text(
                            text = "AED{item.secondaryLabel ?: "Secondary"}: AED{AnalyticsUtils.formatCurrency(value)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    item.tertiaryValue?.let { value ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "AED{item.tertiaryLabel ?: "Other"}: AEDvalue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Primary value and progress
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = AnalyticsUtils.formatCurrency(item.primaryValue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBarLeaderboard(
    data: List<LeaderboardItem>,
    onItemClick: ((LeaderboardItem) -> Unit)?
) {
    val maxValue = data.maxOfOrNull { it.primaryValue } ?: 1.0
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { item ->
            ProgressBarLeaderboardItem(
                item = item,
                maxValue = maxValue,
                onClick = { onItemClick?.invoke(item) }
            )
        }
    }
}

@Composable
private fun ProgressBarLeaderboardItem(
    item: LeaderboardItem,
    maxValue: Double,
    onClick: () -> Unit
) {
    val progressPercentage = AnalyticsUtils.calculateProgress(item.primaryValue, maxValue)
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "progress_bar"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = AnalyticsUtils.formatCurrency(item.primaryValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun CardLeaderboard(
    data: List<LeaderboardItem>,
    onItemClick: ((LeaderboardItem) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(data) { index, item ->
            CardLeaderboardItem(
                item = item,
                rank = index + 1,
                onClick = { onItemClick?.invoke(item) }
            )
        }
    }
}

@Composable
private fun CardLeaderboardItem(
    item: LeaderboardItem,
    rank: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#AEDrank AED{item.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AnalyticsUtils.formatCurrency(item.primaryValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (item.secondaryValue != null || item.tertiaryValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    item.secondaryValue?.let { value ->
                        Column {
                            Text(
                                text = item.secondaryLabel ?: "Secondary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AnalyticsUtils.formatCurrency(value),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    item.tertiaryValue?.let { value ->
                        Column {
                            Text(
                                text = item.tertiaryLabel ?: "Other",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}