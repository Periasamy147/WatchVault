package com.watchvault.ui.screens.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.migration.WishToOwnedConverter
import com.watchvault.data.relation.WishlistItemWithDetails
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.EmptyState
import com.watchvault.ui.common.WatchCard
import com.watchvault.ui.common.WatchCardVariant
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(onOpenAddEdit: (String?) -> Unit, onWatchCreated: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WishlistViewModel = viewModel(
        factory = GenericViewModelFactory { WishlistViewModel(container.wishlistRepository, container.watchRepository) }
    )
    val items by viewModel.items.collectAsState()
    var conversionTarget by remember { mutableStateOf<WishlistItem?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wishlist") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenAddEdit(null) }) { Icon(Icons.Filled.Add, contentDescription = "Add wish") }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                headline = "Nothing on your wishlist yet.",
                body = "Add a watch you're chasing and track it toward its target price.",
                primaryActionLabel = "Add Wish",
                onPrimaryAction = { onOpenAddEdit(null) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(items, key = { it.item.uuid }) { details ->
                    WishCard(
                        details = details,
                        onClick = { onOpenAddEdit(details.item.uuid) },
                        onConvert = { conversionTarget = details.item }
                    )
                }
            }
        }
    }

    conversionTarget?.let { item ->
        WishToOwnedDialog(
            item = item,
            onDismiss = { conversionTarget = null },
            onConfirm = { purchase ->
                viewModel.convertToOwned(item, purchase) { watchUuid ->
                    conversionTarget = null
                    onWatchCreated(watchUuid)
                }
            }
        )
    }
}

// Trivial in-memory derivation from already-loaded currentPrice/targetPrice/priority — no new
// fields on WishlistItem. "₹2,085 above target" reads more concretely than a generic "Near
// Target" chip; falls back to the priority tag when there isn't enough price data to compare.
private fun wishStatusLabel(item: WishlistItem): String? {
    val current = item.currentPrice
    val target = item.targetPrice
    if (current != null && target != null) {
        val diff = current - target
        return when {
            diff <= 0 -> "At target"
            else -> "${formatMoney(diff, item.currency)} above target"
        }
    }
    return if (item.priority == "Grail") "Grail" else null
}

@Composable
private fun WishCard(details: WishlistItemWithDetails, onClick: () -> Unit, onConvert: () -> Unit) {
    val item = details.item
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()
    val status = wishStatusLabel(item)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        WatchCard(
            photo = primaryPhoto,
            brand = item.brand,
            model = item.model,
            variant = WatchCardVariant.LIST,
            primaryValueText = "Current ${formatMoney(item.currentPrice, item.currency)}",
            primaryValueColor = vaultColors.gold,
            secondaryText = "Target ${formatMoney(item.targetPrice, item.currency)}",
            statusLabel = status,
            onClick = onClick
        )
        if (item.convertedToWatchUuid == null) {
            TextButton(onClick = onConvert, contentPadding = PaddingValues(start = Spacing.sm)) { Text("Mark as owned") }
        } else {
            Text(
                "Already converted to owned",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishToOwnedDialog(
    item: WishlistItem,
    onDismiss: () -> Unit,
    onConfirm: (WishToOwnedConverter.PurchaseDetails) -> Unit
) {
    var price by remember { mutableStateOf(item.targetPrice?.toString().orEmpty()) }
    var currency by remember { mutableStateOf(item.currency ?: "INR") }
    var seller by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark ${item.brand} ${item.model} as owned") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Actual purchase price *") })
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency") })
                OutlinedTextField(value = seller, onValueChange = { seller = it }, label = { Text("Seller") })
                OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Condition") })
                Text("Box and papers can be confirmed afterwards from the watch's detail page.", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = price.toDoubleOrNull() != null,
                onClick = {
                    onConfirm(
                        WishToOwnedConverter.PurchaseDetails(
                            actualPurchasePrice = price.toDouble(),
                            purchaseCurrency = currency.ifBlank { "INR" },
                            purchaseDate = System.currentTimeMillis(),
                            seller = seller.ifBlank { null },
                            conditionRaw = condition.ifBlank { null },
                            box = null,
                            papers = null
                        )
                    )
                }
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
