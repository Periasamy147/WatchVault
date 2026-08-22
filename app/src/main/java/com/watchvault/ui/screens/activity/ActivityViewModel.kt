package com.watchvault.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One maintenance record plus the (denormalized-for-display) identity of the watch it belongs
 *  to, so the Activity timeline can render "Brand Model" without a second lookup per row. */
data class MaintenanceActivityEntry(
    val watchUuid: String,
    val brand: String,
    val model: String,
    val record: MaintenanceRecord
)

/**
 * Backs the Activity tab: a real, working chronological timeline of maintenance records across
 * every owned watch. Deliberately scoped to maintenance only — wear logs, accuracy tracking and
 * price history don't exist in this schema yet and are explicitly deferred to a later phase.
 *
 * Built entirely from [WatchRepository.observeAllWithDetails], which already joins in
 * maintenanceRecords per watch via Room's @Relation — no new DAO query needed.
 */
class ActivityViewModel(watchRepository: WatchRepository) : ViewModel() {

    val entries: StateFlow<List<MaintenanceActivityEntry>> = watchRepository.observeAllWithDetails()
        .map { watches ->
            watches
                .flatMap { details ->
                    details.maintenanceRecords.map { record ->
                        MaintenanceActivityEntry(
                            watchUuid = details.watch.uuid,
                            brand = details.watch.brand,
                            model = details.watch.model,
                            record = record
                        )
                    }
                }
                .sortedByDescending { it.record.date }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
