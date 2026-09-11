package dev.parez.sidekick.database.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.database.DbTable
import dev.parez.sidekick.database.DbValue

/** Width of one cell. Fixed so columns line up between the header row and the body. */
private val CellWidth = 160.dp

/**
 * Renders [table] as a scrollable grid.
 *
 * The whole grid scrolls horizontally as one unit so the header stays aligned with its column —
 * per-cell scrolling would let them drift apart.
 *
 * @param onCellClick invoked with (rowIndex, columnIndex) when a cell is tapped. Only called for
 *   tables that carry row ids; see [DbTable.editable].
 */
@Composable
internal fun DbTableGrid(
    table: DbTable,
    onCellClick: (row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScroll = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth().horizontalScroll(horizontalScroll)) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            table.columns.forEach { column ->
                Column(modifier = Modifier.width(CellWidth).padding(horizontal = 8.dp)) {
                    Text(
                        text = column.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (column.type.isNotBlank()) {
                        Text(
                            text = column.type.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        table.rows.forEachIndexed { rowIndex, row ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                row.forEachIndexed { columnIndex, value ->
                    DbCell(
                        value = value,
                        editable = table.editable,
                        onClick = { onCellClick(rowIndex, columnIndex) },
                        modifier = Modifier.width(CellWidth),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun DbCell(
    value: DbValue,
    editable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = editable,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Text(
            text = value.display(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = value.tint(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
}

/** Empty-state row used by both the table pane and the query pane. */
@Composable
internal fun DbEmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DbValue.display(): String =
    when (this) {
        is DbValue.Text -> value
        is DbValue.Number -> value
        is DbValue.Blob -> "<blob ${bytes}B>"
        DbValue.Null -> "NULL"
    }

@Composable
private fun DbValue.tint() =
    when (this) {
        DbValue.Null,
        is DbValue.Blob -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
