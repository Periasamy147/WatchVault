package com.watchvault.ui.screens.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.Watch
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer

/**
 * Add/Edit Watch. The only fields required to save are Brand and Model — everything else can be
 * filled in later. Sections are ordered Photo -> Identity -> Value -> Ownership ->
 * Specifications -> Maintenance -> Notes, matching the read-only order on WatchDetailScreen's
 * tabs, and are individually collapsible so a quick save doesn't force scrolling past every
 * field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWatchScreen(watchUuid: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: AddEditWatchViewModel = viewModel(
        factory = GenericViewModelFactory { AddEditWatchViewModel(container.watchRepository, watchUuid) }
    )

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var purchaseCurrency by remember { mutableStateOf("INR") }
    var estimatedValue by remember { mutableStateOf("") }
    var movementRaw by remember { mutableStateOf("") }
    var conditionRaw by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var box by remember { mutableStateOf<Boolean?>(null) }
    var papers by remember { mutableStateOf<Boolean?>(null) }
    var loadedExisting by remember { mutableStateOf<Watch?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSavedUuid by remember { mutableStateOf<String?>(null) }

    // Delay navigation just long enough for the confirmation to actually be visible, since the
    // screen (and its coroutine scope) is disposed the moment onSaved() triggers navigation.
    LaunchedEffect(pendingSavedUuid) {
        val savedUuid = pendingSavedUuid ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "Watch saved — you can complete the remaining details anytime",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
        onSaved(savedUuid)
    }

    LaunchedEffect(watchUuid) {
        val existing = viewModel.load()
        if (existing != null) {
            loadedExisting = existing
            brand = existing.brand
            model = existing.model
            referenceNumber = existing.referenceNumber.orEmpty()
            purchasePrice = existing.purchasePrice?.toString().orEmpty()
            purchaseCurrency = existing.purchaseCurrency ?: "INR"
            estimatedValue = existing.estimatedValue?.toString().orEmpty()
            movementRaw = existing.movementRaw.orEmpty()
            conditionRaw = existing.conditionRaw.orEmpty()
            notes = existing.notes.orEmpty()
            box = existing.box
            papers = existing.papers
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (watchUuid == null) "Add Watch" else "Edit Watch") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormSection(title = "Photo", initiallyExpanded = false) {
                Text(
                    "Photos can be viewed from the watch's detail screen. Adding photos from this form is coming in a future update.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FormSection(title = "Identity", initiallyExpanded = true) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = referenceNumber, onValueChange = { referenceNumber = it }, label = { Text("Reference number") }, modifier = Modifier.fillMaxWidth())
            }

            SaveWatchButton(
                enabled = brand.isNotBlank() && model.isNotBlank(),
                onClick = {
                    saveWatch(loadedExisting, brand, model, referenceNumber, movementRaw, conditionRaw, purchasePrice, purchaseCurrency, estimatedValue, box, papers, notes, viewModel) { savedUuid ->
                        pendingSavedUuid = savedUuid
                    }
                }
            )

            Text(
                "Everything below is optional and can be filled in anytime.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormSection(title = "Value", initiallyExpanded = false) {
                OutlinedTextField(
                    value = estimatedValue, onValueChange = { estimatedValue = it },
                    label = { Text("Estimated value") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSection(title = "Ownership", initiallyExpanded = false) {
                OutlinedTextField(
                    value = purchasePrice, onValueChange = { purchasePrice = it },
                    label = { Text("Purchase price") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = purchaseCurrency, onValueChange = { purchaseCurrency = it }, label = { Text("Purchase currency") }, modifier = Modifier.fillMaxWidth())
                TriStateRow("Box", box) { box = it }
                TriStateRow("Papers", papers) { papers = it }
            }

            FormSection(title = "Specifications", initiallyExpanded = false) {
                OutlinedTextField(value = movementRaw, onValueChange = { movementRaw = it }, label = { Text("Movement") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = conditionRaw, onValueChange = { conditionRaw = it }, label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())
            }

            FormSection(title = "Maintenance", initiallyExpanded = false) {
                Text(
                    "Maintenance records can be added from the watch's detail screen once it's saved.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FormSection(title = "Notes", initiallyExpanded = false) {
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }

            SaveWatchButton(
                enabled = brand.isNotBlank() && model.isNotBlank(),
                onClick = {
                    saveWatch(loadedExisting, brand, model, referenceNumber, movementRaw, conditionRaw, purchasePrice, purchaseCurrency, estimatedValue, box, papers, notes, viewModel) { savedUuid ->
                        pendingSavedUuid = savedUuid
                    }
                }
            )
        }
    }
}

private fun saveWatch(
    loadedExisting: Watch?,
    brand: String,
    model: String,
    referenceNumber: String,
    movementRaw: String,
    conditionRaw: String,
    purchasePrice: String,
    purchaseCurrency: String,
    estimatedValue: String,
    box: Boolean?,
    papers: Boolean?,
    notes: String,
    viewModel: AddEditWatchViewModel,
    onSaved: (String) -> Unit
) {
    val now = System.currentTimeMillis()
    val watch = (loadedExisting ?: Watch(
        uuid = "", brand = brand, model = model, createdAt = now, updatedAt = now
    )).copy(
        brand = brand,
        model = model,
        referenceNumber = referenceNumber.ifBlank { null },
        movementRaw = movementRaw.ifBlank { null },
        conditionRaw = conditionRaw.ifBlank { null },
        purchasePrice = purchasePrice.toDoubleOrNull(),
        purchaseCurrency = purchaseCurrency.ifBlank { null },
        estimatedValue = estimatedValue.toDoubleOrNull(),
        box = box,
        papers = papers,
        notes = notes.ifBlank { null },
        updatedAt = now
    )
    viewModel.save(watch, onSaved)
}

@Composable
private fun SaveWatchButton(enabled: Boolean, onClick: () -> Unit) {
    Button(enabled = enabled, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Save Watch")
    }
}

@Composable
private fun FormSection(title: String, initiallyExpanded: Boolean, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (expanded) "$title ▲" else "$title ▼",
                style = MaterialTheme.typography.titleMedium
            )
            if (expanded) content()
        }
    }
}

@Composable
private fun TriStateRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column {
        Text(label)
        Row {
            androidx.compose.material3.FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Yes") })
            androidx.compose.material3.FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("No") })
            androidx.compose.material3.FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("Unknown") })
        }
    }
}
