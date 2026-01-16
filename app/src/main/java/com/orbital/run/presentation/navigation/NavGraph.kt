package com.orbital.run.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.orbital.run.presentation.screens.analytics.AnalyticsScreen
import com.orbital.run.presentation.screens.history.HistoryScreen
import com.orbital.run.presentation.screens.home.HomeScreen
import com.orbital.run.presentation.screens.settings.SettingsScreen

/**
 * Main navigation graph for DrawRun.
 *
 * Defines all app destinations and navigation structure.
 * Uses type-safe navigation with sealed class routes.
 *
 * @param navController Navigation controller
 * @param innerPadding Padding from scaffold (for bottom nav, top bar, etc.)
 */
@Composable
fun DrawRunNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        
        // ========================
        // HOME SCREEN (Dashboard)
        // ========================
        
        composable(route = Screen.Home.route) {
            HomeScreen(
                onActivityClick = { activityId ->
                    navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                },
                onNavigationClick = {
                    // TODO: Open drawer or menu
                }
            )
        }
        
        // ========================
        // ANALYTICS SCREEN
        // ========================
        
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen()
        }
        
        // ========================
        // HISTORY SCREEN (Activities List)
        // ========================
        
        composable(route = Screen.History.route) {
            HistoryScreen(
                onActivityClick = { activityId ->
                    navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                }
            )
        }
        
        // ========================
        // SETTINGS SCREEN
        // ========================
        
        composable(route = Screen.Settings.route) {
            SettingsScreen()
        }
        
        // ========================
        // ACTIVITY DETAIL SCREEN  
        // ========================
        
        composable(
            route = Screen.ActivityDetail.route,
            arguments = Screen.ActivityDetail.arguments
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            
            // TODO: Create ActivityDetailScreen
            // ActivityDetailScreen(
            //     activityId = activityId ?: "",
            //     onBackClick = { navController.popBackStack() }
            // )
            androidx.compose.material3.Text("Activity Detail: $activityId - TODO")
        }
    }
}

/**
 * Type-safe navigation routes.
 *
 * Each screen is represented as a sealed class for compile-time safety.
 */
sealed class Screen(val route: String) {
    
    // ========================
    // BOTTOM NAV SCREENS
    // ========================
    
    /** Home/Dashboard screen (default start destination) */
    object Home : Screen("home")
    
    /** Analytics & charts screen */
    object Analytics : Screen("analytics")
    
    /** Activity history/list screen */
    object History : Screen("history")
    
    /** Settings screen */
    object Settings : Screen("settings")
    
    // ========================
    // DETAIL SCREENS
    // ========================
    
    /** Activity detail screen (requires activityId) */
    object ActivityDetail : Screen("activity/{activityId}") {
        fun createRoute(activityId: String) = "activity/$activityId"
        val arguments = listOf(
            androidx.navigation.navArgument("activityId") {
                type = androidx.navigation.NavType.StringType
            }
        )
    }
}
