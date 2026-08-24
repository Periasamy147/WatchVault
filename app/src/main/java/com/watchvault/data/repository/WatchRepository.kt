package com.watchvault.data.repository

import com.watchvault.data.dao.MaintenanceRecordDao
import com.watchvault.data.dao.PriceRecordDao
import com.watchvault.data.dao.WatchDao
import com.watchvault.data.dao.WatchPhotoDao
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.PriceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.relation.WatchWithDetails
import kotlinx.coroutines.flow.Flow

/** Owned-collection data access. Everything is suspend/Flow so callers (ViewModels) stay
 *  off the main thread and can observe changes reactively. */
class WatchRepository(
    private val watchDao: WatchDao,
    private val watchPhotoDao: WatchPhotoDao,
    private val maintenanceRecordDao: MaintenanceRecordDao,
    private val priceRecordDao: PriceRecordDao
) {
    fun observeAllWithDetails(): Flow<List<WatchWithDetails>> = watchDao.observeAllWithDetails()

    fun observeWithDetails(uuid: String): Flow<WatchWithDetails?> = watchDao.observeWithDetails(uuid)

    suspend fun getByUuid(uuid: String): Watch? = watchDao.getByUuid(uuid)

    suspend fun upsert(watch: Watch, isNew: Boolean) {
        if (isNew) watchDao.insert(watch) else watchDao.update(watch)
    }

    suspend fun delete(watch: Watch) = watchDao.delete(watch)

    suspend fun addPhotos(photos: List<WatchPhoto>) {
        if (photos.isNotEmpty()) watchPhotoDao.insertAll(photos)
    }

    suspend fun photosForWatch(uuid: String): List<WatchPhoto> = watchPhotoDao.forWatch(uuid)

    suspend fun updatePhotos(photos: List<WatchPhoto>) {
        if (photos.isNotEmpty()) watchPhotoDao.updateAll(photos)
    }

    suspend fun deletePhoto(photo: WatchPhoto) = watchPhotoDao.delete(photo)

    suspend fun addMaintenanceRecord(record: MaintenanceRecord) = maintenanceRecordDao.insert(record)

    suspend fun maintenanceHistory(watchUuid: String): List<MaintenanceRecord> =
        maintenanceRecordDao.forWatch(watchUuid)

    suspend fun priceHistory(watchUuid: String): List<PriceRecord> = priceRecordDao.forWatch(watchUuid)

    suspend fun unresolvedPriceConflicts(watchUuid: String): List<PriceRecord> =
        priceRecordDao.unresolvedConflictsForWatch(watchUuid)

    /** Duplicate check per PHASE1 spec: reference number first, then brand+model. */
    suspend fun findPossibleDuplicates(referenceNumber: String?, brand: String, model: String): List<Watch> {
        val byRef = referenceNumber?.let { watchDao.findByReferenceNumber(it) } ?: emptyList()
        if (byRef.isNotEmpty()) return byRef
        return watchDao.findByBrandModel(brand, model)
    }
}
