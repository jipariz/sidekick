package dev.parez.sidekick.crash.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.crash.CrashRecord

// Mirrors log-monitor's LogUiUtils. "Fatal" is this panel's severity axis, so it gets
// the same colour / label / icon vocabulary the log levels use — which is what lets the
// badges and identity discs look like they belong to the same overlay.

internal fun CrashRecord.severityLabel(): String = if (fatal) "FATAL" else "NON-FATAL"

internal fun CrashRecord.shortLabel(): String = if (fatal) "F" else "N"

@Composable
internal fun CrashRecord.color(): Color =
    if (fatal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

@Composable
internal fun CrashRecord.onColor(): Color =
    if (fatal) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary

/** Tonal container for the identity disc — recessed badge rather than a high-emphasis fill. */
@Composable
internal fun CrashRecord.containerColor(): Color =
    if (fatal) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

@Composable
internal fun CrashRecord.onContainerColor(): Color =
    if (fatal) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

internal fun CrashRecord.icon(): ImageVector =
    if (fatal) Icons.Default.Error else Icons.Default.WarningAmber

/** Wall-clock time of day, matching the log monitor's timestamp format. */
internal fun formatTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = (totalSeconds / 3600) % 24
    return "${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)}"
}

private fun pad2(n: Long): String = if (n < 10) "0$n" else "$n"
