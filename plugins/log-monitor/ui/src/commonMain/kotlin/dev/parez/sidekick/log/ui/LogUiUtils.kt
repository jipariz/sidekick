package dev.parez.sidekick.log.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.log.LogLevel

internal fun LogLevel.label(): String = when (this) {
    LogLevel.VERBOSE -> "V"
    LogLevel.DEBUG   -> "D"
    LogLevel.INFO    -> "I"
    LogLevel.WARN    -> "W"
    LogLevel.ERROR   -> "E"
    LogLevel.ASSERT  -> "A"
}

internal fun LogLevel.fullLabel(): String = when (this) {
    LogLevel.VERBOSE -> "Verbose"
    LogLevel.DEBUG   -> "Debug"
    LogLevel.INFO    -> "Info"
    LogLevel.WARN    -> "Warn"
    LogLevel.ERROR   -> "Error"
    LogLevel.ASSERT  -> "Assert"
}

@Composable
internal fun LogLevel.color(): Color = when (this) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.outline
    LogLevel.DEBUG   -> MaterialTheme.colorScheme.secondary
    LogLevel.INFO    -> MaterialTheme.colorScheme.primary
    LogLevel.WARN    -> MaterialTheme.colorScheme.tertiary
    LogLevel.ERROR   -> MaterialTheme.colorScheme.error
    LogLevel.ASSERT  -> MaterialTheme.colorScheme.error
}

@Composable
internal fun LogLevel.onColor(): Color = when (this) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurface
    LogLevel.DEBUG   -> MaterialTheme.colorScheme.onSecondary
    LogLevel.INFO    -> MaterialTheme.colorScheme.onPrimary
    LogLevel.WARN    -> MaterialTheme.colorScheme.onTertiary
    LogLevel.ERROR   -> MaterialTheme.colorScheme.onError
    LogLevel.ASSERT  -> MaterialTheme.colorScheme.onError
}

/**
 * Tonal-container backgrounds for the pane-identity disc in the detail header.
 * Mirrors [color] but uses the M3 `*Container` roles so the disc reads as a
 * recessed badge instead of a high-emphasis fill.
 */
@Composable
internal fun LogLevel.containerColor(): Color = when (this) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.surfaceContainerHigh
    LogLevel.DEBUG   -> MaterialTheme.colorScheme.secondaryContainer
    LogLevel.INFO    -> MaterialTheme.colorScheme.primaryContainer
    LogLevel.WARN    -> MaterialTheme.colorScheme.tertiaryContainer
    LogLevel.ERROR   -> MaterialTheme.colorScheme.errorContainer
    LogLevel.ASSERT  -> MaterialTheme.colorScheme.errorContainer
}

@Composable
internal fun LogLevel.onContainerColor(): Color = when (this) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.DEBUG   -> MaterialTheme.colorScheme.onSecondaryContainer
    LogLevel.INFO    -> MaterialTheme.colorScheme.onPrimaryContainer
    LogLevel.WARN    -> MaterialTheme.colorScheme.onTertiaryContainer
    LogLevel.ERROR   -> MaterialTheme.colorScheme.onErrorContainer
    LogLevel.ASSERT  -> MaterialTheme.colorScheme.onErrorContainer
}

/** Severity-specific icon for the pane-identity disc. */
internal fun LogLevel.icon(): ImageVector = when (this) {
    LogLevel.VERBOSE -> Icons.Default.Article
    LogLevel.DEBUG   -> Icons.Default.BugReport
    LogLevel.INFO    -> Icons.Default.Info
    LogLevel.WARN    -> Icons.Default.Warning
    LogLevel.ERROR   -> Icons.Default.Error
    LogLevel.ASSERT  -> Icons.Default.Error
}

internal fun formatTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val ms = millis % 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = (totalSeconds / 3600) % 24
    return "${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)}.${pad3(ms)}"
}

private fun pad2(n: Long): String = if (n < 10) "0$n" else "$n"
private fun pad3(n: Long): String = when {
    n < 10 -> "00$n"
    n < 100 -> "0$n"
    else -> "$n"
}
