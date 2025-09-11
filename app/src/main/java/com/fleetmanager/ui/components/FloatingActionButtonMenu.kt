package com.fleetmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun FloatingActionButtonMenu(
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Background overlay when expanded - removed to prevent blocking touch events
        // The menu will close when clicking outside due to state management

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.zIndex(2f)
        ) {
            // Income FAB
            AnimatedVisibility(
                visible = isExpanded,
                enter = scaleIn(
                    animationSpec = tween(300, delayMillis = 50)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 50)),
                exit = scaleOut(
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                FabMenuItem(
                    text = "Income",
                    icon = Icons.Default.AttachMoney,
                    onClick = {
                        isExpanded = false
                        onIncomeClick()
                    },
                    backgroundColor = MaterialTheme.colorScheme.secondary
                )
            }

            // Expense FAB
            AnimatedVisibility(
                visible = isExpanded,
                enter = scaleIn(
                    animationSpec = tween(300, delayMillis = 100)
                ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
                exit = scaleOut(
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                FabMenuItem(
                    text = "Expense",
                    icon = Icons.Default.MoneyOff,
                    onClick = {
                        isExpanded = false
                        onExpenseClick()
                    },
                    backgroundColor = MaterialTheme.colorScheme.error
                )
            }

            // Main FAB
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close menu" else "Open menu",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun FabMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Label
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // FAB
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = backgroundColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White
            )
        }
    }
}