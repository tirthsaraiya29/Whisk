package com.whisk.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Monochrome Standard Colors
val TrueBlack = Color(0xFF000000)
val TrueWhite = Color(0xFFFFFFFF)

// Obsidian Charcoal Dark Theme Palette
val DarkSurface = Color(0xFF0F0F10)
val DarkSurfaceVariant = Color(0xFF18181B)
val DarkSurfaceElevated = Color(0xFF27272A)
val DarkHairline = Color(0xFF27272A)
val DarkBorder = Color(0xFF3F3F46)
val DarkTextPrimary = Color(0xFFF4F4F5)
val DarkTextSecondary = Color(0xFFA1A1AA)
val DarkTextMuted = Color(0xFF71717A)

// Obsidian Paper Light Theme Palette
val LightSurface = Color(0xFFF8F9FA)
val LightSurfaceVariant = Color(0xFFF1F3F5)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightHairline = Color(0xFFE9ECEF)
val LightBorder = Color(0xFFCED4DA)
val LightTextPrimary = Color(0xFF212529)
val LightTextSecondary = Color(0xFF495057)
val LightTextMuted = Color(0xFF868E96)

// Brand & Semantic Accent Colors (Obsidian Purple & Crisp Slate)
val WhiskPurple = Color(0xFF7C3AED)
val WhiskBlue = Color(0xFF3B82F6)
val WhiskIndigo = Color(0xFF6366F1)
val NothingRed = Color(0xFFEF4444)
val StatusGreen = Color(0xFF10B981)
val StatusAmber = Color(0xFFF59E0B)

data class ThemeColorTokens(
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
)
