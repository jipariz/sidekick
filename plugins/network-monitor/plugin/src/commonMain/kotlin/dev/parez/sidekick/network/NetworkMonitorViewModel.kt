package dev.parez.sidekick.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class NetworkMonitorViewModel(
    private val store: NetworkMonitorStore,
) : ViewModel() {

    val calls: StateFlow<List<NetworkCall>> = store.calls.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    var selected by mutableStateOf<NetworkCall?>(null)
        private set

    fun select(call: NetworkCall?) {
        selected = call
    }

    fun clear() {
        viewModelScope.launch { store.clear() }
    }
}
