package com.watchvault.ui.screens.watchdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Watch detail as one continuous editorial scroll, not colored/filled section cards or tabs:
 * a large photo, brand -> model -> reference in plain typography, a value block, then
 * label/value rows grouped under a small uppercase heading + a 1dp divider (Specs, Ownership,
 * Service, Notes — Notes only rendered when present). All fields shown are the same ones the
 * previous tabbed layout showed; only the presentation changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchDetailScreen(watchUuid: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WatchDetailViewModel = viewModel(
        factory = GenericViewModelFactory { WatchDetailViewModel(container.watchRepository, watchUuid) }
    )
    val details by viewModel.watch.collectAsState()
    val vaultColors = LocalVaultColors.current

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
        val photos = details?.photos ?: emptyList()
        val primaryPhoto = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(12.dp))
            )

            IdentityBlock(watch)

            ValueBlock(watch, vaultColors.gold, vaultColors.success, vaultColors.danger)

            DetailSection(title = "SPECS") { SpecificationsContent(watch) }
            DetailSection(title = "OWNERSHIP") { OwnershipContent(watch) }
            DetailSection(title = "SERVICE") { MaintenanceContent(details?.maintenanceRecords ?: emptyList()) }
            if (!watch.notes.isNullOrBlank()) {
                DetailSection(title = "NOTES") { Text(watch.notes, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun IdentityBlock(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(watch.brand.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(watch.model, style = MaterialTheme.typography.headlineMedium)
        watch.referenceNumber?.let {
            Text("Ref. $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ValueBlock(
    watch: Watch,
    goldColor: androidx.compose.ui.graphics.Color,
    successColor: androidx.compose.ui.graphics.Color,
    dangerColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            formatMoney(watch.estimatedValue, watch.estimatedValueCurrency),
            style = MaterialTheme.typography.headlineSmall,
            color = goldColor
        )
        val purchase = watch.purchasePrice
        val current = watch.estimatedValue
        Text(
            "Purchased ${formatMoney(purchase, watch.purchaseCurrency, watch.purchaseCurrencyAssumed)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (purchase != null && current != null) {
            val diff = current - purchase
            val sign = if (diff >= 0) "+" else ""
            Text(
                "$sign${formatMoney(diff, watch.estimatedValueCurrency ?: watch.purchaseCurrency)} since purchase",
                style = MaterialTheme.typography.bodySmall,
                color = if (diff >= 0) successColor else dangerColor
            )
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = vaultColors.border, thickness = 1.dp)
        content()
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SpecificationsContent(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
private fun OwnershipContent(watch: Watch) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledValue("Ownership status", watch.ownershipStatus)
        LabeledValue("Nickname", watch.nickname ?: "—")
        LabeledValue("Source", watch.source)
        LabeledValue("Purchase date", formatDate(watch.purchaseDate))
        LabeledValue("Seller", watch.seller ?: "—")
        LabeledValue("Location", watch.purchaseLocation ?: "—")
        LabeledValue("Invoice number", watch.invoiceNumber ?: "—")
        LabeledValue("First owner", watch.isFirstOwner?.toString() ?: "Unknown")
    }
}

@Composable
private fun MaintenanceContent(records: List<MaintenanceRecord>) {
    if (records.isEmpty()) {
        Text("No maintenance records yet.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        records.forEach { record ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${record.type ?: "Service"} — ${formatDate(record.date)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(
                        record.technician,
                        record.cost?.let { formatMoney(it, "INR") },
                        if (record.isOverhaul) "Overhaul" else null,
                        if (record.pressureTested) "Pressure tested" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
