package com.watchvault.data.repository

import com.watchvault.data.dao.WatchPhotoDao
import com.watchvault.data.dao.WishlistDao
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.relation.WishlistItemWithDetails
import kotlinx.coroutines.flow.Flow

class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val watchPhotoDao: WatchPhotoDao
) {
    fun observeAllWithDetails(): Flow<List<WishlistItemWithDetails>> = wishlistDao.observeAllWithDetails()

    suspend fun getByUuid(uuid: String): WishlistItem? = wishlistDao.getByUuid(uuid)

    suspend fun upsert(item: WishlistItem, isNew: Boolean) {
        if (isNew) wishlistDao.insert(item) else wishlistDao.update(item)
    }

    suspend fun delete(item: WishlistItem) = wishlistDao.delete(item)

    suspend fun addPhotos(photos: List<WatchPhoto>) {
        if (photos.isNotEmpty()) watchPhotoDao.insertAll(photos)
    }

    suspend fun photosFor(itemUuid: String): List<WatchPhoto> = watchPhotoDao.forWishlistItem(itemUuid)
}
