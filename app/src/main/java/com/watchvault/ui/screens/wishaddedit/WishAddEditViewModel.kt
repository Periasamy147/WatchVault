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

/** Classifies a failed URL fetch/import into a user-facing category so the Composable only ever
 *  renders based on this enum, never by inspecting exception types itself. */
enum class UrlFetchFailureCategory {
    /** Site returned 403/429 — refuses automated lookups. */
    BLOCKED,
    /** Site returned 404 — nothing at that address. */
    NOT_FOUND,
    /** Timeout / DNS / connection failure. */
    NETWORK,
    /** The pasted text isn't a usable http(s) URL at all — retrying won't help. */
    MALFORMED_URL,
    /** Anything else — parse failure, unexpected exception, etc. */
    UNKNOWN
}

sealed interface UrlImportState {
    data object Idle : UrlImportState
    data object Loading : UrlImportState
    data class Preview(val data: ExtractedProductData) : UrlImportState
    // [message] is always a plain, user-facing sentence — never a raw exception message, stack
    // trace, or HTTP status string. [technicalDetail] keeps the underlying cause for logging /
    // future debugging only; it must never be rendered in the UI.
    data class Error(
        val category: UrlFetchFailureCategory,
        val message: String,
        val technicalDetail: String? = null
    ) : UrlImportState
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
        if (!isPlausibleUrl(url)) {
            _urlImportState.value = UrlImportState.Error(
                category = UrlFetchFailureCategory.MALFORMED_URL,
                message = "That doesn't look like a valid product link.",
                technicalDetail = "rejected before fetch: '$url'"
            )
            return
        }
        viewModelScope.launch {
            _urlImportState.value = UrlImportState.Loading
            _urlImportState.value = try {
                UrlImportState.Preview(urlImportPipeline.run(url))
            } catch (e: Exception) {
                // The underlying cause (network error, non-200 status, parse failure, etc.) is
                // kept only for logging — the user always sees a plain, actionable sentence.
                android.util.Log.e("WishAddEditViewModel", "URL import failed for $url", e)
                classifyFailure(e)
            }
        }
    }

    private fun isPlausibleUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        return try {
            java.net.URI(trimmed).host != null
        } catch (e: Exception) {
            false
        }
    }

    /** Turns a raw fetch/parse exception into a user-facing [UrlImportState.Error] — the only
     *  place in this flow that inspects exception types/messages. Everything downstream (the
     *  Composable) renders purely off [UrlFetchFailureCategory]. */
    private fun classifyFailure(e: Exception): UrlImportState.Error {
        val detail = e.message
        val lower = detail?.lowercase().orEmpty()
        return when {
            e is java.net.MalformedURLException || e is IllegalArgumentException ->
                UrlImportState.Error(UrlFetchFailureCategory.MALFORMED_URL, "That doesn't look like a valid product link.", detail)
            lower.contains("http 403") || lower.contains("http 429") ->
                UrlImportState.Error(UrlFetchFailureCategory.BLOCKED, "That store doesn't allow automated product lookup.", detail)
            lower.contains("http 404") ->
                UrlImportState.Error(UrlFetchFailureCategory.NOT_FOUND, "We couldn't find that product page.", detail)
            e is java.net.SocketTimeoutException || e is java.net.UnknownHostException || e is java.net.ConnectException ->
                UrlImportState.Error(UrlFetchFailureCategory.NETWORK, "Couldn't reach that page — check your connection.", detail)
            else ->
                UrlImportState.Error(UrlFetchFailureCategory.UNKNOWN, "Couldn't retrieve product details — try again or enter details manually.", detail)
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
