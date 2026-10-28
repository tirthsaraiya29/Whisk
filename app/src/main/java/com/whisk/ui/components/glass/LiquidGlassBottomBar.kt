package com.whisk.ui.components.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.ui.components.NavDestination

/**
 * Native iOS-Inspired Liquid Glass Floating Bottom Navigation Capsule with Quick-Add Lens.
 * Features fluid animated selected lens indicator, optical depth, backdrop blur,
 * keyboard awareness, safe insets, and high-contrast theme adaptivity.
 */
@Composable
fun LiquidGlassBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val destinations = remember {
        listOf(
            NavDestination.Notes,
            NavDestination.Search,
            NavDestination.Graph,
            NavDestination.AI,
            NavDestination.Settings
        )
    }

    val selectedIndex = remember(currentRoute) {
        val index = destinations.indexOfFirst { it.route == currentRoute }
        if (index >= 0) index else 0
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Keyboard visibility detection via WindowInsets
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    AnimatedVisibility(
        visible = !isKeyboardVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Floating Liquid Glass Capsule Container
                LiquidGlassBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = CircleShape,
                    blurRadius = 24.dp,
                    elevation = 12.dp,
                    isDarkTheme = isDarkTheme
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        val totalWidth = maxWidth
                        val tabCount = destinations.size
                        val tabWidth = totalWidth / tabCount

                        // Fluid Animated Selected Lens Indicator Position
                        val animatedIndexFraction by animateFloatAsState(
                            targetValue = selectedIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "SelectedLensAnimation"
                        )

                        val lensOffsetPx = with(LocalDensity.current) {
                            (animatedIndexFraction * tabWidth.toPx()).toInt()
                        }

                        // Sliding Inner Glass Lens Pill
                        val lensTint = if (isDarkTheme) LiquidGlassDefaults.DarkLensTint else LiquidGlassDefaults.LightLensTint
                        LiquidGlassBox(
                            modifier = Modifier
                                .offset { IntOffset(x = lensOffsetPx, y = 0) }
                                .width(tabWidth)
                                .fillMaxHeight(),
                            shape = CircleShape,
                            blurRadius = 12.dp,
                            elevation = 2.dp,
                            tintColor = lensTint,
                            isDarkTheme = isDarkTheme
                        ) {}

                        // Crisp Foreground Navigation Items Row
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            destinations.forEachIndexed { index, dest ->
                                val isSelected = index == selectedIndex
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                    label = "IconScale"
                                )

                                val activeColor = if (isDarkTheme) LiquidGlassDefaults.DarkActiveAccent else LiquidGlassDefaults.LightActiveContent
                                val inactiveColor = if (isDarkTheme) LiquidGlassDefaults.DarkInactiveContent else LiquidGlassDefaults.LightInactiveContent

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .semantics {
                                            role = Role.Tab
                                            this.selected = isSelected
                                            contentDescription = dest.label
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onNavigate(dest.route) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.scale(scale)
                                    ) {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) activeColor else inactiveColor,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Text(
                                            text = dest.label,
                                            style = TextStyle(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 9.5.sp,
                                                color = if (isSelected) activeColor else inactiveColor
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Add Floating Glass Action Jewel
                val addLensTint = if (isDarkTheme) Color(0xCC0284C7) else Color(0xCC2563EB)
                LiquidGlassBox(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onQuickAdd)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Quick Add Note"
                        },
                    shape = CircleShape,
                    blurRadius = 24.dp,
                    elevation = 12.dp,
                    tintColor = addLensTint,
                    isDarkTheme = isDarkTheme
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
