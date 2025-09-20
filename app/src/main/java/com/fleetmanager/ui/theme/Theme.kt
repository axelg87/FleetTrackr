package com.fleetmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FleetDarkColorScheme = darkColorScheme(
    primary = FleetPrimary,
    onPrimary = FleetOnPrimary,
    primaryContainer = FleetPrimaryContainer,
    onPrimaryContainer = FleetOnPrimaryContainer,
    secondary = FleetSecondary,
    onSecondary = FleetOnSecondary,
    secondaryContainer = FleetSecondaryContainer,
    onSecondaryContainer = FleetOnSecondaryContainer,
    tertiary = FleetTertiary,
    onTertiary = FleetOnTertiary,
    tertiaryContainer = FleetTertiaryContainer,
    onTertiaryContainer = FleetOnTertiaryContainer,
    background = FleetBackground,
    onBackground = FleetOnBackground,
    surface = FleetSurface,
    onSurface = FleetOnSurface,
    surfaceVariant = FleetSurfaceVariant,
    onSurfaceVariant = FleetOnSurfaceVariant,
    outline = FleetOutline,
    outlineVariant = FleetOutlineVariant,
    scrim = FleetScrim,
    inverseSurface = FleetInverseSurface,
    inverseOnSurface = FleetInverseOnSurface,
    inversePrimary = FleetInversePrimary
)

private val FleetLightColorScheme = lightColorScheme(
    primary = FleetPrimary,
    onPrimary = FleetOnPrimary,
    primaryContainer = FleetPrimaryContainer,
    onPrimaryContainer = FleetOnPrimaryContainer,
    secondary = FleetSecondary,
    onSecondary = FleetOnSecondary,
    secondaryContainer = FleetSecondaryContainer,
    onSecondaryContainer = FleetOnSecondaryContainer,
    tertiary = FleetTertiary,
    onTertiary = FleetOnTertiary,
    tertiaryContainer = FleetTertiaryContainer,
    onTertiaryContainer = FleetOnTertiaryContainer,
    background = FleetBackground,
    onBackground = FleetOnBackground,
    surface = FleetSurface,
    onSurface = FleetOnSurface,
    surfaceVariant = FleetSurfaceVariant,
    onSurfaceVariant = FleetOnSurfaceVariant,
    outline = FleetOutline,
    outlineVariant = FleetOutlineVariant,
    scrim = FleetScrim,
    inverseSurface = FleetInverseSurface,
    inverseOnSurface = FleetInverseOnSurface,
    inversePrimary = FleetInversePrimary
)

@Composable
fun FleetManagerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDarkTheme = if (dynamicColor) isSystemInDarkTheme() else darkTheme
    val colorScheme = if (isDarkTheme) FleetDarkColorScheme else FleetLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            contentColor = colorScheme.onBackground
        ) {
            content()
        }
    }
}
