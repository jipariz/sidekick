package dev.parez.sidekick.database.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.database.DbTable
import dev.parez.sidekick.database.DbValue

// Mirrors log-monitor's LogUiUtils: a value's storage class is the Database panel's
// equivalent of a log level, so it gets the same colour/label/icon vocabulary and
// therefore the same badges and identity discs.

internal fun DbValue.typeLabel(): String =
    when (this) {
        is DbValue.Text -> "TEXT"
        is DbValue.Number -> "NUM"
        is DbValue.Blob -> "BLOB"
        DbValue.Null -> "NULL"
    }

@Composable
internal fun DbValue.color(): Color =
    when (this) {
        is DbValue.Text -> MaterialTheme.colorScheme.primary
        is DbValue.Number -> MaterialTheme.colorScheme.secondary
        is DbValue.Blob -> MaterialTheme.colorScheme.tertiary
        DbValue.Null -> MaterialTheme.colorScheme.outline
    }

/** Text colour for a cell. NULL and blobs are recessive — they carry no readable value. */
@Composable
internal fun DbValue.contentColor(): Color =
    when (this) {
        DbValue.Null,
        is DbValue.Blob -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

internal fun DbValue.display(): String =
    when (this) {
        is DbValue.Text -> value
        is DbValue.Number -> value
        is DbValue.Blob -> "blob · ${bytes}B"
        DbValue.Null -> "NULL"
    }

/** Pre-fill for the cell editor. NULL and blobs have no sensible text form. */
internal fun DbValue.asEditableText(): String =
    when (this) {
        is DbValue.Text -> value
        is DbValue.Number -> value
        is DbValue.Blob,
        DbValue.Null -> ""
    }

/**
 * Identity-disc colours for a table, keyed on whether it can be edited. Read-only tables read as
 * recessed surface rather than a coloured container, the same way VERBOSE logs do.
 */
@Composable
internal fun DbTable.containerColor(): Color =
    if (editable) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

@Composable
internal fun DbTable.onContainerColor(): Color =
    if (editable) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

internal fun DbTable.icon(): ImageVector =
    if (editable) Icons.Default.Inventory2 else Icons.Default.Lock

/** Icon for a column, chosen from its declared affinity. */
internal fun columnIcon(declaredType: String): ImageVector =
    when {
        declaredType.isBlank() -> Icons.Default.DataObject
        declaredType.contains("INT", ignoreCase = true) ||
            declaredType.contains("REAL", ignoreCase = true) ||
            declaredType.contains("NUM", ignoreCase = true) ||
            declaredType.contains("DOUBLE", ignoreCase = true) -> Icons.Default.Numbers
        declaredType.contains("BLOB", ignoreCase = true) -> Icons.Default.DataObject
        else -> Icons.Default.TextFields
    }

internal fun rowCountLabel(rows: Int, columns: Int): String =
    "$rows ${if (rows == 1) "row" else "rows"} · $columns ${if (columns == 1) "column" else "columns"}"
