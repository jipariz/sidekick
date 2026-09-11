package dev.parez.sidekick.database

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Release-variant stubs for the Database Inspector.
 *
 * Same fully-qualified names and signatures as the real family, so consumer code compiles unchanged
 * — but nothing opens a connection, reads a schema, or renders a panel.
 */
@Immutable
public sealed interface DbValue {
    @Immutable public data class Text(val value: String) : DbValue

    @Immutable public data class Number(val value: String) : DbValue

    @Immutable public data class Blob(val bytes: Long) : DbValue

    @Immutable public data object Null : DbValue
}

@Immutable public data class DbColumn(val name: String, val type: String)

@Immutable
public data class DbTable(
    val name: String,
    val columns: List<DbColumn>,
    val rows: List<List<DbValue>>,
    val rowIds: List<Long>? = null,
) {
    public val rowCount: Int
        get() = rows.size

    public val editable: Boolean
        get() = rowIds != null
}

@Immutable
public data class DbInfo(val fileName: String, val engine: String, val rowCountLabel: String)

public const val ROW_LIMIT: Int = 500

@Stable
public interface DatabaseController {
    public fun refresh()

    public fun updateCell(table: String, rowId: Long, column: String, value: String?)

    public suspend fun query(sql: String): DbTable
}

public fun isReadOnlySql(sql: String): Boolean {
    val trimmed = sql.trim().trimEnd(';').trim()
    if (trimmed.isEmpty() || trimmed.contains(';')) return false
    val firstWord = trimmed.takeWhile { !it.isWhitespace() && it != '(' }.uppercase()
    return firstWord in setOf("SELECT", "PRAGMA", "EXPLAIN")
}

public sealed interface DbSupport {
    public data object Detached : DbSupport

    public data object Available : DbSupport

    public data class Unsupported(val reason: String) : DbSupport
}

@Immutable
public data class DatabaseInspectorState(
    val support: DbSupport = DbSupport.Detached,
    val info: DbInfo? = null,
    val tables: List<DbTable> = emptyList(),
    val controller: DatabaseController? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
)

public class DatabaseInspectorStore {
    private val _state = MutableStateFlow(DatabaseInspectorState())
    public val state: StateFlow<DatabaseInspectorState> = _state.asStateFlow()

    @Suppress("UNUSED_PARAMETER") public fun attach(controller: DatabaseController): Unit = Unit

    @Suppress("UNUSED_PARAMETER") public fun markUnsupported(reason: String): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun publish(info: DbInfo, tables: List<DbTable>): Unit = Unit

    @Suppress("UNUSED_PARAMETER") public fun setRefreshing(refreshing: Boolean): Unit = Unit

    @Suppress("UNUSED_PARAMETER") public fun publishError(message: String): Unit = Unit

    public fun refresh(): Unit = Unit
}

/** Release-variant stub — renders nothing. */
public class DatabaseInspectorPlugin : SidekickPlugin {
    override val id: String = "sidekick.database-inspector"
    override val title: String = "Database"
    override val icon: ImageVector = Icons.Default.Storage

    @Composable override fun Content(): Unit = Unit
}
