package com.watchvault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.watchvault.data.dao.*
import com.watchvault.data.entity.*

/**
 * Version history:
 *  1 - initial schema (Phase 1). No prior versions exist, so no migration objects needed yet.
 *      Future schema changes MUST ship a real Migration (never fallbackToDestructiveMigration
 *      in production builds) since this app's entire value proposition is "never lose data".
 */
@Database(
    entities = [
        Watch::class, WatchPhoto::class, WishlistItem::class,
        MaintenanceRecord::class, AccuracyRecord::class, WearRecord::class,
        PriceRecord::class, Document::class, Tag::class, WatchTagCrossRef::class,
        ImportJob::class, ExportJob::class, Settings::class
    ],
    version = 1,
    exportSchema = true // schema JSON committed under app/schemas/ for future migration diffing
)
abstract class WatchVaultDatabase : RoomDatabase() {
    abstract fun watchDao(): WatchDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun watchPhotoDao(): WatchPhotoDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun priceRecordDao(): PriceRecordDao
    abstract fun importJobDao(): ImportJobDao

    companion object {
        const val DATABASE_NAME = "watch_vault.db"

        // Placeholder for the first real future migration, e.g.:
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE watches ADD COLUMN some_new_field TEXT")
        //     }
        // }
    }
}
