package dev.parez.sidekick.database.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.database.DbTable
import dev.parez.sidekick.database.DbValue

/** Fixed so the header row and the body stay in the same columns while scrolling together. */
private val CellWidth = 168.dp

/**
 * Storage-class badge.
 *
 * Same shape as `MethodBadge` and `LevelBadge`: extraSmall corner, labelSmall monospace, 6/2
 * padding. The Database panel reuses that vocabulary so a badge means the same thing everywhere in
 * the overlay.
 */
@Composable
internal fun DbTypeBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/**
 * The pane-identity disc used by every Sidekick detail header — a tonal circle carrying the pane's
 * icon, so the user can tell at a glance which plugin they are looking at.
 */
@Composable
internal fun DbIdentityDisc(
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = containerColor, shape = CircleShape, modifier = modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Renders [table] as a scrollable grid.
 *
 * The whole grid scrolls horizontally as one unit so headers stay aligned with their column —
 * per-cell scrolling would let them drift apart.
 *
 * @param onCellClick receives (rowIndex, columnIndex). Only meaningful for tables that carry row
 *   ids; see [DbTable.editable].
 */
@Composable
internal fun DbTableGrid(
    table: DbTable,
    onCellClick: (row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScroll = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth().horizontalScroll(horizontalScroll)) {
        // fillMaxWidth so the header band runs the full viewport width like the row
        // dividers below it, rather than stopping at the last column.
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(vertical = 10.dp)) {
                table.columns.forEach { column ->
                    Column(
                        modifier = Modifier.width(CellWidth).padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = columnIcon(column.type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = column.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (column.type.isNotBlank()) {
                            DbTypeBadge(column.type)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        table.rows.forEachIndexed { rowIndex, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            color = value.contentColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        )
    }
}

/**
 * The overlay's shared empty state — 48dp outline-variant icon, titleMedium headline, bodySmall
 * supporting line. Identical shape to the network and log monitors' empty states.
 */
@Composable
internal fun DbEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
