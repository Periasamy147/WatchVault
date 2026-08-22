package com.watchvault.ui.screens.importexport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.migration.MyInnosImporter
import com.watchvault.data.repository.BackupRepository
import com.watchvault.data.repository.RestorePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OperationState {
    data object Idle : OperationState
    data object Running : OperationState
    data class MyInnosDone(val result: MyInnosImporter.ImportResult) : OperationState
    data class RestorePreviewReady(val uri: Uri, val preview: RestorePreview) : OperationState
    data object RestoreDone : OperationState
    data object ExportDone : OperationState
    data class Failed(val message: String) : OperationState
}

class ImportExportViewModel(private val repository: BackupRepository) : ViewModel() {

    private val _state = MutableStateFlow<OperationState>(OperationState.Idle)
    val state: StateFlow<OperationState> = _state.asStateFlow()

    fun resetState() { _state.value = OperationState.Idle }

    fun importMyInnos(uri: Uri) {
        viewModelScope.launch {
            _state.value = OperationState.Running
            repository.importMyInnosBackup(uri)
                .onSuccess { _state.value = OperationState.MyInnosDone(it) }
                .onFailure { _state.value = OperationState.Failed(it.message ?: "Import failed") }
        }
    }

    fun previewRestore(uri: Uri) {
        viewModelScope.launch {
            _state.value = OperationState.Running
            runCatching { repository.previewRestore(uri) }
                .onSuccess { _state.value = OperationState.RestorePreviewReady(uri, it) }
                .onFailure { _state.value = OperationState.Failed(it.message ?: "Could not read backup") }
        }
    }

    fun confirmRestore(uri: Uri) {
        viewModelScope.launch {
            _state.value = OperationState.Running
            repository.restoreBackup(uri)
                .onSuccess { _state.value = OperationState.RestoreDone }
                .onFailure { _state.value = OperationState.Failed(it.message ?: "Restore failed") }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = OperationState.Running
            runCatching { repository.exportBackup(uri) }
                .onSuccess { _state.value = OperationState.ExportDone }
                .onFailure { _state.value = OperationState.Failed(it.message ?: "Export failed") }
        }
    }
}
