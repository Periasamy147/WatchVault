package com.watchvault.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.WatchSilhouettePlaceholder
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.common.formatPercent
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Home / dashboard v3: a calm, editorial "collection cover page" rather than a stack of Material
 * stat cards. Every figure still comes straight from [HomeViewModel] — only the presentation
 * changed. Layout, top to bottom: wordmark header, portfolio value in type (no card), a large
 * "Featured Timepiece" hero for the most recently added watch, a photo-forward preview strip of
 * the rest of the collection, and the collapsible Collection Insights section (unchanged data,
 * de-emphasized so it doesn't compete with the hero). An explicit empty state replaces all of the
 * above when there are zero watches yet.
 */
@Composable
fun HomeScreen(
    onOpenCollection: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImportExport: () -> Unit,
    onAddWatch: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "WATCHVAULT",
                        style = WatchVaultExtraType.metadata,
                        color = vaultColors.gold
                    )
                    Text(
                        "My Collection",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { padding ->
        if (stats.totalWatches == 0) {
            HomeEmptyState(
                modifier = Modifier.padding(padding),
                onAddWatch = onAddWatch,
                onImport = onOpenImportExport
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            PortfolioSummary(stats, vaultColors.gold, vaultColors.success, vaultColors.danger)

            stats.featured?.let { featured ->
                FeaturedTimepiece(featured, onClick = { onOpenWatch(featured.watch.uuid) })
            }

            CollectionPreview(
                recent = stats.recentWatches.drop(1),
                onOpenWatch = onOpenWatch,
                onSeeAll = onOpenCollection
            )

            CollectionInsightsSection(stats, onOpenWatch = onOpenWatch)

            HorizontalDivider(color = vaultColors.border)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Browse", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun PortfolioSummary(
    stats: HomeStats,
    goldColor: androidx.compose.ui.graphics.Color,
    successColor: androidx.compose.ui.graphics.Color,
    dangerColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Collection value",
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatMoney(stats.collectionValue.takeIf { stats.totalWatches > 0 }, "INR"),
            style = MaterialTheme.typography.displaySmall,
            color = goldColor
        )
        if (stats.totalPurchaseValue > 0.0) {
            val gainColor = if (stats.gainLossAmount >= 0) successColor else dangerColor
            val sign = if (stats.gainLossAmount >= 0) "+" else ""
            Text(
                "$sign${formatMoney(stats.gainLossAmount, "INR")} (${formatPercent(stats.gainLossPercent)}) vs. purchase price",
                style = MaterialTheme.typography.bodyMedium,
                color = gainColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 10.dp)) {
            com.watchvault.ui.common.StatCard("Watches", stats.totalWatches.toString())
            com.watchvault.ui.common.StatCard("Brands", stats.distinctBrandCount.toString())
            com.watchvault.ui.common.StatCard("Avg. value", formatMoney(stats.averageValue.takeIf { stats.totalWatches > 0 }, "INR"))
        }
    }
}

@Composable
private fun FeaturedTimepiece(details: WatchWithDetails, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "FEATURED TIMEPIECE",
            style = WatchVaultExtraType.metadata,
            color = vaultColors.gold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, vaultColors.border, RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        ) {
            WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)
            )
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(watch.brand, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(watch.model, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    watch.referenceNumber?.let {
                        Text("Ref. $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    formatMoney(watch.estimatedValue ?: watch.purchasePrice, watch.estimatedValueCurrency ?: watch.purchaseCurrency),
                    style = MaterialTheme.typography.titleMedium,
                    color = vaultColors.gold
                )
            }
        }
    }
}

@Composable
private fun CollectionPreview(
    recent: List<WatchWithDetails>,
    onOpenWatch: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    if (recent.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Collection", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            recent.forEach { details ->
                val watch = details.watch
                val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onOpenWatch(watch.uuid) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WatchPhotoOrPlaceholder(
                        photo = primaryPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(watch.brand, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(watch.model, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CollectionInsightsSection(stats: HomeStats, onOpenWatch: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val vaultColors = LocalVaultColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(180))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Collection Insights", style = MaterialTheme.typography.titleMedium)
            Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = vaultColors.gold)
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                Text("Movement breakdown", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                stats.movementBreakdown.forEach { (movement, count) ->
                    Text("$movement — $count", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = vaultColors.border)
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

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier, onAddWatch: () -> Unit, onImport: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WatchSilhouettePlaceholder(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)))
        Column(
            modifier = Modifier.padding(top = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Your collection is waiting.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Add your first timepiece to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onAddWatch, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Add Watch", modifier = Modifier.padding(start = 4.dp))
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text("Import existing collection")
        }
    }
}
