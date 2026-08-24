package com.watchvault.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.Capsule
import com.watchvault.ui.common.CapsuleVariant
import com.watchvault.ui.common.EmptyState
import com.watchvault.ui.common.IconActionButton
import com.watchvault.ui.common.TertiaryButton
import com.watchvault.ui.common.PrimaryButton
import com.watchvault.ui.common.WatchCard
import com.watchvault.ui.common.WatchCardVariant
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Collection is where the whole vault is browsed. A quiet portfolio line up top (typography, not
 * a card), a search field that only appears once tapped, a capsule row for the active filters,
 * and grid/list toggle. Filtering and sorting live in one bottom sheet rather than a top-bar
 * dropdown menu, so choosing several filters at once doesn't mean reopening a menu repeatedly.
 */
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

    var searchExpanded by remember { mutableStateOf(false) }
    var filterSheetOpen by remember { mutableStateOf(false) }

    val portfolio = remember(allWatches) {
        val currentValue = allWatches.sumOf { it.watch.estimatedValue ?: it.watch.purchasePrice ?: 0.0 }
        val purchaseValue = allWatches.sumOf { it.watch.purchasePrice ?: 0.0 }
        Triple(currentValue, purchaseValue, currentValue - purchaseValue)
    }

    val activeFilterCount = listOf(filters.brand, filters.movement, filters.condition).count { it != null }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CollectionHeader(
                currentValue = portfolio.first,
                purchaseValue = portfolio.second,
                gainLoss = portfolio.third,
                showPortfolio = allWatches.isNotEmpty(),
                searchExpanded = searchExpanded,
                query = filters.query,
                onQueryChange = viewModel::setQuery,
                onToggleSearch = { searchExpanded = !searchExpanded; if (!searchExpanded) viewModel.setQuery("") },
                layout = filters.layout,
                onToggleLayout = { viewModel.setLayout(if (filters.layout == ViewLayout.GRID) ViewLayout.LIST else ViewLayout.GRID) },
                onOpenFilters = { filterSheetOpen = true },
                activeFilterCount = activeFilterCount,
                onAddWatch = onAddWatch
            )

            if (activeFilterCount > 0) {
                ActiveFilterRow(filters, viewModel)
            }

            if (watches.isEmpty()) {
                EmptyState(
                    headline = if (allWatches.isEmpty()) "Your vault is empty." else "Nothing matches.",
                    body = if (allWatches.isEmpty())
                        "Start with the watch that means the most to you."
                    else
                        "Try clearing your search or filters.",
                    primaryActionLabel = if (allWatches.isEmpty()) "Add your first watch" else null,
                    onPrimaryAction = if (allWatches.isEmpty()) onAddWatch else null,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filters.layout == ViewLayout.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Spacing.screenH, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchCollectionCard(details, WatchCardVariant.GRID, onClick = { onOpenWatch(details.watch.uuid) })
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.screenH, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(watches, key = { it.watch.uuid }) { details ->
                        WatchCollectionCard(details, WatchCardVariant.LIST, onClick = { onOpenWatch(details.watch.uuid) })
                    }
                }
            }
        }
    }

    if (filterSheetOpen) {
        FilterSheet(filters = filters, allWatches = allWatches, viewModel = viewModel, onDismiss = { filterSheetOpen = false })
    }
}

@Composable
private fun CollectionHeader(
    currentValue: Double,
    purchaseValue: Double,
    gainLoss: Double,
    showPortfolio: Boolean,
    searchExpanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    layout: ViewLayout,
    onToggleLayout: () -> Unit,
    onOpenFilters: () -> Unit,
    activeFilterCount: Int,
    onAddWatch: () -> Unit
) {
    val vaultColors = LocalVaultColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenH, vertical = Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Collection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconActionButton(Icons.Filled.Search, contentDescription = "Search", onClick = onToggleSearch)
                Box {
                    IconActionButton(Icons.Filled.FilterList, contentDescription = "Filter and sort", onClick = onOpenFilters)
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(vaultColors.gold)
                        )
                    }
                }
                IconActionButton(
                    icon = if (layout == ViewLayout.GRID) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                    contentDescription = "Toggle layout",
                    onClick = onToggleLayout
                )
                IconActionButton(Icons.Filled.Add, contentDescription = "Add watch", onClick = onAddWatch)
            }
        }

        if (showPortfolio && !searchExpanded) {
            Column(modifier = Modifier.padding(top = Spacing.xs)) {
                Text(formatMoney(currentValue, "INR"), style = WatchVaultExtraType.statisticLarge, color = vaultColors.gold)
                if (purchaseValue > 0.0) {
                    val gainColor = if (gainLoss >= 0) vaultColors.success else vaultColors.danger
                    val sign = if (gainLoss >= 0) "+" else ""
                    Text(
                        "$sign${formatMoney(gainLoss, "INR")} vs. ${formatMoney(purchaseValue, "INR")} cost",
                        style = MaterialTheme.typography.bodySmall,
                        color = gainColor
                    )
                }
            }
        }

        AnimatedVisibility(visible = searchExpanded, enter = fadeIn(), exit = fadeOut()) {
            SearchField(query = query, onQueryChange = onQueryChange, onClear = onToggleSearch)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(vaultColors.gold),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Brand, model, reference, notes…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp).clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun ActiveFilterRow(filters: CollectionFilters, viewModel: CollectionViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenH).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        filters.brand?.let { Capsule(it, variant = CapsuleVariant.ACCENT, onClick = { viewModel.setBrand(null) }) }
        filters.movement?.let { Capsule(it, variant = CapsuleVariant.ACCENT, onClick = { viewModel.setMovement(null) }) }
        filters.condition?.let { Capsule(it, variant = CapsuleVariant.ACCENT, onClick = { viewModel.setCondition(null) }) }
    }
}

@Composable
private fun WatchCollectionCard(details: WatchWithDetails, variant: WatchCardVariant, onClick: () -> Unit) {
    val watch = details.watch
    val primaryPhoto = details.photos.firstOrNull { it.isPrimary } ?: details.photos.firstOrNull()

    WatchCard(
        photo = primaryPhoto,
        brand = watch.brand,
        model = watch.model,
        variant = variant,
        primaryValueText = formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filters: CollectionFilters,
    allWatches: List<WatchWithDetails>,
    viewModel: CollectionViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val brands = remember(allWatches) { allWatches.map { it.watch.brand }.distinct().sorted() }
    val movements = remember(allWatches) {
        allWatches.mapNotNull { it.watch.movementNormalized ?: it.watch.movementRaw }.distinct().sorted()
    }
    val conditions = remember(allWatches) { allWatches.mapNotNull { it.watch.conditionRaw }.distinct().sorted() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("Filter & Sort", style = MaterialTheme.typography.titleLarge)

            SortRow(filters.sort, viewModel::setSort)

            if (brands.isNotEmpty()) {
                FilterGroup("BRAND", brands, filters.brand) { viewModel.setBrand(if (filters.brand == it) null else it) }
            }
            if (movements.isNotEmpty()) {
                FilterGroup("MOVEMENT", movements, filters.movement) { viewModel.setMovement(if (filters.movement == it) null else it) }
            }
            if (conditions.isNotEmpty()) {
                FilterGroup("CONDITION", conditions, filters.condition) { viewModel.setCondition(if (filters.condition == it) null else it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                TertiaryButton(
                    text = "Clear all",
                    onClick = { viewModel.setBrand(null); viewModel.setMovement(null); viewModel.setCondition(null) },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(text = "Apply", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SortRow(current: SortOption, onSort: (SortOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("SORT", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            val options = listOf(
                SortOption.UPDATED_DESC to "Recently updated",
                SortOption.BRAND_ASC to "Brand A–Z",
                SortOption.PURCHASE_DATE_DESC to "Purchase date",
                SortOption.VALUE_DESC to "Value"
            )
            options.forEach { (option, label) ->
                Capsule(
                    label,
                    variant = if (current == option) CapsuleVariant.SELECTED else CapsuleVariant.OUTLINED,
                    onClick = { onSort(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(title: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            options.forEach { option ->
                Capsule(
                    option,
                    variant = if (selected == option) CapsuleVariant.SELECTED else CapsuleVariant.OUTLINED,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}
