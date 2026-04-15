package dev.parez.sidekick.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.parez.sidekick.demo.PokemonDetail
import dev.parez.sidekick.demo.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: PokemonDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class PokemonDetailViewModel(
    private val id: Int,
    private val repository: PokemonRepository,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeDetail(id),
        error,
    ) { detail, err ->
        when {
            err != null && detail == null -> DetailUiState.Error(err)
            detail != null -> DetailUiState.Content(detail)
            else -> DetailUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    init {
        fetchDetail()
    }

    fun fetchDetail() {
        Logger.i("DetailVM") { "fetchDetail: id=$id" }
        viewModelScope.launch {
            runCatching { repository.fetchDetail(id) }
                .onSuccess { Logger.d("DetailVM") { "fetchDetail: success for id=$id" } }
                .onFailure {
                    Logger.e("DetailVM", it) { "fetchDetail failed for id=$id" }
                    error.value = it.message ?: "Unknown error"
                }
        }
    }
}
