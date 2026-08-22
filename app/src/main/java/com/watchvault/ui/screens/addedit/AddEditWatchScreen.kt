package com.watchvault.ui.screens.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = referenceNumber, onValueChange = { referenceNumber = it }, label = { Text("Reference number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = movementRaw, onValueChange = { movementRaw = it }, label = { Text("Movement") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = conditionRaw, onValueChange = { conditionRaw = it }, label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = purchasePrice, onValueChange = { purchasePrice = it },
                label = { Text("Purchase price") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = purchaseCurrency, onValueChange = { purchaseCurrency = it }, label = { Text("Purchase currency") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = estimatedValue, onValueChange = { estimatedValue = it },
                label = { Text("Estimated value") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            TriStateRow("Box", box) { box = it }
            TriStateRow("Papers", papers) { papers = it }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Button(
                enabled = brand.isNotBlank() && model.isNotBlank(),
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun TriStateRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column {
        Text(label)
        androidx.compose.foundation.layout.Row {
            androidx.compose.material3.FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Yes") })
            androidx.compose.material3.FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("No") })
            androidx.compose.material3.FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("Unknown") })
        }
    }
}
