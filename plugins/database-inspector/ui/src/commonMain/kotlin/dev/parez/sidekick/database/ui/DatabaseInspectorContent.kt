package dev.parez.sidekick.database.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.database.DatabaseInspectorState
import dev.parez.sidekick.database.DbSupport
import dev.parez.sidekick.database.DbTable
import dev.parez.sidekick.database.DbValue
import dev.parez.sidekick.database.QueryResult

/** A cell the user has tapped, held while the edit dialog is open. */
private data class PendingEdit(
    val table: String,
    val rowId: Long,
    val column: String,
    val current: String,
)

/**
 * Root of the Database panel.
 *
 * Table list and table contents are one vertical flow rather than a list-detail scaffold: a schema
 * is usually a handful of tables, and the contents pane needs the full width for its grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseInspectorContent(
    state: DatabaseInspectorState,
    selectedTable: String?,
    sql: String,
    queryResult: QueryResult?,
    onSelectTable: (String?) -> Unit,
    onSqlChange: (String) -> Unit,
    onRunQuery: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateCell: (table: String, rowId: Long, column: String, value: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var showQueryPane by remember { mutableStateOf(false) }
    val table = state.tables.firstOrNull { it.name == selectedTable }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTable ?: "Database",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                showQueryPane -> showQueryPane = false
                                selectedTable != null -> onSelectTable(null)
                                else -> onBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.support is DbSupport.Available) {
                        IconButton(onClick = { showQueryPane = !showQueryPane }) {
                            Icon(Icons.Default.Terminal, contentDescription = "SQL console")
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            when (val support = state.support) {
                is DbSupport.Detached ->
                    DbEmptyState(
                        "No database attached. Call DatabaseInspector.attach(database) during app " +
                            "startup to inspect it here."
                    )
                is DbSupport.Unsupported -> DbEmptyState(support.reason)
                DbSupport.Available ->
                    when {
                        showQueryPane ->
                            QueryPane(
                                sql = sql,
                                result = queryResult,
                                onSqlChange = onSqlChange,
                                onRunQuery = onRunQuery,
                            )
                        table != null ->
                            TablePane(
                                table = table,
                                onCellClick = { row, column ->
                                    val rowId = table.rowIds?.getOrNull(row) ?: return@TablePane
                                    pendingEdit =
                                        PendingEdit(
                                            table = table.name,
                                            rowId = rowId,
                                            column = table.columns[column].name,
                                            current = table.rows[row][column].asEditableText(),
                                        )
                                },
                            )
                        else -> TableListPane(state = state, onSelectTable = onSelectTable)
                    }
            }
        }
    }

    pendingEdit?.let { edit ->
        CellEditDialog(
            edit = edit,
            onDismiss = { pendingEdit = null },
            onConfirm = { newValue ->
                onUpdateCell(edit.table, edit.rowId, edit.column, newValue)
                pendingEdit = null
            },
        )
    }
}

@Composable
private fun TableListPane(
    state: DatabaseInspectorState,
    onSelectTable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.tables.isEmpty()) {
        DbEmptyState("This database has no tables.", modifier)
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        state.info?.let { info ->
            item(key = "__info", contentType = "info") {
                ListItem(
                    headlineContent = { Text(info.fileName) },
                    supportingContent = { Text("${info.engine} · ${info.rowCountLabel}") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        items(items = state.tables, key = { it.name }, contentType = { "table" }) { table ->
            ListItem(
                headlineContent = { Text(table.name) },
                supportingContent = {
                    val suffix = if (table.editable) "" else " · read-only"
                    Text("${table.rowCount} rows · ${table.columns.size} columns$suffix")
                },
                // clickable before ListItem's own padding would shrink the touch
                // target, so it goes on the full-width root.
                modifier = Modifier.fillMaxWidth().clickable { onSelectTable(table.name) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun TablePane(
    table: DbTable,
    onCellClick: (row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (!table.editable) {
            Text(
                text = "No rowid on this table — contents are read-only.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        DbTableGrid(table = table, onCellClick = onCellClick)
    }
}

@Composable
private fun QueryPane(
    sql: String,
    result: QueryResult?,
    onSqlChange: (String) -> Unit,
    onRunQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = sql,
            onValueChange = onSqlChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text("SELECT / PRAGMA / EXPLAIN") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            trailingIcon = {
                IconButton(onClick = onRunQuery) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                }
            },
        )
        when (result) {
            null -> DbEmptyState("Read-only statements only. Results are capped at 500 rows.")
            QueryResult.Running -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is QueryResult.Failed ->
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            is QueryResult.Succeeded ->
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    DbTableGrid(table = result.table, onCellClick = { _, _ -> })
                }
        }
    }
}

@Composable
private fun CellEditDialog(edit: PendingEdit, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var text by remember(edit) { mutableStateOf(edit.current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${edit.table}.${edit.column}") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = false,
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                Text(
                    text = "Written as text; SQLite converts by the column's affinity.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = {
            Column {
                TextButton(onClick = { onConfirm(null) }) { Text("Set NULL") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** NULL and blobs have no sensible text form to pre-fill an editor with. */
private fun DbValue.asEditableText(): String =
    when (this) {
        is DbValue.Text -> value
        is DbValue.Number -> value
        is DbValue.Blob,
        DbValue.Null -> ""
    }
