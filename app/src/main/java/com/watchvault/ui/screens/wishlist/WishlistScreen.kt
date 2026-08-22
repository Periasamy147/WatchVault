package com.watchvault.ui.screens.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
private fun WishCard(details: WishlistItemWithDetails, onClick: () -> Unit, onConvert: () -> Unit) {
    val item = details.item
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${item.brand} ${item.model}", style = MaterialTheme.typography.titleSmall)
            Text("Priority: ${item.priority}", style = MaterialTheme.typography.labelMedium)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Target: ${formatMoney(item.targetPrice, item.currency)}", style = MaterialTheme.typography.bodySmall)
                Text("Current: ${formatMoney(item.currentPrice, item.currency)}", style = MaterialTheme.typography.bodySmall)
            }
            if (item.convertedToWatchUuid == null) {
                TextButton(onClick = onConvert) { Text("Mark as owned") }
            } else {
                Text("Already converted to owned", style = MaterialTheme.typography.labelSmall)
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
