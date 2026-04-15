package dev.parez.sidekick.logs

/**
 * Abstract integration interface for logging SDKs.
 * Implement this to bridge any logging library into the log monitor.
 */
fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
