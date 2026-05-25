package dev.parez.sidekick.log.kermit

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import dev.parez.sidekick.log.LogMonitorPlugin
import dev.parez.sidekick.log.LogMonitorStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Returns a [LogMonitorPlugin] with the Kermit bridge already installed onto the process-wide
 * [Logger] singleton — a one-liner for the common case.
 *
 * Equivalent to:
 * ```kotlin
 * LogMonitorPlugin(retentionPeriod = retentionPeriod).also { plugin ->
 *     Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
 * }
 * ```
 *
 * Typical usage:
 * ```kotlin
 * val logPlugin = remember { kermitLogMonitor() }
 * ```
 *
 * For custom [Logger] instances (e.g. tag-scoped loggers), wire manually:
 * ```kotlin
 * val plugin = LogMonitorPlugin()
 * myLogger.mutableConfig.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
 * ```
 *
 * @param retentionPeriod How long captured entries remain available in the panel.
 */
fun kermitLogMonitor(retentionPeriod: Duration = 1.hours): LogMonitorPlugin {
    val plugin = LogMonitorPlugin(retentionPeriod = retentionPeriod)
    Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(LogMonitorStore))
    return plugin
}
