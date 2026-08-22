package com.watchvault.di

import android.content.Context
import androidx.room.Room
import com.watchvault.data.db.WatchVaultDatabase
import com.watchvault.data.repository.BackupRepository
import com.watchvault.data.repository.MaintenanceRepository
import com.watchvault.data.repository.WatchRepository
import com.watchvault.data.repository.WishlistRepository
import com.watchvault.data.settings.ThemePreferencesRepository
import com.watchvault.data.urlimport.HtmlHeuristicExtractor
import com.watchvault.data.urlimport.JsonLdExtractor
import com.watchvault.data.urlimport.MetaTagExtractor
import com.watchvault.data.urlimport.OkHttpUrlFetcher
import com.watchvault.data.urlimport.OpenGraphExtractor
import com.watchvault.data.urlimport.UrlImportPipeline

/**
 * Hand-rolled dependency container (no Hilt/Dagger — this app is small enough that manual
 * wiring stays readable and avoids an annotation-processor dependency). Lives on the
 * Application instance so every dependency is a true app-scoped singleton.
 */
class AppContainer(context: Context) {

    val database: WatchVaultDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, WatchVaultDatabase::class.java, WatchVaultDatabase.DATABASE_NAME)
            // No fallbackToDestructiveMigration: this app's entire value proposition is
            // "never lose data" (see WatchVaultDatabase.kt doc). Future schema changes must
            // ship real Migration objects registered here.
            .build()
    }

    val watchRepository: WatchRepository by lazy {
        WatchRepository(database.watchDao(), database.watchPhotoDao(), database.maintenanceRecordDao(), database.priceRecordDao())
    }

    val wishlistRepository: WishlistRepository by lazy {
        WishlistRepository(database.wishlistDao(), database.watchPhotoDao())
    }

    val maintenanceRepository: MaintenanceRepository by lazy {
        MaintenanceRepository(database.maintenanceRecordDao())
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            context.applicationContext,
            database,
            database.watchDao(),
            database.wishlistDao(),
            database.watchPhotoDao(),
            database.maintenanceRecordDao(),
            database.priceRecordDao(),
            database.importJobDao()
        )
    }

    val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepository(context.applicationContext)
    }

    val urlImportPipeline: UrlImportPipeline by lazy {
        UrlImportPipeline(
            fetcher = OkHttpUrlFetcher(),
            jsonLdExtractor = JsonLdExtractor(),
            openGraphExtractor = OpenGraphExtractor(),
            metaTagExtractor = MetaTagExtractor(),
            htmlHeuristicExtractor = HtmlHeuristicExtractor()
            // siteAdapters intentionally empty — see README "adding a site adapter later".
        )
    }
}
