package dev.parez.sidekick.log.kermit

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import dev.parez.sidekick.log.LogMonitorStore

/**
 * Release-variant stub for `LogMonitorLogWriter`. Subclasses Kermit's [LogWriter] so it can be
 * installed via `Logger.setLogWriters(...)` exactly like the real writer, but discards every entry
 * instead of forwarding to the store.
 */
@Suppress("UNUSED_PARAMETER")
class LogMonitorLogWriter(store: LogMonitorStore = LogMonitorStore) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) = Unit
}
