package com.whisk.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    onQuickAdd: () -> Unit
) {
    val destinations = listOf(
        NavDestination.Notes,
        NavDestination.Search,
        NavDestination.Graph,
        NavDestination.AI,
        NavDestination.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
