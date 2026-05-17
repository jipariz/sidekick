package dev.parez.sidekick.logs

fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
