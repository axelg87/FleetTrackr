package com.fleetmanager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fleet branded loading wheel used for inline progress indicators.
 */
@Composable
fun FleetLoadingWheel(
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 24.dp,
    strokeWidth: Dp = 3.dp,
    colors: List<Color> = emptyList(),
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
) {
    val colorScheme = MaterialTheme.colorScheme
    val indicatorColors = remember(colors, colorScheme) {
        if (colors.isNotEmpty()) {
            colors
        } else {
            listOf(
                colorScheme.primary,
                colorScheme.tertiary,
                colorScheme.primary
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "fleet_loading_wheel_transition")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing)
        ),
        label = "fleet_loading_wheel_rotation"
    )

    val sweep by transition.animateFloat(
        initialValue = 70f,
        targetValue = 320f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fleet_loading_wheel_sweep"
    )

    Canvas(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .size(indicatorSize)
    ) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val resolvedTrackColor = if (trackColor.alpha > 0f) trackColor else colorScheme.primary.copy(alpha = 0.2f)

        drawArc(
            color = resolvedTrackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
            alpha = 0.35f
        )

        drawArc(
            brush = Brush.sweepGradient(colors = indicatorColors, center = center),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = stroke
        )

        if (sweep > 0f) {
            val radius = (size.minDimension / 2f).coerceAtLeast(0f) - stroke.width / 2f
            val endAngle = (-90.0 + sweep.toDouble()) * PI / 180.0
            val dotCenter = Offset(
                x = center.x + cos(endAngle).toFloat() * radius,
                y = center.y + sin(endAngle).toFloat() * radius
            )

            val glowRadius = stroke.width * 1.8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(indicatorColors.last(), indicatorColors.last().copy(alpha = 0f)),
                    center = dotCenter,
                    radius = glowRadius * 1.6f
                ),
                radius = glowRadius,
                center = dotCenter
            )
        }
    }
}
