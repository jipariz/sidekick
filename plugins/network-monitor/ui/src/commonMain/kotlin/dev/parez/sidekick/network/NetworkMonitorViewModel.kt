package dev.parez.sidekick.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class NetworkMonitorViewModel(
    private val store: NetworkMonitorStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _methodFilter = MutableStateFlow<Set<String>>(emptySet())
    val methodFilter: StateFlow<Set<String>> = _methodFilter.asStateFlow()

    private val filterFlow: Flow<NetworkFilter> = combine(
        _query.debounce(150L),
        _methodFilter,
    ) { q, methods -> NetworkFilter(query = q, methods = methods) }
        .distinctUntilChanged()

    val pagedCalls: Flow<PagingData<NetworkCall>> = store.pagedCalls(filterFlow)
        .cachedIn(viewModelScope)

    val filteredCount: StateFlow<Long> = store.filteredCount(filterFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedCall: StateFlow<NetworkCall?> = _selectedId
        .flatMapLatest { id -> if (id == null) flowOf(null) else store.callById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: String?) {
        _selectedId.value = id
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggleMethod(method: String) {
        _methodFilter.update { current ->
            if (method in current) current - method else current + method
        }
    }

    fun clear() {
        viewModelScope.launch {
            store.clear()
            _selectedId.value = null
        }
    }
}
