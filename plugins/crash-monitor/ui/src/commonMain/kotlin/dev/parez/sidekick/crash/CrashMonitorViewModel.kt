package dev.parez.sidekick.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class CrashMonitorViewModel(private val store: CrashMonitorStore) : ViewModel() {

    val crashes: StateFlow<List<CrashRecord>> = store.crashes

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    val selected: StateFlow<CrashRecord?> =
        combine(store.crashes, _selectedId) { list, id -> list.firstOrNull { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: String?) {
        _selectedId.value = id
    }

    fun clear() {
        store.clear()
        _selectedId.value = null
    }
}
