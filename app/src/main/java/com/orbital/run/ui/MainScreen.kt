package com.orbital.run.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orbital.run.presentation.navigation.DrawRunNavGraph
import com.orbital.run.presentation.navigation.Screen
import com.orbital.run.ui.theme.DrawRunTheme

/**
 * Main application screen with bottom navigation.
 *
 * Clean architecture with:
 * - DrawRunTheme for Material3 theming
 * - NavigationBar for bottom nav
 * - NavHost for screen routing
 *
 * Replaces legacy 2000-line MainScreen implementation.
 */
@Composable
fun MainScreen() {
    DrawRunTheme {
        val navController = rememberNavController()
        
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(text = item.label)
                            },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Pop up to start destination to avoid stack buildup
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            DrawRunNavGraph(
                navController = navController,
                innerPadding = innerPadding
            )
        }
    }
}

/**
 * Bottom navigation items configuration.
 */
private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        icon = Icons.Default.Home,
        label = "Accueil"
    ),
    BottomNavItem(
        screen = Screen.Analytics,
        icon = Icons.Default.BarChart,
        label = "Analyses"
    ),
    BottomNavItem(
        screen = Screen.History,
        icon = Icons.Default.History,
        label = "Historique"
    ),
    BottomNavItem(
        screen = Screen.Settings,
        icon = Icons.Default.Settings,
        label = "Réglages"
    )
)
