package dev.parez.sidekick.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.parez.sidekick.demo.PokemonListEntry
import dev.parez.sidekick.demo.PokemonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Content(
        val items: List<PokemonListEntry>,
        val filteredItems: List<PokemonListEntry>,
        val query: String,
        val isLoadingMore: Boolean,
        val hasMore: Boolean,
        val error: String?,
    ) : ListUiState

    data class Error(val message: String) : ListUiState
}

class PokemonListViewModel(
    private val repository: PokemonRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val isLoadingMore = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ListUiState> = combine(
        repository.observePokemonList(),
        query,
        isLoadingMore,
        error,
    ) { items, query, loading, err ->
        println("[ListVM] combine: items=${items.size}, query=$query, loading=$loading, err=$err")
        if (items.isEmpty() && loading && err == null) {
            ListUiState.Loading
        } else if (items.isEmpty() && err != null) {
            ListUiState.Error(err)
        } else {
            val filtered = if (query.isBlank()) items
            else items.filter { it.name.contains(query, ignoreCase = true) }
            ListUiState.Content(
                items = items,
                filteredItems = filtered,
                query = query,
                isLoadingMore = loading,
                hasMore = repository.hasMore,
                error = err,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState.Loading,
    )

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoadingMore.value) return
        isLoadingMore.value = true
        viewModelScope.launch {
            println("[ListVM] loadNextPage: starting")
            error.value = null
            try {
                repository.fetchNextPage(PAGE_SIZE)
                println("[ListVM] loadNextPage: success")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[ListVM] loadNextPage: error=${e.message}")
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoadingMore.value = false
                println("[ListVM] loadNextPage: done, isLoadingMore=false")
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun onRetry() {
        error.value = null
        loadNextPage()
    }
}