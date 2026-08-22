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
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors

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
            Text("Nothing on your wishlist yet.", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
// fields on WishlistItem. Falls back to the existing priority tag when there isn't enough price
// data to say anything about price movement.
private fun wishStatusLabel(item: WishlistItem): String? {
    val current = item.currentPrice
    val target = item.targetPrice
    return when {
        current != null && target != null && current <= target -> "At Target"
        current != null && target != null && current <= target * 1.1 -> "Near Target"
        item.priority == "Grail" -> "Grail"
        else -> null
    }
}

@Composable
private fun WishCard(details: WishlistItemWithDetails, onClick: () -> Unit, onConvert: () -> Unit) {
    val item = details.item
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()
    val status = wishStatusLabel(item)

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.watchvault.ui.common.WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(item.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.model, style = MaterialTheme.typography.titleSmall)
                    }
                    status?.let {
                        androidx.compose.material3.AssistChip(onClick = {}, label = { Text(it, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Target ${formatMoney(item.targetPrice, item.currency)}", style = MaterialTheme.typography.bodySmall)
                    Text("Current ${formatMoney(item.currentPrice, item.currency)}", style = MaterialTheme.typography.bodySmall, color = vaultColors.gold)
                }
                if (item.convertedToWatchUuid == null) {
                    TextButton(onClick = onConvert, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Mark as owned") }
                } else {
                    Text("Already converted to owned", style = MaterialTheme.typography.labelSmall)
                }
            }
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
