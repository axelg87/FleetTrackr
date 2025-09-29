package com.fleetmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.fleetmanager.R
import com.fleetmanager.ui.navigation.DashboardShortcut
import coil.compose.AsyncImage
import coil.request.ImageRequest

// Screen Header Component with Company Logo and Profile Icon
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = true,
    userName: String? = null,
    profilePictureUrl: String? = null,
    onProfileClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showLogo) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "AG Motion Logo",
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(content = actions)
            
            // Show profile icon if userName is provided
            userName?.let { name ->
                ProfileIcon(
                    userName = name,
                    size = 40,
                    profilePictureUrl = profilePictureUrl,
                    onClick = onProfileClick
                )
            }
        }
    }
}

// Stat Card Component
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trend: List<Double> = emptyList(),
    trendColor: Color = Color.Unspecified
) {
    val cardModifier = if (onClick != null) {
        modifier.then(Modifier.clickable { onClick() })
    } else modifier

    val resolvedTrendColor = if (trendColor == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else trendColor
    val showTrend = trend.size >= 2 && trend.any { it != trend.first() }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                if (showTrend) {
                    SparklineChart(
                        values = trend,
                        modifier = Modifier
                            .width(72.dp)
                            .height(28.dp),
                        lineColor = resolvedTrendColor,
                        fillColor = resolvedTrendColor.copy(alpha = 0.18f)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Stats Grid Component - 2x2 grid layout
@Composable
fun StatsGrid(
    stats: List<StatItem>,
    modifier: Modifier = Modifier,
    onStatClick: ((StatItem) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            stats.getOrNull(0)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick ?: if (onStatClick != null) { { onStatClick(stat) } } else null,
                    modifier = Modifier.weight(1f),
                    trend = stat.trend,
                    trendColor = stat.trendColor
                )
            }
            stats.getOrNull(1)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick ?: if (onStatClick != null) { { onStatClick(stat) } } else null,
                    modifier = Modifier.weight(1f),
                    trend = stat.trend,
                    trendColor = stat.trendColor
                )
            }
        }

        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            stats.getOrNull(2)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick ?: if (onStatClick != null) { { onStatClick(stat) } } else null,
                    modifier = Modifier.weight(1f),
                    trend = stat.trend,
                    trendColor = stat.trendColor
                )
            }
            stats.getOrNull(3)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick ?: if (onStatClick != null) { { onStatClick(stat) } } else null,
                    modifier = Modifier.weight(1f),
                    trend = stat.trend,
                    trendColor = stat.trendColor
                )
            }
        }
    }
}

// Action Button Component
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    if (isPrimary) {
        ElevatedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}

// Quick Actions Row Component
@Composable
fun QuickActionsRow(
    actions: List<ActionItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            ActionButton(
                text = action.text,
                icon = action.icon,
                onClick = action.onClick,
                isPrimary = action.isPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Section Header Component
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        action?.invoke()
    }
}

// Empty State Component
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onActionClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionText)
                }
            }
        }
    }
}

// Status Card Component (for sync, errors, etc.)
@Composable
fun StatusCard(
    type: StatusType,
    message: String,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (type) {
        StatusType.Loading -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Sync
        )
        StatusType.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        StatusType.Success -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (type == StatusType.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun SparklineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.2f)
) {
    if (values.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
    val normalizedValues = values.map { value ->
        if (maxValue == minValue) {
            0.5f
        } else {
            ((value - minValue) / range).toFloat()
        }
    }

    Canvas(
        modifier = modifier
    ) {
        val height = size.height
        val width = size.width
        val stepX = if (normalizedValues.size > 1) {
            width / (normalizedValues.size - 1)
        } else {
            width
        }

        val linePath = Path()
        val fillPath = Path()

        normalizedValues.forEachIndexed { index, value ->
            val x = if (normalizedValues.size > 1) stepX * index else width
            val y = height - (value * height)

            if (index == 0) {
                linePath.moveTo(0f, y)
                fillPath.moveTo(0f, height)
                fillPath.lineTo(0f, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = fillColor
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// Data Classes
data class StatItem(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val onClick: (() -> Unit)? = null,
    val shortcut: DashboardShortcut? = null,
    val trend: List<Double> = emptyList(),
    val trendColor: Color = Color.Unspecified
)

data class ActionItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isPrimary: Boolean = true
)

enum class StatusType {
    Loading, Error, Success
}

// Profile Icon Component - Shows user profile picture, initials, or default icon
@Composable
fun ProfileIcon(
    userName: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    profilePictureUrl: String? = null,
    onClick: (() -> Unit)? = null
) {
    val initials = remember(userName) {
        getInitials(userName)
    }
    
    val profileModifier = if (onClick != null) {
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    } else {
        modifier
            .size(size.dp)
            .clip(CircleShape)
    }
    
    if (profilePictureUrl != null) {
        // Show profile picture
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profilePictureUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            modifier = profileModifier
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop
        )
    } else {
        // Show initials or default icon
        Box(
            modifier = profileModifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (size * 0.4).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((size * 0.6).dp)
                )
            }
        }
    }
}

// Helper function to extract initials from a name
private fun getInitials(name: String): String {
    if (name.isBlank()) return ""
    
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> {
            // Take first letter of first word and first letter of last word
            "${words.first().first().uppercase()}${words.last().first().uppercase()}"
        }
        words.size == 1 -> {
            // Take first two letters of the single word if possible, otherwise just one
            val word = words.first()
            if (word.length >= 2) {
                word.take(2).uppercase()
            } else {
                word.take(1).uppercase()
            }
        }
        else -> ""
    }
}

// Common profile click handler for consistent behavior
@Composable
fun rememberProfileClickHandler(onNavigateToProfile: () -> Unit): () -> Unit {
    return remember(onNavigateToProfile) {
        {
            onNavigateToProfile()
        }
    }
}
