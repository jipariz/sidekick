package dev.parez.sidekick.database

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What the Database panel currently knows.
 *
 * Unlike the network and log monitors this store owns no database of its own — it is a view onto
 * the consumer's. There is nothing to persist and nothing to page: a snapshot is at most
 * [ROW_LIMIT] rows per table, re-read on demand.
 */
public class DatabaseInspectorStore {

    private val _state = MutableStateFlow(DatabaseInspectorState())
    public val state: StateFlow<DatabaseInspectorState> = _state.asStateFlow()

    /**
     * Binds a live database. Called by `:plugins:database-inspector:room`'s
     * `DatabaseInspector.attach`, or by any consumer supplying their own [DatabaseController].
     */
    public fun attach(controller: DatabaseController) {
        _state.update { it.copy(controller = controller, support = DbSupport.Available) }
        controller.refresh()
    }

    /** Records that inspection cannot work here — see [DbSupport.Unsupported]. */
    public fun markUnsupported(reason: String) {
        _state.update { it.copy(support = DbSupport.Unsupported(reason), controller = null) }
    }

    /** Replaces the snapshot. Called by the controller after a read. */
    public fun publish(info: DbInfo, tables: List<DbTable>) {
        _state.update { it.copy(info = info, tables = tables, refreshing = false, error = null) }
    }

    public fun setRefreshing(refreshing: Boolean) {
        _state.update { it.copy(refreshing = refreshing) }
    }

    /** Surfaces a failure from a read, a cell write, or a query. */
    public fun publishError(message: String) {
        _state.update { it.copy(refreshing = false, error = message) }
    }

    /** Controllers set their own refreshing flag, so this only dispatches. */
    public fun refresh() {
        _state.value.controller?.refresh()
    }
}

/** Whether database inspection is usable on this platform and in this app. */
public sealed interface DbSupport {
    /** No database attached yet. */
    public data object Detached : DbSupport

    public data object Available : DbSupport

    /** Inspection cannot work here; [reason] is shown to the developer. */
    public data class Unsupported(val reason: String) : DbSupport
}

/**
 * Emitted as a whole new instance on every change — nothing here is mutated in place — so the panel
 * that receives it can be skipped when it has not changed.
 */
@Immutable
public data class DatabaseInspectorState(
    val support: DbSupport = DbSupport.Detached,
    val info: DbInfo? = null,
    val tables: List<DbTable> = emptyList(),
    val controller: DatabaseController? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
)
