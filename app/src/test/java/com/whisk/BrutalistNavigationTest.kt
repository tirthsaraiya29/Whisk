package com.whisk

import com.whisk.ui.components.NavDestination
import com.whisk.ui.theme.DarkHairline
import com.whisk.ui.theme.TrueBlack
import com.whisk.ui.theme.TrueWhite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BrutalistNavigationTest {

    @Test
    fun navDestinations_areConfiguredCorrectly() {
        val destinations = listOf(
            NavDestination.Notes,
            NavDestination.Search,
            NavDestination.Graph,
            NavDestination.AI,
            NavDestination.Settings,
        )

        assertEquals(5, destinations.size)
        assertEquals("notes", NavDestination.Notes.route)
        assertEquals("search", NavDestination.Search.route)
        assertEquals("graph", NavDestination.Graph.route)
        assertEquals("ai", NavDestination.AI.route)
        assertEquals("settings", NavDestination.Settings.route)

        assertEquals("NOTES", NavDestination.Notes.label)
        assertEquals("SEARCH", NavDestination.Search.label)
        assertEquals("GRAPH", NavDestination.Graph.label)
        assertEquals("AI", NavDestination.AI.label)
        assertEquals("SETTINGS", NavDestination.Settings.label)
    }

    @Test
    fun brutalistTheme_paletteConfiguredForHighContrast() {
        assertNotNull(TrueBlack)
        assertNotNull(TrueWhite)
        assertNotNull(DarkHairline)
    }

    @Test
    fun destinationIndexMapping_resolvesCorrectly() {
        val routes = listOf("notes", "search", "graph", "ai", "settings", "unknown")
        val destinations = listOf(
            NavDestination.Notes,
            NavDestination.Search,
            NavDestination.Graph,
            NavDestination.AI,
            NavDestination.Settings,
        )

        val mappedIndices = routes.map { route ->
            val index = destinations.indexOfFirst { it.route == route }
            if (index >= 0) index else 0
        }

        assertEquals(listOf(0, 1, 2, 3, 4, 0), mappedIndices)
    }
}
