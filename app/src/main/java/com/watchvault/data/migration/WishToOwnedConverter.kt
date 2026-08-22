package com.watchvault.data.migration

import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.entity.WishlistItem
import java.util.UUID

/**
 * One-tap "WISH -> OWNED" (spec: My Collection / Wishlist section).
 * Preserves: original wishlist URL, target price, research notes, photos, specifications.
 * Everything else (actual purchase price, purchase date, seller, condition, box/papers) is
 * supplied separately by the user at conversion time — never guessed.
 */
class WishToOwnedConverter {

    data class PurchaseDetails(
        val actualPurchasePrice: Double,
        val purchaseCurrency: String,
        val purchaseDate: Long,
        val seller: String?,
        val conditionRaw: String?,
        val box: Boolean?,
        val papers: Boolean?
    )

    data class ConversionResult(val watch: Watch, val photos: List<WatchPhoto>)

    fun convert(
        item: WishlistItem,
        existingPhotos: List<WatchPhoto>,
        purchase: PurchaseDetails,
        nowMillis: Long
    ): ConversionResult {
        val newUuid = UUID.randomUUID().toString()

        val notesWithProvenance = buildString {
            append("Converted from Wishlist on ${nowMillis}.\n")
            item.productUrl?.let { append("Original wishlist URL: $it\n") }
            item.targetPrice?.let { append("Target price was: ${item.targetPrice} ${item.currency ?: ""}\n") }
            if (!item.notes.isNullOrBlank()) append("\nResearch notes:\n${item.notes}")
        }

        val watch = Watch(
            uuid = newUuid,
            legacyId = null,
            legacyData = item.rawImportData, // preserve the last URL-fetch payload if any
            source = "wishlist_conversion",
            brand = item.brand,
            model = item.model,
            referenceNumber = item.referenceNumber,
            purchaseDate = purchase.purchaseDate,
            purchasePrice = purchase.actualPurchasePrice,
            purchaseCurrency = purchase.purchaseCurrency,
            purchaseCurrencyAssumed = false, // user explicitly provided this at conversion time
            seller = purchase.seller,
            estimatedValue = item.currentPrice,
            estimatedValueCurrency = item.currency,
            estimatedValueSource = "wishlist_conversion",
            conditionRaw = purchase.conditionRaw,
            hasBoxPapersLegacy = null,
            box = purchase.box,
            papers = purchase.papers,
            notes = notesWithProvenance,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )

        val migratedPhotos = existingPhotos.map { it.copy(uuid = UUID.randomUUID().toString(), watchUuid = newUuid, wishlistItemUuid = null) }

        return ConversionResult(watch, migratedPhotos)
    }
}
