package com.watchvault.ui.screens.home

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Home is a calm cover page for the collection, not a dashboard: wordmark + settings, one line of
 * collection stats, a single large "Featured" watch, a short "Recently Added" preview strip, and
 * one link into the full Collection. No stat tiles, no expandable insights, no card backgrounds —
 * all figures still come straight from [HomeViewModel].
 */
@Composable
fun HomeScreen(
    onOpenCollection: () -> Unit,
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "WATCHVAULT",
                    style = WatchVaultExtraType.metadata,
                    color = vaultColors.gold
                )
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CollectionSummaryLine(stats)

            stats.featured?.let { featured ->
                FeaturedTimepiece(featured, onClick = { onOpenWatch(featured.watch.uuid) })
            }

            CollectionPreview(
                recent = stats.recentWatches.drop(1),
                onOpenWatch = onOpenWatch
            )

            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCollection),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Collection", style = MaterialTheme.typography.labelLarge, color = vaultColors.gold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = vaultColors.gold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CollectionSummaryLine(stats: HomeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "MY COLLECTION",
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${stats.totalWatches} ${if (stats.totalWatches == 1) "TIMEPIECE" else "TIMEPIECES"}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Total value ${formatMoney(stats.collectionValue.takeIf { stats.totalWatches > 0 }, "INR")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturedTimepiece(details: WatchWithDetails, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "FEATURED",
            style = WatchVaultExtraType.metadata,
            color = vaultColors.gold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(watch.brand, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(watch.model, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                watch.referenceNumber?.let {
                    Text("Ref. $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenWatch: (String) -> Unit
) {
    if (recent.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "RECENTLY ADDED",
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text(watch.brand, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(watch.model, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier, onAddWatch: () -> Unit, onImport: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WatchSilhouettePlaceholder(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)))
        Column(
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
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
