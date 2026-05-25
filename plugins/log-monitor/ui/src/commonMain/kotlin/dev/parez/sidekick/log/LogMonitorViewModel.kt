package dev.parez.sidekick.log

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
internal class LogMonitorViewModel(private val store: LogMonitorStore) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _levelFilter = MutableStateFlow<Set<LogLevel>>(emptySet())
    val levelFilter: StateFlow<Set<LogLevel>> = _levelFilter.asStateFlow()

    private val filterFlow: Flow<LogFilter> =
        combine(_query.debounce(150L), _levelFilter) { q, levels ->
                LogFilter(query = q, levels = levels)
            }
            .distinctUntilChanged()

    val pagedEntries: Flow<PagingData<LogEntry>> =
        store.pagedEntries(filterFlow).cachedIn(viewModelScope)

    val filteredCount: StateFlow<Long> =
        store
            .filteredCount(filterFlow)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedEntry: StateFlow<LogEntry?> =
        _selectedId
            .flatMapLatest { id -> if (id == null) flowOf(null) else store.entryById(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: String?) {
        _selectedId.value = id
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggleLevel(level: LogLevel) {
        _levelFilter.update { current ->
            if (level in current) current - level else current + level
        }
    }

    fun clear() {
        viewModelScope.launch {
            store.clear()
            _selectedId.value = null
        }
    }
}
