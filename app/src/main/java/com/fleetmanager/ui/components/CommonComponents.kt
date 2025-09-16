package com.fleetmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.fleetmanager.R

// Screen Header Component with Company Logo and Profile Icon
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    showLogo: Boolean = true,
    userName: String? = null,
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_company_logo),
                    contentDescription = "AG Motion Logo",
                    modifier = Modifier.size(40.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified
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
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier.then(Modifier.clickable { onClick() })
    } else modifier

    Card(
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Stats Grid Component - 2x2 grid layout
@Composable
fun StatsGrid(
    stats: List<StatItem>,
    modifier: Modifier = Modifier
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
                    onClick = stat.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
            stats.getOrNull(1)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick,
                    modifier = Modifier.weight(1f)
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
                    onClick = stat.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
            stats.getOrNull(3)?.let { stat ->
                StatCard(
                    icon = stat.icon,
                    value = stat.value,
                    label = stat.label,
                    onClick = stat.onClick,
                    modifier = Modifier.weight(1f)
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
        modifier = modifier.fillMaxWidth()
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
        colors = CardDefaults.cardColors(containerColor = containerColor)
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

// Data Classes
data class StatItem(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val onClick: (() -> Unit)? = null
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

// Profile Icon Component - Shows user initials or default icon
@Composable
fun ProfileIcon(
    userName: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
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
fun rememberProfileClickHandler(): () -> Unit {
    return remember {
        {
            // TODO: Implement profile menu or navigation
            // For now, this is a placeholder that could:
            // 1. Show a profile menu with options like "View Profile", "Account Settings", "Sign Out"
            // 2. Navigate to a dedicated profile screen
            // 3. Open a bottom sheet with user information
        }
    }
}