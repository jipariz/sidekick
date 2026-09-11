package dev.parez.sidekick.database

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * One cell value, already rendered to something displayable.
 *
 * Deliberately not typed as `Any?` — the inspector shows values, it does not compute with them, and
 * a closed set keeps the UI's `when` exhaustive.
 */
@Immutable
public sealed interface DbValue {
    @Immutable public data class Text(val value: String) : DbValue

    @Immutable public data class Number(val value: String) : DbValue

    /** Blobs are summarised by size; the inspector never materialises their bytes. */
    @Immutable public data class Blob(val bytes: Long) : DbValue

    @Immutable public data object Null : DbValue
}

@Immutable public data class DbColumn(val name: String, val type: String)

/**
 * A table snapshot: its columns and up to [ROW_LIMIT] rows.
 *
 * [rowIds] carries the SQLite `rowid` for each row when the table has one, which is what makes cell
 * editing addressable. It is `null` for `WITHOUT ROWID` tables and for query results — both of
 * which the UI therefore renders read-only.
 */
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

/** Maximum rows read per table or query. A debug overlay is not a data browser. */
public const val ROW_LIMIT: Int = 500

/**
 * A live database behind the Database panel.
 *
 * Implemented by `:plugins:database-inspector:room` for Room databases; the contract is
 * deliberately driver-agnostic so another storage engine could be wired in without touching the UI.
 */
@Stable
public interface DatabaseController {
    /** Re-read every table and republish. Bound to the Refresh action. */
    public fun refresh()

    /**
     * Writes one cell. [rowId] comes from [DbTable.rowIds]; a null [value] writes SQL NULL,
     * anything else is bound as text and converted by the column's affinity.
     */
    public fun updateCell(table: String, rowId: Long, column: String, value: String?)

    /**
     * Runs a read-only statement and returns the result as a table. Implementations must reject
     * anything that could mutate the database — see [isReadOnlySql].
     */
    public suspend fun query(sql: String): DbTable
}

/**
 * Whether [sql] is a statement the inspector is willing to run.
 *
 * An allowlist, not a denylist: only `SELECT`, `PRAGMA` and `EXPLAIN` pass, and only as a single
 * statement. A debug overlay that will happily run `DROP TABLE` against a developer's real database
 * because they mistyped is not a feature. Cell editing is the deliberate, narrow write path.
 */
public fun isReadOnlySql(sql: String): Boolean {
    val trimmed = sql.trim().trimEnd(';').trim()
    if (trimmed.isEmpty()) return false
    // A second statement would sail past the keyword check below.
    if (trimmed.contains(';')) return false
    val firstWord = trimmed.takeWhile { !it.isWhitespace() && it != '(' }.uppercase()
    return firstWord in READ_ONLY_STATEMENTS
}

// `WITH` is deliberately absent: SQLite allows a CTE to precede INSERT / UPDATE /
// DELETE, so `WITH x AS (...) DELETE FROM t` would sail through a keyword check.
// Inline the CTE as a subquery instead.
private val READ_ONLY_STATEMENTS = setOf("SELECT", "PRAGMA", "EXPLAIN")
