package com.watchvault.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** The 5 primary destinations shown in the bottom navigation bar. Home is deliberately not one
 *  of these — it stays the app's dashboard/landing screen (see HomeScreen "Dashboard v2"), and
 *  is reached above this bar rather than as a persistent tab, matching the specified 5-tab set:
 *  Collection, Wishlist, Discover, Activity, Settings. */
enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    COLLECTION(Routes.COLLECTION, "Collection", Icons.Filled.Watch),
    WISHLIST(Routes.WISHLIST, "Wishlist", Icons.Filled.Favorite),
    DISCOVER(Routes.DISCOVER, "Discover", Icons.Filled.Explore),
    ACTIVITY(Routes.ACTIVITY, "Activity", Icons.Filled.History),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}

private val bottomNavRoutes = BottomNavItem.values().map { it.route }.toSet()

/** Top-level app shell: hosts the nav graph and shows the 5-tab bottom navigation bar only on
 *  the 5 primary destinations (Collection/Wishlist/Discover/Activity/Settings). Detail, add/edit
 *  and import/export screens — plus the Home dashboard — render full-screen without the bar. */
@Composable
fun WatchVaultApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.values().forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            WatchVaultNavGraph(navController = navController)
        }
    }
}
