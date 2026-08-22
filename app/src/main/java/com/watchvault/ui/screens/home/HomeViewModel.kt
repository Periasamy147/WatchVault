package com.watchvault.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.Watch
import com.watchvault.data.repository.WatchRepository
import com.watchvault.data.repository.WishlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeStats(
    val totalWatches: Int = 0,
    val wishlistCount: Int = 0,
    val collectionValue: Double = 0.0,
    val totalPurchaseValue: Double = 0.0,
    val gainLossAmount: Double = 0.0,
    val gainLossPercent: Double? = null,
    val distinctBrandCount: Int = 0,
    val averageValue: Double = 0.0,
    val movementBreakdown: Map<String, Int> = emptyMap(),
    val mostValuable: Watch? = null,
    val oldestPurchase: Watch? = null,
    val newestPurchase: Watch? = null
)

/**
 * Dashboard v2. Every figure here is computed from fields that already exist on [Watch] —
 * purchasePrice / estimatedValue / brand / movementRaw / movementNormalized / purchaseDate —
 * nothing new was added to the schema for this. "Collection value" uses each watch's
 * estimatedValue where known, falling back to purchasePrice, same as the original dashboard.
 */
class HomeViewModel(
    watchRepository: WatchRepository,
    wishlistRepository: WishlistRepository
) : ViewModel() {

    val stats: StateFlow<HomeStats> = combine(
        watchRepository.observeAllWithDetails(),
        wishlistRepository.observeAllWithDetails()
    ) { watchDetails, wishlist ->
        val watches = watchDetails.map { it.watch }
        val currentValue = watches.sumOf { it.estimatedValue ?: it.purchasePrice ?: 0.0 }
        val purchaseValue = watches.sumOf { it.purchasePrice ?: 0.0 }
        val gainLoss = currentValue - purchaseValue
        val gainLossPercent = if (purchaseValue > 0.0) (gainLoss / purchaseValue) * 100.0 else null
        val movementBreakdown = watches
            .groupBy { it.movementNormalized ?: it.movementRaw ?: "Unknown" }
            .mapValues { (_, group) -> group.size }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        HomeStats(
            totalWatches = watches.size,
            wishlistCount = wishlist.size,
            collectionValue = currentValue,
            totalPurchaseValue = purchaseValue,
            gainLossAmount = gainLoss,
            gainLossPercent = gainLossPercent,
            distinctBrandCount = watches.map { it.brand }.distinct().size,
            averageValue = if (watches.isNotEmpty()) currentValue / watches.size else 0.0,
            movementBreakdown = movementBreakdown,
            mostValuable = watches.maxByOrNull { it.estimatedValue ?: it.purchasePrice ?: 0.0 },
            oldestPurchase = watches.filter { it.purchaseDate != null }.minByOrNull { it.purchaseDate!! },
            newestPurchase = watches.filter { it.purchaseDate != null }.maxByOrNull { it.purchaseDate!! }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())
}
