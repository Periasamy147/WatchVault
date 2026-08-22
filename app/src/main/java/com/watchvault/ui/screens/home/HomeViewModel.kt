package com.watchvault.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.repository.WatchRepository
import com.watchvault.data.repository.WishlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeStats(
    val totalWatches: Int = 0,
    val wishlistCount: Int = 0,
    val collectionValue: Double = 0.0
)

class HomeViewModel(
    watchRepository: WatchRepository,
    wishlistRepository: WishlistRepository
) : ViewModel() {

    val stats: StateFlow<HomeStats> = combine(
        watchRepository.observeAllWithDetails(),
        wishlistRepository.observeAllWithDetails()
    ) { watches, wishlist ->
        HomeStats(
            totalWatches = watches.size,
            wishlistCount = wishlist.size,
            collectionValue = watches.sumOf { it.watch.estimatedValue ?: it.watch.purchasePrice ?: 0.0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())
}
