package com.watchvault.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.migration.WishToOwnedConverter
import com.watchvault.data.relation.WishlistItemWithDetails
import com.watchvault.data.repository.WatchRepository
import com.watchvault.data.repository.WishlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository,
    private val watchRepository: WatchRepository
) : ViewModel() {

    val items: StateFlow<List<WishlistItemWithDetails>> = wishlistRepository.observeAllWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(item: WishlistItem) = viewModelScope.launch { wishlistRepository.delete(item) }

    fun convertToOwned(
        item: WishlistItem,
        purchase: WishToOwnedConverter.PurchaseDetails,
        onConverted: (String) -> Unit
    ) {
        viewModelScope.launch {
            val photos = wishlistRepository.photosFor(item.uuid)
            val result = WishToOwnedConverter().convert(item, photos, purchase, System.currentTimeMillis())
            watchRepository.upsert(result.watch, isNew = true)
            watchRepository.addPhotos(result.photos)
            wishlistRepository.upsert(item.copy(convertedToWatchUuid = result.watch.uuid), isNew = false)
            onConverted(result.watch.uuid)
        }
    }
}
