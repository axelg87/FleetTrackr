package com.fleetmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
data class FabMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val iconTint: Color = Color.Unspecified,
    val backgroundColor: Color = Color.Unspecified
)

@Composable
fun FloatingActionButtonMenu(
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier,
    fabIcon: ImageVector = Icons.Default.Add,
    fabBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    fabContentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Use rememberUpdatedState to prevent unnecessary recompositions
    val currentItems by rememberUpdatedState(items)
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fab_rotation"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Invisible overlay to close menu when tapping outside
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isExpanded = false
                        }
                    }
            )
        }
        
        // Menu items
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(150)) + expandVertically(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(150))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 72.dp)
            ) {
                currentItems.forEach { item ->
                    key(item.label) {
                        FabMenuItemRow(
                            item = item,
                            onClick = {
                                item.onClick()
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = remember { { isExpanded = !isExpanded } },
            containerColor = fabBackgroundColor,
            contentColor = fabContentColor,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = fabIcon,
                contentDescription = if (isExpanded) "Close menu" else "Open menu",
                modifier = Modifier.rotate(rotationAngle)
            )
        }
    }
}

@Composable
private fun FabMenuItemRow(
    item: FabMenuItem,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable { currentOnClick() }
    ) {
        // Label
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        // Mini FAB
        Surface(
            shape = CircleShape,
            color = if (item.backgroundColor != Color.Unspecified) 
                item.backgroundColor 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (item.iconTint != Color.Unspecified) 
                        item.iconTint 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun createDefaultFabMenuItems(
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit
): List<FabMenuItem> {
    return listOf(
        FabMenuItem(
            icon = Icons.Default.TrendingUp,
            label = "Income",
            onClick = onIncomeClick,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary
        ),
        FabMenuItem(
            icon = Icons.Default.ReceiptLong,
            label = "Expense",
            onClick = onExpenseClick,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.error
        )
    )
}