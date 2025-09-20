package com.fleetmanager.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

object FleetThemeDefaults {
    val screenBackgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(FleetBackground, FleetBackground)
    )
}

fun Modifier.fleetScreenBackground(): Modifier =
    background(FleetThemeDefaults.screenBackgroundBrush)
