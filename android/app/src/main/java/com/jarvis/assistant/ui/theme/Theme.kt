package com.jarvis.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisDarkColorScheme = darkColorScheme(
    // Primary
    primary = JarvisCyan,
    onPrimary = JarvisBlack,
    primaryContainer = JarvisCyanDark,
    onPrimaryContainer = JarvisCyanLight,

    // Secondary
    secondary = JarvisGold,
    onSecondary = JarvisBlack,
    secondaryContainer = JarvisOrange,
    onSecondaryContainer = JarvisGold,

    // Tertiary
    tertiary = JarvisAmber,
    onTertiary = JarvisBlack,
    tertiaryContainer = JarvisOrange,
    onTertiaryContainer = JarvisAmber,

    // Background
    background = JarvisBlack,
    onBackground = JarvisTextPrimary,

    // Surface
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    surfaceTint = JarvisCyan,

    // Inverse
    inverseSurface = JarvisTextPrimary,
    inverseOnSurface = JarvisBlack,
    inversePrimary = JarvisCyanDark,

    // Error
    error = JarvisError,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Outline
    outline = JarvisNavy,
    outlineVariant = JarvisDarkBlue,

    // Scrim
    scrim = Color.Black
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = JarvisDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JarvisTypography,
        content = content
    )
}
