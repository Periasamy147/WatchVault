package com.watchvault.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watchvault.ui.theme.LocalVaultColors

/** The 6 primary destinations shown in the bottom navigation bar: Home (dashboard) plus
 *  Collection, Wishlist, Discover, Activity, Settings. */
enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home),
    COLLECTION(Routes.COLLECTION, "Collection", Icons.Filled.Watch),
    WISHLIST(Routes.WISHLIST, "Wishlist", Icons.Filled.Favorite),
    DISCOVER(Routes.DISCOVER, "Discover", Icons.Filled.Explore),
    ACTIVITY(Routes.ACTIVITY, "Activity", Icons.Filled.History),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}

private val bottomNavRoutes = BottomNavItem.values().map { it.route }.toSet()

/** Top-level app shell: hosts the nav graph and shows the 6-tab bottom navigation bar on the 6
 *  primary destinations (Home/Collection/Wishlist/Discover/Activity/Settings). Detail, add/edit
 *  and import/export screens render full-screen without the bar. */
@Composable
fun WatchVaultApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VaultBottomBar(currentRoute = currentRoute) { item ->
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
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

/**
 * A bottom nav bar designed to "disappear into the interface" rather than sit on top of it as a
 * stock opaque Material bar: a subtle translucent surface tint, a hairline top border, small
 * icon+label pairs, and a minimal gold-dot selected indicator instead of the stock M3 filled
 * pill/capsule. Deliberately not a [androidx.compose.material3.NavigationBar] — that component
 * always draws the pill indicator behind the selected icon, which is exactly the look this is
 * reacting against.
 */
@Composable
private fun VaultBottomBar(currentRoute: String?, onSelect: (BottomNavItem) -> Unit) {
    val vaultColors = LocalVaultColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(vaultColors.border)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem.values().forEach { item ->
                        val selected = currentRoute == item.route
                        VaultNavTab(
                            item = item,
                            selected = selected,
                            goldColor = vaultColors.gold,
                            onClick = { onSelect(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultNavTab(
    item: BottomNavItem,
    selected: Boolean,
    goldColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val contentColor = if (selected) goldColor else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) goldColor else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}
