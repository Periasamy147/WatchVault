package com.watchvault.ui.screens.addedit

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.common.Capsule
import com.watchvault.ui.common.CapsuleVariant
import com.watchvault.ui.common.IconActionButton
import com.watchvault.ui.common.PrimaryButton
import com.watchvault.ui.common.TertiaryButton
import com.watchvault.ui.common.WatchPhotoOrPlaceholder
import com.watchvault.ui.common.formatDate
import com.watchvault.ui.common.formatMoney
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Motion
import com.watchvault.ui.theme.Radius
import com.watchvault.ui.theme.Spacing
import com.watchvault.ui.theme.WatchVaultExtraType
import java.util.Calendar

private enum class WizardStep(val title: String) {
    PHOTOS("Photos"), IDENTITY("Identity"), SPECIFICATIONS("Specifications"),
    OWNERSHIP("Ownership"), CONDITION("Condition"), REVIEW("Review")
}

/**
 * Add/Edit Watch as a real step wizard rather than one long scrolling form — cataloguing a watch
 * should feel like the emotional act of adding it to a collection, not filling out a database
 * record. Photos come first on purpose. Only Brand/Model (on the Identity step) are required;
 * "Save" in the top bar lets someone bail out early with just those two plus whatever photos
 * they've added, same as before — the wizard doesn't remove that shortcut, it just gives the
 * full path somewhere better to live than a wall of collapsible cards.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    var saving by remember { mutableStateOf(false) }
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = WizardStep.values()
    val step = steps[stepIndex]

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

    val canSave = brand.isNotBlank() && model.isNotBlank()
    val save: () -> Unit = {
        if (canSave && !saving) {
            saving = true
            viewModel.save(buildWatch(), context) { savedUuid -> pendingSavedUuid = savedUuid }
        }
    }

    Scaffold(
        topBar = {
            WizardTopBar(
                step = step,
                stepIndex = stepIndex,
                stepCount = steps.size,
                isEditing = watchUuid != null,
                canSave = canSave,
                saving = saving,
                onBack = handleBack,
                onSave = save
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(tween(Motion.standard)) togetherWith fadeOut(tween(Motion.quick))
                    },
                    label = "wizardStep"
                ) { current ->
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.screenH),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        when (current) {
                            WizardStep.PHOTOS -> PhotosStep(
                                photos = photos,
                                onAdd = { uris -> viewModel.addPhotos(context, uris) },
                                onSetPrimary = viewModel::setPrimary,
                                onRemove = viewModel::removePhoto,
                                onMove = viewModel::movePhoto
                            )
                            WizardStep.IDENTITY -> IdentityStep(
                                brand = brand, onBrand = { brand = it },
                                model = model, onModel = { model = it },
                                nickname = nickname, onNickname = { nickname = it },
                                referenceNumber = referenceNumber, onReferenceNumber = { referenceNumber = it },
                                watchType = watchType, onWatchType = { watchType = it },
                                serialNumber = serialNumber, onSerialNumber = { serialNumber = it }
                            )
                            WizardStep.SPECIFICATIONS -> SpecificationsStep(
                                movementRaw, { movementRaw = it }, caliber, { caliber = it },
                                powerReserve, { powerReserve = it }, complications, { complications = it },
                                caseDiameterMm, { caseDiameterMm = it }, caseThicknessMm, { caseThicknessMm = it },
                                caseMaterial, { caseMaterial = it }, caseColour, { caseColour = it }, caseShape, { caseShape = it },
                                crystal, { crystal = it }, waterResistance, { waterResistance = it }, lugWidthMm, { lugWidthMm = it },
                                dialColour, { dialColour = it }, dialType, { dialType = it },
                                strap, { strap = it }, strapMaterial, { strapMaterial = it }, strapColour, { strapColour = it },
                                batteryType, { batteryType = it }, batteryLife, { batteryLife = it }
                            )
                            WizardStep.OWNERSHIP -> OwnershipStep(
                                estimatedValue, { estimatedValue = it }, estimatedValueCurrency, { estimatedValueCurrency = it },
                                purchaseDate, { purchaseDate = it }, purchasePrice, { purchasePrice = it }, purchaseCurrency, { purchaseCurrency = it },
                                seller, { seller = it }, purchaseLocation, { purchaseLocation = it }, invoiceNumber, { invoiceNumber = it },
                                warrantyExpiry, { warrantyExpiry = it }, isFirstOwner, { isFirstOwner = it }, box, { box = it }, papers, { papers = it }
                            )
                            WizardStep.CONDITION -> ConditionStep(
                                conditionRaw = conditionRaw, onConditionRaw = { conditionRaw = it },
                                notes = notes, onNotes = { notes = it }
                            )
                            WizardStep.REVIEW -> ReviewStep(
                                brand = brand, model = model, referenceNumber = referenceNumber,
                                photo = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull(),
                                estimatedValue = estimatedValue.toDoubleOrNull(), estimatedValueCurrency = estimatedValueCurrency,
                                quickFacts = listOfNotNull(
                                    movementRaw.ifBlank { null },
                                    caseDiameterMm.toDoubleOrNull()?.let { "${formatMm(it)}mm" },
                                    dialColour.ifBlank { null },
                                    caseMaterial.ifBlank { null }
                                )
                            )
                        }
                    }
                }
            }
            WizardFooter(
                step = step,
                stepIndex = stepIndex,
                stepCount = steps.size,
                canAdvance = if (step == WizardStep.IDENTITY) canSave else true,
                canSave = canSave,
                saving = saving,
                onBack = { if (stepIndex > 0) stepIndex-- },
                onNext = { if (stepIndex < steps.lastIndex) stepIndex++ },
                onFinish = save
            )
        }
    }
}

@Composable
private fun WizardTopBar(
    step: WizardStep,
    stepIndex: Int,
    stepCount: Int,
    isEditing: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val vaultColors = LocalVaultColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconActionButton(Icons.Filled.Close, contentDescription = "Close", onClick = onBack)
            Text(
                if (isEditing) "Edit Watch" else "Add to Vault",
                style = MaterialTheme.typography.titleMedium
            )
            TertiaryButton(text = "Save", onClick = onSave, enabled = canSave && !saving)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            repeat(stepCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index <= stepIndex) vaultColors.gold else vaultColors.border)
                )
            }
        }
        Text(
            step.title.uppercase(),
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.screenH, vertical = Spacing.xxs)
        )
    }
}

@Composable
private fun WizardFooter(
    step: WizardStep,
    stepIndex: Int,
    stepCount: Int,
    canAdvance: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.screenH),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (stepIndex > 0) {
            TertiaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
        }
        if (stepIndex < stepCount - 1) {
            PrimaryButton(text = "Continue", onClick = onNext, enabled = canAdvance, modifier = Modifier.weight(2f))
        } else {
            PrimaryButton(text = "Add to Collection", onClick = onFinish, enabled = canSave, loading = saving, modifier = Modifier.weight(2f))
        }
    }
}

private fun formatMm(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()

// --- Step content -----------------------------------------------------------------------------

@Composable
private fun StepIntro(headline: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(headline, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PhotosStep(
    photos: List<WatchPhoto>,
    onAdd: (List<android.net.Uri>) -> Unit,
    onSetPrimary: (String) -> Unit,
    onRemove: (WatchPhoto) -> Unit,
    onMove: (WatchPhoto, Int) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }

    StepIntro("Start with a photo.", "The watch that means the most to you deserves to lead your vault.")

    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, LocalVaultColors.current.border, RoundedCornerShape(Radius.card))
                .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = LocalVaultColors.current.gold)
                Text("Add photos", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                Text("From your camera or gallery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(Radius.card))
                    .border(1.dp, LocalVaultColors.current.border, RoundedCornerShape(Radius.card))
                    .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Add more photos", tint = LocalVaultColors.current.gold)
            }
        }
        Text(
            "The starred photo is this watch's cover everywhere in your vault.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    Column(modifier = Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            WatchPhotoOrPlaceholder(
                photo = photo,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.thumbnail))
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
            IconActionButton(Icons.Filled.ChevronLeft, contentDescription = "Move left", onClick = onMoveLeft, modifier = Modifier.size(28.dp))
            Text(
                "Remove",
                style = MaterialTheme.typography.labelSmall,
                color = vaultColors.danger,
                modifier = Modifier.clickable(onClick = onRemove).padding(top = 6.dp)
            )
            IconActionButton(Icons.Filled.ChevronRight, contentDescription = "Move right", onClick = onMoveRight, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun IdentityStep(
    brand: String, onBrand: (String) -> Unit,
    model: String, onModel: (String) -> Unit,
    nickname: String, onNickname: (String) -> Unit,
    referenceNumber: String, onReferenceNumber: (String) -> Unit,
    watchType: String, onWatchType: (String) -> Unit,
    serialNumber: String, onSerialNumber: (String) -> Unit
) {
    StepIntro("What is it?", "Brand and model are all you need to keep going.")
    VaultTextField(brand, onBrand, "Brand *")
    VaultTextField(model, onModel, "Model *")
    VaultTextField(nickname, onNickname, "Nickname")
    VaultTextField(referenceNumber, onReferenceNumber, "Reference number")
    VaultTextField(watchType, onWatchType, "Watch type")
    VaultTextField(serialNumber, onSerialNumber, "Serial number")
}

@Composable
private fun SpecificationsStep(
    movementRaw: String, onMovementRaw: (String) -> Unit,
    caliber: String, onCaliber: (String) -> Unit,
    powerReserve: String, onPowerReserve: (String) -> Unit,
    complications: String, onComplications: (String) -> Unit,
    caseDiameterMm: String, onCaseDiameterMm: (String) -> Unit,
    caseThicknessMm: String, onCaseThicknessMm: (String) -> Unit,
    caseMaterial: String, onCaseMaterial: (String) -> Unit,
    caseColour: String, onCaseColour: (String) -> Unit,
    caseShape: String, onCaseShape: (String) -> Unit,
    crystal: String, onCrystal: (String) -> Unit,
    waterResistance: String, onWaterResistance: (String) -> Unit,
    lugWidthMm: String, onLugWidthMm: (String) -> Unit,
    dialColour: String, onDialColour: (String) -> Unit,
    dialType: String, onDialType: (String) -> Unit,
    strap: String, onStrap: (String) -> Unit,
    strapMaterial: String, onStrapMaterial: (String) -> Unit,
    strapColour: String, onStrapColour: (String) -> Unit,
    batteryType: String, onBatteryType: (String) -> Unit,
    batteryLife: String, onBatteryLife: (String) -> Unit
) {
    StepIntro("The details.", "Everything here is optional — add what you know.")

    SpecGroupLabel("Movement")
    VaultTextField(movementRaw, onMovementRaw, "Movement")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(caliber, onCaliber, "Caliber", modifier = Modifier.weight(1f))
        VaultTextField(powerReserve, onPowerReserve, "Power reserve", modifier = Modifier.weight(1f))
    }
    VaultTextField(complications, onComplications, "Complications")

    SpecGroupLabel("Case")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(caseDiameterMm, onCaseDiameterMm, "Diameter", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
        VaultTextField(caseThicknessMm, onCaseThicknessMm, "Thickness", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
    }
    VaultTextField(caseMaterial, onCaseMaterial, "Material")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(caseColour, onCaseColour, "Colour", modifier = Modifier.weight(1f))
        VaultTextField(caseShape, onCaseShape, "Shape", modifier = Modifier.weight(1f))
    }
    VaultTextField(crystal, onCrystal, "Crystal")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(waterResistance, onWaterResistance, "Water resistance", modifier = Modifier.weight(1f))
        VaultTextField(lugWidthMm, onLugWidthMm, "Lug width", unit = "mm", numeric = true, modifier = Modifier.weight(1f))
    }

    SpecGroupLabel("Dial")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(dialColour, onDialColour, "Colour", modifier = Modifier.weight(1f))
        VaultTextField(dialType, onDialType, "Type", modifier = Modifier.weight(1f))
    }

    SpecGroupLabel("Bracelet / Strap")
    VaultTextField(strap, onStrap, "Type")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(strapMaterial, onStrapMaterial, "Material", modifier = Modifier.weight(1f))
        VaultTextField(strapColour, onStrapColour, "Colour", modifier = Modifier.weight(1f))
    }

    SpecGroupLabel("Battery")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(batteryType, onBatteryType, "Type", modifier = Modifier.weight(1f))
        VaultTextField(batteryLife, onBatteryLife, "Life", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpecGroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = WatchVaultExtraType.metadata,
        color = LocalVaultColors.current.gold,
        modifier = Modifier.padding(top = Spacing.sm)
    )
}

@Composable
private fun OwnershipStep(
    estimatedValue: String, onEstimatedValue: (String) -> Unit,
    estimatedValueCurrency: String, onEstimatedValueCurrency: (String) -> Unit,
    purchaseDate: Long?, onPurchaseDate: (Long?) -> Unit,
    purchasePrice: String, onPurchasePrice: (String) -> Unit,
    purchaseCurrency: String, onPurchaseCurrency: (String) -> Unit,
    seller: String, onSeller: (String) -> Unit,
    purchaseLocation: String, onPurchaseLocation: (String) -> Unit,
    invoiceNumber: String, onInvoiceNumber: (String) -> Unit,
    warrantyExpiry: Long?, onWarrantyExpiry: (Long?) -> Unit,
    isFirstOwner: Boolean?, onIsFirstOwner: (Boolean?) -> Unit,
    box: Boolean?, onBox: (Boolean?) -> Unit,
    papers: Boolean?, onPapers: (Boolean?) -> Unit
) {
    StepIntro("Its story so far.", "How you came to own it, and what it's worth.")

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(estimatedValue, onEstimatedValue, "Estimated value", numeric = true, modifier = Modifier.weight(1f))
        VaultTextField(estimatedValueCurrency, onEstimatedValueCurrency, "Currency", modifier = Modifier.width(96.dp))
    }
    DateField("Purchase date", purchaseDate, onPurchaseDate)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        VaultTextField(purchasePrice, onPurchasePrice, "Purchase price", numeric = true, modifier = Modifier.weight(1f))
        VaultTextField(purchaseCurrency, onPurchaseCurrency, "Currency", modifier = Modifier.width(96.dp))
    }
    VaultTextField(seller, onSeller, "Seller")
    VaultTextField(purchaseLocation, onPurchaseLocation, "Purchase location")
    VaultTextField(invoiceNumber, onInvoiceNumber, "Invoice number")
    DateField("Warranty expiry", warrantyExpiry, onWarrantyExpiry)
    TriStateRow("First owner", isFirstOwner, onIsFirstOwner)
    TriStateRow("Box", box, onBox)
    TriStateRow("Papers", papers, onPapers)
}

@Composable
private fun ConditionStep(conditionRaw: String, onConditionRaw: (String) -> Unit, notes: String, onNotes: (String) -> Unit) {
    StepIntro("Condition & notes.", "How it wears today, and anything personal worth remembering.")
    VaultTextField(conditionRaw, onConditionRaw, "Condition")
    VaultTextField(notes, onNotes, "Notes", minLines = 4)
}

@Composable
private fun ReviewStep(
    brand: String,
    model: String,
    referenceNumber: String,
    photo: WatchPhoto?,
    estimatedValue: Double?,
    estimatedValueCurrency: String,
    quickFacts: List<String>
) {
    val vaultColors = LocalVaultColors.current
    StepIntro("Ready to add.", "Take a look before it joins your vault.")
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(Radius.card))
    ) {
        WatchPhotoOrPlaceholder(photo = photo, modifier = Modifier.fillMaxSize())
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (brand.isNotBlank()) Text(brand.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(model.ifBlank { "Untitled watch" }, style = MaterialTheme.typography.headlineSmall)
        if (referenceNumber.isNotBlank()) {
            Text("Ref. $referenceNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (estimatedValue != null) {
            Text(formatMoney(estimatedValue, estimatedValueCurrency), style = MaterialTheme.typography.titleMedium, color = vaultColors.gold)
        }
    }
    if (quickFacts.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            quickFacts.forEach { Capsule(it, variant = CapsuleVariant.NEUTRAL) }
        }
    }
}

// --- Shared field controls --------------------------------------------------------------------

/**
 * The one text-field treatment across the wizard: an uppercase label, the value in real body
 * type, and a hairline underline instead of a boxed Material field — a page in a journal, not a
 * database form. The underline turns gold on focus; that's the only affordance needed to show
 * where the cursor is.
 */
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
        if (focused) vaultColors.gold else vaultColors.border,
        tween(Motion.quick),
        label = "fieldUnderline"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = WatchVaultExtraType.metadata,
            color = if (focused) vaultColors.gold else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = Spacing.xxs)) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(vaultColors.gold),
                    keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                    minLines = minLines,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                )
            }
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs)
                .height(1.dp)
                .background(underlineColor)
        )
    }
}

@Composable
private fun TriStateRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Capsule("Yes", variant = if (value == true) CapsuleVariant.SELECTED else CapsuleVariant.OUTLINED, onClick = { onChange(true) })
            Capsule("No", variant = if (value == false) CapsuleVariant.SELECTED else CapsuleVariant.OUTLINED, onClick = { onChange(false) })
            Capsule("Unknown", variant = if (value == null) CapsuleVariant.SELECTED else CapsuleVariant.OUTLINED, onClick = { onChange(null) })
        }
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
                    color = vaultColors.gold,
                    modifier = Modifier.clickable { onChange(null) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).height(1.dp).background(vaultColors.border))
    }
}
