package com.watchvault.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One maintenance record plus the (denormalized-for-display) identity of the watch it belongs
 *  to, so the Activity timeline can render "Brand Model" without a second lookup per row. */
data class MaintenanceActivityEntry(
    val watchUuid: String,
    val brand: String,
    val model: String,
    val record: MaintenanceRecord
)

/** One relative-date group in the Activity timeline, e.g. "Today", "Yesterday", or a formatted
 *  date for anything older. Presentation-only grouping of [MaintenanceActivityEntry] already
 *  loaded above — no new query. */
data class ActivityGroup(
    val label: String,
    val entries: List<MaintenanceActivityEntry>
)

/** Buckets a millis timestamp into "Today"/"Yesterday"/"d MMM yyyy", ignoring time-of-day. */
private fun relativeDateLabel(epochMillis: Long, now: Long): String {
    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val dayMillis = 24L * 60 * 60 * 1000
    val diffDays = (startOfDay(now) - startOfDay(epochMillis)) / dayMillis
    return when (diffDays) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }
}

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

    /** [entries] grouped into a Today/Yesterday/date timeline for presentation — same underlying
     *  data as [entries], just bucketed for display. */
    val groups: StateFlow<List<ActivityGroup>> = entries
        .map { list ->
            val now = System.currentTimeMillis()
            list.groupBy { relativeDateLabel(it.record.date, now) }
                .toList()
                // Preserve the already-descending-by-date order of [entries]: the first entry seen
                // for each label determines that group's position in the output.
                .sortedByDescending { (_, groupEntries) -> groupEntries.first().record.date }
                .map { (label, groupEntries) -> ActivityGroup(label, groupEntries) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
