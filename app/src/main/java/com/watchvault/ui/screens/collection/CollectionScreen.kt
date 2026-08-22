package com.watchvault.ui.screens.collection

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.watchvault.ui.common.EmptyState
import com.watchvault.ui.common.WatchCard
import com.watchvault.ui.common.WatchCardVariant
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onOpenWatch: (String) -> Unit,
    onAddWatch: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: CollectionViewModel = viewModel(
        factory = GenericViewModelFactory { CollectionViewModel(container.watchRepository) }
    )
    val watches by viewModel.watches.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val allWatches by viewModel.allWatches.collectAsState()
    val vaultColors = LocalVaultColors.current

    // Trivial in-memory aggregation of the already-loaded [allWatches] list — no new DB query.
    val portfolio = remember(allWatches) {
        val currentValue = allWatches.sumOf { it.watch.estimatedValue ?: it.watch.purchasePrice ?: 0.0 }
        val purchaseValue = allWatches.sumOf { it.watch.purchasePrice ?: 0.0 }
        Triple(currentValue, purchaseValue, currentValue - purchaseValue)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Collection") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWatch) { Icon(Icons.Filled.Add, contentDescription = "Add watch") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (allWatches.isNotEmpty()) {
                PortfolioHeader(
                    currentValue = portfolio.first,
                    purchaseValue = portfolio.second,
                    gainLoss = portfolio.third,
                    goldColor = vaultColors.gold,
                    successColor = vaultColors.success,
                    dangerColor = vaultColors.danger
                )
                HorizontalDivider(color = vaultColors.border)
            }
            FilterBar(filters, viewModel, allWatches)
            if (watches.isEmpty()) {
                EmptyState(
                    headline = if (allWatches.isEmpty()) "No watches yet." else "Nothing matches.",
                    body = if (allWatches.isEmpty())
                        "Add one, or import your MyInnos backup from Import/Export."
                    else
                        "Try clearing your search or filters.",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filters.layout == ViewLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchCollectionCard(details, WatchCardVariant.GRID, onClick = { onOpenWatch(details.watch.uuid) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchCollectionCard(details, WatchCardVariant.LIST, onClick = { onOpenWatch(details.watch.uuid) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioHeader(
    currentValue: Double,
    purchaseValue: Double,
    gainLoss: Double,
    goldColor: androidx.compose.ui.graphics.Color,
    successColor: androidx.compose.ui.graphics.Color,
    dangerColor: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
        Text("Total value", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(currentValue, "INR"), style = WatchVaultExtraType.priceLarge, color = goldColor)
        if (purchaseValue > 0.0) {
            val gainColor = if (gainLoss >= 0) successColor else dangerColor
            val sign = if (gainLoss >= 0) "+" else ""
            Text(
                "$sign${formatMoney(gainLoss, "INR")} vs. ${formatMoney(purchaseValue, "INR")} purchase cost",
                style = MaterialTheme.typography.bodySmall,
                color = gainColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(filters: CollectionFilters, viewModel: CollectionViewModel, allWatches: List<WatchWithDetails>) {
    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Search") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                viewModel.setLayout(if (filters.layout == ViewLayout.GRID) ViewLayout.LIST else ViewLayout.GRID)
            }) {
                Icon(
                    if (filters.layout == ViewLayout.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "Toggle layout"
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter and sort")
            }
        }

        androidx.compose.material3.DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text("Sort: last updated") }, onClick = { viewModel.setSort(SortOption.UPDATED_DESC); menuExpanded = false })
            androidx.compose.material3.DropdownMenuItem(text = { Text("Sort: brand A-Z") }, onClick = { viewModel.setSort(SortOption.BRAND_ASC); menuExpanded = false })
            androidx.compose.material3.DropdownMenuItem(text = { Text("Sort: purchase date") }, onClick = { viewModel.setSort(SortOption.PURCHASE_DATE_DESC); menuExpanded = false })
            androidx.compose.material3.DropdownMenuItem(text = { Text("Sort: value") }, onClick = { viewModel.setSort(SortOption.VALUE_DESC); menuExpanded = false })
            androidx.compose.material3.Divider()
            androidx.compose.material3.DropdownMenuItem(text = { Text("Clear brand/movement/condition filters") }, onClick = {
                viewModel.setBrand(null); viewModel.setMovement(null); viewModel.setCondition(null); menuExpanded = false
            })
            allWatches.map { it.watch.brand }.distinct().forEach { brand ->
                androidx.compose.material3.DropdownMenuItem(text = { Text("Brand: $brand") }, onClick = { viewModel.setBrand(brand); menuExpanded = false })
            }
        }

        if (filters.brand != null || filters.movement != null || filters.condition != null) {
            Text(
                "Filtering by: ${listOfNotNull(filters.brand, filters.movement, filters.condition).joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/** Maps a [WatchWithDetails] to the shared [WatchCard] — one place both the grid and list
 *  layouts go through, so the price/gain-loss line reads identically in either view. */
@Composable
private fun WatchCollectionCard(details: WatchWithDetails, variant: WatchCardVariant, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    val gainLossText: String?
    val gainLossColor: androidx.compose.ui.graphics.Color?
    val purchase = watch.purchasePrice
    val current = watch.estimatedValue
    if (purchase != null && current != null) {
        val diff = current - purchase
        val sign = if (diff >= 0) "+" else ""
        gainLossText = "$sign${formatMoney(diff, watch.estimatedValueCurrency ?: watch.purchaseCurrency)}"
        gainLossColor = if (diff >= 0) vaultColors.success else vaultColors.danger
    } else {
        gainLossText = null
        gainLossColor = null
    }

    WatchCard(
        photo = primaryPhoto,
        brand = watch.brand,
        model = watch.model,
        variant = variant,
        primaryValueText = formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
        secondaryText = gainLossText,
        secondaryColor = gainLossColor,
        modifier = if (variant == WatchCardVariant.GRID) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(),
        onClick = onClick
    )
}
