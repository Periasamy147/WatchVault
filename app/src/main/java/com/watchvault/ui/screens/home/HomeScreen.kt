package com.watchvault.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.Capsule
import com.watchvault.ui.common.CapsuleVariant
import com.watchvault.ui.common.EmptyState
import com.watchvault.ui.common.IconActionButton
import com.watchvault.ui.common.WatchCard
import com.watchvault.ui.common.WatchCardVariant
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Home is the vault's cover page, not a dashboard: an editorial greeting, one dominant Featured
 * timepiece treated as hero photography, a quiet snapshot line, a horizontal "Recently Acquired"
 * strip, a couple of insights actually backed by data, and a wishlist teaser. Nothing here is a
 * card for the sake of being a card — hierarchy comes from type scale and space, not containers.
 */
@Composable
fun HomeScreen(
    onOpenCollection: () -> Unit,
    onOpenWishlist: () -> Unit,
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
                    .padding(horizontal = Spacing.screenH, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("WATCHVAULT", style = WatchVaultExtraType.metadata, color = vaultColors.gold)
                IconActionButton(Icons.Filled.Settings, contentDescription = "Settings", onClick = onOpenSettings)
            }
        }
    ) { padding ->
        if (stats.totalWatches == 0) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    headline = "Your vault is empty.",
                    body = "Start with the watch that means the most to you.",
                    primaryActionLabel = "Add your first watch",
                    onPrimaryAction = onAddWatch,
                    secondaryActionLabel = "Import existing collection",
                    onSecondaryAction = onOpenImportExport
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            HeroGreeting(stats)

            stats.featured?.let { featured ->
                FeaturedTimepiece(featured, onClick = { onOpenWatch(featured.watch.uuid) })
            }

            CollectionPreview(recent = stats.recentWatches.drop(1), onOpenWatch = onOpenWatch)

            CollectionInsights(stats)

            WishlistPreviewLine(stats, onOpenWishlist = onOpenWishlist)

            QuickAddAction(onAddWatch = onAddWatch)

            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCollection).padding(bottom = Spacing.md),
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
private fun HeroGreeting(stats: HomeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Your Collection", style = WatchVaultExtraType.heroTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column {
                Text("${stats.totalWatches}", style = WatchVaultExtraType.statisticLarge, color = LocalVaultColors.current.gold)
                Text(if (stats.totalWatches == 1) "TIMEPIECE" else "TIMEPIECES", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text(
                    formatMoney(stats.collectionValue.takeIf { stats.totalWatches > 0 }, "INR"),
                    style = WatchVaultExtraType.statisticLarge
                )
                Text("COLLECTION VALUE", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FeaturedTimepiece(details: WatchWithDetails, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("FEATURED", style = WatchVaultExtraType.metadata, color = vaultColors.gold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
        ) {
            WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.05f)
            )
            // A bottom-anchored gradient scrim so the caption sits directly on the photo like an
            // editorial cover, instead of a separate text block below a bordered image.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(watch.brand.uppercase(), style = WatchVaultExtraType.metadata, color = Color.White.copy(alpha = 0.8f))
                Text(watch.model, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    watch.referenceNumber?.let {
                        Text("Ref. $it", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                    }
                }
                Text(
                    formatMoney(watch.estimatedValue ?: watch.purchasePrice, watch.estimatedValueCurrency ?: watch.purchaseCurrency),
                    style = MaterialTheme.typography.titleMedium,
                    color = vaultColors.goldLight
                )
            }
        }
    }
}

@Composable
private fun CollectionPreview(recent: List<WatchWithDetails>, onOpenWatch: (String) -> Unit) {
    if (recent.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("RECENTLY ACQUIRED", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            recent.forEach { details ->
                val watch = details.watch
                val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()
                WatchCard(
                    photo = primaryPhoto,
                    brand = watch.brand,
                    model = watch.model,
                    variant = WatchCardVariant.GRID,
                    primaryValueText = formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
                    modifier = Modifier.width(128.dp),
                    onClick = { onOpenWatch(watch.uuid) }
                )
            }
        }
    }
}

/** Only insights actually backed by [HomeStats] — no "Most Worn"/"Favorite" placeholders, since
 *  there is no wear-tracking or favoriting feature behind them yet. */
@Composable
private fun CollectionInsights(stats: HomeStats) {
    val mostValuable = stats.mostValuable
    if (mostValuable == null && stats.distinctBrandCount == 0) return

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("COLLECTION INSIGHTS", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            mostValuable?.let { watch ->
                InsightValue(
                    icon = Icons.Filled.Diamond,
                    label = "MOST VALUABLE",
                    value = "${watch.brand} ${watch.model}"
                )
            }
            if (stats.distinctBrandCount > 0) {
                InsightValue(
                    icon = null,
                    label = "BRANDS REPRESENTED",
                    value = "${stats.distinctBrandCount}"
                )
            }
        }
    }
}

@Composable
private fun InsightValue(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, value: String) {
    val vaultColors = LocalVaultColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = vaultColors.gold, modifier = Modifier.size(16.dp))
        }
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(label, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WishlistPreviewLine(stats: HomeStats, onOpenWishlist: () -> Unit) {
    if (stats.wishlistCount == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWishlist),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text("WISHLIST", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val nearest = stats.nearestWish
            val subtitle = if (nearest != null) "${nearest.brand} ${nearest.model}" else "${stats.wishlistCount} watched"
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Capsule("${stats.wishlistCount} ${if (stats.wishlistCount == 1) "ITEM" else "ITEMS"}", variant = CapsuleVariant.NEUTRAL)
    }
}

@Composable
private fun QuickAddAction(onAddWatch: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .clickable(onClick = onAddWatch)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = vaultColors.gold, modifier = Modifier.size(18.dp))
        Text("Add a watch to your vault", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
