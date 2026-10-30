package com.whisk.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whisk.ui.components.glass.LiquidGlassTopBar
import dev.chrisbanes.haze.HazeState

@Composable
fun WhiskTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    LiquidGlassTopBar(
        title = title,
        subtitle = subtitle,
        onBackClick = onBackClick,
        actions = actions,
        modifier = modifier,
        hazeState = hazeState
    )
}
