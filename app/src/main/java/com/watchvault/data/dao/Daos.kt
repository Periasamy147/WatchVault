package com.watchvault.data.dao

import androidx.room.*
import com.watchvault.data.entity.*
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.data.relation.WishlistItemWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(watch: Watch)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(watches: List<Watch>)

    @Update
    suspend fun update(watch: Watch)

    @Delete
    suspend fun delete(watch: Watch)

    @Query("SELECT * FROM watches WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): Watch?

    @Query("SELECT * FROM watches WHERE legacy_id = :legacyId LIMIT 1")
    suspend fun findByLegacyId(legacyId: Int): Watch?

    // Duplicate detection per spec section 4: reference number, then brand+model, then UUID.
    @Query("SELECT * FROM watches WHERE reference_number = :referenceNumber AND reference_number IS NOT NULL")
    suspend fun findByReferenceNumber(referenceNumber: String): List<Watch>

    @Query("SELECT * FROM watches WHERE brand = :brand AND model = :model")
    suspend fun findByBrandModel(brand: String, model: String): List<Watch>

    @Transaction
    @Query("SELECT * FROM watches WHERE uuid = :uuid")
    fun observeWithDetails(uuid: String): Flow<WatchWithDetails?>

    @Transaction
    @Query("SELECT * FROM watches ORDER BY updated_at DESC")
    fun observeAllWithDetails(): Flow<List<WatchWithDetails>>

    @Query("""
        SELECT brand || ' ' || model || ' ' || COALESCE(reference_number,'') || ' ' ||
               COALESCE(serial_number,'') || ' ' || COALESCE(notes,'') AS haystack, uuid
        FROM watches
        WHERE haystack LIKE '%' || :query || '%'
    """)
    suspend fun searchRaw(query: String): List<Map<String, String>>
}

@Dao
interface WishlistDao {
    @Insert
    suspend fun insert(item: WishlistItem)

    @Update
    suspend fun update(item: WishlistItem)

    @Delete
    suspend fun delete(item: WishlistItem)

    @Transaction
    @Query("SELECT * FROM wishlist_items ORDER BY date_added DESC")
    fun observeAllWithDetails(): Flow<List<WishlistItemWithDetails>>

    @Query("SELECT * FROM wishlist_items WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): WishlistItem?
}

@Dao
interface WatchPhotoDao {
    @Insert
    suspend fun insertAll(photos: List<WatchPhoto>)

    @Query("SELECT * FROM watch_photos WHERE watch_uuid = :watchUuid ORDER BY sort_order")
    suspend fun forWatch(watchUuid: String): List<WatchPhoto>

    @Query("SELECT * FROM watch_photos WHERE wishlist_item_uuid = :itemUuid ORDER BY sort_order")
    suspend fun forWishlistItem(itemUuid: String): List<WatchPhoto>

    @Delete
    suspend fun delete(photo: WatchPhoto)
}

@Dao
interface MaintenanceRecordDao {
    @Insert
    suspend fun insertAll(records: List<MaintenanceRecord>)

    @Insert
    suspend fun insert(record: MaintenanceRecord)

    @Query("SELECT * FROM maintenance_records WHERE watch_uuid = :watchUuid ORDER BY date DESC")
    suspend fun forWatch(watchUuid: String): List<MaintenanceRecord>
}

@Dao
interface PriceRecordDao {
    @Insert
    suspend fun insertAll(records: List<PriceRecord>)

    @Query("SELECT * FROM price_records WHERE watch_uuid = :watchUuid ORDER BY recorded_at DESC")
    suspend fun forWatch(watchUuid: String): List<PriceRecord>

    @Query("SELECT * FROM price_records WHERE watch_uuid = :watchUuid AND conflict_status = 'conflicting_not_applied'")
    suspend fun unresolvedConflictsForWatch(watchUuid: String): List<PriceRecord>
}

@Dao
interface ImportJobDao {
    @Insert
    suspend fun insert(job: ImportJob)

    @Update
    suspend fun update(job: ImportJob)

    @Query("SELECT * FROM import_jobs ORDER BY started_at DESC")
    fun observeAll(): Flow<List<ImportJob>>
}
