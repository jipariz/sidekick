package dev.parez.sidekick.log

import androidx.compose.runtime.Immutable

@Immutable
public data class LogFilter(val query: String = "", val levels: Set<LogLevel> = emptySet()) {
    public fun matches(entry: LogEntry): Boolean = false

    public fun toLikeToken(): String = "%"
}
