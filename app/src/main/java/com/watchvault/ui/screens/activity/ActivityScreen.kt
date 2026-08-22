package com.watchvault.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors

/**
 * Real, working chronological timeline of maintenance across the whole collection — not a
 * mockup. Scoped to maintenance records only, since accuracy/wear/price-history entities are
 * out of scope for this pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(onOpenWatch: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ActivityViewModel = viewModel(
        factory = GenericViewModelFactory { ActivityViewModel(container.watchRepository) }
    )
    val entries by viewModel.entries.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Activity") }) }) { padding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    "No maintenance records yet. Add one from a watch's detail screen and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { "${it.watchUuid}-${it.record.uuid}" }) { entry ->
                    ActivityRow(entry, onClick = { onOpenWatch(entry.watchUuid) })
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: MaintenanceActivityEntry, onClick: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(vaultColors.gold)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatDate(entry.record.date), style = MaterialTheme.typography.labelMedium)
                Text("${entry.brand} ${entry.model}", style = MaterialTheme.typography.titleSmall)
                Text(
                    entry.record.type?.replaceFirstChar { it.uppercase() } ?: "Service",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    listOfNotNull(
                        entry.record.technician,
                        entry.record.cost?.let { formatMoney(it, "INR") },
                        if (entry.record.isOverhaul) "Overhaul" else null,
                        if (entry.record.pressureTested) "Pressure tested" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
