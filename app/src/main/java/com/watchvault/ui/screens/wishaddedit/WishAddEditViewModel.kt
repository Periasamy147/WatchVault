package com.watchvault.ui.screens.wishaddedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.repository.WishlistRepository
import com.watchvault.data.urlimport.ExtractedProductData
import com.watchvault.data.urlimport.UrlImportPipeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UrlImportState {
    data object Idle : UrlImportState
    data object Loading : UrlImportState
    data class Preview(val data: ExtractedProductData) : UrlImportState
    // [message] is always a plain, user-facing sentence — never a raw exception message, stack
    // trace, or HTTP status string. [technicalDetail] keeps the underlying cause for logging /
    // future debugging only; it must never be rendered in the UI.
    data class Error(val message: String, val technicalDetail: String? = null) : UrlImportState
}

class WishAddEditViewModel(
    private val repository: WishlistRepository,
    private val urlImportPipeline: UrlImportPipeline,
    private val existingUuid: String?
) : ViewModel() {

    private val _urlImportState = MutableStateFlow<UrlImportState>(UrlImportState.Idle)
    val urlImportState: StateFlow<UrlImportState> = _urlImportState.asStateFlow()

    suspend fun load(): WishlistItem? = existingUuid?.let { repository.getByUuid(it) }

    fun fetchFromUrl(url: String) {
        viewModelScope.launch {
            _urlImportState.value = UrlImportState.Loading
            _urlImportState.value = try {
                UrlImportState.Preview(urlImportPipeline.run(url))
            } catch (e: Exception) {
                // The underlying cause (network error, non-200 status, parse failure, etc.) is
                // kept only for logging — the user always sees a plain, actionable sentence.
                android.util.Log.e("WishAddEditViewModel", "URL import failed for $url", e)
                UrlImportState.Error(
                    message = "Couldn't retrieve product details — try again or enter details manually.",
                    technicalDetail = e.message
                )
            }
        }
    }

    fun clearUrlImport() { _urlImportState.value = UrlImportState.Idle }

    fun save(item: WishlistItem, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val isNew = existingUuid == null
            val toSave = if (isNew) item.copy(uuid = UUID.randomUUID().toString()) else item
            repository.upsert(toSave, isNew)
            onSaved(toSave.uuid)
        }
    }
}
