package com.whisk.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whisk.ui.components.glass.LiquidGlassTopBar

@Composable
fun WhiskTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    LiquidGlassTopBar(
        title = title,
        subtitle = subtitle,
        onBackClick = onBackClick,
        actions = actions,
        modifier = modifier
    )
}
