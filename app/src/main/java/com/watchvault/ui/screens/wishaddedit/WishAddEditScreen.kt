package com.watchvault.ui.screens.wishaddedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.urlimport.ExtractedProductData
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.ErrorSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishAddEditScreen(wishUuid: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WishAddEditViewModel = viewModel(
        factory = GenericViewModelFactory { WishAddEditViewModel(container.wishlistRepository, container.urlImportPipeline, wishUuid) }
    )

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var productUrl by remember { mutableStateOf("") }
    var targetPrice by remember { mutableStateOf("") }
    var currentPrice by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var priority by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }
    var existing by remember { mutableStateOf<WishlistItem?>(null) }
    var urlToFetch by remember { mutableStateOf("") }
    // Manual entry starts collapsed for a brand-new wish (URL paste + Fetch is the primary path)
    // but is already open when editing an existing item, since its fields need to be visible.
    var manualEntryExpanded by remember { mutableStateOf(wishUuid != null) }

    LaunchedEffect(wishUuid) {
        viewModel.load()?.let { item ->
            existing = item
            brand = item.brand; model = item.model
            referenceNumber = item.referenceNumber.orEmpty()
            productUrl = item.productUrl.orEmpty()
            targetPrice = item.targetPrice?.toString().orEmpty()
            currentPrice = item.currentPrice?.toString().orEmpty()
            currency = item.currency ?: "INR"
            priority = item.priority
            notes = item.notes.orEmpty()
        }
    }

    val urlState by viewModel.urlImportState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (wishUuid == null) "Add Wish" else "Edit Wish") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Paste a product page link and we'll try to fill in the details for you.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = urlToFetch, onValueChange = { urlToFetch = it }, label = { Text("Product URL") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (urlToFetch.isNotBlank()) viewModel.fetchFromUrl(urlToFetch) }, modifier = Modifier.fillMaxWidth()) {
                Text("Fetch")
            }

            if (!manualEntryExpanded) {
                TextButton(onClick = { manualEntryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter manually")
                }
            } else {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = referenceNumber, onValueChange = { referenceNumber = it }, label = { Text("Reference number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = productUrl, onValueChange = { productUrl = it }, label = { Text("Product URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = targetPrice, onValueChange = { targetPrice = it }, label = { Text("Target price") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currentPrice, onValueChange = { currentPrice = it }, label = { Text("Current price") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority (Grail/High/Medium/Low)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Button(
                    enabled = brand.isNotBlank() && model.isNotBlank(),
                    onClick = {
                        val now = System.currentTimeMillis()
                        val item = (existing ?: WishlistItem(uuid = "", brand = brand, model = model, dateAdded = now, updatedAt = now)).copy(
                            brand = brand, model = model,
                            referenceNumber = referenceNumber.ifBlank { null },
                            productUrl = productUrl.ifBlank { null },
                            targetPrice = targetPrice.toDoubleOrNull(),
                            currentPrice = currentPrice.toDoubleOrNull(),
                            currency = currency.ifBlank { null },
                            priority = priority.ifBlank { "Medium" },
                            notes = notes.ifBlank { null },
                            updatedAt = now
                        )
                        viewModel.save(item, onSaved)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save") }
            }
        }
    }

    when (val state = urlState) {
        is UrlImportState.Loading -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Fetching…") },
            text = { CircularProgressIndicator() }
        )
        is UrlImportState.Error -> {
            val headline = when (state.category) {
                UrlFetchFailureCategory.BLOCKED -> "Lookup blocked"
                UrlFetchFailureCategory.NOT_FOUND -> "Page not found"
                UrlFetchFailureCategory.NETWORK -> "Connection problem"
                UrlFetchFailureCategory.MALFORMED_URL -> "Invalid link"
                UrlFetchFailureCategory.UNKNOWN -> "Something went wrong"
            }
            if (state.category == UrlFetchFailureCategory.MALFORMED_URL) {
                // Retrying the same bad URL won't help — just acknowledge and let them fix it.
                ErrorSheet(
                    headline = headline,
                    body = state.message,
                    onDismiss = viewModel::clearUrlImport,
                    primaryActionLabel = "OK",
                    onPrimaryAction = viewModel::clearUrlImport
                )
            } else {
                ErrorSheet(
                    headline = headline,
                    body = state.message,
                    onDismiss = viewModel::clearUrlImport,
                    primaryActionLabel = "Try Again",
                    onPrimaryAction = {
                        viewModel.clearUrlImport()
                        viewModel.fetchFromUrl(urlToFetch)
                    },
                    secondaryActionLabel = "Enter Manually",
                    onSecondaryAction = { manualEntryExpanded = true; viewModel.clearUrlImport() }
                )
            }
        }
        is UrlImportState.Preview -> UrlPreviewDialog(
            data = state.data,
            onDismiss = viewModel::clearUrlImport,
            onApply = { data ->
                data.brand.value?.let { brand = it }
                data.title.value?.let { if (model.isBlank()) model = it }
                data.referenceNumber.value?.let { referenceNumber = it }
                data.price.value?.let { currentPrice = it.toString() }
                data.currency.value?.let { currency = it }
                productUrl = data.sourceUrl
                manualEntryExpanded = true
                viewModel.clearUrlImport()
            }
        )
        UrlImportState.Idle -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlPreviewDialog(data: ExtractedProductData, onDismiss: () -> Unit, onApply: (ExtractedProductData) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review before saving") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nothing is saved until you confirm each field below.", style = MaterialTheme.typography.bodySmall)
                PreviewRow("Title", data.title.value, data.title.source?.name)
                PreviewRow("Brand", data.brand.value, data.brand.source?.name)
                PreviewRow("Reference", data.referenceNumber.value, data.referenceNumber.source?.name)
                PreviewRow("Price", data.price.value?.toString(), data.price.source?.name)
                PreviewRow("Currency", data.currency.value, data.currency.source?.name)
            }
        },
        confirmButton = { TextButton(onClick = { onApply(data) }) { Text("Apply to form") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}

@Composable
private fun PreviewRow(field: String, value: String?, source: String?) {
    Column {
        Text("$field: ${value ?: "not found"}", style = MaterialTheme.typography.bodyMedium)
        Text("Source: ${source ?: "needs confirmation"}", style = MaterialTheme.typography.labelSmall)
    }
}
