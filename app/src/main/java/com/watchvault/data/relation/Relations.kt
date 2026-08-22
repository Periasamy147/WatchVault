package com.watchvault.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.watchvault.data.entity.*

data class WatchWithDetails(
    @Embedded val watch: Watch,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val photos: List<WatchPhoto>,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val maintenanceRecords: List<MaintenanceRecord>,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val accuracyRecords: List<AccuracyRecord>,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val wearRecords: List<WearRecord>,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val priceHistory: List<PriceRecord>,

    @Relation(parentColumn = "uuid", entityColumn = "watch_uuid")
    val documents: List<Document>
)

data class WishlistItemWithDetails(
    @Embedded val item: WishlistItem,

    @Relation(parentColumn = "uuid", entityColumn = "wishlist_item_uuid")
    val photos: List<WatchPhoto>,

    @Relation(parentColumn = "uuid", entityColumn = "wishlist_item_uuid")
    val priceHistory: List<PriceRecord>
)
