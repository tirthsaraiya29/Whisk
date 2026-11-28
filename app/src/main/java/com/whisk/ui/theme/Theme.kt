package com.whisk.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Obsidian Charcoal Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = WhiskPurple,
    onPrimary = TrueWhite,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = DarkTextPrimary,
    background = DarkSurface,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceVariant,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkHairline,
    outlineVariant = DarkBorder,
    error = NothingRed,
    onError = TrueWhite,
)

// Obsidian Paper Light Scheme
private val LightColorScheme = lightColorScheme(
    primary = WhiskPurple,
    onPrimary = TrueWhite,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    background = LightSurface,
    onBackground = LightTextPrimary,
    surface = LightSurfaceElevated,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightHairline,
    outlineVariant = LightBorder,
    error = NothingRed,
    onError = LightSurface,
)

@Composable
fun WhiskTheme(
    themeMode: String = "SYSTEM",
    accentColor: String? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }

    val baseColorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val colorScheme = if (!accentColor.isNullOrBlank()) {
        val accent = try {
            Color(android.graphics.Color.parseColor(accentColor))
        } catch (_: Exception) {
            baseColorScheme.primary
        }
        baseColorScheme.copy(primary = accent)
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
