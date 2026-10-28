package com.musifly.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.musifly.android.ui.screens.AdvancedDownloadsScreen

/**
 * Navigation extension for Advanced Downloads Screen
 * 
 * Usage:
 * In your NavHost setup, call:
 * advancedDownloadsScreen()
 */

/**
 * Add Advanced Downloads route to NavGraphBuilder
 */
fun NavGraphBuilder.advancedDownloadsScreen() {
    composable(route = AdvancedDownloadsRoute.route) {
        AdvancedDownloadsScreen(
            onNavigateBack = {
                // Handle back navigation
            }
        )
    }
}

/**
 * Navigation routes for Advanced Downloads
 */
object AdvancedDownloadsRoute {
    const val route = "advanced_downloads"
}

/**
 * Extension function to navigate to Advanced Downloads
 */
fun NavController.navigateToAdvancedDownloads() {
    navigate(AdvancedDownloadsRoute.route)
}

/**
 * Example usage in your app:
 * 
 * // In NavHost setup:
 * NavHost(navController, startDestination = "home") {
 *     composable("home") { HomeScreen(navController) }
 *     composable("downloads") { DownloadsScreen(navController) }
 *     
 *     // Add this line:
 *     advancedDownloadsScreen()
 * }
 * 
 * // In DownloadsScreen (to open Advanced Downloads):
 * IconButton(onClick = { navController.navigateToAdvancedDownloads() }) {
 *     Icon(Icons.Default.Settings, "Advanced Downloads")
 * }
 */
