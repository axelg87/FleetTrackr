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
    primary = Purple80,
    onPrimary = Color(0xFF2B1947),
    primaryContainer = Color(0xFF4A3A7A),
    onPrimaryContainer = Color(0xFFF3E3FF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF1F1A2B),
    secondaryContainer = Color(0xFF383248),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Pink80,
    onTertiary = Color(0xFF420016),
    tertiaryContainer = Color(0xFF65112F),
    onTertiaryContainer = Color(0xFFFFD9E0),
    background = AppBackgroundBlack,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    outline = AppOutlineDark,
    inverseSurface = AppOnSurfaceDark,
    inverseOnSurface = AppSurfaceDark,
    inversePrimary = Purple40
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Color(0xFF1F1147),
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = PurpleGrey80,
    onSecondaryContainer = Color(0xFF201B2C),
    tertiary = Pink40,
    onTertiary = Color.White,
    tertiaryContainer = Pink80,
    onTertiaryContainer = Color(0xFF3D001A),
    background = AppBackgroundBlack,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    outline = AppOutlineDark,
    inverseSurface = AppOnSurfaceDark,
    inverseOnSurface = AppSurfaceDark,
    inversePrimary = Purple80
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
