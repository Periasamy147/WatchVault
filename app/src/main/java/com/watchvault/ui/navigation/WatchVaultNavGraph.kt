package com.watchvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.watchvault.ui.screens.activity.ActivityScreen
import com.watchvault.ui.screens.addedit.AddEditWatchScreen
import com.watchvault.ui.screens.collection.CollectionScreen
import com.watchvault.ui.screens.home.HomeScreen
import com.watchvault.ui.screens.importexport.ImportExportScreen
import com.watchvault.ui.screens.settings.SettingsScreen
import com.watchvault.ui.screens.watchdetail.WatchDetailScreen
import com.watchvault.ui.screens.wishaddedit.WishAddEditScreen
import com.watchvault.ui.screens.wishlist.WishlistScreen

@Composable
fun WatchVaultNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCollection = { navController.navigate(Routes.COLLECTION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenImportExport = { navController.navigate(Routes.IMPORT_EXPORT) },
                onAddWatch = { navController.navigate(Routes.addEditWatch()) },
                onOpenWatch = { uuid -> navController.navigate(Routes.watchDetail(uuid)) }
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onOpenWatch = { uuid -> navController.navigate(Routes.watchDetail(uuid)) },
                onAddWatch = { navController.navigate(Routes.addEditWatch()) }
            )
        }
        composable(Routes.WISHLIST) {
            WishlistScreen(
                onOpenAddEdit = { uuid -> navController.navigate(Routes.addEditWish(uuid)) },
                onWatchCreated = { uuid -> navController.navigate(Routes.watchDetail(uuid)) }
            )
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(onOpenWatch = { uuid -> navController.navigate(Routes.watchDetail(uuid)) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenImportExport = { navController.navigate(Routes.IMPORT_EXPORT) })
        }
        composable(Routes.IMPORT_EXPORT) { ImportExportScreen() }

        composable(
            route = Routes.WATCH_DETAIL,
            arguments = listOf(navArgument("watchUuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("watchUuid").orEmpty()
            WatchDetailScreen(
                watchUuid = uuid,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.addEditWatch(it)) }
            )
        }

        composable(
            route = Routes.ADD_EDIT_WATCH,
            arguments = listOf(navArgument("watchUuid") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("watchUuid")
            AddEditWatchScreen(
                watchUuid = uuid,
                onBack = { navController.popBackStack() },
                onSaved = { savedUuid ->
                    navController.popBackStack()
                    navController.navigate(Routes.watchDetail(savedUuid))
                }
            )
        }

        composable(
            route = Routes.ADD_EDIT_WISH,
            arguments = listOf(navArgument("wishUuid") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("wishUuid")
            WishAddEditScreen(
                wishUuid = uuid,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
