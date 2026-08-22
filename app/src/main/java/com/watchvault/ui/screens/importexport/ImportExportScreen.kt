package com.watchvault.ui.screens.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen() {
    val container = LocalAppContainer.current
    val viewModel: ImportExportViewModel = viewModel(
        factory = GenericViewModelFactory { ImportExportViewModel(container.backupRepository) }
    )
    val state by viewModel.state.collectAsState()

    val myInnosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importMyInnos(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.previewRestore(it) }
    }
    val backupFileName = "watch-vault-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.zip"
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Import / Export") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Import", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { myInnosLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Import MyInnos Watch Collection Backup")
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore Watch Vault Backup")
            }

            Text("Export", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { exportLauncher.launch(backupFileName) }, modifier = Modifier.fillMaxWidth()) {
                Text("Export Full Backup")
            }
        }
    }

    when (val current = state) {
        OperationState.Running -> AlertDialog(
            onDismissRequest = {}, confirmButton = {},
            title = { Text("Working…") }, text = { CircularProgressIndicator() }
        )
        is OperationState.MyInnosDone -> AlertDialog(
            onDismissRequest = viewModel::resetState,
            confirmButton = { TextButton(onClick = viewModel::resetState) { Text("OK") } },
            title = { Text("Import complete") },
            text = {
                Text(
                    "Imported ${current.result.watches.size} watch(es), " +
                        "${current.result.photos.size} photo reference(s), " +
                        "${current.result.maintenanceRecords.size} maintenance record(s). " +
                        (if (current.result.priceConflicts.isNotEmpty())
                            "\n\n${current.result.priceConflicts.size} conflicting external market value(s) were found and logged " +
                                "but NOT applied — the canonical value from the backup was kept."
                        else "")
                )
            }
        )
        is OperationState.RestorePreviewReady -> AlertDialog(
            onDismissRequest = viewModel::resetState,
            confirmButton = { TextButton(onClick = { viewModel.confirmRestore(current.uri) }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = viewModel::resetState) { Text("Cancel") } },
            title = { Text("Restore this backup?") },
            text = {
                Column {
                    Text("Device: ${current.preview.deviceName}")
                    Text("Watches: ${current.preview.watchCount}, Wishlist: ${current.preview.wishlistCount}")
                    Text("Checksum verified: ${if (current.preview.checksumsOk) "yes" else "NO — this file may be corrupt"}")
                }
            }
        )
        OperationState.RestoreDone -> AlertDialog(
            onDismissRequest = viewModel::resetState,
            confirmButton = { TextButton(onClick = viewModel::resetState) { Text("OK") } },
            title = { Text("Restore complete") }, text = { Text("Your backup has been restored.") }
        )
        OperationState.ExportDone -> AlertDialog(
            onDismissRequest = viewModel::resetState,
            confirmButton = { TextButton(onClick = viewModel::resetState) { Text("OK") } },
            title = { Text("Export complete") }, text = { Text("Your backup has been saved.") }
        )
        is OperationState.Failed -> AlertDialog(
            onDismissRequest = viewModel::resetState,
            confirmButton = { TextButton(onClick = viewModel::resetState) { Text("OK") } },
            title = { Text("Something went wrong") }, text = { Text(current.message) }
        )
        OperationState.Idle -> Unit
    }
}
