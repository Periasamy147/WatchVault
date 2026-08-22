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
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
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
                Text(
                    "No watches yet. Add one, or import your MyInnos backup from Import/Export.",
                    modifier = Modifier.padding(24.dp)
                )
            } else if (filters.layout == ViewLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchGridCard(details, onClick = { onOpenWatch(details.watch.uuid) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchListRow(details, onClick = { onOpenWatch(details.watch.uuid) })
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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Total value", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(currentValue, "INR"), style = MaterialTheme.typography.headlineSmall, color = goldColor)
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

@Composable
private fun WatchGridCard(details: WatchWithDetails, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, vaultColors.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        WatchPhotoOrPlaceholder(
            photo = primaryPhoto,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(watch.brand, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(watch.model, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(
                formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
                style = MaterialTheme.typography.labelMedium,
                color = vaultColors.gold
            )
        }
    }
}

@Composable
private fun WatchListRow(details: WatchWithDetails, onClick: () -> Unit) {
    val watch = details.watch
    val vaultColors = LocalVaultColors.current
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, vaultColors.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WatchPhotoOrPlaceholder(
            photo = primaryPhoto,
            modifier = Modifier.fillMaxWidth(0.28f).aspectRatio(1f)
        )
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${watch.brand} ${watch.model}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(watch.referenceNumber ?: "No reference number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
                style = MaterialTheme.typography.labelMedium,
                color = vaultColors.gold
            )
        }
    }
}
