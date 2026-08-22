package com.watchvault.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.StatCard
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.common.formatPercent
import com.watchvault.ui.theme.LocalVaultColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCollection: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWatch: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = GenericViewModelFactory { HomeViewModel(container.watchRepository, container.wishlistRepository) }
    )
    val stats by viewModel.stats.collectAsState()
    val vaultColors = LocalVaultColors.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("WatchVault") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PortfolioCard(stats, goldColor = vaultColors.gold, successColor = vaultColors.success, dangerColor = vaultColors.danger)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Watches", stats.totalWatches.toString(), modifier = Modifier.weight(1f))
                StatCard("Brands", stats.distinctBrandCount.toString(), modifier = Modifier.weight(1f))
                StatCard("Avg. value", formatMoney(stats.averageValue.takeIf { stats.totalWatches > 0 }, "INR"), modifier = Modifier.weight(1f))
            }

            CollectionInsightsSection(stats, onOpenWatch = onOpenWatch)

            HorizontalDivider()
            Text("Browse", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onOpenCollection, modifier = Modifier.fillMaxWidth()) {
                Text("My Collection")
            }
            OutlinedButton(onClick = onOpenWishlist, modifier = Modifier.fillMaxWidth()) {
                Text("Wishlist (${stats.wishlistCount})")
            }
            OutlinedButton(onClick = onOpenDiscover, modifier = Modifier.fillMaxWidth()) {
                Text("Discover")
            }
            OutlinedButton(onClick = onOpenActivity, modifier = Modifier.fillMaxWidth()) {
                Text("Activity")
            }
        }
    }
}

@Composable
private fun PortfolioCard(
    stats: HomeStats,
    goldColor: androidx.compose.ui.graphics.Color,
    successColor: androidx.compose.ui.graphics.Color,
    dangerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Collection value", style = MaterialTheme.typography.labelLarge)
            Text(
                formatMoney(stats.collectionValue.takeIf { stats.totalWatches > 0 }, "INR"),
                style = MaterialTheme.typography.headlineMedium,
                color = goldColor
            )
            if (stats.totalWatches > 0 && stats.totalPurchaseValue > 0.0) {
                val gainColor = if (stats.gainLossAmount >= 0) successColor else dangerColor
                val sign = if (stats.gainLossAmount >= 0) "+" else ""
                Text(
                    "$sign${formatMoney(stats.gainLossAmount, "INR")} (${formatPercent(stats.gainLossPercent)}) vs. purchase price",
                    style = MaterialTheme.typography.bodyMedium,
                    color = gainColor
                )
            }
        }
    }
}

@Composable
private fun CollectionInsightsSection(stats: HomeStats, onOpenWatch: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (expanded) "Collection Insights ▲" else "Collection Insights ▼",
                style = MaterialTheme.typography.titleMedium
            )
            if (expanded) {
                if (stats.totalWatches == 0) {
                    Text("Add a watch to see insights here.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Movement breakdown", style = MaterialTheme.typography.labelLarge)
                    stats.movementBreakdown.forEach { (movement, count) ->
                        Text("$movement — $count", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    stats.mostValuable?.let {
                        Text(
                            "Most valuable: ${it.brand} ${it.model} (${formatMoney(it.estimatedValue ?: it.purchasePrice, "INR")})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onOpenWatch(it.uuid) }
                        )
                    }
                    stats.oldestPurchase?.let {
                        Text(
                            "Oldest purchase: ${it.brand} ${it.model} (${formatDate(it.purchaseDate)})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onOpenWatch(it.uuid) }
                        )
                    }
                    stats.newestPurchase?.let {
                        Text(
                            "Newest purchase: ${it.brand} ${it.model} (${formatDate(it.purchaseDate)})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onOpenWatch(it.uuid) }
                        )
                    }
                }
            }
        }
    }
}
