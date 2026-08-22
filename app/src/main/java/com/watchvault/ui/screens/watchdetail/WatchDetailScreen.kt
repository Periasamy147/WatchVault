package com.watchvault.ui.screens.watchdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.WatchSpecGrid
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * Watch Detail rebuilt as an editorial product page: the photo is the hero (near edge-to-edge,
 * no toolbar consuming it), back/edit/delete float over the image, and everything below is plain
 * typography separated by hairline dividers. Only populated fields render anywhere on this
 * screen — there are no placeholder rows, no raw booleans, no "(assumed)" currency caveats, and
 * no low-level provenance fields (nickname/source/seller/location/invoice/first-owner) in the
 * primary flow. Those live behind a collapsed "Provenance" disclosure at the bottom.
 */
@Composable
fun WatchDetailScreen(watchUuid: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WatchDetailViewModel = viewModel(
        factory = GenericViewModelFactory { WatchDetailViewModel(container.watchRepository, watchUuid) }
    )
    val details by viewModel.watch.collectAsState()
    val vaultColors = LocalVaultColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val watch = details?.watch
    if (watch == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingTopControls(
                onBack = onBack,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onEdit = {},
                onDelete = {},
                enabled = false
            )
        }
        return
    }
    val photos = details?.photos ?: emptyList()
    val primaryPhoto = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this watch?") },
            text = { Text("${watch.brand} ${watch.model} and its records will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    viewModel.delete(watch)
                    onBack()
                }) { Text("Delete", color = vaultColors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            WatchPhotoOrPlaceholder(
                photo = primaryPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            FloatingTopControls(
                onBack = onBack,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onEdit = { onEdit(watchUuid) },
                onDelete = { confirmingDelete = true },
                enabled = true
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            IdentityBlock(watch)
            ValueBlock(watch, vaultColors.gold, vaultColors.success, vaultColors.danger)

            val quickFacts = quickFactsLine(watch)
            if (quickFacts != null) {
                DividedSection(title = null) {
                    Text(quickFacts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            val ownershipRows = ownershipRows(watch)
            if (ownershipRows.isNotEmpty()) {
                DividedSection(title = "OWNERSHIP") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ownershipRows.forEach { (label, value) -> LabeledRow(label, value) }
                    }
                }
            }

            val records = details?.maintenanceRecords ?: emptyList()
            DividedSection(title = "SERVICE") { ServiceContent(records, watch.purchaseCurrency) }

            val specRows = specificationRows(watch)
            if (specRows.isNotEmpty()) {
                DividedSection(title = "SPECIFICATIONS") { WatchSpecGrid(specRows) }
            }

            if (!watch.notes.isNullOrBlank()) {
                DividedSection(title = "NOTES") { CollapsibleNotes(watch.notes) }
            }

            val provenanceRows = provenanceRows(watch)
            if (provenanceRows.isNotEmpty()) {
                ProvenanceDisclosure(provenanceRows)
            }
        }
    }
}

@Composable
private fun FloatingTopControls(
    onBack: () -> Unit,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FloatingIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            if (enabled) {
                Box {
                    FloatingIconButton(icon = Icons.Filled.MoreVert, contentDescription = "More", onClick = { onMenuToggle(true) })
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuToggle(false) }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { onMenuToggle(false); onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { onMenuToggle(false); onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White)
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
private fun ValueBlock(watch: Watch, goldColor: Color, successColor: Color, dangerColor: Color) {
    val current = watch.estimatedValue ?: watch.purchasePrice
    val currentCurrency = watch.estimatedValueCurrency ?: watch.purchaseCurrency
    if (current == null) return

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(formatMoney(current, currentCurrency), style = MaterialTheme.typography.headlineSmall, color = goldColor)
        val purchase = watch.purchasePrice
        val estimated = watch.estimatedValue
        if (estimated != null && purchase != null) {
            val diff = estimated - purchase
            val percent = if (purchase != 0.0) (diff / purchase) * 100 else null
            val sign = if (diff >= 0) "+" else ""
            val percentText = percent?.let { " · ${if (it >= 0) "+" else ""}${"%.1f".format(it)}%" } ?: ""
            Text(
                "$sign${formatMoney(diff, currentCurrency)}$percentText",
                style = MaterialTheme.typography.bodySmall,
                color = if (diff >= 0) successColor else dangerColor
            )
        }
    }
}

/** "AUTOMATIC · 44 MM · GREEN DIAL · STAINLESS STEEL" — only the facts that are actually known,
 *  joined into one line. Returns null when nothing is known at all. */
private fun quickFactsLine(watch: Watch): String? {
    val facts = listOfNotNull(
        watch.movementNormalized ?: watch.movementRaw,
        watch.caseDiameterMm?.let { "${it.toInt()} mm" },
        watch.dialColour,
        watch.caseMaterial
    )
    if (facts.isEmpty()) return null
    return facts.joinToString(" · ") { it.uppercase() }
}

private fun ownershipRows(watch: Watch): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    watch.purchaseDate?.let { rows += "Purchased" to formatDate(it) }
    watch.purchasePrice?.let { rows += "Price" to formatMoney(it, watch.purchaseCurrency) }
    watch.conditionRaw?.let { rows += "Condition" to it }
    boxPapersLabel(watch)?.let { rows += "Box & Papers" to it }
    return rows
}

private fun boxPapersLabel(watch: Watch): String? {
    return when {
        watch.box == true && watch.papers == true -> "Yes"
        watch.box == false && watch.papers == false -> "No"
        watch.box != null || watch.papers != null -> {
            val parts = mutableListOf<String>()
            if (watch.box == true) parts += "Box"
            if (watch.papers == true) parts += "Papers"
            if (parts.isEmpty()) "No" else parts.joinToString(" + ")
        }
        else -> null
    }
}

@Composable
private fun ServiceContent(records: List<MaintenanceRecord>, fallbackCurrency: String?) {
    if (records.isEmpty()) {
        Text("No service recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        records.sortedByDescending { it.date }.forEach { record ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${record.type?.replaceFirstChar { it.uppercase() } ?: "Service"} — ${formatDate(record.date)}", style = MaterialTheme.typography.bodyMedium)
                val details = listOfNotNull(
                    record.technician,
                    record.cost?.let { formatMoney(it, fallbackCurrency ?: "INR") },
                    if (record.isOverhaul) "Overhaul" else null,
                    if (record.pressureTested) "Pressure tested" else null
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatMm(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()

/** Full specification grid — every known technical fact, not just the four-fact summary line
 *  above the value block. Only populated fields are included. */
private fun specificationRows(watch: Watch): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    (watch.movementNormalized ?: watch.movementRaw)?.let { rows += "Movement" to it }
    watch.caseMaterial?.let { rows += "Case" to it }
    watch.caseDiameterMm?.let { rows += "Case diameter" to "${formatMm(it)} mm" }
    watch.caseThicknessMm?.let { rows += "Case thickness" to "${formatMm(it)} mm" }
    watch.crystal?.let { rows += "Crystal" to it }
    watch.dialColour?.let { rows += "Dial" to it }
    listOfNotNull(watch.strap, watch.strapMaterial).joinToString(", ").ifBlank { null }?.let { rows += "Strap" to it }
    watch.waterResistance?.let { rows += "Water resistance" to it }
    watch.lugWidthMm?.let { rows += "Lug width" to "${formatMm(it)} mm" }
    watch.caliber?.let { rows += "Caliber" to it }
    watch.powerReserve?.let { rows += "Power reserve" to it }
    watch.complications?.let { rows += "Complications" to it }
    watch.batteryType?.let { rows += "Battery" to it }
    return rows
}

@Composable
private fun CollapsibleNotes(notes: String) {
    var expanded by remember { mutableStateOf(false) }
    val vaultColors = LocalVaultColors.current
    val isLong = notes.length > 220
    val shown = if (expanded || !isLong) notes else notes.take(220).trimEnd() + "…"
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(shown, style = MaterialTheme.typography.bodyMedium)
        if (isLong) {
            Text(
                if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelSmall,
                color = vaultColors.gold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

private fun provenanceRows(watch: Watch): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    watch.nickname?.let { rows += "Nickname" to it }
    watch.seller?.let { rows += "Seller" to it }
    watch.purchaseLocation?.let { rows += "Purchase location" to it }
    watch.invoiceNumber?.let { rows += "Invoice number" to it }
    watch.isFirstOwner?.let { rows += "First owner" to if (it) "Yes" else "No" }
    if (watch.source == "myinnos_import") rows += "Imported from" to "MyInnos Watch Collection"
    return rows
}

@Composable
private fun ProvenanceDisclosure(rows: List<Pair<String, String>>) {
    var expanded by remember { mutableStateOf(false) }
    val vaultColors = LocalVaultColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = vaultColors.border, thickness = 1.dp)
        Text(
            if (expanded) "PROVENANCE ▴" else "PROVENANCE ▾",
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { expanded = !expanded }
        )
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { (label, value) -> LabeledRow(label, value) }
            }
        }
    }
}

@Composable
private fun DividedSection(title: String?, content: @Composable () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = vaultColors.border, thickness = 1.dp)
        if (title != null) {
            Text(title, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
