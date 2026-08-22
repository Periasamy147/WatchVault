package com.watchvault.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_photos",
    foreignKeys = [ForeignKey(
        entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WatchPhoto(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "wishlist_item_uuid") val wishlistItemUuid: String? = null,
    @ColumnInfo(name = "local_path") val localPath: String,   // path under app-private storage
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "legacy_uri") val legacyUri: String? = null, // original MyInnos device path, for audit
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** Wishlist item. Deliberately NOT foreign-keyed to Watch — a wish need not reference an owned watch. */
@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey val uuid: String,
    val brand: String,
    val model: String,
    @ColumnInfo(name = "reference_number") val referenceNumber: String? = null,
    @ColumnInfo(name = "product_url") val productUrl: String? = null,
    @ColumnInfo(name = "manufacturer_url") val manufacturerUrl: String? = null,
    @ColumnInfo(name = "store_url") val storeUrl: String? = null,
    @ColumnInfo(name = "current_price") val currentPrice: Double? = null,
    @ColumnInfo(name = "target_price") val targetPrice: Double? = null,
    val currency: String? = null,
    val priority: String = "Medium", // Grail | High | Medium | Low
    val category: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "specifications_json") val specificationsJson: String? = null,
    @ColumnInfo(name = "availability_status") val availabilityStatus: String? = null,
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean = false,
    @ColumnInfo(name = "converted_to_watch_uuid") val convertedToWatchUuid: String? = null, // set on WISH -> OWNED
    @ColumnInfo(name = "raw_import_data") val rawImportData: String? = null, // last URL-fetch payload, verbatim
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [ForeignKey(
        entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MaintenanceRecord(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "legacy_service_id") val legacyServiceId: Int? = null,
    val date: Long,
    val cost: Double? = null,
    val technician: String? = null,
    val description: String? = null,
    val type: String? = null, // battery | service | overhaul | pressure_test | strap | crystal | other
    @ColumnInfo(name = "is_overhaul") val isOverhaul: Boolean = false,
    @ColumnInfo(name = "pressure_tested") val pressureTested: Boolean = false,
    val notes: String? = null
)

@Entity(
    tableName = "accuracy_records",
    foreignKeys = [ForeignKey(
        entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AccuracyRecord(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "seconds_per_day") val secondsPerDay: Double,
    @ColumnInfo(name = "measurement_date") val measurementDate: Long,
    @ColumnInfo(name = "reference_source") val referenceSource: String? = null,
    val notes: String? = null
)

@Entity(
    tableName = "wear_records",
    foreignKeys = [ForeignKey(
        entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WearRecord(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "worn_date") val wornDate: Long,
    val notes: String? = null
)

/** Powers both wishlist price tracking and collection valuation history. Exactly one of
 *  watchUuid / wishlistItemUuid should be set. */
@Entity(tableName = "price_records")
data class PriceRecord(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String? = null,
    @ColumnInfo(name = "wishlist_item_uuid") val wishlistItemUuid: String? = null,
    val price: Double,
    val currency: String,
    val source: String, // "myinnos_export" | "manual" | "url_fetch" | "external_research"
    @ColumnInfo(name = "source_url") val sourceUrl: String? = null,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    @ColumnInfo(name = "is_canonical") val isCanonical: Boolean = true,
    @ColumnInfo(name = "conflict_status") val conflictStatus: String? = null // null | "conflicting_not_applied"
)

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Document(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "maintenance_record_uuid") val maintenanceRecordUuid: String? = null,
    @ColumnInfo(name = "local_path") val localPath: String,
    val label: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val uuid: String,
    val name: String
)

@Entity(
    tableName = "watch_tag_cross_ref",
    primaryKeys = ["watch_uuid", "tag_uuid"],
    foreignKeys = [
        ForeignKey(entity = Watch::class, parentColumns = ["uuid"], childColumns = ["watch_uuid"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["uuid"], childColumns = ["tag_uuid"], onDelete = ForeignKey.CASCADE)
    ]
)
data class WatchTagCrossRef(
    @ColumnInfo(name = "watch_uuid") val watchUuid: String,
    @ColumnInfo(name = "tag_uuid") val tagUuid: String
)

@Entity(tableName = "import_jobs")
data class ImportJob(
    @PrimaryKey val uuid: String,
    val type: String, // "myinnos" | "csv" | "json" | "zip_backup" | "url"
    val status: String, // "pending" | "previewed" | "confirmed" | "completed" | "failed"
    @ColumnInfo(name = "source_description") val sourceDescription: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "report_json") val reportJson: String? = null // counts, skipped, duplicates, errors
)

@Entity(tableName = "export_jobs")
data class ExportJob(
    @PrimaryKey val uuid: String,
    val type: String, // "csv" | "json" | "zip_backup"
    @ColumnInfo(name = "output_path") val outputPath: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val key: String,
    val value: String
)
