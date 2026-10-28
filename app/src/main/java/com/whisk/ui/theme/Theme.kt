package com.whisk.ui.theme

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

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianPrimary,
    onPrimary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianSurfaceVariant
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    onPrimary = CyberpunkBackground,
    background = CyberpunkBackground,
    onBackground = CyberpunkTextPrimary,
    surface = CyberpunkSurface,
    onSurface = CyberpunkTextPrimary,
    surfaceVariant = CyberpunkSurfaceVariant,
    onSurfaceVariant = CyberpunkTextSecondary,
    outline = CyberpunkSurfaceVariant
)

private val SolarizedColorScheme = lightColorScheme(
    primary = SolarizedPrimary,
    onPrimary = SolarizedBackground,
    background = SolarizedBackground,
    onBackground = SolarizedTextPrimary,
    surface = SolarizedSurface,
    onSurface = SolarizedTextPrimary,
    surfaceVariant = SolarizedSurfaceVariant,
    onSurfaceVariant = SolarizedTextSecondary,
    outline = SolarizedSurfaceVariant
)

private val MonokaiColorScheme = darkColorScheme(
    primary = MonokaiPrimary,
    onPrimary = MonokaiBackground,
    background = MonokaiBackground,
    onBackground = MonokaiTextPrimary,
    surface = MonokaiSurface,
    onSurface = MonokaiTextPrimary,
    surfaceVariant = MonokaiSurfaceVariant,
    onSurfaceVariant = MonokaiTextSecondary,
    outline = MonokaiSurfaceVariant
)

private val NordicColorScheme = darkColorScheme(
    primary = NordicPrimary,
    onPrimary = NordicBackground,
    background = NordicBackground,
    onBackground = NordicTextPrimary,
    surface = NordicSurface,
    onSurface = NordicTextPrimary,
    surfaceVariant = NordicSurfaceVariant,
    onSurfaceVariant = NordicTextSecondary,
    outline = NordicSurfaceVariant
)

@Composable
fun WhiskTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        "CYBERPUNK" -> CyberpunkColorScheme
        "SOLARIZED" -> SolarizedColorScheme
        "MONOKAI" -> MonokaiColorScheme
        "NORDIC" -> NordicColorScheme
        "LIGHT" -> SolarizedColorScheme
        "DARK" -> ObsidianColorScheme
        else -> if (systemDark) ObsidianColorScheme else SolarizedColorScheme
    }

    val isDark = colorScheme.background == ObsidianBackground ||
            colorScheme.background == CyberpunkBackground ||
            colorScheme.background == MonokaiBackground ||
            colorScheme.background == NordicBackground

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
