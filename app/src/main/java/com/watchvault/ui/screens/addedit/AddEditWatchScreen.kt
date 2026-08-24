package com.watchvault.ui.screens.addedit

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.PrimaryButton
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Radius
import com.watchvault.ui.theme.Spacing
import java.util.Calendar

/**
 * Add/Edit Watch. The only fields required to save are Brand and Model — everything else can be
 * filled in later. Sections are ordered Photos -> Identity -> Value -> Ownership ->
 * Specifications -> Maintenance -> Notes, matching the read-only order on WatchDetailScreen, and
 * are individually collapsible so a quick save doesn't force scrolling past every field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWatchScreen(watchUuid: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: AddEditWatchViewModel = viewModel(
        factory = GenericViewModelFactory { AddEditWatchViewModel(container.watchRepository, watchUuid) }
    )

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var watchType by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }

    var estimatedValue by remember { mutableStateOf("") }
    var estimatedValueCurrency by remember { mutableStateOf("INR") }

    var purchaseDate by remember { mutableStateOf<Long?>(null) }
    var purchasePrice by remember { mutableStateOf("") }
    var purchaseCurrency by remember { mutableStateOf("INR") }
    var seller by remember { mutableStateOf("") }
    var purchaseLocation by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var warrantyExpiry by remember { mutableStateOf<Long?>(null) }
    var isFirstOwner by remember { mutableStateOf<Boolean?>(null) }
    var box by remember { mutableStateOf<Boolean?>(null) }
    var papers by remember { mutableStateOf<Boolean?>(null) }

    var movementRaw by remember { mutableStateOf("") }
    var conditionRaw by remember { mutableStateOf("") }
    var caseDiameterMm by remember { mutableStateOf("") }
    var caseThicknessMm by remember { mutableStateOf("") }
    var caseMaterial by remember { mutableStateOf("") }
    var caseColour by remember { mutableStateOf("") }
    var caseShape by remember { mutableStateOf("") }
    var crystal by remember { mutableStateOf("") }
    var dialColour by remember { mutableStateOf("") }
    var dialType by remember { mutableStateOf("") }
    var strap by remember { mutableStateOf("") }
    var strapMaterial by remember { mutableStateOf("") }
    var strapColour by remember { mutableStateOf("") }
    var lugWidthMm by remember { mutableStateOf("") }
    var waterResistance by remember { mutableStateOf("") }
    var caliber by remember { mutableStateOf("") }
    var powerReserve by remember { mutableStateOf("") }
    var complications by remember { mutableStateOf("") }
    var batteryType by remember { mutableStateOf("") }
    var batteryLife by remember { mutableStateOf("") }

    var notes by remember { mutableStateOf("") }
    var loadedExisting by remember { mutableStateOf<Watch?>(null) }

    val photos by viewModel.photos.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSavedUuid by remember { mutableStateOf<String?>(null) }

    val handleBack: () -> Unit = {
        viewModel.discardIfUnsaved(context)
        onBack()
    }

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
            nickname = existing.nickname.orEmpty()
            referenceNumber = existing.referenceNumber.orEmpty()
            watchType = existing.watchType.orEmpty()
            serialNumber = existing.serialNumber.orEmpty()
            estimatedValue = existing.estimatedValue?.toString().orEmpty()
            estimatedValueCurrency = existing.estimatedValueCurrency ?: existing.purchaseCurrency ?: "INR"
            purchaseDate = existing.purchaseDate
            purchasePrice = existing.purchasePrice?.toString().orEmpty()
            purchaseCurrency = existing.purchaseCurrency ?: "INR"
            seller = existing.seller.orEmpty()
            purchaseLocation = existing.purchaseLocation.orEmpty()
            invoiceNumber = existing.invoiceNumber.orEmpty()
            warrantyExpiry = existing.warrantyExpiry
            isFirstOwner = existing.isFirstOwner
            box = existing.box
            papers = existing.papers
            movementRaw = existing.movementRaw.orEmpty()
            conditionRaw = existing.conditionRaw.orEmpty()
            caseDiameterMm = existing.caseDiameterMm?.let { formatMm(it) }.orEmpty()
            caseThicknessMm = existing.caseThicknessMm?.let { formatMm(it) }.orEmpty()
            caseMaterial = existing.caseMaterial.orEmpty()
            caseColour = existing.caseColour.orEmpty()
            caseShape = existing.caseShape.orEmpty()
            crystal = existing.crystal.orEmpty()
            dialColour = existing.dialColour.orEmpty()
            dialType = existing.dialType.orEmpty()
            strap = existing.strap.orEmpty()
            strapMaterial = existing.strapMaterial.orEmpty()
            strapColour = existing.strapColour.orEmpty()
            lugWidthMm = existing.lugWidthMm?.let { formatMm(it) }.orEmpty()
            waterResistance = existing.waterResistance.orEmpty()
            caliber = existing.caliber.orEmpty()
            powerReserve = existing.powerReserve.orEmpty()
            complications = existing.complications.orEmpty()
            batteryType = existing.batteryType.orEmpty()
            batteryLife = existing.batteryLife.orEmpty()
            notes = existing.notes.orEmpty()
        }
    }

    fun buildWatch(): Watch {
        val now = System.currentTimeMillis()
        return (loadedExisting ?: Watch(uuid = "", brand = brand, model = model, createdAt = now, updatedAt = now)).copy(
            brand = brand,
            model = model,
            nickname = nickname.ifBlank { null },
            referenceNumber = referenceNumber.ifBlank { null },
            watchType = watchType.ifBlank { null },
            serialNumber = serialNumber.ifBlank { null },
            estimatedValue = estimatedValue.toDoubleOrNull(),
            estimatedValueCurrency = estimatedValueCurrency.ifBlank { null },
            purchaseDate = purchaseDate,
            purchasePrice = purchasePrice.toDoubleOrNull(),
            purchaseCurrency = purchaseCurrency.ifBlank { null },
            seller = seller.ifBlank { null },
            purchaseLocation = purchaseLocation.ifBlank { null },
            invoiceNumber = invoiceNumber.ifBlank { null },
            warrantyExpiry = warrantyExpiry,
            isFirstOwner = isFirstOwner,
            box = box,
            papers = papers,
            movementRaw = movementRaw.ifBlank { null },
            conditionRaw = conditionRaw.ifBlank { null },
            caseDiameterMm = caseDiameterMm.toDoubleOrNull(),
            caseThicknessMm = caseThicknessMm.toDoubleOrNull(),
            caseMaterial = caseMaterial.ifBlank { null },
            caseColour = caseColour.ifBlank { null },
            caseShape = caseShape.ifBlank { null },
            crystal = crystal.ifBlank { null },
            dialColour = dialColour.ifBlank { null },
            dialType = dialType.ifBlank { null },
            strap = strap.ifBlank { null },
            strapMaterial = strapMaterial.ifBlank { null },
            strapColour = strapColour.ifBlank { null },
            lugWidthMm = lugWidthMm.toDoubleOrNull(),
            waterResistance = waterResistance.ifBlank { null },
            caliber = caliber.ifBlank { null },
            powerReserve = powerReserve.ifBlank { null },
            complications = complications.ifBlank { null },
            batteryType = batteryType.ifBlank { null },
            batteryLife = batteryLife.ifBlank { null },
            notes = notes.ifBlank { null },
            updatedAt = now
        )
    }

    val save: () -> Unit = {
        viewModel.save(buildWatch(), context) { savedUuid -> pendingSavedUuid = savedUuid }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (watchUuid == null) "Add Watch" else "Edit Watch") },
                navigationIcon = { IconButton(onClick = handleBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.screenH).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FormSection(title = "Photos", initiallyExpanded = true) {
                PhotosEditor(
                    photos = photos,
                    onAdd = { uris -> viewModel.addPhotos(context, uris) },
                    onSetPrimary = viewModel::setPrimary,
                    onRemove = viewModel::removePhoto,
                    onMove = viewModel::movePhoto
                )
            }

            FormSection(title = "Identity", initiallyExpanded = true) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = referenceNumber, onValueChange = { referenceNumber = it }, label = { Text("Reference number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = watchType, onValueChange = { watchType = it }, label = { Text("Watch type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("Serial number") }, modifier = Modifier.fillMaxWidth())
            }

            SaveWatchButton(enabled = brand.isNotBlank() && model.isNotBlank(), onClick = save)

            Text(
                "Everything below is optional and can be filled in anytime.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormSection(title = "Value", initiallyExpanded = false) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(
                        value = estimatedValue, onValueChange = { estimatedValue = it },
                        label = { Text("Estimated value") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estimatedValueCurrency, onValueChange = { estimatedValueCurrency = it },
                        label = { Text("Currency") }, modifier = Modifier.width(96.dp)
                    )
                }
            }

            FormSection(title = "Ownership", initiallyExpanded = false) {
                DateField(label = "Purchase date", valueMillis = purchaseDate, onChange = { purchaseDate = it })
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(
                        value = purchasePrice, onValueChange = { purchasePrice = it },
                        label = { Text("Purchase price") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(value = purchaseCurrency, onValueChange = { purchaseCurrency = it }, label = { Text("Currency") }, modifier = Modifier.width(96.dp))
                }
                OutlinedTextField(value = seller, onValueChange = { seller = it }, label = { Text("Seller") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = purchaseLocation, onValueChange = { purchaseLocation = it }, label = { Text("Purchase location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice number") }, modifier = Modifier.fillMaxWidth())
                DateField(label = "Warranty expiry", valueMillis = warrantyExpiry, onChange = { warrantyExpiry = it })
                TriStateRow("First owner", isFirstOwner) { isFirstOwner = it }
                TriStateRow("Box", box) { box = it }
                TriStateRow("Papers", papers) { papers = it }
            }

            FormSection(title = "Specifications", initiallyExpanded = false) {
                OutlinedTextField(value = movementRaw, onValueChange = { movementRaw = it }, label = { Text("Movement") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = conditionRaw, onValueChange = { conditionRaw = it }, label = { Text("Condition") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = caseDiameterMm, onValueChange = { caseDiameterMm = it }, label = { Text("Case diameter (mm)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = caseThicknessMm, onValueChange = { caseThicknessMm = it }, label = { Text("Case thickness (mm)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = caseMaterial, onValueChange = { caseMaterial = it }, label = { Text("Case material") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = caseColour, onValueChange = { caseColour = it }, label = { Text("Case colour") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = caseShape, onValueChange = { caseShape = it }, label = { Text("Case shape") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = crystal, onValueChange = { crystal = it }, label = { Text("Crystal") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = dialColour, onValueChange = { dialColour = it }, label = { Text("Dial colour") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dialType, onValueChange = { dialType = it }, label = { Text("Dial type") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = strap, onValueChange = { strap = it }, label = { Text("Strap/bracelet") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = strapMaterial, onValueChange = { strapMaterial = it }, label = { Text("Strap material") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = strapColour, onValueChange = { strapColour = it }, label = { Text("Strap colour") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = lugWidthMm, onValueChange = { lugWidthMm = it }, label = { Text("Lug width (mm)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = waterResistance, onValueChange = { waterResistance = it }, label = { Text("Water resistance") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = caliber, onValueChange = { caliber = it }, label = { Text("Caliber") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = powerReserve, onValueChange = { powerReserve = it }, label = { Text("Power reserve") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = complications, onValueChange = { complications = it }, label = { Text("Complications") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(value = batteryType, onValueChange = { batteryType = it }, label = { Text("Battery type") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = batteryLife, onValueChange = { batteryLife = it }, label = { Text("Battery life") }, modifier = Modifier.weight(1f))
                }
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

            SaveWatchButton(enabled = brand.isNotBlank() && model.isNotBlank(), onClick = save)
        }
    }
}

private fun formatMm(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()

@Composable
private fun SaveWatchButton(enabled: Boolean, onClick: () -> Unit) {
    PrimaryButton(text = "Save Watch", onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FormSection(title: String, initiallyExpanded: Boolean, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        shape = RoundedCornerShape(Radius.card),
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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

@Composable
private fun DateField(label: String, valueMillis: Long?, onChange: (Long?) -> Unit) {
    val context = LocalContext.current
    val displayValue = valueMillis?.let { formatDate(it) }.orEmpty()

    val openPicker: () -> Unit = {
        val calendar = Calendar.getInstance()
        valueMillis?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onChange(picked.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            if (valueMillis != null) {
                IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Close, contentDescription = "Clear $label") }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = openPicker)
    )
}

/**
 * Photo management for Add/Edit: pick one or more images via the system Photo Picker (no storage
 * permission needed on any supported API level), show them as a horizontally scrollable strip of
 * thumbnails, and let the user set the primary photo, nudge ordering, or delete individual photos.
 */
@Composable
private fun PhotosEditor(
    photos: List<WatchPhoto>,
    onAdd: (List<android.net.Uri>) -> Unit,
    onSetPrimary: (String) -> Unit,
    onRemove: (WatchPhoto) -> Unit,
    onMove: (WatchPhoto, Int) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (photos.isEmpty()) {
            Text(
                "No photos yet. Add at least one so this watch is recognizable at a glance in your collection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                photos.forEachIndexed { index, photo ->
                    PhotoThumbnail(
                        photo = photo,
                        isFirst = index == 0,
                        isLast = index == photos.lastIndex,
                        onSetPrimary = { onSetPrimary(photo.uuid) },
                        onRemove = { onRemove(photo) },
                        onMoveLeft = { onMove(photo, -1) },
                        onMoveRight = { onMove(photo, 1) }
                    )
                }
            }
        }
        Button(onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Add photos", modifier = Modifier.padding(start = Spacing.xxs))
        }
        if (photos.isNotEmpty()) {
            Text(
                "The starred photo is used as this watch's cover everywhere in the app.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: WatchPhoto,
    isFirst: Boolean,
    isLast: Boolean,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    val vaultColors = LocalVaultColors.current
    Column(
        modifier = Modifier.width(104.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            WatchPhotoOrPlaceholder(
                photo = photo,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Radius.thumbnail))
                    .border(1.dp, vaultColors.border, RoundedCornerShape(Radius.thumbnail))
            )
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onSetPrimary)
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Set as cover photo",
                    tint = if (photo.isPrimary) vaultColors.gold else Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMoveLeft, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Move left", modifier = Modifier.size(18.dp))
            }
            TextButton(onClick = onRemove, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Remove", style = MaterialTheme.typography.labelSmall, color = vaultColors.danger)
            }
            IconButton(onClick = onMoveRight, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Move right", modifier = Modifier.size(18.dp))
            }
        }
    }
}
