package com.watchvault.ui.screens.watchdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors

/**
 * Watch detail, presented as a horizontal tab row rather than a long expandable-section scroll.
 * Tab order — Photo, Identity, Value, Ownership, Specifications, Maintenance, Notes — mirrors
 * the section order on the Add/Edit form. All fields the screen previously showed are still
 * shown; only the presentation (tabs vs. stacked expandable cards) changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchDetailScreen(watchUuid: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WatchDetailViewModel = viewModel(
        factory = GenericViewModelFactory { WatchDetailViewModel(container.watchRepository, watchUuid) }
    )
    val details by viewModel.watch.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.watch?.let { "${it.brand} ${it.model}" } ?: "Watch") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { onEdit(watchUuid) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { padding ->
        val watch = details?.watch
        if (watch == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val tabTitles = listOf("Photo", "Identity", "Value", "Ownership", "Specifications", "Maintenance", "Notes")
        var selectedTab by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> PhotoContent(details?.photos ?: emptyList())
                    1 -> OverviewContent(watch)
                    2 -> ValuationContent(watch)
                    3 -> PurchaseContent(watch)
                    4 -> SpecificationsContent(watch)
                    5 -> MaintenanceContent(details?.maintenanceRecords ?: emptyList())
                    6 -> Text(watch.notes ?: "No notes.")
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PhotoContent(photos: List<WatchPhoto>) {
    if (photos.isEmpty()) {
        Text("No photos on file yet.")
    } else {
        Text("${photos.size} photo(s) on file.")
    }
}

@Composable
private fun OverviewContent(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledValue("Brand / Model", "${watch.brand} ${watch.model}")
        LabeledValue("Reference number", watch.referenceNumber ?: "—")
        LabeledValue("Nickname", watch.nickname ?: "—")
        LabeledValue("Ownership status", watch.ownershipStatus)
        LabeledValue("Source", watch.source)
    }
}

@Composable
private fun SpecificationsContent(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledValue("Movement", watch.movementNormalized ?: watch.movementRaw ?: "—")
        LabeledValue("Case", listOfNotNull(watch.caseMaterial, watch.caseColour, watch.caseShape).joinToString(", ").ifBlank { "—" })
        LabeledValue("Case diameter", watch.caseDiameterMm?.let { "$it mm" } ?: "—")
        LabeledValue("Dial", listOfNotNull(watch.dialColour, watch.dialType).joinToString(", ").ifBlank { "—" })
        LabeledValue("Strap", listOfNotNull(watch.strap, watch.strapMaterial, watch.strapColour).joinToString(", ").ifBlank { "—" })
        LabeledValue("Water resistance", watch.waterResistance ?: "—")
        LabeledValue("Crystal", watch.crystal ?: "—")
        LabeledValue("Condition (as recorded)", watch.conditionRaw ?: "—")
        val boxPapers = when {
            watch.box == true && watch.papers == true -> "Box + papers"
            watch.box == false && watch.papers == false -> "Neither"
            watch.box != null || watch.papers != null -> "Box: ${watch.box?.toString() ?: "unknown"}, Papers: ${watch.papers?.toString() ?: "unknown"}"
            watch.hasBoxPapersLegacy != null -> "Unknown (legacy record only says: ${watch.hasBoxPapersLegacy})"
            else -> "Unknown"
        }
        LabeledValue("Box / Papers", boxPapers)
    }
}

@Composable
private fun PurchaseContent(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledValue("Purchase date", formatDate(watch.purchaseDate))
        LabeledValue("Purchase price", formatMoney(watch.purchasePrice, watch.purchaseCurrency, watch.purchaseCurrencyAssumed))
        LabeledValue("Seller", watch.seller ?: "—")
        LabeledValue("Location", watch.purchaseLocation ?: "—")
        LabeledValue("Invoice number", watch.invoiceNumber ?: "—")
        LabeledValue("First owner", watch.isFirstOwner?.toString() ?: "Unknown")
    }
}

@Composable
private fun ValuationContent(watch: Watch) {
    val vaultColors = LocalVaultColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Text("Estimated value", style = MaterialTheme.typography.labelSmall)
            Text(
                formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
                style = MaterialTheme.typography.headlineSmall,
                color = vaultColors.gold
            )
        }
        LabeledValue("Value source", watch.estimatedValueSource ?: "—")
    }
}

@Composable
private fun MaintenanceContent(records: List<MaintenanceRecord>) {
    if (records.isEmpty()) {
        Text("No maintenance records yet.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        records.forEach { record ->
            Column {
                Text("${record.type ?: "Service"} — ${formatDate(record.date)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(
                        record.technician,
                        record.cost?.let { formatMoney(it, "INR") },
                        if (record.isOverhaul) "Overhaul" else null,
                        if (record.pressureTested) "Pressure tested" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
