package com.watchvault.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.relation.WatchWithDetails
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SortOption { UPDATED_DESC, BRAND_ASC, PURCHASE_DATE_DESC, VALUE_DESC }
enum class ViewLayout { LIST, GRID }

data class CollectionFilters(
    val query: String = "",
    val brand: String? = null,
    val movement: String? = null,
    val condition: String? = null,
    val sort: SortOption = SortOption.UPDATED_DESC,
    val layout: ViewLayout = ViewLayout.GRID
)

class CollectionViewModel(private val repository: WatchRepository) : ViewModel() {

    private val filtersFlow = MutableStateFlow(CollectionFilters())
    val filters: StateFlow<CollectionFilters> = filtersFlow

    val watches: StateFlow<List<WatchWithDetails>> = combine(
        repository.observeAllWithDetails(), filtersFlow
    ) { all, filters ->
        all.filter { details ->
            val w = details.watch
            val matchesQuery = filters.query.isBlank() ||
                listOf(w.brand, w.model, w.referenceNumber, w.serialNumber, w.notes)
                    .any { it?.contains(filters.query, ignoreCase = true) == true }
            val matchesBrand = filters.brand == null || w.brand == filters.brand
            val matchesMovement = filters.movement == null || w.movementNormalized == filters.movement || w.movementRaw == filters.movement
            val matchesCondition = filters.condition == null || w.conditionRaw == filters.condition
            matchesQuery && matchesBrand && matchesMovement && matchesCondition
        }.let { filtered ->
            when (filters.sort) {
                SortOption.UPDATED_DESC -> filtered.sortedByDescending { it.watch.updatedAt }
                SortOption.BRAND_ASC -> filtered.sortedBy { it.watch.brand }
                SortOption.PURCHASE_DATE_DESC -> filtered.sortedByDescending { it.watch.purchaseDate ?: 0L }
                SortOption.VALUE_DESC -> filtered.sortedByDescending { it.watch.estimatedValue ?: 0.0 }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWatches: StateFlow<List<WatchWithDetails>> = repository.observeAllWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(query: String) { filtersFlow.value = filtersFlow.value.copy(query = query) }
    fun setBrand(brand: String?) { filtersFlow.value = filtersFlow.value.copy(brand = brand) }
    fun setMovement(movement: String?) { filtersFlow.value = filtersFlow.value.copy(movement = movement) }
    fun setCondition(condition: String?) { filtersFlow.value = filtersFlow.value.copy(condition = condition) }
    fun setSort(sort: SortOption) { filtersFlow.value = filtersFlow.value.copy(sort = sort) }
    fun setLayout(layout: ViewLayout) { filtersFlow.value = filtersFlow.value.copy(layout = layout) }
}
