package dev.parez.sidekick.log

import androidx.compose.runtime.Immutable

@Immutable
data class LogFilter(val query: String = "", val levels: Set<LogLevel> = emptySet()) {
    fun matches(entry: LogEntry): Boolean = false

    fun toLikeToken(): String = "%"
}
