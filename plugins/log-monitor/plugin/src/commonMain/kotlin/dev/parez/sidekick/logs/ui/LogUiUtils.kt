package dev.parez.sidekick.logs.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.parez.sidekick.logs.LogLevel

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
