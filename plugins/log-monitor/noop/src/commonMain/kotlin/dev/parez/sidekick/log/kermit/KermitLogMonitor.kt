package dev.parez.sidekick.log.kermit

import dev.parez.sidekick.log.LogMonitorPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Release-variant stub for `kermitLogMonitor`. Returns a no-op [LogMonitorPlugin]
 * and intentionally does NOT call `Logger.setLogWriters` — production builds
 * keep their original Kermit setup untouched and no log entries are recorded.
 */
@Suppress("UNUSED_PARAMETER")
fun kermitLogMonitor(
    retentionPeriod: Duration = 1.hours,
): LogMonitorPlugin = LogMonitorPlugin(retentionPeriod = retentionPeriod)
