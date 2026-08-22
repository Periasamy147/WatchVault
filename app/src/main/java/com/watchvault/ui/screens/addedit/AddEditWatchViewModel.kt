package com.watchvault.ui.screens.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.Watch
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.launch
import java.util.UUID

class AddEditWatchViewModel(
    private val repository: WatchRepository,
    private val existingUuid: String?
) : ViewModel() {

    suspend fun load(): Watch? = existingUuid?.let { repository.getByUuid(it) }

    fun save(watch: Watch, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val isNew = existingUuid == null
            val toSave = if (isNew) watch.copy(uuid = UUID.randomUUID().toString()) else watch
            repository.upsert(toSave, isNew)
            onSaved(toSave.uuid)
        }
    }
}
