package com.whisk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.ui.theme.StatusAmber
import com.whisk.ui.theme.StatusGreen
import com.whisk.ui.theme.VercelMonoFontFamily

/**
 * Premium Modern Top Bar.
 * Renders subtle surface elevation, proportional typography, clean save status indicator,
 * and high-contrast accessible action triggers.
 */
@Composable
fun MinimalTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    onCommandPaletteClick: (() -> Unit)? = null,
    saveStateText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Menu / Back Trigger & Breadcrumb Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackClick()
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else if (onMenuClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMenuClick()
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Drawer",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = if (onBackClick != null || onMenuClick != null) 4.dp else 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!saveStateText.isNullOrBlank()) {
                            val isSaved = saveStateText.contains("Saved", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSaved) StatusGreen else StatusAmber),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Contextual Actions Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (onCommandPaletteClick != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCommandPaletteClick()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "⌘K",
                            fontFamily = VercelMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                actions()
            }
        }
    }
}
