package com.fleetmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue80,
    onPrimary = Color(0xFF071425),
    primaryContainer = AccentNavy,
    onPrimaryContainer = Color(0xFFD9E7F8),
    secondary = SecondarySlate80,
    onSecondary = Color(0xFF0D141C),
    secondaryContainer = SecondarySlate40,
    onSecondaryContainer = Color(0xFFE2E6EB),
    tertiary = AccentTeal80,
    onTertiary = Color(0xFF00221B),
    tertiaryContainer = AccentTeal40,
    onTertiaryContainer = Color(0xFFD1F5EB),
    background = AppBackgroundBlack,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    outline = AppOutlineDark,
    inverseSurface = AppOnSurfaceDark,
    inverseOnSurface = AppSurfaceDark,
    inversePrimary = PrimaryBlue40
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue40,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue80,
    onPrimaryContainer = Color(0xFF04142A),
    secondary = SecondarySlate40,
    onSecondary = Color.White,
    secondaryContainer = SecondarySlate80,
    onSecondaryContainer = Color(0xFF1A232E),
    tertiary = AccentTeal40,
    onTertiary = Color.White,
    tertiaryContainer = AccentTeal80,
    onTertiaryContainer = Color(0xFF0A2F29),
    background = AppBackgroundBlack,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    outline = AppOutlineDark,
    inverseSurface = AppOnSurfaceDark,
    inverseOnSurface = AppSurfaceDark,
    inversePrimary = PrimaryBlue80
)

private fun enforceBackgroundPalette(colorScheme: ColorScheme): ColorScheme {
    return colorScheme.copy(
        background = AppBackgroundBlack,
        onBackground = AppOnBackgroundDark,
        surface = AppSurfaceDark,
        onSurface = AppOnSurfaceDark,
        surfaceVariant = AppSurfaceVariantDark,
        onSurfaceVariant = AppOnSurfaceVariantDark,
        outline = AppOutlineDark,
        inverseSurface = AppOnSurfaceDark,
        inverseOnSurface = AppSurfaceDark
    )
}

@Composable
fun FleetManagerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            enforceBackgroundPalette(dynamicScheme)
        }

        darkTheme -> enforceBackgroundPalette(DarkColorScheme)
        else -> enforceBackgroundPalette(LightColorScheme)
    }

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
            color = colorScheme.background
        ) {
            content()
        }
    }
}
