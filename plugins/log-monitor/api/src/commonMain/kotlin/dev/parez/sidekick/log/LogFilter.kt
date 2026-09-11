package dev.parez.sidekick.log

import androidx.compose.runtime.Immutable

@Immutable
public data class LogFilter(val query: String = "", val levels: Set<LogLevel> = emptySet()) {
    public fun matches(entry: LogEntry): Boolean {
        if (
            query.isNotBlank() &&
                !entry.tag.contains(query, ignoreCase = true) &&
                !entry.message.contains(query, ignoreCase = true)
        )
            return false
        if (levels.isNotEmpty() && entry.level !in levels) return false
        return true
    }

    public fun toLikeToken(): String {
        if (query.isBlank()) return "%"
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}
