package dev.parez.sidekick.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DatabaseInspectorViewModel(private val store: DatabaseInspectorStore) : ViewModel() {

    val state: StateFlow<DatabaseInspectorState> = store.state

    private val _selectedTable = MutableStateFlow<String?>(null)
    val selectedTable: StateFlow<String?> = _selectedTable.asStateFlow()

    private val _sql = MutableStateFlow("")
    val sql: StateFlow<String> = _sql.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult: StateFlow<QueryResult?> = _queryResult.asStateFlow()

    fun select(table: String?) {
        _selectedTable.value = table
        // Leaving the query screen drops its result so returning to a table does
        // not show a stale projection.
        if (table != null) _queryResult.value = null
    }

    fun setSql(value: String) {
        _sql.value = value
    }

    fun refresh() {
        store.refresh()
    }

    fun updateCell(table: String, rowId: Long, column: String, value: String?) {
        state.value.controller?.updateCell(table, rowId, column, value)
    }

    fun runQuery() {
        val statement = _sql.value
        val controller = state.value.controller ?: return
        if (!isReadOnlySql(statement)) {
            _queryResult.value =
                QueryResult.Failed(
                    "Only SELECT, PRAGMA and EXPLAIN are allowed, and only one statement at a time."
                )
            return
        }
        viewModelScope.launch {
            _queryResult.value = QueryResult.Running
            _queryResult.value =
                runCatching { controller.query(statement) }
                    .fold(
                        onSuccess = { QueryResult.Succeeded(it) },
                        onFailure = { QueryResult.Failed(it.message ?: "Query failed") },
                    )
        }
    }
}

internal sealed interface QueryResult {
    data object Running : QueryResult

    data class Succeeded(val table: DbTable) : QueryResult

    data class Failed(val message: String) : QueryResult
}
