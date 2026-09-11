package dev.parez.sidekick.log

public fun interface LogCollector {
    public fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
