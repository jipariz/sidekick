package dev.parez.sidekick.database.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.database.DatabaseInspectorState
import dev.parez.sidekick.database.DbSupport
import dev.parez.sidekick.database.DbTable
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
 * Table list and contents are one vertical flow rather than a list-detail scaffold: a schema is
 * usually a handful of tables, and the contents pane needs the full width for its grid.
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
                        text =
                            when {
                                showQueryPane -> "SQL console"
                                selectedTable != null -> selectedTable
                                else -> "Database"
                            },
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
            AnimatedVisibility(visible = state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.error?.let { message -> DbErrorBanner(message) }

            when (val support = state.support) {
                DbSupport.Detached ->
                    DbEmptyState(
                        icon = Icons.Default.Storage,
                        title = "No database attached",
                        description = "Call DatabaseInspector.attach(database) during startup.",
                    )
                is DbSupport.Unsupported ->
                    DbEmptyState(
                        icon = Icons.Default.Storage,
                        title = "Not available on this platform",
                        description = support.reason,
                    )
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
private fun DbErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
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
        DbEmptyState(
            icon = Icons.Default.Storage,
            title = "No tables",
            description = "This database has no tables yet.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        state.info?.let { info ->
            item(key = "__info", contentType = "info") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DbIdentityDisc(
                            icon = Icons.Default.Storage,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Column {
                            Text(
                                text = "DATABASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = info.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = "${info.engine} · ${info.rowCountLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        items(items = state.tables, key = { it.name }, contentType = { "table" }) { table ->
            TableRow(table = table, onClick = { onSelectTable(table.name) })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun TableRow(table: DbTable, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        // clickable before padding would shrink the touch target, so it goes on the
        // full-width root.
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DbIdentityDisc(
            icon = table.icon(),
            containerColor = table.containerColor(),
            contentColor = table.onContainerColor(),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = table.name,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = rowCountLabel(table.rowCount, table.columns.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!table.editable) {
            DbTypeBadge("read-only")
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DbTypeBadge("read-only")
                Text(
                    text = "No rowid on this table, so cells cannot be edited.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = sql,
                onValueChange = onSqlChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "SELECT · PRAGMA · EXPLAIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                shape = MaterialTheme.shapes.small,
            )
            FilledIconButton(
                onClick = onRunQuery,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
            }
        }

        when (result) {
            null ->
                DbEmptyState(
                    icon = Icons.Default.TravelExplore,
                    title = "Read-only statements",
                    description = "SELECT, PRAGMA or EXPLAIN. Results are capped at 500 rows.",
                )
            QueryResult.Running -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is QueryResult.Failed -> DbErrorBanner(result.message)
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
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Default.Storage, contentDescription = null) },
        title = {
            Text(
                text = "${edit.table}.${edit.column}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = false,
                    shape = MaterialTheme.shapes.small,
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                Text(
                    text = "Written as text; SQLite converts by the column's affinity.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onConfirm(null) }) { Text("Set NULL") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
