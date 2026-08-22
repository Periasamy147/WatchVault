package com.watchvault.ui.screens.watchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.Watch
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchDetailViewModel(
    private val repository: WatchRepository,
    watchUuid: String
) : ViewModel() {

    val watch: StateFlow<WatchWithDetails?> = repository.observeWithDetails(watchUuid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(watch: Watch) = viewModelScope.launch { repository.delete(watch) }
}
