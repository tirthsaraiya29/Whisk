package com.whisk.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.whisk.ui.components.glass.LiquidGlassBottomBar

import dev.chrisbanes.haze.HazeState

sealed class NavDestination(val route: String, val label: String, val icon: ImageVector) {
    object Notes : NavDestination("notes", "Notes", Icons.Default.Description)
    object Search : NavDestination("search", "Search", Icons.Default.Search)
    object Graph : NavDestination("graph", "Graph", Icons.Default.Hub)
    object AI : NavDestination("ai", "AI", Icons.Default.AutoAwesome)
    object Settings : NavDestination("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun WhiskBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    LiquidGlassBottomBar(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        onQuickAdd = onQuickAdd,
        modifier = modifier,
        hazeState = hazeState
    )
}
