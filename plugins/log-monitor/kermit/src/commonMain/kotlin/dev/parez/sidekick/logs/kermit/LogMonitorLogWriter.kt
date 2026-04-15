package dev.parez.sidekick.logs.kermit

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import dev.parez.sidekick.logs.LogLevel
import dev.parez.sidekick.logs.LogMonitorStore

/**
 * A Kermit [LogWriter] that forwards log messages to [LogMonitorStore].
 *
 * Install alongside the default platform writer:
 * ```
 * Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(store))
 * ```
 */
class LogMonitorLogWriter(
    private val store: LogMonitorStore = LogMonitorStore,
) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        store.record(
            level = severity.toLogLevel(),
            tag = tag,
            message = message,
            throwable = throwable,
        )
    }

    private fun Severity.toLogLevel(): LogLevel = when (this) {
        Severity.Verbose -> LogLevel.VERBOSE
        Severity.Debug   -> LogLevel.DEBUG
        Severity.Info    -> LogLevel.INFO
        Severity.Warn    -> LogLevel.WARN
        Severity.Error   -> LogLevel.ERROR
        Severity.Assert  -> LogLevel.ASSERT
    }
}
