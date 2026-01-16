package com.orbital.run.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme for DrawRun
 *
 * Inspired by Linear's clarity and Apple's aesthetics.
 * Uses semantic naming for easy maintenance.
 */
private val LightColorScheme = lightColorScheme(
    // Primary brand color
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurfaceContainer,
    onPrimaryContainer = LightOnSurface,
    
    // Secondary/Accent
    secondary = LightAccent,
    onSecondary = LightOnAccent,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    
    // Tertiary (for additional accents)
    tertiary = LightWarning,
    onTertiary = LightOnPrimary,
    tertiaryContainer = LightSurfaceVariant,
    onTertiaryContainer = LightOnSurface,
    
    // Backgrounds & Surfaces
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    // Borders & Dividers
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    
    // States
    error = LightError,
    onError = LightOnError,
    errorContainer = LightSurfaceVariant,
    onErrorContainer = LightError
)

/**
 * Dark color scheme for DrawRun
 *
 * Uses deep grays (Linear-inspired) instead of pure black
 * for reduced eye strain and premium feel.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary brand color (adjusted for dark mode)
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceContainer,
    onPrimaryContainer = DarkOnSurface,
    
    // Secondary/Accent
    secondary = DarkAccent,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    
    // Tertiary
    tertiary = DarkWarning,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkOnSurface,
    
    // Backgrounds & Surfaces
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    // Borders & Dividers
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    
    // States
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkSurfaceVariant,
    onErrorContainer = DarkError
)

/**
 * DrawRun Material3 Theme
 *
 * Automatically adapts to system dark mode setting.
 * Sets status bar colors to match theme.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param content Composable content to wrap with theme
 */
@Composable
fun DrawRunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Set status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
