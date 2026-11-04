package com.whisk.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Brutalist Monochrome Palette
val TrueBlack = Color(0xFF000000)
val TrueWhite = Color(0xFFFFFFFF)

// Dark Theme Delineations (OLED High Contrast)
val DarkSurface = Color(0xFF000000)
val DarkSurfaceVariant = Color(0xFF0A0A0A)
val DarkHairline = Color(0xFF262626)
val DarkBorder = Color(0xFF404040)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA3A3A3)
val DarkTextMuted = Color(0xFF737373)

// Light Theme Delineations (Enhanced Sunlight High Contrast)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F0)
val LightHairline = Color(0xFF737373)
val LightBorder = Color(0xFF171717)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF404040)
val LightTextMuted = Color(0xFF737373)

// Cyberpunk Neon Palette
val CyberpunkNeonSurface = Color(0xFF05050A)
val CyberpunkNeonSurfaceVariant = Color(0xFF10101C)
val CyberpunkNeonBorder = Color(0xFF00F0FF)
val CyberpunkNeonAccent = Color(0xFFFF007F)

// Amber Terminal Palette
val AmberTerminalSurface = Color(0xFF0D0B00)
val AmberTerminalSurfaceVariant = Color(0xFF1A1600)
val AmberTerminalBorder = Color(0xFFFFB000)
val AmberTerminalAccent = Color(0xFFFFCC00)

// Monokai Pro Palette
val MonokaiSurface = Color(0xFF19181A)
val MonokaiSurfaceVariant = Color(0xFF221F22)
val MonokaiBorder = Color(0xFF49464E)
val MonokaiAccent = Color(0xFFFFD866)

// Theme Accents
val NothingRed = Color(0xFFD71921)
val VercelCyan = Color(0xFF00F0FF)
val StatusGreen = Color(0xFF10B981)
val AmberTerminal = Color(0xFFFFB000)
val NeonPurple = Color(0xFFA855F7)
val ElectricBlue = Color(0xFF3B82F6)

data class ThemeColorTokens(
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
)
