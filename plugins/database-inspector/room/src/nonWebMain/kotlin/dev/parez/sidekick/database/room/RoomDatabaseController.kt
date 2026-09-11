package dev.parez.sidekick.database.room

import androidx.room3.PooledConnection
import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
import dev.parez.sidekick.database.DatabaseController
import dev.parez.sidekick.database.DatabaseInspectorStore
import dev.parez.sidekick.database.DbColumn
import dev.parez.sidekick.database.DbInfo
import dev.parez.sidekick.database.DbTable
import dev.parez.sidekick.database.DbValue
import dev.parez.sidekick.database.ROW_LIMIT
import dev.parez.sidekick.database.isReadOnlySql
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Room's own bookkeeping tables are noise in a schema browser.
private const val TABLES_SQL =
    "SELECT name FROM sqlite_master WHERE type = 'table' " +
        "AND name NOT LIKE 'sqlite_%' " +
        "AND name NOT IN ('room_master_table', 'android_metadata') " +
        "ORDER BY name"

/**
 * Reads and writes through Room's pooled connections.
 *
 * This is the only portable path: on desktop and iOS there is no `SupportSQLiteDatabase` underneath
 * a Room 3 database, but `useReaderConnection` / `useWriterConnection` / `usePrepared` are
 * available on every target Room publishes.
 */
internal class RoomDatabaseController(
    private val database: RoomDatabase,
    private val fileName: String?,
    private val store: DatabaseInspectorStore,
) : DatabaseController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun refresh() {
        scope.launch {
            store.setRefreshing(true)
            runCatching { snapshot() }
                .onFailure { store.publishError("Refresh failed: ${it.readableMessage()}") }
        }
    }

    override fun updateCell(table: String, rowId: Long, column: String, value: String?) {
        scope.launch {
            runCatching {
                    database.useWriterConnection { connection ->
                        // Identifiers cannot be bound as parameters, so they are quoted.
                        // They originate from PRAGMA output, not user input.
                        connection.usePrepared(
                            "UPDATE ${table.quoted()} SET ${column.quoted()} = ? WHERE rowid = ?"
                        ) { statement ->
                            if (value == null) statement.bindNull(1)
                            else statement.bindText(1, value)
                            statement.bindLong(2, rowId)
                            statement.step()
                        }
                    }
                    // Wakes any Flow the host app has on this table, so its own UI
                    // reflects the edit instead of silently diverging.
                    runCatching { database.invalidationTracker.refreshAsync() }
                    snapshot()
                }
                .onFailure { store.publishError("Update failed: ${it.readableMessage()}") }
        }
    }

    override suspend fun query(sql: String): DbTable {
        require(isReadOnlySql(sql)) {
            "Only SELECT, PRAGMA and EXPLAIN statements are allowed, and only one at a time."
        }
        return database.useReaderConnection { connection ->
            connection.usePrepared(sql) { statement ->
                val columns =
                    (0 until statement.getColumnCount()).map {
                        DbColumn(statement.getColumnName(it), type = "")
                    }
                val rows = buildList {
                    while (size < ROW_LIMIT && statement.step()) add(statement.readRow(from = 0))
                }
                // No rowIds: a projection has no addressable rows, so results are read-only.
                DbTable(name = "query", columns = columns, rows = rows)
            }
        }
    }

    private suspend fun snapshot() {
        val tables = database.useReaderConnection { connection -> connection.readTables() }
        store.publish(
            info =
                DbInfo(
                    fileName = fileName ?: "room",
                    engine = "SQLite · Room",
                    rowCountLabel = "${tables.sumOf { it.rowCount }} rows in ${tables.size} tables",
                ),
            tables = tables,
        )
    }

    private suspend fun PooledConnection.readTables(): List<DbTable> {
        val names =
            usePrepared(TABLES_SQL) { statement ->
                buildList { while (statement.step()) add(statement.getText(0)) }
            }
        return names.map { name -> readTable(name) }
    }

    private suspend fun PooledConnection.readTable(name: String): DbTable {
        val columns =
            usePrepared("PRAGMA table_info(${name.quoted()})") { statement ->
                buildList {
                    while (statement.step()) {
                        add(DbColumn(name = statement.getText(1), type = statement.getText(2)))
                    }
                }
            }

        // rowid first so cell edits can address the row. WITHOUT ROWID tables reject
        // the column and fall back to a read-only projection.
        val withRowId = runCatching {
            usePrepared("SELECT rowid AS _sk_rowid, * FROM ${name.quoted()} LIMIT $ROW_LIMIT") {
                statement ->
                val ids = mutableListOf<Long>()
                val rows = buildList {
                    while (statement.step()) {
                        ids += statement.getLong(0)
                        add(statement.readRow(from = 1))
                    }
                }
                ids.toList() to rows
            }
        }

        val (rowIds, rows) =
            withRowId.getOrElse {
                usePrepared("SELECT * FROM ${name.quoted()} LIMIT $ROW_LIMIT") { statement ->
                    null to buildList { while (statement.step()) add(statement.readRow(from = 0)) }
                }
            }

        return DbTable(name = name, columns = columns, rows = rows, rowIds = rowIds)
    }
}

/**
 * Reads the current row from [from] onward, dispatching on each column's *actual* type rather than
 * its declared affinity — SQLite is dynamically typed, so a column declared INTEGER can hold text.
 */
private fun SQLiteStatement.readRow(from: Int): List<DbValue> =
    (from until getColumnCount()).map { index ->
        when (getColumnType(index)) {
            SQLITE_DATA_NULL -> DbValue.Null
            SQLITE_DATA_INTEGER -> DbValue.Number(getLong(index).toString())
            SQLITE_DATA_FLOAT -> DbValue.Number(getDouble(index).toString())
            SQLITE_DATA_BLOB -> DbValue.Blob(getBlob(index).size.toLong())
            else -> DbValue.Text(getText(index))
        }
    }

/** Escapes an SQLite identifier for interpolation: backticks, with embedded ones doubled. */
private fun String.quoted(): String = "`" + replace("`", "``") + "`"

private fun Throwable.readableMessage(): String =
    message ?: this::class.simpleName ?: "unknown error"
