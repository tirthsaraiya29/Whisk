package com.whisk.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Monochrome Standard Colors
val TrueBlack = Color(0xFF000000)
val TrueWhite = Color(0xFFFFFFFF)

// Charcoal Dark Theme (Obsidian & Linear Inspired OLED/Slate)
val DarkSurface = Color(0xFF0B0F17)
val DarkSurfaceVariant = Color(0xFF131A26)
val DarkSurfaceElevated = Color(0xFF1E293B)
val DarkHairline = Color(0xFF1E293B)
val DarkBorder = Color(0xFF334155)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// Warm Light Theme (Craft & Notion Inspired Paper Light)
val LightSurface = Color(0xFFFAFAF8)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightHairline = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

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

// Brand & Semantic Accent Colors
val WhiskBlue = Color(0xFF3B82F6)
val WhiskIndigo = Color(0xFF6366F1)
val NothingRed = Color(0xFFEF4444)
val StatusGreen = Color(0xFF10B981)
val StatusAmber = Color(0xFFF59E0B)
val StatusPurple = Color(0xFF8B5CF6)

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
