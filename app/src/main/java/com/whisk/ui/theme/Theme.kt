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

// Pure Brutalist Monochrome Dark Scheme (Vercel / Nothing Aesthetic)
private val BrutalistDarkColorScheme = darkColorScheme(
    primary = DarkTextPrimary,
    onPrimary = DarkSurface,
    background = DarkSurface,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkHairline,
    outlineVariant = DarkBorder,
    error = NothingRed,
    onError = DarkTextPrimary,
)

// High-Contrast Brutalist Light Scheme (Optimized for Outdoor Sunlight Legibility)
private val BrutalistLightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = LightSurface,
    background = LightSurface,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightHairline,
    outlineVariant = LightBorder,
    error = NothingRed,
    onError = LightSurface,
)

// Cyberpunk Neon Scheme
private val CyberpunkNeonColorScheme = darkColorScheme(
    primary = CyberpunkNeonBorder,
    onPrimary = CyberpunkNeonSurface,
    background = CyberpunkNeonSurface,
    onBackground = TrueWhite,
    surface = CyberpunkNeonSurface,
    onSurface = TrueWhite,
    surfaceVariant = CyberpunkNeonSurfaceVariant,
    onSurfaceVariant = CyberpunkNeonBorder.copy(alpha = 0.7f),
    outline = CyberpunkNeonBorder.copy(alpha = 0.5f),
    outlineVariant = CyberpunkNeonBorder,
    error = CyberpunkNeonAccent,
    onError = TrueWhite,
)

// Amber Terminal Scheme
private val AmberTerminalColorScheme = darkColorScheme(
    primary = AmberTerminalAccent,
    onPrimary = AmberTerminalSurface,
    background = AmberTerminalSurface,
    onBackground = AmberTerminalAccent,
    surface = AmberTerminalSurface,
    onSurface = AmberTerminalAccent,
    surfaceVariant = AmberTerminalSurfaceVariant,
    onSurfaceVariant = AmberTerminalAccent.copy(alpha = 0.7f),
    outline = AmberTerminalBorder.copy(alpha = 0.4f),
    outlineVariant = AmberTerminalBorder,
    error = NothingRed,
    onError = AmberTerminalSurface,
)

// Monokai Pro Scheme
private val MonokaiColorScheme = darkColorScheme(
    primary = MonokaiAccent,
    onPrimary = MonokaiSurface,
    background = MonokaiSurface,
    onBackground = TrueWhite,
    surface = MonokaiSurface,
    onSurface = TrueWhite,
    surfaceVariant = MonokaiSurfaceVariant,
    onSurfaceVariant = TrueWhite.copy(alpha = 0.7f),
    outline = MonokaiBorder,
    outlineVariant = MonokaiAccent,
    error = NothingRed,
    onError = TrueWhite,
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
        "DARK", "CYBERPUNK", "AMBER_TERMINAL", "MONOKAI", "NORDIC" -> true
        else -> systemDark
    }

    val baseColorScheme = when (themeMode) {
        "LIGHT" -> BrutalistLightColorScheme
        "CYBERPUNK" -> CyberpunkNeonColorScheme
        "AMBER_TERMINAL" -> AmberTerminalColorScheme
        "MONOKAI" -> MonokaiColorScheme
        "DARK" -> BrutalistDarkColorScheme
        else -> if (systemDark) BrutalistDarkColorScheme else BrutalistLightColorScheme
    }

    // Apply custom accent color if chosen
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
