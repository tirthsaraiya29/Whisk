package com.whisk.ui.components.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Jetpack Compose Liquid Glass Material Specification.
 * Provides backdrop blur (RenderEffect on API 31+), dual optical rim lighting,
 * specular gloss gradient sheen, theme-adaptive glass tinting, and elevated drop shadow.
 */
object LiquidGlassDefaults {
    val PillShape = CircleShape
    val CardShape = RoundedCornerShape(20.dp)

    // Light Theme Glass Palette
    val LightGlassTint = Color(0xF2F8FAFC) // 95% translucent crisp slate white
    val LightGlassBorderHighlight = Color(0xE6FFFFFF) // Bright top/left rim highlight
    val LightGlassBorderShadow = Color(0x33000000) // Soft bottom/right rim shadow
    val LightGlassSpecular = Color(0x7FFFFFFF) // Specular gloss sheen
    val LightGlassShadowColor = Color(0x26000000)

    // Dark Theme Glass Palette
    val DarkGlassTint = Color(0xEB0E1726) // 92% translucent dark obsidian
    val DarkGlassBorderHighlight = Color(0x80FFFFFF) // Refractive top/left rim stroke
    val DarkGlassBorderShadow = Color(0x99000000) // Deep bottom/right rim shadow
    val DarkGlassSpecular = Color(0x40FFFFFF) // Specular gloss sheen
    val DarkGlassShadowColor = Color(0x80000000)

    // Selected Lens Glass Palette
    val LightLensTint = Color(0x330F172A) // Slate glass lens fill in light mode
    val DarkLensTint = Color(0x6638BDF8) // Vibrant sky cyan glass lens fill in dark mode

    // High-Contrast Content Colors
    val LightActiveContent = Color(0xFF0F172A)
    val LightInactiveContent = Color(0x99334155)
    val DarkActiveContent = Color(0xFFFFFFFF)
    val DarkActiveAccent = Color(0xFF38BDF8)
    val DarkInactiveContent = Color(0xB394A3B8)
}

/**
 * Modifier applying the Liquid Glass material treatment to a background surface.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = LiquidGlassDefaults.PillShape,
    blurRadius: Dp = 24.dp,
    elevation: Dp = 10.dp,
    tintColor: Color? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
): Modifier {
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { blurRadius.toPx() }

    val baseTint = tintColor ?: if (isDarkTheme) LiquidGlassDefaults.DarkGlassTint else LiquidGlassDefaults.LightGlassTint
    val borderHighlight = if (isDarkTheme) LiquidGlassDefaults.DarkGlassBorderHighlight else LiquidGlassDefaults.LightGlassBorderHighlight
    val borderShadow = if (isDarkTheme) LiquidGlassDefaults.DarkGlassBorderShadow else LiquidGlassDefaults.LightGlassBorderShadow
    val specularGloss = if (isDarkTheme) LiquidGlassDefaults.DarkGlassSpecular else LiquidGlassDefaults.LightGlassSpecular
    val shadowColor = if (isDarkTheme) LiquidGlassDefaults.DarkGlassShadowColor else LiquidGlassDefaults.LightGlassShadowColor

    val glassBorderBrush = remember(borderHighlight, borderShadow) {
        Brush.linearGradient(
            colors = listOf(
                borderHighlight,
                borderHighlight.copy(alpha = borderHighlight.alpha * 0.4f),
                borderShadow
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val specularBrush = remember(specularGloss) {
        Brush.verticalGradient(
            colors = listOf(
                specularGloss,
                specularGloss.copy(alpha = 0f)
            )
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .graphicsLayer {
            this.shape = shape
            this.clip = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx > 0f) {
                renderEffect = RenderEffect
                    .createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        }
        .background(color = baseTint, shape = shape)
        .drawBehind {
            // Specular gloss highlight (top light reflection)
            drawRect(
                brush = specularBrush,
                size = Size(size.width, size.height * 0.45f)
            )

            // Dual rim light stroke around glass geometry
            val strokeWidth = 1.5.dp.toPx()
            when (val outline = shape.createOutline(size, layoutDirection, this)) {
                is Outline.Rectangle -> drawRect(
                    brush = glassBorderBrush,
                    style = Stroke(width = strokeWidth)
                )
                is Outline.Rounded -> drawRoundRect(
                    brush = glassBorderBrush,
                    cornerRadius = CornerRadius(
                        outline.roundRect.topLeftCornerRadius.x,
                        outline.roundRect.topLeftCornerRadius.y
                    ),
                    style = Stroke(width = strokeWidth)
                )
                is Outline.Generic -> drawPath(
                    path = outline.path,
                    brush = glassBorderBrush,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
}

/**
 * Container composable rendering a blurred Liquid Glass background layer underneath un-blurred content.
 */
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDefaults.PillShape,
    blurRadius: Dp = 24.dp,
    elevation: Dp = 10.dp,
    tintColor: Color? = null,
    isDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidGlass(
                    shape = shape,
                    blurRadius = blurRadius,
                    elevation = elevation,
                    tintColor = tintColor,
                    isDarkTheme = isDarkTheme
                )
        )

        Box(
            modifier = Modifier.matchParentSize(),
            content = content
        )
    }
}
