package com.watchvault.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * "Discover" tab: today this is a lightweight entry point into the existing URL-import flow
 * (which lives on the Add/Edit Wish screen) plus a clearly-labelled placeholder for future
 * watch-research features. No fake data, no new pipeline logic — this screen only links out to
 * the existing, working URL-import UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(onAddFromUrl: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Discover") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Link, contentDescription = null)
                    Text("Add from URL", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Paste a product page link and we'll try to pull the brand, model, reference and price into a wishlist draft for you to review before anything is saved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onAddFromUrl, modifier = Modifier.fillMaxWidth()) {
                        Text("Add from URL")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Text("Watch research — coming later", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Reference lookups, market comparisons and brand catalogues are planned for a future release. Nothing here yet — this section is a placeholder, not live data.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
