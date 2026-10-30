package com.whisk.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WhiskTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    saveStateText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    MinimalTopBar(
        title = title,
        subtitle = subtitle,
        onBackClick = onBackClick,
        onMenuClick = onMenuClick,
        saveStateText = saveStateText,
        actions = actions,
        modifier = modifier
    )
}
