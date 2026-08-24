package com.watchvault.ui.screens.addedit

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.IconActionButton
import com.watchvault.ui.common.PrimaryButton
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Motion
import com.watchvault.ui.theme.Radius
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType
import java.util.Calendar

private const val MAX_PHOTOS = 8
private val CONDITION_OPTIONS = listOf("New", "Like New", "Excellent", "Good", "Fair", "Needs Service")
private val MOVEMENT_OPTIONS = listOf("Automatic", "Manual", "Quartz", "Solar", "Spring Drive", "Other")

private fun movementHelperText(movement: String): Pair<String, String>? = when (movement) {
    "Automatic" -> "WRIST-POWERED" to "The movement winds automatically as the watch is worn."
    "Manual" -> "HAND-WOUND" to "This movement must be wound by hand to keep running."
    "Quartz" -> "BATTERY-POWERED" to "A quartz crystal regulates timekeeping electronically."
    "Solar" -> "LIGHT-POWERED" to "Ambient light recharges the movement's power cell."
    "Spring Drive" -> "HYBRID MOVEMENT" to "Mechanical power regulated by a quartz-controlled brake."
    else -> null
}

/**
 * Add/Edit Watch as one continuous screen — curating a watch into the vault, not filling out a
 * form. Only Brand and Model are required; everything else can be filled in now or later. Photos,
 * identity, purchase, ownership, condition and movement are always visible; specifications, service,
 * documents and notes stay collapsed until the user actually wants them, per the same progressive-
 * disclosure principle used everywhere else in the app.
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
    var isFirstOwner by remember { mutableStateOf(false) }
    var box by remember { mutableStateOf(false) }
    var papers by remember { mutableStateOf(false) }

    var conditionRaw by remember { mutableStateOf("") }
    var movementRaw by remember { mutableStateOf("") }

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
    var saving by remember { mutableStateOf(false) }
    var conditionSheetOpen by remember { mutableStateOf(false) }
    var movementSheetOpen by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        viewModel.discardIfUnsaved(context)
        onBack()
    }

    LaunchedEffect(pendingSavedUuid) {
        val savedUuid = pendingSavedUuid ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "Added to your collection",
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
            isFirstOwner = existing.isFirstOwner ?: false
            box = existing.box ?: false
            papers = existing.papers ?: false
            conditionRaw = existing.conditionRaw.orEmpty()
            movementRaw = existing.movementRaw.orEmpty()
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

    val canSave = brand.isNotBlank() && model.isNotBlank()
    val save: () -> Unit = {
        if (canSave && !saving) {
            saving = true
            viewModel.save(buildWatch(), context) { savedUuid -> pendingSavedUuid = savedUuid }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconActionButton(Icons.Filled.ChevronLeft, contentDescription = "Back", onClick = handleBack)
                Text("ADD WATCH", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = Spacing.xs))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.screenH)) {
                PrimaryButton(
                    text = "Save Watch",
                    onClick = save,
                    enabled = canSave,
                    loading = saving,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            PhotosSection(
                photos = photos,
                onAdd = { uris -> viewModel.addPhotos(context, uris.take(MAX_PHOTOS - photos.size)) },
                onSetPrimary = viewModel::setPrimary,
                onRemove = viewModel::removePhoto,
                onMove = viewModel::movePhoto
            )

            FormSection("IDENTITY") {
                VaultTextField(brand, { brand = it }, "Brand *")
                VaultTextField(model, { model = it }, "Model *")
                VaultTextField(referenceNumber, { referenceNumber = it }, "Reference Number")
            }

            FormSection("PURCHASE") {
                DateField("Purchase Date", purchaseDate, { purchaseDate = it })
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(purchasePrice, { purchasePrice = it }, "Purchase Price", numeric = true, unit = purchaseCurrency, modifier = Modifier.weight(1f))
                    VaultTextField(estimatedValue, { estimatedValue = it }, "Estimated Value", numeric = true, unit = estimatedValueCurrency, modifier = Modifier.weight(1f))
                }
            }

            FormSection("OWNERSHIP") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    OwnershipSwitch("First Owner", isFirstOwner, { isFirstOwner = it }, Modifier.weight(1f))
                    OwnershipSwitch("Box & Papers", box && papers, { box = it; papers = it }, Modifier.weight(1f))
                }
            }

            SelectorField("CONDITION", conditionRaw.ifBlank { null }, onClick = { conditionSheetOpen = true })

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SelectorField("MOVEMENT", movementRaw.ifBlank { null }, onClick = { movementSheetOpen = true })
                movementHelperText(movementRaw)?.let { (title, body) -> MovementHelper(title, body) }
            }

            CollapsibleSection("ADVANCED DETAILS") {
                VaultTextField(nickname, { nickname = it }, "Nickname")
                VaultTextField(watchType, { watchType = it }, "Watch Type")
                VaultTextField(serialNumber, { serialNumber = it }, "Serial Number")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(caseDiameterMm, { caseDiameterMm = it }, "Diameter", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
                    VaultTextField(caseThicknessMm, { caseThicknessMm = it }, "Thickness", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
                }
                VaultTextField(caseMaterial, { caseMaterial = it }, "Case Material")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(caseColour, { caseColour = it }, "Case Colour", modifier = Modifier.weight(1f))
                    VaultTextField(caseShape, { caseShape = it }, "Case Shape", modifier = Modifier.weight(1f))
                }
                VaultTextField(crystal, { crystal = it }, "Crystal")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(waterResistance, { waterResistance = it }, "Water Resistance", modifier = Modifier.weight(1f))
                    VaultTextField(lugWidthMm, { lugWidthMm = it }, "Lug Width", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(dialColour, { dialColour = it }, "Dial Colour", modifier = Modifier.weight(1f))
                    VaultTextField(dialType, { dialType = it }, "Dial Type", modifier = Modifier.weight(1f))
                }
                VaultTextField(strap, { strap = it }, "Bracelet / Strap")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(strapMaterial, { strapMaterial = it }, "Strap Material", modifier = Modifier.weight(1f))
                    VaultTextField(strapColour, { strapColour = it }, "Strap Colour", modifier = Modifier.weight(1f))
                }
                VaultTextField(caliber, { caliber = it }, "Caliber")
                VaultTextField(powerReserve, { powerReserve = it }, "Power Reserve")
                VaultTextField(complications, { complications = it }, "Complications")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    VaultTextField(batteryType, { batteryType = it }, "Battery Type", modifier = Modifier.weight(1f))
                    VaultTextField(batteryLife, { batteryLife = it }, "Battery Life", modifier = Modifier.weight(1f))
                }
                VaultTextField(seller, { seller = it }, "Seller")
                VaultTextField(purchaseLocation, { purchaseLocation = it }, "Purchase Location")
                VaultTextField(invoiceNumber, { invoiceNumber = it }, "Invoice Number")
                DateField("Warranty Expiry", warrantyExpiry, { warrantyExpiry = it })
                VaultTextField(purchaseCurrency, { purchaseCurrency = it }, "Purchase Currency")
                VaultTextField(estimatedValueCurrency, { estimatedValueCurrency = it }, "Estimated Value Currency")
            }

            CollapsibleSection("SERVICE HISTORY") {
                Text(
                    "Service records can be added from the watch's detail screen once it's saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CollapsibleSection("DOCUMENTS") {
                Text(
                    "Invoices, warranties and certificates aren't supported yet — this section is a placeholder, not live data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CollapsibleSection("NOTES") {
                VaultTextField(notes, { notes = it }, "My Notes", minLines = 4)
            }

            // Bottom padding so the last collapsible section clears the fixed bottom Save bar.
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }

    if (conditionSheetOpen) {
        SelectionBottomSheet(
            title = "CONDITION",
            options = CONDITION_OPTIONS,
            selected = conditionRaw,
            onSelect = { conditionRaw = it; conditionSheetOpen = false },
            onDismiss = { conditionSheetOpen = false }
        )
    }
    if (movementSheetOpen) {
        SelectionBottomSheet(
            title = "MOVEMENT",
            options = MOVEMENT_OPTIONS,
            selected = movementRaw,
            onSelect = { movementRaw = it; movementSheetOpen = false },
            onDismiss = { movementSheetOpen = false }
        )
    }
}

private fun formatMm(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()

// --- Sections ----------------------------------------------------------------------------------

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(title, style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Collapsed by default — this is what keeps the screen from overwhelming a quick add. Tapping
 *  the header expands in place; nothing here is required to save. */
@Composable
private fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) { content() }
        }
    }
}

@Composable
private fun PhotosSection(
    photos: List<WatchPhoto>,
    onAdd: (List<android.net.Uri>) -> Unit,
    onSetPrimary: (String) -> Unit,
    onRemove: (WatchPhoto) -> Unit,
    onMove: (WatchPhoto, Int) -> Unit
) {
    val vaultColors = LocalVaultColors.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }
    val launchPicker = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val hero = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()
    val rest = photos.filterNot { it.uuid == hero?.uuid }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (hero == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, vaultColors.border, RoundedCornerShape(28.dp))
                    .clickable(onClick = launchPicker),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text("Add Watch Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("Use a clear photo of your watch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(onClick = launchPicker)
            ) {
                WatchPhotoOrPlaceholder(photo = hero, modifier = Modifier.fillMaxSize())
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("GALLERY", style = WatchVaultExtraType.sectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${photos.size}/$MAX_PHOTOS", style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                rest.forEach { photo ->
                    GalleryThumbnail(
                        photo = photo,
                        onSetPrimary = { onSetPrimary(photo.uuid) },
                        onRemove = { onRemove(photo) }
                    )
                }
                if (photos.size < MAX_PHOTOS) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, vaultColors.border, RoundedCornerShape(18.dp))
                            .clickable(onClick = launchPicker),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add more photos", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (rest.isNotEmpty()) {
                Text(
                    "Tap the star on a photo to make it the cover of this watch.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GalleryThumbnail(photo: WatchPhoto, onSetPrimary: () -> Unit, onRemove: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Box(modifier = Modifier.size(88.dp)) {
        WatchPhotoOrPlaceholder(
            photo = photo,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
        )
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onSetPrimary)
        ) {
            Icon(Icons.Filled.Star, contentDescription = "Set as cover photo", tint = Color.White, modifier = Modifier.padding(4.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onRemove)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun OwnershipSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SelectorField(label: String, value: String?, onClick: () -> Unit) {
    val vaultColors = LocalVaultColors.current
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(label, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xxs)) {
            Text(
                value ?: "Not set",
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).height(1.dp).background(vaultColors.border))
    }
}

@Composable
private fun MovementHelper(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        Text(title, style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBottomSheet(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).padding(bottom = Spacing.xl)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = Spacing.sm))
            options.forEach { option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(horizontal = Spacing.md, vertical = Spacing.md)
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- Shared field controls --------------------------------------------------------------------

@Composable
private fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    numeric: Boolean = false,
    minLines: Int = 1
) {
    val vaultColors = LocalVaultColors.current
    var focused by remember { mutableStateOf(false) }
    val underlineColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else vaultColors.border,
        tween(Motion.quick),
        label = "fieldUnderline"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = WatchVaultExtraType.metadata,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = Spacing.xxs)) {
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                    minLines = minLines,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).height(1.dp).background(underlineColor))
    }
}

@Composable
private fun DateField(label: String, valueMillis: Long?, onChange: (Long?) -> Unit) {
    val context = LocalContext.current
    val vaultColors = LocalVaultColors.current

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

    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = openPicker)) {
        Text(label.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xxs)) {
            Text(
                valueMillis?.let { formatDate(it) } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = if (valueMillis != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
            if (valueMillis != null) {
                Text(
                    "Clear",
                    style = WatchVaultExtraType.metadata,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onChange(null) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).height(1.dp).background(vaultColors.border))
    }
}
